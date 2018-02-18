package spacegraph.widget.windo;

import jcog.tree.rtree.rect.RectFloat2D;
import org.jbox2d.collision.shapes.PolygonShape;
import spacegraph.render.JoglSpace;
import spacegraph.test.WidgetTest;
import spacegraph.widget.button.PushButton;

public class Box2DTest {


    public static class Box2DTest1_Boxes {

        public static void main(String[] args) {
            PhyWall s = new PhyWall();

            JoglSpace.window(s, 800, 800);


            {
                PhyWall.PhyWindow w = s.newWindow(WidgetTest.widgetDemo(), RectFloat2D.XYWH(0, 0, 550, 520));
                //s.newWindow(WidgetTest.widgetDemo(), RectFloat2D.XYWH(+400, 0, 300, 100));

                for (int i = 0; i < 10; i++)
                    w.newPort();
            }

//        d.addWindo(grid(new PushButton("x"), new PushButton("y"))).pos(10, 10, 50, 50);

            for (int i = 0; i < 8; i++) {
                float rx = (float) (Math.random() * 1000f / 2);
                float ry = (float) (Math.random() * 1000f / 2);
                float rw = 55 + 150 * (float) Math.random();
                float rh = 50 + 150 * (float) Math.random();
                s.newWindow(new PushButton(String.valueOf((char) ('w' + i))), RectFloat2D.XYWH(rx, ry, rw, rh));
            }

            //d.newWindo(grid(new PushButton("x"), new PushButton("y"))).pos(-100, -100, 0, 0);
            //d.children.add(new GridTex(16).pos(0,0,1000,1000));


        }
    }

    public static class Box2DTest2_Joints {
        public static void main(String[] args) {

            PhyWall s = new PhyWall() {



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
