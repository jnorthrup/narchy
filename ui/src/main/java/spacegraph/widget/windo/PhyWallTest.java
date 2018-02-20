package spacegraph.widget.windo;

import jcog.exe.Loop;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jbox2d.collision.shapes.PolygonShape;
import spacegraph.Surface;
import spacegraph.input.Wiring;
import spacegraph.layout.Gridding;
import spacegraph.math.v2;
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
                    s.W.newDynamicBody(PolygonShape.box(0.1f, 0.1f), 1, 0.1f);
                //s.W.newDynamicBody(PolygonShape.box(100, 100), 1, 0.1f).pos.add(-100, -100);
            //});

        }
    }

    public static class Box2DTest3_Basic_Link_Unlink {

        public static void main(String[] args) {

            PhyWall s = PhyWall.window(800, 800);

            s.addWindow(new Gridding(0.1f, new Port() {
                @Override
                protected void onWired(Wiring w) {
                    w.source().link(w.target());
                }
            }), RectFloat2D.XYWH(-1, 0, 0.25f, 0.25f));

            s.addWindow(new Port(), RectFloat2D.XYWH(+1, 0, 0.25f, 0.25f));

        }
    }

    public static class Box2DTest_WeldGrow {

        public static void main(String[] args) {

            PhyWall s = PhyWall.window(800, 800);

            PhyWall.PhyWindow a = s.addWindow(new Label("X"), RectFloat2D.XYWH(-0.5f, +0.5f, 0.4f, 0.25f));
            a.grow(new Label("R"), 1f, 1, new v2(1,0));
            a.grow(new Label("L"), 1f, 1, new v2(-1,0));
            a.grow(new Label("D"), 1f, 1, new v2(0,+1));
            a.grow(new Label("U1"), 0.5f, 0.5f, new v2(+0.5f,-1));
            a.grow(new Label("U2"), 0.5f, 0.5f, new v2(-0.5f,-1));


        }
    }

    public static class Box2DTest_SwitchedSignal {

        public static void main(String[] args) {

            PhyWall s = PhyWall.window(800, 800);

            Port A = new Port();
            PhyWall.PhyWindow a = s.addWindow(A, RectFloat2D.XYWH(-1, 0, 0.25f, 0.25f));


            Port B = new ToStringLabelPort();
            PhyWall.PhyWindow b = s.addWindow(B, RectFloat2D.XYWH(+1, 0, 0.25f, 0.25f));

            TogglePort AB = new TogglePort();
            s.addWindow(AB, RectFloat2D.XYWH(0, 0, 0.25f, 0.25f));

            A.link(AB.port);
            AB.port.link(B);

            Loop.of(()->{
                A.out(String.valueOf(s.rng.nextInt(5)));
            }).runFPS(0.3f);
        }

        private static class ToStringLabelPort extends Port {
            final Label l = new Label("?");

            ToStringLabelPort() {
                children(l);
                on((v)->l.text(v.toString()));
            }



        }
    }

}
