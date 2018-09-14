package spacegraph.space2d.widget;

import jcog.data.graph.ObjectGraph;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.WidgetTest;
import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.console.TextEdit;
import spacegraph.space2d.widget.meta.ProtoWidget;
import spacegraph.space2d.widget.meta.WizardFrame;
import spacegraph.space2d.widget.windo.Port;
import spacegraph.space2d.widget.windo.WiredWall;

import java.lang.reflect.Field;


public class Dyn2DSurfaceTest {

//    public static class Dyn2DEmbedded {
//        public static void main(String[] args) {
////            Dyn2DSurface s = new Dyn2DSurface()
////
////                    .enclose()
////            ;
////
////            Dyn2DSurface t = new Dyn2DSurface()
////
////                    .enclose()
////                    ;
//
//            SpaceGraph.window(
//                new Gridding(
//                        new Gridding(
//                            new PushButton("x"),
//                            new PushButton("y")
//                        ),
//                        new Clipped(t),
//                        new Clipped(s)),
//
//                1400, 1000
//            );
//
//
//
//
//            for (int i = 0; i < 80; i++)
//                s.W.newDynamicBody(PolygonShape.box(30,30), 0.01f, 0.1f);
//
//        }
//    }

    public static class Box2DTest1_Demo {

        public static void main(String[] args) {
            WiredWall s = SpaceGraph.wall(1000, 800);
            demo(s);
        }
    }

    public static void demo(WiredWall s) {
        WiredWall.PhyWindow w = s.put(WidgetTest.widgetDemo(), 100f, 100f);
        w.move(50,50);

        w.sprout(
                new Gridding(0.1f, 1f, new TextEdit(16, 3, "wtf").surface()),
                0.3f
        );
        /*.getOne().sprout(
                new Gridding(0.1f, 1f, new PushButton("wtf")),
                0.3f
        );*/

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
//
//        for (int i = 0; i < 4; i++) {
//            float rx = s.rngPolar(2);
//            float ry = s.rngPolar(2);
//            float rw = 0.05f + s.rngNormal(0.2f);
//            float rh = 0.05f + s.rngNormal(0.2f);
//            s.put(new VectorLabel(String.valueOf((char) i)),
//                    RectFloat2D.XYWH(rx, ry, rw, rh));
//        }

        
        
    }

//    public static class Box2DTest2_Raw_Geom_Rendered__No_Surfaces {
//        public static void main(String[] args) {
//
//            Dyn2DSurface s = SpaceGraph.wall(800, 800);
//
//
//
//            for (int i = 0; i < 200; i++)
//                s.W.newDynamicBody(PolygonShape.box((float) (0.1f+Math.random()*0.2f), 0.1f), 1, 0.1f);
//
//
//
//        }
//    }


//    public static class Box2DTest_WeldGrow {
//
//        public static void main(String[] args) {
//
//            Dyn2DSurface s = SpaceGraph.wall(800, 800);
//
//            Dyn2DSurface.PhyWindow a = s.put(new VectorLabel("X"), RectFloat2D.XYWH(-0.5f, +0.5f, 0.4f, 0.25f));
//            a.grow(new VectorLabel("R"), 1f, 1, new v2(1, 0));
//            a.grow(new VectorLabel("L"), 1f, 1, new v2(-1, 0));
//            a.grow(new VectorLabel("D"), 1f, 1, new v2(0, +1));
//            a.grow(new VectorLabel("U1"), 0.5f, 0.5f, new v2(+0.5f, -1));
//            a.grow(new VectorLabel("U2"), 0.5f, 0.5f, new v2(-0.5f, -1));
//
//
//        }
//    }





    public static class Box2DTest_ObjGraph {
        public static void main(String[] args) {
            WiredWall s = SpaceGraph.wall(800, 800);

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
                WiredWall.PhyWindow oo = s.put(new PushButton(n.id().getClass().toString()), RectFloat2D.XYWH(0, 0, 1, 1));
            });






        }
    }


    public static class Box2DTest_ProtoWidget {

        public static void main(String[] args) {
            WiredWall s = SpaceGraph.wall(800, 800);

            s.put(
                    new WizardFrame( new ProtoWidget() ),
            1, 1);

        }
    }

}