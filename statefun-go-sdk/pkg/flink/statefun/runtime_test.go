// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package statefun

import (
	"github.com/apache/flink-statefun/statefun-go-sdk/pkg/flink/statefun/internal/messages"
	"github.com/apache/flink-statefun/statefun-go-sdk/pkg/flink/statefun/io"
	"github.com/golang/protobuf/ptypes/any"
	"github.com/stretchr/testify/assert"
	"google.golang.org/protobuf/proto"
	"google.golang.org/protobuf/types/known/anypb"
	"testing"
	"time"
)

func TestRuntime(t *testing.T) {
	initialState := &any.Any{
		TypeUrl: "initialState",
	}

	serializedInitialState, _ := proto.Marshal(initialState)

	updatedState := &any.Any{
		TypeUrl: "updatedState",
	}

	serializedUpdatedState, _ := proto.Marshal(updatedState)

	target := Address{
		FunctionType: FunctionType{
			Namespace: "namespace",
			Type:      "type",
		},
		Id: "id",
	}

	sendMessage := &anypb.Any{
		TypeUrl: "send",
	}

	sendDelayMessage := &anypb.Any{
		TypeUrl: "send-delay",
	}

	egress := io.EgressIdentifier{
		EgressNamespace: "namespace",
		EgressType:      "egress-type",
	}

	egressMessage := &anypb.Any{
		TypeUrl: "egress",
	}

	initialStates := []*messages.ToFunction_PersistedValue{
		{
			StateName:  "A",
			StateValue: serializedInitialState,
		},
		{
			StateName:  "B",
			StateValue: serializedInitialState,
		},
		{
			StateName:  "C",
			StateValue: serializedInitialState,
		},
		{
			StateName:  "D",
			StateValue: nil,
		},
	}

	runtime, err := newRuntime(initialStates)
	assert.NoError(t, err, "failed to create a runtime")

	// Read and modify states

	var stateA anypb.Any
	exists, err := runtime.Get("A", &stateA)

	assert.True(t, exists, "state A does not exist")
	assert.NoError(t, err, "failed to read state A")
	assert.Equal(t, initialState.TypeUrl, stateA.TypeUrl, "wrong value read for state A")

	var stateB anypb.Any
	exists, err = runtime.Get("B", &stateB)

	assert.True(t, exists, "state B does not exist")
	assert.NoError(t, err, "failed to read state B")
	assert.Equal(t, initialState.TypeUrl, stateB.TypeUrl, "wrong value read for state A")

	runtime.Clear("B")

	var stateC anypb.Any
	exists, err = runtime.Get("C", &stateC)

	assert.True(t, exists, "state C does not exist")
	assert.NoError(t, err, "failed to read state C")
	assert.Equal(t, initialState.TypeUrl, stateC.TypeUrl, "wrong value read for state A")

	err = runtime.Set("C", updatedState)
	assert.NoError(t, err, "failed to update state C")

	var stateD anypb.Any
	exists, err = runtime.Get("D", &stateD)

	assert.False(t, exists, "state D does not exist")
	assert.NoError(t, err, "failed to read empty state D")

	var stateE anypb.Any
	exists, err = runtime.Get("E", &stateE)

	assert.Error(t, err, "read invalid state E")

	// send messages
	err = runtime.Send(target, sendMessage)
	assert.NoError(t, err, "failed to send a message")

	err = runtime.SendAfter(target, 1*time.Minute, sendDelayMessage)
	assert.NoError(t, err, "failed to send a message with delay")

	err = runtime.SendEgress(egress, egressMessage)
	assert.NoError(t, err, "failed to send a message to an egress")

	fromFunction, err := runtime.fromFunction()
	assert.NoError(t, err, "failed to generate response")

	result := fromFunction.GetInvocationResult()
	assert.NotNil(t, result, "generated nil result")

	assert.Len(t, result.StateMutations, 2, "wrong number of state mutations encoded")

	assert.Contains(t, result.StateMutations, &messages.FromFunction_PersistedValueMutation{
		MutationType: messages.FromFunction_PersistedValueMutation_DELETE,
		StateName:    "B",
	}, "failed to encode deleted state B")

	assert.Contains(t, result.StateMutations, &messages.FromFunction_PersistedValueMutation{
		MutationType: messages.FromFunction_PersistedValueMutation_MODIFY,
		StateName:    "C",
		StateValue:   serializedUpdatedState,
	}, "failed to encode updated state C")

	assert.Len(t, result.OutgoingMessages, 1, "wrong number of outgoing messages encoded")
	assert.Contains(t, result.OutgoingMessages, &messages.FromFunction_Invocation{
		Target: &messages.Address{
			Namespace: target.FunctionType.Namespace,
			Type:      target.FunctionType.Type,
			Id:        target.Id,
		},
		Argument: sendMessage,
	}, "failed to encode outgoing message")

	assert.Len(t, result.DelayedInvocations, 1, "wrong number of outgoing delayed messages encoded")
	assert.Contains(t, result.DelayedInvocations, &messages.FromFunction_DelayedInvocation{
		Target: &messages.Address{
			Namespace: target.FunctionType.Namespace,
			Type:      target.FunctionType.Type,
			Id:        target.Id,
		},
		DelayInMs: 60000,
		Argument:  sendDelayMessage,
	}, "failed to encode delayed outgoing message")

	assert.Len(t, result.OutgoingEgresses, 1, "wrong number of outgoing egress messages encoded")
	assert.Contains(t, result.OutgoingEgresses, &messages.FromFunction_EgressMessage{
		EgressNamespace: egress.EgressNamespace,
		EgressType:      egress.EgressType,
		Argument:        egressMessage,
	}, "failed to encode delayed egress message")
}
