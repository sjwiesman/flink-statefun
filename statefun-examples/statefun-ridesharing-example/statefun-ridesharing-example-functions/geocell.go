package main

import (
	"context"
	"github.com/sjwiesman/statefun-go/pkg/flink/statefun"
	"google.golang.org/protobuf/types/known/anypb"
)

const (
	gcDrivers = "drivers"
)

func GeoCellFunc(ctx context.Context, runtime statefun.StatefulFunctionRuntime, msg *anypb.Any) error {
	caller := statefun.Caller(ctx)

	cell := JoinCell{}
	if err := msg.UnmarshalTo(&cell); err == nil {
		return addDriver(caller.Id, runtime)
	}

	leave := LeaveCell{}
	if err := msg.UnmarshalTo(&leave); err == nil {
		return removeDriver(caller.Id, runtime)
	}

	get := GetDriver{}
	if err := msg.UnmarshalTo(&get); err == nil {
		return getDriver(caller, runtime)
	}

	return nil
}

func addDriver(driverId string, runtime statefun.StatefulFunctionRuntime) error {
	state := GeoCellState{}
	if _, err := runtime.Get(gcDrivers, &state); err != nil {
		return err
	}

	state.DriverId[driverId] = true

	return runtime.Set(gcDrivers, &state)
}

func removeDriver(driverId string, runtime statefun.StatefulFunctionRuntime) error {
	state := GeoCellState{}
	if _, err := runtime.Get(gcDrivers, &state); err != nil {
		return err
	}

	delete(state.DriverId, driverId)

	return runtime.Set(gcDrivers, &state)
}

func getDriver(caller *statefun.Address, runtime statefun.StatefulFunctionRuntime) error {
	state := GeoCellState{}
	if _, err := runtime.Get(gcDrivers, &state); err != nil {
		return err
	}

	for driverId := range state.DriverId {
		response := &DriverInCell{
			DriverId: driverId,
		}

		return runtime.Send(caller, response)
	}

	return runtime.Send(caller, &DriverInCell{})
}
