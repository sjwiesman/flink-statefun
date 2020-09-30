package main

import (
	"context"
	"fmt"
	"github.com/sjwiesman/statefun-go/pkg/flink/statefun"
	"github.com/sjwiesman/statefun-go/pkg/flink/statefun/io"
	"google.golang.org/protobuf/types/known/anypb"
)

const (
	dRide     = "ride"
	dLocation = "location"
)

func DriverFunc(ctx context.Context, runtime statefun.StatefulFunctionRuntime, msg *anypb.Any) error {
	pickup := PickupPassenger{}
	if err := msg.UnmarshalTo(&pickup); err == nil {
		return pickupNeeded(ctx, runtime, &pickup)
	}

	driverMessage := InboundDriverMessage{}
	if err := msg.UnmarshalTo(&driverMessage); err == nil {
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
	currentRide := CurrentRide{}
	exists, err := runtime.Get(dRide, &currentRide)
	if err != nil {
		return err
	}

	if exists {
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

	// We are called by the dRide function, so we remember its id
	// for future communication.
	currentRide.RideId = statefun.Caller(ctx).Id
	if err := runtime.Set(dRide, &currentRide); err != nil {
		return err
	}

	currentLocation := CurrentLocation{}
	if _, err := runtime.Get(dLocation, &currentLocation); err != nil {
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

	// Reply to the dRide, saying we are taking this rPassenger
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

	// Also send a command to the physical rDriver to pickup the rPassenger
	record := io.KafkaRecord{
		Topic: "to-rDriver",
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
	if _, err := runtime.Get(dLocation, &currentLocation); err != nil {
		return err
	}

	currentRide := CurrentRide{}
	if _, err := runtime.Get(dRide, &currentRide); err != nil {
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
	if _, err := runtime.Get(dRide, &currentRide); err != nil {
		return nil
	}

	rideAddress := &statefun.Address{
		FunctionType: Ride,
		Id:           currentRide.RideId,
	}

	if err := runtime.Send(rideAddress, &RideEnded{}); err != nil {
		return err
	}
	runtime.Clear(dRide)

	// register the rDriver as free at the current dLocation
	currentLocation := CurrentLocation{}
	if _, err := runtime.Get(dLocation, &currentLocation); err != nil {
		return err
	}

	geoCell := &statefun.Address{
		FunctionType: GeoCell,
		Id:           fmt.Sprint(currentLocation.Location),
	}

	return runtime.Send(geoCell, &JoinCell{})
}

func whenLocationUpdated(runtime statefun.StatefulFunctionRuntime, update *InboundDriverMessage_LocationUpdate) error {
	currentLocation := CurrentLocation{}
	exists, err := runtime.Get(dLocation, &currentLocation)
	if err != nil {
		return err
	}

	if !exists {
		// this is the first time this rDriver gets
		// a dLocation update so we notify the relevant
		// geo cell function.

		currentLocation.Location = update.CurrentGeoCell

		geoCell := &statefun.Address{
			FunctionType: GeoCell,
			Id:           fmt.Sprint(currentLocation.Location),
		}

		return runtime.Send(geoCell, &currentLocation)
	}

	if currentLocation.Location == update.CurrentGeoCell {
		return nil
	}

	currentLocation.Location = update.CurrentGeoCell
	return runtime.Set(dLocation, &currentLocation)
}
