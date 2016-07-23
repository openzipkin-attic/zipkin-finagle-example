package finagle;

import com.twitter.finagle.Service;
import com.twitter.finagle.builder.ClientBuilder;
import com.twitter.finagle.builder.ServerBuilder;
import com.twitter.finagle.http.Http;
import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.util.Future;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Frontend extends Service<Request, Response> {

  @Override
  public Future<Response> apply(Request request) {
    switch (request.getUri()) {
      case "/":
        return client.apply(Request.apply("/api"));
      default:
        Response response = Response.apply();
        response.setStatusCode(404);
        return Future.value(response);
    }
  }

  Service<Request, Response> client =
      ClientBuilder.safeBuild(ClientBuilder.get()
          .codec(Http.get().enableTracing(true))
          .hosts("localhost:9000")
          .hostConnectionLimit(1));

  public static void main(String[] args) {
    // The frontend makes a tracing decision. This property says sample 100% of traces.
    System.setProperty("zipkin.initialSampleRate", "1.0");
    // All servers need to point to the same zipkin transport
    System.setProperty("zipkin.kafka.bootstrapServers", "192.168.99.100:9092");
    ServerBuilder.safeBuild(
        new Frontend(),
        ServerBuilder.get()
            .codec(Http.get().enableTracing(true))
            .bindTo(new InetSocketAddress(InetAddress.getLoopbackAddress(), 8081))
            .name("frontend"));
  }
}
