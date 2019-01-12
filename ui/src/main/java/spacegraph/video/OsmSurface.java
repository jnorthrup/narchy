package spacegraph.video;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import jcog.math.FloatRange;
import jcog.math.v2;
import jcog.tree.rtree.HyperRegion;
import jcog.tree.rtree.rect.HyperRectFloat;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.FingerMove;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.util.geo.IRL;
import spacegraph.util.geo.osm.Osm;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class OsmSurface extends Surface {

    /** TODO move scale, center, translate to a generic 2D projection impl */
    final FloatRange scale = new FloatRange(16f, 0.001f, 1000f);

    private final IRL index;

    private OsmSpace.LonLatProjection projection = OsmSpace.LonLatProjection.Raw;

    public final AtomicBoolean showIndexBounds = new AtomicBoolean(false);

    public OsmSurface(IRL i) {
        this.index = i;
    }


    private v2 center = new v2();
    final v2 translate = new v2();

    @Deprecated transient protected Osm o = null;

    @Override
    protected void paint(GL2 gl, SurfaceRender surfaceRender) {
        gl.glPushMatrix();

        gl.glTranslatef(
                translate.x + bounds.x + bounds.w/2,
                translate.y + bounds.y + bounds.h/2, 0); //center in view

        transform(gl);

        {
            renderMap(gl);

            if (showIndexBounds.get())
                renderIndexBounds(gl);
        }

        gl.glPopMatrix();

    }

    /** setup the initial rendering transform TODO integrate with projection */
    private void transform(GL2 gl) {
        float viewScale = this.scale.floatValue() * Math.max(bounds.w, bounds.h);

        gl.glScalef(viewScale, viewScale, 1);

        gl.glTranslatef(-center.x, -center.y, 0);
    }

    private void renderIndexBounds(GL2 gl) {

        gl.glLineWidth(2);

        index.index.root().streamNodesRecursively().forEach(n -> {
            HyperRegion b = n.bounds();
            if (b instanceof HyperRectFloat) {
                HyperRectFloat r = (HyperRectFloat)b;
                float x1 = r.min.coord(0);
                float y1 = r.min.coord(1);
                Draw.colorHash(gl, r.hashCode(), 0.25f);
                //Draw.rect(
                Draw.rectStroke(
                        x1, y1, r.max.coord(0)-x1, r.max.coord(1)-y1,
                        gl
                );
            }
        });
    }

    private void renderMap(GL2 gl) {
        if (o !=null) {

            RectFloat b = o.geoBounds;

            if (b != null) {



                Consumer<GL2> renderProc;

                if (!o.ready)
                    renderProc = loading;
                else {
                    GLContext ctx = gl.getContext();
                    Object c = ctx.getAttachedObject(o.id);
                    if (projection.changed() && c!=null) {
                        //detach and create new
                        ctx.detachObject(o.id);
                        c = null;
                    }
                    if (c == null) {
                        c = new OsmSpace.OsmRenderer(gl, projection);
                        OsmSpace.OsmRenderer r = ((OsmSpace.OsmRenderer) c);
//                        HyperRectFloat viewBounds = new HyperRectFloat(
//                                new float[] { },
//                                new float[] { }
//                        );
                        o.ways.forEach(w -> r.addWay(w));
                        o.nodes.values().forEach(n -> r.addNode(n));
//                        //index.index.forEach(e -> {//whileEachIntersecting(viewBounds,e->{
//                            if (e instanceof OsmWay)
//                                r.addWay((OsmWay)e);
//                            else if (e instanceof OsmNode)
//                                r.addNode((OsmNode)e);
//                            //return true;
//                        });
                        ctx.attachObject(o.id, c);
                        projection.changeNoticed();
                    }
                    renderProc = (Consumer<GL2>) c;
                }

                renderProc.accept(gl);

                /* debug */ {
                    gl.glColor4f(0.5f, 0.5f, 0.5f, 0.5f);
                    Draw.rectFrame(gl, b.cx(), b.cy(),
                            b.w, b.h, 0.0001f);

                }

            }
        }
    }


    private static final Consumer<GL2> loading = (gl)->{
        gl.glColor3f(1, 0, 0);
        Draw.rectFrame(gl, 0, 0, 1, 1, 0.1f);
    };

    public OsmSurface go(Osm o) {
        this.o = o;
        center.set(o.geoBounds.cx(), o.geoBounds.cy());
        return this;
    }

    public OsmSurface go(float lon, float lat, float lonRange, float latRange) {
        this.o = index.request(lon, lat, lonRange, latRange);
        center.set(lon, lat);
        return this;
    }

    private class AnimLabel extends VectorLabel {
        final Supplier<String> text;

        public AnimLabel(Supplier<String> text) {
            this.text = text;
        }

        @Override
        protected boolean prePaint(SurfaceRender r) {
            text(text.get());
            return super.prePaint(r);
        }
    }

    final FingerMove pan = new FingerMove(0) {


        private v2 dragStart;

        @Override
        protected boolean startDrag(Finger f) {
            dragStart = center.clone();
            return super.startDrag(f);
        }

        @Override
        public void move(float tx, float ty) {
            float s = 1f  / (scale.floatValue() * Math.max(bounds.w, bounds.h));
            center.set(dragStart.x - tx*s, dragStart.y - ty*s);
        }
    };



    @Override
    public Surface finger(Finger finger) {
        float wheel;
        if ((wheel = finger.rotationY(true)) != 0) {
            scale.multiply((1f - wheel * 0.1f));
            return this;
        }

        if (finger.tryFingering(pan)) {
            return this;
        }

        return null;
    }

    public Surface view() {
        return new Stacking(
                this
//                ,
//                new Bordering().south(
//                    Gridding.col(
//                        new AnimLabel(()->"translation: " + translate.toString()),
//                        new AnimLabel(()->"scale: " + scale.toString())
//                    )
//                )
        );
    }


}
