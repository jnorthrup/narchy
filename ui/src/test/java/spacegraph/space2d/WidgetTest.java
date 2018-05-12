package spacegraph.space2d;

import jcog.Texts;
import jcog.Util;
import spacegraph.SpaceGraph;
import spacegraph.space2d.container.Container;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.console.TextEdit;
import spacegraph.space2d.widget.sketch.Sketch2DBitmap;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.slider.SliderModel;
import spacegraph.space2d.widget.slider.XYSlider;
import spacegraph.space2d.widget.text.Label;

import static spacegraph.space2d.container.grid.Gridding.*;

public class WidgetTest {

    public static void main(String[] args) {

        SpaceGraph.window(

                widgetDemo()
                , 1200, 800);


        //SpaceGraph dd = SpaceGraph.window(new Cuboid(widgetDemo(), 16, 8f).color(0.5f, 0.5f, 0.5f, 0.25f), 1000, 1000);

//        new SpaceGraph2D(
//                new Cuboid(widgetDemo(), 16, 8f, 0.2f).color(0.5f, 0.5f, 0.5f, 0.25f).move(0,0,0)
//        ).show(800, 600);

    }


    public static Container widgetDemo() {
        return
                grid(
                        row(new PushButton("row1"), new PushButton("row2"), new PushButton("clickMe()", (p) -> {
                            p.label(Texts.n2(Math.random()));
                        })),
                        new Splitting(
                                new PushButton("vsplit"),
                                row(
                                        col(new CheckBox("checkbox"), new CheckBox("checkbox")),
                                        grid(
                                                new PushButton().icon("fontawesome://code"),
                                                new PushButton().icon("fontawesome://trash"),
                                                new PushButton().icon("fontawesome://fighter-jet"),
                                                new PushButton().icon("fontawesome://wrench")
                                        )
                                ), 0.8f
                        ),
                        col(
                                new Label("label"),
                                new FloatSlider("solid slider", .25f  /* pause */, 0, 1),
                                new FloatSlider("knob slider", 0.75f, 0, 1).type(SliderModel.KnobHoriz)
                        ),
                        new XYSlider(),//.state(Widget.META),
                        new DummyConsole().surface(),
                        new Sketch2DBitmap(256, 256)//.state(Widget.META)
                );
    }

    private static class DummyConsole extends TextEdit implements Runnable {

        public DummyConsole() {
            super(15, 15);
            Thread tt = new Thread(this);
            tt.setDaemon(true);
            tt.start();
        }

        @Override
        public void run() {

            int i = 0;
            while (true) {

                addLine((Math.random()) + "");
                if (++i % 7 == 0) {
                    text(""); //clear
                }

                Util.sleep(400);

            }
        }
    }
}
