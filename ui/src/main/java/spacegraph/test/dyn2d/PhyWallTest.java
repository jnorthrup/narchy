package spacegraph.test.dyn2d;

import jcog.data.graph.ObjectGraph;
import jcog.exe.Loop;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jbox2d.collision.shapes.PolygonShape;
import spacegraph.Ortho;
import spacegraph.Surface;
import spacegraph.container.Gridding;
import spacegraph.math.v2;
import spacegraph.test.WidgetTest;
import spacegraph.widget.button.PushButton;
import spacegraph.widget.console.TextEdit;
import spacegraph.widget.meta.ProtoWidget;
import spacegraph.widget.meta.WizardFrame;
import spacegraph.widget.text.Label;
import spacegraph.widget.text.LabeledPane;
import spacegraph.widget.windo.*;

import java.lang.reflect.Field;

import static spacegraph.container.Gridding.HORIZONTAL;
import static spacegraph.container.Gridding.VERTICAL;
import static spacegraph.test.dyn2d.TensorGlow.rng;


public class PhyWallTest {


    public static class Box2DTest1_Boxes {

        public static void main(String[] args) {
            PhyWall s = PhyWall.window(1000, 800);


            PhyWall.PhyWindow w = s.addWindow(WidgetTest.widgetDemo(), 1f, 1f);

            w.sprout(
                    new Gridding(0.1f, 1f, new TextEdit(16, 3, "wtf").surface()),
                    0.3f
            ).getOne().sprout(
                    new Gridding(0.1f, 1f, new PushButton("wtf")),
                    0.3f
            );//.getOne().sproutBranch("Other", 0.5f, ()->{

            w.sproutBranch("Other", 0.25f, 0.33f, () -> {
                        return new Surface[]{
                                new PushButton("X"),
                                new PushButton("Y"),
                                new PushButton("Z")
                        };
                    }
            );

//            {
//                w.sprout(new WebCam().view(), 1f, 1f);
//            }

//            {
//                WaveCapture au = new WaveCapture(new AudioSource(20));
//                au.runFPS(20f);
//                w.sprout(au.view(), 1f, 1f);
//            }

            for (int i = 0; i < 4; i++)
                w.sprout(new Port(), (float) (0.1f + Math.random() * 0.1f));

            for (int i = 0; i < 4; i++) {
                float rx = s.rngPolar(2);
                float ry = s.rngPolar(2);
                float rw = 0.05f + s.rngNormal(0.2f);
                float rh = 0.05f + s.rngNormal(0.2f);
                s.addWindow(new Label(String.valueOf((char) i)),
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
            for (int i = 0; i < 200; i++)
                s.W.newDynamicBody(PolygonShape.box(0.1f+rng.nextFloat()*0.2f, 0.1f), 1, 0.1f);
            //s.W.newDynamicBody(PolygonShape.box(100, 100), 1, 0.1f).pos.add(-100, -100);
            //});

        }
    }


    public static class Box2DTest_WeldGrow {

        public static void main(String[] args) {

            PhyWall s = PhyWall.window(800, 800);

            PhyWall.PhyWindow a = s.addWindow(new Label("X"), RectFloat2D.XYWH(-0.5f, +0.5f, 0.4f, 0.25f));
            a.grow(new Label("R"), 1f, 1, new v2(1, 0));
            a.grow(new Label("L"), 1f, 1, new v2(-1, 0));
            a.grow(new Label("D"), 1f, 1, new v2(0, +1));
            a.grow(new Label("U1"), 0.5f, 0.5f, new v2(+0.5f, -1));
            a.grow(new Label("U2"), 0.5f, 0.5f, new v2(-0.5f, -1));


        }
    }

    public static class Box2DTest_SwitchedSignal {

        public static void main(String[] args) {

            PhyWall s = PhyWall.window(800, 800);

            Port A = new Port();
            PhyWall.PhyWindow a = s.addWindow(A, RectFloat2D.XYWH(-1, 0, 0.25f, 0.25f));


            Port B = //LabeledPort.generic();
                    new Port();
            PhyWall.PhyWindow b = s.addWindow(B, RectFloat2D.XYWH(+1, 0, 0.25f, 0.25f));

            TogglePort AB = new TogglePort();
            s.addWindow(AB, RectFloat2D.XYWH(0, 0, 0.25f, 0.25f));

//            A.link(AB.port);
//            AB.port.link(B);

            Loop.of(() -> {
                A.out(String.valueOf(s.rng.nextInt(5)));
            }).runFPS(0.3f);
        }

    }

    public static class Box2DTest_FloatMux {

        public static void main(String[] args) {
            PhyWall s = PhyWall.window(800, 800);
            ((Ortho) s.root()).scaleMin = 100f;
            ((Ortho) s.root()).scaleMax = 500;

            Surface mux = new Gridding(HORIZONTAL, new LabeledPane("->", new Gridding(VERTICAL,
                    new Port(),
                    new Port()
            )), new LabeledPane("->", new Port()));
            s.addWindow(mux, 0.5f, 0.5f);

            Port A = new FloatPort(0.5f, 0, 1);
            PhyWall.PhyWindow a = s.addWindow(A, RectFloat2D.XYWH(-1, 0, 0.25f, 0.25f));

            Port B = new FloatPort(0.5f, 0, 1);
            PhyWall.PhyWindow b = s.addWindow(B, RectFloat2D.XYWH(-1, 0, 0.25f, 0.25f));

            Port Y = LabeledPort.generic();
            PhyWall.PhyWindow y = s.addWindow(Y, RectFloat2D.XYWH(+1, 0, 0.25f, 0.25f));

        }
    }

    public static class Box2DTest_ObjGraph {
        public static void main(String[] args) {
            PhyWall s = PhyWall.window(800, 800);

            ObjectGraph og = new ObjectGraph(2, s) {

                @Override
                public boolean includeValue(Object v) {
                    return true;
                }

                @Override
                public boolean includeClass(Class<?> c) {
                    return true;
                }

                @Override
                public boolean includeField(Field f) {
                    return true;
                }
            };

            og.forEachNode(n -> {
                PhyWall.PhyWindow oo = s.addWindow(new PushButton(n.id.getClass().toString()), RectFloat2D.XYWH(0, 0, 1, 1));
            });
//            og.forEachNode(n->{
//                s.links.node(n)
//            }
//            n.edges()
//            oo.link()

        }
    }


    public static class Box2DTest_ProtoWidget {

        public static void main(String[] args) {
            PhyWall s = PhyWall.window(800, 800);

            s.addWindow(
                    new WizardFrame( new ProtoWidget() ),
            1, 1);

        }
    }

}