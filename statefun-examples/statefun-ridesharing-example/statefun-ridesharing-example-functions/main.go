package main

import (
	"github.com/sjwiesman/statefun-go/pkg/flink/statefun"
	"github.com/sjwiesman/statefun-go/pkg/flink/statefun/io"
	"net/http"
)

var (
	GeoCell = statefun.FunctionType{
		Namespace: "ridesharing",
		Type:      "geocell",
	}

	Driver = statefun.FunctionType{
		Namespace: "ridesharing",
		Type:      "driver",
	}

	Ride = statefun.FunctionType{
		Namespace: "ridesharing",
		Type:      "ride",
	}

	Passenger = statefun.FunctionType{
		Namespace: "ridesharing",
		Type:      "passenger",
	}

	ToPassengerEgress = io.EgressIdentifier{
		EgressNamespace: "ridesharing",
		EgressType:      "to-passenger",
	}

	ToDriverEgress = io.EgressIdentifier{
		EgressNamespace: "ridesharing",
		EgressType:      "to-driver",
	}
)

func main() {

	registry := statefun.NewFunctionRegistry()
	registry.RegisterFunctionPointer(GeoCell, GeoCellFunc)
	registry.RegisterFunctionPointer(Driver, DriverFunc)
	registry.RegisterFunctionPointer(Passenger, PassengerFunc)
	registry.RegisterFunctionPointer(Ride, RideFunc)

	http.Handle("/statefun", registry)
	_ = http.ListenAndServe(":8000", nil)
}
