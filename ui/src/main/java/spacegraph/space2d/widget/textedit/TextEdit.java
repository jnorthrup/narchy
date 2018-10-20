package spacegraph.space2d.widget.textedit;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.textedit.keybind.EmacsKeyListener;
import spacegraph.space2d.widget.textedit.view.BufferView;
import spacegraph.video.Draw;


public class TextEdit extends Widget {

    public final TextEditModel model;

    public static TextEditModel defaultModel() {
        TextEditModel e = new TextEditModel();
        e.actions = new TextEditActions();
        e.keys = new EmacsKeyListener(e);
        return e;
    }

    public TextEdit() {
        this(defaultModel());
    }

    public TextEdit(TextEditModel editor) {
        this.model = editor;
    }

    @Override
    protected void paintWidget(GL2 gl, RectFloat bounds) {

        //float charAspect = 1.4f;
        int charsWide = 16;
        int charsHigh = 8;
        Draw.bounds(bounds, gl, g -> {
            gl.glTranslatef(0f, 1f - 1f/charsHigh, 0);
            gl.glScalef(1f/charsWide, 1f/charsHigh, 1f);

            BufferView v = model.view;
            if (v!=null)
                v.paint(g);
        });
    }

    @Override
    public final boolean key(KeyEvent e, boolean pressedOrReleased) {
        model.keys.key(e, pressedOrReleased);
        return true;
    }

}
