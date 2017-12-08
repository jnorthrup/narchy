package jcog.pri.op;

import jcog.pri.Pri;
import jcog.pri.Prioritized;
import jcog.pri.Priority;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PriMergeTest {
    final static float tol = 0.01f;

    static final Pri a = new Pri(1);
    static final Pri b = new Pri(0.5f);
    static final Pri c = new Pri(0.25f);

    @Test
    public void testPlusDQBlend() {
        PriMerge m = PriMerge.plus;

        testMerge(z(), a, m,  1f, 0 /*overflow*/);  //adding to zero equals the incoming
        testMerge(a, z(),  m, a.pri());  //merging with zero should hae no effect

        testMerge(b, c,  m,0.75f); //test correct affect of components


        testMerge(a, c, m,  1,  //priority saturation behavior
                0.25f); //with overflow


        testMerge(a, a,  m, a.pri()); //no change since saturated with the same incoming values

    }

    @Test
    public void testAvg() {
        PriMerge AVG = PriMerge.avg;

        //z,a - averaging with zero results in half of the incoming
        testMerge(z(), a,  AVG, 0.5f * a.pri());
        //z,a(scale=0.5)
        //a,z - should be identical to z,a being that AVG is commutive:
        testMerge(a, z(),  AVG, 0.5f * a.pri());

        //average with itself should have no effect regardless of the applied scale factor
        testMerge(b, b,  AVG, b.pri()); //scale of one also should have no effect with itself

        testMerge(b, c,  AVG, 0.375f); //test correct affect of components; values closer to b since it is dominant
        testMerge(c, b, AVG, 0.375f); //test correct affect of components; values closer to b since it is dominant


        testMerge(a, c,  AVG, 0.625f); //priority decrease but less than the previous test which involves a weaker existing quality

    }

    @NotNull
    private Pri z() {
        return new Pri(0);
    }

//    @NotNull
//    private static RawBudget testMerge(float scale, @NotNull BudgetMerge m,
//                                       float exPri, float exDur, float exQua, //start value
//                                       float inPri, float inQua, //incoming merge
//                                       float ouPri, float ouQua  //expected result
//    )    {
//        RawBudget x = new RawBudget(exPri, exQua);
//        testMerge(m, x, inPri, inQua, scale, ouPri, ouQua);
//        return x;
//    }
//
//    private static Budget testMerge(@NotNull BudgetMerge m, Budget x, float inPri, float inQua, float scale, float ouPri, float ouQua) {
//        RawBudget y = new RawBudget(inPri, inQua);
//        return testMerge(x, y, scale, m, ouPri, ouQua);
//    }
    private static Prioritized testMerge(Priority x, Prioritized y, @NotNull PriMerge m, float ouPri) {
        return testMerge(x, y, m, ouPri, -1f);
    }
    private static Prioritized testMerge(Priority x, Prioritized y, @NotNull PriMerge m, float ouPri, float expectedOverflow) {
        x = x.clonePri();

        Prioritized x0 = x.clonePri();

        float overflow = m.merge(x, y);

        System.out.println(x0 + " <-merge<- " + y + " x "  + "\t\texpect:" + new Pri(ouPri) + " ?? actual:" + x);
        assertEquals(ouPri, x.pri(), tol);

        if (expectedOverflow > 0)
            assertEquals(expectedOverflow, overflow, 0.01f);

        return x;
    }

}