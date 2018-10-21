package spacegraph.space2d.widget.textedit;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.input.finger.Finger;
import spacegraph.input.key.KeyPressed;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRoot;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.textedit.keybind.EmacsKeyListener;
import spacegraph.space2d.widget.textedit.view.BufferView;
import spacegraph.video.Draw;

import java.util.function.Predicate;


public class TextEdit extends Widget implements KeyPressed {

    public final TextEditModel model;


    final Predicate<Finger> grabFocus = Finger.clicked(0, this::keyFocus);

    private boolean keyFocused = false;

    public TextEdit(String initialText) {
        this();

        text(initialText);
    }

    @Override
    public void keyStart() {
        keyFocused = true;
    }

    @Override
    public void keyEnd() {
        keyFocused = false;
    }

    /** request keyboard focus */
    public boolean keyFocus() {
        SurfaceRoot r = root();
        if (r == null)
            return false;

        return r.keyFocus(this);
    }


    @Override
    public Surface finger(Finger finger) {
//        Surface s = super.finger(finger);
//        if (s == this) {
            if (grabFocus.test(finger)) {
                //return this;
            }
//        }
        return this;
    }

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
        Draw.bounds(bounds, gl, gg -> {
            Draw.stencilMask(gg, true, (g)->{
                Draw.rectUnit(g);
            }, (g)->{
                g.glPushMatrix();
                g.glTranslatef(0f, 1f - 1f/charsHigh, 0);
                g.glScalef(1f/charsWide, 1f/charsHigh, 1f);

                BufferView v = model.view;
                if (v!=null)
                    v.paint(cursorVisible(), g);
                g.glPopMatrix();
            });
        });
    }

    public boolean cursorVisible() {
        return keyFocused;
    }

    @Override
    public final boolean key(KeyEvent e, boolean pressedOrReleased) {
        model.keys.key(e, pressedOrReleased);
        return true;
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

}
