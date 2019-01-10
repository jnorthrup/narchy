package spacegraph.video;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import jcog.math.FloatRange;
import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.FingerMove;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.util.geo.IRL;
import spacegraph.util.geo.osm.Osm;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class OsmSurface extends Surface {

    /** TODO move scale, center, translate to a generic 2D projection impl */
    final FloatRange scale = new FloatRange(16f, 0.001f, 1000f);
    private v2 center = new v2();
    final v2 translate = new v2();

    transient protected Osm o = null;

    final FingerMove pan = new FingerMove(0) {


        private v2 translateStart;

        @Override
        protected boolean startDrag(Finger f) {
            translateStart = translate.clone();
            return super.startDrag(f);
        }

        @Override
        public void move(float tx, float ty) {
            translate.set(translateStart.x + tx, translateStart.y + ty);
        }
    };

    private OsmSpace.LonLatProjection projection = OsmSpace.LonLatProjection.Raw;


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

    @Override
    protected void paint(GL2 gl, SurfaceRender surfaceRender) {

        if (o !=null) {

            RectFloat b = o.geoBounds;

            if (b != null) {

                gl.glPushMatrix();

                gl.glTranslatef(
                        translate.x + bounds.x + bounds.w/2,
                        translate.y + bounds.y + bounds.h/2, 0); //center in view

                float viewScale = this.scale.floatValue() * Math.max(bounds.w, bounds.h);

                gl.glScalef(viewScale, viewScale, 1);

                gl.glTranslatef(-center.x, -center.y, 0);

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
                        c = new OsmSpace.OsmRenderer(gl, o, projection);
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

                gl.glPopMatrix();
            }
        }

    }


    private static final Consumer<GL2> loading = (gl)->{
        gl.glColor3f(1, 0, 0);
        Draw.rectFrame(gl, 0, 0, 1, 1, 0.1f);
    };

    public OsmSurface go(IRL i, float lon, float lat, float lonRange, float latRange) {
        o = i.request(lon, lat, lonRange, latRange);
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
}
