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
	"fmt"
	"github.com/apache/flink-statefun/statefun-go-sdk/pkg/flink/statefun/internal/messages"
	"log"
	"time"
)

type ExpireMode int

const (
	NONE ExpireMode = iota
	AfterWrite
	AfterInvoke
)

func (e ExpireMode) String() string {
	switch e {
	case NONE:
		return "NONE"
	case AfterWrite:
		return "AfterWrite"
	case AfterInvoke:
		return "AfterInvoke"
	default:
		return "UNKNOWN"
	}
}

func (e ExpireMode) toInternal() messages.FromFunction_ExpirationSpec_ExpireMode {
	switch e {
	case NONE:
		return messages.FromFunction_ExpirationSpec_NONE
	case AfterWrite:
		return messages.FromFunction_ExpirationSpec_AFTER_WRITE
	case AfterInvoke:
		return messages.FromFunction_ExpirationSpec_AFTER_INVOKE
	default:
		log.Panicf("unknown expiration mode %v", e)
		// this return is just to make the compiler happy
		// it will never be reached
		return 0
	}
}

type ExpirationSpec struct {
	ExpireMode ExpireMode
	Ttl        time.Duration
}

func (e ExpirationSpec) String() string {
	return fmt.Sprintf("ExpirationSpec{ExpireMode=%s, Ttl=%s}", e.ExpireMode.String(), e.Ttl)
}

type StateSpec struct {
	StateName      string
	ExpirationSpec ExpirationSpec
}

func (s StateSpec) String() string {
	return fmt.Sprintf("StateSpec{StateName=%v, ExpirationSpec=%v}", s.StateName, s.ExpirationSpec)
}

func (s StateSpec) toInternal() *messages.FromFunction_PersistedValueSpec {
	return &messages.FromFunction_PersistedValueSpec{
		StateName: s.StateName,
		ExpirationSpec: &messages.FromFunction_ExpirationSpec{
			Mode:              s.ExpirationSpec.ExpireMode.toInternal(),
			ExpireAfterMillis: s.ExpirationSpec.Ttl.Milliseconds(),
		},
	}
}
