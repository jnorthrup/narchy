package spacegraph.space2d.widget.meta;

import jcog.WTF;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.Container;
import spacegraph.space2d.container.collection.MutableListContainer;
import spacegraph.space2d.hud.Ortho;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.VectorLabel;

/**
 * a dynamic frame for attaching to widgets providing access to context menus, controls, and display
 */
public class MetaFrame extends Bordering {


    private MetaFrame() {
        super();
    }

    public MetaFrame(Surface surface) {
        super(surface);
    }

    private class SatelliteMetaFrame extends MetaFrame {

        public SatelliteMetaFrame(Surface surface) {
            super(null);
            surface.reattach(this);
        }

        @Override
        protected void click() {
            MetaFrame.this.dock();
        }
    }

    /**
     * referent; by default - the surface
     */
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
    SatelliteMetaFrame satellite = null;

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
    public boolean detachChild(Surface s) {
        synchronized (this) {
            if (get(0) == s) {
                put(0, null, false);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean attachChild(Surface s) {
        synchronized (this) {
            if (get(0) == null) {
                put(0, s, false);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void starting() {
        super.starting();

        PushButton n = new PushButton(new VectorLabel(name()));
        set(N, n);

        Surface surface = get(0);
        Surface wm = (surface instanceof Menu) ? ((Menu) surface).menu() : null;
        if(wm!=null)

            set(S,wm);
        else
            borderSouth=0;



        n.click(() -> {

            click();


            //root().zoom(MetaFrame.this);
//                if (borderWest == 0) {
//                    set(W, newMetaMenu(), 0.1f);
//                } else {
//                    remove(W);
//                    borderSize(W, 0);
//                }

        });


    }

    /**
     * titlebar clicked
     */
    protected void click() {
        synchronized (this) {

            boolean e = expanded;

            Ortho hud = (Ortho) parent(Ortho.class).space.layers.get(3);
            MutableListContainer target = (MutableListContainer) hud.the();
            if (!e) {
                //TODO unexpand any other MetaFrame popup that may be expanded.  check the root context's singleton map

                undock(target);
            } else {


                dock();

                //
//
//                    SurfaceBase p = parent;
//                    if (p!=null) {
//                        ((Container) p).layout();
//                    }


            }
        }
    }

    private void dock() {
        synchronized (this) {
            if (expanded) {
                assert (satellite != null);

                if (satellite.the().reattach(this)) {
                    expanded = false;

                    if (!satellite.remove())
                        throw new WTF();

                    satellite = null;
                    
                    ((Container)parent).layout();
                } else
                    throw new WTF();
            }
        }
    }


    private void undock(MutableListContainer target) {
        if (target.isEmpty()) {

            Surface content = the();
            if (content != null) {
                SatelliteMetaFrame wrapper = new SatelliteMetaFrame(content);
                {
                    target.add(wrapper);
                    wrapper.pos(target.bounds.scale(0.8f));
                    expanded = true;
                    satellite = wrapper;
                }
            }

        }

    }


//        Surface m = grid(
//                PushButton.awesome("tag"),
//                PushButton.awesome("sitemap")
//        );
//        set(E, m);

//        PushButton hideButton = PushButton.awesome("times");
//        set(NE, new Scale(hideButton, 0.8f));


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
