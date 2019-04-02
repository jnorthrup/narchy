package spacegraph.test;

import jcog.tree.rtree.rect.RectFloat;
import org.jetbrains.annotations.NotNull;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Container;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.graph.EditGraph2D;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.*;
import spacegraph.space2d.widget.chip.NoiseVectorChip;
import spacegraph.space2d.widget.chip.SpeakChip;
import spacegraph.space2d.widget.console.TextEdit0;
import spacegraph.space2d.widget.menu.ListMenu;
import spacegraph.space2d.widget.menu.TabMenu;
import spacegraph.space2d.widget.menu.view.GridMenuView;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.meta.ProtoWidget;
import spacegraph.space2d.widget.meta.WizardFrame;
import spacegraph.space2d.widget.port.LabeledPort;
import spacegraph.space2d.widget.port.Port;
import spacegraph.space2d.widget.port.TogglePort;
import spacegraph.space2d.widget.sketch.Sketch2DBitmap;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.slider.SliderModel;
import spacegraph.space2d.widget.slider.XYSlider;
import spacegraph.space2d.widget.text.BitmapLabel;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.space2d.widget.textedit.TextEdit;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
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

                "Wiring", () ->  new TabMenu(wiringDemos()),

                "Geo", () -> OSMTest.osmTest()
        );

        m = new HashMap<>(m); //escape arg limitation of Map.of()
        m.put("Sketch", () -> new MetaFrame(new Sketch2DBitmap(256, 256)));
        m.put("Speak", () -> new SpeakChip());
        m.put("Resplit", () -> new Splitting(
            new Splitting<>(iconButton(), 0.618f, true,  iconButton()).resizeable(),
            0.618f,
            new Splitting<>(iconButton(), 0.618f, false, iconButton()).resizeable()
            ).resizeable()
        );
        m.put("Timeline", ()->Timeline2DTest.timeline2dTest());
        m.put("Tsne", ()-> TsneTest.testTsneModel());
        menu = m;
    }

    @NotNull
    private static Map<String, Supplier<Surface>> wiringDemos() {
        return Map.of(
            "Intro", ()->wiringDemo((EditGraph2D g)->{
                    g.add(WidgetTest.widgetDemo()).posRel(1, 1, 0.5f, 0.25f);
                    for (int i = 1; i < 3; i++)
                        g.add(new WizardFrame(new ProtoWidget())).posRel(0.5f, 0.5f, 0.45f/i, 0.35f/i);
                }),
                //"", ()-> wiringDemo((g)->{})
                "Basic", ()-> wiringDemo((g)->{
                    /** switched signal */

                    NoiseVectorChip A = new NoiseVectorChip();
                    Container a = g.add(A).pos(RectFloat.Unit.transform(500, 250, 250));


                    Port B = LabeledPort.generic();
                    Container b = g.add(B).pos(RectFloat.XYWH(+1, 0, 0.25f, 0.25f).scale(500));

                    TogglePort AB = new TogglePort();
                    g.add(AB).pos(RectFloat.XYWH(0, 0, 0.25f, 0.25f).scale(500));

//                    Loop.of(() -> {
//                        A.out(Texts.n4(Math.random()));
//                    }).setFPS(0.3f);
                })
        );
    }

    private static Surface wiringDemo(Consumer<EditGraph2D> o) {
        EditGraph2D<Surface> g;
        g = new EditGraph2D<>() {
            @Override
            protected void starting() {
                super.starting();
                physics.invokeLater(()->o.accept(this)); //() -> {
            }
        };
        return g;
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
