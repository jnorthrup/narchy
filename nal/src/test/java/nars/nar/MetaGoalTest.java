package nars.nar;

import nars.NAR;
import nars.NARS;
import nars.control.MetaGoal;
import nars.test.impl.DeductiveMeshTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.SortedMap;

@Disabled
class MetaGoalTest {

    @Test
    void test1() {
        NAR n = NARS.tmp(6);
        analyzeCauses(n); //init total summing

        n.feel.want(MetaGoal.Believe, 0.01f);
        n.feel.want(MetaGoal.PerceiveCmplx, -0.01f);

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
//            double perceive = c.credit[MetaGoal.PerceiveCmplx.ordinal()].total();
//            double believe = c.credit[MetaGoal.Believe.ordinal()].total();
//            double desire = c.credit[MetaGoal.Desire.ordinal()].total();
//            if (perceive > 0) {
                c.print(System.out);
//            }
        });
    }
}