package finagle;

import com.twitter.app.Flags;
import com.twitter.finagle.Http;
import com.twitter.finagle.ListeningServer;
import com.twitter.finagle.Service;
import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.finagle.tracing.traceId128Bit$;
import com.twitter.finagle.zipkin.core.SamplingTracer;
import com.twitter.util.Await;
import com.twitter.util.Future;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import zipkin2.finagle.http.HttpZipkinTracer;

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

  public static void main(String[] args) throws Exception{
    if (args == null || args.length == 0) {
      args = new String[] {
          // The frontend makes a sampling decision (via Trace.letTracerAndId) and propagates it downstream.
          // This property says sample 100% of traces.
          "-zipkin.initialSampleRate", "1.0",
          // All servers need to point to the same zipkin transport (note this is default)
          "-zipkin.http.host", "localhost:9411",
          // Originate 128-bit trace IDs
          "-com.twitter.finagle.tracing.traceId128Bit", "true"
      };
    }

    // parse any commandline arguments
    new Flags("frontend", true, true).parseOrExit1(args, false);

    System.out.println(traceId128Bit$.MODULE$.get());

    // It is unreliable to rely on implicit tracer config (Ex sometimes NullTracer is used).
    // Always set the tracer explicitly. The default constructor reads from system properties.
    SamplingTracer tracer = new HttpZipkinTracer();

    Service<Request, Response> backendClient = Http.client()
        .withTracer(tracer)
        .withLabel("frontend") // this assigns the local service name
        .newService("localhost:9000");

    ListeningServer server = Http.server()
        .withTracer(tracer)
        .withLabel("frontend") // this assigns the local service name
        .serve(
            new InetSocketAddress(InetAddress.getLoopbackAddress(), 8081),
            new Frontend(backendClient)
        );

    Await.ready(server);
  }
}
