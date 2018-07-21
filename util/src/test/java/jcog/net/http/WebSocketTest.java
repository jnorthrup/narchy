package jcog.net.http;

import jcog.Util;
import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.handshake.ServerHandshakeBuilder;
import org.java_websocket.server.WebSocketServer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("slow") class WebSocketTest {

    /**
     * A simple WebSocketServer implementation. Keeps track of a "chatroom".
     */
    class Server extends WebSocketServer {

        Server(int port) throws UnknownHostException {
            super( new InetSocketAddress( port ), List.of(new Draft_6455()) );
            WebSocketImpl.DEBUG = true;


        }

        @Override
        public ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(WebSocket conn, Draft draft, ClientHandshake request) throws InvalidDataException {
            ServerHandshakeBuilder builder = super.onWebsocketHandshakeReceivedAsServer( conn, draft, request );
            builder.put( "Access-Control-Allow-Origin" , "*");
            return builder;
        }

        @Override
        public void onOpen( WebSocket conn, ClientHandshake handshake ) {
            conn.send("Welcome to the server!"); 
            broadcast( "new connection: " + handshake.getResourceDescriptor() ); 
            System.out.println( conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!" );
        }

        @Override
        public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
            broadcast( conn + " has left the room!" );
            System.out.println( conn + " has left the room!" );
        }

        @Override
        public void onMessage( WebSocket conn, String message ) {
            broadcast( message );
            System.out.println( conn + ": " + message );
        }
        @Override
        public void onMessage( WebSocket conn, ByteBuffer message ) {
            broadcast( message.array() );
            System.out.println( conn + ": " + message );
        }



        @Override
        public void onError(WebSocket conn, Exception ex ) {
            ex.printStackTrace();
            if( conn != null ) {
                
            }
        }

        @Override
        public void onStart() {
            System.out.println("Server started!");
        }

    }

    static class Client extends WebSocketClient {


        public Client() throws URISyntaxException {
            super( new URI( "ws://localhost:8080"), new Draft_6455() );

        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {

        }

        @Override
        public void onMessage(String message) {
            System.out.println(this + " recv " + message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {

        }

        @Override
        public void onError(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    void test1() throws IOException, URISyntaxException, InterruptedException {
        Server s = new Server(8080);
        s.start();


        Util.sleepMS(1000);

        Client c = new Client();
        c.connect();

        Util.sleepMS(100);

        assertTrue(c.isOpen());

        c.send("abc");
        s.broadcast( "a" );

        Util.sleepMS(1000);

        s.stop();
    }
}
