package spacegraph.test;

import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Container;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.*;
import spacegraph.space2d.widget.chip.SpeakChip;
import spacegraph.space2d.widget.console.TextEdit0;
import spacegraph.space2d.widget.menu.ListMenu;
import spacegraph.space2d.widget.menu.TabMenu;
import spacegraph.space2d.widget.menu.view.GridMenuView;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.meta.ProtoWidget;
import spacegraph.space2d.widget.meta.WizardFrame;
import spacegraph.space2d.widget.sketch.Sketch2DBitmap;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.slider.SliderModel;
import spacegraph.space2d.widget.slider.XYSlider;
import spacegraph.space2d.widget.text.BitmapLabel;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.space2d.widget.textedit.TextEdit;
import spacegraph.space2d.widget.windo.GraphEdit;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import static spacegraph.space2d.container.grid.Gridding.*;

public class WidgetTest {

    public static Container widgetDemo() {
        //return new TabMenu(menu);
        return new ListMenu(menu, new GridMenuView());
    }

    static final Map<String, Supplier<Surface>> menu;

    static {
        Map<String, Supplier<Surface>> m = Map.of(
                "Container", () -> grid(
                        LabeledPane.the("grid",
                                grid(iconButton(), iconButton(), iconButton(), iconButton(), iconButton(), iconButton(), iconButton(), iconButton())
                        ),
                        LabeledPane.the("grid wide",
                                new Gridding(0.618f, iconButton(), iconButton(), iconButton(), iconButton(), iconButton(), iconButton(), iconButton(), iconButton())
                        ),
                        LabeledPane.the("grid tall",
                                new Gridding(1 / 0.618f, iconButton(), iconButton(), iconButton(), iconButton(), iconButton(), iconButton(), iconButton(), iconButton())
                        ),
                        LabeledPane.the("column",
                                column(iconButton(), iconButton(), iconButton(), iconButton(), iconButton(), iconButton(), iconButton(), iconButton())
                        ),
                        LabeledPane.the("row",
                                row(iconButton(), iconButton(), iconButton(), iconButton(), iconButton(), iconButton(), iconButton(), iconButton())
                        ),
                        LabeledPane.the("vsplit",
                                Splitting.column(iconButton(), 0.618f, iconButton())
                        ),
                        LabeledPane.the("hsplit",
                                Splitting.row(iconButton(), 0.618f, iconButton())
                        )
                ),
                "Button", () -> grid(
                        new PushButton("PushButton"),
                        new CheckBox("CheckBox"),
                        new HexButton("gears", "HexButton")
                ),
                "Slider", () -> grid(
                        Splitting.row(
                                grid(new FloatSlider(.25f, 0, 1, "solid slider"   /* pause */),
                                        new FloatSlider(0.75f, 0, 1, "knob slider").type(SliderModel.KnobHoriz)),
                                0.9f,
                                new FloatSlider(0.33f, 0, 1).type(SliderModel.KnobVert)
                        ),
                        new XYSlider()
                ),
                "Dialog", () -> grid(
                        new TextEdit0("xyz").show(),
                        new FloatSlider(0.33f, 0.25f, 1, "Level"),
                        new ButtonSet(ButtonSet.Mode.One, new CheckBox("X"), new CheckBox("y"), new CheckBox("z")),

                        Submitter.text("OK", (String result) -> {
                        })
                ),

                "Wizard", () -> new ProtoWidget(),
                "Label", () -> grid(
                        new VectorLabel("vector"),
                        new BitmapLabel("bitmap")
                ),
                "TextEdit", () ->
                        new TextEdit("Edit this\n...").focus(), //new TextEdit0(new DummyConsole())
                "Graph2D", () -> new TabMenu(Map.of(
                        "Simple", () -> Graph2DTest.newSimpleGraph(),
                        "UJMP", () -> Graph2DTest.newUjmpGraph(),
                        "Types", () -> Graph2DTest.newTypeGraph()
                )),
                "Wiring", () -> {
                    GraphEdit<Surface> g;
                    g = new GraphEdit<>();
                    g.physics.invokeLater(() -> {
                        g.add(WidgetTest.widgetDemo()).posRel(1, 1, 0.5f, 0.25f);
                        for (int i = 0; i < 1; i++)
                            g.add(new WizardFrame(new ProtoWidget())).posRel(1, 1, 0.25f, 0.2f);
                    });
                    return g;
                },
                "Geo", () -> OSMTest.osmTest()
        );

        m = new HashMap<>(m); //escape arg limitation of Map.of()
        m.put("Sketch", () -> new MetaFrame(new Sketch2DBitmap(256, 256)));
        m.put("Speak", () -> new SpeakChip());
        m.put("Resplit", () -> new Splitting<>(iconButton(), 0.618f, iconButton()).resizeable());
        menu = m;
    }

    private static Surface iconButton() {
        String s;
        switch (ThreadLocalRandom.current().nextInt(6)) {
            case 0: s = "code"; break;
            case 1: s = "trash"; break;
            case 2: s = "wrench"; break;
            case 3: s = "fighter-jet"; break;
            case 4: s = "exclamation-triangle"; break;
            case 5: s = "shopping-cart"; break;
//            case 6: s = "dna"; break;
            default: s = null; break;
        }
        return PushButton.awesome(s);


                //            switch (ThreadLocalRandom.current().nextInt(6)) {
//                case 0-> "code";
//                default -> null;
//            });

    }

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
