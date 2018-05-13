package spacegraph.space2d.widget.meta;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.Label;

import static spacegraph.space2d.container.grid.Gridding.grid;

/**
 * a dynamic frame for attaching to widgets providing access to context menus, controls, and display
 */
public class MetaFrame extends Bordering {

//    private final Widget widget;


    public MetaFrame(Surface surface) {
        super(surface);
//        this.widget = widget;

        Surface m = grid(
                PushButton.awesome("tag"), //tag
                PushButton.awesome("sitemap") //inspect
        );

        Runnable zoomer = () -> {
            //TODO if already significantly zoomed (ex: > 75% view consumed by the widget) then unzoom
            surface.root().zoom(surface);
        };


        Surface n =
                //new BitmapLabel(name(widget));
                new Label(name(surface));
                //grid(new PushButton(name(surface), zoomer));

        PushButton hideButton = PushButton.awesome("times");

        borderWest = 0;
        set(N, n);
        set(E, m);
        set(NE, hideButton);

        //PushButton zoomButton = new PushButton("*", zoomer);
        //set(SW, zoomButton);


        Surface wm = (surface instanceof Menu) ? ((Menu) surface).menu() : null;
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
