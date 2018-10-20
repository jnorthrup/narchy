package spacegraph.space2d.widget.textedit;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.textedit.keybind.ActionRepository;
import spacegraph.space2d.widget.textedit.keybind.EmacsKeyListener;
import spacegraph.video.Draw;


public class TextEdit extends Widget {

    public final TextEditModel model;

    public static TextEditModel defaultModel() {
        TextEditModel e = new TextEditModel();
        e.actions = new ActionRepository();
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

        Draw.bounds(bounds, gl, g -> {
            gl.glTranslatef(0.5f, 0.5f, 0); //HACK
            gl.glScalef(0.25f, 0.25f, 0.25f); //HACK

            model.drawables().forEach(base -> base.draw(g));
        });
    }

    @Override
    public final boolean key(KeyEvent e, boolean pressedOrReleased) {
        model.keys.key(e, pressedOrReleased);
        return true;
    }

}
