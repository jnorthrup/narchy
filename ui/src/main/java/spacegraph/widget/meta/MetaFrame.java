package spacegraph.widget.meta;

import spacegraph.AspectAlign;
import spacegraph.Surface;
import spacegraph.layout.Stacking;
import spacegraph.widget.button.PushButton;
import spacegraph.widget.text.Label;

import static spacegraph.layout.Grid.grid;

/**
 * a dynamic frame for attaching to widgets providing access to context menus, controls, and display
 */
public class MetaFrame extends Stacking {

    private final Surface base;


    public MetaFrame(Surface base) {
        super();
        this.base = base;

        Surface m = grid(
                new PushButton("@"), //tag
                new PushButton("?"), //inspect
                new PushButton("X")  //hide
        );
        Surface n = grid(
                new Label(this.base.toString())
        );

        children(
                //new Scale(base, 0.5f),
                new AspectAlign(base, 1f, AspectAlign.Align.RightTop, 0.1f, 0.1f),
                new AspectAlign(m, 1f, AspectAlign.Align.RightTop, 0.1f, 0.1f),
                new AspectAlign(n, 1f, AspectAlign.Align.LeftTopOut, 1f, 0.1f));
    }

    @Override
    public boolean tangible() {
        return false;
    }

//    public static void toggle(Widget base) {
//        SurfaceRoot r = base.root();
//        if (r == null) //not attached
//            return;
//
//        MetaFrame existing = (MetaFrame) r.the(MetaFrame.class);
//        if (existing != null && existing.base == base) {
//            //toggle off: detach
//            r.the(MetaFrame.class, null, null);
//
//            r.unzoom();
//
//        } else {
//            //toggle on: attach
//
//            MetaFrame mfer = new MetaFrame(base);
//            r.the(MetaFrame.class, mfer, mfer::close);
//            base.add(mfer);
//
//            r.zoom(base.cx(), base.cy(), base.w(), base.h());
//        }
//    }

    public void close() {
        //base.children().remove(this);
    }


}
