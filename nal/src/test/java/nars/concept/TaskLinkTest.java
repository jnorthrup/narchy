package nars.concept;

import jcog.pri.bag.Bag;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Task;
import nars.link.TaskLink;
import org.apache.commons.math3.stat.Frequency;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class TaskLinkTest {

    /** when sampling links, weaker beliefs should be selected at a proportional rate.
     * dont allow return the top belief tasklinks
     */
    @Test public void testEnsureFairBeliefSelection() {
        float confA = 0.9f, confB = 0.5f;
        Frequency f = sampleLink((n)->{
            try {
                n.believe("x", 1f, confA);
                n.believe("x", 1f, confB);
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }
        }, 64);

        System.out.println(f);

        //test relative selection frequency
        assertEquals(2, f.getUniqueCount());
        Iterator<Comparable<?>> ff = f.valuesIterator();
        Comparable<?> aa = ff.next();
        Comparable<?> bb = ff.next();
        assertTrue((f.getPct(bb) / f.getPct(aa)) > 1.5f);  //some significant difference


    }



    @Test public void testEnsureFairQuestionSelection() {
        Frequency f = sampleLink((n)->{

                Task a = n.question("x");
                a.pri(0.25f);
                Task b = n.question("x");
                b.pri(0.5f);

        },64);

        System.out.println(f);
        assertEquals(2, f.getUniqueCount());

        assertEquals(2, f.getUniqueCount());
        Iterator<Comparable<?>> ff = f.valuesIterator();
        Comparable<?> aa = ff.next();
        Comparable<?> bb = ff.next();
        assertTrue((f.getPct(bb) / f.getPct(aa)) > 1.75f);  //some significant difference
    }

    private static Frequency sampleLink(Consumer<NAR> setup, int samples) {
        NAR n = NARS.shell();
        setup.accept(n);
        n.run(1);

        Bag<?, TaskLink> links = n.concept("x").tasklinks();
        assertEquals(1, links.size());
        @Nullable TaskLink l = links.iterator().next();
        assertNotNull(l);

        Frequency f = new Frequency();
        for (int i = 0; i < samples; i++)
            f.addValue(l.apply(n).toString());
        return f;
    }
}
