package spacegraph.space2d.widget.textedit;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.container.ScrollXY;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.textedit.buffer.*;
import spacegraph.util.math.v2;


public class TextEdit extends Widget implements ScrollXY.ScrolledXY {

    public final TextEditModel model;

    @Nullable
    private ScrollXY scroll = null;

    public TextEdit(String initialText) {
        this();

        text(initialText);
    }

    public ScrollXY scrolled() {
        ScrollXY s = new ScrollXY<>(this);

        model.buffer.addListener(new BufferListener() {


            @Override
            public void update(Buffer buffer) {

            }

            @Override
            public void updateCursor(CursorPosition cursor) {

            }

            @Override
            public void addLine(BufferLine bufferLine) {

            }

            @Override
            public void removeLine(BufferLine bufferLine) {

            }

            @Override
            public void moveChar(BufferLine fromLine, BufferLine toLine, BufferChar c) {

            }
        });
        return s;
    }

    public static TextEditModel defaultModel() {
        TextEditModel e = new TextEditModel();
        e.actions = TextEditActions.DEFAULT_ACTIONS;
        e.keys = TextEditActions.DEFAULT_KEYS;
        return e;
    }

    public TextEdit() {
        this(defaultModel());
    }

    public TextEdit(TextEditModel editor) {
        this.model = editor;
    }

    @Override
    protected final void paintWidget(RectFloat bounds, GL2 gl) {
        ScrollXY s = this.scroll;
        model.paint(bounds, s !=null ? s.view() : RectFloat.X0Y0WH(0, 0, model.buffer.width(), model.buffer.height()), focused, gl);
    }

    @Override
    public final boolean key(KeyEvent e, boolean pressedOrReleased) {
        //TODO anything from super.key(..) ?
        return model.keys.key(e, pressedOrReleased, model);
    }

    public void append(String text) {
        model.buffer().insert(text);
    }

    public void text(String text) {
        model.buffer().set(text);
    }

    public String text() {
        return model.buffer().text();
    }

    @Override
    public void update(ScrollXY s) {
        this.scroll = s;
        s.viewMin(new v2(1,1));
        updateScroll();
        s.view(0, 0, Math.min(model.buffer.width(),80), Math.min(model.buffer.height(), 20));
    }

    private void updateScroll() {
        scroll.viewMax(new v2(model.buffer.width(), model.buffer.height()));
    }
}
