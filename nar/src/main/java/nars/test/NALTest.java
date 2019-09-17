package nars.test;

import jcog.exe.Exe;
import nars.NAR;
import nars.NARS;
import org.junit.jupiter.api.AfterEach;
import org.opentest4j.AssertionFailedError;

import java.lang.reflect.Method;

public abstract class NALTest {


//    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger(NALTest.class);
//    static {
//        ((ch.qos.logback.classic.Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.WARN);
//    }

    public TestNAR test;

    protected NALTest() {

        Exe.single();

        test = new TestNAR(nar());
    }

    public NALTest(TestNAR t) {
        test = t;
    }

    public static NALTest test(TestNAR tt, Method m) {
        NALTest t = null;
        try {
//            t = (NALTest) m.getDeclaringClass().getConstructor().newInstance();
            t = (NALTest) ((Class) m.getDeclaringClass())
                    .getConstructor().newInstance();
            t.test = (tt);

            m.invoke(t);
        } catch (Throwable e) {
            e.printStackTrace();
            return t;
        }

        try {
            t.test.test();
        } catch (AssertionFailedError ae) {
          //ignore
        } catch (Throwable ee) {
          ee.printStackTrace();
        }
        return t;
    }


    protected NAR nar() {
        return NARS.tmp();
    }


    @AfterEach
    public void end() {
        try {
            test.test();
        } finally {
            test = null;
            //test.nar.delete();
        }
    }

}
