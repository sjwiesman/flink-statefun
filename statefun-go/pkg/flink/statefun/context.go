package statefun

import (
	"errors"
	"fmt"
	"github.com/golang/protobuf/proto"
	"github.com/golang/protobuf/ptypes"
	"github.com/golang/protobuf/ptypes/any"
	"time"
)

type state struct {
	updated bool
	value   *any.Any
}

type Context struct {
	self              *Address
	caller            *Address
	states            map[string]*state
	invocations       []*FromFunction_Invocation
	delayedInvocation []*FromFunction_DelayedInvocation
	outgoingEgress    []*FromFunction_EgressMessage
}

func newContext(self *Address, persistedValues []*ToFunction_PersistedValue) Context {
	ctx := Context{
		self:              self,
		caller:            nil,
		states:            map[string]*state{},
		invocations:       []*FromFunction_Invocation{},
		delayedInvocation: []*FromFunction_DelayedInvocation{},
		outgoingEgress:    []*FromFunction_EgressMessage{},
	}

	for _, persistedValue := range persistedValues {
		value := any.Any{}
		err := proto.Unmarshal(persistedValue.StateValue, &value)
		if err != nil {

		}

		ctx.states[persistedValue.StateName] = &state{
			updated: false,
			value:   &value,
		}
	}

	return ctx
}

func (ctx *Context) Self() *Address {
	return ctx.self
}

func (ctx *Context) Caller() *Address {
	return ctx.caller
}

func (ctx *Context) Get(name string) (*any.Any, error) {
	state := ctx.states[name]
	if state == nil {
		return nil, errors.New(fmt.Sprintf("Unknown state name %s", name))
	}

	return state.value, nil
}

func (ctx *Context) GetAndUnpack(name string, state proto.Message) error {
	packedState := ctx.states[name]
	if packedState == nil {
		return errors.New(fmt.Sprintf("Unknown state name %s", name))
	}

	if packedState.value == nil || packedState.value.TypeUrl == "" {
		return nil
	}

	err := ptypes.UnmarshalAny(packedState.value, state)
	if err != nil {
		return err
	}

	return nil
}

func (ctx *Context) Set(name string, value *any.Any) error {
	state := ctx.states[name]
	if state == nil {
		return errors.New(fmt.Sprintf("Unknown state name %s", name))
	}

	state.updated = true
	state.value = value
	ctx.states[name] = state

	return nil
}

func (ctx *Context) SetAndPack(name string, value proto.Message) error {
	if value == nil {
		return ctx.Set(name, nil)
	}

	state := ctx.states[name]
	if state == nil {
		return errors.New(fmt.Sprintf("Unknown state name %s", name))
	}

	packedState, err := ptypes.MarshalAny(value)
	if err != nil {
		return err
	}

	state.updated = true
	state.value = packedState
	ctx.states[name] = state

	return nil
}

func (ctx *Context) Clear(name string) {
	_ = ctx.Set(name, nil)
}

func (ctx *Context) Send(target *Address, message *any.Any) error {
	if message == nil {
		return errors.New("cannot send nil message to function")
	}

	invocation := &FromFunction_Invocation{
		Target:   target,
		Argument: message,
	}

	ctx.invocations = append(ctx.invocations, invocation)
	return nil
}

func (ctx *Context) SendAndPack(target *Address, message proto.Message) error {
	if message == nil {
		return errors.New("cannot send nil message to function")
	}

	packedState, err := ptypes.MarshalAny(message)
	if err != nil {
		return err
	}

	return ctx.Send(target, packedState)
}

func (ctx *Context) Reply(message *any.Any) error {
	if message == nil {
		return errors.New("cannot send nil message to function")
	}

	return ctx.Send(ctx.caller, message)
}

func (ctx *Context) ReplyAndPack(message proto.Message) error {
	return ctx.SendAndPack(ctx.caller, message)
}

func (ctx *Context) SendAfter(target *Address, duration time.Duration, message *any.Any) error {
	if message == nil {
		return errors.New("cannot send nil message to function")
	}

	delayedInvocation := &FromFunction_DelayedInvocation{
		Target:    target,
		DelayInMs: duration.Milliseconds(),
		Argument:  message,
	}

	ctx.delayedInvocation = append(ctx.delayedInvocation, delayedInvocation)
	return nil
}

func (ctx *Context) SendAfterAndPack(target *Address, duration time.Duration, message proto.Message) error {
	if message == nil {
		return errors.New("cannot send nil message to function")
	}

	packedMessage, err := ptypes.MarshalAny(message)
	if err != nil {
		return err
	}

	return ctx.SendAfter(target, duration, packedMessage)
}

func (ctx *Context) SendEgress(egress Egress, message *any.Any) error {
	if message == nil {
		return errors.New("cannot send nil message to egress")
	}

	egressMessage := &FromFunction_EgressMessage{
		EgressNamespace: egress.EgressNamespace,
		EgressType:      egress.EgressType,
		Argument:        message,
	}

	ctx.outgoingEgress = append(ctx.outgoingEgress, egressMessage)
	return nil
}

func (ctx *Context) SendEgressAndPack(egress Egress, message proto.Message) error {
	if message == nil {
		return errors.New("cannot send nil message to egress")
	}

	packedMessage, err := ptypes.MarshalAny(message)
	if err != nil {
		return err
	}

	return ctx.SendEgress(egress, packedMessage)
}

func (ctx *Context) SendKafka(egress Egress, topic string, key string, message *any.Any) error {
	if message == nil {
		return errors.New("cannot send nil message to kafka")
	}
	valueBytes, err := proto.Marshal(message)
	if err != nil {
		return err
	}
	kafkaProducerRecord := &KafkaProducerRecord{
		Key:        key,
		Topic:      topic,
		ValueBytes: valueBytes,
	}

	return ctx.SendEgressAndPack(egress, kafkaProducerRecord)
}

func (ctx *Context) SendKafkaAndPack(egress Egress, topic string, key string, message proto.Message) error {
	if message == nil {
		return errors.New("cannot send nil message to kafka")
	}

	packedMessage, err := ptypes.MarshalAny(message)
	if err != nil {
		return err
	}

	return ctx.SendKafka(egress, topic, key, packedMessage)
}

func (ctx *Context) fromFunction() (*FromFunction, error) {
	var mutations []*FromFunction_PersistedValueMutation
	for name, state := range ctx.states {
		if !state.updated {
			continue
		}

		mutationType := FromFunction_PersistedValueMutation_MODIFY
		if state.value == nil {
			mutationType = FromFunction_PersistedValueMutation_DELETE
		}

		bytes, err := proto.Marshal(state.value)
		if err != nil {
			return nil, err
		}

		mutation := &FromFunction_PersistedValueMutation{
			MutationType: mutationType,
			StateName:    name,
			StateValue:   bytes,
		}

		mutations = append(mutations, mutation)
	}

	return &FromFunction{
		Response: &FromFunction_InvocationResult{
			InvocationResult: &FromFunction_InvocationResponse{
				StateMutations:     mutations,
				OutgoingMessages:   ctx.invocations,
				DelayedInvocations: ctx.delayedInvocation,
				OutgoingEgresses:   ctx.outgoingEgress,
			},
		},
	}, nil
}
