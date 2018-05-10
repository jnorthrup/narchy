package nars.perf;

import com.google.common.util.concurrent.AtomicDouble;
import jcog.test.JUnitPlanetX;
import nars.NAR;
import nars.Param;
import nars.util.NALTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class JUnitNAR {

    public static void main(String[] args) throws FileNotFoundException {

        JUnitPlanetX j = new JUnitPlanetX()
                .test("nars")
                //.test(NAL1Test.class)
                .run();

        j.report(new File("/tmp/test/" + System.currentTimeMillis() + ".arff"));
        j.report(System.out);


    }

    /**
     * HACK runs all Junit test methods, returnign the sum of the test scores divided by tests (mean test score)
     */
    public static float tests(Executor exe, Supplier<NAR> s, float fractionToRun, Class<? extends NALTest>... c) {

        List<Method> methods = Stream.of(c)
                .flatMap(cc -> Stream.of(cc.getMethods())
                        .filter(x -> x.getAnnotation(Test.class) != null))
                .collect(toList());

        int mm = methods.size();
        if (fractionToRun < 1f) {
            int toRemove = Math.round((1f-fractionToRun)* mm);
            for (int i = 0; i < toRemove; i++){
                methods.remove(Math.round(Math.random() * (--mm)));
            }
        }

        if (methods.isEmpty())
            throw new RuntimeException("no tests remain");

        int totalTests = mm;
        final CountDownLatch remain = new CountDownLatch(methods.size());
        final AtomicDouble sum = new AtomicDouble(0);
        methods.forEach(m -> exe.execute(() -> {
            try {
                sum.addAndGet(test(s, m));
            } finally {
                remain.countDown();
            }
        }));
        try {
            remain.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return sum.floatValue()/totalTests;
    }

    private static float test(Supplier<NAR> s, Method m) {
        try {
            NALTest t = (NALTest) m.getDeclaringClass().getConstructor().newInstance();
            t.test.set(s.get()); //overwrite NAR with the supplier
            t.test.nar.random().setSeed(
                    System.nanoTime()
                    //1 //should change on each iteration so constant value wont work
            );
            try {
                m.invoke(t);
            } catch (Throwable ee) {
                return -1; //fatal setup
            }

            Param.DEBUG = false;

            try {
                t.test.test(false);
                return t.test.score;
                //return 1 + t.test.score; //+1 for successful completion
            } catch (Throwable ee) {
                //return -2f;
                //return -1f;
                return 0.0f; //fatal during test
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0f;
        }
    }
}
