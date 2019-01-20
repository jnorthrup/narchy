package spacegraph.video;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import jcog.Texts;
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



    private final IRL index;

    private OsmSpace.LonLatProjection projection =
            new OsmSpace.RawProjection();
            //new OsmSpace.ECEFProjection();

    public final AtomicBoolean showIndexBounds = new AtomicBoolean(false);


    final v2 translate = new v2();

    public OsmSurface(IRL i) {
        this.index = i;
    }



    @Deprecated transient protected Osm o = null;

    @Override
    protected void paint(GL2 gl, SurfaceRender surfaceRender) {
        gl.glPushMatrix();

        gl.glTranslatef(
                translate.x + bounds.x + bounds.w/2,
                translate.y + bounds.y + bounds.h/2, 0); //center in view


        projection.transform(gl, bounds);

        {
            renderMap(gl);

            if (showIndexBounds.get())
                renderIndexBounds(gl);
        }

        projection.untransform(gl, bounds);

        gl.glPopMatrix();

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
        projection.center(o.geoBounds.cx(), o.geoBounds.cy());
        return this;
    }

    public OsmSurface go(float lon, float lat, float lonRange, float latRange) {
        this.o = index.request(lon, lat, lonRange, latRange);
        projection.center(lon, lat);
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



        v2 prev = new v2();
        @Override
        protected boolean startDrag(Finger f) {
            prev.set(0,0);
            return super.startDrag(f);
        }

        @Override
        public void move(float tx, float ty) {
            projection.pan(tx - prev.x, ty - prev.y, bounds);
            prev.set(tx, ty);
        }
    };



    @Override
    public Surface finger(Finger finger) {
        float wheel;
        if ((wheel = finger.rotationY(true)) != 0) {
            projection.zoom(wheel);
            return this;
        }

        if (finger.tryFingering(pan)) {
            return this;
        } else {
            v2 pos = finger.posPixel;
            float wx = -bounds.w/2 + pos.x;
            float wy = -bounds.h/2 + pos.y;
            float wz = 0;

            //TODO unproject screen to world

            float touch[] = new float[3];
            projection.unproject(wx, wy, wz, touch);
            System.out.println(Texts.n4(wx,wy,wz) + " -> " + Texts.n4(touch));
            index.index.whileEachIntersecting(HyperRectFloat.cube(touch, 0.0001f), (each)->{
                if (each.tags!=null) {
                    System.out.println(each.tags);
                }
                return true;
            });

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
