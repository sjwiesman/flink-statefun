package main

import (
	"fmt"
	any "google.golang.org/protobuf/types/known/anypb"
	"net/http"
	"statefun-go/pkg/flink/statefun"
	"time"
)

type Greeter struct{}

func greeting(name string, seen int32) string {
	switch seen {
	case 0:
		return fmt.Sprintf("Hello %s ! \\uD83D\\uDE0E", name)
	case 1:
		return fmt.Sprintf("Hello again %s ! \\uD83E\\uDD17", name)
	case 2:
		return fmt.Sprintf("Third time is a charm! %s! \\uD83E\\uDD73", name)
	case 3:
		return fmt.Sprintf("Happy to see you once again %s ! \\uD83D\\uDE32", name)
	default:
		return fmt.Sprintf("Hello at the %d-th time %s \\uD83D\\uDE4C", seen+1, name)
	}
}

func (greeter Greeter) Invoke(_ *any.Any, ctx *statefun.Context) error {

	counter := &Counter{}
	if err := ctx.GetAndUnpack("counter", counter); err != nil {
		return err
	}

	greeting := &Greeting{greeting(ctx.Self().Id, counter.Count)}
	counter.Count += 1

	if err := ctx.ReplyAndPack(greeting); err != nil {
		return err
	}

	if err := ctx.SendAfterAndPack(ctx.Caller(), time.Duration(60000), greeting); err != nil {
		return err
	}

	if err := ctx.SendEgressAndPack(statefun.Egress{"test", "egress"}, greeting); err != nil {
		return err
	}

	if err := ctx.SetAndPack("counter", counter); err != nil {
		return err
	}

	return nil
}

func main() {
	functions := statefun.NewStatefulFunctions()
	functions.StatefulFunction(statefun.FunctionType{
		Namespace: "remote",
		Type:      "greeter",
	}, Greeter{})

	http.Handle("/statefun", functions)
	http.ListenAndServe(":8080", nil)
}
