package spacegraph.space2d;

import jcog.Util;
import spacegraph.SpaceGraph;
import spacegraph.space2d.container.Container;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.console.TextEdit0;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.sketch.Sketch2DBitmap;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.slider.SliderModel;
import spacegraph.space2d.widget.slider.XYSlider;
import spacegraph.space2d.widget.text.VectorLabel;

import static spacegraph.space2d.container.grid.Gridding.*;

public class WidgetTest {

    public static void main(String[] args) {
        SpaceGraph.window(widgetDemo(), 1200, 800);
    }


    public static Container widgetDemo() {
        return
                grid(
                        row(new PushButton("row1"), new PushButton("row2"), new PushButton("clickMe()")),
                        new Splitting(
                                new PushButton("vsplit"),
                                row(
                                        col(new CheckBox("checkbox"), new CheckBox("checkbox")),
                                        grid(
                                                PushButton.awesome("code"),
                                                PushButton.awesome("trash"),
                                                PushButton.awesome("fighter-jet"),
                                                PushButton.awesome("wrench")
                                        )
                                ), 0.8f
                        ),
                        col(
                                new VectorLabel("label"),
                                new FloatSlider("solid slider", .25f  /* pause */, 0, 1),
                                new FloatSlider("knob slider", 0.75f, 0, 1).type(SliderModel.KnobHoriz)
                        ),
                        new XYSlider(),
                        new TextEdit0(new DummyConsole()),
                        new MetaFrame(new Sketch2DBitmap(256, 256))
                );
    }

    private static class DummyConsole extends TextEdit0.TextEditUI implements Runnable {

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
                    text("");
                }

                Util.sleepMS(400);

            }
        }
    }
}
