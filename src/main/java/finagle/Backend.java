package finagle;

import com.twitter.app.Flags;
import com.twitter.finagle.Http;
import com.twitter.finagle.ListeningServer;
import com.twitter.finagle.Service;
import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.finagle.zipkin.core.SamplingTracer;
import com.twitter.util.Await;
import com.twitter.util.Future;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Date;
import zipkin.finagle.http.HttpZipkinTracer;

public class Backend extends Service<Request, Response> {

  @Override
  public Future<Response> apply(Request request) {
    Response response = Response.apply();
    if (request.path().equals("/api")) {
      response.write(new Date().toString());
    } else {
      response.setStatusCode(404);
    }
    return Future.value(response);
  }

  public static void main(String[] args) throws Exception {
    if (args == null || args.length == 0) {
      args = new String[] {
          // All servers need to point to the same zipkin transport (note this is default)
          "-zipkin.http.host", "localhost:9411"
      };
    }

    // parse any commandline arguments
    new Flags("backend", true, true).parseOrExit1(args, false);

    // It is unreliable to rely on implicit tracer config (Ex sometimes NullTracer is used).
    // Always set the tracer explicitly. The default constructor reads from system properties.
    SamplingTracer tracer = new HttpZipkinTracer();

    ListeningServer server = Http.server()
        .withTracer(tracer)
        .withLabel("backend") // this assigns the local service name
        .serve(new InetSocketAddress(InetAddress.getLoopbackAddress(), 9000), new Backend());

    Await.ready(server);
  }
}
