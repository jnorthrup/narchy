package nars.test;

import nars.NAR;
import nars.NARS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;


public abstract class NALTest {

    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger(NALTest.class);

    public TestNAR test;

    protected NALTest() {
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
            t.test.score = Float.NEGATIVE_INFINITY;

            m.invoke(t);
        } catch (Throwable e) {
            e.printStackTrace();
            return t;
        }

        try {



            t.test.quiet = true;
//        t.test.nar.random().setSeed(
//                System.nanoTime()
//        );


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


    }


}
