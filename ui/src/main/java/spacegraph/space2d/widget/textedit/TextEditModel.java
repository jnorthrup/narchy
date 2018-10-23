package spacegraph.space2d.widget.textedit;

import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.widget.textedit.buffer.Buffer;
import spacegraph.space2d.widget.textedit.keybind.TextEditKeys;
import spacegraph.space2d.widget.textedit.view.TextEditView;
import spacegraph.video.Draw;

public final class TextEditModel  {


    /** current buffer */
    protected Buffer buffer;

    public TextEditView view = null;

    public TextEditActions actions;
    public TextEditKeys keys;

    public TextEditModel() {
        this(new Buffer("", ""));
    }

    public TextEditModel(Buffer buf) {
        setBuffer(buf);
    }

    public synchronized void setBuffer(Buffer buf) {
        if (buffer != buf) {
            buffer = buf;
            view = new TextEditView(buf);
        }
    }

    public void execute(String name, String... args) {
        actions.run(this, name, args);
    }

    public final Buffer buffer() {
        return this.buffer;
    }


    public void createNewBuffer() {
        setBuffer(new Buffer("scratch-" + System.currentTimeMillis(), ""));
    }



    public void paint(RectFloat bounds, RectFloat viewed, boolean cursorVisible, GL2 gl) {

        TextEditView v = view;
        if (v!=null) {
            Draw.bounds(bounds, gl, gg ->
                Draw.stencilMask(gg, true, Draw::rectUnit,
                    g -> v.paint(cursorVisible, viewed, g)
                )
            );
        }

    }
}
