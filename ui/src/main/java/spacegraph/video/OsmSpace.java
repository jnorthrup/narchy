package spacegraph.video;


import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;
import com.jogamp.opengl.glu.GLUtessellatorCallback;
import com.jogamp.opengl.math.FloatUtil;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.math.FloatRange;
import jcog.math.v2;
import jcog.math.v3;
import jcog.tree.rtree.rect.RectFloat;
import jcog.util.ArrayUtil;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.util.geo.ECEF;
import spacegraph.util.geo.osm.GeoVec3;
import spacegraph.util.geo.osm.OsmNode;
import spacegraph.util.geo.osm.OsmWay;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.jogamp.opengl.GL.GL_LINE_STRIP;
import static com.jogamp.opengl.GL.GL_POINTS;
import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;

/**
 * OSM Renderer context
 * Created by unkei on 2017/04/25.
 */
public enum OsmSpace  { ;

    public static final Logger logger = LoggerFactory.getLogger(OsmSpace.class);


    public abstract static class LonLatProjection {

        private boolean changed = false;

         public final void project(float lon, float lat, float alt, float[] target) {
             project(lon, lat, alt, target, 0);
         }

        public abstract void project(float lon, float lat, float alt, float[] target, int offset);

        public abstract void unproject(float x, float y, float z, float[] target);

        public final float[] project(GeoVec3 global, float[] target) {
            return project(global, target, 0);
        }

        public final float[] project(GeoVec3 global, float[] target, int offset) {
            project(global.x /* lon */, global.y /* lat */, global.z /* alt */, target, offset);
            return target;
        }

        /** notifies the renderer that points will need reprojected */
        public final boolean changed() {
            return changed;
        }

        public final void changeNoticed() {
            changed = false;
        }

        public abstract void transform(GL2 gl, RectFloat bounds);

        public abstract void pan(float tx, float ty, RectFloat bounds);

        public abstract void zoom(float wheel);

        public abstract void center(float lon, float lat);

        public void untransform(GL2 gl, RectFloat bounds) {

        }
    }

    /** for debugging; this will be distorted, increasingly towards the poles (extreme latitudes) */
    public static final class RawProjection extends LonLatProjection {
        private final v2 center = new v2();

        /** TODO move scale, center, translate to a generic 2D projection impl */
        final FloatRange scale = new FloatRange(16f, 0.001f, 1000f);
        private float viewScale;

        @Override
        public void project(float lon, float lat, float alt, float[] target, int offset) {
            target[offset++] = lon;
            target[offset++] = lat;
            target[offset] = alt;
        }

        @Override
        public void unproject(float x, float y, float z, float[] target) {
            float scale = viewScale;
            target[0] = (x/scale+ center.x) ;
            target[1] = (y/scale+ center.y) ;
            target[2] = 0;
        }

        @Override
        public void transform(GL2 gl, RectFloat bounds) {

            viewScale = scale(bounds);

            gl.glScalef(viewScale, viewScale, 1);
            gl.glTranslatef(-center.x, -center.y, 0);

        }

        float scale(RectFloat bounds) {
            return this.scale.floatValue() * Math.max(bounds.w, bounds.h);
        }

        @Override
        public void pan(float tx, float ty, RectFloat bounds) {
            float speed = 0.5f;
            float s =
                    //1f / viewScale;
                    (speed / viewScale);
            center.added(tx*s, ty*s);
        }

        @Override
        public void zoom(float wheel) {
            scale.multiply(1f - wheel * 0.1f);
        }

        @Override
        public void center(float lon, float lat) {
            center.set(lon, lat);
        }
    }

    public static final class ECEFProjection extends LonLatProjection {

        public final v3 camFwd = new v3(), camUp = new v3();
        final v3 camPos = new v3(0,0,-10), rot = new v3();

        private final float[] mat4f = new float[16];

        final FloatRange scale = new FloatRange(1/(50_000.0f), 1/250_000f, 1/10_000f);

        {
            camPos.set(0, 0, 0);
        }

        @Override
        public void unproject(float x, float y, float z, float[] target) {
            //TODO
            target[0] = x;
            target[1] = y;
        }

        @Override
        public void project(float lon, float lat, float alt, float[] target, int offset) {

            double[] d = ECEF.latlon2ecef(lat , lon , alt); //HACK
            target[offset++] = (float)(d[0]); //HACK
            target[offset++] = (float)(d[1]); //HACK
            target[offset]   = (float)(d[2]); //HACK
        }

        @Override
        public void transform(GL2 gl, RectFloat bounds) {


            camUp.set(0, 1, 0);
            camFwd.set(0, 0, -1);

            gl.glMatrixMode(GL_PROJECTION);
            gl.glPushMatrix();
            gl.glLoadIdentity();
            float zNear = 0.5f, zFar = 1200;
            float tanFovV = (float) Math.tan(45 * FloatUtil.PI / 180.0f / 2f);
            float aspect =
                    //1;
                    //bounds.h / bounds.w;
                    bounds.w / bounds.h;
            float top = tanFovV * zNear;
            float right = aspect * top;
            float bottom = -top;
            float left = -right;

            gl.glMultMatrixf(FloatUtil.makePerspective(mat4f, 0, true, 45 * FloatUtil.PI / 180.0f, aspect, zNear, zFar), 0);

            Draw.glu.gluLookAt(0 - camFwd.x, 0 - camFwd.y, 0 - camFwd.z,
                    0, 0, 0,
                    camUp.x, camUp.y, camUp.z);

            gl.glMatrixMode(GL_MODELVIEW);
            gl.glPushMatrix();
            gl.glLoadIdentity();

            System.out.println(camPos + " " + rot);

            gl.glTranslatef(camPos.x, camPos.y, camPos.z);

            gl.glRotatef(rot.x, 0, 0, 1);
            gl.glRotatef(rot.y, 1, 0, 0);

//            System.out.println(scale);
            float scale = this.scale.floatValue();
            gl.glScalef(scale,scale,scale);



            //debug:
            gl.glColor4f(1,1,1, 0.5f);

            //gl.glLineWidth(2);
            //gl.glBegin(GL_LINE_STRIP );

            gl.glPointSize(4);
            gl.glBegin(GL_POINTS);
            float[] ff = new float[3];
            for (int lat = -90; lat < +90; lat+=10) {
                for (int lon = -180; lon < +180; lon+=10) {
                    project(lon, lat, 0, ff, 0);
                    gl.glVertex3fv(ff, 0);
                }
            }
            gl.glEnd();

//            gl.glMatrixMode(GL_PROJECTION);
//            gl.glLoadIdentity();
//            gl.glOrtho(0, w, 0, h, -1.5, 1.5);
//            gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
//            gl.glLoadIdentity();
        }

        @Override
        public void untransform(GL2 gl, RectFloat bounds) {
            gl.glPopMatrix();
            gl.glMatrixMode(GL_PROJECTION);
            gl.glPopMatrix();
            gl.glMatrixMode(GL_MODELVIEW);
        }

        @Override
        public void pan(float tx, float ty, RectFloat bounds) {
//            camPos.x = tx * 1;
//            camPos.z = ty * 1;
            rot.x += tx / 10f;
            rot.y += ty / 10f;
        }

        @Override
        public void zoom(float wheel) {
            //scale.multiply(1f + (wheel) * 0.1f);
            camPos.z += wheel * 30;
        }

        @Override
        public void center(float lon, float lat) {

        }
    }

    public static class OsmRenderer implements GLUtessellatorCallback, Consumer<GL2> {
        public final GLUtessellator tobj = GLU.gluNewTess();
        private final LonLatProjection project;

        @Deprecated
        public transient GL2 gl;
        public List<Consumer<GL2>> draw = new FasterList();

        boolean wireframe = false;

        public List<Consumer<GL2>> dbuf = new FasterList();
        private final FloatArrayList vbuf = new FloatArrayList(8*1024);
        private int nextType;

        public OsmRenderer(GL2 gl, LonLatProjection project) {
            this.gl = gl;
            this.project = project;

            GLU.gluTessCallback(tobj, GLU.GLU_TESS_VERTEX, this);
            GLU.gluTessCallback(tobj, GLU.GLU_TESS_BEGIN, this);
            GLU.gluTessCallback(tobj, GLU.GLU_TESS_END, this);
            GLU.gluTessCallback(tobj, GLU.GLU_TESS_ERROR, this);
            GLU.gluTessCallback(tobj, GLU.GLU_TESS_COMBINE, this);

        }

        public void clear() {
            draw.clear();
        }

        public void addNode(OsmNode node) {
            Map<String, String> tags = node.tags;

            float pointSize = 1;
            float r = 0.5f, g = 0.5f, b = 0.5f, a = 1f;
            if (tags!=null && !tags.isEmpty()) {
                String highway = tags.get("highway");
                String natural = tags.get("natural");

                if ("bus_stop".equals(highway)) {

                    pointSize = 3;
                    r = g = b = 1f;
                    a = 0.7f;
                } else if ("traffic_signals".equals(highway)) {
                    pointSize = 3;
                    r = g = 1f;
                    b = 0f;
                    a = 0.7f;

                } else if ("tree".equals(natural) || "scrub".equals(natural)) {
                    pointSize = 3;
                    g = 1f;
                    r = b = 0f;
                    a = 0.7f;

                } else {
                    pointSize = 3;
                    r = 1f;
                    g = b = 0f;
                    a = 0.7f;
                }
            }

            float[] c3 = new float[3];

            project.project(node.pos, c3);

            draw.add(new OsmDrawPoint(pointSize, r, g, b, a, c3));
        }

        public void addWay(OsmWay w) {

            Map<String, String> tags = w.tags;


            boolean isPolygon = false;
            float lw = 1f;
            short ls = (short) 0xffff;
            float r = 0.5f;
            float g = 0.5f;
            float b = 0.5f;
            float a = 1f;

            if (tags!=null && !tags.isEmpty()) {


                for (Map.Entry<String, String> entry : tags.entrySet()) {
                    String k = entry.getKey(), v = entry.getValue();
                    switch (k) {
                        case "building":
                            switch (v) {
                                case "commercial":
                                    r = 0.25f;
                                    g = 0.5f;
                                    b = 0.5f;
                                    a = 1f;
                                    lw = 2f;
                                    isPolygon = true;
                                    break;
                                default:
                                    r = 0.5f;
                                    g = 0.25f;
                                    b = 0.5f;
                                    a = 1f;
                                    lw = 1f;
                                    isPolygon = true;
                                    break;
                            }
                            break;
                        case "barrier":
                            r = 0.5f;
                            g = 0.5f;
                            b = 0.2f;
                            a = 1f;
                            lw = 1f;
                            break;
                        case "waterway":
                            switch (v) {
                                case "stream":
                                case "river":
                                    r = 0f;
                                    g = 0f;
                                    b = 1f;
                                    a = 1f;
                                    lw = 1f;
                                    break;
                            }
                            break;
                        case "natural":
                            switch (v) {
                                case "wetland":
                                case "water":
                                    r = 0f;
                                    g = 0f;
                                    b = 1f;
                                    a = 1f;
                                    lw = 1f;
                                    isPolygon = true;
                                    break;
                                case "scrub":
                                    r = 0f;
                                    g = 0.5f;
                                    b = 0f;
                                    a = 1f;
                                    lw = 1f;
                                    isPolygon = true;
                                    break;
                                case "valley":
                                    r = 0.5f;
                                    g = 0.5f;
                                    b = 0f;
                                    a = 1f;
                                    lw = 1f;
                                    isPolygon = true;
                                    break;
                                case "wood":
                                    r = 0f;
                                    g = 1f;
                                    b = 0f;
                                    a = 1f;
                                    lw = 1f;
                                    isPolygon = true;
                                    break;
                                default:
                                    System.out.println("unstyled: " + k + " = " + v);
                                    break;
                            }
                            break;
                        case "leisure":
                            switch (v) {
                                case "park":
                                    r = 0.1f;
                                    g = 0.9f;
                                    b = 0f;
                                    a = 0.25f;
                                    lw = 1f;
                                    isPolygon = true;
                                    break;
                            }
                            break;
                        case "landuse":
                            switch (v) {
                                case "forest":
                                case "grass":
                                case "recreation_ground":
                                    r = 0.1f;
                                    g = 0.9f;
                                    b = 0f;
                                    a = 1f;
                                    lw = 1f;
                                    isPolygon = true;
                                    break;
//                                case "industrial":
//                                    break;
//                                default:
//                                    System.out.println("unstyled: " + k + " = " + v);
//                                    break;
                            }
                            break;
                        case "route":
                            switch (v) {

//                                case "sidewalk":
//                                    r = 0f;
//                                    g = 0.5f;
//                                    b = 0f;
//                                    a = 1f;
//                                    lw = v.equals("both") ? 1.5f : 0.75f;
//                                    break;
                                case "road":
                                    r = 1f;
                                    g = 1f;
                                    b = 1f;
                                    a = 1f;
                                    lw = 1f;
                                    break;
                                case "train":
                                    r = 1f;
                                    g = 1f;
                                    b = 1f;
                                    a = 1f;
                                    lw = 5f;
                                    ls = (short) 0xF0F0;
                                    break;
                                default:
                                    System.out.println("unstyled: " + k + " = " + v);
                                    break;
                            }
                            break;

                        case "highway":
                            switch (v) {
                                case "service":
                                    r = 1f;
                                    g = 0f;
                                    b = 1f;
                                    a = 1f;
                                    lw = 2f;
                                    break;
                                case "path":
                                case "pedestrian":
                                    r = 0f;
                                    g = 0.5f;
                                    b = 0f;
                                    a = 1f;
                                    lw = 2f;
                                    break;
                                case "motorway_link":
                                case "motorway":
                                    r = 1f;
                                    g = 0.5f;
                                    b = 0f;
                                    a = 1f;
                                    lw = 5f;
                                    break;
                                case "tertiary":
                                    r = 0.7f;
                                    g = 0.5f;
                                    b = 0f;
                                    a = 1f;
                                    lw = 4f;
                                    break;
                                default:
                                    r = 1f;
                                    g = 1f;
                                    b = 1f;
                                    a = 0.5f;
                                    lw = 3f;
                                    break;
                            }
                            break;
                        default:
//                            System.out.println("unstyled: " + k + " = " + v + "(" + tags + ")");
                            break;
                    }
                }

            }

            isPolygon = isPolygon && w.isLoop();

            if (isPolygon && !wireframe) {
                List nn = w.getOsmNodes();
                int s = nn.size();
                if (s > 0) {
                    float[][] coord = new float[s][7];
                    for (int i = 0; i < s; i++) {
                        float[] ci = project.project(((OsmNode)nn.get(i)).pos, coord[i]);
                        ci[3] = r;
                        ci[4] = g;
                        ci[5] = b;
                        ci[6] = a;
                    }

                    draw.add(new OsmPolygonDraw(r, g, b, a, lw, ls, this, nn, coord));
                }


            } else {

                List ways = w.getOsmNodes();
                int ws = ways.size();
                if (ws > 0) {
                    float[] c3 = new float[3 * ws];
                    for (int i = 0, waysSize = ws; i < waysSize; i++) {
                        project.project(((OsmNode)ways.get(i)).pos, c3, i * 3);
                    }

                    draw.add(new OsmLineDraw(r, g, b, a, lw, ls, c3));
                }

            }
        }
        @Override
        public void begin(int type) {
            //gl.glBegin(type);
            nextType = type;
        }

        @Override
        public void end() {
            float[] coord = vbuf.toArray();
            vbuf.clear();
            int myNextType = nextType;
            dbuf.add((gl)->{
                gl.glBegin(myNextType);
                int ii = 0;
                for (int i = 0; i < coord.length / 7; i++) {
                    gl.glColor4fv(coord, ii); ii+=4;
                    gl.glVertex3fv(coord, ii); ii+=3;
                }
                gl.glEnd();
            });
            //gl.glEnd();
        }



        @Override
        public void vertex(Object vertexData) {
            if (vertexData instanceof double[]) {

                double[] pointer = (double[]) vertexData;

                if (pointer.length >= 7) {
                    //gl.glColor3dv(pointer, 3);
                    vbuf.add((float) pointer[3]);
                    vbuf.add((float) pointer[4]);
                    vbuf.add((float) pointer[5]);
                    vbuf.add((float) pointer[6]);
                } else {
                    vbuf.add(1);
                    vbuf.add(1);
                    vbuf.add(1);
                    vbuf.add(1);
                }
                //gl.glVertex3dv(pointer, 0);

                vbuf.add((float) pointer[0]);
                vbuf.add((float) pointer[1]);
                vbuf.add((float) pointer[2]);


            } else if (vertexData instanceof float[]) {
                float[] pointer = (float[]) vertexData;

                if (pointer.length >= 7) {
//                    gl.glColor3fv(pointer, 3);
                    vbuf.add(pointer[3]);
                    vbuf.add(pointer[4]);
                    vbuf.add(pointer[5]);
                    vbuf.add(pointer[6]);
                } else {
                    vbuf.add(1);
                    vbuf.add(1);
                    vbuf.add(1);
                    vbuf.add(1);
                }

                //gl.glVertex3fv(pointer, 0);
                vbuf.add(pointer[0]);
                vbuf.add(pointer[1]);
                vbuf.add(pointer[2]);

            } else
                throw new UnsupportedOperationException();
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
            float[] vertex = new float[7];

            vertex[0] = (float) coords[0];
            vertex[1] = (float) coords[1];
            vertex[2] = (float) coords[2];
            for (int cc = 3; cc < 7; cc++) {
                float v = 0;
                for (int dd = 0; dd < data.length; dd++) {
                    float[] d = (float[]) data[dd];
                    if (d != null) {
                        v += weight[dd] * d[cc];
                    }
                }
                vertex[cc] = v;
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
            for (Consumer<GL2> d : draw) {
                d.accept(g);
            }
        }
    }

//    public OsmSpace(IRL irl) {
//
//        this.irl = irl;



//        double minLat = osm.geoBounds.minLat;
//        double minLon = osm.geoBounds.minLon;
//        double maxLat = osm.geoBounds.maxLat;
//        double maxLon = osm.geoBounds.maxLon;
//        scaleLat = (float) ((maxLat - minLat));
//        scaleLon = (float) ((maxLon - minLon));
//        scaleLat = scaleLon = Math.max(scaleLat, scaleLon);
//        center = new GeoVec3((maxLat + minLat) / 2, (maxLon + minLon) / 2);

//    }
//
//    public OsmVolume volume() {
//        return new OsmVolume();
//    }

//    public static class OsmVolume extends AbstractSpatial<Osm> {
//
//        OsmVolume() {
//            super(null);
//        }
//
//        @Override
//        public void forEachBody(Consumer<Collidable> c) {
//
//        }
//
//        @Override
//        public float radius() {
//            return 0;
//        }
//
//        @Override
//        public void renderAbsolute(GL2 gl, int dtMS) {
////            if (mapRender == null) {
////                mapRender = compileMap(gl, osm);
////            }
////            mapRender.render(gl);
//        }
//
//    }

    private static class OsmPolygonDraw implements Consumer<GL2> {
        private final float r,g,b,a;
        private final float lw;
        private final short ls;
        //private final float[] coord;
        private final Consumer<GL2> draw;

        OsmPolygonDraw(float r, float g, float b, float a, float lw, short ls, OsmRenderer s, List<OsmNode> nn, float[][] coord) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.lw = lw;
            this.ls = ls;

            GLUtessellator tobj = s.tobj;

            GLU.gluTessBeginPolygon(tobj, null);
            GLU.gluTessBeginContour(tobj);
            for (float[] ci : coord) {
                GLU.gluTessVertex(tobj, Util.toDouble(ci), 0, ci);
            }
            GLU.gluTessEndContour(tobj);
            GLU.gluTessEndPolygon(tobj);

            Consumer<GL2>[] draws = s.dbuf.toArray(ArrayUtil.EMPTY_CONSUMER_ARRAY);
            if (draws.length == 1)
                this.draw = draws[0];
            else {
                this.draw = G ->{
                    for (Consumer<GL2> d : draws)
                        d.accept(G);
                };
            }

            s.dbuf.clear();
        }

        @Override
        public void accept(GL2 gl) {
            gl.glColor4f(r, g , b, a);
            gl.glLineWidth(lw);
            gl.glLineStipple(1, ls);

            draw.accept(gl);
        }
    }

    private static class OsmLineDraw implements Consumer<GL2> {
        private final float r,g,b,a;
        private final float lw;
        private final short ls;
        private final float[] c3;

        public OsmLineDraw(float r, float g, float b, float a, float lw, short ls, float[] c3) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.lw = lw;
            this.ls = ls;
            this.c3 = c3;
        }

        @Override
        public void accept(GL2 gl) {
            gl.glColor4f(r, g, b, a);
            gl.glLineWidth(lw);
            gl.glLineStipple(1, ls);
            gl.glBegin(GL_LINE_STRIP);
            for (int i = 0; i < c3.length / 3; i++)
                gl.glVertex3fv(c3, i * 3);
            gl.glEnd();
        }
    }

    private static class OsmDrawPoint implements Consumer<GL2> {
        private final float pointSize;
        private final float r, g, b, a;
        private final float[] c3;

        public OsmDrawPoint(float pointSize, float r, float g, float b, float a, float[] c3) {
            this.pointSize = pointSize;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.c3 = c3;
        }

        @Override
        public void accept(GL2 gl) {
            gl.glPointSize(pointSize);
            gl.glBegin(GL_POINTS);
            gl.glColor4f(r, g, b, a);
            gl.glVertex3fv(c3, 0);
            gl.glEnd();
        }
    }


}
