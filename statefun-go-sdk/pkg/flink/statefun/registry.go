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
	"context"
	"github.com/apache/flink-statefun/statefun-go-sdk/pkg/flink/statefun/internal/errors"
	"github.com/apache/flink-statefun/statefun-go-sdk/pkg/flink/statefun/internal/messages"
	"github.com/valyala/bytebufferpool"
	"google.golang.org/protobuf/proto"
	"log"
	"net/http"
)

// Keeps a mapping from FunctionType to stateful functions
// and serves them to the Flink runtime.
//
// HTTP Endpoint
//
//    import "net/http"
//
//	  func main() {
//    	registry := NewFunctionRegistry()
//		registry.RegisterFunction(greeterType, GreeterFunction{})
//
//	  	http.Handle("/service", registry)
//	  	_ = http.ListenAndServe(":8000", nil)
//	  }
//
// AWS Lambda
//
//    import "github.com/aws/aws-lambda"
//
//	  func main() {
//    	registry := NewFunctionRegistry()
//		registry.RegisterFunction(greeterType, GreeterFunction{})
//
//		lambda.StartHandler(registry)
//	  }
type FunctionRegistry interface {
	// Handler for processing runtime messages from
	// an http endpoint
	http.Handler

	// Handler for processing arbitrary payloads.
	// This method provides compliance with AWS Lambda
	// handler.
	Invoke(ctx context.Context, payload []byte) ([]byte, error)

	// Register a StatefulFunction under a FunctionType.
	RegisterFunction(funcType FunctionType, function StatefulFunction)
}

type functions struct {
	module     map[FunctionType]StatefulFunction
	stateSpecs map[FunctionType]map[string]*messages.FromFunction_PersistedValueSpec
}

func NewFunctionRegistry() FunctionRegistry {
	return &functions{
		module:     make(map[FunctionType]StatefulFunction),
		stateSpecs: make(map[FunctionType]map[string]*messages.FromFunction_PersistedValueSpec),
	}
}

func (functions *functions) RegisterFunction(funcType FunctionType, function StatefulFunction) {
	specs := function.StateSpecs()
	log.Printf("registering stateful function %s with states %v", funcType.String(), specs)

	functions.module[funcType] = function
	functions.stateSpecs[funcType] = make(map[string]*messages.FromFunction_PersistedValueSpec, len(specs))

	for _, state := range specs {
		functions.stateSpecs[funcType][state.StateName] = state.toInternal()
	}
}

func (functions functions) Invoke(ctx context.Context, payload []byte) ([]byte, error) {
	toFunction := &messages.ToFunction{}
	if err := proto.Unmarshal(payload, toFunction); err != nil {
		return nil, errors.BadRequest("failed to unmarshal payload %w", err)
	}

	fromFunction, err := functions.executeBatch(ctx, toFunction)
	if err != nil {
		return nil, err
	}

	return proto.Marshal(fromFunction)
}

func (functions functions) ServeHTTP(w http.ResponseWriter, req *http.Request) {
	if !validRequest(w, req) {
		return
	}

	buffer := bytebufferpool.Get()
	defer bytebufferpool.Put(buffer)

	_, err := buffer.ReadFrom(req.Body)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	bytes, err := functions.Invoke(req.Context(), buffer.Bytes())
	if err != nil {
		http.Error(w, err.Error(), errors.ToCode(err))
		log.Print(err)
		return
	}

	_, _ = w.Write(bytes)
}

// perform basic HTTP validation on the incoming payload
func validRequest(w http.ResponseWriter, req *http.Request) bool {
	if req.Method != "POST" {
		http.Error(w, "invalid request method", http.StatusMethodNotAllowed)
		return false
	}

	contentType := req.Header.Get("Content-type")
	if contentType != "" && contentType != "application/octet-stream" {
		http.Error(w, "invalid content type", http.StatusUnsupportedMediaType)
		return false
	}

	if req.Body == nil || req.ContentLength == 0 {
		http.Error(w, "empty request body", http.StatusBadRequest)
		return false
	}

	return true
}

func checkRegisteredStates(
	providedStates []*messages.ToFunction_PersistedValue,
	statesSpecs map[string]*messages.FromFunction_PersistedValueSpec) *messages.FromFunction {

	stateSet := make(map[string]bool, len(providedStates))
	for _, state := range providedStates {
		stateSet[state.StateName] = true
	}

	var missingValues []*messages.FromFunction_PersistedValueSpec
	for name, spec := range statesSpecs {
		if _, exists := stateSet[name]; !exists {
			missingValues = append(missingValues, spec)
		}
	}

	if missingValues == nil {
		return nil
	}

	return &messages.FromFunction{
		Response: &messages.FromFunction_IncompleteInvocationContext_{
			IncompleteInvocationContext: &messages.FromFunction_IncompleteInvocationContext{
				MissingValues: missingValues,
			},
		},
	}
}

func (functions functions) executeBatch(ctx context.Context, request *messages.ToFunction) (*messages.FromFunction, error) {
	invocations := request.GetInvocation()
	if invocations == nil {
		return nil, errors.BadRequest("missing invocations for batch")
	}

	funcType := FunctionType{
		Namespace: invocations.Target.Namespace,
		Type:      invocations.Target.Type,
	}

	function, exists := functions.module[funcType]
	if !exists {
		return nil, errors.BadRequest("%s does not exist", funcType.String())
	}

	if missing := checkRegisteredStates(request.GetInvocation().State, functions.stateSpecs[funcType]); missing != nil {
		return missing, nil
	}

	runtime, err := newRuntime(invocations.State)
	if err != nil {
		return nil, errors.Wrap(err, "failed to setup runtime for batch")
	}

	self := addressFromInternal(invocations.Target)
	ctx = context.WithValue(ctx, selfKey, self)
	for _, invocation := range invocations.Invocations {
		select {
		case <-ctx.Done():
			return nil, ctx.Err()
		default:
			caller := addressFromInternal(invocation.Caller)
			ctx = context.WithValue(ctx, callerKey, caller)
			err := function.Invoke(ctx, runtime, (*invocation).Argument)

			if err != nil {
				return nil, errors.Wrap(err, "failed to execute function %s", self.String())
			}
		}
	}

	return runtime.fromFunction()
}
