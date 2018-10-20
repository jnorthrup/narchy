package spacegraph.space2d.widget;

import spacegraph.SpaceGraph;
import spacegraph.space2d.widget.textedit.TextEdit;
import spacegraph.space2d.widget.textedit.TextEditModel;

/**
 * https://github.com/mabe02/lanterna/blob/master/src/main/java/com/googlecode/lanterna/gui2/TextBox.java
 * https://viewsourcecode.org/snaptoken/kilo/
 * TODO
 * */
public class TextEditTest  {


    public static void main(String[] args) {
        TextEdit t = new TextEdit();
        TextEditModel e = t.model;
        e.executeAction("type", "x");
        e.executeAction("type", "y");
        e.executeAction("type", "z");

        SpaceGraph.window(t, 800, 800);


    }
}
