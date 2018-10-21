package nars.gui;

import jcog.event.Off;
import nars.agent.NAgent;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meter.Plot2D;

public class EmotionPlot extends Gridding {

    private final NAgent a;
    Plot2D plot1;
    private Off on;


    public EmotionPlot(int plotHistory, NAgent a) {


        this.a = a;
        set(
                plot1 = new Plot2D(plotHistory, Plot2D.Line)
                        .add("Dex+0", () -> a.dexterity()/*, 0f, 1f*/)
//                        .add("Dex+2", () -> a.dexterity(a.now() + 2 * a.nar().dur()))
//                        .add("Dex+4", () -> a.dexterity(a.now() + 4 * a.nar().dur()))

                //plot3 = new Plot2D(plotHistory, Plot2D.Line)
        );


    }

    @Override
    protected void starting() {
        super.starting();
        on = a.onFrame(this::update);
    }

    @Override
    protected void stopping() {
        on.off();
        on = null;
    }

    protected void update() {
        plot1.update();
    }

}
