package nars.perf;


import nars.NAR;
import nars.NARS;
import nars.nal.nal1.NAL1Test;
import nars.test.NALTest;
import nars.test.TestNAR;
import nars.test.TestNARSuite;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JUnitNARTest {


    /**
     * junit test for running NALTest outside of Junit lol
     */
    @Test
    void testTestNAROutsideJUnit() {
        TestNAR tt = new TestNAR(NARS.tmp(1));

        List<Method> nal1TestMethods = TestNARSuite.tests(NAL1Test.class).collect(toList());
        assertTrue(nal1TestMethods.size() > 5);
        assertTrue(nal1TestMethods.toString().contains("deduction"));
//        assertTrue(nal1TestMethods.toString().contains("induction"));
//        System.out.println( nal1TestMethods.toString() );

        NALTest n = NALTest.test(tt, nal1TestMethods.get(0));
        //assertTrue(n.test.score > 0.5f);

    }

    @Test
    void testTestNARSuiteOutsideJUnit() {
        TestNARSuite s = new TestNARSuite(new Supplier<NAR>() {
            @Override
            public NAR get() {
                return NARS.tmp(1);
            }
        }, NAL1Test.class);
        s.run(true);
        s.print();
        //assertTrue(s.score() > 0);
    }

}
