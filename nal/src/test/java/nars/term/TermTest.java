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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.term;

import nars.*;
import nars.concept.Concept;
import nars.derive.match.EllipsisMatch;
import nars.subterm.ArrayTermVector;
import nars.subterm.Subterms;
import nars.subterm.UnitSubterm;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.TreeSet;
import java.util.function.Supplier;

import static java.lang.Long.toBinaryString;
import static nars.$.$;
import static nars.Op.*;
import static nars.task.RevisionTest.x;
import static nars.time.Tense.DTERNAL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author me
 */
public class TermTest {

    final NAR n = NARS.shell();

    public static void assertReallyEquals(Term c, Term f) {
        assertTrue(f!=c,
                ()->"identical, nothing is being tested");

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
                         ci.compareTo(fi) );
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


    protected void assertEquivalentTerm(@NotNull String term1String, @NotNull String term2String) {

        try {


            Termed term1 = $.$(term1String);
            Termed term2 = $.$(term2String);

            //assertNotEquals(term1String, term2String);

            assertEquivalentTerm(term1.term(), term2.term());

        } catch (Exception e) {
            fail(e.toString());
        }
    }

    public void assertEquivalentTerm(Term term1, Term term2) {

        assertEquals(term1, term2);
        assertEquals(term2, term1);
        assertEquals(term1.hashCode(), term2.hashCode());
        assertEquals(term1.dt(), term2.dt());
        assertEquals(0, term1.compareTo(term2));
        assertEquals(0, term1.compareTo(term1));
        assertEquals(0, term2.compareTo(term1));
        assertEquals(0, term2.compareTo(term2));
    }

    @Test public void testInstantiateBoolsFromEquivString() {
        for (Term b : new Term[] { True, False, Null })
            assertSame(b, $.the(b.toString()));
    }

    @Test public void testIntifyVarCountOfSubtermsContainingVars() throws Narsese.NarseseException {
        assertEquals(2, $("(add(s(s(0)),s(s(0)),?R)==>goal(?R))").varQuery());
    }

    @Test
    public void testConjCommutivity() {

        assertEquivalentTerm("(&&,a,b)", "(&&,b,a)");
        assertEquivalentTerm("(&&,(||,(b),(c)),(a))", "(&&,(a),(||,(b),(c)))");
        assertEquivalentTerm("(&&,(||,(c),(b)),(a))", "(&&,(a),(||,(b),(c)))");
        assertEquivalentTerm("(&&,(||,(c),(b)),(a))", "(&&,(a),(||,(c),(b)))");
    }

    @Test
    public void testSetCommutivity() throws Exception {

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
    public void testOtherCommutivity() throws Exception {
        assertEquivalentTerm("(&,a,b)", "(&,b,a)");
        assertEquivalentTerm("(|,a,b)", "(|,b,a)");

        assertEquivalentTerm("<{Birdie}<->{Tweety}>", "<{Tweety}<->{Birdie}>");
        assertEquivalentTerm($("<{Birdie}<->{Tweety}>"),
                        $("<{Tweety}<->{Birdie}>"));
        assertEquivalentTerm(
                $.sim($("{Birdie}"),$("{Tweety}")),
                $.sim($("{Tweety}"),$("{Birdie}"))
        );

//        //test ordering after derivation
//        assertEquals("<{Birdie}<->{Tweety}>",
//            (((Compound)$("<{Birdie}<->{Tweety}>")).term(
//                new Term[] { $("{Tweety}"), $("{Birdie}") }).toString())
//        );
    }

    @Test
    public void testCommutativivity()  {
        assertFalse(SETe.the(Atomic.the("x")).isCommutative());
        assertTrue(SETe.the(Atomic.the("x"), Atomic.the("y")).isCommutative());
    }



    @Test
    public void testTermSort() throws Exception {


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

//    @Test public void testDisjunctionEllipsis() throws Narsese.NarseseException {
//         assertEquals("(||,%A..+)", $.$("(||,%A..+)"));
//    }
    @Test
    public void testConjunction1Term() throws Narsese.NarseseException {

        assertEquals("a", $.$("(&&,a)").toString());
        assertEquals("x(a)", $.$("(&&,x(a))").toString());
        assertEquals("a", $.$("(&&,a, a)").toString());

        assertEquals("((before-->x) &&+10 (after-->x))",
                $.$("(x:after &&-10 x:before)").toString());
        assertEquals("((before-->x) &&+10 (after-->x))",
                $.$("(x:before &&+10 x:after)").toString());

        //assertEquals("a", n.term("(&&+0,a)").toString());
        //assertEquals("a", n.term("(&&+3,a)").toString());
    }

    @Test
    public void testConjunctionTreeSet() throws Narsese.NarseseException {

        //these 2 representations are equal, after natural ordering
        String term1String = "<#1 --> (&,boy,(/,taller_than,{Tom},_))>";
        Term term1 = $.$(term1String).term();
        String term1Alternate = "<#1 --> (&,(/,taller_than,{Tom},_),boy)>";
        Term term1a = $.$(term1Alternate).term();


        // <#1 --> (|,boy,(/,taller_than,{Tom},_))>
        Term term2 = $.$("<#1 --> (|,boy,(/,taller_than,{Tom},_))>").term();

        assertEquals(term1a.toString(), term1.toString());
        assertTrue(term1.complexity() > 1);
        assertEquals(term1.complexity(), term2.complexity());

        assertSame(term1.op(), INH);


        //System.out.println("t1: " + term1 + ", complexity=" + term1.getComplexity());
        //System.out.println("t2: " + term2 + ", complexity=" + term2.getComplexity());


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
    public void testUnconceptualizedTermInstancing() throws Narsese.NarseseException {

        String term1String = "<a --> b>";
        Term term1 = $.$(term1String).term();
        Term term2 = $.$(term1String).term();

        assertEquals(term1, term2);
        assertEquals(term1.hashCode(), term2.hashCode());

        Compound cterm1 = ((Compound) term1);
        Compound cterm2 = ((Compound) term2);

        //test subterms
        assertEquals(cterm1.sub(0), cterm2.sub(0)); //'a'

    }



    /** test consistency between subterm conceptualization and term conceptualization */
    @Test public void testRootOfImplWithConj() throws Narsese.NarseseException {
        String ys = "((--,tetris(isRow,13,true))&&tetris(isRowClear,6,true))";

        Term y = $.$(ys);
        String yc;
        assertEquals("((--,tetris(isRow,13,true))&&tetris(isRowClear,6,true))", yc = y.concept().toString());

        assertEquals(yc, y.root().toString());

        Term x = $.$("(tetris(isRowClear,10,true)==>" + ys + ")");
        assertEquals("(tetris(isRowClear,10,true)==>" + yc + ")", x.concept().toString());

    }

//    @Test
//    public void testEscaping() {
//        bidiEscape("c d", "x$# x", "\\\"sdkf sdfjk", "_ _");
//
////        NAR n = NARS.shell().builder();
////        n.addInput("<a --> \"b c\">.");
////        n.step(1);
////        n.finish(1);
////
////        Term t = new Term("\\\"b_c\\\"");
////        System.out.println(t);
////        System.out.println(n.memory.getConcepts());
////        System.out.println(n.memory.conceptProcessor.getConcepts());
////
////
////        assertTrue(n.memory.concept(new Term("a"))!=null);
////        assertTrue(n.memory.concept(t)!=null);
//
//    }

//    protected void bidiEscape(String... tests) {
//        for (String s : tests) {
//            s = '"' + s + '"';
//            String escaped = Texts.escape(s).toString();
//            String unescaped = Texts.unescape(escaped).toString();
//            //System.out.println(s + " " + escaped + " " + unescaped);
//            assertEquals(s, unescaped);
//        }
//    }

    @Test
    public void invalidTermIndep() {

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




//        } catch (Throwable ex) {
//            assertTrue(ex.toString(), false);
//        }
    }


    @Deprecated static boolean isOperation(@NotNull Termed _t) {
        Term t = _t.term();
        if (t.op() == Op.INH) { //Op.hasAll(t.structure(), Op.OperationBits) &&
            Compound c = (Compound) t;
            return c.subIs(1, Op.ATOM) &&
                    c.subIs(0, Op.PROD);
        }
        return false;
    }

    @Test
    public void testParseOperationInFunctionalForm() throws Narsese.NarseseException {

//        assertFalse(Op.isOperation(n.term("(a,b)")));
//        assertFalse(Op.isOperation(n.term("^wonder")));

            Term x = $.$("wonder(a,b)").term();
            assertEquals(INH, x.op());
            assertTrue(isOperation(x));
            assertEquals("wonder(a,b)", x.toString());



    }


    @Test public void testFromEllipsisMatch() {
        Term xy = EllipsisMatch.match($.the("x"), $.the("y"));

        for (Op o : new Op[] { Op.SECTi, SECTe, DIFFe, DIFFi, CONJ }) {
            Term z = o.the(DTERNAL, xy);
            assertEquals("(x" + o.str + "y)", z.toString());
            assertEquals(3, z.volume());
            assertEquals(Op.ATOM, z.sub(0).op());
            assertEquals(Op.ATOM, z.sub(1).op());
        }
    }

//    public void nullCachedName(String term) {
//        NAR n = NARS.shell();
//        n.input(term + ".");
//        n.run(1);
//        assertNull("term name string was internally generated although it need not have been", ((Compound) n.concept(term).getTerm()).nameCached());
//    }
//
//    @Test public void avoidsNameConstructionUnlessOutputInheritance() {
//        nullCachedName("<a --> b>");
//    }
//
//    @Test public void avoidsNameConstructionUnlessOutputNegationAtomic() {
//        nullCachedName("(--, a)");
//    }
//    @Test public void avoidsNameConstructionUnlessOutputNegationCompound() {
//        nullCachedName("(--, <a-->b> )");
//    }
//
//    @Test public void avoidsNameConstructionUnlessOutputSetInt1() {
//        nullCachedName("[x]");
//    }
//    @Test public void avoidsNameConstructionUnlessOutputSetExt1() {
//        nullCachedName("{x}");
//    }

    @Test public void testPatternVar() throws Narsese.NarseseException {
        assertSame($("%x").op(), Op.VAR_PATTERN);
    }

    @Test
    public void termEqualityWithQueryVariables() throws Narsese.NarseseException {

        String a = "<?1-->bird>";
        assertEquals($.$(a), $.$(a));
        String b = "<bird-->?1>";
        assertEquals($.$(b), $.$(b));
    }

    protected void testTermEqualityNonNormalizing(@NotNull String s) {
        try {
            testTermEquality(s, false);
        } catch (Narsese.NarseseException e) {
            fail(e);
        }
    }
    protected void testTermEquality(@NotNull String s)  {
        try {
            testTermEquality(s, true);
        } catch (Narsese.NarseseException e) {
            fail(e);
        }
    }


    protected void testTermEquality(@NotNull String s, boolean conceptualize) throws Narsese.NarseseException {


        Term a = $.$(s).term();

        NAR n2 = NARS.shell();
        Term b = $.$(s).term();

        //assertTrue(a != b);

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
            assertNotNull( n2a, a + " should conceptualize");
            assertNotNull(b);
            assertEquals(n2a.toString(), b.toString());
            assertEquals(n2a.hashCode(), b.hashCode());
            assertEquals(n2a.term(), b);
        }

    }

    @Test
    public void termEqualityOfVariables1() {
        testTermEqualityNonNormalizing("#1");
    }

    @Test
    public void termEqualityOfVariables2() {
        testTermEqualityNonNormalizing("$1");
    }

    @Test
    public void termEqualityOfVariables3() {
        testTermEqualityNonNormalizing("?1");
    }

    @Test
    public void termEqualityOfVariables4() {
        testTermEqualityNonNormalizing("%1");
    }


    @Test
    public void termEqualityWithVariables1() {
        testTermEqualityNonNormalizing("<#2 --> lock>");
    }

    @Test
    public void termEqualityWithVariables2() {
        testTermEquality("<<#2 --> lock> --> x>");
    }

    @Test
    public void termEqualityWithVariables3() throws Narsese.NarseseException {
        testTermEquality("(&&, x, <#2 --> lock>)", false);
        testTermEquality("(&&, x, <#1 --> lock>)", false);
    }

    @Test
    public void termEqualityWithVariables4() throws Narsese.NarseseException {
        testTermEquality("(&&, <<$1 --> key> ==> <#2 --> ( open, $1 )>>, <#2 --> lock>)", false);
    }

    @Test
    public void termEqualityWithMixedVariables() throws Narsese.NarseseException {

        String s = "(&&, <<$1 --> key> ==> <#2 --> ( open, $1 )>>, <#2 --> lock>)";
        Termed a = $.$(s);

        NAR n2 = NARS.shell();
        Termed b = $.$(s);

        //assertTrue(a != b);
        assertEquals(a, b);

        //todo: method results ignored ?

//        assertEquals("re-normalizing doesn't affect: " + n2.concept(a), b,
//                n2.concept(a));

    }


    @Test
    public void statementHash() throws Narsese.NarseseException {
        //this is a case where a faulty hash function produced a collision
        statementHash("i4", "i2");
        statementHash("{i4}", "{i2}");
        statementHash("<{i4} --> r>", "<{i2} --> r>");


        statementHash("<<{i4} --> r> ==> A(7)>", "<<{i2} --> r> ==> A(8)>");

        statementHash("<<{i4} --> r> ==> A(7)>", "<<{i2} --> r> ==> A(7)>");

    }

    @Test
    public void statementHash2() throws Narsese.NarseseException {
        statementHash("<<{i4} --> r> ==> A(7)>", "<<{i2} --> r> ==> A(9)>");
    }

    @Test
    public void statementHash3() throws Narsese.NarseseException {

        //this is a case where a faulty hash function produced a collision
        statementHash("<<{i0} --> r> ==> A(8)>", "<<{i1} --> r> ==> A(7)>");

        //this is a case where a faulty hash function produced a collision
        statementHash("<<{i10} --> r> ==> A(1)>", "<<{i11} --> r> ==> A(0)>");
    }

    public void statementHash(@NotNull String a, @NotNull String b) throws Narsese.NarseseException {


        Term ta = $(a);
        Term tb = $(b);

        assertNotEquals(ta, tb);
        assertNotEquals(ta.hashCode(),
                tb.hashCode(), ta + " vs. " + tb);


    }

    @Test public void testHashConsistent() {
        Term x = $.the("z");
        Subterms a = new UnitSubterm(x);
        Subterms b = new ArrayTermVector(x);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(a.hashCodeSubterms(), b.hashCodeSubterms());
        assertEquals(a.toString(), b.toString());
    }

    @Test public void testHashDistribution() {
        int ah = new UnitSubterm($.the("x")).hashCode(); //one letter apart
        int bh = new UnitSubterm($.the("y")).hashCode();
        assertTrue(Math.abs(ah-bh) > 1, ah + " vs " + bh);
    }

    @Test
    public void testTermComplexityMass() throws Narsese.NarseseException {


        testTermComplexityMass(n, "x", 1, 1);

        testTermComplexityMass(n, "#x", 0, 1, 0, 1, 0);
        testTermComplexityMass(n, "$x", 0, 1, 1, 0, 0);
        testTermComplexityMass(n, "?x", 0, 1, 0, 0, 1);

        testTermComplexityMass(n, "<a --> b>", 3, 3);
        testTermComplexityMass(n, "<#a --> b>", 2, 3, 0, 1, 0);

        testTermComplexityMass(n, "<a --> (c & d)>", 5, 5);
        testTermComplexityMass(n, "<$a --> (c & #d)>", 3, 5, 1, 1, 0);
    }

    private void testTermComplexityMass(@NotNull NAR n, @NotNull String x, int complexity, int mass) throws Narsese.NarseseException {
        testTermComplexityMass(n, x, complexity, mass, 0, 0, 0);
    }

    private void testTermComplexityMass(@NotNull NAR n, @NotNull String x, int complexity, int mass, int varIndep, int varDep, int varQuery) throws Narsese.NarseseException {
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
    public <C extends Compound> C testStructure(@NotNull String term, String bits) throws Narsese.NarseseException {

        C a = (C) $.$(term).term();
        assertEquals(bits, toBinaryString(a.structure()));
        assertEquals(term, a.toString());
        return a;
    }

//    @Disabled
//    @Test
//    public void testSubtermsVector() {
//
//        NAR n = NARS.shell();
//
//        Term a3 = n.term("c");
//
//        Compound a = testStructure("<c </> <a --> b>>", "1000000000000000000001000001");
//        Compound a0 = testStructure("<<a --> b> </> c>", "1000000000000000000001000001");
//
//        Compound a1 = testStructure("<c <|> <a --> b>>", "10000000000000000000001000001");
//        Compound a2 = testStructure("<c <=> <a --> b>>", "100000000000000000001000001");
//
//        Compound b = testStructure("<?1 </> <$2 --> #3>>", "1000000000000000000001001110");
//        Compound b2 = testStructure("<<$1 --> #2> </> ?3>", "1000000000000000000001001110");
//
//
//        assertTrue(a.impossibleStructureMatch(b.structure()));
//        assertFalse(a.impossibleStructureMatch(a3.structure()));
//
//
//        assertEquals("no additional structure code in upper bits",
//                a.structure(), a.structure());
//        assertEquals("no additional structure code in upper bits",
//                b.structure(), b.structure());
//
//
//    }



    public static void assertValid(Term o) {
        assertNotNull(o);
        assertTrue(!(o instanceof Bool));
    }

    public static void assertValidTermValidConceptInvalidTaskContent(@NotNull Supplier<Term> o) {
        try {
            Term x = o.get();
            assertNotNull(x);

            NAR t = NARS.shell();
            t.believe(x);

            fail(x + " should not have been allowed as a task content");


        } catch (Exception e) {
            //correct if happens here
        }
    }



    static void assertValidTermValidConceptInvalidTaskContent(String o) {
        try {

            NARS.shell().believe(o);

            fail(o + " should not have been allowed as a task content");


        } catch (Exception e) {
            //correct if happens here
        }
    }

//    @Test
//    public void testImageInhConstruction() {
//        Compound p = $.p("a", "b", "c");
//        assertEquals("(a-->(/,_,b,c))", $.imge(0, p).toString());
//        assertEquals("(a-->(/,_,b,c))", $.image(0, p.toArray()).toString());
//        assertEquals("(b-->(/,a,_,c))", $.imge(1, p).toString());
//        assertEquals("(c-->(/,a,b,_))", $.imge(2, p).toString());
//
//        assertEquals("((\\,_,b,c)-->a)", $.imgi(0, p).toString());
//        assertEquals("((\\,_,b,c)-->a)", $.imgi(0, p.toArray()).toString());
//        assertEquals("((\\,a,_,c)-->b)", $.imgi(1, p).toString());
//        assertEquals("((\\,a,b,_)-->c)", $.imgi(2, p).toString());
//
//    }



//    @Test
//    public void testImageConstructionExt() throws Narsese.NarseseException {
//
//
//
//
//        assertEquals(
//            "(A-->(/,%1,_))", $("<A --> (/, %1, _)>").toString()
//        );
//        assertEquals(
//            "(A-->(/,_,%1))", $("<A --> (/, _, %1)>").toString()
//        );
////        assertEquals(
////                "(/,_,%X)", $("(/, _, %X)").toString()
////        );
//
//        assertEquals(
//                imageExt($("X"), $("_"), $("Y")), $("(/, X, _, Y)")
//        );
//        assertEquals(
//                imageExt($("_"), $("X"), $("Y")), $("(/, _, X, Y)")
//        );
//        assertEquals(
//                imageExt($("X"), $("Y"), $("_")), $("(/, X, Y, _)")
//        );
//    }
//    @Test
//    public void testImageConstructionInt() throws Narsese.NarseseException {
//        assertEquals(
//                imageInt($("X"), $("_"), $("Y")), $("(\\, X, _, Y)")
//        );
//        assertEquals(
//                imageInt($("_"), $("X"), $("Y")), $("(\\, _, X, Y)")
//        );
//        assertEquals(
//                imageInt($("X"), $("Y"), $("_")), $("(\\, X, Y, _)")
//        );
//    }

//    @Test
//    public void testImageOrdering1() throws Narsese.NarseseException {
//        testImageOrdering('/');
//    }
//
//    @Test
//    public void testImageOrdering2() throws Narsese.NarseseException {
//        testImageOrdering('\\');
//    }
//
//    void testImageOrdering(char v) throws Narsese.NarseseException {
//        NAR n = new Terminal(16);
//
//        Termed<Compound> aa = n.term("(" + v + ",x, y, _)");
//        Compound a = aa.term();
//        Termed<Compound> bb = n.term("(" + v + ",x, _, y)");
//        Compound b = bb.term();
//        Termed<Compound> cc = n.term("(" + v + ",_, x, y)");
//        Compound c = cc.term();
//        assertNotEquals(a.dt(), b.dt());
//        assertNotEquals(b.dt(), c.dt());
//
//        assertNotEquals(a, b);
//        assertNotEquals(b, c);
//        assertNotEquals(a, c);
//
//        assertNotEquals(a.hashCode(), b.hashCode());
//        assertNotEquals(b.hashCode(), c.hashCode());
//        assertNotEquals(a.hashCode(), c.hashCode());
//
//        assertEquals(+1, a.compareTo(b));
//        assertEquals(-1, b.compareTo(a));
//
//        assertEquals(+1, a.compareTo(c));
//        assertEquals(-1, c.compareTo(a));
//
//        assertNotEquals(0, b.compareTo(c));
//        assertEquals(-c.compareTo(b), b.compareTo(c));
//
//
//    }
//
//    @Test
//    public void testImageStructuralVector() throws Narsese.NarseseException {
//
//        String i1 = "(/,x,y,_)";
//        String i2 = "(/,x,_,y)";
//        Compound a = testStructure(i1, "1000000000001");
//        Compound b = testStructure(i2, "1000000000001");
//
//        /*assertNotEquals("additional structure code in upper bits",
//                a.structure2(), b.structure2());*/
//        assertNotEquals(a.dt(), b.dt());
//        assertNotEquals("structure code influenced contentHash",
//                b.hashCode(), a.hashCode());
//
//        NAR n = new Terminal(8);
//        Termed<Compound> x3 = n.term('<' + i1 + " --> z>");
//        Termed<Compound> x4 = n.term('<' + i1 + " --> z>");
//
//        assertFalse("i2 is a possible subterm of x3, structurally, even if the upper bits differ",
//                x3.term().impossibleSubTermOrEquality(n.term(i2).term()));
//        assertFalse(
//                x4.term().impossibleSubTermOrEquality(n.term(i1).term()));
//
//
//    }


    @Test
    public void testSubTermStructure() throws Narsese.NarseseException {
        assertTrue( x.term().impossibleSubTerm( x.term() ) );
        assertTrue( !x.hasAll($.$("<a-->#b>").term().structure()) );
    }

    @Test
    public void testCommutativeWithVariableEquality() throws Narsese.NarseseException {

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
    public void testHash1() throws Narsese.NarseseException {
        testUniqueHash("<A --> B>", "<A <-> B>");
        testUniqueHash("<A --> B>", "<A ==> B>");
        testUniqueHash("A", "B");
        testUniqueHash("%1", "%2");
        testUniqueHash("%A", "A");
        testUniqueHash("$1", "A");
        testUniqueHash("$1", "#1");
    }

    public void testUniqueHash(@NotNull String a, @NotNull String b) throws Narsese.NarseseException {

        NAR t = NARS.shell();
        int h1 = $.$(a).hashCode();
        int h2 = $.$(b).hashCode();
        assertNotEquals(h1, h2);
    }

    @Test public void testSetOpFlags() throws Narsese.NarseseException {
        assertTrue( $("{x}").op().isSet() );
        assertTrue( $("[y]}").op().isSet() );
        assertFalse( $("x").op().isSet() );
        assertFalse( $("a:b").op().isSet() );
    }

    @Test public void testEmptyProductEquality() throws Narsese.NarseseException {
        assertEquals( $("()"),$("()") );
        assertEquals(ZeroProduct, $("()"));
    }


    public static void assertInvalid(@NotNull String o) {
        assertInvalid(() -> {
                try {
                    Term x = $(o);
                    return x;
                } catch (Narsese.NarseseException e) {
                    return Null;
                }
        });
    }
    public static void assertInvalid(@NotNull Supplier<Term> o) {
        try {
            Term recv = o.get();
            if (recv!=Null) //False also signals invalid reduction
                fail(recv + " was not null");
        } catch (Term.InvalidTermException e) {
            //correct if happens here
        }
    }

}
