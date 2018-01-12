package spacegraph.geo;


import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;
import com.jogamp.opengl.glu.GLUtessellatorCallback;
import jcog.list.FasterList;
import spacegraph.SpaceGraph;
import spacegraph.geo.data.GeoCoordinate;
import spacegraph.geo.data.Osm;
import spacegraph.geo.data.OsmNode;
import spacegraph.geo.data.OsmWay;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.jogamp.opengl.GL.GL_LINE_STRIP;
import static com.jogamp.opengl.GL.GL_POINTS;

/**
 * Created by unkei on 2017/04/25.
 */
public class OsmViewer extends SpaceGraph implements GLUtessellatorCallback {

    Osm osm;
    //    double scale = 0.3; //global scale
//    double scaleLat = 1;
//    double scaleLon = 1;
//    GeoCoordinate center;
    final GeoCoordinate min;
    final GeoCoordinate max;

    boolean wireframe;

    final GLUtessellator tobj = GLU.gluNewTess();

    Consumer<OsmViewer> render = null;

    public OsmViewer(Osm osm) {
        super();
        min = new GeoCoordinate(0, 0);
        max = new GeoCoordinate(0, 0);

        GLU.gluTessCallback(tobj, GLU.GLU_TESS_VERTEX, this);
        GLU.gluTessCallback(tobj, GLU.GLU_TESS_BEGIN, this);
        GLU.gluTessCallback(tobj, GLU.GLU_TESS_END, this);
        GLU.gluTessCallback(tobj, GLU.GLU_TESS_ERROR, this);
        GLU.gluTessCallback(tobj, GLU.GLU_TESS_COMBINE, this);


        setOsm(osm);

    }

    public void setOsm(Osm osm) {
        this.osm = osm;
        double minLat = osm.bounds.minLat;
        double minLon = osm.bounds.minLon;
        double maxLat = osm.bounds.maxLat;
        double maxLon = osm.bounds.maxLon;
//        scaleLat = scale * 2f / (maxLat - minLat);
//        scaleLon = scale * 2f / (maxLon - minLon);
//        center = new GeoCoordinate((maxLat + minLat) / 2, (maxLon + minLon) / 2);
    }

    static void project(GeoCoordinate global, double[] target) {
        project(global, target, 0);
    }

    static void project(GeoCoordinate global, double[] target, int offset) {

//        //2D flat projection
//        target[0] = (global.latitude - center.latitude) * scaleLat;
//        target[1] = (global.longitude - center.longitude) * scaleLon;
//        target[2] = 0;

        //3D ECEF
        double[] t = ECEF.latlon2ecef(global.latitude * 20, global.longitude * 20, global.altitude);
        double s = 100 * 1E-7;
        target[offset++] = t[0] * s;
        target[offset++] = t[1] * s;
        target[offset/*++*/] = t[2] * s;

    }


    protected void compile() {
        List<Consumer<OsmViewer>> draw = new FasterList();

        for (OsmWay way : osm.ways) {

            Map<String, String> tags = way.tags;
            String building, building_part, landuse, natural, route, highway;
            if (!tags.isEmpty()) {

                building = tags.get("building");
                building_part = tags.get("building:part");
                landuse = tags.get("landuse");
                natural = tags.get("natural");
                route = tags.get("route");
                highway = tags.get("highway");
            } else {
                building = building_part = landuse = natural = route = highway = null;
            }

            boolean isPolygon = false;
            boolean isClosed = way.isClosed();
            float r, g, b, a;
            float lw;
            short ls;

            if (building != null || building_part != null) {
                r = 0f;
                g = 1f;
                b = 1f;
                a = 1f;
                lw = 1f;
                ls = (short) 0xFFFF;
                isPolygon = !wireframe && isClosed;
            } else if ("forest".equals(landuse) || "grass".equals(landuse) || "wood".equals(natural)) {
                r = 0f;
                g = 1f;
                b = 0f;
                a = 1f;
                lw = 1f;
                ls = (short) 0xFFFF;
                isPolygon = !wireframe && isClosed;
            } else if ("water".equals(natural)) {
                r = 0f;
                g = 0f;
                b = 1f;
                a = 1f;
                lw = 1f;
                ls = (short) 0xFFFF;
                isPolygon = !wireframe && isClosed;
            } else if ("pedestrian".equals(highway)) {
                r = 0f;
                g = 0.5f;
                b = 0f;
                a = 1f;
                lw = 2f;
                ls = (short) 0xFFFF;
            } else if ("motorway".equals(highway)) {
                r = 1f;
                g = 0.5f;
                b = 0f;
                a = 1f;
                lw = 5f;
                ls = (short) 0xFFFF;
            } else if (highway != null) {
                r = 1f;
                g = 1f;
                b = 1f;
                a = 1f;
                lw = 3f;
                ls = (short) 0xFFFF;
            } else if ("road".equals(route)) {
                r = 1f;
                g = 1f;
                b = 1f;
                a = 1f;
                lw = 1f;
                ls = (short) 0xFFFF;
            } else if ("train".equals(route)) {
                r = 1f;
                g = 1f;
                b = 1f;
                a = 1f;
                lw = 5f;
                ls = (short) 0xF0F0;
            } else {
                r = 0.5f;
                g = 0f;
                b = 0.5f;
                a = 1f;
                lw = 1f;
                ls = (short) 0xFFFF;
            }


            if (isPolygon) {
                List<OsmNode> nn = way.getOsmNodes();
                double coord[][] = new double[nn.size()][7];
                for (int i = 0, nnSize = nn.size(); i < nnSize; i++) {
                    OsmNode node = nn.get(i);

                    double[] ci = coord[i];
                    project(node.geoCoordinate, ci);
                    ci[3] = r;
                    ci[4] = g;
                    ci[5] = b;
                    ci[6] = a;
                }

                draw.add((v) -> {
                    GL2 gl = v.gl;
                    gl.glColor4f(r * 0.5f, g * .5f, b * 0.5f, a);
                    gl.glLineWidth(lw);
                    gl.glLineStipple(1, ls);

                    GLU.gluTessBeginPolygon(tobj, null);
                    GLU.gluTessBeginContour(tobj);
                    for (int i = 0, nnSize = nn.size(); i < nnSize; i++) {
                        double[] ci = coord[i];
                        GLU.gluTessVertex(tobj, ci, 0, ci);
                    }
                    GLU.gluTessEndContour(tobj);
                    GLU.gluTessEndPolygon(tobj);

                });


            } else {

                List<OsmNode> ways = way.getOsmNodes();
                int ws = ways.size();
                double c3[] = new double[3 * ws];
                for (int i = 0, waysSize = ws; i < waysSize; i++) {
                    project(ways.get(i).geoCoordinate, c3, i * 3);
                }

                draw.add((v) -> {
                    GL2 gl = v.gl;
                    gl.glColor4f(r, g, b, a);
                    gl.glLineWidth(lw);
                    gl.glLineStipple(1, ls);
                    gl.glBegin(GL_LINE_STRIP);
                    for (int i = 0; i < c3.length/3; i++) {
                        gl.glVertex3dv(c3, i * 3);
                    }
                    gl.glEnd();
                });

            }

        }


        for (OsmNode node : osm.nodes) {
            Map<String, String> tags = node.tags;

            if (tags.isEmpty()) continue;
            String highway = tags.get("highway");
            String natural = tags.get("natural");

            float pointSize;
            float r, g, b, a;
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

            double[] c3 = new double[3];
            project(node.geoCoordinate, c3);

            draw.add((v) -> {
                GL2 gl = v.gl;

                gl.glPointSize(pointSize);
                gl.glBegin(GL_POINTS);
                gl.glColor4f(r, g, b, a);

                gl.glVertex3d(c3[0], c3[1], c3[2]);
                gl.glEnd();
            });

        }

        render = (v) -> draw.forEach(d -> d.accept(v));
    }

    @Override
    protected void render(int dtMS) {

        if (render == null) {
            compile();
        }

        render.accept(this);

        super.render(dtMS);
    }

//    @Override
//    public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int w, int h) {
//        System.out.printf("reshape(%d, %d, %d, %d)\n", x, y, w, h);
//
//        GL2 gl = glAutoDrawable.getGL().getGL2();
//        gl.glMatrixMode(GL_PROJECTION);
//        gl.glLoadIdentity();
//        gl.glViewport(x, y, w, h);
////        gl.glOrtho((float)-w/300, (float)w/300, (float)-h/300, (float)h/300, -1f, 1f);
////        gl.glOrtho(-1f, 1f, (float)-h/w, (float)h/w, -1f, 1f);
//        gl.glOrtho((float) -w / h, (float) w / h, -1f, 1f, -1f, 1f);
//
//    }
//
//    @Override
//    public void keyPressed(KeyEvent keyEvent) {
//        switch (keyEvent.getKeyCode()) {
//            case KeyEvent.VK_ESCAPE:
//                System.exit(0);
//                break;
//            case KeyEvent.VK_SPACE:
//                wireframe = !wireframe;
//                break;
//            default:
//                break;
//        }
//    }
//
//    @Override
//    public void keyReleased(KeyEvent keyEvent) {
//
//    }


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
    public void combine(double[] coords, Object[] data, //
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
    public void combineData(double[] coords, Object[] data, //
                            float[] weight, Object[] outData, Object polygonData) {
    }

    @Override
    public void error(int errnum) {

        String estring = glu.gluErrorString(errnum);
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
}
