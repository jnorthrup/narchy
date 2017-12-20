package nars.term;

import jcog.list.FasterList;
import nars.*;
import nars.io.NarseseTest;
import nars.task.util.InvalidTaskException;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.*;
import static nars.$.conj;
import static nars.Op.*;
import static nars.term.TermTest.assertValid;
import static nars.term.TermTest.assertValidTermValidConceptInvalidTaskContent;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 12/10/15.
 */
public class TermReductionsTest extends NarseseTest {

    @Nullable
    final Term p = Atomic.the("P"), q = Atomic.the("Q"), r = Atomic.the("R"), s = Atomic.the("S");


    @Test
    public void testIntersectExtReduction1() throws Narsese.NarseseException {
        // (&,R,(&,P,Q)) = (&,P,Q,R)
        assertEquals("(&,P,Q,R)", secte(r, secte(p, q)).toString());
        assertEquals("(&,P,Q,R)", $("(&,R,(&,P,Q))").toString());
    }

    @Test
    public void testIntersectExtReduction2() throws Narsese.NarseseException {
        // (&,(&,P,Q),(&,R,S)) = (&,P,Q,R,S)
        assertEquals("(&,P,Q,R,S)", secte(secte(p, q), secte(r, s)).toString());
        assertEquals("(&,P,Q,R,S)", $("(&,(&,P,Q),(&,R,S))").toString());
    }

    @Test
    public void testIntersectExtReduction3() throws Narsese.NarseseException {
        // (&,(&,P,Q),(&,R,S)) = (&,P,Q,R,S)
        assertEquals("(&,P,Q,R,S,T,U)", $("(&,(&,P,Q),(&,R,S), (&,T,U))").toString());
    }

    @Test
    public void testIntersectExtReduction2_1() throws Narsese.NarseseException {
        // (&,R,(&,P,Q)) = (&,P,Q,R)
        assertEquals("(&,P,Q,R)", $("(&,R,(&,P,Q))").toString());
    }

    @Test
    public void testIntersectExtReduction4() throws Narsese.NarseseException {
        //UNION if (term1.op(Op.SET_INT) && term2.op(Op.SET_INT)) {
        assertEquals("{P,Q,R,S}", secte(sete(p, q), sete(r, s)).toString());
        assertEquals("{P,Q,R,S}", $("(&,{P,Q},{R,S})").toString());
    }

    @Test
    public void testIntersectExtReduction5() {
        assertEquals(Null /* emptyset */, secte(seti(p, q), seti(r, s)));
    }

    @Test
    public void testIntersectIntReduction1() throws Narsese.NarseseException {
        // (|,R,(|,P,Q)) = (|,P,Q,R)
        assertEquals("(|,P,Q,R)", secti(r, secti(p, q)).toString());
        assertEquals("(|,P,Q,R)", $("(|,R,(|,P,Q))").toString());
    }

    @Test
    public void testIntersectIntReduction2() throws Narsese.NarseseException {
        // (|,(|,P,Q),(|,R,S)) = (|,P,Q,R,S)
        assertEquals("(|,P,Q,R,S)", secti(secti(p, q), secti(r, s)).toString());
        assertEquals("(|,P,Q,R,S)", $("(|,(|,P,Q),(|,R,S))").toString());
    }

    @Test
    public void testIntersectIntReduction3() throws Narsese.NarseseException {
        // (|,R,(|,P,Q)) = (|,P,Q,R)
        assertEquals("(|,P,Q,R)", $("(|,R,(|,P,Q))").toString());
    }

    @Test
    public void testIntersectIntReduction4() throws Narsese.NarseseException {
        //UNION if (term1.op(Op.SET_INT) || term2.op(Op.SET_INT)) {
        assertEquals("[P,Q,R,S]", secti(seti(p, q), seti(r, s)).toString());
        assertEquals("[P,Q,R,S]", $("(|,[P,Q],[R,S])").toString());

    }

    @Test
    public void testCyclicalNAL1_and_NAL2() {

        assertInvalidTerms("((#1~swan)-->#1)");
        assertInvalidTerms(
                "((swimmer~swan)-->swimmer)",
                "((x|y)-->x)",
                "(y<->(x|y))",
                "(#1<->(#1|y))"
        );
    }

    @Test
    public void testIntersectIntReductionToZero() {
        assertInvalidTerms("(|,{P,Q},{R,S})");
    }

    @Test
    public void testIntersectIntReduction_to_one() throws Narsese.NarseseException {
        assertEquals("(robin-->bird)", $("<robin-->(|,bird)>").toString());
        assertEquals("(robin-->bird)", $("<(|,robin)-->(|,bird)>").toString());
    }

    @Test
    public void testFunctionRecursion() throws Narsese.NarseseException {
        //that this is valid, though self referential
        assertTrue($("task((polarize(%1,task) ==>+- polarize(%2,belief)))").subs() > 0);
    }

//    @Test
//    public void testInvalidEquivalences() throws Narsese.NarseseException {
//        assertEquals("(P<=>Q)", equi(p, q).toString());
//
//        assertInvalid(() -> equi(impl(p, q), r));
//        assertInvalid(() -> equi(equi(p, q), r));
//        assertInvalidTerms("<<a <=> b> <=> c>");
//    }


    @Test
    public void testSimilarityNegatedSubtermsDoubleNeg() throws Narsese.NarseseException {
        assertEquals(("((--,(P))<->(--,(Q)))"), $("((--,(P))<->(--,(Q)))").toString()); //SAME should not change
        /*
        <patham9> <-> is a relation in meaning not in truth
        <patham9> so negation can't enforce any equivalence here
        */
    }

    @Test
    public void testSimilarityNegatedSubterms() throws Narsese.NarseseException {
        assertEquals("((--,(Q))<->(P))", $("((P)<->(--,(Q)))").toString()); //NO change
        assertEquals("((--,(P))<->(Q))", $("((--,(P))<->(Q))").toString()); //NO change
    }


//    @Test
//    public void testEquivalenceNegatedSubterms() throws Narsese.NarseseException {
//        assertEquals(("(--,((P)<=>(Q)))"), $("((P)<=>(--,(Q)))").toString());
//        assertEquals(("(--,((P)<=>(Q)))"), $("((--,(P))<=>(Q))").toString());
//        assertEquals(("((P) <=>+1 (Q))"), $("((--,(P)) <=>+1 (--,(Q)))").toString());
//        assertEquals(("((P)<=>(Q))"), $("((--,(P))<=>(--,(Q)))").toString());
//    }

    @Test
    public void testImplicationNegatedPredicate() throws Narsese.NarseseException {
        assertEquals("(--,((P)==>(Q)))", $("((P)==>(--,(Q)))").toString());
        assertEquals(("((--,(P))==>(Q))"), $("((--,(P))==>(Q))").toString()); //SAME should not change
    }

    @Test
    public void testConjInhReflexive() throws Narsese.NarseseException {
        assertEquals("((a &&+5 x)-->a)", $("((a &&+5 x)-->a)").toString());
        assertEquals("(x-->(a &&+5 x))", $("(x-->(a &&+5 x))").toString());
        assertEquals("((a&&b)-->(a&&c))", $("((a&&b)-->(a&&c))").toString());
    }

    @Test
    public void testPointlessImplicationSubtermRepeat() throws Narsese.NarseseException {
        assertEquals("((a &&+5 x) ==>+5 c)", $("((a &&+5 x)=|>(x &&+5 c))").toString());

        assertEquals(True, $("((a &&+5 x)=|>x)"));

        assertEquals("((a &&+5 $1) ==>+5 c)", $("((a &&+5 $1)=|>($1 &&+5 c))").toString());

        assertEquals(Null, $("((a &&+5 $1) ==>-5 a"));

    }

    @Test
    public void testPointlessImplicationSubtermRepeat2() throws Narsese.NarseseException {
        //unaffected because of eternality
        //assertEquals("((a &&+5 x)=|>(&&,x,y,z))", $("((a &&+5 x)=|>(&&,x,y,z))").toString());
        assertEquals("((a &&+5 x)=|>(y&&z))", $("((a &&+5 x)=|>(&&,x,y,z))").toString());

        //affected because of temporality
        assertEquals("((a &&+5 x)=|>(y&|z))", $("((a &&+5 x)=|>((x&|y)&|z)))").toString());

    }


    @Test
    public void testImplicationShouldBeValid() throws Narsese.NarseseException {
        String o = "(((--,tetris(isRow,2,true))&|tetris(isRowClear,8,true)) ==>-807 (((--,tetris(isRow,2,true))&&tetris(isRowClear,8,true))&|tetris(isRowClear,8,true)))";
        Term oo = $(o);
        assertEquals(o, oo.toString() );
    }

    @Test
    public void testImplicationNegatedPredicateImplicated() throws Narsese.NarseseException {

        //subject disallows implication
        assertEquals(Null, $("((--,(x==>y)) ==> z)"));

        //but predicate it is reduced to conj in subj
        assertEquals("(--,((x&&y)==>z))", $("(x ==> (--,(y==>z)))").toString());
    }


//    @Test
//    public void testReducedAndInvalidImplications1() throws Narsese.NarseseException {
//        assertInvalidTerms("<<P<=>Q> ==> R>");
//    }

    @Test
    public void testReducedAndInvalidImplications5() {

        assertInvalidTerms("((P==>Q) ==> R)");
    }

//    @Test
//    public void testReducedAndInvalidImplications6() throws Narsese.NarseseException {
//        assertInvalidTerms("<R ==> <P<=>Q>>");
//    }

    @Test
    public void testConjPosNegElimination1() throws Narsese.NarseseException {
        //  (a ∧ ¬(a ∧ b))  =⇒ ¬b WRONG
        //  a and not(a and b)
        //  http://www.wolframalpha.com/input/?i=(a+and+not(a+and+b))
        assertEquals("((--,b)&&a)", $.$("(a && --(a && b))").toString());
    }

    @Test
    public void testConjPosNegElimination2() throws Narsese.NarseseException {
        //  (¬a ∧ (a ∨ b)) =⇒ b WRONG
        //  not(a) and (a or b)
        //  http://www.wolframalpha.com/input/?i=not(a)+and+(a+or+b)
        assertEquals("((--,a)&&b)", $.$("(--a && (||,a,b))").toString());
    }

    @Test
    public void testReducedAndInvalidImplications2() throws Narsese.NarseseException {
        assertEquals("((P&&R)==>Q)", $("(R==>(P==>Q))").toString());
        assertEquals("((R &&+2 P) ==>+1 Q)", $("(R ==>+2 (P ==>+1 Q))").toString());
        assertEquals("(((S &&+1 R) &&+2 P) ==>+1 Q)", $("((S &&+1 R) ==>+2 (P ==>+1 Q))").toString());
    }



    @Test
    public void testConjParallelConceptualShouldntBeXTERNAL() throws Narsese.NarseseException {


        for (int dt : new int[]{ /*XTERNAL,*/ DTERNAL, 0}) {
            assertEquals("(&&,a,b,c)",
                    CONJ.the(
                            dt,
                            $.$("a"),
                            $.$("b"),
                            $.$("c")
                    ).conceptual().toString());
        }

        //"(&&,(--,(&&,(bx-->noid),(happy-->noid),#1)),(--,(&&,(bx-->noid),(happy-->noid),#1)),(--,(dx-->noid)))",
        //$("(&&,(--,(&|,(bx-->noid),(happy-->noid),#1)),(--,( &&+- ,(bx-->noid),(happy-->noid),#1)),(--,(dx-->noid)))")

        assertEquals(
                "(&&,(bx-->noid),(happy-->noid),#1)",
                $("(--,(((bx-->noid) &| (happy-->noid)) &| #1))")
                        .conceptual().toString());
        assertEquals(
                "(x,(--,(&&,a,b,c)))",
                $("(x,(--,(( a &| b) &| c)))")
                        .conceptual().toString());
    }


    @Test
    public void testConjRepeatPosNeg() {
        Term x = $.the("x");
        assertEquals( +1, CONJ.the(-1, x, x).dt());
        assertEquals( +1, CONJ.the(+1, x, x).dt());
        assertArrayEquals(IO.termToBytes(CONJ.the(+32, x, x)), IO.termToBytes(CONJ.the(-32, x, x)));
        assertEquals( +1, CONJ.the(XTERNAL, x, x).dt(-1).dt());
        assertEquals( +1, CONJ.the(XTERNAL, x, x).dt(+1).dt());
        assertEquals( CONJ.the(-1, x, x), CONJ.the(+1, x, x) );
        assertEquals( CONJ.the(XTERNAL, x, x).dt(-1), CONJ.the( XTERNAL, x, x).dt(+1) );
    }

    @Test
    public void testConjEvents() throws Narsese.NarseseException {
        assertEquals(
                "(a &&+16 ((--,a)&|b))",
                Op.conj(
                        new FasterList<LongObjectPair<Term>>(new LongObjectPair[]{
                                PrimitiveTuples.pair(298L, $.$("a")),
                                PrimitiveTuples.pair(314L, $.$("b")),
                                PrimitiveTuples.pair(314L, $.$("(--,a)"))})
                ).toString()
        );
    }

    @Test
    public void testReducedAndInvalidImplications3() {
        assertInvalidTerms("<R==><P==>R>>");
    }

    @Test
    public void testReducedAndInvalidImplications4() throws Narsese.NarseseException {
        assertEquals("(R==>P)", $("(R==>(R==>P))").toString());
    }

//    @Test public void testReducedAndInvalidImplicationsTemporal() throws Narsese.NarseseException {
//        assertNull($("<<P<=>Q> =/> R>"));
//        assertNull($("<R =/> <P<=>Q>>"));
//
//        assertNull($("<<P==>Q> =/> R>"));
//        assertNull($("<<P==>Q> =|> R>"));
//        assertNull($("<<P==>Q> =|> R>"));
//    }
//
//    @Test public void testReducedAndInvalidImplicationsTemporal2() throws Narsese.NarseseException {
//        assertEquals("<(&|,P,R)=|>Q>", $("<R=|><P==>Q>>").toString());
//    }
//    @Test public void testReducedAndInvalidImplicationsTemporal3() throws Narsese.NarseseException {
//        assertEquals("<(&/,R,P)=/>Q>", $("<R=/><P==>Q>>").toString());
//    }
//    @Test public void testReducedAndInvalidImplicationsTemporal4() throws Narsese.NarseseException {
//        assertEquals("<(&/,P,R)=\\>Q>", $("<R=\\><P==>Q>>").toString());
//    }

    //TODO:
        /*
            (&,(&,P,Q),R) = (&,P,Q,R)
            (&,(&,P,Q),(&,R,S)) = (&,P,Q,R,S)

            // set union
            if (term1.op(Op.SET_INT) && term2.op(Op.SET_INT)) {

            // set intersection
            if (term1.op(Op.SET_EXT) && term2.op(Op.SET_EXT)) {

         */

    @Test
    public void testIntersectionRange() {
        assertEquals("(8|4..5)", Op.SECTi.the(Int.the(4), Int.the(8), Int.range(4, 5)).toString());
        assertEquals("(8&4..5)", Op.SECTe.the(Int.the(4), Int.the(8), Int.range(4, 5)).toString());
        //TODO for Set's
    }

    @Test
    public void testDisjunctEqual() {
        @NotNull Term pp = p(this.p);
        assertEquals(pp, disj(pp, pp));
    }

    @Test
    public void testDisjReduction1() {
        //"(||,R,R) |- R"
        Term x = $.the("x");
        assertEquals(x, $.disj(x, x));
        assertEquals(x, CONJ.the(DTERNAL, x.neg(), x.neg()).neg());
    }

    @Disabled
    @Test
    public void testRepeatConjunctionTaskSimplification() throws Narsese.NarseseException {
        //the repeats in the conjunction term can be replaced with a single event with equivalent start/stop time
        assertEquals(
                "$.50 (x). 0⋈10 %1.0;.90%",
                Narsese.the().task("((x) &&+10 (x)). :|:", NARS.shell()).toString());
    }

    @Test
    public void testConjParallelWithSeq() throws Narsese.NarseseException {
        assertEquals("(a &&+5 b)", $("((a &&+5 b)&|a)").toString());

        assertEquals(False, $("((--a &&+5 b)&|a)"));
    }

    @Test
    public void testEmbeddedConjNormalizationN2() throws Narsese.NarseseException {
        Compound bad = $("(a &&+1 (b &&+1 c))");
        Compound good = $("((a &&+1 b) &&+1 c)");
        assertEquals(good, bad);
        assertEquals(good.toString(), bad.toString());
        assertEquals(good.dt(), bad.dt());
        assertEquals(good.subterms(), bad.subterms());
    }

    @Test
    public void testEmbeddedConjNormalizationN2Neg() throws Narsese.NarseseException {
        Compound alreadyNormalized = $("((c &&+1 b) &&+1 a)");
        Compound needsNormalized = $("(a &&-1 (b &&-1 c))");
        assertEquals(alreadyNormalized, needsNormalized);
        assertEquals(alreadyNormalized.toString(), needsNormalized.toString());
        assertEquals(alreadyNormalized.dt(), needsNormalized.dt());
        assertEquals(alreadyNormalized.subterms(), needsNormalized.subterms());
    }

    @Test
    public void testEmbeddedConjNormalizationN3() throws Narsese.NarseseException {

        String ns = "((a &&+1 b) &&+1 (c &&+1 d))";
        Compound normal = $(ns);
        //normal.printRecursive();
        assertEquals(3, normal.dtRange());
        assertEquals(ns, normal.toString());

        for (String unnormalized : new String[]{
                "(a &&+1 (b &&+1 (c &&+1 d)))", //imbalanced towards right
                "(((a &&+1 b) &&+1 c) &&+1 d)"  //imbalanced towards left
        }) {
            Compound u = $(unnormalized);
            assertEquals(normal, u);
            assertEquals(normal.toString(), u.toString());
            assertEquals(normal.dt(), u.dt());
            assertEquals(normal.subterms(), u.subterms());
        }
    }

    @Test
    public void testEmbeddedConjNormalizationWithNeg1() throws Narsese.NarseseException {
        String d = "(((d) &&+3 (a)) &&+1 (b))"; //correct grouping

        String c = "((d) &&+3 ((a) &&+1 (b)))"; //incorrect grouping
        Term cc = $(c);
        assertEquals(d, cc.toString());

        String a = "(((a) &&+1 (b)) &&-3 (d))"; //incorrect order
        Term aa = $(a);
        assertEquals(d, aa.toString());


//        aa.printRecursive();
//        cc.printRecursive();


        //correct subterm ordering by volume
        assertTrue(aa.sub(0).subs() > aa.sub(1).subs());
        assertTrue(cc.sub(0).subs() > cc.sub(1).subs());

    }

    @Test
    public void testEmbeddedConjNormalizationB() throws Narsese.NarseseException {
        assertEquals(
                "(((--,noid(0,5)) &&+- noid(11,2)) &&+- (noid(11,2) &&+- noid(11,2)))",
                $("((((--,noid(0,5)) &&+- noid(11,2)) &&+- noid(11,2)) &&+- noid(11,2))").toString()
        );
    }

    @Test
    public void testEmbeddedConjNormalization2() throws Narsese.NarseseException {
        assertEquals(
                "((a &&+1 b) &&+3 (c &&+5 d))",
                $("(a &&+1 (b &&+3 (c &&+5 d)))").toString()
        );

        assertEquals(
                "(((t2-->hold) &&+1 (t1-->at)) &&+3 ((t1-->[opened]) &&+5 open(t1)))",
                $("(hold:t2 &&+1 (at:t1 &&+3 ([opened]:t1 &&+5 open(t1))))").toString()
        );
    }

    @Test
    public void testConjMergeABCShift() throws Narsese.NarseseException {
        /* WRONG:
            $.23 ((a &&+5 ((--,a)&|b)) &&+5 ((--,b) &&+5 (--,c))). 1⋈16 %1.0;.66% {171: 1;2;3;;} ((%1,%2,task("."),time(raw),time(dtEvents),notImpl(%1),notImpl(%2)),((polarize(%1,task) &&+- polarize(%2,belief)),((IntersectionDepolarized-->Belief))))
              $.50 (a &&+5 (--,a)). 1⋈6 %1.0;.90% {1: 1}
              $.47 ((b &&+5 (--,b)) &&+5 (--,c)). 6⋈16 %1.0;.73% {43: 2;3;;} ((%1,%1,task("&&")),(dropAnyEvent(%1),((StructuralDeduction-->Belief),(StructuralDeduction-->Goal))))
        */
        Term a = $.$("(a &&+5 (--,a))");
        Term b = $.$("((b &&+5 (--,b)) &&+5 (--,c))");
        Term ab = Op.conjMerge(a, 1, b, 6);
        assertEquals("((a &&+5 ((--,a)&|b)) &&+5 ((--,b) &&+5 (--,c)))", ab.toString());
    }


    @Test
    public void testConjunctionEqual() {
        assertEquals(p, conj(p, p));
    }

    @Test
    public void testConjunctionNormal() throws Narsese.NarseseException {
        Term x = $("(&&, <#1 --> lock>, <#1 --> (/, open, #2, _)>, <#2 --> key>)");
        assertEquals(3, x.subs());
        assertEquals(CONJ, x.op());
    }

    @Test
    public void testIntExtEqual() {
        assertEquals(p, secte(p, p));
        assertEquals(p, secti(p, p));
    }

    @Test
    public void testDiffIntEqual() {

        assertEquals(Null, diffi(p, p));
    }

    @Test
    public void testDiffExtEqual() {

        assertEquals(Null, diffe(p, p));
    }


    @Test
    public void testDifferenceSorted() {
//        assertArrayEquals(
//            new Term[] { r, s },
//            Terms.toArray(TermContainer.differenceSorted(sete(r, p, q, s), sete(p, q)))
//        );
        //check consistency with differenceSorted
        assertArrayEquals(
                new Term[]{r, s},
                ((Compound) Op.difference(Op.SETe, sete(r, p, q, s), sete(p, q))).arrayClone()
        );
    }

    @Test
    public void testDifferenceSortedEmpty() {
//        assertArrayEquals(
//                new Term[] { },
//                Terms.toArray(TermContainer.differenceSorted(sete(p, q), sete(p, q)))
//        );
        //check consistency with differenceSorted
        assertEquals(
                Null,
                Op.difference(Op.SETe, sete(p, q), sete(p, q))
        );
    }


    @Test
    public void testDifference() throws Narsese.NarseseException {
        /*tester.believe("<planetX --> {Mars,Pluto,Venus}>",0.9f,0.9f); //.en("PlanetX is Mars, Pluto, or Venus.");
        tester.believe("<planetX --> {Pluto,Saturn}>", 0.1f, 0.9f); //.en("PlanetX is probably neither Pluto nor Saturn.");
        tester.mustBelieve(cycles, "<planetX --> {Mars,Venus}>", 0.81f ,0.81f); //.en("PlanetX is either Mars or Venus.");*/


        assertEquals(
                $("{Mars,Venus}"),
                Op.difference(Op.SETe, $("{Mars,Pluto,Venus}"), $.<Compound>$("{Pluto,Saturn}"))
        );
        assertEquals(
                $("{Saturn}"),
                Op.difference(Op.SETe, $("{Pluto,Saturn}"), $.<Compound>$("{Mars,Pluto,Venus}"))
        );


//        //test identity does not create new instance, single term
//        Compound b = $("{Mars}");
//        assertTrue(
//                b ==
//                TermContainer.difference(
//                        b,
//                        $("{Pluto}")
//                )
//        );
//
//        //test identity does not create new instance, multiterm
//        Compound a = $("{Mars,Venus}");
//        assertTrue(
//                a ==
//                        TermContainer.difference(
//                                a,
//                                $("{Pluto,PlanetX}")
//                        )
//        );
    }


    @Test
    public void testDifferenceImmediate() throws Narsese.NarseseException {

        Term d = diffi(
                seti($("a"), $("b"), $("c")),
                seti($("d"), $("b")));
        assertEquals(Op.SETi, d.op());
        assertEquals(2, d.subs());
        assertEquals("[a,c]", d.toString());
    }

    @Test
    public void testDifferenceImmediate2() throws Narsese.NarseseException {


        Term a = sete($("a"), $("b"), $("c"));
        Term b = sete($("d"), $("b"));
        Term d = diffe(a, b);
        assertEquals(Op.SETe, d.op());
        assertEquals(2, d.subs());
        assertEquals("{a,c}", d.toString());

    }

    @Test
    public void testDisjunctionReduction() throws Narsese.NarseseException {

        assertEquals("(||,(a-->x),(b-->x),(c-->x),(d-->x))",
                $("(||,(||,x:a,x:b),(||,x:c,x:d))").toString());
        assertEquals("(||,(b-->x),(c-->x),(d-->x))",
                $("(||,x:b,(||,x:c,x:d))").toString());
    }

    @Test
    public void testConjunctionReduction() throws Narsese.NarseseException {
        assertEquals("(&&,a,b,c,d)",
                $("(&&,(&&,a,b),(&&,c,d))").toString());
        assertEquals("(&&,b,c,d)",
                $("(&&,b,(&&,c,d))").toString());
    }

    @Test
    public void testTemporalConjunctionReduction1() throws Narsese.NarseseException {
        assertEquals("(a&|b)", $("(a &&+0 b)").toString());
        assertEquals(
                $("((--,(ball_left)) &&-270 (ball_right))"),
                $("((ball_right) &&+270 (--,(ball_left)))"));

    }

    @Test
    public void testConjunctionParallelWithConjunctionParallel() throws Narsese.NarseseException {
        assertEquals(
                "(&|,nario(13,27),nario(21,27),nario(24,27))",
                $("((nario(21,27)&|nario(24,27))&|nario(13,27))").toString()
        );
    }

    @Test
    public void testTemporalConjunctionReduction2() throws Narsese.NarseseException {
        assertEquals("((a&|b) &&+1 c)", $("(a &&+0 (b &&+1 c))").toString());
    }

    @Test
    public void testTemporalConjunctionReduction3() throws Narsese.NarseseException {
        assertEquals("(a&|b)", $("( (a &&+0 b) && (a &&+0 b) )").toString());
    }

    @Test
    public void testTemporalConjunctionReduction5() throws Narsese.NarseseException {
        assertEquals("((a&|b)&&(a &&+1 b))",
                $("( (a&|b) && (a &&+1 b) )").toString());
    }

    @Test
    public void testTemporalConjunctionReduction4() throws Narsese.NarseseException {
        assertEquals("(a&|b)", $("( a &&+0 (b && b) )").toString());
    }


    @Test
    public void testTemporalNTermConjunctionParallel() throws Narsese.NarseseException {
        //+0 is the only case in which temporal && can have arity>2
        //TODO fix spacing:
        assertEquals("(&|,a,b,c)", $("( a &&+0 (b &&+0 c) )").toString());
    }

    @Disabled
    @Test
    public void testTemporalNTermEquivalenceParallel() throws Narsese.NarseseException {
        //+0 is the only case in which temporal && can have arity>2
        assertEquals("(<|>, a, b, c)", $("( a <|> (b <|> c) )").toString());
    }


    @Test
    public void testMultireduction() {
        //TODO probably works
    }

    @Test
    public void testConjunctionMultipleAndEmbedded() throws Narsese.NarseseException {

        assertEquals("(&&,a,b,c,d)",
                $("(&&,(&&,a,b),(&&,c,d))").toString());
        assertEquals("(&&,a,b,c,d,e,f)",
                $("(&&,(&&,a,b),(&&,c,d), (&&, e, f))").toString());
        assertEquals("(&&,a,b,c,d,e,f,g,h)",
                $("(&&,(&&,a,b, (&&, g, h)),(&&,c,d), (&&, e, f))").toString());
    }

    @Test
    public void testConjunctionEquality() throws Narsese.NarseseException {

        assertEquals(
                $("(&&,r,s)"),
                $("(&&,s,r)"));
//        assertNotEquals(
//            $("(&/,r,s)"),
//            $("(&/,s,r)"));
//        assertEquals(
//            $("(&|,r,s)"),
//            $("(&|,s,r)"));

    }

    @Test
    public void testImplicationTrue() throws Narsese.NarseseException {
        assertEquals(True, $("(x==>x)"));
        assertEquals(True, $("((x)==>(x))"));
        assertEquals(Null, $("(--x==>x)"));
        assertEquals(Null, $("(--(x)==>(x))"));
    }
    @Test
    public void testImplicationInequality() throws Narsese.NarseseException {

        assertNotEquals(
                $("<r ==> s>"),
                $("<s ==> r>"));
//        assertNotEquals(
//                $("<r =/> s>"),
//                $("<s =/> r>"));
//        assertNotEquals(
//                $("<r =\\> s>"),
//                $("<s =\\> r>"));
//        assertNotEquals(
//                $("<r =|> s>"),
//                $("<s =|> r>"));

    }

    @Test
    public void testDisjunctionMultipleAndEmbedded() throws Narsese.NarseseException {

        assertEquals("(||,(a),(b),(c),(d))",
                $("(||,(||,(a),(b)),(||,(c),(d)))").toString());
        assertEquals("(||,(a),(b),(c),(d),(e),(f))",
                $("(||,(||,(a),(b)),(||,(c),(d)), (||,(e),(f)))").toString());
        assertEquals("(||,(a),(b),(c),(d),(e),(f),(g),(h))",
                $("(||,(||,(a),(b), (||,(g),(h))),(||,(c),(d)), (||,(e),(f)))").toString());

    }

    @Test
    public void testImplicationConjCommonSubterms() throws Narsese.NarseseException {
        assertEquals("((&&,a,b,c)==>d)",
                $("((&&, a, b, c) ==> (&&, a, d))").toString());
        assertEquals("((a&&d)==>(b&&c))",
                $("((&&, a, d) ==> (&&, a, b, c))").toString());
        assertInvalidTerms("((&&, a, b, c) ==> (&&, a, b))");
        assertEquals("((a&&b)==>c)", $("((&&, a, b) ==> (&&, a, b, c))").toString());
        assertEquals(True, $("((&&, a, b, c) ==> a)"));

        assertEquals("(a==>(b&&c))", $("(a ==> (&&, a, b, c))").toString());
    }

    @Test
    public void testConegatedConjunctionTerms0() throws Narsese.NarseseException {
        assertEquals(Null, $("(#1 && (--,#1))"));
        assertEquals(Null, $("(#1 &| (--,#1))"));
        assertEquals(Null, parallel(varDep(1), varDep(1).neg()));

        assertEquals(Null, $("(&&, #1, (--,#1), (x))"));
        assertEquals(Null, $("(&&, --(#1 && (--,#1)), (x))"));

        assertSame($("((x) &&+1 --(x))").op(), CONJ);
        assertSame($("(#1 &&+1 (--,#1))").op(), CONJ);


    }


    @Test
    public void testInvalidStatementIndepVarTask() {
        NAR t = NARS.shell();
        try {
            t.inputTask("at($1,$2,$3)");
            fail("");
        } catch (Narsese.NarseseException | InvalidTaskException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testConegatedConjunctionTerms1() throws Narsese.NarseseException {
        assertEquals($("((--,((y)&&(z)))&&(x))"), $("((x) && --((y) && (z)))"));
    }

    @Test
    public void testConegatedConjunctionTerms0not() throws Narsese.NarseseException {
        //dont unwrap due to different 'dt'
        assertEquals("((--,((y)&|(z)))&&(x))", $("((x)&&--((y) &&+0 (z)))").toString());

        assertEquals("((--,((y)&&(z)))&|(x))", $("((x) &&+0 --((y) && (z)))").toString());
    }

    @Test
    public void testConegatedConjunctionTerms1not() throws Narsese.NarseseException {
        //dont unwrap due to different 'dt'
        assertEquals("((--,((y) &&+1 (z)))&&(x))", $("((x)&&--((y) &&+1 (z)))").toString());

        assertEquals("((x) &&+1 (--,((y)&&(z))))", $("((x) &&+1 --((y) && (z)))").toString());
    }

    @Test
    public void testConegatedConjunctionTerms2() throws Narsese.NarseseException {
        //(x && not(x&&y)) |- x && not(y)
        assertEquals(
                "((--,(robin-->swimmer))&&#1)",
                $("(#1 && --(#1&&(robin-->swimmer)))").toString()
        );
    }

    @Test
    public void testDemorgan1() throws Narsese.NarseseException {
        //https://en.wikipedia.org/wiki/De_Morgan%27s_laws


        // \neg(P\and Q)\iff(\neg P)\or(\neg Q)
        assertEquals("(--,((p)&&(q)))",
                $("(||, --(p), --(q))").toString());
    }

    @Disabled
    @Test
    public void testDemorgan2() throws Narsese.NarseseException {

        // \neg(P\or Q)\iff(\neg P)\and(\neg Q),
        assertEquals("(--,((p)||(q)))",
                $("(--(p) && --(q))").toString());
    }

    @Test
    public void testCoNegatedJunction() throws Narsese.NarseseException {
        //the conegation cancels out conflicting terms

        assertEquals(Null, $("(&&,x,a:b,(--,a:b))"));

        assertEquals(Null, $("(&&, (a), (--,(a)), (b))")); //a cancels, reduce to 'b'
        assertEquals(Null, $("(&&, (a), (--,(a)), (b), (c))"));


        assertEquals(Null, $("(&&,x,y,a:b,(--,a:b))"));
    }


    @Test
    public void testCoNegatedDisjunction() throws Narsese.NarseseException {

        assertEquals(Null,
                $("(||,x,a:b,(--,a:b))"));

        assertEquals(Null,
                $("(||,x,y,a:b,(--,a:b))"));

    }

    @Test
    public void testFilterCommutedWithCoNegatedSubterms() throws Narsese.NarseseException {
        //any commutive terms with both a subterm and its negative are invalid


        assertValidTermValidConceptInvalidTaskContent(("((--,(a1)) && (a1))"));
        assertValidTermValidConceptInvalidTaskContent("((--,(a1)) &&+0 (a1))");
        assertValid($("((--,(a1)) &&+1 (a1))"));
        assertValid($("((a1) &&+1 (a1))"));

        assertEquals($("(a1)"), $("((a1) &&+0 (a1))"));
        assertEquals($("(a1)"), $("((a1) && (a1))"));
        assertNotEquals($("(a1)"), $("((a1) &&+1 (a1))"));

        assertInvalidTerms("((--,(a1)) || (a1))");

    }

    @Test
    public void testRepeatInverseEquivalent() throws Narsese.NarseseException {
        assertEquals($("((a1) &&-1 (a1))"), $("((a1) &&+1 (a1))"));
        assertEquals($("((a1) <=>-1 (a1))"), $("((a1) <=>+1 (a1))"));
    }

    @Test
    public void testAllowInhNegationStatements() throws Narsese.NarseseException {
        assertEquals(True, $("(a-->a)"));

        assertEquals("((--,a)-->b)", $("((--,a) --> b)").toString());
        assertNotEquals("(a-->b)", $("((--,a) --> b)").toString());
        assertEquals("(b-->(--,a))", $("(b --> (--,a))").toString());
        assertNotEquals("(a-->b)", $("(b --> (--,a))").toString());
        assertEquals("((--,a)-->(--,b))", $("(--a --> --b)").toString());

        assertEquals("((--,a)-->a)", $("((--,a)-->a)").toString());
        assertEquals("(a-->(--,a))", $("(a-->(--,a))").toString());

    }

    @Test
    public void testAllowSimNegationStatements() throws Narsese.NarseseException {
        assertEquals(True, $("(a<->a)"));

        assertNotEquals($("(--a <-> b)"), $("(a <-> --b)"));

        assertEquals("((--,a)<->b)", $("((--,a) <-> b)").toString());
        assertNotEquals("(a<->b)", $("((--,a) <-> b)").toString());
        assertEquals("((--,a)<->b)", $("(b <-> (--,a))").toString());
        assertNotEquals("(a<->b)", $("(b <-> (--,a))").toString());
        assertEquals("((--,a)<->(--,b))", $("(--a <-> --b)").toString());

        assertEquals("((--,a)<->a)", $("((--,a)<->a)").toString());

    }


    @Test
    public void testCoNegatedImpl() {
        assertValidTermValidConceptInvalidTaskContent(("((--,(a)) ==> (a))"));
        assertValidTermValidConceptInvalidTaskContent(("((--,(a)) ==>+0 (a))"));
    }

//    @Test
//    public void testXternalIsInvalidForTaskContent() {
//        assertValidTermValidConceptInvalidTaskContent(("((--,(a)) <=>+- (a))"));
//    }

//    @Test
//    public void testCoNegatedEqui() throws Narsese.NarseseException {
//
//        assertEquals(False, $("((--,(a)) <=> (a))"));
//
//        assertEquals(False, $("((--,(a)) <=>+0 (a))"));
//
//        String e = "(--,((a) <=>+1 (a)))";
//        assertEquals(e, $(e).toString());
//
//        //due to the unpaired $3
//        assertInvalidTasks("(((--,isIn($1,xyz))&&(--,(($1,xyz)-->$2)))<=>((--,(($1,xyz)-->$2))&&(--,isIn($3,xyz)))).");
//    }

    @Test
    public void testImplCommonSubterms() throws Narsese.NarseseException {
        //factor out the common sub-term
        assertEquals(
                "(((--,isIn($1,xyz))&&(--,(($1,xyz)-->$2)))==>((y-->x)))", //involves an additional negation factoring out to top level
                $("(((--,isIn($1,xyz))&&(--,(($1,xyz)-->$2)))==>((--,(($1,xyz)-->$2))&&(x:y)))").toString());
    }

    @Test
    public void testConjNearIdentity() throws Narsese.NarseseException {
        assertEquals(
                True,
                $("( (a&&b) ==> (a&|b) )") //differ in dt=0 and dt=DTERNAL
        );

        assertEquals(
                //"(a&|b)",
                "(&|,(a&&b),a,b)",
                $("( (a&&b) &| (a&|b) )").toString() //differ in dt=0 and dt=DTERNAL
        );

        assertEquals(
                //$("((X,x)&|#1)"),
                "(&|,((X,x)&&#1),(X,x),#1)",
                $("( ((X,x)&&#1) &| ((X,x)&|#1) )").toString() //differ in dt=0 and dt=DTERNAL
        );

        assertEquals(
                //$("(--,((X,x)&|#1)"),
                "((--,((X,x)&&#1))&|(--,((X,x)&|#1)))",
                $("( (--,((X,x)&&#1)) &| (--,((X,x)&|#1)) )").toString() //differ in dt=0 and dt=DTERNAL
        );
    }

    @Test
    public void testTrueDuh() throws Narsese.NarseseException {
        assertEquals("x", $.impl(Op.True, $.$("x")).toString());
    }
    @Test
    public void testTrueUnneg() {
        assertEquals(True, True.unneg());
        assertEquals(False, True.neg());
        assertEquals(True, False.unneg());
        assertEquals(True, False.neg());
    }

    @Test
    public void testCommutizeRepeatingConjunctions() throws Narsese.NarseseException {
        assertEquals("a",
                $("(a &&+1 a)").dt(DTERNAL).toString());
        assertEquals(Null,
                $("(a &&+1 --a)").dt(DTERNAL));

        assertEquals("a",
                $("(a &&+1 a)").dt(0).toString());
        assertEquals(Null,
                $("(a &&+1 --a)").dt(0));

        assertEquals("(a &&+- a)",
                $("(a &&+1 a)").dt(XTERNAL).toString());
        assertEquals("((--,a) &&+- a)",
                $("(a &&+1 --a)").dt(XTERNAL).toString());


        assertEquals(True,
                $("(a ==>+1 a)").dt(DTERNAL));
        assertEquals(Null,
                $("(--a ==>+1 a)").dt(DTERNAL));

        assertEquals(True,
                $("(a ==>+1 a)").dt(0));
        assertEquals(Null,
                $("(--a ==>+1 a)").dt(0));


        assertEquals("(a ==>+- a)",
                $("(a ==>+1 a)").dt(XTERNAL).toString());
        assertEquals("((--,a) ==>+- a)",
                $("(--a ==>+1 a)").dt(XTERNAL).toString());
    }

    @Test
    public void testImplCommonSubterms2() throws Narsese.NarseseException {
        assertEquals(True,
                $("((tetris(isRowClear,7,true)&&tetris(7,14))==>tetris(7,14))"));
        assertEquals(True,
                $("((tetris(isRowClear,7,true)==>tetris(7,14))&&tetris(7,14))"));
        assertEquals(True,
                $("((tetris(isRowClear,7,true)=|>tetris(7,14))&&tetris(7,14))"));

        assertEquals(True,
                $("((tetris(isRowClear,7,true)==>tetris(7,14))&|tetris(7,14))"));
        assertEquals(True,
                $("((tetris(isRowClear,7,true)=|>tetris(7,14))&|tetris(7,14))"));

        assertEquals(True,
                $("((tetris(isRowClear,7,true)&&tetris(7,14))=|>tetris(7,14))"));

        assertEquals("((tetris(isRowClear,7,true)&&tetris(7,14)) ==>+10 tetris(7,14))",
                $("((tetris(isRowClear,7,true)&&tetris(7,14)) ==>+10 tetris(7,14))").toString());
    }

    @Test
    public void testImplCommonSubterms3() throws Narsese.NarseseException {

        assertEquals(
                True,
                $("((x(intValue,(),0)&&x(set,0))==>x(intValue,(),0))"));
        assertEquals(
                "x(set,0)",
                $("((x(intValue,(),0)==>x(intValue,(),0)) && x(set,0))").toString());
        assertEquals(
                True,
                $("((x(set,0)==>x(intValue,(),0)) && x(intValue,(),0))"));

    }

    @Test
    public void testCoNegatedImplOK() throws Narsese.NarseseException {
        assertValid($("((--,(a)) ==>+1 (a))"));
        assertValid($("((--,a) ==>+1 a)"));
    }

//    @Test
//    public void testCoNegatedEquiOK() throws Narsese.NarseseException {
//        assertEquals("(--,((a) <=>+1 (a)))", $("((--,(a)) <=>+1 (a))").toString());
//        assertEquals("(--,(a <=>+1 a))", $("((--,a) <=>+1 a)").toString());
//    }

    @Test
    public void testRepeatEvent() throws Narsese.NarseseException {
        NAR n = NARS.shell();

        for (String x : new String[]{
                "((a) ==>+1 (a))",
                "((a) &&+1 (a))",

                /*"((a) &&+1 (a))",*/ //<-- conjunction case is special, see repeating conjunction simplification test
        }) {
            Term t = $(x);
            assertTrue(t instanceof Compound, x + " :: " + t);
            assertTrue(t.dt() != DTERNAL);

            Task y = task(t, Op.BELIEF, t(1f, 0.9f)).apply(n);

            y.term().printRecursive();
            assertEquals(x, y.term().toString());

        }


    }


    @Test
    public void testCoNegatedDifference() {
        //..
    }

    @Test
    public void testCoNegatedIntersection() {
        //..
    }

    @Test
    public void testSimEquivOfAbsoluteTrueNull1() {
        assertEquals(True, sim(True, True));
    }

    @Test
    public void testSimEquivOfAbsoluteTrueNull2() {
        assertEquals(Null, sim(Null, Null));
    }

    @Test
    public void testSimEquivOfAbsoluteTrueNull3() {
        assertEquals(Null, sim(True, Null));
    }

    @Test
    public void testSimEquivOfAbsoluteTrueNull4() {
        assertEquals(Null, sim(True.neg(), Null));
        assertEquals(False, sim(True, False));
        assertEquals(True, sim(True.neg(), False));
    }

    /**
     * conjunction and disjunction subterms which can occurr as a result
     * of variable substitution, etc which don't necessarily affect
     * the resulting truth of the compound although if the statements
     * were alone they would not form valid tasks themselves
     */
    @Test
    public void testSingularStatementsInConjunction() throws Narsese.NarseseException {
        assertEquals($("(&&,c:d,e:f)"), $("(&&,(a<->a),c:d,e:f)"));
//        assertEquals($("(&&,c:d,e:f)"), $("(&&,(a<=>a),c:d,e:f)"));
        assertEquals($("(&&,c:d,e:f)"), $("(&&,(a-->a),c:d,e:f)"));
        assertEquals($("(&&,c:d,e:f)"), $("(&&,(a==>a),c:d,e:f)"));
        assertEquals(False, $("(&&,(--,(a==>a)),c:d,e:f)"));

    }

    @Test
    public void testSingularStatementsInDisjunction() {

        assertInvalidTerms("(||,(a<->a),c:d,e:f)"); //null, singular true
    }

    @Test
    public void testSingularStatementsInDisjunction2() throws Narsese.NarseseException {
        assertEquals($("x:y"), $("(&&,(||,(a<->a),c:d,e:f),x:y)")); //double fall-thru
        assertEquals(False, $("(&&,(--,(||,(a<->a),c:d,e:f)),x:y)")); //double fall-thru

//        assertEquals($("(||,c:d,e:f)"), $("(||,(a<=>a),c:d,e:f)"));
//        assertEquals($("(||,c:d,e:f)"), $("(||,(a-->a),c:d,e:f)"));
//        assertEquals($("(||,c:d,e:f)"), $("(||,(a==>a),c:d,e:f)"));
//        assertEquals($("(||,c:d,e:f)"), $("(||,(--,(a==>a)),c:d,e:f)")); //VALID

    }

    @Test
    public void testOneArgIntersection() throws Narsese.NarseseException {
        Term x = $.p($.the("x"));
        assertEquals(x, $("(|,(x))"));
        assertEquals(x, $("(|,(x),(x))"));
        assertEquals(x, $("(&,(x))"));
        assertEquals(x, $("(&,(x),(x))"));
    }

    @Test
    public void testCoNegatedIntersectionAndDiffs() {
        assertInvalidTerms("(|,(x),(--,(x))");
        assertInvalidTerms("(&,(x),(--,(x))");
        assertInvalidTerms("(-,(x),(--,(x))");
        assertInvalidTerms("(~,(x),(--,(x))");
        assertInvalidTerms("(-,(x),(x))");
    }


    @Test
    public void testGroupNonDTemporalParallelComponents() throws Narsese.NarseseException {
        //$.76;.45;.70$ ( &&+0 ,(ball_left),(ball_right),((--,(ball_left)) &&-270 (ball_right))). :3537: %.64;.15%
        //$.39;.44;.70$ (((--,(ball_left)) &&-233 (ball_right)) &&-1 ((ball_left) &&+0 (ball_right))). :3243: %.53;.23%
        Term c1 = $("((--,(ball_left)) &&-270 (ball_right)))");

        assertEquals("((ball_right) &&+270 (--,(ball_left)))", c1.toString());
        assertEquals(
                "(((ball_right)&|(ball_left)) &&+270 (--,(ball_left)))", //ball_right subsumed by the sequence

                parallel($("(ball_left)"), $("(ball_right)"), c1)
                        .toString());

    }

    @Test
    public void testReducibleImplFactored() throws Narsese.NarseseException {
        assertEquals("((x&|y)=|>z)",
                $("((y &| x) =|> (y &| z))").toString()
        );
        assertEquals("((x&|y)==>z)",
                $("((y &| x) ==> (y &| z))").toString()
        );
    }

    @Test
    public void testReducibleImplFactored2() throws Narsese.NarseseException {
        assertEquals("((x&&y)==>z)",
                $("((y && x) ==> (y && z))").toString()
        );
        assertEquals("((&&,a,x,y)==>z)",
                $("((&&, x, y, a) ==> (y && z))").toString()
        );
        assertEquals("((y &&+1 x)=|>(z &&+1 y))", //not reducible
                $("((y &&+1 x)=|>(z &&+1 y))").toString()
        );
        assertEquals("((x&&y) ==>+1 (y&&z))",//not reducible
                $("((y&&x) ==>+1 (y&&z))").toString()
        );

    }

    @Test
    public void testReducibleImpl() throws Narsese.NarseseException {

        assertEquals("(--,((--,x)==>y))", $("(--x ==> (--y && --x))").toString());

        assertEquals("(x=|>y)",
                $("(x ==>+0 (y &| x))").toString()
        );
        assertEquals(True,
                $("((y &| x) =|> x)")//.toString()
        );
        assertEquals("(--,((--,$1)=|>#2))",
                $("((--,$1)=|>((--,$1)&|(--,#2)))").toString()
        );
    }

    @Test
    public void testReducibleImplConjCoNeg() throws Narsese.NarseseException {
        for (String i : new String[]{"==>", "=|>"}) {
            for (String c : new String[]{"&&", "&|"}) {
                assertEquals(Null,
                        $("(x " + i + " (y " + c + " --x))")
                );
                assertEquals(Null,
                        $("(--x " + i + " (y " + c + " x))")
                );
                assertEquals(Null,
                        $("((y " + c + " --x) " + i + " x)")
                );
                assertEquals(False,  //this case reduces to negative of the outer term, so it becomes True
                        $("((y " + c + " x) " + i + " --x)")
                );
            }
        }
    }

    @Test
    public void testReducibleImplParallelNeg() throws Narsese.NarseseException {
        assertEquals("(--,((--,x)=|>y))",
                $("(--x =|> (--y &| --x))").toString()
        );
        assertEquals(Null,
                $("((--y &| --x) =|> --x)")//.toString()
        );

    }

    @Test
    public void testInvalidCircularImpl() throws Narsese.NarseseException {
        assertNotEquals(Null, $("(x(intValue,(),1) ==>+10 ((--,x(intValue,(),0)) &| x(intValue,(),1)))"));
        assertEquals("(--,(x(intValue,(),1)=|>x(intValue,(),0)))",
                $("(x(intValue,(),1) =|> ((--,x(intValue,(),0)) &| x(intValue,(),1)))").toString());
        assertEquals("(--,(x(intValue,(),1)==>x(intValue,(),0)))",
                $("(x(intValue,(),1) ==> ((--,x(intValue,(),0)) &| x(intValue,(),1)))").toString());
    }

    @Test
    public void testImplInImplDTernal() throws Narsese.NarseseException {
        assertEquals(
                "(((--,(in))&&(happy))==>(out))",
                $("((--,(in)) ==> ((happy)  ==> (out)))").toString());
    }

    @Test
    public void testImplInImplDTemporal() throws Narsese.NarseseException {
        assertEquals(
                "(((--,(in)) &&+1 (happy)) ==>+2 (out))",
                $("((--,(in)) ==>+1 ((happy) ==>+2 (out)))").toString());
    }


    @Test
    public void testConjunctiveCoNegationAcrossImpl() {
        //((--,(&&,(--,(pad_top)),(pad_bottom),(pad_top))) ==>+133 (--,(pad_bottom)))! :4355: %.73;.24%

        /*
        (
            (&&,(--,(23)),(--,(31)),(23),(31))
                <=>
            (&&,(--,(23)),(--,(31)),(23),(31),((--,(31)) &&+98 (23)))) (class nars.term.compound.GenericCompound): Failed atemporalization, becoming: ¿".
        ((&&,(--,(2,3)),(--,(3,1)),(2,3),(3,1))<=>(&&,(--,(2,3)),(--,(3,1)),(2,3),(3,1),((--,(3,1)) &&+98 (2,3)))) (class nars.term.compound.GenericCompound): Failed atemporalization, becoming: ¿".
        ((&&,(--,(0,2)),(--,(2,0)),((((--,(0,2)) &&+428 (--,(2,0))) ==>+1005 (--,(2,0))) &&+0 ((--,(2,0)) <=>-1005 ((--,(0,2)) &&+428 (--,(2,0))))))<=>(&&,(--,(0,2)),((--,(0,2)) &&-395 (--,(2,0))),((((--,(0,2)) &&+428 (--,(2,0))) ==>+1005 (--,(2,0))) &&+0 ((--,(2,0)) <=>-1005 ((--,(0,2)) &&+428 (--,(2,0))))))) (class nars.term.compound.GenericCompound): Failed atemporalization, becoming: ¿".
        temporal conjunction requires exactly 2 arguments {&&, dt=-125, args=[(1,4), ((&&,(--,(1,4)),(--,(2,4)),(2,4)) ==>+125 (--,(1,4))), ((&&,(--,(1,4)),(--,(2,4)),(1,4),(2,4)) ==>+125 (--,(1,4)))]}
            temporalizing from (&&,(1,4),((&&,(--,(1,4)),(--,(2,4)),(2,4)) ==>+125 (--,(1,4))),((&&,(--,(1,4)),(--,(2,4)),(1,4),(2,4)) ==>+125 (--,(1,4))))
            deriving rule <(P ==> M), (S ==> M), neq(S,P), time(dtBminT) |- (S ==> P), (Belief:Induction, Derive:AllowBackward)>".
        */


    }


    @Test
    public void testConjDisjNeg() throws Narsese.NarseseException {
        assertEquals(
                "((--,(out))&&(happy))",
                $("((--,(out))&&(||,(happy),(out)))").toString());
    }

    @Test
    public void taskWithFlattenedConunctions() throws Narsese.NarseseException {
        //$0.0;NaN$ ((busyVol)&&((busyPri)&&(busyVol))). %.19;.10%  //<-- should not be allowed to be constructed
        //  instead should have been: (busyVol&&busyPri)

        @NotNull Term x = $("((hear(what)&&(hear(is)&&(hear(is)&&(hear(what)&&(hear(is)&&(hear(is)&&(hear(what)&&(hear(is)&&(hear(is)&&(hear(is)&&hear(what))))))))))) ==>+153 hear(is)).");
        assertEquals("((hear(what)&&hear(is)) ==>+153 hear(is))", x.toString());

    }

//    @Test
//    public void reduceComplex() throws Narsese.NarseseException {
//        String s = "(((x) &&+2 (y)) <=>+8236 ((--,(x)) &&+3 (--,((--,(x)) &&+0 ((x)&&((y)&&((x)&&(y))))))))";
//
//        Term t = $(s);
//
//        assertEquals(
//                //"(((x) &&+2 (y)) <=>+8236 ((--,(x)) &&+3 (--,((--,(x)) &&+0 ((x)&&(y))))))",
//                //"(--,(((x) &&+2 (y)) <=>+8236 (x)))", //TODO check this reduction
//                False,
//                t);
//    }

    @Test
    public void testDontPromoteEternalToParallel() throws Narsese.NarseseException {
        String s = "(a&|(b && c))";
        assertEquals(
                //"(&|,a,b,c)",
                "((b&&c)&|a)",
                $(s).toString());
    }

    @Test
    public void testPromoteEternalToParallelDont() throws Narsese.NarseseException {
        String s = "(a&&(b&|c))";
        assertEquals(
                "((b&|c)&&a)",
                $(s).toString()
        );
    }

    @Test
    public void testCoNegatedConjunctionParallelEternal() throws Narsese.NarseseException {
        //mix of parallel and eternal
        assertEquals(
                //False,
                "(((--,(y&&z))&&x)&|(--,x))",
                $("((--,x)&|((--,(z&&y))&&x))").toString()
        );
    }


    /**
     * TODO decide if it should not apply this reduction to eternal
     */
    @Test
    public void testConjImplReduction0() throws Narsese.NarseseException {
        assertEquals(
                "((inside(john,playground)&&inside(bob,office))==>inside(bob,kitchen))",
                $("(inside(bob,office) && (inside(john,playground)==>inside(bob,kitchen)))").toString()
        );
    }

    @Test
    public void testConjImplReduction() throws Narsese.NarseseException {
        Term a = $("((a,b) ==>+1 (b,c))");
        Term b = $("(c,d)");
        Term x = Op.CONJ.the(4, a, b);

        assertEquals("(((a,b) &&+4 (c,d)) ==>-3 (b,c))",
                x.toString());
    }

    @Test
    public void testConjImplReductionNegConj() throws Narsese.NarseseException {
        Term a = $("((a,b) ==>+1 (b,c))");
        Term b = $("(c,d)");
        Term x = Op.CONJ.the(-4, a, b);

        assertEquals("(((c,d) &&+4 (a,b)) ==>+1 (b,c))", x.toString());
    }

    @Test
    public void testConjImplReductionNegConj2() throws Narsese.NarseseException { //tests for same result as testConjImplReductionNegConj
        Term b = $("(c,d)");
        Term a = $("((a,b) ==>+1 (b,c))");
        Term x = Op.CONJ.the(4, b, a);

        assertEquals("(((c,d) &&+4 (a,b)) ==>+1 (b,c))", x.toString());
    }

    @Test
    public void testConjImplReductionNegConj2b() throws Narsese.NarseseException {
        Term b = $("(c,d)");
        Term a = $("((a,b) ==>-1 (b,c))");
        Term x = Op.CONJ.the(4, b, a);

        assertEquals("(((c,d) &&+4 (a,b)) ==>-1 (b,c))", x.toString());
    }

    @Test
    public void testConjImplReductionNegImpl() throws Narsese.NarseseException {
        Term a = $("((a,b) ==>-1 (b,c))");
        Term b = $("(c,d)");
        Term x = Op.CONJ.the(4, a, b);

        assertEquals("(((a,b) &&+4 (c,d)) ==>-5 (b,c))", x.toString());
    }

    @Test
    public void testConjImplReductionWithVars() throws Narsese.NarseseException {
        Term a = $("((a,#1) ==>+1 (#1,c))");
        Term b = $("(c,d)");
        Term x = Op.CONJ.the(4, a, b);

        assertEquals("(((a,#1) &&+4 (c,d)) ==>-3 (#1,c))",
                x.toString());
    }

    @Test
    public void testConjImplReduction1() throws Narsese.NarseseException {
        assertEquals(
                "((inside(john,playground)&|inside(bob,office))==>inside(bob,kitchen))",
                $("(inside(bob,office)&|(inside(john,playground)==>inside(bob,kitchen)))").toString()
        );
    }

    @Test
    public void testConjImplReduction2() throws Narsese.NarseseException {
        //with some dt's

        Term t = $("(inside(bob,office) &&+1 (inside(john,playground) ==>+1 inside(bob,kitchen)))");

        assertEquals(
                "((inside(bob,office) &&+1 inside(john,playground)) ==>+1 inside(bob,kitchen))",
                t.toString()
        );
        assertEquals(0, t.dtRange());
    }

    @Test
    public void testConjImplReductionNeg2() throws Narsese.NarseseException {
        //with some dt's
        assertEquals(
                "((inside(bob,office) &&+1 (--,inside(john,playground))) ==>+1 inside(bob,kitchen))",
                $("(inside(bob,office) &&+1 (--inside(john,playground) ==>+1 inside(bob,kitchen)))").toString()
        );
    }

    @Test
    public void testConjImplReduction3() throws Narsese.NarseseException {
        //with some dt's
        assertEquals(
                "((j &&+1 b) ==>-2 k)",
                $("((j ==>-1 k) &&+1 b)").toString()
        );

        assertEquals(
                "((j &&+1 b) ==>-2 k)",
                $("(b &&-1 (j ==>-1 k))").toString()
        );
    }


//        @Test public void testImageUnwrap0() throws Narsese.NarseseException {
//        assertEquals("(a,b)",
//           p(imageUnwrap($("(\\,n,_,b)"), $("a"))).toString());
//    }
//    @Test public void testImageUnwrap1() throws Narsese.NarseseException {
//        assertEquals("(a,b)",
//            p(imageUnwrap($("(\\,n,a,_)"), $("b"))).toString());
//    }
//    @Test public void testImageUnwrap2() throws Narsese.NarseseException {
//        assertEquals("(a,b)",
//            p(imageUnwrap($("(/,n,_,b)"), $("a"))).toString());
//    }
//    @Test public void testImageUnwrap3() throws Narsese.NarseseException {
//        assertEquals(     "(a,b)",
//           p(imageUnwrap($("(/,n,a,_)"), $("b"))).toString());
//    }
//
//    @Test public void testImageInSubtermsProductNormalFormIntensional() throws Narsese.NarseseException {
//
//        //<neutralization --> (acid,base)>" //en("Neutralization is a relation between an acid and a base. ");
//        //  <(\,neutralization,acid,_) --> base> //en("Something that can be neutralized by an acid is a base.");
//        //  <(\,neutralization,_,base) --> acid> //en("Something that can neutralize a base is an acid.");
//        assertEquals(
//                "((x)==>(n-->(a,b)))",
//                $("((x)==>((\\,n,a,_)-->b))").toString());
//        assertEquals(
//                "((x)==>(n-->(a,b)))",
//                $("((x)==>((\\,n,_,b)-->a))").toString());
//    }
//
//    @Test public void testImageInSubtermsProductNormalFormExtensional() throws Narsese.NarseseException {
//        //<(acid,base) --> reaction> //en("An acid and a base can have a reaction.");
//        //  <base --> (/,reaction,acid,_)> //en("A base is something that has a reaction with an acid.");
//        //  <acid --> (/,reaction,_,base)> //en("Acid can react with base.");
//        assertEquals(
//                  "{r(a,b)}",
//                $("{(b-->(/,r,a,_))}").toString());
//
//        assertEquals(
//                  "{likes(cat,{sky})}",
//                $("{({sky}-->(/,likes,cat,_))}").toString());
//
//        assertEquals(
//                "(--,r(a,b))",
//                $("(--,(a-->(/,r,_,b)))").toString());
//
//    }
//    @Test public void testNegatedImageInSubtermsProductNormalForm() throws Narsese.NarseseException {
//        assertEquals(
//                  "{(--,r(a,b)),(z-->(x,y))}",
//                $("{ (--,(b-->(/,r,a,_))), ((\\,z,x,_)-->y) }").toString());
//
//    }

}
