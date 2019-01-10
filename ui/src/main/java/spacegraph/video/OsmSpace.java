package spacegraph.video;


import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;
import com.jogamp.opengl.glu.GLUtessellatorCallback;
import jcog.data.list.FasterList;
import jcog.math.FloatRange;
import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.FingerMove;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.space3d.AbstractSpatial;
import spacegraph.space3d.phys.Collidable;
import spacegraph.util.geo.IRL;
import spacegraph.util.geo.osm.Osm;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * OSM Renderer context
 * Created by unkei on 2017/04/25.
 */
public class OsmSpace  {

    public static final Logger logger = LoggerFactory.getLogger(OsmSpace.class);
    private final IRL irl;


    public static class OsmRenderer implements GLUtessellatorCallback, Consumer<GL2> {
        public final GLUtessellator tobj = GLU.gluNewTess();

        @Deprecated
        public transient GL2 gl;
        public List<Consumer<GL2>> draw = new FasterList();;

        public OsmRenderer(GL2 gl) {
            this.gl = gl;

            GLU.gluTessCallback(tobj, GLU.GLU_TESS_VERTEX, this);
            GLU.gluTessCallback(tobj, GLU.GLU_TESS_BEGIN, this);
            GLU.gluTessCallback(tobj, GLU.GLU_TESS_END, this);
            GLU.gluTessCallback(tobj, GLU.GLU_TESS_ERROR, this);
            GLU.gluTessCallback(tobj, GLU.GLU_TESS_COMBINE, this);
        }
        @Override
        public void begin(int type) {
            gl.glBegin(type);
        }

        @Override
        public void end() {
            gl.glEnd();
        }

        @Override
        public void vertex(Object vertexData) {
            if (vertexData instanceof double[]) {
                double[] pointer = (double[]) vertexData;
                if (pointer.length == 6)
                    gl.glColor3dv(pointer, 3);
                gl.glVertex3dv(pointer, 0);
            } else if (vertexData instanceof float[]) {
                float[] pointer = (float[]) vertexData;
                if (pointer.length == 6)
                    gl.glColor3fv(pointer, 3);
                gl.glVertex3fv(pointer, 0);
            }
        }

        @Override
        public void vertexData(Object vertexData, Object polygonData) {
        }

        /*
         * combineCallback is used to create a new vertex when edges intersect.
         * coordinate location is trivial to calculate, but weight[4] may be used to
         * average color, normal, or texture coordinate data. In this program, color
         * is weighted.
         */
        @Override
        public void combine(double[] coords, Object[] data,
                            float[] weight, Object[] outData) {
            float[] vertex = new float[6];

            vertex[0] = (float) coords[0];
            vertex[1] = (float) coords[1];
            vertex[2] = (float) coords[2];
            for (int i = 3; i < 6/* 7OutOfBounds from C! */; i++) {
                float v = 0;
                for (int j = 0; j < data.length; j++) {
                    float[] d = (float[]) data[j];
                    if (d != null) {
                        v += weight[j] * d[i];
                    }
                }
                vertex[i] = v/data.length;
            }
            outData[0] = vertex;
        }

        @Override
        public void combineData(double[] coords, Object[] data,
                                float[] weight, Object[] outData, Object polygonData) {
        }

        @Override
        public void error(int errnum) {

            String estring = Draw.glu.gluErrorString(errnum);
            System.err.println("Tessellation Error: " + estring);
            System.exit(0);
        }

        @Override
        public void beginData(int type, Object polygonData) {
        }

        @Override
        public void endData(Object polygonData) {
        }

        @Override
        public void edgeFlag(boolean boundaryEdge) {
        }

        @Override
        public void edgeFlagData(boolean boundaryEdge, Object polygonData) {
        }

        @Override
        public void errorData(int errnum, Object polygonData) {
        }

        @Override
        public void accept(GL2 g) {
            draw.forEach(d -> d.accept(g));
        }
    }

    public OsmSpace(IRL irl) {

        this.irl = irl;



//        double minLat = osm.geoBounds.minLat;
//        double minLon = osm.geoBounds.minLon;
//        double maxLat = osm.geoBounds.maxLat;
//        double maxLon = osm.geoBounds.maxLon;
//        scaleLat = (float) ((maxLat - minLat));
//        scaleLon = (float) ((maxLon - minLon));
//        scaleLat = scaleLon = Math.max(scaleLat, scaleLon);
//        center = new GeoVec3((maxLat + minLat) / 2, (maxLon + minLon) / 2);

    }



    public OsmSurface surface() {
        return new OsmSurface();
    }

    public OsmVolume volume() {
        return new OsmVolume();
    }

    public class OsmSurface extends Surface {

        final FloatRange scale = new FloatRange(16f, 0.001f, 1000f);
        private v2 center = new v2();
        final v2 translate = new v2();

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
                    this,
                    new Bordering().south(
                        Gridding.col(
                            new AnimLabel(()->"translation: " + translate.toString()),
                            new AnimLabel(()->"scale: " + scale.toString())
                        )
                    )
            );
        }

        @Override
        protected void paint(GL2 gl, SurfaceRender surfaceRender) {

            RectFloat view = bounds;

            gl.glPushMatrix();

            gl.glTranslatef(translate.x + bounds.x + bounds.w/2,
                    translate.y + bounds.y + bounds.h/2, 0); //center in view

            float mapScale = this.scale.floatValue();

            int rx = 2, ry = 2;
            float wx = center.x, wy = center.y;

            for (int x = -rx; x < (rx+1); x++) {
                for (int y = -ry; y < (ry+1); y++) {
                    render(wx + x * IRL.TILE_SIZE, wy + y * IRL.TILE_SIZE,
                            mapScale, gl);
                }
            }

            gl.glPopMatrix();
        }

        private float viewScale() {
            return Math.max(bounds.w, bounds.h) * this.scale.get();
        }


        private void render(float cx, float cy, float mapScale, GL2 gl) {

            Osm tile = irl.tile(cx, cy);
            if (tile==null)
                return;
            RectFloat b = tile.geoBounds;
            if(b==null)
                return; //not ready yet?

            //System.out.println(cx + "," + cy + " x " + mapScale + ": "  + tile);

//            System.out.println(tile.id + " at " + cx + " " + cy);

            gl.glPushMatrix();


            float viewScale = mapScale * Math.max(bounds.w, bounds.h);


            gl.glScalef( viewScale, viewScale, 1);

            gl.glTranslatef(-(center.x),-(center.y),0);






            tile.render(gl).accept(gl);

            {
                gl.glColor4f(0.5f, 0.5f, 0.5f, 1f);
                Draw.rectFrame(gl, b.cx(), b.cy(),
                        b.w, b.h, 0.0001f);

            }

            gl.glPopMatrix();
        }

        public OsmSurface go(float lon, float lat) {
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

    public class OsmVolume extends AbstractSpatial<Osm> {

        OsmVolume() {
            super(null);
        }

        @Override
        public void forEachBody(Consumer<Collidable> c) {

        }

        @Override
        public float radius() {
            return 0;
        }

        @Override
        public void renderAbsolute(GL2 gl, int dtMS) {
//            if (mapRender == null) {
//                mapRender = compileMap(gl, osm);
//            }
//            mapRender.render(gl);
        }

    }



}
