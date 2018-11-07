package spacegraph.space2d.widget.meta;

import com.jogamp.opengl.GL2;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.Container;
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
    protected Surface the() {
        return get(0);
    }



    //    protected Surface newMetaMenu() {
//        return new Gridding(
//
//            new PushButton("^").click(()->{
//                //pop-out
//                Surface s = get(0);
//                if (s.remove()) {
//                    SpaceGraph.window(s, 500, 500); //TODO size by window TODO child window
//                }
//            }),
//            new PushButton("=").click(()->{
//                //tag
//                throw new TODO();
//            }),
//            new PushButton("?").click(()->{
//                //pop-up inspection view
//                Object tt = the();
//                if (tt instanceof Surface) {
//                    SpaceGraph.window(new Inspector((Surface) tt), 500, 500); //TODO size by window TODO child window
//                }
//            })
//            //copy, paste, ...
//            //modal view lock to bounds
//            //etc
//        );
//    }

    boolean expanded = false;
    SurfaceRender renderExpanded = null;

    @Override
    protected boolean prePaint(SurfaceRender r) {
        if (expanded) {
            pos(r.visible().scale(0.8f));
            //doLayout(0);
            showing = true;
            renderExpanded.set(r);
            r.overlay(this::paintLater);
            return false;
        } else
            return super.prePaint(r);
    }

    @Override
    public boolean showing() {
        return expanded ? true : super.showing(); //HACK
    }

    private void paintLater(GL2 gl) {
        doPaint(gl, renderExpanded);
    }

    @Override
    protected void starting() {
        super.starting();

        PushButton n = new PushButton(new VectorLabel(name()));

        n.click(()->{

            synchronized (MetaFrame.this) {
                boolean e = expanded;
                if (!e) {
                    //TODO unexpand any other MetaFrame popup that may be expanded.  check the root context's singleton map

                    if (renderExpanded==null)
                        renderExpanded = new SurfaceRender();
                    
                    //clipBounds = false;

                } else {

                    //clipBounds = true;

                    SurfaceBase p = parent;
                    if (p!=null) {
                        ((Container) p).layout();
                    }

                }
                layout();

                expanded = !expanded;

                //root().zoom(MetaFrame.this);
//                if (borderWest == 0) {
//                    set(W, newMetaMenu(), 0.1f);
//                } else {
//                    remove(W);
//                    borderSize(W, 0);
//                }
            }
        });
        set(N, n);



//        Surface m = grid(
//                PushButton.awesome("tag"),
//                PushButton.awesome("sitemap")
//        );
//        set(E, m);

//        PushButton hideButton = PushButton.awesome("times");
//        set(NE, new Scale(hideButton, 0.8f));


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

    @Override
    public String toString() {
        return name();
    }

//    public void close() {
//
//    }


    public interface Menu {
        Surface menu();
    }
}
