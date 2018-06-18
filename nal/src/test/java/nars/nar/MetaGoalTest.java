package nars.nar;

import nars.NAR;
import nars.NARS;
import nars.control.MetaGoal;
import nars.test.impl.DeductiveMeshTest;
import org.junit.jupiter.api.Test;

import java.util.SortedMap;

class MetaGoalTest {

    @Test
    void test1() {
        NAR n = NARS.tmp(6);

        n.emotion.want(MetaGoal.Believe, 0.05f);
        n.emotion.want(MetaGoal.Perceive, -0.05f);

        n.log();

        DeductiveMeshTest m = new DeductiveMeshTest(n, new int[] { 3, 3 }, 3500);
        m.test.test();
        

        analyzeCauses(n);
    }

    private static void analyzeCauses(NAR n) {

        SortedMap<String, Object> x = n.stats();
        x.forEach((k, v) -> {
            System.out.println(k + "\t" + v);
        });

        n.causes.forEach(c -> {
            c.commit();
            double perceive = c.goal[MetaGoal.Perceive.ordinal()].total;
            double believe = c.goal[MetaGoal.Believe.ordinal()].total;
            double desire = c.goal[MetaGoal.Desire.ordinal()].total;
            if (perceive > 0) {
                c.print(System.out);
            }
        });
    }
}