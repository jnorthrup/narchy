package spacegraph.space2d;

import com.fasterxml.jackson.core.JsonProcessingException;
import jcog.Util;
import jcog.exe.Loop;
import jcog.net.UDPeer;
import jcog.util.Grok;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;

/** stand-alone local/remote log consumer and visualization */
public class SpaceLog {

    /** aux logger, for console or another downstream target */
    final Logger logger;

    final UDPeer udp;

    final Grok grok = Grok.all();

    public SpaceLog() throws IOException {
        this(0);
    }

    public SpaceLog(int port) throws IOException {
        this.udp = new UDPeer(port);
        this.udp.receive.on(this::receive);
        this.udp.runFPS(10f);

        logger = LoggerFactory.getLogger(SpaceLog.class.getSimpleName() + "@" + udp.name());

    }

    protected void receive(UDPeer.MsgReceived m) {
        //1. try default json/msgpack decode:
        byte[] data = m.data();
        try {
            Object x = Util.fromBytes(data, Object.class);
            //JsonNode x = Util.msgPackMapper.readTree(data);
            logger.info("recv: {}\n\t{}", m.from, x);
        } catch (IOException e) {
            //try to interpret it via UTF-8 String
            String s = new String(data);
            Grok.Match ms = grok.capture(s);
            if (!ms.isNull()) {
                logger.info("recv: {}\n{}", m.from, ms.toMap());
            }
        }
    }

    protected void gui() {
//
//        Timeline2D.SimpleTimelineModel dummyModel = new Timeline2D.SimpleTimelineModel();
//        dummyModel.add(new Timeline2D.SimpleEvent("x", 0, 1));
//        dummyModel.add(new Timeline2D.SimpleEvent("y", 1, 3));
//        dummyModel.add(new Timeline2D.SimpleEvent("z", 2, 5));
//        dummyModel.add(new Timeline2D.SimpleEvent("w", 3, 3)); //point
//
//        SpaceGraph.window(new Timeline2D<>(dummyModel, e->new PushButton(e.name)){
//            @Override
//            protected void paintBelow(GL2 gl) {
//                gl.glColor3f(0, 0, 0.1f);
//                Draw.rect(gl, bounds);
//            }
//        }.view(0, 5), 800, 600);

    }

    public static void main(String[] args) throws IOException {
        SpaceLog s = new SpaceLog();

        Loop.of(new DummyLogGenerator(new UDPeer())).runFPS(0.75f);
    }


    private static class DummyLogGenerator implements Runnable {

        private final UDPeer out;

        public DummyLogGenerator(UDPeer udPeer) {
            this.out = udPeer;
            out.runFPS(10f);
        }

        @Override
        public void run() {
            //echo -n "hello" >/dev/udp/localhost/44416

            try {
                out.tellSome("my time is " + new Date(), 3, false);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }
}
