package spacegraph.test;

import jcog.Texts;
import jcog.Util;
import spacegraph.container.Container;
import spacegraph.container.Splitting;
import spacegraph.render.JoglSpace;
import spacegraph.widget.button.CheckBox;
import spacegraph.widget.button.PushButton;
import spacegraph.widget.console.TextEdit;
import spacegraph.widget.sketch.Sketch2DBitmap;
import spacegraph.widget.slider.BaseSlider;
import spacegraph.widget.slider.FloatSlider;
import spacegraph.widget.slider.XYSlider;
import spacegraph.widget.text.Label;
import spacegraph.widget.windo.Widget;

import static spacegraph.container.Gridding.*;

public class WidgetTest {

    public static void main(String[] args) {

        JoglSpace.window(

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
                                new FloatSlider("knob slider", 0.75f, 0, 1).type(BaseSlider.Knob)
                        ),
                        new XYSlider().state(Widget.META),
                        new DummyConsole().surface(),
                        new Sketch2DBitmap(256, 256).state(Widget.META)
                );
    }

    private static class DummyConsole extends TextEdit implements Runnable {

        public DummyConsole() {
            super(15, 15);
            new Thread(this).start();
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
