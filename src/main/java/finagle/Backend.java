package finagle;

import com.twitter.finagle.Service;
import com.twitter.finagle.builder.ServerBuilder;
import com.twitter.finagle.http.Http;
import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.util.Future;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Date;

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

  public static void main(String[] args) {
    // All servers need to point to the same zipkin transport
    System.setProperty("zipkin.kafka.bootstrapServers", "192.168.99.100:9092");
    ServerBuilder.safeBuild(
        new Backend(),
        ServerBuilder.get()
            .codec(Http.get().enableTracing(true))
            .bindTo(new InetSocketAddress(InetAddress.getLoopbackAddress(), 9000))
            .name("backend"));
  }
}
