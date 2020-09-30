package main

import (
	"context"
	"fmt"
	"github.com/sjwiesman/statefun-go/pkg/flink/statefun"
	"google.golang.org/protobuf/types/known/anypb"
)

const (
	rPassenger = "passenger"
	rDriver    = "driver"
)

func RideFunc(ctx context.Context, runtime statefun.StatefulFunctionRuntime, msg *anypb.Any) error {
	join := PassengerJoinsRide{}
	if err := msg.UnmarshalTo(&join); err == nil {
		return whenPassengerJoinsRide(runtime, &join)
	}

	driverInCell := DriverInCell{}
	if err := msg.UnmarshalTo(&driverInCell); err == nil {
		return whenGeoCellResponds(runtime, &driverInCell)
	}

	reject := DriverRejectsPickup{}
	if err := msg.UnmarshalTo(&reject); err == nil {
		return whenDriverRejectsPickup(runtime)
	}

	joinsRide := DriverJoinsRide{}
	if err := msg.UnmarshalTo(&joinsRide); err == nil {
		return whenDriverJoinsRide(ctx, runtime, &joinsRide)
	}

	rideStarted := RideStarted{}
	if err := msg.UnmarshalTo(&rideStarted); err == nil {
		return startingRide(runtime, &rideStarted)
	}

	rideEnded := RideEnded{}
	if err := msg.UnmarshalTo(&rideEnded); err == nil {
		return endingRide(runtime, &rideEnded)
	}

	return nil
}

// When a passenger joins a ride, we have to:
// 1. remember what passenger id
// 2. remember the starting location
// 3. contact the geo cell of the starting location
// and ask for a free driver
func whenPassengerJoinsRide(runtime statefun.StatefulFunctionRuntime, join *PassengerJoinsRide) error {
	if err := runtime.Set(rPassenger, join); err != nil {
		return err
	}

	cell := statefun.Address{
		FunctionType: GeoCell,
		Id:           fmt.Sprint(join.StartGeoCell),
	}

	return runtime.Send(&cell, &GetDriver{})
}

// Geo cell responds, it might respond with: - there is no driver, in that case we fail the ride -
// there is a driver, let's ask them to pickup the rPassenger.
func whenGeoCellResponds(runtime statefun.StatefulFunctionRuntime, in *DriverInCell) error {
	request := PassengerJoinsRide{}
	if _, err := runtime.Get(rPassenger, &request); err != nil {
		return err
	}

	if len(in.DriverId) == 0 {
		// no free gcDrivers in this geo cell, at this example we just fail the dRide
		// but we can imagine that this is where we will expand our search to near geo cells
		passengerAddress := &statefun.Address{
			FunctionType: Passenger,
			Id:           request.PassengerId,
		}

		// by clearing our state, we essentially delete this instance of the dRide actor
		runtime.Clear(rPassenger)
		return runtime.Send(passengerAddress, &RideFailed{})
	}

	driverAddress := &statefun.Address{
		FunctionType: Driver,
		Id:           in.DriverId,
	}

	pickup := &PickupPassenger{
		DriverId:           in.DriverId,
		PassengerId:        request.PassengerId,
		PassengerStartCell: request.StartGeoCell,
		PassengerEndCell:   request.EndGeoCell,
	}

	return runtime.Send(driverAddress, pickup)
}

// A driver might not be free, or for some other reason they cannot take this ride,
// so we try another rDriver in that cell.
func whenDriverRejectsPickup(runtime statefun.StatefulFunctionRuntime) error {
	// try another rDriver, realistically we need to pass in a list of 'banned' gcDrivers,
	// so that the GeoCell will not offer us these gcDrivers again, but in this example
	// if a rDriver rejects a dRide, it means that he is currently busy (and it would soon delete
	// itself from the geo cell)

	passengerJoinsRide := PassengerJoinsRide{}
	if _, err := runtime.Get(rPassenger, &passengerJoinsRide); err != nil {
		return err
	}

	startGeoCell := &statefun.Address{
		FunctionType: GeoCell,
		Id:           fmt.Sprint(passengerJoinsRide.StartGeoCell),
	}

	return runtime.Send(startGeoCell, &GetDriver{})
}

func whenDriverJoinsRide(ctx context.Context, runtime statefun.StatefulFunctionRuntime, joinsRide *DriverJoinsRide) error {
	currentDriver := CurrentDriver{
		DriverId: statefun.Caller(ctx).Id,
	}

	if err := runtime.Set(rDriver, &currentDriver); err != nil {
		return err
	}

	request := PassengerJoinsRide{}
	if _, err := runtime.Get(rPassenger, &request); err != nil {
		return err
	}

	passengerAddress := &statefun.Address{
		FunctionType: Passenger,
		Id:           request.PassengerId,
	}

	return runtime.Send(passengerAddress, joinsRide)
}

// A driver has successfully picked up the passenger
func startingRide(runtime statefun.StatefulFunctionRuntime, started *RideStarted) error {
	request := PassengerJoinsRide{}
	if _, err := runtime.Get(rPassenger, &request); err != nil {
		return err
	}

	passengerAddress := &statefun.Address{
		FunctionType: Passenger,
		Id:           request.PassengerId,
	}

	return runtime.Send(passengerAddress, started)
}

// The driver has successfully reached the destination
func endingRide(runtime statefun.StatefulFunctionRuntime, ended *RideEnded) error {
	request := PassengerJoinsRide{}
	if _, err := runtime.Get(rPassenger, &request); err != nil {
		return err
	}

	passengerAddress := &statefun.Address{
		FunctionType: Passenger,
		Id:           request.PassengerId,
	}

	runtime.Clear(rPassenger)
	runtime.Clear(rDriver)

	return runtime.Send(passengerAddress, ended)
}
