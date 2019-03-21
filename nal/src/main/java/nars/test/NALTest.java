package nars.test;

import jcog.exe.Exe;
import nars.NAR;
import nars.NARS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.opentest4j.AssertionFailedError;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

@Execution(CONCURRENT)
public abstract class NALTest {

    public TestNAR test;

    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger(NALTest.class);

    protected NALTest() {

        Exe.singleThread();

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
            t.test.quiet = true;
            t.test.test();
        } catch (AssertionFailedError ae) {
          //ignore
        } catch (Throwable ee) {
          ee.printStackTrace();
        }
        return t;
    }

    public static Stream<Method> tests(Class<? extends NALTest>... c) {
        return Stream.of(c)
                .flatMap(cc -> Stream.of(cc.getDeclaredMethods())
                        .filter(x -> x.getAnnotation(Test.class) != null))
                        .peek(AccessibleObject::trySetAccessible)
                        .collect(toList()).stream();
    }


    protected NAR nar() {
        return NARS.tmp();
    }


    @AfterEach
    public void end() {

        test.nar.synch();

        test.test();

        test.nar.stop();

//        test = null;

    }


}
