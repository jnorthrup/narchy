package spacegraph.widget.windo;

import jcog.tree.rtree.rect.RectFloat2D;
import org.jbox2d.collision.shapes.PolygonShape;
import spacegraph.Surface;
import spacegraph.layout.Gridding;
import spacegraph.test.WidgetTest;
import spacegraph.widget.button.PushButton;
import spacegraph.widget.console.TextEdit;
import spacegraph.widget.text.Label;

public class PhyWallTest {


    public static class Box2DTest1_Boxes {

        public static void main(String[] args) {
            PhyWall s = PhyWall.window(1000, 800);


            PhyWall.PhyWindow w = s.addWindow(WidgetTest.widgetDemo(), RectFloat2D.XYWH(0, 0, 1f, 1f));
            //s.newWindow(WidgetTest.widgetDemo(), RectFloat2D.XYWH(+400, 0, 300, 100));

            w.sprout(
                    new Gridding(0.1f, 1f, new TextEdit(16, 3, "wtf").surface()),
                    0.3f
            ).getOne().sprout(
                    new Gridding(0.1f, 1f, new PushButton("wtf")),
                    0.3f
            );//.getOne().sproutBranch("Other", 0.5f, ()->{
            w.sproutBranch("Other", 0.25f, 0.33f, ()->{
                    return new Surface[] {
                        new PushButton("X"),
                        new PushButton("Y"),
                        new PushButton("Z")
                    };
                }
            );

            for (int i = 0; i < 4; i++)
                w.sprout(new Port(), (float) (0.1f + Math.random()*0.1f));

            for (int i = 0; i < 4; i++) {
                float rx = s.rngPolar(2);
                float ry = s.rngPolar(2);
                float rw = 0.05f + s.rngNormal(0.2f);
                float rh = 0.05f + s.rngNormal(0.2f);
                s.addWindow(new Label(String.valueOf((char)i)),
                        RectFloat2D.XYWH(rx, ry, rw, rh));
            }

            //d.newWindo(grid(new PushButton("x"), new PushButton("y"))).pos(-100, -100, 0, 0);
            //d.children.add(new GridTex(16).pos(0,0,1000,1000));


        }
    }

    public static class Box2DTest2_Raw_Geom_Rendered__No_Surfaces {
        public static void main(String[] args) {

            PhyWall s = PhyWall.window(800, 800);

            //s.W.invoke(()->{
                for (int i = 0; i < 100; i++)
                    s.W.newDynamicBody(PolygonShape.box(20, 20), 1, 0.1f);
                //s.W.newDynamicBody(PolygonShape.box(100, 100), 1, 0.1f).pos.add(-100, -100);
            //});

        }
    }
}
