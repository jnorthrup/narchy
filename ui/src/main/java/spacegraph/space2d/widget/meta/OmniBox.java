package spacegraph.space2d.widget.meta;

import jcog.User;
import jcog.data.list.FasterList;
import org.apache.lucene.document.Document;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.collection.MutableListContainer;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.space2d.widget.textedit.TextEdit;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * super repl
 */
public class OmniBox extends Widget {

    private final TextEdit edit;


    private final Gridding results;
    private final Gridding content;

    private final Model model;

//    @Deprecated
//    public OmniBox() {
//        this(new JShellModel());
//    }


    public OmniBox(Model m) {
        super();

        this.model = m;

        content = new Gridding();

        results = new Gridding() {
            @Override
            public void add(Surface... s) {
                synchronized (content) {
                    int sizeBefore = size();

                    super.add(s);

                    if (size() > 0 && sizeBefore == 0) {
                        content.add(this);
                    }
                }
            }

            @Override
            public Gridding clear() {
                super.clear();
                synchronized (content) {
                    content.detachChild(this);
                }
                return null;
            }

        };












        edit = new TextEdit(40, 1) {

//            @Override
//            public TextEdit onKey(Consumer<KeyEvent> e) {
//                return super.onKey(e);
//            }
//
//            @Override
//            protected void onKeyCtrlEnter() {
//                String t = text();
//                model.onTextChangeControlEnter(t, results);
//                clear();
//            }
//
//            @Override
//            protected void cursorChange(String next, TerminalPosition cp) {
//                if (cp.getRow() == 0) {
//                    model.onTextChange(next, cp.getColumn(), results);
//                } else {
//                    results.clear();
//                }
//            }
//
//            @Override
//            protected void textChange(String next) {
//                super.textChange(next);
//                TerminalPosition cp = getCursorPosition();
//                cursorChange(next, cp);
//            }

        };


//        TextEdit0 te = new TextEdit0(edit);
//        te.resize(40, 1);

        content.add(edit);
        set(content);

    }

    @FunctionalInterface public interface Model {
        void onTextChange(String text, int cursorPos, MutableListContainer target);

        default void onTextChangeControlEnter(String t, MutableListContainer target) {
            /** default nothing */
        }
    }

//    public static class JShellModel extends Model {
//
//        static final Logger logger = LoggerFactory.getLogger(JShellModel.class);
//
//        private final JShell js;
//        private final SourceCodeAnalysis jsAnalyze;
//
//        private transient volatile String currentText = "";
//        private transient volatile int currentPos = 0;
//
//        public JShellModel() {
//            JShell.Builder builder = JShell.builder();
//            Map<String, String> params = new HashMap<>();
//            builder.executionEngine(new LocalExecutionControlProvider(), params);
//
//            js = builder.build();
//
//            jsAnalyze = js.sourceCodeAnalysis();
//
//
//
//        }
//
//        @Override
//        public void onTextChange(String text, int cursorPos, MutableContainer target) {
//
//            currentText = text;
//            currentPos = cursorPos;
//
//            if (text.isEmpty())
//                return;
//
//            Exe.invokeLater(() -> {
//                if (cursorPos!=currentPos || !text.equals(currentText))
//                    return;
//
//                List<SourceCodeAnalysis.Suggestion> sugg = jsAnalyze.completionSuggestions(text,
//                        cursorPos /* TODO take actual cursor pos */,
//                        new int[1]);
//
//                if (cursorPos!=currentPos || !text.equals(currentText))
//                    return;
//
//                target.clear();
//                sugg.stream().map(SourceCodeAnalysis.Suggestion::continuation).sorted().map(PushButton::new).forEach(target::addAt);
//            });
//        }
//
//        @Override
//        public void onTextChangeControlEnter(String _text, MutableContainer target) {
//            String text = _text.trim();
//            if (text.isEmpty())
//                return;
//
//            target.clear();
//
//
//
//
//            String cmd = OmniBox.class.getName() +
//                    ".popup(" + Texts.quote(text) + "," + text + ");";
//
//            js.eval(cmd).forEach(e -> logger.info("{}:\n\t{}", text, e));
//        }
//
//    }


    public static void popup(String src, Object x) {
        Surface surface;
        if (x instanceof String || x.getClass().isPrimitive() || x instanceof Number) {
            surface = new VectorLabel(x.toString());
        } else {
            surface = new ObjectSurface(x);
        }

        SpaceGraph.window(LabeledPane.the(src, surface), 800, 800);
    }

    /**
     * TODO further abstract this as the prototype for other async models
     */
    static class LuceneQueryModel implements Model {

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

        static class Result {
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

//            Object get() {
//                return user.undocument(doc);
//            }

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


    /*protected void in(String s) {
        user.notice.emit("omnibox: " + s);
    }*/

}
