package nars.gui;

import jcog.event.Ons;
import nars.NAR;
import nars.agent.NAgent;
import nars.concept.Concept;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meter.Plot2D;

public class EmotionPlot extends Gridding {

    Plot2D plot1, plot2, plot3;
    

    private Ons on;

    public EmotionPlot(int plotHistory, NAgent a) {
        this(plotHistory, a, a.nar());
    }

    public EmotionPlot(int plotHistory, NAgent a, NAR x) {
        super();

        NAR nar = x;

        set(
            plot1 = new Plot2D(plotHistory, Plot2D.Line)
                    .add("Dex+0", () -> a.dexterity(a.now)/*, 0f, 1f*/)
                    .add("Dex+2", () -> a.dexterity(a.now + 2 * a.nar().dur()))
                    .add("Dex+4", () -> a.dexterity(a.now + 4 * a.nar().dur())),
            plot2 = new Plot2D(plotHistory, Plot2D.Line)
                    .add("Busy", nar.emotion.busyVol::getSum),
            plot3 = new Plot2D(plotHistory, Plot2D.Line));
        a.happy.forEach(h -> {
            @Nullable Concept hc = nar.concept(h);
            plot3.add(h.toString(), () -> {
                Truth t = hc.beliefs().truth(a.now, a.nar());
                return t == null ? Float.NaN : t.freq();
            }, 0, 1f);



        });

        















        Ons ons = new Ons(
                a.onFrame(n -> plot1.update()),
                a.onFrame(n -> plot2.update()),
                a.onFrame(n -> plot3.update()));
        this.on = ons;
    }

    @Override
    public boolean stop() {
        if (super.stop()) {
            if (on != null) {
                on.off();
                on = null;
            }
            return true;
        }
        return false;
    }

}
