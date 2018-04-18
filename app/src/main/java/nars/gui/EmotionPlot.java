package nars.gui;

import nars.NAR;
import nars.NAgent;
import nars.concept.Concept;
import nars.control.DurService;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meter.Plot2D;

public class EmotionPlot extends Gridding {

//    private final int plotHistory;
    @Deprecated
    private DurService on; //TODO use DurSurface
    Plot2D plot1, plot2, plot3;

    public EmotionPlot(int plotHistory, NAgent a) {
        this(plotHistory, a, a.nar());
    }

    public EmotionPlot(int plotHistory, NAgent a, NAR x) {
        super();

        NAR nar = x;

//        this.plotHistory = plotHistory;
        plot1 = new Plot2D(plotHistory, Plot2D.Line);
        plot2 = new Plot2D(plotHistory, Plot2D.Line);
        plot3 = new Plot2D(plotHistory, Plot2D.Line);

//            TextEdit console = new TextEdit(20, 3);
//            console.textBox.setCaretWarp(true);
        set(plot1, plot2, plot3);

        //plot1.add("Conf", nar.emotion.confident::getSum);
        plot2.add("Busy", nar.emotion.busyVol::getSum);

        plot1.add("Dex+0", () -> a.dexterity(a.now)/*, 0f, 1f*/);
        plot1.add("Dex+2", () -> a.dexterity(a.now + 2 * a.nar().dur()));
        plot1.add("Dex+4", () -> a.dexterity(a.now + 4 * a.nar().dur()));

        a.happy.forEach(h -> {
            @Nullable Concept hc = nar.concept(h);
            plot3.add(h.toString(), () -> {
                Truth t = hc.beliefs().truth(a.now, a.nar());
                return t == null ? Float.NaN : t.freq();
            }, 0, 1f);
//                plot3.add("WantHpy", () -> {
//                    return hc.goals().freq(a.now, a.nar);
//                }, 0, 1f);
        });

//            plot4.add("Sad", () -> {
//                return a.sad.beliefs().freq(a.now, a.nar);
//            }, 0, 1f);
//            plot4.add("WantSad", () -> {
//                return a.sad.goals().exp(a.now, a.nar);
//            }, 0, 1f);

//            plot4.add("Hapy", nar.emotion.happy::getSum);
//            plot4.add("Sad", nar.emotion.sad::getSum);
//                plot4.add("Errr", ()->nar.emotion.errr.getSum());

        on = a.onFrame((aa)->{
            plot1.update();
            plot2.update();
            plot3.update();
        });
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
