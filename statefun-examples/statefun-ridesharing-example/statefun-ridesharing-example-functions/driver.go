package main

import (
	"context"
	"fmt"
	"github.com/golang/protobuf/ptypes"
	"github.com/golang/protobuf/ptypes/any"
	"github.com/sjwiesman/statefun-go/pkg/flink/statefun"
	"github.com/sjwiesman/statefun-go/pkg/flink/statefun/io"
)

var (
	ride     = "ride"
	location = "location"
)

func DriverFunc(ctx context.Context, runtime statefun.StatefulFunctionRuntime, msg *any.Any) error {
	pickup := PickupPassenger{}
	if err := ptypes.UnmarshalAny(msg, &pickup); err == nil {
		return pickupNeeded(ctx, runtime, &pickup)
	}

	driverMessage := InboundDriverMessage{}
	if err := ptypes.UnmarshalAny(msg, &driverMessage); err == nil {
		if rideStarted := driverMessage.GetRideStarted(); rideStarted != nil {
			return whenRideStarted(ctx, runtime)
		}

		if rideEnded := driverMessage.GetRideEnded(); rideEnded != nil {
			return whenRideEnded(runtime)
		}

		if locationUpdate := driverMessage.GetLocationUpdate(); locationUpdate != nil {
			return whenLocationUpdated(runtime, locationUpdate)
		}
	}

	return nil
}

func pickupNeeded(ctx context.Context, runtime statefun.StatefulFunctionRuntime, pickup *PickupPassenger) error {
	if runtime.Exists(ride) {
		// this driver is currently in a ride,
		// and therefore cannot take anymore
		// passengers
		driverId := statefun.Self(ctx).Id
		caller := statefun.Caller(ctx)

		response := DriverRejectsPickup{
			DriverId: driverId,
			RideId:   caller.Id,
		}
		return runtime.Send(caller, &response)
	}

	// We are called by the ride function, so we remember its id
	// for future communication.

	currentRide := CurrentRide{
		RideId: statefun.Caller(ctx).Id,
	}
	if err := runtime.Set(ride, &currentRide); err != nil {
		return err
	}

	currentLocation := CurrentLocation{}
	if err := runtime.Get(location, &currentLocation); err != nil {
		return err
	}

	// We also need to unregister ourselves from the current geo cell we belong to.
	cell := &statefun.Address{
		FunctionType: GeoCell,
		Id:           fmt.Sprint(currentLocation.Location),
	}

	if err := runtime.Send(cell, &LeaveCell{}); err != nil {
		return err
	}

	// Reply to the ride, saying we are taking this passenger
	driverId := statefun.Self(ctx).Id
	ride := statefun.Caller(ctx)
	rideId := ride.Id
	join := &DriverJoinsRide{
		DriverId:       driverId,
		PassengerId:    rideId,
		DriverLocation: currentLocation.Location,
	}

	if err := runtime.Send(ride, join); err != nil {
		return err
	}

	// Also send a command to the physical driver to pickup the passenger
	record := io.KafkaRecord{
		Topic: "to-driver",
		Key:   driverId,
		Value: &OutboundDriverMessage{
			DriverId: driverId,
			Message: &OutboundDriverMessage_PickupPassenger_{
				PickupPassenger: &OutboundDriverMessage_PickupPassenger{
					RideId:           rideId,
					StartGeoLocation: pickup.PassengerStartCell,
					EndGeoLocation:   pickup.PassengerEndCell,
				},
			},
		},
	}

	message, err := record.ToMessage()
	if err != nil {
		return err
	}

	return runtime.SendEgress(ToDriverEgress, message)
}

func whenRideStarted(ctx context.Context, runtime statefun.StatefulFunctionRuntime) error {
	currentLocation := CurrentLocation{}
	if err := runtime.Get(location, &currentLocation); err != nil {
		return err
	}

	currentRide := CurrentRide{}
	if err := runtime.Get(ride, &currentRide); err != nil {
		return err
	}

	started := RideStarted{
		DriverId:      statefun.Self(ctx).Id,
		DriverGeoCell: currentLocation.Location,
	}

	address := &statefun.Address{
		FunctionType: Ride,
		Id:           currentRide.RideId,
	}

	return runtime.Send(address, &started)
}

func whenRideEnded(runtime statefun.StatefulFunctionRuntime) error {
	currentRide := CurrentRide{}
	if err := runtime.Get(ride, &currentRide); err != nil {
		return nil
	}

	rideAddress := &statefun.Address{
		FunctionType: Ride,
		Id:           currentRide.RideId,
	}

	if err := runtime.Send(rideAddress, &RideEnded{}); err != nil {
		return err
	}
	runtime.Clear(ride)

	// register the driver as free at the current location
	currentLocation := CurrentLocation{}
	if err := runtime.Get(location, &currentLocation); err != nil {
		return err
	}

	geoCell := &statefun.Address{
		FunctionType: GeoCell,
		Id:           fmt.Sprint(currentLocation.Location),
	}

	return runtime.Send(geoCell, &JoinCell{})
}

func whenLocationUpdated(runtime statefun.StatefulFunctionRuntime, update *InboundDriverMessage_LocationUpdate) error {
	if !runtime.Exists(location) {
		// this is the first time this driver gets
		// a location update so we notify the relevant
		// geo cell function.

		location := &CurrentLocation{
			Location: update.CurrentGeoCell,
		}

		geoCell := &statefun.Address{
			FunctionType: GeoCell,
			Id:           fmt.Sprint(location.Location),
		}

		return runtime.Send(geoCell, location)
	}

	currentLocation := CurrentLocation{}
	if err := runtime.Get(location, &currentLocation); err != nil {
		return err
	}

	if currentLocation.Location == update.CurrentGeoCell {
		return nil
	}

	currentLocation.Location = update.CurrentGeoCell
	return runtime.Set(location, &currentLocation)
}
