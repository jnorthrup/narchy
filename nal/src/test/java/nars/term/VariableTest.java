package nars.term;

import nars.*;
import nars.term.atom.Atomic;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 8/28/15.
 */
public class VariableTest {


    @Test
    public void testPatternVarVolume() throws Narsese.NarseseException {

        assertEquals(0, Narsese.term("$x").complexity());
        assertEquals(1, Narsese.term("$x").volume());

        assertEquals(0, Narsese.term("%x").complexity());
        assertEquals(1, Narsese.term("%x").volume());

        assertEquals(Narsese.term("<x --> y>").volume(),
                Narsese.term("<%x --> %y>").volume());

    }

    @Test public void testNumVars() throws Narsese.NarseseException {
        assertEquals(1, Narsese.term("$x").vars());
        assertEquals(1, Narsese.term("#x").vars());
        assertEquals(1, Narsese.term("?x").vars());
        assertEquals(0, Narsese.term("%x").vars());

        //the pattern variable is not counted toward # vars
        assertEquals(1, $("<$x <-> %y>").vars());
    }

//    @Test
//    public void testIndpVarNorm() {
//        assertEquals(2, $.$("<$x <-> $y>").vars());
//
//        testIndpVarNorm("$x", "$y", "($1,$2)");
//        testIndpVarNorm("$x", "$x", "($1,$1)");
//        testIndpVarNorm("$x", "#x", "($1,#2)");
//        testIndpVarNorm("#x", "#x", "(#1,#1)");
//    }
//
//    @Test
//    public void testIndpVarNormCompound() {
//        //testIndpVarNorm("<$x <-> $y>", "<$x <-> $y>", "(<$1 <-> $2>, <$3 <-> $4>)");
//
//        testIndpVarNorm("$x", "$x", "($1,$1)");
//        testIndpVarNorm("#x", "#x", "(#1,#1)");
//        testIndpVarNorm("<#x<->#y>", "<#x<->#y>", "((#1<->#2),(#1<->#2))");
//        testIndpVarNorm("<$x<->$y>", "<$x<->$y>", "(($1<->$2),($1<->$2))");
//    }
//    public void testIndpVarNorm(String vara, String varb, String expect) {
//
//
//        Term a = $.$(vara);
//        Term b = $.$(varb);
//        //System.out.println(a + " " + b + " "  + Product.make(a, b).normalized().toString());
//
//        assertEquals(
//            expect,
//            t.concept($.p(a, b)).toString()
//        );
//    }

    @Test public void testBooleanReductionViaHasPatternVar() throws Narsese.NarseseException {
        Compound d = $("<a <-> <$1 --> b>>");
        assertEquals(0,  d.varPattern() );

        Compound c = $("<a <-> <%1 --> b>>");
        assertEquals(1,  c.varPattern() );

        Compound e = $("<%2 <-> <%1 --> b>>");
        assertEquals(2,  e.varPattern() );

    }

    /** tests term sort order consistency */
    @Test public void testVariableSubtermSortAffect0() {

        assertEquals(-1, $.varIndep(1).compareTo($.varIndep(2)));


        Compound k1 = $.inh($.varIndep(1), Atomic.the("key")); //raw("($1 --> key)");
        Compound k2 = $.inh($.varIndep(2), Atomic.the("key")); //raw("($2 --> key)");
        Compound l1 = $.inh($.varIndep(1), Atomic.the("lock")); //raw("($1 --> lock)");
        Compound l2 = $.inh($.varIndep(2), Atomic.the("lock")); //raw("($2 --> lock)");
        assertEquals(-1, k1.compareTo(k2));
        assertEquals(+1, k2.compareTo(k1));
        assertEquals(-1, l1.compareTo(l2));
        assertEquals(+1, l2.compareTo(l1));

        assertEquals(l1.compareTo(k1), -k1.compareTo(l1));
        assertEquals(l2.compareTo(k2), -k2.compareTo(l2));

        //HERE IS THE EXCEPTION: non-variable terms need to be compared first, because normalization implicitly involves the subservience of variables to non-variables
        assertEquals(+1, k1.compareTo(l2));
        assertEquals(+1, k2.compareTo(l1));
        assertEquals(-1, l2.compareTo(k1));
        assertEquals(-1, l1.compareTo(k2));


        testVariableSorting("(($1-->key)&&($2-->lock))", "(($1-->lock)&&($2-->key))");

    }

    /** tests term sort order consistency */
    @Test public void testVariableSubtermSortAffectNonComm() {

        testVariableSorting("(($1-->key),($2-->lock))", "(($2-->key),($1-->lock))");

    }

    static void testVariableSorting(String a, String b) {
        Compound A = raw(a);
        Compound B = raw(b);

        //not equal...
        assertNotEquals(A, B);

        //but normalization should make them equal:
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
    @Test public void testVariableSubtermSortAffect1() {

        testVariableSorting(
                "((($1-->lock)&&($2-->key))==>open($2,$1))",
                "((($1-->key)&&($2-->lock))==>open($1,$2))"
                );
        testVariableSorting(
                "((($1-->key)&&($2-->lock))==>open($1,$2))",
                "((($1-->lock)&&($2-->key))==>open($2,$1))"

                );

    }
//    @Test public void testEqualityOfVariablesAndTheirConceptInstances() {
//        @NotNull VarPattern vp0 = $.varPattern(0);
//        VariableConcept vc0 = new VariableConcept(vp0);
//        assertEquals(vp0, vc0);
//        assertEquals(vc0, vp0); //reverse
//        assertEquals(vp0, vc0.term());
//    }

//    @Test public void testTransformVariables() {
//        NAR nar = new Default();
//        Compound c = nar.term("<$a --> x>");
//        Compound d = Compound.transformIndependentToDependentVariables(c).normalized();
//        assertTrue(c!=d);
//        assertEquals(d, nar.term("<#1 --> x>"));
//    }

    @Test
    public void testDestructiveNormalization() throws Narsese.NarseseException {
        String t = "<$x --> y>";
        String n = "($1-->y)";
        NAR nar = NARS.shell();
        Termed x = $.$(t);
        assertEquals(n, x.toString());
        //assertTrue("immediate construction of a term from a string should automatically be normalized", x.isNormalized());

    }




//    public void combine(String a, String b, String expect) {
//        NAR n = new Default();
//        Term ta = n.term(a);
//        Term tb = n.term(b);
//        Term c = Conjunction.make(ta, tb).normalized();
//
//        Term e = n.term(expect).normalized();
//        Term d = e.normalized();
//        assertNotNull(e);
//        assertEquals(d, c);
//        assertEquals(e, c);
//    }

    @Test public void varNormTestIndVar() throws Narsese.NarseseException {
        //<<($1, $2) --> bigger> ==> <($2, $1) --> smaller>>. gets changed to this: <<($1, $4) --> bigger> ==> <($2, $1) --> smaller>>. after input

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

        //TextOutput.out(n);
        n.run(10);

    }

}
