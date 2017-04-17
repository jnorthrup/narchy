package nars.budget;

import jcog.math.MultiStatistics;
import jcog.pri.PLink;
import nars.Focus;
import nars.Narsese;
import nars.Op;
import nars.concept.Concept;
import nars.nar.Default;
import nars.term.Term;
import nars.term.Termed;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

/**
 * Created by me on 1/12/17.
 */
public class BudgetFairnessTest {


    @Test
    public void test1() throws Narsese.NarseseException {
        //TODO partitioned subgraph memory generator
        int subgraphs = 3;
        int vocabularySize = 3;


        final int cycles = 10;
        final int conceptsFirePerCycle = 10;

        MultiStatistics<Term> m = new MultiStatistics<>(
                new MultiStatistics.Condition<>("atom", (t) -> t.op() == Op.ATOM),
                volumeIn(0,1),
                volumeIn(2,3),
                volumeIn(4,5)
        );
        Default d = new Default(1024, conceptsFirePerCycle, 3);
        Focus c = d.focus();
        d.setFocus(new Focus() {
            @Override
            public void activate(Concept term, float priToAdd) {

                c.activate(term, priToAdd);
                m.value(priToAdd, term.term());
            }

            @Override
            public void sample(int max, IntObjectToIntFunction<? super PLink<Concept>> p) {
                c.sample(max, p);
            }

            @Override
            public float pri(@NotNull Termed concept) {
                return c.pri(concept);
            }

            @Override
            public Iterable<PLink<Concept>> concepts() {
                return c.concepts();
            }
        });

        d.input("a:b.", "b:c.", "c:d.").run(cycles);
        System.out.println(m);

    }

    private static <X extends Termed> MultiStatistics.Condition<X> complexityLTE(int complexityLTE) {
        return new MultiStatistics.Condition<X>("complexity<=" + complexityLTE, t -> t.complexity() <= complexityLTE);
    }
    private static <X extends Termed> MultiStatistics.Condition<X> volumeLTE(int complexityLTE) {
        return new MultiStatistics.Condition<X>("volume<=" + complexityLTE, t -> t.volume() <= complexityLTE);
    }
    private static <X extends Termed> MultiStatistics.Condition<X> volumeIn(int min, int max) {
        return new MultiStatistics.Condition<X>("volume=" + min + ".." + max, t -> {
            int v = t.volume();
            return (v >= min && v <= max);
        });
    }
}
