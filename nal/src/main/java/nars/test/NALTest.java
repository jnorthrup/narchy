package nars.test;

import nars.NAR;
import nars.NARS;
import nars.control.MetaGoal;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.LoggerFactory;


public abstract class NALTest {


    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger(NALTest.class);

    public TestNAR test;
    public final MetaGoal.Report metagoals = new MetaGoal.Report();


    protected NALTest() {
        test = new TestNAR(nar());
    }

    public NALTest(TestNAR t) {
        test = t;
    }


    protected NAR nar() {
        return NARS.tmp();
    }


    @AfterEach
    public void end() {

        test.nar.synch();

        test.test();

        test.nar.stop();


    }


}
