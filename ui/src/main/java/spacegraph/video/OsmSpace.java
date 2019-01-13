package spacegraph.video;


import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;
import com.jogamp.opengl.glu.GLUtessellatorCallback;
import jcog.Util;
import jcog.data.list.FasterList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.util.geo.osm.GeoVec3;
import spacegraph.util.geo.osm.OsmNode;
import spacegraph.util.geo.osm.OsmWay;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.jogamp.opengl.GL.GL_LINE_STRIP;
import static com.jogamp.opengl.GL.GL_POINTS;

/**
 * OSM Renderer context
 * Created by unkei on 2017/04/25.
 */
public enum OsmSpace  { ;

    public static final Logger logger = LoggerFactory.getLogger(OsmSpace.class);


    public static abstract class LonLatProjection {

        private boolean changed = false;

        abstract public void project(float lon, float lat, float alt, float[] target, int offset);

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

        /** this will be distorted, increasingly towards the poles (extreme latitudes) */
        public static final LonLatProjection Raw = new LonLatProjection() {
            @Override
            public void project(float lon, float lat, float alt, float[] target, int offset) {
                target[offset++] = lon;
                target[offset++] = lat;
                target[offset] = alt;
            }
        };
    }

    /** TODO */
    abstract public static class ECEFProjection extends LonLatProjection {

    }

    public static class OsmRenderer implements GLUtessellatorCallback, Consumer<GL2> {
        public final GLUtessellator tobj = GLU.gluNewTess();
        private final LonLatProjection project;

        @Deprecated
        public transient GL2 gl;
        public List<Consumer<GL2>> draw = new FasterList();;

        boolean wireframe = false;

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

                } else if ("tree".equals(natural)) {
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
                            r = 0f;
                            g = 1f;
                            b = 1f;
                            a = 1f;
                            lw = 1f;
                            isPolygon = true;
                            break;
                        case "natural":
                            switch (v) {
                                case "water":
                                    r = 0f;
                                    g = 0f;
                                    b = 1f;
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
//                        case "landuse":
//                            switch (v) {
//                                case "forest":
//                                case "grass":
//                                    r = 0.1f;
//                                    g = 0.9f;
//                                    b = 0f;
//                                    a = 1f;
//                                    lw = 1f;
//                                    isPolygon = true;
//                                    break;
//                                case "industrial":
//                                    break;
//                                default:
//                                    System.out.println("unstyled: " + k + " = " + v);
//                                    break;
//                            }
//                            break;
                        case "route":
                            switch (v) {
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
                                case "pedestrian":
                                    r = 0f;
                                    g = 0.5f;
                                    b = 0f;
                                    a = 1f;
                                    lw = 2f;
                                    break;
                                case "motorway":
                                    r = 1f;
                                    g = 0.5f;
                                    b = 0f;
                                    a = 1f;
                                    lw = 5f;
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

                    draw.add(new OsmPolygonDraw(r, g, b, a, lw, ls, tobj, nn, coord));
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
        private final GLUtessellator tobj;
        private final List<OsmNode> nn;
        private final float[][] coord;

        OsmPolygonDraw(float r, float g, float b, float a, float lw, short ls, GLUtessellator tobj, List<OsmNode> nn, float[][] coord) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.lw = lw;
            this.ls = ls;
            this.tobj = tobj;
            this.nn = nn;
            this.coord = coord;
        }

        @Override
        public void accept(GL2 gl) {
            gl.glColor4f(r, g , b, a);
            gl.glLineWidth(lw);
            gl.glLineStipple(1, ls);

            GLU.gluTessBeginPolygon(tobj, null);
            GLU.gluTessBeginContour(tobj);
            for (int i = 0, nnSize = nn.size(); i < nnSize; i++) {
                float[] ci = coord[i];
                GLU.gluTessVertex(tobj, Util.toDouble(ci), 0, ci);
            }
            GLU.gluTessEndContour(tobj);
            GLU.gluTessEndPolygon(tobj);

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
            for (int i = 0; i < c3.length / 3; i++) {
                gl.glVertex3fv(c3, i * 3);
            }
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

            gl.glVertex3f(c3[0], c3[1], c3[2]);
            gl.glEnd();
        }
    }


}
