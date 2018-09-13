package nars;

import jcog.User;
import jcog.data.list.FasterList;
import nars.gui.NARui;
import org.apache.lucene.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.collection.MutableListContainer;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.OmniBox;
import spacegraph.space2d.widget.meta.ServicesTable;
import spacegraph.space2d.widget.windo.WiredWall;

import java.util.List;
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


        logger.info("start SpaceGraph UI");


        window(NARui.top(nar), 1024, 800).eventClosed.on(() -> {
            nar.reset();
        });

        nar.startFPS(10f);

//            window(new Gridding(
//                    List.of("")
//            ), 500, 500);


    }

    static void wall(NAR nar) {
        WiredWall w = SpaceGraph.wall(800, 600);
        w.frame(new ServicesTable(nar.services), 5, 4);
        w.frame(new OmniBox(new LuceneQueryModel()), 6, 1);
        w.frame(NARui.top(nar), 4, 4);
    }

    /**
     * TODO further abstract this as the prototype for other async models
     */
    static class LuceneQueryModel extends OmniBox.Model {

        private final User user;

        public LuceneQueryModel() {
            this(User.the());
        }

        public LuceneQueryModel(User u) {
            super();
            this.user = u;
        }

        private final AtomicReference<Querying> query = new AtomicReference<>(null);

        final class Querying implements Predicate<User.DocObj>, Runnable {


            public final String q;
            final List<Result> results = new FasterList();
            private final MutableListContainer target;

            Querying(String text, MutableListContainer target) {
                this.q = text;
                this.target = target;
            }

            public Querying start() {
                if (query.get() == this) {

                    user.run(this);
                }
                return this;
            }

            @Override
            public boolean test(User.DocObj docObj) {

                if (query.get() != this)
                    return false;
                else {
                    Document d = docObj.doc();
                    Result r = new Result(d);
                    Surface s = result(r);
                    if (query.get() == this) {
                        results.add(r);
                        target.add(s);
                        return true;
                    } else {
                        return false;
                    }
                }
            }

            @Override
            public void run() {
                if (query.get() != this)
                    return;

                target.clear();
                user.get(q, 16, this);
            }


            private Surface result(Result r) {
                return new PushButton(r.id);
            }

            void clear() {
                results.clear();
            }
        }

        class Result {
            public final String id;
            public final String type;
            final Document doc;


            Result(Document doc) {
                this.doc = doc;
                this.id = doc.get("i");
                switch (this.type = doc.get("c")) {
                    case "blob":

                        break;
                }


            }

            Object get() {
                return user.undocument(doc);
            }

        }

        @Override
        public void onTextChange(String next, int cursorPos, MutableListContainer target) {
            Querying prev = null;
            if (next.isEmpty()) {
                prev = query.getAndSet(null);
            } else {

                Querying q = query.get();
                if (q == null || !q.q.equals(next)) {
                    Querying qq = new Querying(next, target);
                    prev = query.getAndSet(qq);
                    qq.start();
                }
            }
            if (prev != null)
                prev.clear();

        }
    }
}
