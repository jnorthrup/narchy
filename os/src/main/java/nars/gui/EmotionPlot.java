package nars.gui;

import nars.NAR;
import nars.agent.NAgent;
import nars.concept.Concept;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meter.Plot2D;

public class EmotionPlot extends DurSurface<Gridding> {

    private final int plotHistory;
    private final NAgent a;
    Plot2D plot1, plot2, plot3;


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

        the.set(
                plot1 = new Plot2D(plotHistory, Plot2D.Line)
                        .add("Dex+0", () -> a.dexterity(a.now())/*, 0f, 1f*/)
                        .add("Dex+2", () -> a.dexterity(a.now() + 2 * a.nar().dur()))
                        .add("Dex+4", () -> a.dexterity(a.now() + 4 * a.nar().dur())),
                plot2 = new Plot2D(plotHistory, Plot2D.Line)
                        .add("Busy", nar.emotion.busyVol::getSum),
                plot3 = new Plot2D(plotHistory, Plot2D.Line));

        nar.runLater(()->{
            a.happy.forEach(h -> {
                @Nullable Concept hc = nar.concept(h);
                plot3.add(h.toString(), () -> {
                    Truth t = hc.beliefs().truth(a.now(), a.nar());
                    return t == null ? Float.NaN : t.freq();
                }, 0, 1f);
            });
        });
    }

    @Override
    protected void update() {
        plot1.update();
        plot2.update();
        plot3.update();
    }

}
