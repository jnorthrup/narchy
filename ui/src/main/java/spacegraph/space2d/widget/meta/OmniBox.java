package spacegraph.space2d.widget.meta;

import jcog.User;
import jcog.list.FasterList;
import org.apache.lucene.document.Document;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.console.TextEdit;
import spacegraph.space2d.widget.windo.Widget;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * super repl
 */
public class OmniBox extends Widget {

    final TextEdit edit;

    private final User user;
    private final Gridding results;
    private final Splitting divider;

    private final AtomicReference<Querying> query = new AtomicReference<>(null);

    public OmniBox() {
        this(User.the());
    }

    public OmniBox(User u) {
        super();

        this.user = u;
        edit = new TextEdit() {

            @Override
            protected void onKeyCtrlEnter() {
                String t = text();
                in(t);
                clear();
            }

            @Override
            protected void textChange(String next) {
                Querying prev = null;
                if (next.isEmpty()) {
                    prev = query.getAndSet(null);
                } else {

                    Querying q = query.get();
                    if (q == null || !q.q.equals(next)) {
                        Querying qq = new Querying(next);
                        prev = query.getAndSet(qq);
                        qq.start();
                    }
                }
                if (prev!=null)
                    prev.clear();
            }

        };
        divider = new Splitting();
        results = new Gridding() {
            @Override
            public void doLayout(int dtMS) {
                if (size() == 0) {
                    divider.split(0);
                } else {
                    divider.split(0.5f);
                    //TODO stretch window?
                }
                super.doLayout(dtMS);
            }
        };

        content(divider.split(edit.surface().scale(2), results, 0));
    }

    class Result {
        public final String id;
        public final String type;
        final Document doc;
        //icon

        Result(Document doc) {
            this.doc = doc;
            this.id = doc.get("i");
            switch (this.type = doc.get("c")) {
                case "blob":
                    //
                    break;
            }
//            System.out.println(id);
//            d.forEach(f -> {
//                System.out.println(f.name() + " " + f.fieldType());
//            });
        }

        Object get() {
            return user.undocument(doc);
        }

    }

    final class Querying implements Predicate<User.DocObj>, Runnable {


        public final String q;
        final List<Result> results = new FasterList();

        Querying(String text) {
            this.q = text;
        }

        public Querying start() {
            if (query.get() == this) {
                //System.out.println("query start: " + q);
                user.run(this);
            }
            return this;
        }

        @Override
        public boolean test(User.DocObj docObj) {
            //System.out.println(q + ": " + docObj);
            if (query.get()!=this)
                return false;
            else {
                Document d = docObj.doc();
                Result r = new Result(d);
                Surface s = result(r);
                if (query.get()==this) {
                    results.add(r);
                    OmniBox.this.results.add(s);
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

            OmniBox.this.results.clear();
            user.get(q, 16, this);
        }



        private Surface result(Result r) {
            return new PushButton(r.id);
        }

        void clear() {
            results.clear();
        }
    }


    protected void in(String s) {
        user.notice.emit("omnibox: " + s);
    }

    public static void main(String[] args) {
        SpaceGraph.window(new OmniBox(), 800, 250);
    }
}
