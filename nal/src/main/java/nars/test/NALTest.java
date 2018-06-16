package nars.test;

import nars.NAR;
import nars.NARS;
import nars.Param;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

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
        } catch (Exception e) {
            return null;
        }

        t.test.quiet = true;
        t.test.nar.random().setSeed(
                System.nanoTime()

        );


        try {
            m.invoke(t);
        } catch (Throwable ee) {
            return null;
        }

        Param.DEBUG = false;

        try {
            t.test.test();

        } catch (Throwable ee) {


            return null;
        }
        return t;

    }

    public static Stream<Method> tests(Class<? extends NALTest>... c) {

        return Stream.of(c)
                .flatMap(cc -> Stream.of(cc.getMethods())
                        .filter(x -> x.getAnnotation(Test.class) != null))
                        .collect(toList()).parallelStream();
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
