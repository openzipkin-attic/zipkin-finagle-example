package finagle;

import com.twitter.app.Flags;
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
import zipkin.finagle.http.HttpZipkinTracer;

public class Frontend extends Service<Request, Response> {

  final Service<Request, Response> backendClient;

  Frontend(Service<Request, Response> backendClient) {
    this.backendClient = backendClient;
  }

  @Override
  public Future<Response> apply(Request request) {
    if (request.uri().equals("/")) {
      return backendClient.apply(Request.apply("/api"));
    }
    Response response = Response.apply();
    response.setStatusCode(404);
    return Future.value(response);
  }

  public static void main(String[] args) {
    if (args == null || args.length == 0) {
      args = new String[] {
          // The frontend makes a sampling decision (via Trace.letTracerAndId) and propagates it downstream.
          // This property says sample 100% of traces.
          "-zipkin.initialSampleRate", "1.0",
          // All servers need to point to the same zipkin transport (note this is default)
          "-zipkin.http.host", "localhost:9411"
      };
    }

    // parse any commandline arguments
    new Flags("frontend", true, true).parseOrExit1(args, false);

    // It is unreliable to rely on implicit tracer config (Ex sometimes NullTracer is used).
    // Always set the tracer explicitly. The default constructor reads from system properties.
    SamplingTracer tracer = new HttpZipkinTracer();

    Service<Request, Response> backendClient =
        ClientBuilder.safeBuild(ClientBuilder.get()
            .tracer(tracer)
            .codec(Http.get().enableTracing(true))
            .hosts("localhost:9000")
            .hostConnectionLimit(1)
            .name("frontend")); // this assigns the local service name

    ServerBuilder.safeBuild(
        new Frontend(backendClient),
        ServerBuilder.get()
            .tracer(tracer)
            .codec(Http.get().enableTracing(true))
            .bindTo(new InetSocketAddress(InetAddress.getLoopbackAddress(), 8081))
            .name("frontend")); // this assigns the local service name
  }
}
