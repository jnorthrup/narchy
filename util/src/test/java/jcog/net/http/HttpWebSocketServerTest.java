package jcog.net.http;

import jcog.Util;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import sun.net.www.content.text.PlainTextInputStream;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpWebSocketServerTest {

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        HttpServer server = new HttpServer("localhost", 8080, new File("/tmp"), new HttpModel() {

            @Override
            public void wssOpen(WebSocket ws, ClientHandshake handshake) {
                ws.send("hi");
            }

            @Override
            public void response(HttpConnection h) {
                h.respond("");
            }
        });


        server.runFPS(20f);

        //test http client connect
        URL u = new URL("http://localhost:8080/");
        URLConnection urlConnection = u.openConnection();
        PlainTextInputStream content = (PlainTextInputStream) urlConnection.getContent();

        String x = new String(content.readAllBytes());
        assertEquals("", x); //empty string, default server content handler for now
        //------------------

        //test websocket client connect
        WebSocketTest.Client c = new WebSocketTest.Client();

        c.connectBlocking();

        Util.sleep(500);

        assertTrue(c.isOpen());

        c.send("abc");

        Util.sleep(500);

        c.closeBlocking();
        //-----------


        server.stop();


    }
}