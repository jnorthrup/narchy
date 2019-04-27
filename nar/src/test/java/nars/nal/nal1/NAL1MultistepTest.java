package nars.nal.nal1;

import nars.NAR;
import nars.NARS;
import nars.test.NALTest;
import nars.test.impl.DeductiveChainTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.test.impl.DeductiveChainTest.*;

public class NAL1MultistepTest extends NALTest {

    @Override protected NAR nar() {
        NAR n = NARS.tmp(6);
        n.termVolumeMax.set(6);
        n.freqResolution.set(0.2f);
        n.confResolution.set(0.1f);
        return n;
    }

    @Test
    void multistepInh2() {
        new DeductiveChainTest(test, 2, 200, inh);
    }

    @Test
    void multistepSim2() {
        new DeductiveChainTest(test, 2, 200, sim);
    }

    @Test
    void multistepInh3() {
        new DeductiveChainTest(test, 3, 400, inh);
    }

    @Test
    void multistepSim3() {

        new DeductiveChainTest(test, 3, 500, sim);
    }

    @Test
    void multistepInh4() {
        new DeductiveChainTest(test, 4, 1500, inh);
    }

    @Test public void multistepSim4() {
        new DeductiveChainTest(test, 4, 1500, sim);
    }

    @Test
    void multistepImpl2() {

        new DeductiveChainTest(test, 2, 200, impl);
    }

    @Test
    void multistepImpl4() {
        new DeductiveChainTest(test, 4, 1500, impl);
    }

    @Disabled
    @Test
    void multistepImpl5() {
        new DeductiveChainTest(test, 5, 3500, impl);
    }

}
