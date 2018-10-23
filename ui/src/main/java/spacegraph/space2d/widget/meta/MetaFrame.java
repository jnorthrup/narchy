package spacegraph.space2d.widget.meta;

import jcog.TODO;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.Scale;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.VectorLabel;

/**
 * a dynamic frame for attaching to widgets providing access to context menus, controls, and display
 */
public class MetaFrame extends Bordering {


    public MetaFrame(Surface surface) {
        super(surface);
    }

    /** referent; by default - the surface */
    protected Object the() {
        return get(0);
    }

    protected Surface newMetaMenu() {
        return new Gridding(

            new PushButton("^").click(()->{
                //pop-out
                Surface s = get(0);
                if (s.remove()) {
                    SpaceGraph.window(s, 500, 500); //TODO size by window TODO child window
                }
            }),
            new PushButton("=").click(()->{
                //tag
                throw new TODO();
            }),
            new PushButton("?").click(()->{
                //pop-up inspection view
                Object tt = the();
                if (tt instanceof Surface) {
                    SpaceGraph.window(new Inspector((Surface) tt), 500, 500); //TODO size by window TODO child window
                }
            })
            //copy, paste, ...
            //modal view lock to bounds
            //etc
        );
    }

    @Override
    protected void starting() {
        super.starting();

        PushButton n = new PushButton(new VectorLabel(name()));
        n.click(()->{
            synchronized (MetaFrame.this) {
                //root().zoom(MetaFrame.this);
                if (borderWest == 0) {
                    set(W, newMetaMenu(), 0.1f);
                } else {
                    remove(W);
                    borderSize(W, 0);
                }
            }
        });
        set(N, n);



//        Surface m = grid(
//                PushButton.awesome("tag"),
//                PushButton.awesome("sitemap")
//        );
//        set(E, m);

        PushButton hideButton = PushButton.awesome("times");
        set(NE, new Scale(hideButton, 0.8f));


        Surface surface = get(0);
        Surface wm = (surface instanceof Menu) ? ((Menu) surface).menu() : null;
        if (wm != null)
            set(S, wm);
        else
            borderSouth = 0;
    }



    protected String name() {
        return childrenCount() == 0 ? "" : get(0).toString();
    }


    public void close() {

    }


    public interface Menu {
        Surface menu();
    }
}
