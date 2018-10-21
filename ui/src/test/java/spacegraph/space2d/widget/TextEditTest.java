package spacegraph.space2d.widget;

import spacegraph.SpaceGraph;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.textedit.TextEdit;

import static spacegraph.space2d.container.Bordering.S;

/**
 * https://github.com/mabe02/lanterna/blob/master/src/main/java/com/googlecode/lanterna/gui2/TextBox.java
 * https://viewsourcecode.org/snaptoken/kilo/
 * TODO
 * */
public class TextEditTest  {

    static class Basic {
        public static void main(String[] args) {
            SpaceGraph.window(new TextEdit("xyz"), 800, 800);
        }
    }

    static class View {
        public static void main(String[] args) {
            String lorum =
                    "Lorem ipsum dolor sit amet,\n" +
                    "consectetur adipiscing elit,\n" +
                    "sed do eiusmod tempor incididunt\n" +
                    "ut labore et dolore magna aliqua.\n" +
                    "Ut enim ad minim veniam, quis\n" +
                    "nostrud exercitation ullamco\n" +
                    "laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.\n";
            TextEdit a = new TextEdit(lorum);
            TextEdit b = new TextEdit("?");
            Bordering x = new Bordering(
                    new Gridding(a, b)
            ).set(S, new Gridding(
                    new PushButton("Y")
            ), 0.1f);

            SpaceGraph.window(x, 1200, 800);
        }
    }

}
