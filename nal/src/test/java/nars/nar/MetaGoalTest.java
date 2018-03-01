package nars.nar;

import nars.NAR;
import nars.NARS;
import nars.test.DeductiveMeshTest;
import org.junit.jupiter.api.Test;

import java.util.SortedMap;

public class MetaGoalTest {

    @Test
    public void test1() {
        NAR n = NARS.tmp(1);

        DeductiveMeshTest m = new DeductiveMeshTest(n, 4, 4);
        n.log();
        n.run(500);

        SortedMap<String, Object> x = n.stats();
        x.forEach((k, v) -> {
            System.out.println(k + "\t" + v);
        });

        n.causes.forEach(c -> {
            c.commit();
            c.print(System.out);
        });
    }
}