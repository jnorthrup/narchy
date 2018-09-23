//package jcog.net.http;
//
//import jcog.Util;
//import jcog.data.list.FasterList;
//import jcog.data.map.CustomConcurrentHashMap;
//import jcog.exe.Exe;
//import org.graalvm.polyglot.Context;
//import org.graalvm.polyglot.Value;
//import org.java_websocket.WebSocket;
//import org.java_websocket.framing.CloseFrame;
//import org.java_websocket.handshake.ClientHandshake;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.script.ScriptException;
//import java.lang.ref.Reference;
//import java.lang.reflect.Method;
//import java.lang.reflect.Parameter;
//import java.util.Collections;
//import java.util.List;
//import java.util.Queue;
//import java.util.concurrent.ArrayBlockingQueue;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.function.Function;
//import java.util.function.Supplier;
//
//import static jcog.data.map.CustomConcurrentHashMap.*;
//
//
///**
// * exposes an interface thru javascript engine to HTTP/WebSockets
// * --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED
// */
//public class JSSocket<X> implements HttpModel {
//
//    private static final Context.Builder JS_BUILDER = Context.newBuilder().allowNativeAccess(true);
//
//
////    private static final Logger logger = LoggerFactory.getLogger(JSSocket.class);
//
//    private final CustomConcurrentHashMap<WebSocket, JsSession<X>> session =
//            new CustomConcurrentHashMap<>(
//                    WEAK, EQUALS, STRONG, IDENTITY,
//                    1024
//            );
//
//    private final Supplier<X> target;
//
//    /**
//     * swagger-like API guide for the interface
//     */
//    private final String manual;
//
//    public JSSocket(Supplier<X> target) {
//        this.target = target;
//        this.manual = manual(target.get()).toString();
//    }
//
//    private static <X> StringBuilder manual(X x) {
//        List<Method> m = new FasterList();
//        Collections.addAll(m, x.getClass().getMethods());
//
//        StringBuilder s = new StringBuilder(1024);
//        for (Method y : m) {
//            if (y.getDeclaringClass() == Object.class)
//                continue;
//
//            s.append(y.getName()).append(" (");
//            Parameter[] parameters = y.getParameters();
//            for (int i = 0, parametersLength = parameters.length; i < parametersLength; i++) {
//                Parameter p = parameters[i];
//                s.append(p.getType()).append(' ').append(p.getName());
//                if (i < parametersLength - 1)
//                    s.append(", ");
//            }
//            s.append(")\n");
//
//        }
//
//        return s;
//    }
//
//    private static Object eval(String code,  Context engine) {
//        Object o;
//
//        try {
////            if (bindings == null)
////                o = engine.eval(code);
////            else
////                o = engine.eval(code, bindings);
//            Value v = engine.eval("js", code);
//            //System.out.println(v);
//            o = v.asHostObject();
//
//
//        } catch (Throwable t) {
//            o = t;
//        }
//
//
//
//        return o;
//    }
//
//    @Override
//    public void response(HttpConnection h) {
//        h.respond(manual);
//    }
//
//    @Override
//    public void wssOpen(WebSocket ws, ClientHandshake handshake) {
//        onOpened(session.computeIfAbsent(ws, (Function<WebSocket, JsSession<X>>) (ss) -> new JsSession<>(ss, target.get())));
//    }
//
//    @Override
//    public void wssClose(WebSocket ws, int code, String reason, boolean remote) {
//        onClosed(session.remove(ws));
//    }
//
//    @Override
//    public void wssMessage(WebSocket ws, String _message) {
//        String message = _message.trim();
//        if (message.isEmpty())
//            return;
//
//        session.get(ws).invoke(message);
//
//    }
//
//    private void onOpened(JsSession<X> session) {
//
//    }
//
//    private void onClosed(JsSession<X> session) {
//
//    }
//
//    static class JsSession<X> implements Runnable {
//
//        private final X context;
//        private final WebSocket socket;
//
//        final int MAX_QUEUE_SIZE = 64;
//
//        private final Context JS = JS_BUILDER.build();
//
//        final AtomicBoolean pending = new AtomicBoolean(false);
//        final Queue<String> q = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);
//        private final Value bindings;
//
//        JsSession(WebSocket s, X context) {
//            this.socket = s;
//            this.context = context;
//            this.bindings = JS.getBindings("js");
//            bindings.putMember("i", context);
//        }
//
//        void invoke(String expr) {
//            if (socket.isClosed())
//                return;
//
//            if (!q.offer(expr))
//                socket.close(CloseFrame.TOOBIG, "Overflow");
//
//            if (pending.compareAndSet(false, true)) {
//                Exe.invokeLater(this);
//            }
//        }
//
//        @Override
//        public void run() {
//
//            if (socket.isClosed())
//                return;
//
//            pending.set(false);
//
//            q.removeIf(message -> {
//                try {
//
//
//
//                    Object x = eval("i." + message, JS);
//                    if (x == null)
//                        return true;
//
//                    if (socket.isClosed())
//                        return true;
//
//                    if (x instanceof ScriptException)
//                        x = ((Throwable) x).getMessage();
//
//                    if (x instanceof String)
//                        socket.send((String) x);
//                    else {
//                        try {
//                            socket.send(Util.jsonNode(x).asText());
//                        } catch (Exception serialization) {
//                            socket.send(x.toString());
//                        }
//                    }
//
//                } catch (Exception e) {
//                    socket.send(e.getMessage());
//                }
//
//                return true;
//            });
//
//        }
//    }
//
//}
