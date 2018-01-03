package spacegraph.geo;


import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;
import com.jogamp.opengl.glu.GLUtessellatorCallback;
import com.jogamp.opengl.util.FPSAnimator;
import spacegraph.geo.data.GeoCoordinate;
import spacegraph.geo.data.Osm;
import spacegraph.geo.data.OsmNode;
import spacegraph.geo.data.OsmWay;

import java.util.Map;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;

/**
 * Created by unkei on 2017/04/25.
 */
public class OsmViewer implements GLEventListener, KeyListener {

    Osm osm;
    double scale = 0.1; //global scale
    
    double scaleLat = 1;
    double scaleLon = 1;
    GeoCoordinate center;
    final GeoCoordinate min;
    final GeoCoordinate max;

    boolean wireframe;
    private tessellCallBack tessCallback;
    private GLUtessellator tobj;

    public OsmViewer() {
        this(null);
    }

    public OsmViewer(Osm osm) {
        center = new GeoCoordinate(0, 0);
        min = new GeoCoordinate(0, 0);
        max = new GeoCoordinate(0, 0);
        setOsm(osm);

        GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2));
        GLWindow glWindow = GLWindow.create(caps);
        glWindow.setTitle("First demo (Newt)");
        glWindow.setSize(300, 300);
        glWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyed(WindowEvent windowEvent) {
                super.windowDestroyed(windowEvent);
                System.exit(0);
            }
        });
        glWindow.addGLEventListener(this);
        glWindow.addKeyListener(this);
        FPSAnimator animator = new FPSAnimator(15);
        animator.add(glWindow);
        animator.start();
        glWindow.setVisible(true);
    }

    public void setOsm(Osm osm) {
        this.osm = osm;
        double minLat = osm.bounds.minLat;
        double minLon = osm.bounds.minLon;
        double maxLat = osm.bounds.maxLat;
        double maxLon = osm.bounds.maxLon;
        scaleLat = scale * 2f / (maxLat - minLat);
        scaleLon = scale * 2f / (maxLon - minLon);
        center = new GeoCoordinate((maxLat + minLat) / 2, (maxLon + minLon) / 2);
    }

    void project(GeoCoordinate global, double[] target) {

        //2D flat projection
        target[0] = (global.latitude - center.latitude) * scaleLat;
        target[1] = (global.longitude - center.longitude) * scaleLon;
        target[2] = 0;


    }

    @Override
    public void init(GLAutoDrawable glAutoDrawable) {
        System.out.print("init\n");
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
        System.out.print("dispose\n");
    }


    @Override
    public void display(GLAutoDrawable glAutoDrawable) {
        GL2 gl = glAutoDrawable.getGL().getGL2();
        GLU glu = new GLU();
        gl.glClearColor(0f, 0f, 0f, 1f);
        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

//        gl.glEnable(GL_DEPTH_TEST);
        gl.glEnable(GL_BLEND);
        gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        gl.glMatrixMode(GL_MODELVIEW);
        gl.glLoadIdentity();
        // TODO: Enable anti-aliasing

        // rotating animation
        float tick = ((float) (System.currentTimeMillis() % 64000) / 64000); // 64000ms cycle
        float angle = 360f * tick;
        float rad = 2f * (float) Math.PI * tick;
        gl.glRotatef(angle, 0, 0f, 1f);
        gl.glTranslatef(0.5f * (float) Math.cos(rad), 0.5f * (float) Math.sin(rad), 0f);

        tessellCallBack tessCallback;
        GLUtessellator tobj = this.tobj;
        if (this.tessCallback == null) {
            tessCallback = this.tessCallback = new tessellCallBack(gl, glu);
            tobj = this.tobj = GLU.gluNewTess();
            GLU.gluTessCallback(tobj, GLU.GLU_TESS_VERTEX, tessCallback);
            GLU.gluTessCallback(tobj, GLU.GLU_TESS_BEGIN, tessCallback);
            GLU.gluTessCallback(tobj, GLU.GLU_TESS_END, tessCallback);
            GLU.gluTessCallback(tobj, GLU.GLU_TESS_ERROR, tessCallback);
            GLU.gluTessCallback(tobj, GLU.GLU_TESS_COMBINE, tessCallback);
        }
        double[] c3 = new double[3];


        // render elements
        for (OsmWay way : osm.ways) {

            Map<String, String> tags = way.tags;

            String building = tags.get("building");
            String building_part = tags.get("building:part");
            String landuse = tags.get("landuse");
            String natural = tags.get("natural");
            String route = tags.get("route");
            String highway = tags.get("highway");

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
                r *= 0.5f;
                g *= 0.5f;
                b *= 0.5f;
                gl.glColor4f(r, g, b, a);
                gl.glLineWidth(lw);
                gl.glLineStipple(1, ls);


                GLU.gluTessBeginPolygon(tobj, null);
                GLU.gluTessBeginContour(tobj);
                for (OsmNode node : way.getOsmNodes()) {

//                    gl.glVertex2f((float) local.longitude, (float) local.latitude);

                    double coord[] = new double[7];
                    project(node.getGeoCoordinate(), coord);
                    coord[3] = r;
                    coord[4] = g;
                    coord[5] = b;
                    coord[6] = a;

                    GLU.gluTessVertex(tobj, coord, 0, coord);
                }
                GLU.gluTessEndContour(tobj);
                GLU.gluTessEndPolygon(tobj);


            } else {
                gl.glColor4f(r, g, b, a);
                gl.glLineWidth(lw);
                gl.glLineStipple(1, ls);
                gl.glBegin(GL_LINE_STRIP);

                for (OsmNode node : way.getOsmNodes()) {
                    project(node.getGeoCoordinate(), c3);
                    gl.glVertex3d(c3[0], c3[1], c3[2]);
                }
                gl.glEnd();
            }
        }

        for (OsmNode node : osm.nodes) {
            Map<String, String> tags = node.tags;

            if (tags.isEmpty()) continue;

            String highway = tags.get("highway");
            String natural = tags.get("natural");
            if ("bus_stop".equals(highway)) {
                gl.glPointSize(3f);
                gl.glBegin(GL_POINTS);
                gl.glColor4f(1f, 1f, 1f, 0.7f);
            } else if ("traffic_signals".equals(highway)) {
                gl.glPointSize(3f);
                gl.glBegin(GL_POINTS);
                gl.glColor4f(1f, 1f, 0f, 0.7f);
            } else if ("tree".equals(natural)) {
                gl.glPointSize(3f);
                gl.glBegin(GL_POINTS);
                gl.glColor4f(0f, 1f, 0f, 0.7f);
            } else {
                gl.glPointSize(3f);
                gl.glBegin(GL_POINTS);
                gl.glColor4f(1f, 0, 0, 0.7f);
            }
            double[] coord = new double[3];
            project(node.getGeoCoordinate(), coord);
            gl.glVertex3d(coord[0], coord[1], coord[2]);
            gl.glEnd();
        }

//        gl.glDisable(GL_DEPTH_TEST);
        gl.glDisable(GL_BLEND);
    }

    @Override
    public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int w, int h) {
        System.out.printf("reshape(%d, %d, %d, %d)\n", x, y, w, h);

        GL2 gl = glAutoDrawable.getGL().getGL2();
        gl.glMatrixMode(GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glViewport(x, y, w, h);
//        gl.glOrtho((float)-w/300, (float)w/300, (float)-h/300, (float)h/300, -1f, 1f);
//        gl.glOrtho(-1f, 1f, (float)-h/w, (float)h/w, -1f, 1f);
        gl.glOrtho((float) -w / h, (float) w / h, -1f,  1f, -1f, 1f);

    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        switch (keyEvent.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                System.exit(0);
                break;
            case KeyEvent.VK_SPACE:
                wireframe = !wireframe;
                break;
            default:
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {

    }

    static class tessellCallBack implements GLUtessellatorCallback {
        private final GL2 gl;
        private final GLU glu;

        tessellCallBack(GL2 gl, GLU glu) {
            this.gl = gl;
            this.glu = glu;
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
}
