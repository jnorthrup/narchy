package nars.term;

import jcog.list.FasterList;
import nars.*;
import nars.io.NarseseTest;
import nars.task.util.InvalidTaskException;
import nars.term.atom.Atomic;
import nars.term.compound.util.Conj;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.*;
import static nars.Op.*;
import static nars.term.TermTest.assertValid;
import static nars.term.TermTest.assertValidTermValidConceptInvalidTaskContent;
import static nars.time.Tense.*;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 12/10/15.
 */
public class TermReductionsTest extends NarseseTest {

    @Nullable
    final static Term p = Atomic.the("P"), q = Atomic.the("Q"), r = Atomic.the("R"), s = Atomic.the("S");


    @Test
    public void testIntersectExtReduction1() {
        
        assertEquals("(&,P,Q,R)", SECTe.the(r, SECTe.the(p, q)).toString());
        assertReduction("(&,P,Q,R)", "(&,R,(&,P,Q))");
    }

    @Test
    public void testIntersectExtReduction2() {
        
        assertEquals("(&,P,Q,R,S)", SECTe.the(SECTe.the(p, q), SECTe.the(r, s)).toString());
        assertReduction("(&,P,Q,R,S)", "(&,(&,P,Q),(&,R,S))");
    }

    @Test
    public void testIntersectExtReduction3() {
        
        assertReduction("(&,P,Q,R,S,T,U)", "(&,(&,P,Q),(&,R,S), (&,T,U))");
    }

    @Test
    public void testIntersectExtReduction2_1() {
        
        assertReduction("(&,P,Q,R)", "(&,R,(&,P,Q))");
    }

    @Test
    public void testIntersectExtReduction4() {
        
        assertEquals("{P,Q,R,S}", SECTe.the(SETe.the(p, q), SETe.the(r, s)).toString());
        assertReduction("{P,Q,R,S}", "(&,{P,Q},{R,S})");
    }

    @Test
    public void testIntersectExtReduction5() {
        assertEquals(Null /* emptyset */, SECTe.the(SETi.the(p, q), SETi.the(r, s)));
    }

    @Test
    public void testIntersectIntReduction1() {
        
        assertEquals("(|,P,Q,R)", SECTi.the(r, SECTi.the(p, q)).toString());
        assertReduction("(|,P,Q,R)", "(|,R,(|,P,Q))");
    } 

    @Test
    public void testIntersectIntReduction2() {
        
        assertEquals("(|,P,Q,R,S)", SECTi.the(SECTi.the(p, q), SECTi.the(r, s)).toString());
        assertReduction("(|,P,Q,R,S)", "(|,(|,P,Q),(|,R,S))");
    }

    @Test
    public void testIntersectIntReduction3() {
        
        assertReduction("(|,P,Q,R)", "(|,R,(|,P,Q))");
    }

    @Test
    public void testIntersectIntReduction4() {
        
        assertEquals("[P,Q,R,S]", SECTi.the(SETi.the(p, q), SETi.the(r, s)).toString());
        assertReduction("[P,Q,R,S]", "(|,[P,Q],[R,S])");

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
    public void testIntersectIntReduction_to_one() {
        assertReduction("(robin-->bird)", "<robin-->(|,bird)>");
        assertReduction("(robin-->bird)", "<(|,robin)-->(|,bird)>");
    }

    @Test
    public void testFunctionRecursion() throws Narsese.NarseseException {
        
        assertTrue($("task((polarize(%1,task) ==>+- polarize(%2,belief)))").subs() > 0);
    }











    @Test
    public void testSimilarityNegatedSubtermsDoubleNeg() {
        assertReduction("((--,(P))<->(--,(Q)))", "((--,(P))<->(--,(Q)))");
        /*
        <patham9> <-> is a relation in meaning not in truth
        <patham9> so negation can't enforce any equivalence here
        */
    }

    @Test
    public void testSimilarityNegatedSubterms() {
        assertReduction("((--,(Q))<->(P))", "((P)<->(--,(Q)))");
        assertReduction("((--,(P))<->(Q))", "((--,(P))<->(Q))");
    }










    @Test
    public void testImplicationTrue2() {
        assertReduction(True, "((&&,x1,$1) ==> $1)");
    }

    @Test
    public void testImplicationNegatedPredicate() {
        assertReduction("(--,((P)==>(Q)))", "((P)==>(--,(Q)))");
        assertReduction("((--,(P))==>(Q))", "((--,(P))==>(Q))");
    }

    @Test
    public void testConjInhReflexive() {
        assertReduction("((a &&+5 x)-->a)", "((a &&+5 x)-->a)");
        assertReduction("(x-->(a &&+5 x))", "(x-->(a &&+5 x))");
        assertReduction("((a&&b)-->(a&&c))", "((a&&b)-->(a&&c))");
    }

    @Test void testConjParallelsMixture() {
        
        assertReduction(False, "(((b &&+4 a)&|(--,b))&|((--,c) &&+6 a))");

        assertReduction("((&|,a,b2,b3) &&+1 (c&|b1))",
                "(((a &&+1 b1)&|b2)&|(b3 &&+1 c))");
        assertReduction("((a &&+1 (b1&|b2)) &&+1 c)", "((a &&+1 (b1&|b2)) &&+1 c)");



    }
    @Test public void testConjParallelWithNegMix() {
        String x = "((--,(x &| y)) &| (--,y))";
        assertEquals($$(x).toString(), $$($$(x).toString()).toString());

        assertReduction("(--,y)",
                x);

        assertReduction("(--,y)",
                "((--,(x&|y))&|(--,y))");

        
        assertEquals("((--,(x&|y)) &&+1 (--,y))", $$("((--,(x &| y)) &&+1 (--,y))").toString());
        assertEquals("((--,(x &&+1 y))&|(--,y))", $$("((--,(x &&+1 y)) &| (--,y))").toString());
    }

    @Test
    public void implSubjSimultaneousWithTemporalPred() {
        Term x = $$("((--,(tetris-->happy))=|>(tetris(isRow,(2,true),true) &&+5 (tetris-->happy)))");
        assertEquals(
                "((--,(tetris-->happy))=|>(tetris(isRow,(2,true),true) &&+5 (tetris-->happy)))",
                x.toString());
    }

    @Test
    public void testPointlessImplicationSubtermRepeat() {
        assertReduction("((a &&+5 x) ==>+5 c)", "((a &&+5 x)=|>(x &&+5 c))");

        assertReduction(True, "((a &&+5 x)=|>x)");

        assertReduction("((a &&+5 $1) ==>+5 c)", "((a &&+5 $1)=|>($1 &&+5 c))");

        assertReduction(True, "((a &&+5 $1) ==>-5 a)");

    }

    @Test
    public void testPointlessImplicationSubtermRepeat2() {
        
        
        assertReduction("((a &&+5 x)=|>(y&&z))", "((a &&+5 x)=|>(&&,x,y,z))");

        
        assertReduction("((a &&+5 x)=|>(y&|z))", "((a &&+5 x)=|>((x&|y)&|z)))");

    }


    @Test
    public void testImplicationNegatedPredicateImplicated() {

        
        assertReduction(Null, "((--,(x==>y)) ==> z)");

        
        assertReduction("(--,((x&&y)==>z))", "(x ==> (--,(y==>z)))");
    }







    @Test
    public void testReducedAndInvalidImplications5() {

        assertInvalidTerms("((P==>Q) ==> R)");
    }







    @Test
    public void testConjPosNegElimination1() throws Narsese.NarseseException {
        
        
        
        assertEquals("((--,b)&&a)", $.$("(a && --(a && b))").toString());
    }

    @Test
    public void testConjPosNegElimination2() throws Narsese.NarseseException {
        
        
        
        assertEquals("((--,a)&&b)", $.$("(--a && (||,a,b))").toString());
    }

    @Test
    public void testReducedAndInvalidImplications2() {
        assertReduction("((P&&R)==>Q)", "(R==>(P==>Q))");
        assertReduction("((R &&+2 P) ==>+1 Q)", "(R ==>+2 (P ==>+1 Q))");
        assertReduction("(((S &&+1 R) &&+2 P) ==>+1 Q)", "((S &&+1 R) ==>+2 (P ==>+1 Q))");
    }


    @Test
    public void testConjParallelConceptualShouldntBeXTERNAL() throws Narsese.NarseseException {

        assertEquals(1, $("(&&,a,b,c)").eventCount());
        assertEquals(3, $("(&|,a,b,c)").eventCount());
        assertEquals(3, CONJ.compound(XTERNAL, new Term[]{$("a"), $("b"), $("c")}).eventCount());

        for (int dt : new int[]{ /*XTERNAL,*/ 0, DTERNAL}) {
            assertEquals("(&&,a,b,c)",
                    CONJ.compound(dt, new Term[]{$.$("a"), $.$("b"), $.$("c")}).concept().toString(), ()->"dt=" + dt);
        }

        
        

        assertEquals(
                "(&&,(bx-->noid),(happy-->noid),#1)",
                $("(--,(((bx-->noid) &| (happy-->noid)) &| #1))")
                        .concept().toString());
        assertEquals(
                "(x,(--,(&&,a,b,c)))",
                $("(x,(--,(( a &| b) &| c)))")
                        .concept().toString());
    }


    @Test
    public void testConjRepeatPosNeg() {
        Term x = $.the("x");
        assertEquals(+1, CONJ.compound(-1, new Term[]{x, x}).dt());
        assertEquals(+1, CONJ.compound(+1, new Term[]{x, x}).dt());
        assertArrayEquals(IO.termToBytes(CONJ.compound(+32, new Term[]{x, x})), IO.termToBytes(CONJ.compound(-32, new Term[]{x, x})));
        assertEquals(+1, CONJ.compound(XTERNAL, new Term[]{x, x}).dt(-1).dt());
        assertEquals(+1, CONJ.compound(XTERNAL, new Term[]{x, x}).dt(+1).dt());
        assertEquals(CONJ.compound(-1, new Term[]{x, x}), CONJ.compound(+1, new Term[]{x, x}));
        assertEquals(CONJ.compound(XTERNAL, new Term[]{x, x}).dt(-1), CONJ.compound(XTERNAL, new Term[]{x, x}).dt(+1));
    }

    @Test
    public void testConjEvents1a() throws Narsese.NarseseException {
        assertEquals(
                "(a &&+16 ((--,a)&|b))",
                Conj.conj(
                        new FasterList<LongObjectPair<Term>>(new LongObjectPair[]{
                                pair(298L, $.$("a")),
                                pair(314L, $.$("b")),
                                pair(314L, $.$("(--,a)"))})
                ).toString()
        );
    }
    @Test
    public void testConjEvents1b() throws Narsese.NarseseException {
        assertEquals(
                "((a&|b) &&+1 (--,a))",
                Conj.conj(
                        new FasterList<LongObjectPair<Term>>(new LongObjectPair[]{
                                pair(1L, $.$("a")),
                                pair(1L, $.$("b")),
                                pair(2L, $.$("(--,a)"))})
                ).toString()
        );
    }
    @Test
    public void testConjEvents2() throws Narsese.NarseseException {
        assertEquals(
                "((a &&+1 (&|,b1,b2,b3)) &&+1 (c &&+1 (d1&|d2)))",
                Conj.conj(
                        new FasterList<LongObjectPair<Term>>(new LongObjectPair[]{
                                pair(1L, $.$("a")),
                                pair(2L, $.$("b1")),
                                pair(2L, $.$("b2")),
                                pair(2L, $.$("b3")),
                                pair(3L, $.$("c")),
                                pair(4L, $.$("d1")),
                                pair(4L, $.$("d2")),
                                pair(5L, True /* ignored */)
                        })).toString());
    }

    @Test
    public void testConjEventsWithFalse() throws Narsese.NarseseException {
        assertEquals(
                False,
                Conj.conj(
                        new FasterList<LongObjectPair<Term>>(new LongObjectPair[]{
                                pair(1L, $.$("a")),
                                pair(2L, $.$("b1")),
                                pair(2L, False)
                        })));
        assertEquals(
                False,
                Conj.conj(
                        new FasterList<LongObjectPair<Term>>(new LongObjectPair[]{
                                pair(1L, $.$("a")),
                                pair(1L, $.$("--a"))
                        })));
    }

    @Test
    public void testReducedAndInvalidImplications3() {
        assertInvalidTerms("<R==><P==>R>>");
    }

    @Test
    public void testReducedAndInvalidImplications4() {
        assertReduction("(R==>P)", "(R==>(R==>P))");
    }




















    
        /*
            (&,(&,P,Q),R) = (&,P,Q,R)
            (&,(&,P,Q),(&,R,S)) = (&,P,Q,R,S)

            
            if (term1.op(Op.SET_INT) && term2.op(Op.SET_INT)) {

            
            if (term1.op(Op.SET_EXT) && term2.op(Op.SET_EXT)) {

         */

    @Test
    public void testDisjunctEqual() {
        @NotNull Term pp = p(p);
        assertEquals(pp, disj(pp, pp));
    }

    @Test
    public void testDisjReduction1() {
        
        Term x = $.the("x");
        assertEquals(x, $.disj(x, x));
        assertEquals(x, CONJ.compound(DTERNAL, new Term[]{x.neg(), x.neg()}).neg());
    }

    @Disabled
    @Test
    public void testRepeatConjunctionTaskSimplification() throws Narsese.NarseseException {
        
        assertEquals(
                "$.50 (x). 0⋈10 %1.0;.90%",
                Narsese.the().task("((x) &&+10 (x)). :|:", NARS.shell()).toString());
    }

    @Test
    public void testConjParallelWithSeq() {
        assertReduction("(a &&+5 b)", "((a &&+5 b)&|a)");

        assertReduction(False, "((--a &&+5 b)&|a)");
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
        
        assertEquals(3, normal.dtRange());
        assertEquals(ns, normal.toString());

        for (String unnormalized : new String[]{
                "(a &&+1 (b &&+1 (c &&+1 d)))", 
                "(((a &&+1 b) &&+1 c) &&+1 d)"  
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
        String d = "(((d) &&+3 (a)) &&+1 (b))"; 

        String c = "((d) &&+3 ((a) &&+1 (b)))"; 
        Term cc = $(c);
        assertEquals(d, cc.toString());

        String a = "(((a) &&+1 (b)) &&-3 (d))"; 
        Term aa = $(a);
        assertEquals(d, aa.toString());






        
        assertTrue(aa.sub(0).subs() > aa.sub(1).subs());
        assertTrue(cc.sub(0).subs() > cc.sub(1).subs());

    }

    @Test
    public void testEmbeddedConjNormalizationB() {
        assertReduction(
                "(((--,noid(0,5)) &&+- noid(11,2)) &&+- (noid(11,2) &&+- noid(11,2)))",
                "((((--,noid(0,5)) &&+- noid(11,2)) &&+- noid(11,2)) &&+- noid(11,2))");
    }

    @Test
    public void testEmbeddedConjNormalization2() {
        assertReduction("((a &&+1 b) &&+3 (c &&+5 d))", "(a &&+1 (b &&+3 (c &&+5 d)))");

        assertReduction("(((t2-->hold) &&+1 (t1-->at)) &&+3 ((t1-->[opened]) &&+5 open(t1)))", "(hold:t2 &&+1 (at:t1 &&+3 ([opened]:t1 &&+5 open(t1))))");
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
        Term ab = Conj.conjMerge(a, 1, b, 6);
        assertEquals("((a &&+5 ((--,a)&|b)) &&+5 ((--,b) &&+5 (--,c)))", ab.toString());
    }


    @Test
    public void testConjunctionEqual() {
        assertEquals(p, CONJ.the(p, p));
    }

    @Test
    public void testConjunctionNormal() throws Narsese.NarseseException {
        Term x = $("(&&, <#1 --> lock>, <#1 --> (/, open, #2, _)>, <#2 --> key>)");
        assertEquals(3, x.subs());
        assertEquals(CONJ, x.op());
    }

    @Test
    public void testIntExtEqual() {
        assertEquals(p, SECTe.the(p, p));
        assertEquals(p, SECTi.the(p, p));
    }

    @Test
    public void testDiffIntEqual() {

        assertEquals(False, DIFFi.the(p, p));
    }

    @Test
    public void testDiffExtEqual() {

        assertEquals(False, DIFFe.the(p, p));
    }


    @Test
    public void testDifferenceSorted() {




        
        assertArrayEquals(
                new Term[]{r, s},
                ((Compound) Op.differenceSet(Op.SETe, SETe.the(r, p, q, s), SETe.the(p, q))).arrayClone()
        );
    }

    @Test
    public void testDifferenceSortedEmpty() {




        
        assertEquals(
                Null,
                Op.differenceSet(Op.SETe, SETe.the(p, q), SETe.the(p, q))
        );
    }


    @Test
    public void testDifference() throws Narsese.NarseseException {


        assertEquals(
                $("{Mars,Venus}"),
                Op.differenceSet(Op.SETe, $("{Mars,Pluto,Venus}"), $.<Compound>$("{Pluto,Saturn}"))
        );
        assertEquals(
                $("{Saturn}"),
                Op.differenceSet(Op.SETe, $("{Pluto,Saturn}"), $.<Compound>$("{Mars,Pluto,Venus}"))
        );





















    }


    @Test
    public void testDifferenceImmediate() throws Narsese.NarseseException {

        Term d = DIFFi.the(SETi.the($("a"), $("b"), $("c")), SETi.the($("d"), $("b")));
        assertEquals(Op.SETi, d.op());
        assertEquals(2, d.subs());
        assertEquals("[a,c]", d.toString());
    }

    @Test
    public void testDifferenceImmediate2() throws Narsese.NarseseException {


        Term a = SETe.the($("a"), $("b"), $("c"));
        Term b = SETe.the($("d"), $("b"));
        Term d = DIFFe.the(a, b);
        assertEquals(Op.SETe, d.op());
        assertEquals(2, d.subs());
        assertEquals("{a,c}", d.toString());

    }

    @Test
    public void testDisjunctionReduction() {

        assertReduction("(||,(a-->x),(b-->x),(c-->x),(d-->x))", "(||,(||,x:a,x:b),(||,x:c,x:d))");
        assertReduction("(||,(b-->x),(c-->x),(d-->x))", "(||,x:b,(||,x:c,x:d))");
    }

    @Test
    public void testConjunctionReduction() {
        assertReduction("(&&,a,b,c,d)", "(&&,(&&,a,b),(&&,c,d))");
        assertReduction("(&&,b,c,d)", "(&&,b,(&&,c,d))");
    }

    @Test
    public void testTemporalConjunctionReduction1() throws Narsese.NarseseException {
        assertReduction("(a&|b)", "(a &&+0 b)");
        assertEquals(
                $("((--,(ball_left)) &&-270 (ball_right))"),
                $("((ball_right) &&+270 (--,(ball_left)))"));

    }

    @Test
    public void testConjunctionParallelWithConjunctionParallel() {
        assertReduction("(&|,nario(13,27),nario(21,27),nario(24,27))", "((nario(21,27)&|nario(24,27))&|nario(13,27))");
    }

    @Test
    public void testTemporalConjunctionReduction2() {
        assertReduction("((a&|b) &&+1 c)", "(a &&+0 (b &&+1 c))");
    }

    @Test
    public void testTemporalConjunctionReduction3() {
        assertReduction("(a&|b)", "( (a &&+0 b) && (a &&+0 b) )");
    }

    @Test
    public void testTemporalConjunctionReduction5() {
        assertReduction("((a&|b)&&(a &&+1 b))", "( (a&|b) && (a &&+1 b) )");
    }

    @Test
    public void testTemporalConjunctionReduction4() {
        assertReduction("(a&|b)", "( a &&+0 (b && b) )");
    }


    @Test
    public void testTemporalNTermConjunctionParallel() {
        
        
        assertReduction("(&|,a,b,c)", "( a &&+0 (b &&+0 c) )");
    }

    @Disabled
    @Test
    public void testTemporalNTermEquivalenceParallel() {
        
        assertReduction("(<|>, a, b, c)", "( a <|> (b <|> c) )");
    }


    @Test
    public void testMultireduction() {
        
    }

    @Test
    public void testConjunctionMultipleAndEmbedded() {

        assertReduction("(&&,a,b,c,d)", "(&&,(&&,a,b),(&&,c,d))");
        assertReduction("(&&,a,b,c,d,e,f)", "(&&,(&&,a,b),(&&,c,d), (&&, e, f))");
        assertReduction("(&&,a,b,c,d,e,f,g,h)", "(&&,(&&,a,b, (&&, g, h)),(&&,c,d), (&&, e, f))");
    }

    @Test
    public void testConjunctionEquality() throws Narsese.NarseseException {

        assertEquals(
                $("(&&,r,s)"),
                $("(&&,s,r)"));







    }

    @Test
    public void testImplicationTrue() {
        assertReduction(False, "(--x==>x)");
        assertReduction(True, "(x==>x)");
        assertReduction(True, "((x)==>(x))");
        assertReduction(False, "(--(x)==>(x))");
    }

    @Test
    public void testImplicationInequality() throws Narsese.NarseseException {

        assertNotEquals(
                $("<r ==> s>"),
                $("<s ==> r>"));










    }

    @Test
    public void testDisjunctionMultipleAndEmbedded() {

        assertReduction("(||,(a),(b),(c),(d))", "(||,(||,(a),(b)),(||,(c),(d)))");
        assertReduction("(||,(a),(b),(c),(d),(e),(f))", "(||,(||,(a),(b)),(||,(c),(d)), (||,(e),(f)))");
        assertReduction("(||,(a),(b),(c),(d),(e),(f),(g),(h))", "(||,(||,(a),(b), (||,(g),(h))),(||,(c),(d)), (||,(e),(f)))");

    }

    @Test
    public void testImplicationConjCommonSubterms() {
        assertReduction("((&&,a,b,c)==>d)", "((&&, a, b, c) ==> (&&, a, d))");
        assertReduction("((a&&d)==>(b&&c))", "((&&, a, d) ==> (&&, a, b, c))");
        assertInvalidTerms("((&&, a, b, c) ==> (&&, a, b))");
        assertReduction("((a&&b)==>c)", "((&&, a, b) ==> (&&, a, b, c))");
        assertReduction(True, "((&&, a, b, c) ==> a)");

        assertReduction("(a==>(b&&c))", "(a ==> (&&, a, b, c))");
    }

    @Test
    public void testConjPosNeg() throws Narsese.NarseseException {
        
        
        assertEquals(False, $.$("(x && --x)"));
        assertEquals(True, $.$("--(x && --x)"));
        assertEquals(True, $.$("(||, x, --x)"));

        assertEquals("y", $.$("(y && --(&&,x,--x))").toString());
    }

    @Test
    public void testTrueFalseInXternal() {
        for (int i : new int[]{XTERNAL, 0, DTERNAL}) {
            assertEquals("x", CONJ.compound(i, new Term[]{$.the("x"), True}).toString());
            assertEquals(False, CONJ.compound(i, new Term[]{$.the("x"), False}));
            assertEquals(Null, CONJ.compound(i, new Term[]{$.the("x"), Null}));
        }
    }

    @Test
    public void testConegatedConjunctionTerms0() throws Narsese.NarseseException {
        assertReduction("((--,#1) &&+- #1)", "(#1 &&+- (--,#1))");
        assertReduction("(#1 &&+1 (--,#1))", "(#1 &&+1 (--,#1))");
        assertReduction(False, "(#1 && (--,#1))");
        assertReduction(False, "(#1 &| (--,#1))");
        assertEquals(False, parallel(varDep(1), varDep(1).neg()));

        assertReduction(False, "(&&, #1, (--,#1), (x))");
        assertReduction(False, "(&|, #1, (--,#1), (x))");

        assertReduction("(x)", "(&&, --(#1 && (--,#1)), (x))");

        assertSame($("((x) &&+1 --(x))").op(), CONJ);
        assertSame($("(#1 &&+1 (--,#1))").op(), CONJ);


    }

    @Test
    public void testCoNegatedJunction() {
        

        assertReduction(False, "(&&,x,a:b,(--,a:b))");

        assertReduction(False, "(&&, (a), (--,(a)), (b))");
        assertReduction(False, "(&&, (a), (--,(a)), (b), (c))");


        assertReduction(False, "(&&,x,y,a:b,(--,a:b))");
    }

    @Test
    public void testCoNegatedDisjunction() {

        assertReduction(True, "(||,x,a:b,(--,a:b))");

        assertReduction(True, "(||,x,y,a:b,(--,a:b))");

    }

    @Test
    public void testInvalidStatementIndepVarTask() {
        NAR t = NARS.shell();
        try {
            t.inputTask("at($1,$2,$3).");
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
    public void testConegatedConjunctionTerms0not() {
        
        assertReduction("((--,((y)&|(z)))&&(x))", "((x)&&--((y) &&+0 (z)))");

        assertReduction("((--,((y)&&(z)))&|(x))", "((x) &&+0 --((y) && (z)))");
    }

    @Test
    public void testConegatedConjunctionTerms1not() {
        
        assertReduction("((--,((y) &&+1 (z)))&&(x))", "((x)&&--((y) &&+1 (z)))");

        assertReduction("((x) &&+1 (--,((y)&&(z))))", "((x) &&+1 --((y) && (z)))");
    }

    @Test
    public void testConegatedConjunctionTerms2() {
        
        assertReduction("((--,(robin-->swimmer))&&#1)", "(#1 && --(#1&&(robin-->swimmer)))");
    }

    @Test
    public void testDemorgan1() {
        


        
        assertReduction("(--,((p)&&(q)))", "(||, --(p), --(q))");
    }

    @Disabled
    @Test
    public void testDemorgan2() {

        
        assertReduction("(--,((p)||(q)))", "(--(p) && --(q))");
    }


    @Test
    public void testFilterCommutedWithCoNegatedSubterms() throws Narsese.NarseseException {
        


        assertValidTermValidConceptInvalidTaskContent(("((--,x) && x)"));
        assertValidTermValidConceptInvalidTaskContent("((--,x) &&+0 x)");
        assertValid($("((--,x) &&+1 x)"));
        assertValid($("(x &&+1 x)"));

        assertEquals($("x"), $("(x &&+0 x)"));
        assertEquals($("x"), $("(x && x)"));
        assertNotEquals($("x"), $("(x &&+1 x)"));

        assertInvalidTerms("((--,x) || x)");

    }

    @Test
    public void testRepeatInverseEquivalent() throws Narsese.NarseseException {
        assertEquals($("(x &&-1 x)"), $("(x &&+1 x)"));
        assertEquals($("(x =|> x)"), $("(x =|> x)"));
        assertEquals(True, $("(x =|> x)"));
        assertEquals($("x"), $("(x &| x)"));
    }

    @Test
    public void testAllowInhAndSimBetweenTemporallySimilarButInequalTerms() {
        assertValid("((x &&+1 y)<->(x &&+10 y))");
        assertValid("((y &&+10 x)<->(x &&+1 y))");
        assertValid("((x=|>y)-->(x ==>-10 y))");
    }

    @Test
    public void testAllowInhNegationStatements() throws Narsese.NarseseException {
        assertReduction(True, "(a-->a)");

        assertReduction("((--,a)-->b)", "((--,a) --> b)");
        assertNotEquals("(a-->b)", $("((--,a) --> b)").toString());
        assertReduction("(b-->(--,a))", "(b --> (--,a))");
        assertNotEquals("(a-->b)", $("(b --> (--,a))").toString());
        assertReduction("((--,a)-->(--,b))", "(--a --> --b)");

        assertReduction("((--,a)-->a)", "((--,a)-->a)");
        assertReduction("(a-->(--,a))", "(a-->(--,a))");

    }

    @Test
    public void testAllowSimNegationStatements() throws Narsese.NarseseException {
        assertReduction(True, "(a<->a)");

        assertNotEquals($("(--a <-> b)"), $("(a <-> --b)"));

        assertReduction("((--,a)<->b)", "((--,a) <-> b)");
        assertNotEquals("(a<->b)", $("((--,a) <-> b)").toString());
        assertReduction("((--,a)<->b)", "(b <-> (--,a))");
        assertNotEquals("(a<->b)", $("(b <-> (--,a))").toString());
        assertReduction("((--,a)<->(--,b))", "(--a <-> --b)");

        assertReduction("((--,a)<->a)", "((--,a)<->a)");

    }


    @Test
    public void testCoNegatedImpl() {
        assertValidTermValidConceptInvalidTaskContent(("((--,(a)) ==> (a))"));
        assertValidTermValidConceptInvalidTaskContent(("((--,(a)) ==>+0 (a))"));
    }




















    @Test
    public void testImplCommonSubterms() {
        
        assertReduction("(((--,isIn($1,xyz))&&(--,(($1,xyz)-->$2)))==>((y-->x)))", "(((--,isIn($1,xyz))&&(--,(($1,xyz)-->$2)))==>((--,(($1,xyz)-->$2))&&(x:y)))");
    }


    @Test public void testConjParallelOverrideEternal2() {

        assertReduction(
                "(&&,(a&|b),a,b)",
                //"(a&|b)",
                "( (a&&b) && (a&|b) )");

    }
    @Test
    public void testConjInImplicationTautology() {
        Term x0 = $.$$("((x &&+2 x) ==>-2 x)");
        assertEquals(True, x0);

        Term x = $.$$("((((_1,_2)&|(_1,_3)) &&+2 ((_1,_2)&|(_1,_3))) ==>-2 ((_1,_2)&|(_1,_3)))");
        assertEquals(True, x);
    }

    @Test
    public void testCommutizeRepeatingConjunctions() throws Narsese.NarseseException {
        assertEquals("a",
                $("(a &&+1 a)").dt(DTERNAL).toString());
        assertEquals(False,
                $("(a &&+1 --a)").dt(DTERNAL));

        assertEquals("a",
                $("(a &&+1 a)").dt(0).toString());
        assertEquals(False,
                $("(a &&+1 --a)").dt(0));

        assertEquals("(a &&+- a)",
                $("(a &&+1 a)").dt(XTERNAL).toString());
        assertEquals("((--,a) &&+- a)",
                $("(a &&+1 --a)").dt(XTERNAL).toString());


        assertEquals(True,
                $("(a ==>+1 a)").dt(DTERNAL));
        assertEquals(False,
                $("(--a ==>+1 a)").dt(DTERNAL));

        assertEquals(True,
                $("(a ==>+1 a)").dt(0));
        assertEquals(False,
                $("(--a ==>+1 a)").dt(0));


        assertEquals("(a ==>+- a)",
                $("(a ==>+1 a)").dt(XTERNAL).toString());
        assertEquals("((--,a) ==>+- a)",
                $("(--a ==>+1 a)").dt(XTERNAL).toString());
    }

    @Test public void testImplXternalPredicateImpl() {
        assertReduction("((x &&+- y) ==>+1 z)",
                "(x ==>+- (y ==>+1 z))");
        assertReduction("(((x &&+1 y) &&+- z) ==>+1 w)",
                "((x &&+1 y) ==>+- (z ==>+1 w))");
    }






    @Test
    public void testImplCommonSubterms2() {
        assertReduction(True, "((tetris(isRowClear,7,true)&&tetris(7,14))==>tetris(7,14))");
        
        

        
        

        assertReduction(True, "((tetris(isRowClear,7,true)&&tetris(7,14))=|>tetris(7,14))");

        assertReduction("((tetris(isRowClear,7,true)&&tetris(7,14)) ==>+10 tetris(7,14))", "((tetris(isRowClear,7,true)&&tetris(7,14)) ==>+10 tetris(7,14))");
    }

    @Test
    public void testImplCommonSubterms3() {

        assertReduction(True, "((x(intValue,(),0)&&x(set,0))==>x(intValue,(),0))");
        assertReduction("x(set,0)", "((x(intValue,(),0)==>x(intValue,(),0)) && x(set,0))");
        assertReduction(
                
                "((x(set,0)==>x(intValue,(),0))&&x(intValue,(),0))",
                "((x(set,0)==>x(intValue,(),0)) && x(intValue,(),0))");

    }

    @Test
    public void testCoNegatedImplOK() throws Narsese.NarseseException {
        assertValid($("((--,(a)) ==>+1 (a))"));
        assertValid($("((--,a) ==>+1 a)"));
    }







    @Test
    public void testRepeatEvent() throws Narsese.NarseseException {
        NAR n = NARS.shell();

        for (String x : new String[]{
                "((a) ==>+1 (a))",
                "((a) &&+1 (a))",

                /*"((a) &&+1 (a))",*/
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
    public void testCoNegatedDifference() throws Narsese.NarseseException {
        
        


        {
            NAR n = NARS.shell();
            n.believe("X", 1.0f, 0.9f);
            n.believe("Y", 0.5f, 0.9f);
            tryDiff(n, "(X~Y)", "%.50;.81%");
            tryDiff(n, "((--,Y)~(--,X))", "%.50;.81%");
            tryDiff(n, "(Y~X)", "%0.0;.81%");
            tryDiff(n, "((--,X)~(--,Y))", "%0.0;.81%");
        }
        {
            NAR n = NARS.shell();
            n.believe("X", 1.0f, 0.9f);
            n.believe("Y", 0.75f, 0.9f);
            tryDiff(n, "(X~Y)", "%.25;.81%");

            tryDiff(n, "((--,Y)~(--,X))", "%.25;.81%");

            tryDiff(n, "(Y~X)", "%0.0;.81%");
            tryDiff(n, "((--,X)~(--,Y))", "%0.0;.81%");
        }
        assertReduction("(Y~X)", "((--,X)~(--,Y))");
        assertReduction("(X~Y)", "((--,Y)~(--,X))");
        assertReduction("((Y~X)-->A)", "(((--,X)~(--,Y))-->A)");
        assertReduction("(A-->(Y-X))", "(A-->((--,X)-(--,Y)))");

    }

    public void tryDiff(NAR n, String term, String truthExpected) throws Narsese.NarseseException {
        assertEquals(truthExpected, n.beliefTruth(term, ETERNAL).toString(), term::toString);

    }

    @Test
    public void testCoNegatedIntersection() {
        
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

        assertEquals($("(&&,c:d,e:f)"), $("(&&,(a-->a),c:d,e:f)"));
        assertEquals($("(&&,c:d,e:f)"), $("(&&,(a==>a),c:d,e:f)"));
        assertReduction(False, "(&&,(--,(a==>a)),c:d,e:f)");

    }

    @Test
    public void testSingularStatementsInDisjunction() {

        assertInvalidTerms("(||,(a<->a),c:d,e:f)"); 
    }

    @Test
    public void testSingularStatementsInDisjunction2() throws Narsese.NarseseException {
        assertEquals($("x:y"), $("(&&,(||,(a<->a),c:d,e:f),x:y)")); 
        assertReduction(False, "(&&,(--,(||,(a<->a),c:d,e:f)),x:y)");






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
        
        
        Term c1 = $("((--,(ball_left)) &&-270 (ball_right)))");

        assertEquals("((ball_right) &&+270 (--,(ball_left)))", c1.toString());
        assertEquals(
                "(((ball_right)&|(ball_left)) &&+270 (--,(ball_left)))", 

                parallel($("(ball_left)"), $("(ball_right)"), c1)
                        .toString());

    }

    @Test
    public void testReducibleImplFactored() {
        assertReduction("((x&|y)=|>z)", "((y &| x) =|> (y &| z))");
        assertReduction("((x&|y)==>z)", "((y &| x) ==> (y &| z))");
    }

    @Test
    public void testReducibleImplFactored2() {
        assertReduction("((x&&y)==>z)", "((y && x) ==> (y && z))");
        assertReduction("((&&,a,x,y)==>z)", "((&&, x, y, a) ==> (y && z))");
        assertReduction("((y &&+1 x)=|>(z &&+1 y))", "((y &&+1 x)=|>(z &&+1 y))");
    }

    @Test
    public void testReducibleImplFactoredPredShouldRemainIntact() {
        
        for (String cp : new String[]{"&&", "&|", " &&+- "}) {
            assertReduction("((x&&y) ==>+1 (y" + cp + "z))", "((y&&x) ==>+1 (y" + cp + "z))");
            assertReduction("(a ==>+1 (b &&+1 (y" + cp + "z)))", "(a ==>+1 (b &&+1 (y" + cp + "z)))");
        }


    }

    static Term assertReduction(String exp, String is)  {
        Term t = $$(is);
        assertEquals(exp, t.toString(), () -> is + " reduces to " + exp);
        return t;
    }

    static void assertReduction(Term exp, String is)  {
        assertEquals(exp, $$(is), () -> exp + " reduces to " + is);
    }

    static void assertReduction(String exp, Term is)  {
        assertEquals(exp, is.toString(), () -> exp + " reduces to " + is);
    }

    @Test
    public void testReducibleImpl() {

        assertReduction("(--,((--,x)==>y))", "(--x ==> (--y && --x))");

        assertReduction("(x=|>y)", "(x ==>+0 (y &| x))");
        assertReduction(True, "((y &| x) =|> x)");
        assertReduction("(--,((--,$1)=|>#2))", "((--,$1)=|>((--,$1)&|(--,#2)))");
    }

    @Test
    public void testReducibleImplConjCoNeg() {
        for (String i : new String[]{"==>", "=|>"}) {
            for (String c : new String[]{"&&", "&|"}) {
                assertReduction(False, "(x " + i + " (y " + c + " --x))");
                assertReduction(False, "(--x " + i + " (y " + c + " x))");
                assertReduction(False, "((y " + c + " --x) " + i + " x)");
                assertReduction(False, "((y " + c + " x) " + i + " --x)");
            }
        }
    }


    @Test
    public void testReducibleImplParallelNeg() {
        assertReduction("(--,((--,x)=|>y))", "(--x =|> (--y &| --x))");
        assertReduction(True, "((--y &| --x) =|> --x)");

    }

    @Test
    public void testInvalidCircularImpl() throws Narsese.NarseseException {
        assertNotEquals(Null, $("(x(intValue,(),1) ==>+10 ((--,x(intValue,(),0)) &| x(intValue,(),1)))"));
        assertReduction("(--,(x(intValue,(),1)=|>x(intValue,(),0)))", "(x(intValue,(),1) =|> ((--,x(intValue,(),0)) &| x(intValue,(),1)))");
        assertReduction("(--,(x(intValue,(),1)==>x(intValue,(),0)))", "(x(intValue,(),1) ==> ((--,x(intValue,(),0)) &| x(intValue,(),1)))");
    }

    @Test
    public void testImplInImplDTernal() {
        assertReduction("(((--,(in))&&(happy))==>(out))", "((--,(in)) ==> ((happy)  ==> (out)))");
    }

    @Test
    public void testImplInImplDTemporal() {
        assertReduction("(((--,(in)) &&+1 (happy)) ==>+2 (out))", "((--,(in)) ==>+1 ((happy) ==>+2 (out)))");
    }

    @Test public void testImplInConjPos() throws Narsese.NarseseException {
        String s = "((c==>a)&&a)";
        assertEquals(
                
                
                s,
                $.$(s).toString());
    }
    @Test public void testImplInConjNeg() throws Narsese.NarseseException {
        String s = "((--,(c==>a))&&(--,a))";
        assertEquals(
                
                s, 
                $.$(s).toString()); 
    }
    @Test public void testImplInConj2xPos() throws Narsese.NarseseException {
        String s = "((c==>a)&&(d==>a))";
        assertEquals(
                
                s, 
                $.$(s).toString());
    }

    @Test public void testImplInConj2xNeg() throws Narsese.NarseseException {
        String s = "((--,(c==>a))&&(--,(d==>a)))";
        
        assertEquals(
                
                s, 
                $.$(s).toString());
    }





    @Test
    public void testConjunctiveCoNegationAcrossImpl() {
        

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
    public void testConjDisjNeg() {
        assertReduction("((--,(out))&&(happy))", "((--,(out))&&(||,(happy),(out)))");
    }

    @Test
    public void taskWithFlattenedConunctions() throws Narsese.NarseseException {
        
        

        @NotNull Term x = $("((hear(what)&&(hear(is)&&(hear(is)&&(hear(what)&&(hear(is)&&(hear(is)&&(hear(what)&&(hear(is)&&(hear(is)&&(hear(is)&&hear(what))))))))))) ==>+153 hear(is)).");
        assertEquals("((hear(what)&&hear(is)) ==>+153 hear(is))", x.toString());

    }














    @Test
    public void testPromoteEternalToParallel() {
        String s = "(a&|(b && c))";
        assertReduction(
                "((b&&c)&|a)", 
                s);
    }

    @Test
    public void testPromoteEternalToParallelDont() {
        String s = "(a&&(b&|c))";
        assertReduction("((b&|c)&&a)", s);
    }

    @Test
    public void negatedConjunctionAndNegatedSubterm() throws Narsese.NarseseException {
        

        
        assertEquals("(--,x)", $.$("((--,(x &| y)) &| (--,x))").toString());
        assertEquals("(--,x)", $.$("((--,(x && y)) && (--,x))").toString());

        
        assertEquals("(--,x)", $.$("((--,(x && y)) && (--,x))").toString());
        assertEquals("(--,x)", $.$("((--,(&&,x,y,z)) && (--,x))").toString());

        assertEquals("((--,(y&&z))&&x)", $.$("((--,(&&,x,y,z)) && x)").toString()); 
    }

    @Disabled @Test
    public void testCoNegatedConjunctionParallelEternal1() {
        
        assertReduction(False,
                "(((--,(z&&y))&&x)&|(--,x))");
    }

    @Disabled @Test
    public void testCoNegatedConjunctionParallelEternal2() {
        assertReduction(False,
                "(((--,(y&&z))&|x)&&(--,x))");

    }


    /**
     * TODO decide if it should not apply this reduction to eternal
     */
    @Test
    public void testConjImplReduction0() {
        assertReduction(
                
                "((inside(john,playground)==>inside(bob,kitchen))&&inside(bob,office))",
                "(inside(bob,office) && (inside(john,playground)==>inside(bob,kitchen)))");
    }

    @Test
    public void testConjImplReduction() throws Narsese.NarseseException {
        Term a = $("((a,b) ==>+1 (b,c))");
        Term b = $("(c,d)");
        Term x = Op.CONJ.compound(4, new Term[]{a, b});

        assertEquals(
                
                "(((a,b) ==>+1 (b,c)) &&+4 (c,d))",
                x.toString());
    }

    @Test
    public void testConjImplNonReductionNegConj() throws Narsese.NarseseException {
        Term a = $("((a,b) ==>+1 (b,c))");
        Term b = $("(c,d)");
        Term x = Op.CONJ.compound(-4, new Term[]{a, b});

        assertEquals(
                "((c,d) &&+4 ((a,b) ==>+1 (b,c)))",
                
                x.toString());
    }

    @Test
    public void testConjImplReductionNegConj2() throws Narsese.NarseseException { 
        Term b = $("(c,d)");
        Term a = $("((a,b) ==>+1 (b,c))");
        Term x = Op.CONJ.compound(4, new Term[]{b, a});

        assertEquals(
                
                "((c,d) &&+4 ((a,b) ==>+1 (b,c)))",
                x.toString());
    }
    @Test
    public void testConjImplNonReductionNegConj2() throws Narsese.NarseseException {
        Term a = $("((a,b) ==>+1 (b,c))");
        Term b = $("(c &&+1 d)");
        Term x = Op.CONJ.compound(-4, new Term[]{a, b});

        assertEquals(
                
                "((c &&+1 d) &&+4 ((a,b) ==>+1 (b,c)))",
                x.toString());
    }
    @Test
    public void testConjImplNonReductionNegConj3() throws Narsese.NarseseException {
        Term a = $("((a,b) ==>+1 (b,c))");
        Term b = $("(c &&+1 d)");
        {
            Term x = Op.CONJ.compound(+4, new Term[]{a, b});

            assertEquals(
                    
                    "((((a,b) ==>+1 (b,c)) &&+4 c) &&+1 d)",
                    x.toString());

            Term x2 = Conj.conjMerge(a, 0, b, 4);
            assertEquals(x, x2);
        }
    }

    @Test
    public void testConjImplReductionNegConj2b() throws Narsese.NarseseException {
        Term b = $("(c,d)");
        Term a = $("((a,b) ==>-1 (b,c))");
        Term x = Op.CONJ.compound(4, new Term[]{b, a});

        assertEquals(
                
                "((c,d) &&+4 ((a,b) ==>-1 (b,c)))",
                x.toString());
    }

    @Test
    public void testConjImplReductionNegImpl() throws Narsese.NarseseException {
        Term a = $("((a,b) ==>-1 (b,c))");
        Term b = $("(c,d)");
        Term x = Op.CONJ.compound(4, new Term[]{a, b});

        assertEquals(
                
                "(((a,b) ==>-1 (b,c)) &&+4 (c,d))",
                x.toString());
    }

    @Test
    public void testConjImplReductionWithVars() throws Narsese.NarseseException {
        Term a = $("((a,#1) ==>+1 (#1,c))");
        Term b = $("(c,d)");
        Term x = Op.CONJ.compound(4, new Term[]{a, b});

        assertEquals(
                
                "(((a,#1) ==>+1 (#1,c)) &&+4 (c,d))",
                x.toString());
    }

    @Test
    public void testConjImplReduction1() {
        assertReduction(
                
                "((inside(john,playground)==>inside(bob,kitchen))&|inside(bob,office))",
                "(inside(bob,office)&|(inside(john,playground)==>inside(bob,kitchen)))");
    }

    @Test
    public void testConjImplReduction2() throws Narsese.NarseseException {
        

        Term t = $("(inside(bob,office) &&+1 (inside(john,playground) ==>+1 inside(bob,kitchen)))");

        assertEquals(
                
                "(inside(bob,office) &&+1 (inside(john,playground) ==>+1 inside(bob,kitchen)))",
                t.toString()
        );
    }

    @Test
    public void testConjImplReductionNeg2() {
        
        assertReduction(
                
                "(inside(bob,office) &&+1 ((--,inside(john,playground)) ==>+1 inside(bob,kitchen)))",
                "(inside(bob,office) &&+1 (--inside(john,playground) ==>+1 inside(bob,kitchen)))");
    }

    @Test
    public void testConjImplReduction3() {
        
        assertReduction(
                
                "((j ==>-1 k) &&+1 b)",
                "((j ==>-1 k) &&+1 b)");

        assertReduction(
                
                "((j ==>-1 k) &&+1 b)",
                "(b &&-1 (j ==>-1 k))");
    }
























































}
