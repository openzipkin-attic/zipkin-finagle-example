package finagle;

import com.twitter.finagle.Service;
import com.twitter.finagle.builder.ClientBuilder;
import com.twitter.finagle.builder.ServerBuilder;
import com.twitter.finagle.http.Http;
import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.finagle.zipkin.core.SamplingTracer;
import com.twitter.util.Future;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import zipkin.finagle.kafka.KafkaZipkinTracer;

public class Frontend extends Service<Request, Response> {

  final Service<Request, Response> backendClient;

  Frontend(Service<Request, Response> backendClient) {
    this.backendClient = backendClient;
  }

  @Override
  public Future<Response> apply(Request request) {
    if (request.getUri().equals("/")) {
      return backendClient.apply(Request.apply("/api"));
    }
    Response response = Response.apply();
    response.setStatusCode(404);
    return Future.value(response);
  }

  public static void main(String[] args) {
    // The frontend makes a sampling decision (via Trace.letTracerAndId) and propagates it downstream.
    // This property says sample 100% of traces.
    System.setProperty("zipkin.initialSampleRate", "1.0");
    // All servers need to point to the same zipkin transport
    System.setProperty("zipkin.kafka.bootstrapServers", "192.168.99.100:9092");

    // It is unreliable to rely on implicit tracer config (Ex sometimes NullTracer is used).
    // Always set the tracer explicitly. The default constructor reads from system properties.
    SamplingTracer tracer = new KafkaZipkinTracer();

    Service<Request, Response> backendClient =
        ClientBuilder.safeBuild(ClientBuilder.get()
            .tracer(tracer)
            .codec(Http.get().enableTracing(true))
            .hosts("localhost:9000")
            .hostConnectionLimit(1));

    ServerBuilder.safeBuild(
        new Frontend(backendClient),
        ServerBuilder.get()
            .tracer(tracer)
            .codec(Http.get().enableTracing(true))
            .bindTo(new InetSocketAddress(InetAddress.getLoopbackAddress(), 8081))
            .name("frontend"));
  }
}
