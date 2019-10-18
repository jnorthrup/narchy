package nars.term.util.transform;

import jcog.random.XorShift128PlusRandom;
import jcog.version.Versioned;
import nars.$;
import nars.NAL;
import nars.Narsese;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Variable;
import nars.term.var.ellipsis.Ellipsis;
import nars.unify.Unify;
import nars.unify.mutate.Choose1;
import nars.unify.mutate.Choose2;
import nars.unify.mutate.CommutivePermutations;
import nars.unify.mutate.Termutator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static nars.$.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by me on 12/22/15.
 */
class TermutatorTest {

    private static final int TTL = 256;

    private final Unify unifier = new Unify(Op.VAR_PATTERN, new XorShift128PlusRandom(1),
            NAL.unify.UNIFICATION_STACK_CAPACITY, TTL) {
        @Override public boolean match() {
            return false;
        }
    };

    @Test
    void testChoose1_2() {

        assertTermutatorProducesUniqueResults(
                new Choose1(e1, p2,
                        ((Compound)p("a", "b")).toSetSorted()), 2);

    }

    @Test
    void testChoose1_3() {

        assertTermutatorProducesUniqueResults(
                new Choose1(e1, p2,
                        ((Compound)p("a", "b", "c")).toSetSorted()), 3);
    }

    @Test
    void testChoose1_4() {

        assertTermutatorProducesUniqueResults(
                new Choose1(e1, p2,
                        ((Compound)p("a", "b", "c", "d")).toSetSorted()), 4);
    }


    private static final Term e0;
    static {
        
        Term ee0;
        try {
            ee0 = $("%A..+");
        } catch (Narsese.NarseseException e) {
            ee0 = null;
            e.printStackTrace();
            System.exit(1);
        }
        e0 = ee0;
    }
    private static final Ellipsis e1 = Ellipsis.EllipsisPrototype.make((byte) 1,1);


    private static final Variable p2= v(Op.VAR_PATTERN, (byte) 2);
    private static final SortedSet<Term> p2p3 = ((Compound)$.p( p2, v(Op.VAR_PATTERN, (byte) 3) )).toSetSorted();

    @Test
    void testChoose2_2() {



        assertTermutatorProducesUniqueResults(
                new Choose2(e1, p2p3, ((Compound)p("a", "b")).toSetSorted()
                ), 2);
    }

    @Test
    void testChoose2_3() {

        assertTermutatorProducesUniqueResults(
                new Choose2(e1, p2p3, ((Compound)p("a", "b", "c")).toSetSorted()
                ), 6);
    }
    @Test
    void testChoose2_4() {

        Set<String> series = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            String s = assertTermutatorProducesUniqueResults(
                    new Choose2(e1, p2p3, ((Compound) p("a", "b", "c", "d")).toSetSorted()
                    ), 12);
            series.add(s);
        }

        assertTrue(series.size() > 1); 
    }



    @Test
    void testComm2() throws Narsese.NarseseException {
        assertTermutatorProducesUniqueResults(
                new CommutivePermutations($("{%A,%B}"),
                        $("{x,y}")), 2);
    }
    @Test
    void testComm3() throws Narsese.NarseseException {
        assertTermutatorProducesUniqueResults(
                new CommutivePermutations($("{%A,%B,%C}"),
                        $("{x,y,z}")), 6);
    }
    @Test
    void testComm3Conj() throws Narsese.NarseseException {
        assertTermutatorProducesUniqueResults(
                new CommutivePermutations($("(&&,%A,%B,%C)"),
                        $("(&&,x,y,z)")), 6);
    }
    @Test
    void testComm4() throws Narsese.NarseseException {
        assertTermutatorProducesUniqueResults(
                new CommutivePermutations($("{%A,%B,%C,%D}"),
                        $("{w,x,y,z}")), 24);
    }

    private String assertTermutatorProducesUniqueResults(@NotNull Termutator t, int num) {

        

        Set<String> s = new LinkedHashSet(); 
        int[] actual = {0};
        
        int[] duplicates = {0};

        unifier.setTTL(TTL);
        

        t.mutate(new Termutator[] { t, (chain, current, u) -> {
            TreeMap t1 = new TreeMap();
            for (Map.Entry<Variable, Versioned<Term>> entry : u.xy.map.entrySet()) {
                Variable key = entry.getKey();
                Versioned<Term> value = entry.getValue();
                t1.put(key, value);
            }

            if (s.add( t1.toString() )) {
                actual[0]++;
            } else {
                duplicates[0]++;
            }

        }}, 0, unifier);


        String res = s.toString();
        System.out.println(res);

        assertEquals(num, s.size());
        assertEquals(num, actual[0]);
        assertEquals(0, duplicates[0]);

        return res;
    }

}