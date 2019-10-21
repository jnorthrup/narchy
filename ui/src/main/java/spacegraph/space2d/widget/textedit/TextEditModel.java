package spacegraph.space2d.widget.textedit;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import jcog.event.ListTopic;
import jcog.event.Topic;
import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.container.ScrollXY;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.textedit.buffer.Buffer;
import spacegraph.space2d.widget.textedit.keybind.TextEditKeys;
import spacegraph.space2d.widget.textedit.view.TextEditView;
import spacegraph.video.Draw;

import java.util.function.Consumer;

public class TextEditModel extends Widget /* TODO Surface */ implements ScrollXY.ScrolledXY {

    public final Topic<KeyEvent> keyPress = new ListTopic<>();

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

    public /*synchronized*/ void setBuffer(Buffer buf) {
        if (buffer != buf) {
            buffer = buf;
            view = new TextEditView(buf) {
                @Override
                protected void updateY() {
                    super.updateY();
                    updated();
                }
            };
        }
    }

    protected void updated() {

    }

    @Override
    public final boolean key(KeyEvent e, boolean pressedOrReleased) {
        //TODO anything from super.key(..) ?
        boolean b = keys.key(e, pressedOrReleased, this);
        if (b) {
            if (pressedOrReleased)
                keyPress.emit(e); //release
        }
        return b;
    }


    @Override
    public void update(ScrollXY s) {

        //calculate min,max scales, with appropriate aspect ratio restrictions

        int w = Math.max(1, Math.min(buffer.width(), 80));
        int h = Math.max(1, Math.min(buffer.height(), 20));
        s.viewMinMax(new v2(1.0F, 1.0F), new v2((float) w, (float) h));

        s.scroll((float) 0, (float) 0, (float) w, (float) h);
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
            Draw.bounds(bounds, gl, new Consumer<GL2>() {
                        @Override
                        public void accept(GL2 gg) {
                            Draw.stencilMask(gg, true, Draw::rectUnit,
                                    new Consumer<GL2>() {
                                        @Override
                                        public void accept(GL2 g) {
                                            v.paint(cursorVisible, viewed, g);
                                        }
                                    }
                            );
                        }
                    }
            );
        }

    }
}
