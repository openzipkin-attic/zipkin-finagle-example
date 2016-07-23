# Basic example showing distributed tracing across Finagle apps
This is an example app where two Finagle (Java) services collaborate on an http request. Notably, timing of these requests are recorded into [Zipkin](http://zipkin.io/), a distributed tracing system. This allows you to see the how long the whole operation took, as well how much time was spent in each service.

Here's an example of what it looks like
<img width="972" alt="zipkin screen shot" src="https://cloud.githubusercontent.com/assets/64215/16300537/ff858dd6-3972-11e6-8e4c-4f7f4a6c707a.png">

# Implementation Overview

Web requests are served by [Finagle HTTP](https://github.com/twitter/finagle/tree/develop/finagle-http) controllers, which trace requests by default.

These traces are sent out of process with [Zipkin Finagle integration](https://github.com/openzipkin/zipkin-finagle) via a Kafka topic.

This example intentionally avoids advanced topics like async and load balancing, eventhough Finagle supports them.

# Running the example
This example has two services: frontend and backend. They both report trace data to zipkin over Kafka. To setup the demo, you need to start Frontend, Backend and Zipkin.

Once the services are started, open http://localhost:8080/
* This will call the backend (http://localhost:9000/api) and show the result, which defaults to a formatted date.

Next, you can view traces that went through the backend via http://192.168.99.100:9411/?serviceName=backend
* This is a docker run zipkin service which keeps traces in cassandra

## Starting the Services
First, start [Zipkin](http://zipkin.io/), which stores and queries traces reported by the above services.

Since the demos use Kafka, the easiest way to start everything is using Docker

```bash
$ git clone https://github.com/openzipkin/docker-zipkin
$ cd docker-zipkin
$ docker-compose -f docker-compose.yml -f docker-compose-kafka.yml up
```

Once Zipkin is up, open this project in IntelliJ and run [finagle.Frontend](/src/main/java/finagle/Frontend.java) and [finagle.Backend](/src/main/java/finagle/Backend.java)

