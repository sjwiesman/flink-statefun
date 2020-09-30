package main

import (
	"context"
	"fmt"
	"github.com/sjwiesman/statefun-go/pkg/flink/statefun"
	"github.com/sjwiesman/statefun-go/pkg/flink/statefun/io"
	"google.golang.org/protobuf/types/known/anypb"
	"math/rand"
)

func PassengerFunc(ctx context.Context, runtime statefun.StatefulFunctionRuntime, msg *anypb.Any) error {
	inbound := InboundPassengerMessage{}
	if err := msg.UnmarshalTo(&inbound); err == nil {
		request := inbound.GetRequestRide()
		return whenRideRequested(ctx, runtime, request)
	}

	driverJoin := DriverJoinsRide{}
	if err := msg.UnmarshalTo(&driverJoin); err == nil {
		return whenDriverJoins(ctx, runtime, &driverJoin)
	}

	rideFailed := RideFailed{}
	if err := msg.UnmarshalTo(&rideFailed); err == nil {
		return whenRideFailed(ctx, runtime, &rideFailed)
	}

	rideStarted := RideStarted{}
	if err := msg.UnmarshalTo(&rideStarted); err == nil {
		return whenRideHasStarted(ctx, runtime, &rideStarted)
	}

	rideEnded := RideEnded{}
	if err := msg.UnmarshalTo(&rideEnded); err == nil {
		return whenRideHasEnded(ctx, runtime)
	}

	return nil
}

func whenRideRequested(ctx context.Context, runtime statefun.StatefulFunctionRuntime, request *InboundPassengerMessage_RequestRide) error {
	passengerId := statefun.Self(ctx).Id
	rideId := fmt.Sprintf("ride-%d", rand.Uint64())

	joinRide := &PassengerJoinsRide{
		PassengerId:  passengerId,
		StartGeoCell: request.GetStartGeoCell(),
		EndGeoCell:   request.GetEndGeoCell(),
	}

	ride := &statefun.Address{
		FunctionType: Ride,
		Id:           rideId,
	}

	return runtime.Send(ride, joinRide)
}

func whenDriverJoins(ctx context.Context, runtime statefun.StatefulFunctionRuntime, driverJoin *DriverJoinsRide) error {
	passengerId := statefun.Self(ctx).Id
	record := io.KafkaRecord{
		Topic: "to-rPassenger",
		Key:   passengerId,
		Value: &OutboundPassengerMessage{
			PassengerId: passengerId,
			Message: &OutboundPassengerMessage_DriverFound{
				DriverFound: &OutboundPassengerMessage_DriverHasBeenFound{
					DriverId:      driverJoin.DriverId,
					DriverGeoCell: driverJoin.DriverLocation,
				},
			},
		},
	}

	message, err := record.ToMessage()
	if err != nil {
		return err
	}

	return runtime.SendEgress(ToPassengerEgress, message)
}

func whenRideFailed(ctx context.Context, runtime statefun.StatefulFunctionRuntime, rideFailed *RideFailed) error {
	passengerId := statefun.Self(ctx).Id
	record := io.KafkaRecord{
		Topic: "to-rPassenger",
		Key:   passengerId,
		Value: &OutboundPassengerMessage{
			PassengerId: passengerId,
			Message: &OutboundPassengerMessage_RideFailed_{
				RideFailed: &OutboundPassengerMessage_RideFailed{
					RideId: rideFailed.RideId,
				},
			},
		},
	}

	message, err := record.ToMessage()
	if err != nil {
		return err
	}

	return runtime.SendEgress(ToPassengerEgress, message)
}

func whenRideHasStarted(ctx context.Context, runtime statefun.StatefulFunctionRuntime, started *RideStarted) error {
	passengerId := statefun.Self(ctx).Id
	record := io.KafkaRecord{
		Topic: "to-rPassenger",
		Key:   passengerId,
		Value: &OutboundPassengerMessage{
			PassengerId: passengerId,
			Message: &OutboundPassengerMessage_RideStarted_{
				RideStarted: &OutboundPassengerMessage_RideStarted{
					DriverId: started.DriverId,
				},
			},
		},
	}

	message, err := record.ToMessage()
	if err != nil {
		return err
	}

	return runtime.SendEgress(ToPassengerEgress, message)
}

func whenRideHasEnded(ctx context.Context, runtime statefun.StatefulFunctionRuntime) error {
	passengerId := statefun.Self(ctx).Id
	record := io.KafkaRecord{
		Topic: "to-rPassenger",
		Key:   passengerId,
		Value: &OutboundPassengerMessage{
			PassengerId: passengerId,
			Message: &OutboundPassengerMessage_RideEnded_{
				RideEnded: &OutboundPassengerMessage_RideEnded{},
			},
		},
	}

	message, err := record.ToMessage()
	if err != nil {
		return err
	}

	return runtime.SendEgress(ToPassengerEgress, message)
}
