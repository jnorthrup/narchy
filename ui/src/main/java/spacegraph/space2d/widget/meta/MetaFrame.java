package spacegraph.space2d.widget.meta;

import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.Container;
import spacegraph.space2d.hud.Ortho;
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


    @Override
    protected boolean prePaint(SurfaceRender r) {
        if (expanded) {
            clipBounds = false;

            RectFloat v = r.pixelVisible().scale(0.8f);
            pos(v);
            //pos(0, 0, r.pw, r.ph);

            //renderExpanded.set(r);
            //renderExpanded.restart(r.pw, r.ph, r.dtMS);
//            renderExpanded.dtMS = r.dtMS;
//            renderExpanded.scaleX = 1;
//            renderExpanded.scaleY = 1;
//            renderExpanded.x1 = 0;
//            renderExpanded.y1 = 0;
//            renderExpanded.x2 = r.pw;
//            renderExpanded.y2 = r.ph;
//            renderExpanded.pw = r.pw;
//            renderExpanded.ph = r.ph;
            //renderExpanded = r;

            //r.overlay(this::paintLater);
            return true;
        } else {
            clipBounds = true;
            return super.prePaint(r);
        }
    }

    @Override
    public boolean showing() {
        return expanded ? true : super.showing(); //HACK
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

                    Surface overlay = parent(Ortho.class).space.layers.get(3);
                    ((Ortho)overlay).setSurface(the());
                    set(null);

                } else {

                    set(((Ortho)parent(Ortho.class).space.layers.get(3)).content());


                    SurfaceBase p = parent;
                    if (p!=null) {
                        ((Container) p).layout();
                    }

                }

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
