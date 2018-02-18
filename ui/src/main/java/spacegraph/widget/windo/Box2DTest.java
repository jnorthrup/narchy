package spacegraph.widget.windo;

import jcog.tree.rtree.rect.RectFloat2D;
import org.jbox2d.collision.shapes.PolygonShape;
import spacegraph.ZoomOrtho;
import spacegraph.render.JoglSpace;
import spacegraph.render.SpaceGraphFlat;
import spacegraph.test.WidgetTest;
import spacegraph.widget.text.Label;

public class Box2DTest {


    public static class Box2DTest1_Boxes {

        public static void main(String[] args) {
            PhyWall s = new PhyWall();
            s.pos(-1,-1,1,1);

            new SpaceGraphFlat(
                new ZoomOrtho(s) {

                    @Override
                    public boolean autoresize() {
                        s.root().zoom(s);
                        return false;
                    }

                }
            ).show(1000, 1000);



            {
                PhyWall.PhyWindow w = s.newWindow(WidgetTest.widgetDemo(), RectFloat2D.XYWH(0, 0, 1f, 1f));
                //s.newWindow(WidgetTest.widgetDemo(), RectFloat2D.XYWH(+400, 0, 300, 100));

                for (int i = 0; i < 10; i++)
                    w.newPort();
            }

//        d.addWindo(grid(new PushButton("x"), new PushButton("y"))).pos(10, 10, 50, 50);

            for (int i = 0; i < 8; i++) {
                float rx = s.rngPolar(2);
                float ry = s.rngPolar(2);
                float rw = 0.1f + s.rngNormal(0.5f);
                float rh = 0.1f + s.rngNormal(0.5f);
                s.newWindow(new Label(String.valueOf((char) ('w' + i))),
                        RectFloat2D.XYWH(rx, ry, rw, rh));
            }

            //d.newWindo(grid(new PushButton("x"), new PushButton("y"))).pos(-100, -100, 0, 0);
            //d.children.add(new GridTex(16).pos(0,0,1000,1000));


        }
    }

    public static class Box2DTest2_Joints {
        public static void main(String[] args) {

            PhyWall s = new PhyWall();
            JoglSpace.window(s, 800, 800);

            //s.W.invoke(()->{
                for (int i = 0; i < 100; i++)
                    s.W.newDynamicBody(PolygonShape.box(20, 20), 1, 0.1f);
                //s.W.newDynamicBody(PolygonShape.box(100, 100), 1, 0.1f).pos.add(-100, -100);
            //});

        }
    }
}
