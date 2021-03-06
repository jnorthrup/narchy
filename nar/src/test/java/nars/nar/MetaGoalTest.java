package nars.nar;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Task;
import nars.control.Cause;
import nars.control.MetaGoal;
import nars.term.Term;
import nars.test.impl.DeductiveMeshTest;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MetaGoalTest {
    @Test void causesAppliedToDerivations() throws Narsese.NarseseException {

        //test causes of inputs (empty) and derivations (includes all necessary premise construction steps)
        Multimap<ShortHashSet, Task> tasks = MultimapBuilder.hashKeys().linkedHashSetValues().build();
        NAR n = NARS.tmp(1);
        n.what().onTask(new Consumer<Task>() {
            @Override
            public void accept(Task t) {
                Term why = t.why();
                if (why != null)
                    tasks.put(new ShortHashSet(why.volume()), t);
            }
        });
        n.input("(x-->y).");
        n.input("(y-->z).");
        int cycles = 64;
        n.run(cycles);


        n.control.why.forEach(new Procedure<Cause>() {
            @Override
            public void value(Cause w) {
                System.out.println(w.id + " " + w);
            }
        });
        tasks.forEach(new BiConsumer<ShortHashSet, Task>() {
            @Override
            public void accept(ShortHashSet c, Task t) {
                System.out.println(c + "\t" + t);
            }
        });

        assertTrue(tasks.size() > 2);
        Collection<Task> tt = tasks.values();
        Predicate<Task> isDerived = new Predicate<Task>() {
            @Override
            public boolean test(Task x) {
                return !x.isInput();
            }
        };
        long count = tt.stream().filter(isDerived).count();
        assertTrue(count >= 1);

        assertTrue(tt.stream().allMatch(new Predicate<Task>() {
            @Override
            public boolean test(Task x) {
                int ww = new ShortHashSet(x.why().volume()).size();
                if (x.stamp().length == 1) {
                    //input
                    System.out.print("IN ");
                    if (ww != 0)
                        return false;
                } else {
                    System.out.print("DE ");
                    if (ww < 3)
                        return false;
                }
                System.out.println(ww + "\t" + x);
                return true;
            }
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
        for (Map.Entry<String, Object> entry : x.entrySet()) {
            String k = entry.getKey();
            Object v = entry.getValue();
            System.out.println(k + '\t' + v);
        }
    }
}