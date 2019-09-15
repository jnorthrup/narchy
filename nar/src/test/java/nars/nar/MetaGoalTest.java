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
        //analyzeCauses(n); //init total summing


        n.emotion.want(MetaGoal.Believe, 0.01f);
        n.emotion.want(MetaGoal.Perceive, -0.01f);

        DeductiveMeshTest m = new DeductiveMeshTest(n, new int[] { 3, 3 }, 3500);
        m.test.test();
        
        analyzeCauses(n);
    }

    private static void analyzeCauses(NAR n) {

        SortedMap<String, Object> x = n.stats(true, true);
        x.forEach((k, v) -> {
            System.out.println(k + '\t' + v);
        });

        n.control.why.forEach(c -> {
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