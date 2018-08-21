package spacegraph.space3d.widget;


import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;
import com.jogamp.opengl.glu.GLUtessellatorCallback;
import jcog.data.list.FasterList;
import jcog.math.FloatRange;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.FingerMove;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.windo.Widget;
import spacegraph.space3d.AbstractSpatial;
import spacegraph.space3d.phys.Collidable;
import spacegraph.util.geo.osm.GeoCoordinate;
import spacegraph.util.geo.osm.Osm;
import spacegraph.util.geo.osm.OsmNode;
import spacegraph.util.geo.osm.OsmWay;
import spacegraph.util.math.v2;
import spacegraph.video.Draw;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.jogamp.opengl.GL.GL_LINE_STRIP;
import static com.jogamp.opengl.GL.GL_POINTS;

/**
 * Created by unkei on 2017/04/25.
 */
public class OsmSpace implements GLUtessellatorCallback {



    



    private final GeoCoordinate min;
    private final GeoCoordinate max;
    private final Osm osm;
    private final GeoCoordinate center;

    private float scaleLat = 1;
    private float scaleLon = 1;

    private boolean wireframe;

    private final GLUtessellator tobj = GLU.gluNewTess();

    private Consumer<GL2> render = null;
    private GL2 gl;

    public OsmSpace(Osm osm) {
        this.osm = osm;
        min = new GeoCoordinate(0, 0);
        max = new GeoCoordinate(0, 0);

        GLU.gluTessCallback(tobj, GLU.GLU_TESS_VERTEX, this);
        GLU.gluTessCallback(tobj, GLU.GLU_TESS_BEGIN, this);
        GLU.gluTessCallback(tobj, GLU.GLU_TESS_END, this);
        GLU.gluTessCallback(tobj, GLU.GLU_TESS_ERROR, this);
        GLU.gluTessCallback(tobj, GLU.GLU_TESS_COMBINE, this);


        double minLat = osm.bounds.minLat;
        double minLon = osm.bounds.minLon;
        double maxLat = osm.bounds.maxLat;
        double maxLon = osm.bounds.maxLon;
        scaleLat = (float) ( (maxLat - minLat));
        scaleLon = (float) ( (maxLon - minLon));
        scaleLat = scaleLon = Math.max(scaleLat, scaleLon); 
        center = new GeoCoordinate((maxLat + minLat) / 2, (maxLon + minLon) / 2);

    }


    private void project(GeoCoordinate global, double[] target) {
        project(global, target, 0);
    }

    private void project(GeoCoordinate global, double[] target, int offset) {

        
        target[offset++] = (global.latitude - center.latitude);
        target[offset++] = (global.longitude - center.longitude);
        target[offset/*++*/] = 0;








    }

    public Surface surface() {
        return new OsmSurface();

    }

    public OsmVolume volume() {
        return new OsmVolume();
    }

    public class OsmSurface extends Widget  {

        final FloatRange scale = new FloatRange(1f, 0.001f, 1000f);
        final v2 translate= new v2();

        final FingerMove pan = new FingerMove(0) {


            private v2 translateStart;

            @Override
            protected boolean startDrag(Finger f) {
                translateStart = translate.clone();
                return super.startDrag(f);
            }

            @Override
            public void move(float tx, float ty) {
                
                float scale = OsmSurface.this.scale.floatValue()/2;
                translate.set((translateStart.x+tx*scale), (translateStart.y+ty*scale));
            }
        };

        @Override
        public Surface tryTouch(Finger finger) {
            float wheel;
            if ((wheel = finger.rotationY())!=0) {
                
                scale.multiply( (1f - wheel*0.1f) );
            }

            if (finger.tryFingering(pan)) {
            }

            return this;
        }

        @Override
        protected void paintBelow(GL2 gl) {

        }

        @Override
        protected void paintWidget(GL2 gl, RectFloat2D bounds) {
            if (render == null) {
                render = compile(gl, osm);
            }

            gl.glPushMatrix();
            gl.glTranslatef( translate.x + bounds.x+bounds.w/2, translate.y + bounds.y+bounds.h/2, 0);

            float baseScale = Math.max(bounds.w, bounds.h);
            float scale = this.scale.floatValue();
            gl.glScalef(baseScale * scale, baseScale * scale, 1);

            render.accept(gl);

            gl.glPopMatrix();
        }
    }

    public class OsmVolume extends AbstractSpatial<Osm> {

        OsmVolume() {
            super(osm);
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
            if (render == null) {
                render = compile(gl, osm);
            }
            render.accept(gl);
        }

    }

    private Consumer<GL2> compile(GL2 _gl, Osm osm) {
        this.gl = _gl;
        List<Consumer<GL2>> draw = new FasterList();

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

                draw.add((gl) -> {
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

                draw.add((gl) -> {
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

            draw.add((gl) -> {
                gl.glPointSize(pointSize);
                gl.glBegin(GL_POINTS);
                gl.glColor4f(r, g, b, a);

                gl.glVertex3d(c3[0], c3[1], c3[2]);
                gl.glEnd();
            });

        }

        return (v) -> draw.forEach(d -> d.accept(v));
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
}
