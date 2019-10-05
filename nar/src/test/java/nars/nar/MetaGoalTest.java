package nars.nar;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Task;
import nars.control.MetaGoal;
import nars.test.impl.DeductiveMeshTest;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.SortedMap;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MetaGoalTest {
    @Test void causesAppliedToDerivations() throws Narsese.NarseseException {
        int cycles = 64;

        //test causes of inputs (empty) and derivations (includes all necessary premise construction steps)
        Multimap<ShortHashSet, Task> tasks = MultimapBuilder.hashKeys().linkedHashSetValues().build();
        NAR n = NARS.tmp(1);
        n.what().onTask(t -> {
            tasks.put(new ShortHashSet(t.why().volume()), t);
        });
        n.input("(x-->y).");
        n.input("(y-->z).");
        n.run(cycles);


        n.control.why.forEach(w -> {
            System.out.println(w.id + " " + w);
        });
        tasks.forEach((c,t)->{
            System.out.println(c + "\t" + t);
        });

        assertTrue(tasks.size() > 2);
        Collection<Task> tt = tasks.values();
        Predicate<Task> isDerived = x -> !x.isInput();
        assertTrue(tt.stream().filter(isDerived).count() >= 1);

        assertTrue(tt.stream().allMatch(x -> {
            int ww = new ShortHashSet(x.why().volume()).size();
            if (x.stamp().length == 1) {
                //input
                System.out.print("IN ");
                if (ww!=0)
                    return false;
            } else {
                System.out.print("DE ");
                if (ww < 3)
                    return false;
            }
            System.out.println(ww + "\t" + x);
            return true;
        }));

    }

    @Disabled
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