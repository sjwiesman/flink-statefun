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
		Type:      "rDriver",
	}

	Ride = statefun.FunctionType{
		Namespace: "ridesharing",
		Type:      "dRide",
	}

	Passenger = statefun.FunctionType{
		Namespace: "ridesharing",
		Type:      "rPassenger",
	}

	ToPassengerEgress = io.EgressIdentifier{
		EgressNamespace: "ridesharing",
		EgressType:      "to-rPassenger",
	}

	ToDriverEgress = io.EgressIdentifier{
		EgressNamespace: "ridesharing",
		EgressType:      "to-rDriver",
	}
)

func main() {

	registry := statefun.NewFunctionRegistry()
	registry.RegisterFunctionPointer(GeoCell, GeoCellFunc)
	registry.RegisterFunctionPointer(Driver, DriverFunc)
	registry.RegisterFunctionPointer(Passenger, PassengerFunc)
	registry.RegisterFunctionPointer(Ride, RideFunc)

	http.Handle("/functions", registry)
	_ = http.ListenAndServe(":8000", nil)
}
