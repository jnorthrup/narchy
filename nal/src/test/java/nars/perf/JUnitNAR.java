package nars.perf;

import jcog.test.JUnitPlanetX;
import nars.Param;
import nars.test.NALTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class JUnitNAR {

    public static void main(String[] args) throws FileNotFoundException {

        JUnitPlanetX j = new JUnitPlanetX()
                .test("nars")

                .run();

        j.report(new File("/tmp/test/" + System.currentTimeMillis() + ".arff"));
        j.report(System.out);


    }


    public static Method randomTest(Class<? extends NALTest>... c) {

        List<Method> methods = tests(c);
        int mm = methods.size();

        return methods.get(ThreadLocalRandom.current().nextInt(mm));
    }

    public static List<Method> tests(Class<? extends NALTest>... c) {

        List<Method> methods = Stream.of(c)
                .flatMap(cc -> Stream.of(cc.getMethods())
                        .filter(x -> x.getAnnotation(Test.class) != null))
                .collect(toList());
        return methods;
    }

    public static float test(TestNAR tt, List<Method> m) {
        final double[] sum = {0};
        for (Method x: m) {
            NALTest n = test(tt, x);
            if (n != null) {
                sum[0] += n.test.score;
            }
            tt.nar.reset();
            tt = new TestNAR(tt.nar);
        }
        return (float)sum[0];
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
}
