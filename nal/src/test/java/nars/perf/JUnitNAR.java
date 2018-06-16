package nars.perf;

import jcog.test.JUnitPlanetX;
import nars.test.NALTest;
import nars.test.TestNAR;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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

        List<Method> methods = NALTest.tests(c).collect(toList());
        int mm = methods.size();

        return methods.get(ThreadLocalRandom.current().nextInt(mm));
    }

    public static float test(TestNAR tt, List<Method> m) {
        final double[] sum = {0};
        for (Method x: m) {
            NALTest n = NALTest.test(tt, x);
            if (n != null) {
                sum[0] += n.test.score;
            }
            tt.nar.reset();
            tt = new TestNAR(tt.nar);
        }
        return (float)sum[0];
    }

}
