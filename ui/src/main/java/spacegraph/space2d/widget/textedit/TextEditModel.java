package spacegraph.space2d.widget.textedit;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.container.ScrollXY;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.textedit.buffer.Buffer;
import spacegraph.space2d.widget.textedit.keybind.TextEditKeys;
import spacegraph.space2d.widget.textedit.view.TextEditView;
import jcog.math.v2;
import spacegraph.video.Draw;

public class TextEditModel extends Widget implements ScrollXY.ScrolledXY{


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
    @Override
    public final boolean key(KeyEvent e, boolean pressedOrReleased) {
        //TODO anything from super.key(..) ?
        return keys.key(e, pressedOrReleased, this);
    }



    @Override
    public void update(ScrollXY s) {

        s.viewMin(new v2(1, 1));
        int w = Math.max(1, Math.min(buffer.width(), 80));
        int h = Math.max(1, Math.min(buffer.height(), 20));
        s.viewMax(new v2(w, h));
        s.view(0, 0, w, h);
    }

    @Override
    protected final void paintWidget(RectFloat bounds, GL2 gl) {

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
