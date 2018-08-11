/*
 * Copyright (C) 2014 me
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:
 */
package nars.term;

import nars.*;
import nars.concept.Concept;
import nars.subterm.ArrayTermVector;
import nars.subterm.Subterms;
import nars.subterm.UniSubterm;
import nars.task.util.TaskException;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.util.TermException;
import nars.unify.match.EllipsisMatch;
import nars.util.TimeAware;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.Long.toBinaryString;
import static nars.$.$;
import static nars.$.$$;
import static nars.Op.*;
import static nars.task.RevisionTest.x;
import static nars.time.Tense.DTERNAL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author me
 */
public class TermTest {

    private final NAR n = NARS.shell();

    public static void assertReallyEquals(Term c, Term f) {
        assertTrue(f != c, "identical, nothing is being tested");

        assertEquals(c.op(), f.op());
        assertEquals(c.subs(), f.subs());
        int s = f.subterms().subs();
        assertEquals(c.subterms().subs(), s);

        assertEquals(c.hashCode(), f.hashCode());

        for (int i = 0; i < s; i++) {
            Term ci = c.subterms().sub(i);
            Term fi = f.subterms().sub(i);
            assertEquals(ci, fi);
            assertEquals(fi, ci);
            assertEquals(fi.subterms(), ci.subterms());
            assertEquals(ci.subterms(), fi.subterms());
            assertEquals(fi.hashCode(), ci.hashCode());
            assertEquals(-fi.compareTo(ci),
                    ci.compareTo(fi));
        }

        assertArrayEquals(c.subterms().arrayShared(), f.subterms().arrayShared());
        assertEquals(c.subterms().hashCodeSubterms(), f.subterms().hashCodeSubterms());
        assertEquals(c.subterms().hashCode(), f.subterms().hashCode());

        assertEquals(c.structure(), f.structure());
        assertEquals(c.complexity(), f.complexity());
        assertEquals(c.volume(), f.volume());
        assertEquals(c.toString(), f.toString());
        assertEquals(c, c);
        assertEquals(f, f);
        assertEquals(c, f);
        assertEquals(f, c);
        assertEquals(0, f.compareTo(c));
        assertEquals(0, c.compareTo(f));
    }


    public static void assertEquivalentTerm(@NotNull String term1String, @NotNull String term2String) {

        try {


            Termed term1 = $.$(term1String);
            Termed term2 = $.$(term2String);


            assertEquivalentTerm(term1.term(), term2.term());

        } catch (Exception e) {
            fail(e.toString());
        }
    }

    public static void assertEquivalentTerm(Term term1, Term term2) {

        assertEquals(term1, term2);
        assertEquals(term2, term1);
        assertEquals(term1.hashCode(), term2.hashCode());
        assertEquals(term1.dt(), term2.dt());
        assertEquals(0, term1.compareTo(term2));
        assertEquals(0, term1.compareTo(term1));
        assertEquals(0, term2.compareTo(term1));
        assertEquals(0, term2.compareTo(term2));
    }

    public static Term assertStable(String is) {
        return assertEq(is,is);
    }

    public static Term assertEq(String exp, String is) {
        Term t = $$(is);
        assertEquals(exp, t.toString(), () -> is + " reduces to " + exp);


        //test for stability:
        Term u = $$(t.toString());
        assertEquals(u, t, ()-> is + " unstable:\n0:\t" + t + "\n1:\t" + u);

        return t;
    }

    public static void assertEq(Term exp, String is) {
        assertEquals(exp, $$(is), () -> exp + " reduces to " + is);
    }

    public static void assertEq(String exp, Term is) {
        assertEquals(exp, is.toString(), () -> exp + " reduces to " + is);
    }

    @Test
    void testInstantiateBoolsFromEquivString() {
        for (Term b : new Term[]{True, False, Null})
            assertSame(b, $.the(b.toString()));
    }

    @Test
    void testIntifyVarCountOfSubtermsContainingVars() throws Narsese.NarseseException {
        assertEquals(2, $("(add(s(s(0)),s(s(0)),?R)==>goal(?R))").varQuery());
    }


    @Test
    void testSetCommutivity() throws Exception {

        assertEquals("{a,b}", $("{b,a}").toString());
        assertEquals("{a,b}", $("{a,b}").toString());
        assertEquals("{a,b}", SETe.the($.the("a"), $.the("b")).toString());
        assertEquals("{a,b}", SETe.the($.the("b"), $.the("a")).toString());

        assertEquivalentTerm("{b,a}", "{b,a}");
        assertEquivalentTerm("{a,b}", "{b,a}");


        assertEquivalentTerm("{b,a,c}", "{b,a,c}");
        assertEquivalentTerm("{b,a,c}", "{a,c,b}");
        assertEquivalentTerm("{b,a,c}", "{b,c,a}");

        assertEquivalentTerm("[a,c,b]", "[b,a,c]");
    }

    @Test
    void testSectCommutivity() throws Exception {
        assertEquivalentTerm("(&,a,b)", "(&,b,a)");
        assertEquivalentTerm("(|,a,b)", "(|,b,a)");
    }

    @Test
    void testSimCommutivity() throws Exception {
        assertEquivalentTerm("<{Birdie}<->{Tweety}>", "<{Tweety}<->{Birdie}>");
        assertEquivalentTerm($("<{Birdie}<->{Tweety}>"),
                $("<{Tweety}<->{Birdie}>"));

        assertEquivalentTerm(
                $.sim($("{Birdie}"), $("{Tweety}")),
                $.sim($("{Tweety}"), $("{Birdie}"))
        );


    }

    @Test
    void testCommutativivity() {
        Atomic W = Atomic.the("w");
        Atomic X = Atomic.the("x");
        Atomic Y = Atomic.the("y");
        Atomic Z = Atomic.the("z");
        Stream.of(Op.ops).filter(x -> x.commutative).forEach(o -> {
            assertFalse(o.the(X).isCommutative());

            Term xy = o.the(X, Y);
            assertTrue(xy.isCommutative());
            assertEquals(xy, o.the(Y, X), () -> "commutivity failed for " + xy);
            assertEquals(xy, o.the(List.of(Y, X)), () -> "commutivity failed for " + xy);
            assertEquals(xy, o.the(Set.of(Y, X)), () -> "commutivity failed for " + xy);

            if (o.maxSize > 2) {
                Term xyz = o.the(X, Y, Z);
                assertTrue(xy.isCommutative());
                assertEquals(xyz, o.the(Y, X, Z), () -> "commutivity failed for " + xyz);
                assertEquals(xyz, o.the(List.of(Y, X, Z)), () -> "commutivity failed for " + xyz);
                assertEquals(xyz, o.the(Set.of(Y, X, Z)), () -> "commutivity failed for " + xyz);
                assertEquals(xyz, o.the(X, Z, Y), () -> "commutivity failed for " + xyz);
                assertEquals(xyz, o.the(X, Y, Z), () -> "commutivity failed for " + xyz);
                assertEquals(xyz, o.the(Z, Y, X), () -> "commutivity failed for " + xyz);
                assertEquals(xyz, o.the(Z, X, Y), () -> "commutivity failed for " + xyz);
                assertEquals(xyz, o.the(Y, Z, X), () -> "commutivity failed for " + xyz);

                Term wxyz = o.the(W, X, Y, Z);
                assertEquals(wxyz, o.the(Y, W, X, Z), () -> "commutivity failed for " + wxyz);
                assertEquals(wxyz, o.the(List.of(Y, W, X, Z)), () -> "commutivity failed for " + wxyz);
                assertEquals(wxyz, o.the(Set.of(Y, W, X, Z)), () -> "commutivity failed for " + wxyz);
                //TODO other permutes
            }
        });
    }


    @Test
    void testTermSort() throws Exception {


        Term a = $.$("a").term();
        Term b = $.$("b").term();
        Term c = $.$("c").term();

        assertEquals(3, Terms.sorted(a, b, c).length);
        assertEquals(2, Terms.sorted(a, b, b).length);
        assertEquals(1, Terms.sorted(a, a).length);
        assertEquals(1, Terms.sorted(a).length);
        assertEquals(a, Terms.sorted(a, b)[0], "correct natural ordering");
        assertEquals(a, Terms.sorted(a, b, c)[0], "correct natural ordering");
        assertEquals(b, Terms.sorted(a, b, c)[1], "correct natural ordering");
        assertEquals(c, Terms.sorted(a, b, c)[2], "correct natural ordering");
        assertEquals(b, Terms.sorted(a, a, b, b, c, c)[1], "correct natural ordering");

    }


    @Test
    void testConjunctionTreeSet() throws Narsese.NarseseException {


        String term1String = "<#1 --> (&,boy,(/,taller_than,{Tom},_))>";
        Term term1 = $.$(term1String).term();
        String term1Alternate = "<#1 --> (&,(/,taller_than,{Tom},_),boy)>";
        Term term1a = $.$(term1Alternate).term();


        Term term2 = $.$("<#1 --> (|,boy,(/,taller_than,{Tom},_))>").term();

        assertEquals(term1a.toString(), term1.toString());
        assertTrue(term1.complexity() > 1);
        assertEquals(term1.complexity(), term2.complexity());

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
        assertTrue(added1, "term 1 added to set");
        assertTrue(added2, "term 2 added to set");

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
     * test consistency between subterm conceptualization and term conceptualization
     */
    @Test
    void testRootOfImplWithConj() throws Narsese.NarseseException {
        String ys = "((--,tetris(isRow,13,true))&&tetris(isRowClear,6,true))";

        Term y = $.$(ys);
        String yc;
        assertEquals("((--,tetris(isRow,13,true)) &&+- tetris(isRowClear,6,true))", yc = y.concept().toString());

        assertEquals(yc, y.root().toString());

        Term x = $.$("(tetris(isRowClear,10,true) ==>+- " + ys + ")");
        assertEquals("(tetris(isRowClear,10,true) ==>+- " + yc + ")", x.concept().toString());

    }


    @Test
    void invalidTermIndep() {

        String t = "($1-->({place4}~$1))";


        try {
            Task x = n.inputTask(t + '.');
            fail(t + " is invalid compound term");
        } catch (Throwable tt) {
            assertTrue(true);
        }

        Term subj = null, pred = null;
        try {
            subj = $.varIndep(1);
            pred = $.$("(~,{place4},$1)").term();

            assertTrue(true);

        } catch (Throwable ex) {
            ex.printStackTrace();
            fail(ex);
        }


    }

    @Test
    void testValidIndep() {
        assertTrue(Task.taskConceptTerm(
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


    @Test
    void testFromEllipsisMatch() {
        Term xy = EllipsisMatch.matched($.the("x"), $.the("y"));

        for (Op o : new Op[]{Op.SECTi, SECTe, DIFFe, DIFFi, CONJ}) {
            Term z = o.the(DTERNAL, xy);
            assertEquals("(x" + o.str + "y)", z.toString());
            assertEquals(3, z.volume());
            assertEquals(Op.ATOM, z.sub(0).op());
            assertEquals(Op.ATOM, z.sub(1).op());
        }
    }


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

    private void testTermEqualityNonNormalizing(@NotNull String s) {
        try {
            testTermEquality(s, false);
        } catch (Narsese.NarseseException e) {
            fail(e);
        }
    }

    private void testTermEquality(@NotNull String s) {
        try {
            testTermEquality(s, true);
        } catch (Narsese.NarseseException e) {
            fail(e);
        }
    }


    private void testTermEquality(@NotNull String s, boolean conceptualize) throws Narsese.NarseseException {


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
            assertNotNull(n2a, a + " should conceptualize");
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

        TimeAware n2 = NARS.shell();
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

    private void statementHash(@NotNull String a, @NotNull String b) throws Narsese.NarseseException {


        Term ta = $(a);
        Term tb = $(b);

        assertNotEquals(ta, tb);
        assertNotEquals(ta.hashCode(),
                tb.hashCode(), ta + " vs. " + tb);


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
        assertTrue(Math.abs(ah - bh) > 1, ah + " vs " + bh);
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

    private void testTermComplexityMass(@NotNull TimeAware n, @NotNull String x, int complexity, int mass) throws Narsese.NarseseException {
        testTermComplexityMass(n, x, complexity, mass, 0, 0, 0);
    }

    private void testTermComplexityMass(@NotNull TimeAware n, @NotNull String x, int complexity, int mass, int varIndep, int varDep, int varQuery) throws Narsese.NarseseException {
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

    @NotNull <C extends Compound> C testStructure(@NotNull String term, String bits) throws Narsese.NarseseException {

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
        assertTrue(!(o instanceof Bool));
        return o;
    }

    static void assertValidTermValidConceptInvalidTaskContent(@NotNull Supplier<Term> o) {
        try {
            Term x = o.get();
            assertNotNull(x);

            NAR t = NARS.shell();
            t.believe(x);

            fail(x + " should not have been allowed as a task content");


        } catch (Exception e) {

        }
    }


    static void assertValidTermValidConceptInvalidTaskContent(String o) {
        assertThrows(TaskException.class, ()-> { NARS.shell().input(o); });
    }


    @Test
    void testSubTermStructure() throws Narsese.NarseseException {
        assertTrue(x.term().impossibleSubTerm(x.term()));
        assertTrue(!x.hasAll($.$("(a-->#b)").term().structure()));
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

    private void testUniqueHash(@NotNull String a, @NotNull String b) throws Narsese.NarseseException {

        TimeAware t = NARS.shell();
        int h1 = $.$(a).hashCode();
        int h2 = $.$(b).hashCode();
        assertNotEquals(h1, h2);
    }

    @Test
    void testSetOpFlags() throws Narsese.NarseseException {
        assertTrue($("{x}").op().isSet());
        assertTrue($("[y]}").op().isSet());
        assertFalse($("x").op().isSet());
        assertFalse($("a:b").op().isSet());
    }

    @Test
    void testEmptyProductEquality() throws Narsese.NarseseException {
        assertEquals($("()"), $("()"));
        assertEquals(EmptyProduct, $("()"));
    }


    static void assertInvalid(@NotNull String o) {
        assertThrows(TermException.class, () -> {
            $(o);
        });
    }

    @Test
    void reuseVariableTermsDuringNormalization2() throws Narsese.NarseseException {
        for (String v : new String[]{"?a", "?b", "#a", "#c"}) {
            Compound x = $("((" + v + " --> b) ==> (" + v + " --> c))");
            Term a = x.subPath((byte) 0, (byte) 0);
            Term b = x.subPath((byte) 1, (byte) 0);
            assertNotEquals(a, x.subPath((byte) 0, (byte) 1));
            assertEquals(a, b, x + " subterms (0,0)==(1,0)");
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
