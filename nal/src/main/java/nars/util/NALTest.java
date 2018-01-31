package nars.util;

import nars.NAR;
import nars.NARS;
import nars.Param;
import nars.control.MetaGoal;
import nars.test.TestNAR;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestReporter;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;


//@ExtendWith(NALTestStats.class)
public abstract class NALTest {

    static {
        Param.DEBUG = true;
    }

    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger(NALTest.class);

    public final NAR nar;
    public final TestNAR test;
    public final MetaGoal.Report metagoals = new MetaGoal.Report();

    private TestInfo testInfo;

    protected NALTest() {
        test = new TestNAR(nar = nar()) {
            @Override
            protected void assertSuccess(boolean success) {
                afterTest(testInfo);
                super.assertSuccess(success);
            }
        };
    }
    protected NALTest(Supplier<NAR> s) {
        test = new TestNAR(nar = s.get()) {
            @Override
            protected void assertSuccess(boolean success) {
                afterTest(testInfo);
                super.assertSuccess(success);
            }
        };
    }

    @BeforeEach
    void init() {
        Param.DEBUG = true;
        Param.ANSWER_REPORTING = false;
    }


    protected NAR nar() {
        return NARS.tmp();
    }


    @AfterEach
    public void end(TestInfo i, TestReporter c) {

        this.testInfo = i;

        test.nar.time.synch(test.nar);

        test.test();

        nar.stop();

        metagoals.add(nar.causes).print(System.out);


        //        c.publishEntry(t.toString() /*context.getUniqueId() */ + ".NAR.stats",
//                nar.stats().toString());

//        if (n.metagoals != null)
//            metagoals.add(n.metagoals);
    }

    protected void afterTest(TestInfo testInfo) {

    }

}
