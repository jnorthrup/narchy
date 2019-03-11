package spacegraph.space2d.widget.meta;

import jcog.WTF;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Labeled;
import spacegraph.space2d.MenuSupplier;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.Container;
import spacegraph.space2d.container.collection.MutableListContainer;
import spacegraph.space2d.hud.HudHover;
import spacegraph.space2d.hud.ZoomOrtho;
import spacegraph.space2d.widget.text.BitmapLabel;
import spacegraph.space2d.widget.text.VectorLabel;

/**
 * a dynamic frame for attaching to widgets providing access to context menus, controls, and display
 */
public class MetaFrame extends Bordering implements HudHover {


    private MetaFrame() {
        super();
    }

    public MetaFrame(Object o) {
        this(new ObjectSurface(o));
    }

    public MetaFrame(Surface surface) {
        super(surface);
    }


    /**
     * referent; by default - the surface
     */
    protected final Surface the() {
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

            pos(r.pixelVisible().scale(0.8f));
            //pos(0, 0, r.pw, r.ph);

            //renderExpanded.setAt(r);
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
        return expanded || super.showing(); //HACK
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



        Surface surface = get(0);
        Surface wm = (surface instanceof MenuSupplier) ? ((MenuSupplier) surface).menu() : null;
        if(wm!=null)

            set(S,wm);
        else
            borderSouth=0;



        set(N, label());
        //n.click(this::click); //DISABLED

    }

    /**
     * titlebar clicked
     */
    protected void click() {
        synchronized (this) {

            boolean e = expanded;

            if (!e) {
                //TODO unexpand any other MetaFrame popup that may be expanded.  check the root context's singleton map

                undock();
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


    private void undock() {
        MutableListContainer hud = hud();

        /*if (hud.isEmpty())*/ {

            Surface content = the();
            if (content != null) {
                SatelliteMetaFrame wrapper = new SatelliteMetaFrame(content);
                hud.add(wrapper);
                wrapper.pos(hud.bounds.scale(0.8f));
                expanded = true;
                satellite = wrapper;
            }

        }

    }

    private MutableListContainer hud() {
        return (MutableListContainer) parent(ZoomOrtho.class).hud().get(3);
    }


//        Surface m = grid(
//                PushButton.awesome("tag"),
//                PushButton.awesome("sitemap")
//        );
//        setAt(E, m);

//        PushButton hideButton = PushButton.awesome("times");
//        setAt(NE, new Scale(hideButton, 0.8f));


    @Deprecated private String name() {
        //try to avoid
        return childrenCount() == 0 ? "" : the().toString();
    }

    protected Surface label() {
        Surface label;
        Surface x = the();
        if (x!=null) {
            if (x instanceof Labeled)
                label = ((Labeled) x).label();
            else
                label = new VectorLabel(name());
            return label;
        } else
            return null;
    }

    @Override
    public String toString() {
        return name();
    }

    @Override
    public Surface hover(RectFloat screenBounds, Finger f) {
        return new BitmapLabel(screenBounds.toString()).pos(screenBounds.scale(1.5f));
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

}
