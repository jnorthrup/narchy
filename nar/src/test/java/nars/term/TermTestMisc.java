package nars.term;

import nars.*;
import nars.concept.Concept;
import nars.subterm.ArrayTermVector;
import nars.subterm.Subterms;
import nars.subterm.UniSubterm;
import nars.task.RevisionTest;
import nars.task.util.TaskException;
import nars.term.atom.Atomic;
import nars.term.atom.IdempotentBool;
import nars.term.util.TermException;
import nars.term.util.TermTest;
import nars.util.Timed;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.TreeSet;
import java.util.function.Supplier;

import static java.lang.Long.toBinaryString;
import static nars.$.$;
import static nars.$.$$;
import static nars.Op.*;
import static org.junit.jupiter.api.Assertions.*;

/** target tests to be organzied more specifically */
public class TermTestMisc {


    private final NAR n = NARS.shell();

    @Test
    void testInstantiateBoolsFromEquivString() {
        for (Term b : new Term[]{IdempotentBool.True, IdempotentBool.False, IdempotentBool.Null})
            assertSame(b, $.the(b.toString()));
    }

    @Test
    void testIntifyVarCountOfSubtermsContainingVars() throws Narsese.NarseseException {
        assertEquals(2, $.$("(addAt(s(s(0)),s(s(0)),?R)==>goal(?R))").varQuery());
    }


    @Test
    void testSetCommutivity() throws Exception {

        assertEquals("{a,b}", $.$("{b,a}").toString());
        assertEquals("{a,b}", $.$("{a,b}").toString());
        assertEquals("{a,b}", SETe.the($.the("a"), $.the("b")).toString());
        assertEquals("{a,b}", SETe.the($.the("b"), $.the("a")).toString());

        TermTest.assertEquivalentTerm("{b,a}", "{b,a}");
        TermTest.assertEquivalentTerm("{a,b}", "{b,a}");


        TermTest.assertEquivalentTerm("{b,a,c}", "{b,a,c}");
        TermTest.assertEquivalentTerm("{b,a,c}", "{a,c,b}");
        TermTest.assertEquivalentTerm("{b,a,c}", "{b,c,a}");

        TermTest.assertEquivalentTerm("[a,c,b]", "[b,a,c]");
    }


    @Test
    void testSimCommutivity() throws Exception {
        TermTest.assertEquivalentTerm("<{Birdie}<->{Tweety}>", "<{Tweety}<->{Birdie}>");

        TermTest.assertEq($.$("<{Birdie}<->{Tweety}>"), $.<Term>$("<{Tweety}<->{Birdie}>"));

        TermTest.assertEq($.sim($.$("{Birdie}"), $.$("{Tweety}")), $.<Term>sim($.$("{Tweety}"), $.$("{Birdie}")));


    }

    @Test
    void testCommutativivity() {
        Atomic W = Atomic.the("w");
        Atomic X = Atomic.the("x");
        Atomic Y = Atomic.the("y");
        Atomic Z = Atomic.the("z");
        Op.all().filter(x -> x.commutative).forEach(o -> {
            if (o.minSubs < 2)
                assertFalse(o.the(X).isCommutative());

            Term xy = o.the(X, Y);
            assertTrue(xy.isCommutative());
            assertEquals(xy, o.the(Y, X), () -> "commutivity failed for " + xy);
            assertEquals(xy, o.the(List.of(Y, X)), () -> "commutivity failed for " + xy);
            assertEquals(xy, o.the(java.util.Set.of(Y, X)), () -> "commutivity failed for " + xy);

            if (o.maxSubs > 2) {
                Term xyz = o.the(X, Y, Z);
                assertTrue(xy.isCommutative());
                assertEquals(xyz, o.the(Y, X, Z), () -> "commutivity failed for " + xyz);
                assertEquals(xyz, o.the(List.of(Y, X, Z)), () -> "commutivity failed for " + xyz);
                assertEquals(xyz, o.the(java.util.Set.of(Y, X, Z)), () -> "commutivity failed for " + xyz);
                assertEquals(xyz, o.the(X, Z, Y), () -> "commutivity failed for " + xyz);
                assertEquals(xyz, o.the(X, Y, Z), () -> "commutivity failed for " + xyz);
                assertEquals(xyz, o.the(Z, Y, X), () -> "commutivity failed for " + xyz);
                assertEquals(xyz, o.the(Z, X, Y), () -> "commutivity failed for " + xyz);
                assertEquals(xyz, o.the(Y, Z, X), () -> "commutivity failed for " + xyz);

                Term wxyz = o.the(W, X, Y, Z);
                assertEquals(wxyz, o.the(Y, W, X, Z), () -> "commutivity failed for " + wxyz);
                assertEquals(wxyz, o.the(List.of(Y, W, X, Z)), () -> "commutivity failed for " + wxyz);
                assertEquals(wxyz, o.the(java.util.Set.of(Y, W, X, Z)), () -> "commutivity failed for " + wxyz);
                //TODO other permutes
            }
        });
    }


    @Test
    void testTermSort() throws Exception {


        Term a = $.$("a").term();
        Term b = $.$("b").term();
        Term c = $.$("c").term();

        assertEquals(3, Terms.commute(a, b, c).length);
        assertEquals(2, Terms.commute(a, b, b).length);
        assertEquals(1, Terms.commute(a, a).length);
        assertEquals(1, Terms.commute(a).length);
        assertEquals(a, Terms.commute(a, b)[0], "correct natural ordering");
        assertEquals(a, Terms.commute(a, b, c)[0], "correct natural ordering");
        assertEquals(b, Terms.commute(a, b, c)[1], "correct natural ordering");
        assertEquals(c, Terms.commute(a, b, c)[2], "correct natural ordering");
        assertEquals(b, Terms.commute(a, a, b, b, c, c)[1], "correct natural ordering");

    }


    @Test
    void testConjunctionTreeSet() throws Narsese.NarseseException {


        String term1String = "<#1 --> (&,boy,(taller_than,{Tom},_))>";
        Term term1 = $.$(term1String).term();
        String term1Alternate = "<#1 --> (&,(taller_than,{Tom},_),boy)>";
        Term term1a = $.$(term1Alternate).term();


        Term term2 = $.$("<#1 --> (|,boy,(taller_than,{Tom},_))>").term();

        assertEquals(term1a.toString(), term1.toString());
        assertTrue(term1.complexity() > 1);
//        assertEquals(term1.complexity(), term2.complexity());

        assertSame(term1.op(), INH);


        boolean t1e2 = term1.equals(term2);
        int t1c2 = term1.compareTo(term2);
        int t2c1 = term2.compareTo(term1);

        assertTrue(!t1e2);
        assertTrue(t1c2 != 0, "term1 and term2 inequal, so t1.compareTo(t2) should not = 0");
        assertTrue(t2c1 != 0, "term1 and term2 inequal, so t2.compareTo(t1) should not = 0");

    /*
    System.out.println("t1 equals t2 " + t1e2);
    System.out.println("t1 compareTo t2 " + t1c2);
    System.out.println("t2 compareTo t1 " + t2c1);
    */

        TreeSet<Term> set = new TreeSet<>();
        boolean added1 = set.add(term1);
        boolean added2 = set.add(term2);
        assertTrue(added1, "target 1 added to setAt");
        assertTrue(added2, "target 2 added to setAt");

        assertEquals(2, set.size());

    }

    @Test
    void testUnconceptualizedTermInstancing() throws Narsese.NarseseException {

        String term1String = "<a --> b>";
        Term term1 = $.$(term1String).term();
        Term term2 = $.$(term1String).term();

        assertEquals(term1, term2);
        assertEquals(term1.hashCode(), term2.hashCode());

        Compound cterm1 = ((Compound) term1);
        Compound cterm2 = ((Compound) term2);


        assertEquals(cterm1.sub(0), cterm2.sub(0));

    }


    /**
     * test consistency between subterm conceptualization and target conceptualization
     */
    @Test
    void testRootOfImplWithConj() throws Narsese.NarseseException {
        String ys = "((--,tetris(isRow,13,true))&&tetris(isRowClear,6,true))";

        Term y = $.$(ys);
        String yc;
        assertEquals("((--,tetris(isRow,13,true)) &&+- tetris(isRowClear,6,true))", yc = y.concept().toString());

        assertEquals(yc, y.root().toString());

        Term x = $.$("(tetris(isRowClear,10,true) ==>+- " + ys + ')');
        assertEquals("(tetris(isRowClear,10,true) ==>+- " + yc + ')', x.concept().toString());

    }

    @Test
    void testNormalizedConceptTerm() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        assertEquals("(#1-->x)", n.conceptualize("(#2 --> x)").term().toString());
        assertEquals("((#1-->x),y)", n.conceptualize("((#2 --> x),y)").term().toString());
    }

    @Test
    void invalidTermIndep() {


        try {
            String t = "($1-->({place4}~$1))";
            Task x = n.inputTask(t + '.');
            fail(() -> t + " is invalid compound target");
        } catch (Throwable tt) {
            assertTrue(true);
        }

        try {
            Term subj = $.varIndep(1);
            Term pred = $.$("(~,{place4},$1)").term();

            assertTrue(true);

        } catch (Throwable ex) {
            ex.printStackTrace();
            fail(ex);
        }


    }

    @Test
    void testValidIndep() {
        assertTrue(Task.validTaskTerm(
                $$("(($1 &&+4 $1) ==>-2 ((--,angX) &&+8 $1))")));
    }

    @Deprecated
    static boolean isOperation(Termed _t) {
        Term t = _t.term();
        if (t.op() == Op.INH) {
            Compound c = (Compound) t;
            return c.subIs(1, Op.ATOM) &&
                    c.subIs(0, Op.PROD);
        }
        return false;
    }

    @Test
    void testParseOperationInFunctionalForm() throws Narsese.NarseseException {


        Term x = $.$("wonder(a,b)").term();
        assertEquals(INH, x.op());
        assertTrue(isOperation(x));
        assertEquals("wonder(a,b)", x.toString());


    }


//    @Test
//    void testFromEllipsisMatch() {
//        Term xy = EllipsisMatch.matched($.the("x"), $.the("y"));
//
//        for (Op o : new Op[]{Op.SECTi, SECTe, CONJ}) {
//            Term z = o.the(DTERNAL, xy);
//            assertEquals("(x" + o.str + "y)", z.toString());
//            assertEquals(3, z.volume());
//            assertEquals(Op.ATOM, z.sub(0).op());
//            assertEquals(Op.ATOM, z.sub(1).op());
//        }
//    }


    @Test
    void testPatternVar() throws Narsese.NarseseException {
        assertSame($("%x").op(), Op.VAR_PATTERN);
    }

    @Test
    void termEqualityWithQueryVariables() throws Narsese.NarseseException {

        String a = "<?1-->bird>";
        assertEquals($.$(a), $.$(a));
        String b = "<bird-->?1>";
        assertEquals($.$(b), $.$(b));
    }

    private static void testTermEqualityNonNormalizing(@NotNull String s) {
        try {
            testTermEquality(s, false);
        } catch (Narsese.NarseseException e) {
            fail(e);
        }
    }

    private static void testTermEquality(@NotNull String s) {
        try {
            testTermEquality(s, true);
        } catch (Narsese.NarseseException e) {
            fail(e);
        }
    }


    private static void testTermEquality(@NotNull String s, boolean conceptualize) throws Narsese.NarseseException {


        Term a = $.$(s).term();

        NAR n2 = NARS.shell();
        Term b = $.$(s).term();


        if (a instanceof Compound) {
            assertEquals(a.subterms(), b.subterms());
        }
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(a.toString(), b.toString());

        assertEquals(a, b);

        assertEquals(a.compareTo(a), a.compareTo(b));
        assertEquals(0, b.compareTo(a));

        if (conceptualize) {
            Concept n2a = n2.conceptualize(a);
            assertNotNull(n2a, () -> a + " should conceptualize");
            assertNotNull(b);
            assertEquals(n2a.toString(), b.toString());
            assertEquals(n2a.hashCode(), b.hashCode());
            assertEquals(n2a.term(), b);
        }

    }

    @Test
    void termEqualityOfVariables1() {
        testTermEqualityNonNormalizing("#1");
    }

    @Test
    void termEqualityOfVariables2() {
        testTermEqualityNonNormalizing("$1");
    }

    @Test
    void termEqualityOfVariables3() {
        testTermEqualityNonNormalizing("?1");
    }

    @Test
    void termEqualityOfVariables4() {
        testTermEqualityNonNormalizing("%1");
    }


    @Test
    void termEqualityWithVariables1() {
        testTermEqualityNonNormalizing("<#2 --> lock>");
    }

    @Test
    void termEqualityWithVariables2() {
        testTermEquality("<<#2 --> lock> --> x>");
    }

    @Test
    void termEqualityWithVariables3() throws Narsese.NarseseException {
        testTermEquality("(&&, x, <#2 --> lock>)", false);
        testTermEquality("(&&, x, <#1 --> lock>)", false);
    }

    @Test
    void termEqualityWithVariables4() throws Narsese.NarseseException {
        testTermEquality("(&&, <<$1 --> key> ==> <#2 --> ( open, $1 )>>, <#2 --> lock>)", false);
    }

    @Test
    void termEqualityWithMixedVariables() throws Narsese.NarseseException {

        String s = "(&&, <<$1 --> key> ==> <#2 --> ( open, $1 )>>, <#2 --> lock>)";
        Termed a = $.$(s);

        Timed n2 = NARS.shell();
        Termed b = $.$(s);


        assertEquals(a, b);


    }


    @Test
    void statementHash() throws Narsese.NarseseException {

        statementHash("i4", "i2");
        statementHash("{i4}", "{i2}");
        statementHash("<{i4} --> r>", "<{i2} --> r>");


        statementHash("<<{i4} --> r> ==> A(7)>", "<<{i2} --> r> ==> A(8)>");

        statementHash("<<{i4} --> r> ==> A(7)>", "<<{i2} --> r> ==> A(7)>");

    }

    @Test
    void statementHash2() throws Narsese.NarseseException {
        statementHash("<<{i4} --> r> ==> A(7)>", "<<{i2} --> r> ==> A(9)>");
    }

    @Test
    void statementHash3() throws Narsese.NarseseException {


        statementHash("<<{i0} --> r> ==> A(8)>", "<<{i1} --> r> ==> A(7)>");


        statementHash("<<{i10} --> r> ==> A(1)>", "<<{i11} --> r> ==> A(0)>");
    }

    private static void statementHash(@NotNull String a, @NotNull String b) throws Narsese.NarseseException {


        Term ta = $(a);
        Term tb = $(b);

        assertNotEquals(ta, tb);
        assertNotEquals(ta.hashCode(),
                tb.hashCode(), () -> ta + " vs. " + tb);


    }

    @Test
    void testHashConsistent() {
        Term x = $.the("z");
        Subterms a = new UniSubterm(x);
        Subterms b = new ArrayTermVector(x);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(a.hashCodeSubterms(), b.hashCodeSubterms());
        assertEquals(a.toString(), b.toString());
    }

    @Test
    void testHashDistribution() {
        int ah = new UniSubterm($.the("x")).hashCode();
        int bh = new UniSubterm($.the("y")).hashCode();
        assertTrue(Math.abs(ah - bh) > 1, () -> ah + " vs " + bh);
    }

    @Test
    void testTermComplexityMass() throws Narsese.NarseseException {


        testTermComplexityMass(n, "x", 1, 1);

        testTermComplexityMass(n, "#x", 0, 1, 0, 1, 0);
        testTermComplexityMass(n, "$x", 0, 1, 1, 0, 0);
        testTermComplexityMass(n, "?x", 0, 1, 0, 0, 1);

        testTermComplexityMass(n, "<a --> b>", 3, 3);
        testTermComplexityMass(n, "<#a --> b>", 2, 3, 0, 1, 0);

        testTermComplexityMass(n, "<a --> (c & d)>", 5, 5);
        testTermComplexityMass(n, "<$a --> (c & #d)>", 3, 5, 1, 1, 0);
    }

    private static void testTermComplexityMass(@NotNull Timed n, @NotNull String x, int complexity, int mass) throws Narsese.NarseseException {
        testTermComplexityMass(n, x, complexity, mass, 0, 0, 0);
    }

    private static void testTermComplexityMass(@NotNull Timed n, @NotNull String x, int complexity, int mass, int varIndep, int varDep, int varQuery) throws Narsese.NarseseException {
        Term t = $.$(x).term();

        assertNotNull(t);
        assertEquals(complexity, t.complexity());
        assertEquals(mass, t.volume());

        assertEquals(varDep, t.varDep());
        assertEquals(varDep != 0, t.hasVarDep());

        assertEquals(varIndep, t.varIndep());
        assertEquals(varIndep != 0, t.hasVarIndep());

        assertEquals(varQuery, t.varQuery());
        assertEquals(varQuery != 0, t.hasVarQuery());

        assertEquals(varDep + varIndep + varQuery, t.vars());
        assertEquals((varDep + varIndep + varQuery) != 0, t.vars() > 0);
    }

    @NotNull
    static <C extends Compound> C testStructure(@NotNull String term, String bits) throws Narsese.NarseseException {

        C a = (C) $.$(term).term();
        assertEquals(bits, toBinaryString(a.structure()));
        assertEquals(term, a.toString());
        return a;
    }


    public static void assertValid(String o) {
        assertEquals(o, assertValid($$(o)).toString());
    }

    public static Term assertValid(Term o) {
        assertNotNull(o);
        assertTrue(!(o instanceof IdempotentBool));
        return o;
    }

    static void assertValidTermValidConceptInvalidTaskContent(@NotNull Supplier<Term> o) {
        try {
            Term x = o.get();
            assertNotNull(x);

            NAR t = NARS.shell();
            t.believe(x);

            fail(() -> x + " should not have been allowed as a task content");


        } catch (Exception e) {

        }
    }


    public static void assertValidTermValidConceptInvalidTaskContent(String o) {
        assertThrows(TaskException.class, () -> NARS.shell().input(o));
    }


    @Test
    void testSubTermStructure() throws Narsese.NarseseException {
        Assertions.assertTrue(RevisionTest.x.term().impossibleSubTerm(RevisionTest.x.term()));
        assertTrue(!RevisionTest.x.hasAll($.$("(a-->#b)").term().structure()));
    }

    @Test
    void testCommutativeWithVariableEquality() throws Narsese.NarseseException {

        Termed a = $.$("<(&&, <#1 --> M>, <#2 --> M>) ==> <#2 --> nonsense>>");
        Termed b = $.$("<(&&, <#2 --> M>, <#1 --> M>) ==> <#2 --> nonsense>>");
        assertEquals(a, b);

        Termed c = $.$("<(&&, <#1 --> M>, <#2 --> M>) ==> <#1 --> nonsense>>");
        assertNotEquals(a, c);

        Termed x = $.$("(&&, <#1 --> M>, <#2 --> M>)");
        Term xa = x.term().sub(0);
        Term xb = x.term().sub(1);
        int o1 = xa.compareTo(xb);
        int o2 = xb.compareTo(xa);
        assertEquals(o1, -o2);
        assertNotEquals(0, o1);
        assertNotEquals(xa, xb);
    }

    @Test
    void testHash1() throws Narsese.NarseseException {
        testUniqueHash("(A --> B)", "(A <-> B)");
        testUniqueHash("(A --> B)", "(A ==> B)");
        testUniqueHash("A", "B");
        testUniqueHash("%1", "%2");
        testUniqueHash("%A", "A");
        testUniqueHash("$1", "A");
        testUniqueHash("$1", "#1");
    }

    private static void testUniqueHash(@NotNull String a, @NotNull String b) throws Narsese.NarseseException {

        Timed t = NARS.shell();
        int h1 = $.$(a).hashCode();
        int h2 = $.$(b).hashCode();
        assertNotEquals(h1, h2);
    }

    @Test
    void testSetOpFlags() throws Narsese.NarseseException {
        Term e = $("{x}");
        assertTrue(e.op().set);
        Term i = $("[y]");
        assertTrue(i.op().set);
        assertFalse($("x").op().set);
        assertFalse($("a:b").op().set);
    }

    @Test
    void testEmptyProductEquality() throws Narsese.NarseseException {
        assertEquals($("()"), $("()"));
        assertEquals(EmptyProduct, $("()"));
    }


    static void assertInvalid(@NotNull String o) {
        assertThrows(TermException.class, () -> $(o));
    }

    @Test
    void reuseVariableTermsDuringNormalization2() throws Narsese.NarseseException {
        for (String v : new String[]{"?a", "?b", "#a", "#c"}) {
            Compound x = $(String.join(v, "((", " --> b) ==> (", " --> c))"));
            Term a = x.subPath((byte) 0, (byte) 0);
            Term b = x.subPath((byte) 1, (byte) 0);
            assertNotEquals(a, x.subPath((byte) 0, (byte) 1));
            assertEquals(a, b, () -> x + " subterms (0,0)==(1,0)");
            assertSame(a, b);
        }
    }

    @Test
    void testConjNorm() throws Narsese.NarseseException {
        String a = "(&&,(#1-->key),(#2-->lock),open(#1,#2))";
        String b = "(&&,(#2-->key),(#1-->lock),open(#2,#1))";

        assertEquals($.$(a), $.$(b));

    }
}
