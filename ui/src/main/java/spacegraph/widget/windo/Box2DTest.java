package spacegraph.widget.windo;

import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body2D;
import org.jbox2d.dynamics.Dynamics2D;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.MouseJoint;
import org.jbox2d.dynamics.joints.MouseJointDef;
import org.jbox2d.fracture.PolygonFixture;
import spacegraph.math.Tuple2f;
import spacegraph.math.v2;
import spacegraph.render.JoglSpace;
import spacegraph.test.WidgetTest;
import spacegraph.widget.button.PushButton;

public class Box2DTest {


    public static class Box2DTest1_Boxes {

        public static void main(String[] args) {
            PhyWall d = new PhyWall();

            JoglSpace.window(d, 800, 800);


            {
                d.newWindow(WidgetTest.widgetDemo(), RectFloat2D.XYWH(-250, 0, 150, 320));
                d.newWindow(WidgetTest.widgetDemo(), RectFloat2D.XYWH(+400, 0, 300, 100));
//            Windo.Port p = w.addPort("X");
            }

//        d.addWindo(grid(new PushButton("x"), new PushButton("y"))).pos(10, 10, 50, 50);

            for (int i = 0; i < 8; i++) {
                float rx = (float) (Math.random() * 1000f / 2);
                float ry = (float) (Math.random() * 1000f / 2);
                float rw = 55 + 150 * (float) Math.random();
                float rh = 50 + 150 * (float) Math.random();
                d.newWindow(new PushButton(String.valueOf((char) ('w' + i))), RectFloat2D.XYWH(rx, ry, rw, rh));
            }

            //d.newWindo(grid(new PushButton("x"), new PushButton("y"))).pos(-100, -100, 0, 0);
            //d.children.add(new GridTex(16).pos(0,0,1000,1000));


        }
    }

    public static class Box2DTest2_Joints {
        public static void main(String[] args) {

            PhyWall s = new PhyWall() {


                @Override
                protected void paintBelow(GL2 gl) {
                    super.paintBelow(gl);
                    //vykresli tuhe telesa
                    Dynamics2D w = this.W;
                    for (Body2D b = w.bodies(); b != null; b = b.next()) {
                        drawBody(b, gl);
                    }

                    //vykresli joiny
                    for (Joint j = w.joints(); j != null; j = j.next) {
                        drawJoint(j, gl);
                    }
                }

                private void drawJoint(Joint j, GL2 gl) {
                    //TODO
                }

                private void drawBody(Body2D body, GL2 gl) {
//                    if (body.getType() == BodyType.DYNAMIC) {
//                        g.setColor(Color.LIGHT_GRAY);
//                    } else {
//                        g.setColor(Color.GRAY);
//                    }
                    gl.glColor3f(0.5f, 0.5f, 0.5f);

                    Tuple2f v = new v2();
                    //List<PolygonFixture> generalPolygons = new FasterList<>();
                    for (Fixture f = body.fixtures; f != null; f = f.next) {
                        PolygonFixture pg = f.polygon;
                        if (pg != null) {
                            //generalPolygons.add(pg);
                        } else {
                            Shape shape = f.shape();
                            switch (shape.m_type) {
                                case POLYGON:
                                    PolygonShape poly = (PolygonShape) shape;
                                    gl.glBegin(GL2.GL_TRIANGLE_FAN);
                                    int n = poly.vertices;
                                    for (int i = 0; i <= n; ++i) {
                                        boolean done = false;
                                        if (i == n) {
                                            i = 0;
                                            done = true;
                                        }
                                        body.getWorldPointToOut(poly.vertex[i], v);
                                        gl.glVertex2f(v.x, v.y);
                                        if (done)
                                            break;
                                    }
                                    gl.glEnd();
                                    break;
                                case CIRCLE:
//                                    CircleShape circle = (CircleShape) shape;
//                                    float r = circle.m_radius;
//                                    body.getWorldPointToOut(circle.m_p, v);
//                                    Point p = getPoint(v);
//                                    int wr = (int) (r * zoom);
//                                    g.fillOval(p.x - wr, p.y - wr, wr * 2, wr * 2);
                                    break;
                                case EDGE:
//                                    EdgeShape edge = (EdgeShape) shape;
//                                    Tuple2f v1 = edge.m_vertex1;
//                                    Tuple2f v2 = edge.m_vertex2;
//                                    Point p1 = getPoint(v1);
//                                    Point p2 = getPoint(v2);
//                                    g.drawLine(p1.x, p1.y, p2.x, p2.y);
                                    break;
                            }
                        }
                    }

//                    if (generalPolygons.size() != 0) {
//                        PolygonFixture[] polygonArray = generalPolygons.toArray(new PolygonFixture[generalPolygons.size()]);
//                        for (PolygonFixture poly : polygonArray) {
//                            int n = poly.size();
//                            int x[] = new int[n];
//                            int y[] = new int[n];
//                            for (int i = 0; i < n; ++i) {
//                                body.getWorldPointToOut(poly.get(i), v);
//                                Point p = getPoint(v);
//                                x[i] = p.x;
//                                y[i] = p.y;
//                            }
//                            g.fillPolygon(x, y, n);
//                        }
                    }

                };
            JoglSpace.window(s, 800, 800);

            s.W.invokeLater(()->{
                for (int i = 0; i < 100; i++)
                    s.W.newDynamicBody(PolygonShape.box(20, 20), 1, 0.1f);
                //s.W.newDynamicBody(PolygonShape.box(100, 100), 1, 0.1f).pos.add(-100, -100);
            });

        }
    }
}
