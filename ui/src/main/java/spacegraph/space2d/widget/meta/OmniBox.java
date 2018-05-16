package spacegraph.space2d.widget.meta;

import com.googlecode.lanterna.TerminalPosition;
import jcog.exe.Loop;
import jdk.jshell.JShell;
import jdk.jshell.SourceCodeAnalysis;
import jdk.jshell.execution.LocalExecutionControlProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.MutableContainer;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.console.TextEdit;
import spacegraph.space2d.widget.text.Label;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.space2d.widget.windo.Widget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * super repl
 */
public class OmniBox extends Widget {

    final TextEdit edit;


    private final Gridding results;
    private final Gridding content;

    private final Model model;

    @Deprecated
    public OmniBox() {
        this(new JShellModel());
    }


    public OmniBox(Model m) {
        super();

        this.model = m;

        content = new Gridding();

        results = new Gridding() {
            @Override
            public void add(Surface s) {
                synchronized (content) {
                    int sizeBefore = size();

                    super.add(s);

                    if (size() > 0 && sizeBefore == 0) {
                        content.add(this);
                    }
                }
            }

            @Override
            public void clear() {
                super.clear();
                synchronized (content) {
                    content.remove(this);
                }
            }

        };
//        {
//            @Override
//            public void doLayout(int dtMS) {
//                if (size() == 0) {
//                    divider.split(0);
//                } else {
//                    divider.split(0.5f);
//                    //TODO stretch window?
//                }
//                super.doLayout(dtMS);
//            }
//        };
        edit = new TextEdit() {

            @Override
            protected void onKeyCtrlEnter() {
                String t = text();
                model.onTextChangeControlEnter(t, results);
                clear();
            }

            @Override
            protected void cursorChange(String next, TerminalPosition cp) {
                if (cp.getRow() == 0) {
                    model.onTextChange(next, cp.getColumn(), results);
                } else {
                    results.clear();
                }
            }

            @Override
            protected void textChange(String next) {
                TerminalPosition cp = getCursorPosition();
                cursorChange(next, cp);
            }

        };

        content.add(edit.surface().scale(2));
        set(content);

        //content(divider.split(edit.surface().scale(2), results, 0));
    }

    abstract public static class Model {
        abstract public void onTextChange(String text, int cursorPos, MutableContainer target);

        public void onTextChangeControlEnter(String t, MutableContainer target) {
            /** default nothing */
        }
    }

    public static class JShellModel extends Model {

        static final Logger logger = LoggerFactory.getLogger(JShellModel.class);

        private final JShell js;
        private final SourceCodeAnalysis jsAnalyze;

        private transient volatile String currentText = "";
        private transient volatile int currentPos = 0;

        public JShellModel() {
            JShell.Builder builder = JShell.builder();
            Map<String, String> params = new HashMap<>();
            builder.executionEngine(new LocalExecutionControlProvider(), params);

            js = builder.build();

            jsAnalyze = js.sourceCodeAnalysis();

            //js.eval("import spacegraph.space2d.widget.meta.OmniBox.*;");
            //js.eval("void popup(Object x) { " + OmniBox.class.getName() + ".popup(x); }");
        }

        @Override
        public void onTextChange(String text, int cursorPos, MutableContainer target) {

            currentText = text;
            currentPos = cursorPos;

            if (text.isEmpty())
                return; //though it works , temporary to avoid it clearing after ctrl-enter

            Loop.invokeLater(() -> {
                List<SourceCodeAnalysis.Suggestion> sugg = jsAnalyze.completionSuggestions(text,
                        cursorPos + 1 /* TODO take actual cursor pos */,
                        new int[1]);

                if (cursorPos!=currentPos || !text.equals(currentText))
                    return; //text or position changed while this was processing; discard these results

                target.clear();
                sugg.stream().map(SourceCodeAnalysis.Suggestion::continuation).sorted().map(x -> new PushButton(x)).forEach(target::add);
            });
        }

        @Override
        public void onTextChangeControlEnter(String _text, MutableContainer target) {
            String text = _text.trim();
            if (text.isEmpty())
                return;

            target.clear();

            //t = "popup(" + t + ");";
            //String nextVar = "next";
            //"Object " + nextVar + " = " + t + ";"
            String cmd = OmniBox.class.getName() +
                    ".popup(\"" + text + "\"," + text + ");";

            js.eval(cmd).forEach(e -> {
                logger.info("{}:\n\t{}", text, e);
//                Snippet es = e.snippet();
//                if (es instanceof VarSnippet) { //!(es instanceof StatementSnippet) && !(es instanceof ErroneousSnippet)) {
////                    //Snippet:VariableKey($2)#3-2+2
////                    //HACK extract variable from toString() WTF
////                    String ses = es.toString();
////                    if (ses.startsWith("Snippet:VariableKey(")) {
////                        String var = ses.substring("Snippet:VariableKey(".length(), ses.indexOf(')'));
////
////                        js.eval(OmniBox.class.getName() + ".popup(" + var + ")");
////
////                    }
////                    js.eval(OmniBox.class.getName() + ".popup(next)");
//
//                }
//                Snippet es = e.snippet();
//                if (es instanceof VarSnippet) {
//                    js.eval(OmniBox.class.getName() + ".popup(x);");
//                }
//                Object v = e.value();
//                String msg = e + " " + e.causeSnippet() + " " + e.exception();
//                System.out.println(v + "\n\t" + msg);
//                //target.add(new Label(msg));
//                SpaceGraph.window(new Label(msg), 400, 400);
            });
        }

    }


    public static void popup(String src, Object x) {
        Surface surface;
        if (x instanceof String || x.getClass().isPrimitive() || x instanceof Number) {
            surface = new Label(x.toString());
        } else {
            surface = new AutoSurface<>(x);
        }

        SpaceGraph.window(new LabeledPane(src, surface), 800, 800, true);
    }


    /*protected void in(String s) {
        user.notice.emit("omnibox: " + s);
    }*/

}
