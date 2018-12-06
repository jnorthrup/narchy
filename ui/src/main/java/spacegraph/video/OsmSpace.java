package spacegraph.video;


import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;
import com.jogamp.opengl.glu.GLUtessellatorCallback;
import jcog.data.list.FasterList;
import jcog.math.FloatRange;
import jcog.tree.rtree.rect.RectFloat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.FingerMove;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space3d.AbstractSpatial;
import spacegraph.space3d.phys.Collidable;
import spacegraph.util.geo.IRL;
import spacegraph.util.geo.osm.Osm;
import spacegraph.util.math.v2;

import java.util.List;
import java.util.function.Consumer;

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
            double[] vertex = new double[6];

            vertex[0] = coords[0];
            vertex[1] = coords[1];
            vertex[2] = coords[2];
            for (int i = 3; i < 6/* 7OutOfBounds from C! */; i++) {
                double v = 0;
                for (int j = 0; j < data.length; j++) {
                    double[] d = (double[]) data[j];
                    if (d != null) {
                        v += weight[j] * d[i];
                    }
                }
                vertex[i] = v;
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

        final FloatRange scale = new FloatRange(4f, 0.001f, 1000f);
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


                float scale = viewScale();

                translate.set((translateStart.x + tx / scale), (translateStart.y + ty / scale));

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

        @Override
        protected void paint(GL2 gl, SurfaceRender surfaceRender) {



            RectFloat view = bounds;


            float mapScale = this.scale.floatValue();

//            gl.glPushMatrix();

            int r = Math.min(2,Math.max(0,  (int) Math.floor(100f/mapScale)));
//            System.out.println(r + " " + mapScale);
            for (int x = -r; x < r; x++) {
                for (int y = -r; y < r; y++) {
                    render(translate.x + x * IRL.TILE_SIZE, translate.y + y * IRL.TILE_SIZE,
                            mapScale, gl);
                }
            }


//            gl.glPopMatrix();
        }

        private float viewScale() {
            return Math.max(bounds.w, bounds.h) * this.scale.get();
        }


        private void render(float cx, float cy, float mapScale, GL2 gl) {

            Osm tile = irl.tile(cx, cy);

//            RectFloat b = tile.geoBounds;
//            if (b!=null) {
//                cx = b.cx(); //use the actual position
//                cy = b.cy(); //use the actual position
//            }

//            System.out.println(tile.id + " at " + cx + " " + cy);

            gl.glPushMatrix();

            float viewScale = Math.max(bounds.w, bounds.h) * mapScale;

            gl.glTranslatef(bounds.x + bounds.w/2,
                    bounds.y + bounds.h/2, 0); //center in view

//            gl.glTranslatef((-(translate.x))/viewScale + 0.5f,
//                    (-(translate.y))/viewScale + 0.5f, 0);





            gl.glScalef( viewScale, viewScale, 1);







//            if (b!=null) {
//                gl.glColor4f(0.5f, 0.5f, 0.5f, 0.25f);
//                Draw.rectFrame(gl, 0, 0,
//                        b.w, b.h, Math.max(b.w, b.h) * 0.05f);
//            }

            tile.render(OsmSpace.this, gl).accept(gl);

            gl.glPopMatrix();
        }

        public OsmSurface go(float lon, float lat) {
            translate.set(lon, lat);
            return this;
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
