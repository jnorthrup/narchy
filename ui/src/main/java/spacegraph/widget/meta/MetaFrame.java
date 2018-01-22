package spacegraph.widget.meta;

import spacegraph.AspectAlign;
import spacegraph.Scale;
import spacegraph.Surface;
import spacegraph.layout.Stacking;
import spacegraph.widget.button.PushButton;
import spacegraph.widget.text.Label;
import spacegraph.widget.windo.Widget;

import static spacegraph.layout.Grid.grid;

/**
 * a dynamic frame for attaching to widgets providing access to context menus, controls, and display
 */
public class MetaFrame extends Stacking {

//    private final Widget widget;


    public MetaFrame(Widget widget) {
        super();
//        this.widget = widget;

        Surface m = grid(
                new PushButton("@"), //tag
                new PushButton("?"), //inspect
                new PushButton("X")  //hide
        );
        Surface n = grid(
                new Label(name(widget))
        );

        set(
                new Scale(widget.content, 0.9f),
                //widget.content,
                new AspectAlign(m, 1f, AspectAlign.Align.RightTop, 0.1f, 0.1f),
                new AspectAlign(n, 1f, AspectAlign.Align.LeftTopOut, 1f, 0.1f));
    }

    protected String name(Surface widget) {
        return widget.toString();
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
