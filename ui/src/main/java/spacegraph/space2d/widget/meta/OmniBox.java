package spacegraph.space2d.widget.meta;

import jdk.jshell.JShell;
import jdk.jshell.SourceCodeAnalysis;
import spacegraph.SpaceGraph;
import spacegraph.space2d.container.MutableContainer;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.console.TextEdit;
import spacegraph.space2d.widget.text.Label;
import spacegraph.space2d.widget.windo.Widget;

import java.util.List;

/**
 * super repl
 */
public class OmniBox extends Widget {

    final TextEdit edit;


    private final Gridding results;
    private final Splitting divider;

    private final Model model;

    abstract public static class Model {
        abstract public void onTextChange(String text, MutableContainer target);

        public void onTextChangeControlEnter(String t, MutableContainer target) {
            /** default nothing */
        }
    }


    @Deprecated public OmniBox() {
        this(new JShellModel());
    }

    public OmniBox(Model m) {
        super();

        this.model = m;

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
        edit = new TextEdit() {

            @Override
            protected void onKeyCtrlEnter() {
                String t = text();
                model.onTextChangeControlEnter(t, results);
                clear();
            }

            @Override
            protected void textChange(String next) {
                model.onTextChange(next, results);
            }

        };
        content(divider.split(edit.surface().scale(2), results, 0));
    }

    private static class JShellModel extends Model {

        private final JShell js;

        public JShellModel() {
            js = JShell.create();
        }

        @Override
        public void onTextChange(String text, MutableContainer target) {
            if (text.isEmpty())
                return; //though it works , temporary to avoid it clearing after ctrl-enter

            List<SourceCodeAnalysis.Suggestion> sugg = js.sourceCodeAnalysis().completionSuggestions(text,
                    text.length() /* TODO take actual cursor pos */,
                    new int[1]);


            target.clear();
            sugg.stream().map(SourceCodeAnalysis.Suggestion::continuation).sorted().map(x -> new PushButton(x)).forEach(target::add);
        }

        @Override
        public void onTextChangeControlEnter(String t, MutableContainer target) {
            target.clear();
            js.eval(t).forEach(e -> {
                String msg = e + " "+ e.causeSnippet() + " " + e.value() + " " + e.exception();
                target.add(new Label(msg));
            });
        }
    }




    /*protected void in(String s) {
        user.notice.emit("omnibox: " + s);
    }*/

    public static void main(String[] args) {
        SpaceGraph.window(new OmniBox(new JShellModel()), 800, 250);
    }
}
