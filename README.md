# Basic example showing distributed tracing across Finagle apps
This is an example app where two Finagle (Java) services collaborate on an http request. Notably, timing of these requests are recorded into [Zipkin](http://zipkin.io/), a distributed tracing system. This allows you to see the how long the whole operation took, as well how much time was spent in each service.

Here's an example of what it looks like
<img width="972" alt="zipkin screen shot" src="https://cloud.githubusercontent.com/assets/64215/17093008/05b9fe02-5279-11e6-9fab-3118522ba684.png">

# Implementation Overview

Web requests are served by [Finagle HTTP](https://github.com/twitter/finagle/tree/develop/finagle-http) controllers, which trace requests by default.

These traces are sent out of process with [Zipkin Finagle integration](https://github.com/openzipkin/zipkin-finagle) via Http.

This example intentionally avoids advanced topics like async and load balancing, eventhough Finagle supports them.

# Running the example
This example has two services: frontend and backend. They both report trace data to zipkin. To setup the demo, you need to start Frontend, Backend and Zipkin.

Once the services are started, open http://localhost:8081/
* This will call the backend (http://localhost:9000/api) and show the result, which defaults to a formatted date.

Next, you can view traces that went through the backend via http://localhost:9411/?serviceName=backend
* This is a locally run zipkin service which keeps traces in memory

## Starting the Services
In a separate tab or window, start each of [finagle.Frontend](/src/main/java/finagle/Frontend.java) and [finagle.Backend](/src/main/java/finagle/Backend.java):
```bash
$ ./mvnw compile exec:java -Dexec.mainClass=finagle.Frontend
$ ./mvnw compile exec:java -Dexec.mainClass=finagle.Backend
```

Next, run [Zipkin](http://zipkin.io/), which stores and queries traces reported by the above services.

```bash
curl -sSL https://zipkin.io/quickstart.sh | bash -s
java -jar zipkin.jar
```
