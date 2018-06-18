package nars.nal.nal1;

import nars.NAR;
import nars.NARS;
import nars.test.NALTest;
import nars.test.impl.DeductiveChainTest;
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
        new DeductiveChainTest(test, 2, 1000, inh);
    }

    @Test
    void multistepSim2() {
        new DeductiveChainTest(test, 2, 1000, sim);
    }

    @Test
    void multistepInh3() {
        new DeductiveChainTest(test, 3, 8000, inh);
    }
    @Test
    void multistepSim3() {

        new DeductiveChainTest(test, 3, 1000, sim);
    }

    @Test
    void multistepInh4() {
        new DeductiveChainTest(test, 4, 16000, inh);
    }

    @Test public void multistepSim4() {
        new DeductiveChainTest(test, 4, 16000, sim);
    }

    @Test
    void multistepImpl2() {
        new DeductiveChainTest(test, 2, 1000, impl);
    }

    @Test
    void multistepImpl4() {
        new DeductiveChainTest(test, 4, 2000, impl);
    }

    @Test
    void multistepImpl5() {
        new DeductiveChainTest(test, 5, 4000, impl);
    }

}
