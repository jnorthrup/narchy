package nars.nal.nal1;

import nars.test.DeductiveChainTest;
import nars.util.NALTest;
import org.junit.jupiter.api.Test;

import static nars.test.DeductiveChainTest.*;

//import static nars.util.meter.experiment.DeductiveChainTest.inh;

public class NAL1MultistepTest extends NALTest {


    @Test public void multistepInh2() {
        test.nar.log();
        new DeductiveChainTest(test, 2, 1000, inh);
    }

    @Test public void multistepSim2() {
        new DeductiveChainTest(test, 2, 1000, sim);
    }

    @Test public void multistepInh3() {
        new DeductiveChainTest(test, 3, 1000, inh);
    }
    @Test public void multistepSim3() {
        new DeductiveChainTest(test, 3, 1000, sim);
    }

    @Test public void multistepInh4() {
        new DeductiveChainTest(test, 4, 4000, inh);
    }
    @Test public void multistepSim4() {
        new DeductiveChainTest(test, 4, 2000, sim);
    }

    @Test public void multistepImpl2() {
        new DeductiveChainTest(test, 2, 500, impl);
    }

    @Test public void multistepImpl4() {
        new DeductiveChainTest(test, 4, 1500, impl);
    }

    @Test public void multistepImpl5() {
        new DeductiveChainTest(test, 5, 1500, impl);
    }
//    @Test public void multistepEqui5() {
//        test.nar.nal(6);
//        new DeductiveChainTest(test, 5, 3500, equiv);
//    }




//    @Test
//    @Deprecated public void multistep() throws InvalidInputException {
//        long time = 150;
//
//        //TextOutput.out(n);
//
//        TestNAR test = test();
//
//        //we know also 73% is the theoretical maximum it can reach
//        if (test.nar.nal() <= 2)
//            test.mustBelieve(time, "<a --> d>", 1f, 1f, 0.25f, 0.99f);
//        else
//            //originally checked for 0.25% exact confidence
//            test.mustBelieve(time, "<a --> d>", 1f, 1f, 0.25f, 0.99f);
//
//        test.believe("<a --> b>", 1.0f, 0.9f);
//        test.believe("<b --> c>", 1.0f, 0.9f);
//        test.believe("<c --> d>", 1.0f, 0.9f);
//
//
//        test.run();
//    }

}
