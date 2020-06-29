package main

type Counter struct {
	Count int32
}

func (c Counter) Reset() {}
func (c Counter) String() string {
	return ""
}
func (c Counter) ProtoMessage() {}

type Greeting struct {
	Msg string
}

func (c Greeting) Reset() {}
func (c Greeting) String() string {
	return ""
}
func (c Greeting) ProtoMessage() {}
