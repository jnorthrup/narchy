package nars.web;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.cache.DirectBufferCache;
import io.undertow.server.handlers.resource.CachingResourceManager;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.extensions.PerMessageDeflateHandshake;
import javassist.scopedpool.SoftValueHashMap;
import nars.Memory;
import nars.NAR;
import nars.NARLoop;
import nars.bag.BLink;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.concept.DefaultConceptBuilder;
import nars.nar.Default;
import nars.op.mental.Abbreviation;
import nars.op.mental.Anticipate;
import nars.op.mental.Inperience;
import nars.term.Term;
import nars.term.Termed;
import nars.term.index.MapIndex2;
import nars.time.RealtimeMSClock;
import nars.util.data.random.XORShiftRandom;
import ognl.OgnlException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import static io.undertow.Handlers.*;
import static java.util.zip.Deflater.BEST_SPEED;


public class WebServer /*extends PathHandler*/ {


    public final NAR nar;

    public final Undertow server;
    public NARLoop loop;


    final static Logger logger = LoggerFactory.getLogger(WebServer.class);


    public static HttpHandler socket(WebSocketConnectionCallback w) {
        return websocket(w).addExtension(new PerMessageDeflateHandshake(false, BEST_SPEED));
    }


    @SuppressWarnings("HardcodedFileSeparator")
    public WebServer(NAR nar, float initialFPS, int httpPort) throws OgnlException {



        //TODO use ClassPathHandler and store the resources in the .jar

        File c = new File("./web/src/main/resources/");
        //File c = new File(WebServer.class.getResource("/").getPath());
        String cp = c.getAbsolutePath().replace("./", "");
        Path p = Paths.get(
                //System.getProperty("user.home")
                cp
        );//.toAbsolutePath();
        logger.info("Serving resources: {}", p);



        //https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/sessionhandling/SessionServer.java

        server = Undertow.builder()
                .addHttpListener(httpPort, "localhost")
                .setIoThreads(4)
                .setHandler(
                    path()
                        .addPrefixPath("/", resource(
                                new CachingResourceManager(
                                        16384,
                                        16*1024*1024,
                                        new DirectBufferCache(100, 10, 1000),
                                        new PathResourceManager(p, 0, true, true),
                                        0 //7 * 24 * 60 * 60 * 1000
                                )
                            )
                                .setDirectoryListingEnabled(true)
                                .addWelcomeFiles("index.html")
                        )
                        .addPrefixPath("/terminal", socket(new NarseseIOService(nar)))
                        .addPrefixPath("/emotion", socket(new EvalService(nar, "emotion", 500)))
                        .addPrefixPath("/active", socket(new TopConceptService<Object[]>(nar, 1200, 100) {

                            @Override
                            Object[] summarize(BLink<? extends Concept> bc, int n) {
                                Concept c = bc.get();
                                return new Object[] {
                                    escape(c), //ID
                                    b(bc.pri()), b(bc.dur()), b(bc.qua()),
                                    termLinks(c, (int)Math.ceil(((float)n/maxConcepts.intValue())*(maxTermLinks-minTermLinks)+minTermLinks) )
                                    //TODO tasklinks, beliefs
                                };
                            }

                            final int maxTermLinks = 5;
                            final int minTermLinks = 0;

                            private Object[] termLinks(Concept c, int num) {
                                Bag<Termed> b = c.termlinks();
                                Object[] tl = new Object[ Math.min(num, b.size() )];
                                final int[] n = {0};
                                b.forEach(num, t -> {
                                    tl[n[0]++] = new Object[] {
                                       escape(t.get()), //ID
                                       b(t.pri()), b(t.dur()), b(t.qua())
                                    };
                                });
                                return tl;
                            }

                            private int b(float budgetValue) {
                                return Math.round(budgetValue  * 1000);
                            }
                        }))
                )
                .build();


        this.nar = nar;
        this.loop = nar.loop(initialFPS);

        logger.info("HTTP+Websocket server starting: port={}", httpPort);
        server.start();

    }


    public void stop() {
        synchronized (server) {
            server.stop();

            try {
                loop.stop();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {


//                new Default(
//                new Memory(
//                        new RealtimeMSClock(),
//                        new XorShift1024StarRandom(1),
//                        GuavaCacheBag.make(
//                            1024*1024
//                        )),
//                1024,
//                1, 2, 3
//        );

        //nar.forEachConcept(c -> System.out.println(c));

        int httpPort = args.length < 1 ? 8080 : Integer.parseInt(args[0]);

        NAR nar = newRealtimeNAR();

        nar.input("a:b. b:c. c:d! ");


        new WebServer(nar, 20, httpPort);

        /*if (nlp!=null) {
            System.out.println("  NLP enabled, using: " + nlpHost + ":" + nlpPort);
        }*/

    }


    /**
     * Sends <var>text</var> to all currently connected WebSocket clients.
     *
     * @param text The String to send across the network.
     * @throws InterruptedException When socket related I/O errors occur.
     */
    /*public void sendToAll(String text) {
        Collection<WebSocket> con = connections();
        synchronized (con) {
            for (WebSocket c : con) {
                c.send(text);
            }
        }
    }*/
    @NotNull
    public static Default newRealtimeNAR() {
        Memory mem = new Memory(new RealtimeMSClock(),
                new MapIndex2(
                        new SoftValueHashMap(256*1024), new DefaultConceptBuilder(new XORShiftRandom(), 32, 32)
                )
                //new MapCacheBag(
                //new WeakValueHashMap<>()

                //GuavaCacheBag.make(1024*1024)
                /*new InfiniCacheBag(
                    InfiniPeer.tmp().getCache()
                )*/
                //)
        );

        int numConceptsPerCycle = 1;
        Default nar = new Default(1024, numConceptsPerCycle, 3, 3) {
            @Override
            public Function<Term, Concept> newConceptBuilder() {
                return new DefaultConceptBuilder(random, 32, 128);
            }
        };

        nar.conceptActivation.setValue(1f/numConceptsPerCycle);
        nar.cyclesPerFrame.set(20);

        //nar.log();
        nar.with(
                Anticipate.class,
                Inperience.class
        );
        nar.with(new Abbreviation(nar,"is"));
        nar.conceptRemembering.setValue(1000 * 1);
        nar.termLinkRemembering.setValue(1000 * 10);
        nar.taskLinkRemembering.setValue(1000 * 5);
        return nar;
    }

}
