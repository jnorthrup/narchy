package nars.io;

import nars.*;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.Present;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Proposed syntax extensions, not implemented yet
 */
class NarseseExtendedTest extends NarseseTest {


    @Test
    void testRuleComonent0() throws Narsese.NarseseException {
        assertNotNull($.$("((P ==> S), (S ==> P))"));
        assertNotNull($.$("((P ==> S), (S ==> P), neqCom(S,P), time(dtCombine))"));
    }

    @Test
    void testRuleComonent1() throws Narsese.NarseseException {
        String s = "((P ==> S), (S ==> P), neqCom(S,P), time(dtCombine), notImpl(P), notEqui(S), task(\"?\"))";
        assertNotNull($.$(s));
    }

    @Test void testBoolean() {
        assertSame(Bool.True, $$("true"));
        assertSame(Bool.False, $$("false"));
        assertSame(Bool.Null, $$("null"));
    }

    private void eternal(Task t) {
        assertNotNull(t);
        tensed(t, true, Tense.Eternal);
    }
    private void tensed(@NotNull Task t, @NotNull Tense w) {
        tensed(t, false, w);
    }
    private void tensed(@NotNull Task t, boolean eternal, @NotNull Tense w) {
        assertEquals(eternal, t.start() == ETERNAL);
        if (!eternal) {
            switch (w) {
                case Past: assertTrue(t.start() < 0); break;
                case Future: assertTrue(t.start() > 0); break;
                case Present: assertEquals(0, t.start()); break;
                case Eternal: assertEquals(ETERNAL, t.start()); break;
            }
        }
    }

    @Test
    void testOriginalTruth() throws Narsese.NarseseException {
        
        eternal(NarseseTest.task("(a && b). %1.0;0.9%"));

        
        tensed(NarseseTest.task("(a && b). :|: %1.0;0.9%"), Present);
        tensed(NarseseTest.task("(a && b). | %1.0;0.9%"), Present);
    }



    /** compact representation combining truth and tense */
    @Test
    void testTruthTense() throws Narsese.NarseseException {






        eternal(NarseseTest.task("(a && b). %1.0;0.7%"));

        /*tensed(task("(a & b). %1.0|"), Present);
        tensed(task("(a & b). %1.0/"), Future);
        tensed(task("(a & b). %1.0\\"), Past);*/
        eternal(NarseseTest.task("(a && b). %1.0;0.9%"));


    }

    @Test
    void testQuestionTenseOneCharacter() {
        
    }

    @Test
    void testColonReverseInheritance() throws Narsese.NarseseException {
        Compound t = NarseseTest.term("namespace:named");
        assertEquals(Op.INH, t.op());
        assertEquals("named", t.sub(0).toString());
        assertEquals("namespace", t.sub(1).toString());



        Compound u = NarseseTest.term("<a:b --> c:d>");
        assertEquals("((b-->a)-->(d-->c))", u.toString());

        Task ut = NarseseTest.task("<a:b --> c:d>.");
        assertNotNull(ut);
        assertEquals(ut.term(), u);

    }









    private static void eqTerm(@NotNull String shorter, String expected) {
        Narsese p = Narsese.the();


        try {
            Term a = Narsese.term(shorter);
            assertNotNull(a);
            assertEquals(expected, a.toString());

            eqTask(shorter, expected);
        } catch (Narsese.NarseseException e) {
            fail(e);
        }
    }


    private static void eqTask(String x, String b) throws Narsese.NarseseException {
        Task a = Narsese.task(x + '.', new DummyNAL());
        assertNotNull(a);
        assertEquals(b, a.term().toString());
    }

    @Test
    void testNamespaceTerms2() {
        eqTerm("a:b", "(b-->a)");
        eqTerm("a : b", "(b-->a)");
    }

    @Test
    void testNamespaceTermsNonAtomicSubject() {
        eqTerm("c:{a,b}", "({a,b}-->c)");
    }

    @Disabled  @Test
    void testNamespaceTermsNonAtomicPredicate() {
        eqTerm("<a-->b>:c", "(c-->(a-->b))");
        eqTerm("{a,b}:c", "(c-->{a,b})");
        eqTerm("(a,b):c", "(c-->(a,b))");
    }

    @Disabled @Test
    void testNamespaceTermsChain() {

        eqTerm("d:{a,b}:c", "((c-->{a,b})-->d)");


        eqTerm("c:{a,b}", "({a,b}-->c)");
        eqTerm("a:b:c",   "((c-->b)-->a)");
        eqTerm("a :b :c",   "((c-->b)-->a)");
    }

    @Test
    void testNamespaceLikeJSON() throws Narsese.NarseseException {
        Narsese p = Narsese.the();
        Term a = Narsese.term("{ a:x, b:{x,y} }");
        assertNotNull(a);
        assertEquals(Narsese.term("{<{x,y}-->b>, <x-->a>}"), a);

    }

    @Test
    void testNegation2() throws Narsese.NarseseException {


        for (String s : new String[]{"--(negated-->a)!", "-- (negated-->a)!"}) {
            Task t = NarseseTest.task(s);

            
            /*
            (--,(negated))! %1.00;0.90% {?: 1}
            (--,(negated))! %1.00;0.90% {?: 2}
            */

            Term tt = t.term();
            assertEquals(Op.INH, tt.op());
            assertEquals("(negated-->a)", tt.toString());
            assertEquals(t.punc(), Op.GOAL);
        }
    }

    @Test
    void testNegationShortHandOnAtomics() throws Narsese.NarseseException {
        Assertions.assertEquals("(--,x)", NarseseTest.term("--x").toString());
        Assertions.assertEquals("(--,wtf)", NarseseTest.term("--wtf").toString());
        Assertions.assertEquals("(--,1)", NarseseTest.term("--1").toString());

        Assertions.assertEquals("(--,(before-->x))", NarseseTest.term("--x:before").toString());
    }

    @Test
    void testNegationShortHandOnVars() throws Narsese.NarseseException {
        for (char n : new char[] { '1', 'x' } )
            for (char t : new char[] { '%', '$', '#', '?' } ) {
                Assertions.assertEquals("(--," + t + n + ')', NarseseTest.term("--" + t + n).toString());
                Assertions.assertEquals("(a,(--," + t + n + "))", NarseseTest.term("(a, --" + t + n + ')').toString());
            }

    }

    @Test
    void testNegationShortHandOnFunc() throws Narsese.NarseseException {
        Assertions.assertEquals("(--,sentence(x))", NarseseTest.term("--sentence(x)").toString());
    }

    @Test
    void testNegationShortHandAsSubterms() throws Narsese.NarseseException {
        Assertions.assertEquals("(--,a)", NarseseTest.term("--a").toString());
        Assertions.assertEquals("((--,a))", NarseseTest.term("(--a)").toString());
        Assertions.assertEquals("((--,a),a,c)", NarseseTest.term("( --a , a, c)").toString());
        Assertions.assertEquals("((--,a),a,c)", NarseseTest.term("(--a, a, c)").toString());
        Assertions.assertEquals("(a,(--,a),c)", NarseseTest.term("(a, --a, c)").toString());
        Assertions.assertEquals("((--,a),(--,(a)),a,c)", NarseseTest.term("(--a, --(a), a, c)").toString());
    }

    @Test
    void testNegation3() throws Narsese.NarseseException {



        Assertions.assertEquals( "(--,(x))", NarseseTest.term("--(x)").toString() );
        Assertions.assertEquals( "(--,(x))", NarseseTest.term("-- (x)").toString() );

        Assertions.assertEquals( "(--,(x&&y))", NarseseTest.term("-- (x && y)").toString() );

        Assertions.assertEquals( NarseseTest.term("(goto(z) ==>+5 --(x))"),
                NarseseTest.term("(goto(z) ==>+5 (--,(x)))")
        );

        Assertions.assertEquals( NarseseTest.term("(goto(z) ==>+5 --x:y)"),
                      NarseseTest.term("(goto(z) ==>+5 (--,x:y))")
        );

        Compound nab = NarseseTest.term("--(a & b)");
        assertSame(nab.op(), Op.NEG);

        assertSame(nab.sub(0).op(), Op.CONJ);







    }

    /** tests correct order of operations */
    @Test
    void testNegationOfShorthandInh() throws Narsese.NarseseException {
        Assertions.assertEquals(
                
                "(--,(b-->a))",
                NarseseTest.term("--a:b").toString() );
        Assertions.assertEquals(
                //"((--,b)-->a)",
                "(--,(b-->a))",
                NarseseTest.term("a:--b").toString() );
        Assertions.assertEquals(
                
                //"(--,((--,b)-->a))",
                "(b-->a)",
                NarseseTest.term("--a:--b").toString() );
    }

    @Disabled
    @Test
    void testOptionalCommas() throws Narsese.NarseseException {
        Term pABC1 = $.$("(a b c)");
        Term pABC2 = $.$("(a,b,c)");
        assertEquals(pABC1, pABC2);

        Term pABC11 = $.$("(a      b c)");
        assertEquals(pABC1, pABC11);
    }


    @Test
    void testQuoteEscaping() {
        assertEquals("\"it said: \\\"wtf\\\"\"",
                $.quote("it said: \"wtf\"").toString());
    }

    @Test
    void testTripleQuote() throws Narsese.NarseseException {
        Assertions.assertEquals( "(\"\"\"triplequoted\"\"\")", NarseseTest.term("(\"\"\"triplequoted\"\"\")").toString() );
        Assertions.assertEquals( "(\"\"\"triple\"quoted\"\"\")", NarseseTest.term("(\"\"\"triple\"quoted\"\"\")").toString() );
    }

    @Test
    void testParallelTemporals() throws Narsese.NarseseException {

        
        Assertions.assertEquals("(a==>b)", NarseseTest.term("(a =|> b)").toString());
    }

    @Test
    void testParallelTemporals2() throws Narsese.NarseseException {
        Assertions.assertEquals("(x &&+2 y)", NarseseTest.term("(x &&+2 y)").toString());
    }
    @Test
    void testParallelConjInfix() throws Narsese.NarseseException {
        Assertions.assertEquals("(a&&b)", NarseseTest.term("(a &| b)").toString());
        Assertions.assertEquals("(x &&+2 ((a)&&(b)))", NarseseTest.term("(x &&+2 ((a) &| (b)))").toString());
        Assertions.assertEquals("(x &&+2 (&&,(a),(b),(c)))", NarseseTest.term("(x &&+2 ( ((a) &| (b)) &| (c)))").toString());
    }
    @Test
    void testParallelConjPrefix() throws Narsese.NarseseException {
        Assertions.assertEquals("(&&,a,b,c)", NarseseTest.term("(&|, a, b, c)").toString());
        Assertions.assertEquals("(&&,(a),(b),(c))", NarseseTest.term("(&|, (a), (b), (c))").toString());
        Assertions.assertEquals("(&&,(a),(b),(c))", NarseseTest.term("(&|,(a), (b), (c))").toString());
        Assertions.assertEquals("(x &&+2 (&&,(a),(b),(c)))", NarseseTest.term("(x &&+2 (&|,(a), (b), (c)))").toString());
    }
 
    @Test
    void testImdex() throws Narsese.NarseseException {







        
        Assertions.assertEquals(
                "(a_b)",
                NarseseTest.term("(a_b)").toString()
        );


    }

    @Test
    void testAnonymousVariable() throws Narsese.NarseseException {

        
        String input = "((_,_) <-> x)";

        Compound x = NarseseTest.term(input);
        
        assertEquals("((_,_)<->x)", x.toString());

        Term y = x.normalize();
        
        assertEquals("((#1,#2)<->x)", y.toString());

        Task question = NarseseTest.task(x + "?");
        assertEquals("((#1,#2)<->x)?", question.toStringWithoutBudget());

        Task belief = NarseseTest.task(x + ".");
        assertEquals("((#1,#2)<->x). %1.0;.90%", belief.toStringWithoutBudget());

    }
}

