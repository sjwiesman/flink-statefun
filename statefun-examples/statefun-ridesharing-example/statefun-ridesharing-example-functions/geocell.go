package main

import (
	"context"
	"github.com/golang/protobuf/ptypes"
	"github.com/golang/protobuf/ptypes/any"
	"github.com/sjwiesman/statefun-go/pkg/flink/statefun"
)

const (
	drivers = "drivers"
)

func GeoCellFunc(ctx context.Context, runtime statefun.StatefulFunctionRuntime, msg *any.Any) error {
	caller := statefun.Caller(ctx)

	cell := JoinCell{}
	if err := ptypes.UnmarshalAny(msg, &cell); err == nil {
		return addDriver(caller.Id, runtime)
	}

	leave := LeaveCell{}
	if err := ptypes.UnmarshalAny(msg, &leave); err == nil {
		return removeDriver(caller.Id, runtime)
	}

	get := GetDriver{}
	if err := ptypes.UnmarshalAny(msg, &get); err == nil {
		return getDriver(caller, runtime)
	}

	return nil
}

func addDriver(driverId string, runtime statefun.StatefulFunctionRuntime) error {
	state := GeoCellState{}
	if err := runtime.Get(drivers, &state); err != nil {
		return err
	}

	state.DriverId[driverId] = true

	return runtime.Set(drivers, &state)
}

func removeDriver(driverId string, runtime statefun.StatefulFunctionRuntime) error {
	state := GeoCellState{}
	if err := runtime.Get(drivers, &state); err != nil {
		return err
	}

	delete(state.DriverId, driverId)

	return runtime.Set(drivers, &state)
}

func getDriver(caller *statefun.Address, runtime statefun.StatefulFunctionRuntime) error {
	state := GeoCellState{}
	if err := runtime.Get(drivers, &state); err != nil {
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
