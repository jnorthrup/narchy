package nars.gui;

import nars.NAR;
import nars.agent.NAgent;
import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.widget.meter.Plot2D;

public class EmotionPlot extends DurSurface<Gridding> {

    private final int plotHistory;
    private final NAgent a;
    Plot2D plot1;


    public EmotionPlot(int plotHistory, NAgent a) {
        this(plotHistory, a, a.nar());
    }

    public EmotionPlot(int plotHistory, NAgent a, NAR x) {
        super(new Gridding(), x);

        this.plotHistory = plotHistory;
        this.a = a;


    }

    @Override
    protected void starting() {

        super.starting();

        durs(0.5f); //2x sample

        the.set(
                plot1 = new Plot2D(plotHistory, Plot2D.Line)
                        .add("Dex+0", () -> a.dexterity()/*, 0f, 1f*/)
//                        .add("Dex+2", () -> a.dexterity(a.now() + 2 * a.nar().dur()))
//                        .add("Dex+4", () -> a.dexterity(a.now() + 4 * a.nar().dur()))

                //plot3 = new Plot2D(plotHistory, Plot2D.Line)
        );

//        nar.runLater(()->{
//            a.happy.forEach(h -> {
//                Concept hc = nar.concept(h);
//                plot3.add(h.toString(), () -> {
//                    Truth t = hc.beliefs().truth(a.now(), a.nar());
//                    return t == null ? Float.NaN : t.freq();
//                }, 0, 1f);
//            });
//        });
    }

    @Override
    protected void update() {
        plot1.update();
    }

}
