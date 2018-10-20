package spacegraph.space2d.widget;

import spacegraph.SpaceGraph;
import spacegraph.space2d.widget.textedit.TextEdit;

/**
 * https://github.com/mabe02/lanterna/blob/master/src/main/java/com/googlecode/lanterna/gui2/TextBox.java
 * https://viewsourcecode.org/snaptoken/kilo/
 * TODO
 * */
public class TextEditTest  {


    public static void main(String[] args) {
        TextEdit t = new TextEdit();
        t.text("xyz");

//        TextEditModel e = t.model;
//        e.execute("type", "x");
//        e.execute("type", "y");
//        e.execute("type", "z");

        SpaceGraph.window(t, 800, 800);


    }
}
