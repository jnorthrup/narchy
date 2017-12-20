package spacegraph.widget.meta;

import com.jogamp.opengl.GL2;
import spacegraph.AspectAlign;
import spacegraph.Surface;
import spacegraph.SurfaceRoot;
import spacegraph.widget.button.PushButton;
import spacegraph.widget.text.Label;
import spacegraph.widget.windo.Widget;

import static spacegraph.layout.Grid.grid;

/**
 * a dynamic frame for attaching to widgets providing access to context menus, controls, and display
 */
public class MetaFrame extends Widget {

    private final Widget base;


    MetaFrame(Widget base) {
        super();
        this.base = base;

        build();
    }

    @Override
    public boolean tangible() {
        return false;
    }

    public static void toggle(Widget base) {
        SurfaceRoot r = base.root();
        if (r == null) //not attached
            return;

        MetaFrame existing = (MetaFrame) r.the(MetaFrame.class);
        if (existing != null && existing.base == base) {
            //toggle off: detach
            r.the(MetaFrame.class, null, null);

        } else {
            //toggle on: attach
            MetaFrame mfer = new MetaFrame(base);
            r.the(MetaFrame.class, mfer, mfer::close);

            base.children.add(mfer);
            r.zoom(base.cx(), base.cy(), base.w(), base.h());
        }
    }

    protected void build() {
        Surface m = grid(
                new PushButton("@"), //tag
                new PushButton("?"), //inspect
                new PushButton("X")  //hide
        );
        children.add(new AspectAlign(m, 1f,
                AspectAlign.Align.RightTop, 0.1f, 0.1f));

        Surface n = grid(
                new Label(base.toString())
        );
        children.add(new AspectAlign(n, 1f,
                AspectAlign.Align.LeftTopOut,
                1f, 0.1f));

    }

    public void close() {
        base.children.remove(this);
    }

    @Override
    public void doLayout(int dtMS) {
        pos(base.bounds);
        super.doLayout(dtMS);
    }

    @Override
    protected void paintIt(GL2 gl) {

    }
}
