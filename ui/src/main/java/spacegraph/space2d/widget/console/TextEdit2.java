package spacegraph.space2d.widget.console;

import com.jogamp.opengl.GL;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.widget.text.BitmapLabel;

import java.util.function.Consumer;

/**
 * https://github.com/mabe02/lanterna/blob/master/src/main/java/com/googlecode/lanterna/gui2/TextBox.java
 * https://viewsourcecode.org/snaptoken/kilo/
 * TODO
 * */
public class TextEdit2 extends BitmapLabel {
    int cursorX = 0, cursorY = 0;

    public static class TextCell {
        char c;
        float r, g, b;
        //TODO other properties
        @Nullable Consumer<GL> render;
    }
}
