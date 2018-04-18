package spacegraph.space2d.widget.meta;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.windo.Widget;

import static spacegraph.space2d.container.grid.Gridding.grid;

/**
 * a dynamic frame for attaching to widgets providing access to context menus, controls, and display
 */
public class MetaFrame extends Bordering {

//    private final Widget widget;


    public MetaFrame(Widget widget) {
        super(widget.content());
//        this.widget = widget;

        Surface m = grid(
                new PushButton("@"), //tag
                new PushButton("?") //inspect
        );

        Runnable zoomer = () -> {
            //TODO if already significantly zoomed (ex: > 75% view consumed by the widget) then unzoom
            widget.root().zoom(widget);
        };


        Surface n =
                //new BitmapLabel(name(widget));
                //new Label(name(widget));
                grid(new PushButton(name(widget), zoomer));

        PushButton hideButton = new PushButton("X");

        borderWest = 0;
        set(N, n);
        set(E, m);
        set(NE, hideButton);

        //PushButton zoomButton = new PushButton("*", zoomer);
        //set(SW, zoomButton);


        Surface wm = (widget instanceof Menu) ? ((Menu) widget).menu() : null;
        if (wm != null)
            set(S, wm);
        else
            borderSouth = 0;

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


    public interface Menu {
        Surface menu();
    }
}
