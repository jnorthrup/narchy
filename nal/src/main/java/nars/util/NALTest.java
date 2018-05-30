package nars.util;

import nars.NAR;
import nars.NARS;
import nars.control.MetaGoal;
import nars.test.TestNAR;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;



public abstract class NALTest {


    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger(NALTest.class);

    public final TestNAR test;
    public final MetaGoal.Report metagoals = new MetaGoal.Report();

    

    protected NALTest() {
        test = new TestNAR(nar());
    }
    protected NALTest(Supplier<NAR> s) {
        test = new TestNAR(s.get());
    }

    @BeforeEach
    void init() {
        
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
