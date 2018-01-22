package spacegraph.widget;

import jcog.Texts;
import jcog.Util;
import spacegraph.SpaceGraph;
import spacegraph.layout.Container;
import spacegraph.layout.VSplit;
import spacegraph.widget.button.CheckBox;
import spacegraph.widget.button.PushButton;
import spacegraph.widget.console.TextEdit;
import spacegraph.widget.slider.BaseSlider;
import spacegraph.widget.slider.FloatSlider;
import spacegraph.widget.slider.XYSlider;
import spacegraph.widget.text.Label;

import static spacegraph.layout.Grid.*;

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
                            p.setLabel(Texts.n2(Math.random()));
                        })),
                        new VSplit(
                                new PushButton("vsplit"),
                                row(
                                        col(new CheckBox("checkbox"), new CheckBox("checkbox")),
                                        grid(
                                                new PushButton("a"), new PushButton("b"), new PushButton("c"), new PushButton("d")
                                        )
                                ), 0.8f
                        ),
                        col(
                                new Label("label"),
                                new FloatSlider("solid slider", .25f  /* pause */, 0, 1),
                                new FloatSlider("knob slider", 0.75f, 0, 1).type(BaseSlider.Knob)
                        ),
                        new XYSlider(),
                        new DummyConsole().surface()
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
