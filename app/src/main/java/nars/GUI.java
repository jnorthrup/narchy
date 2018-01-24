package nars;

import jcog.User;
import jcog.exe.Loop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.SpaceGraph;
import spacegraph.widget.console.TextEdit;
import spacegraph.widget.meta.AutoSurface;
import spacegraph.widget.windo.Widget;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static spacegraph.SpaceGraph.window;

/**
 * main UI entry point
 */
public class GUI {

    static final Logger logger = LoggerFactory.getLogger(GUI.class);

    public static void main(String[] args) {

        NAR nar = NARchy.ui();


        Loop loop = nar.startFPS(10f); //10hz alpha
        ((NARLoop) loop).throttle.set(0.1f);

        window(new AutoSurface<>(nar), 700, 600);

        window(new OmniBox(), 600, 200);

        try {

            //1. try to open a Spacegraph openGL window
            logger.info("Starting Spacegraph UI");

            //            window(new ConsoleTerminal(new TextUI(nar).session(8f)) {
            //                {
            //                    Util.pause(50); term.addInput(KeyStroke.fromString("<pageup>")); //HACK trigger redraw
            //                }
            //            }, 800, 600);


        } catch (Throwable t) {
            //2. if that fails:
            logger.info("Fallback to Terminal UI");
            new Shell(nar);
        }


    }

    /**
     * super repl
     */
    public static class OmniBox extends Widget {


        final TextEdit edit;
        private final User user;

        public OmniBox() {
            this(User.the());
        }

        public OmniBox(User u) {
            super();

            this.user = u;


            children((edit = new TextEdit() {

                @Override
                protected void onKeyCtrlEnter() {
                    String t = text();
                    in(t);
                    clear();
                }

                @Override
                protected void textChange(String next) {
                    if (next.isEmpty()) return;

                    Query q = query.get();
                    if (q == null || !q.q.equals(next)) {
                        synchronized (query) {

                            Query qq;
                            if (query.compareAndSet(q, qq = new Query(next))) {
                                if (q != null) q.kontinue = false;
                                qq.start();
                            }
                        }
                    }
                }

            }).surface().scale(2));
        }

        final class Query implements Predicate<User.DocObj>, Runnable {

            public final String q;

            public volatile boolean kontinue = true;

            Query(String text) {
                this.q = text;
            }

            public Query start() {
                if (kontinue) {
                    System.out.println("query start: " + q);
                    user.run(this);
                }
                return this;
            }

            @Override
            public boolean test(User.DocObj docObj) {
                System.out.println(q + ": " + docObj);
                return kontinue;
            }

            @Override
            public void run() {
                if (kontinue) {
                    user.getAll(q, this);
                    query.compareAndSet(this, null);
                }
            }
        }

        private volatile AtomicReference<Query> query = new AtomicReference<>(null);

        protected void in(String s) {
            user.notice.emit("omnibox: " + s);
        }

        public static void main(String[] args) {
            SpaceGraph.window(new OmniBox(), 800, 250);
        }
    }

}
