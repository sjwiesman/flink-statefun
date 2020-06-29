package statefun

import (
	"errors"
	"github.com/golang/protobuf/proto"
	any "google.golang.org/protobuf/types/known/anypb"
	"io/ioutil"
	"net/http"
)

type statefulFunctionPointer struct {
	f func(message *any.Any, ctx *Context) error
}

func (pointer *statefulFunctionPointer) Invoke(message *any.Any, ctx *Context) error {
	return pointer.f(message, ctx)
}

type StatefulFunctions struct {
	module map[FunctionType]StatefulFunction
}

func NewStatefulFunctions() StatefulFunctions {
	return StatefulFunctions{
		module: map[FunctionType]StatefulFunction{},
	}
}

func (functions *StatefulFunctions) StatefulFunction(funcType FunctionType, function StatefulFunction) {
	functions.module[funcType] = function
}

func (functions *StatefulFunctions) StatefulFunctionPointer(funcType FunctionType, function func(message *any.Any, ctx *Context) error) {
	functions.module[funcType] = &statefulFunctionPointer{
		f: function,
	}
}

func (functions StatefulFunctions) Process(request *ToFunction) (*FromFunction, error) {
	invocations := request.GetInvocation()
	if invocations == nil {
		return nil, errors.New("missing invocations for batch")
	}

	funcType := FunctionType{
		Namespace: invocations.Target.Namespace,
		Type:      invocations.Target.Type,
	}

	function, exists := functions.module[funcType]
	if !exists {
		return nil, errors.New(funcType.String() + " does not exist")
	}

	ctx := newContext(invocations.Target, invocations.State)

	for _, invocation := range invocations.Invocations {
		ctx.caller = invocation.Caller
		err := function.Invoke((*invocation).Argument, &ctx)
		if err != nil {
			return nil, err
		}
	}

	return ctx.fromFunction()
}

func (functions StatefulFunctions) ServeHTTP(w http.ResponseWriter, req *http.Request) {
	if req.Method != "POST" {
		http.Error(w, "Invalid request method", http.StatusMethodNotAllowed)
	}

	bytes, err := ioutil.ReadAll(req.Body)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
	}

	if len(bytes) == 0 {
			http.Error(w,"Empty request body", http.StatusBadRequest)
	}

	var request ToFunction
	err = proto.Unmarshal(bytes, &request)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
	}

	response, err := functions.Process(&request)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
	}

	bytes, err = proto.Marshal(response)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
	}

	w.WriteHeader(http.StatusOK)
	_, _ = w.Write(bytes)
}
