package nars.gui;

import jcog.Util;
import nars.NAR;
import nars.NARS;
import nars.concept.sensor.Signal;
import nars.term.Term;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meta.MetaFrame;

import static nars.$.$$;
import static spacegraph.SpaceGraph.window;

class BeliefTableChart2Test {

    @Disabled
    @Test
    public void test1() {
        NAR n = NARS.tmp();
        Term x = $$("x");
        float f = 1/8f;
        new Signal(x, ()->{
            return (float) (Math.sin(n.time() * f)/2f + 0.5f);
        }, n).auto(n);
        window(
            new Gridding(
                new MetaFrame(new BeliefTableChart2(x, n)),
                new BeliefTableChart(n, x, new long[] { 0, 64 })
            )
        , 800, 500);
        n.startFPS(4f);
        n.log();
        Util.sleepMS(500000);
    }

}