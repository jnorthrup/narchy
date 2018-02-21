package spacegraph.widget.windo;

import jcog.Util;
import jcog.data.graph.ObjectGraph;
import jcog.exe.Loop;
import jcog.math.FloatRange;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.*;
import spacegraph.Surface;
import spacegraph.input.Wiring;
import spacegraph.layout.EmptySurface;
import spacegraph.layout.Gridding;
import spacegraph.math.v2;
import spacegraph.test.WidgetTest;
import spacegraph.widget.button.PushButton;
import spacegraph.widget.console.TextEdit;
import spacegraph.widget.slider.FloatSlider;
import spacegraph.widget.text.Label;

import javax.annotation.Nullable;
import java.lang.reflect.Field;


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
            w.sproutBranch("Other", 0.25f, 0.33f, () -> {
                        return new Surface[]{
                                new PushButton("X"),
                                new PushButton("Y"),
                                new PushButton("Z")
                        };
                    }
            );

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
            for (int i = 0; i < 100; i++)
                s.W.newDynamicBody(PolygonShape.box(0.1f, 0.1f), 1, 0.1f);
            //s.W.newDynamicBody(PolygonShape.box(100, 100), 1, 0.1f).pos.add(-100, -100);
            //});

        }
    }


    public static class Box2DTest3_Basic_Link_Unlink {

        public static void main(String[] args) {

            PhyWall s = PhyWall.window(800, 800);
//
//            s.addWindow(new Port(), RectFloat2D.XYWH(+1, 0, 0.25f, 0.25f));
//
//            s.addWindow(new Port(), RectFloat2D.XYWH(+1, 0, 0.25f, 0.25f));



                Body2D start = s.addWindow(new Port(), RectFloat2D.XYWH(+1, 0, 0.25f, 0.25f)).body;
                Body2D end = s.addWindow(new Port(), RectFloat2D.XYWH(+1, 0, 0.25f, 0.25f)).body;

                {

                    Snake ss = new Snake(start, end, 8, 0.3f, 0.1f);

//                    Util.sleep(1000);
//
//                    ss.remove(false);

                }

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

            A.link(AB.port);
            AB.port.link(B);

            Loop.of(() -> {
                A.out(String.valueOf(s.rng.nextInt(5)));
            }).runFPS(0.3f);
        }

    }

    public static class FloatPort extends /*Source*/Port {

        static final float EPSILON = 0.001f;

        private float curValue = Float.NaN;
        private final FloatRange f;

        public FloatPort(FloatRange f/*, Consumer<Runnable> updater*/) {
            this.f = f;

            FloatSlider s = new FloatSlider(f);
            content(new Gridding(0.25f, new EmptySurface(), s));
        }

        @Override
        public void prePaint(int dtMS) {
            //TODO make this an optional synchronous method of updating

            float nextValue = f.get();
            if (!Util.equals(nextValue, curValue, EPSILON)) {
                curValue = nextValue;
                out();
            }

            super.prePaint(dtMS);
        }

        public void out() {
            out(curValue);
        }

        @Override
        public void on(@Nullable InPort i) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean in(Wire from, Object s) {
            return false;
        }

        @Override
        public boolean onWireIn(@Nullable Wiring w, boolean preOrPost) {
            return false; //disallow
        }

        @Override
        protected void onWired(Wiring w) {
            out();
        }
    }

    public static class Box2DTest_FloatMux {

        public static void main(String[] args) {
            PhyWall s = PhyWall.window(800, 800);

            Port A = new FloatPort(new FloatRange(0.5f, 0, 1));
            PhyWall.PhyWindow a = s.addWindow(A, RectFloat2D.XYWH(-1, 0, 0.25f, 0.25f));

            Port B = new FloatPort(new FloatRange(0.5f, 0, 1));
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
}
