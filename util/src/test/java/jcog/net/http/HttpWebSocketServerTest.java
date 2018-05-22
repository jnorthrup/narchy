package jcog.net.http;

import jcog.Util;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;

class HttpWebSocketServerTest {

    public static void main(String[] args) throws IOException {
        InetSocketAddress listenAddress = new InetSocketAddress("localhost", 8080);
        ServerSocketChannel ssChannel = HttpServer.openServerChannel(listenAddress);

// MyWebSocketListener should implement HttpWebSocketServerListener
        //HttpWebSocketServer server = new HttpWebSocketServer(ne wHttpW
        HttpServer server = new HttpServer(ssChannel, new File("/tmp"), new HttpWebSocketServerListener() {
            @Override
            public boolean wssConnect(SelectionKey key) {
                return false;
            }

            @Override
            public void wssOpen(WebSocket ws, ClientHandshake handshake) {

            }

            @Override
            public void wssClose(WebSocket ws, int code, String reason, boolean remote) {

            }

            @Override
            public void wssMessage(WebSocket ws, String message) {

            }

            @Override
            public void wssMessage(WebSocket ws, ByteBuffer message) {

            }

            @Override
            public void wssError(WebSocket ws, Exception ex) {

            }
        });

// Spawn threads


        server.runFPS(10f);

        Util.sleep(200000);

//        while (!Thread.interrupted())
//        {
//            // Accept new connections (non blocking)
//            // You could run this in your main loop, or as a seperate thread
//            server.loop();
//
//            // ArrayBlockingQueue.poll(1, TimeUnit.MILLISECONDS); would also work,
//            // for example when you have an event loop with worker threads.
//
//            // Use a simple sleep in this example
//            Thread.sleep(1);
//        }


        server.stop();


    }
}