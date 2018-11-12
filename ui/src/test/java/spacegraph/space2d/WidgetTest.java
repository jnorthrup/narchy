package spacegraph.space2d;

import spacegraph.SpaceGraph;
import spacegraph.space2d.container.Container;
import spacegraph.space2d.container.Graph2DTest;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.menu.ListMenu;
import spacegraph.space2d.widget.menu.TabMenu;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.sketch.Sketch2DBitmap;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.slider.SliderModel;
import spacegraph.space2d.widget.slider.XYSlider;
import spacegraph.space2d.widget.text.BitmapLabel;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.space2d.widget.textedit.TextEdit;
import spacegraph.space2d.widget.windo.GraphEdit;

import java.util.Map;
import java.util.function.Supplier;

import static spacegraph.space2d.container.grid.Gridding.grid;

public class WidgetTest {

    public static Container widgetDemo() {
        //return new TabMenu(menu);
        return new ListMenu(menu);
    }

    static final Map<String, Supplier<Surface>> menu = Map.of(
            "Button", () -> grid(
                    grid(
                            new PushButton("PushButton"),
                            new CheckBox("checkbox"),
                            grid(
                                    PushButton.awesome("code"),
                                    PushButton.awesome("trash"),
                                    PushButton.awesome("fighter-jet"),
                                    PushButton.awesome("wrench")
                            )
                    )
            ),
            "Slider", () -> grid(
                    new FloatSlider("solid slider", .25f  /* pause */, 0, 1),
                    new FloatSlider("knob slider", 0.75f, 0, 1).type(SliderModel.KnobHoriz),
                    new XYSlider()
            ),
            "Label", () -> grid(
                    new VectorLabel("vector"),
                    new BitmapLabel("bitmap")
            ),
            "TextEdit", () ->
                    new TextEdit("Edit this\n...").focus(), //new TextEdit0(new DummyConsole())
            "Graph2D", () ->
                    new TabMenu(Map.of(
                            "Graph2D Simple", () -> Graph2DTest.newSimpleGraph(),
                            "Graph2D UJMP", () -> Graph2DTest.newUjmpGraph()
                    )),
            "Wiring", () ->
                    new GraphEdit<>(1000, 1000)//new GraphEditTest.TinySpeechTest()
            ,
            "Toy", () -> new MetaFrame(new Sketch2DBitmap(256, 256))

    );

    public static void main(String[] args) {
        SpaceGraph.window(widgetDemo(), 1200, 800);
    }




//    private static class DummyConsole extends TextEdit0.TextEditUI implements Runnable {
//
//        public DummyConsole() {
//            super(15, 15);
//            Thread tt = new Thread(this);
//            tt.setDaemon(true);
//            tt.start();
//        }
//
//        @Override
//        public void run() {
//
//            int i = 0;
//            while (true) {
//
//                addLine((Math.random()) + "");
//                if (++i % 7 == 0) {
//                    text("");
//                }
//
//                Util.sleepMS(400);
//
//            }
//        }
//    }
}
