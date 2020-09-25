FROM golang:1.14

RUN mkdir -p go/ridesharing

COPY . go/ridesharing

WORKDIR go/ridesharing

RUN go get -d -v ./...

RUN go build -v ./...

CMD ["./ridesharing"]