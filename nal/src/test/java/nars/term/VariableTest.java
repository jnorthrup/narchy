package nars.term;

import nars.*;
import nars.term.atom.Atomic;
import nars.util.TimeAware;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 8/28/15.
 */
class VariableTest {


    @Test
    void testPatternVarVolume() throws Narsese.NarseseException {

        assertEquals(0, Narsese.term("$x").complexity());
        assertEquals(1, Narsese.term("$x").volume());

        assertEquals(0, Narsese.term("%x").complexity());
        assertEquals(1, Narsese.term("%x").volume());

        assertEquals(Narsese.term("<x --> y>").volume(),
                Narsese.term("<%x --> %y>").volume());

    }

    @Test
    void testNumVars() throws Narsese.NarseseException {
        assertEquals(1, Narsese.term("$x").vars());
        assertEquals(1, Narsese.term("#x").vars());
        assertEquals(1, Narsese.term("?x").vars());
        assertEquals(1, Narsese.term("%x").vars());

        assertEquals(2, $("<$x <-> %y>").vars());
    }

































    @Test
    void testBooleanReductionViaHasPatternVar() throws Narsese.NarseseException {
        Compound d = $("<a <-> <$1 --> b>>");
        assertEquals(0,  d.varPattern() );

        Compound c = $("<a <-> <%1 --> b>>");
        assertEquals(1,  c.varPattern() );

        Compound e = $("<%2 <-> <%1 --> b>>");
        assertEquals(2,  e.varPattern() );

    }

    /** tests term sort order consistency */
    @Test
    void testVariableSubtermSortAffect0() {

        assertEquals(-1, $.varIndep(1).compareTo($.varIndep(2)));


        Compound k1 = $.inh($.varIndep(1), Atomic.the("key")); 
        Compound k2 = $.inh($.varIndep(2), Atomic.the("key")); 
        Compound l1 = $.inh($.varIndep(1), Atomic.the("lock")); 
        Compound l2 = $.inh($.varIndep(2), Atomic.the("lock")); 
        assertEquals(-1, k1.compareTo(k2));
        assertEquals(+1, k2.compareTo(k1));
        assertEquals(-1, l1.compareTo(l2));
        assertEquals(+1, l2.compareTo(l1));

        assertEquals(l1.compareTo(k1), -k1.compareTo(l1));
        assertEquals(l2.compareTo(k2), -k2.compareTo(l2));

        
        assertEquals(+1, k1.compareTo(l2));
        assertEquals(+1, k2.compareTo(l1));
        assertEquals(-1, l2.compareTo(k1));
        assertEquals(-1, l1.compareTo(k2));


        testVariableSorting("(($1-->key)&&($2-->lock))", "(($1-->lock)&&($2-->key))");

    }

    /** tests term sort order consistency */
    @Test
    void testVariableSubtermSortAffectNonComm() {

        testVariableSorting("(($1-->key),($2-->lock))", "(($2-->key),($1-->lock))");

    }

    private static void testVariableSorting(String a, String b) {
        Compound A = raw(a);
        Compound B = raw(b);

        
        assertNotEquals(A, B);

        
        Term NA = A.normalize();
        Term NB = B.normalize();
        System.out.println(A + "\t" + B);
        System.out.println(NA + "\t" + NB);

        assertEquals(NA, NB);
    }

    private static Compound raw(String a) {
        try {
            return (Compound) Narsese.term(a, false);
        } catch (Narsese.NarseseException e) {
            fail(e);
            return null;
        }
   }

    /** tests term sort order consistency */
    @Test
    void testVariableSubtermSortAffect1() {

        testVariableSorting(
                "((($1-->lock)&&($2-->key))==>open($2,$1))",
                "((($1-->key)&&($2-->lock))==>open($1,$2))"
                );
        testVariableSorting(
                "((($1-->key)&&($2-->lock))==>open($1,$2))",
                "((($1-->lock)&&($2-->key))==>open($2,$1))"

                );

    }
















    @Test
    void testDestructiveNormalization() throws Narsese.NarseseException {
        String t = "<$x --> y>";
        String n = "($1-->y)";
        TimeAware timeAware = NARS.shell();
        Termed x = $.$(t);
        assertEquals(n, x.toString());
        

    }

















    @Test
    void varNormTestIndVar() throws Narsese.NarseseException {
        

        NAR n = NARS.shell();

        String t = "<<($1, $2) --> bigger> ==> <($2, $1) --> smaller>>";

        Termed term = $.$(t);
        Task task = Narsese.the().task(t + '.', n);

        System.out.println(t);
        assertEquals("(bigger($1,$2)==>smaller($2,$1))", task.term().toString());
        System.out.println(term);
        System.out.println(task);


        Task t2 = n.inputTask(t + '.');
        System.out.println(t2);

        
        n.run(10);

    }

}
