package spacegraph;

import jcog.Texts;
import jcog.net.http.HttpModel;
import jcog.net.http.HttpServer;
import spacegraph.web.util.ClientBuilder;

import java.io.IOException;

public class SpacegraphWeb {

    final static int DEFAULT_PORT = 8080;

    public static void main(String[] args) throws IOException {

        ClientBuilder.rebuildAsync(WebClient.class, false);

        int port;
        if (args.length > 0) {
            port = Texts.i(args[0]);
        } else {
            port = DEFAULT_PORT;
        }

        jcog.net.http.HttpServer h = new HttpServer(port, new WebServer() {

        });
        h.setFPS(10f);

    }
}
