package nars.util.term.transform;

import jcog.math.random.XorShift128PlusRandom;
import nars.*;
import nars.derive.premise.PremisePatternIndex;
import nars.term.Compound;
import nars.term.Term;
import nars.term.anon.Anon;
import nars.test.TestNAR;
import nars.unify.Unify;
import nars.util.signal.RuleTest;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;


public class UnifyTest {

    public static final int INITIAL_TTL = 512;

    @Test
    public void testFindSubst1() throws Narsese.NarseseException {
        testUnify($.$("<a-->b>"), $.$("<?C-->b>"), true);
        testUnify($.$("(--,(a))"), $.$("<?C-->(b)>"), false);
    }

//        static {
//
//    }

//
//    @Before
//    public void start() {
//        t = new TestNAR(
//                //new Terminal()
//                new NARBuilder().get() //TODO return to using Terminal as a demo of its minimal functionality
//        );
//    }
//
//    public TestNAR test() {
//        return t;
//    }

    public static /**/ Term pattern(/**/ String s) throws Narsese.NarseseException {
        Term ss = Narsese.the().term(s, false);
        return pattern(ss);
    }

    public static Term pattern(Term ss) {
        return new PremisePatternIndex().pattern(ss);
    }


    void test(/**/ Op type,  String s1,  String s2, boolean shouldSub) {
        test(type, s1, s2, shouldSub, false, false);
//        test(type, s1, s2, shouldSub, true, true);
        test(type, s1, s2, shouldSub, true, true);
    }

    Unify test(/**/ Op type,  String s1,  String s2, boolean shouldSub, boolean anon1, boolean anon2) {

        //

        //NAR nar = NARS.shell();
        Anon a = new Anon();

        try {

            Term t2 = Narsese.the().term(s2, true);
            if (anon2) t2 = a.put(t2).normalize();

            Term t1;
            if (type == Op.VAR_PATTERN) {
                t1 = Narsese.the().term(s1, false); //special handling for ellipsis
                if (anon1) t1 = pattern(a.put(t1)).normalize();
                else t1 = pattern(t1).normalize();
            } else {
                t1 = Narsese.the().term(s1, true);
                if (anon1) t1 = a.put(t1).normalize();
            }


            assertNotNull(t1);
            assertNotNull(t2);



//            nar.question(s2);
//            nar.run(cycles);


            Set<Term> t1u = ((Compound)t1).recurseTermsToSet(type);
            //assertTrue(t1u.size() > 0);
            //Set<Term> t2u = ((Compound)t2).recurseTermsToSet(type);
            //assertTrue(t2u.size() == 0);

            int n1 = t1u.size(); //Sets.difference(t1u, t2u).size();
//            int n2 = Sets.difference(t2u, t1u).size();

            //a somewhat strict lower bound
            //int power = 4 * (1 + t1.volume() * t2.volume());
            //power*=power;

            AtomicBoolean subbed = new AtomicBoolean(false);

            Unify sub = new Unify(type,
                    new XorShift128PlusRandom(1),
                    Param.UnificationStackMax, INITIAL_TTL) {


                @Override
                public void tryMatch() {

                    if (shouldSub) {


                        this.xy.forEachVersioned((k, v) -> {
                            if (matchType(k.op()))
                                assertNotNull(v);
                            return true;
                        });

                        if (/*((n2) <= (yx.size())*/
                                (n1) <= (xy.size())) {
                            subbed.set(true);

                        } /*else {
                            System.out.println("incomplete:\n\t" + xy);
                        }*/

//                        assertFalse("incomplete: " + toString(), this.isEmpty());


                    } else {
                        //HACK there should be incomplete assignments even though this says it matched
                        assertTrue((n1) > (xy.size()), "why matched?: " + xy); //|| (n2) <= (yx.size()));
                        //assertFalse("match found but should not have", true);
                    }

                }
            };

            sub.unify(t1, t2, true);

            sub.revert(0); //for testing

            assertEquals(shouldSub, subbed.get());

            return sub;


        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }

    }


    @Test
    public void unificationP0() {
        test(Op.VAR_PATTERN,
                "<%A ==> %B>",
                "<<a --> A> ==> <b --> B>>",
                true
        );
    }

    @Test
    public void unificationP1() {
        test(Op.VAR_DEP,
                "<(#1,#1) --> wu>",
                "<(a,b) --> wu>",
                false
        );
    }

    @Test
    public void unificationP2() {
        test(Op.VAR_DEP,
                "<(#1,c) --> wu>",
                "<(a,b) --> wu>",
                false
        );
    }

    @Test
    public void unificationP3() {
        test(Op.VAR_PATTERN,
                "<(%1,%1,#1) --> wu>",
                "<(lol,lol2,#1) --> wu>",
                false
        );
    }

    @Test
    public void unificationQ3() {
        test(Op.VAR_QUERY,
                "<(?1,?2,a) --> wu>",
                "<(lol,lol2,a) --> wu>",
                true
        );
        test(Op.VAR_QUERY,
                "<(?1,?1,#1) --> wu>",
                "<(lol,lol2,#1) --> wu>",
                false
        );
    }

    @Test
    public void unificationP5() {
        test(Op.VAR_DEP,
                "<#x --> lock>",
                "<{lock1} --> lock>",
                true
        );
    }

//    @Test
//    public void unification4() {
//        test(Op.VAR_PATTERN,
//                "<(%1,%2,%3,$1) --> wu>",
//                "<(%1,lol2,%1,$1) --> wu>",
//                true
//        );
//    }


    @Test
    public void pattern_trySubs_Dep_Var() {
        test(Op.VAR_PATTERN,
                "<%A ==> %B>",
                "<<#1 --> A> ==> <?1 --> B>>",
                true);
    }

    @Test
    public void pattern_trySubs_Var_2_parallel() {
        test(Op.VAR_QUERY,
                "(&&,<(?1,x) --> on>,<(SELF,#2) --> at>)",
                "(&&,<({t002},x) --> on>,<(SELF,#1) --> at>)",
                true);
    }

    @Test
    public void pattern_trySubs_Var_2_product_and_common_depvar_bidirectional() {
        test(Op.VAR_DEP,
                "(<(#1,x) --> on>,<(SELF,x) --> at>)",
                "(<(SELF,x) --> on>,<(#1,x) --> at>)",
                true);

        //THESE NEED TO BE TESTED WITHIN THE SUBSTITUTION BECAUSE XY AND YX WILL HAVE BEEN CLEARED BY NOW
        //additional test that verifies correct common variable substitution result
//        assertEquals("{#1={t002}}", sub.xy.toString());
//        assertEquals("{(SELF,x)=#1}", sub.yx.toString());
    }

    @Test
    public void pattern_trySubs_2_product() {
        test(Op.VAR_QUERY,
                "(on(?1,x),     at(SELF,x))",
                "(on({t002},x), at(SELF,x))",
                true);
    }

    @Test
    public void pattern_trySubs_Dep_Var_2_product() {
        test(Op.VAR_DEP,
                "(<(#1,x) --> on>,<(SELF,x) --> at>)",
                "(<({t002},x) --> on>,<(SELF,x) --> at>)",
                true);
    }

    @Test
    public void pattern_trySubs_Indep_Var_2_set() {
        test(Op.VAR_DEP,
                "{<(#1,x) --> on>,<(SELF,x) --> at>}",
                "{<({t002},x) --> on>,<(SELF,x) --> at>}",
                true);
    }

    @Test
    public void pattern_trySubs_Indep_Var_2_set2() {
        test(Op.VAR_DEP,
                "{<(#1,x) --> on>,<(SELF,x) --> at>}",
                "{<(z,x) --> on>,<(SELF,x) --> at>}",
                true);
    }

    @Test
    public void pattern_trySubs_Pattern_Var_2_setSimple() {
        test(Op.VAR_PATTERN,
                "{%1,y}",
                "{z,y}",
                true);

    }


    @Test
    public void pattern_trySubs_Pattern_Var_2_setSimpler() {
        test(Op.VAR_PATTERN,
                "{%1}",
                "{z}",
                true);
    }

    @Test
    public void pattern_trySubs_Pattern_Var_2_setComplex0() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>,y}",
                "{<(z,x) --> on>,y}",
                true);
    }


    @Test
    public void pattern_trySubs_Pattern_Var_2_setComplex0_1() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>,<x-->y>}",
                "{<(z,x) --> on>,<x-->y>}",
                true);
    }

    @Test
    public void pattern_trySubs_Pattern_Var_2_setComplex0_2() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>,(a,b)}",
                "{<(z,x) --> on>,(a,b)}",
                true);
    }

    @Test
    public void pattern_trySubs_Pattern_Var_2_setComplex0_3() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>, c:(a)}",
                "{<(z,x) --> on>, c:(a)}",
                true);
    }

    @Test
    public void pattern_trySubs_Pattern_Var_2_setComplex0_4() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>, c:a:b}",
                "{<(z,x) --> on>, c:a:b}",
                true);
    }

    @Test
    public void pattern_trySubs_Pattern_Var_2_setComplex0_5() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>, c:(a,b)}",
                "{<(z,x) --> on>, c:(a,b)}",
                true);
    }

    @Test
    public void pattern_trySubs_Pattern_Var_2_setComplex0_5_n() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>, c:(a,b)}",
                "{<(z,x) --> on>, c:(b,a)}",
                false);
    }

    @Test
    public void pattern_trySubs_Pattern_Var_2_setComplex0_5_1() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>, c:(a && b)}",
                "{<(z,x) --> on>, c:(a && b)}",
                true);
    }

    @Test
    public void pattern_trySubs_Pattern_Var_2_setComplex0_5_c() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>, c:{a,b}}",
                "{<(z,x) --> on>, c:{a,b}}",
                true);
    }

    @Test
    public void pattern_trySubs_Pattern_Var_2_setComplex0_5_c1() {
        test(Op.VAR_PATTERN,
                "{<(z,%1) --> on>, c:{a,b}}",
                "{<(z,x) --> on>, c:{a,b}}",
                true);
    }

    @Test
    public void pattern_trySubs_Pattern_Var_2_setComplex0_5_c2() {
        test(Op.VAR_PATTERN,
                "{<(%1, z) --> on>, w:{a,b,c}}",
                "{<(x, z) --> on>, w:{a,b,c}}",
                true);
    }


    @Test
    public void pattern_trySubs_Pattern_Var_2_setComplex0_5_s() {
        //may require more termutation matches than default is set but it should work if it had all
        test(Op.VAR_PATTERN,
                "{<{%1,x} --> on>, c:{a,b}}",
                "{<{z,x} --> on>, c:{a,b}}",
                true);
    }

    @Test
    public void pattern_trySubs_Pattern_Var_2_setComplex0_5_r() {
        test(Op.VAR_PATTERN,
                "{on:{%1,x}, c}",
                "{on:{z,x}, c}",
                true);
    }


    @Test
    public void pattern_trySubs_Pattern_Var_2_setComplex1() {
        test(Op.VAR_PATTERN,
                "{%1,<(SELF,x) --> at>}",
                "{z,<(SELF,x) --> at>}",
                true);
    }

    @Test
    public void pattern_trySubs_Pattern_Var_2_setComplex2() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>,<(SELF,x) --> at>}",
                "{<(z,x) --> on>,<(SELF,x) --> at>}",
                true);
    }

    @Test
    public void pattern_trySubs_Dep_Var_2_set() {
        test(Op.VAR_DEP,
                "{<(#1,x) --> on>,<(SELF,x) --> at>}",
                "{<({t002},x) --> on>,<(SELF,x) --> at>}",
                true);
    }


    @Test
    public void pattern_trySubs_Indep_Var_32() {
        test(Op.VAR_PATTERN,
                "<%A ==> <(SELF,$1) --> reachable>>",
                "<(&&,<($1,#2) --> on>,<(SELF,#2) --> at>) ==> <(SELF,$1) --> reachable>>",
                true);
    }

    @Test
    public void pattern_trySubs_set3() {
        test(Op.VAR_PATTERN,
                "{%1,%2,%3}",
                "{a,b,c}",
                true);
    }


    @Test
    public void pattern_trySubs_set2_1() {
        test(Op.VAR_PATTERN,
                "{%1,b}", "{a,b}",
                true);
    }

    @Test
    public void pattern_trySubs_set2_2() {
        test(Op.VAR_PATTERN,
                "{a,%1}", "{a,b}",
                true);
    }

    @Test
    public void pattern_trySubs_set3_1_b() {
        test(Op.VAR_PATTERN,
                "{a,b,c}",
                "{%1,b,%2}",
                true);
    }

    @Test
    public void pattern_trySubs_set3_1_b_reverse() {
        test(Op.VAR_PATTERN,
                "{%1,b,%2}",
                "{a,b,c}",
                true);
    }

    @Test
    public void pattern_trySubs_set3_1_b_commutative_inside_statement() {
        test(Op.VAR_PATTERN,
                "<{a,b,c} --> d>",
                "<{%1,b,%2} --> %3>",
                true);
    }

    @Test
    public void pattern_trySubs_set3_1_statement_of_specific_commutatives() {
        test(Op.VAR_PATTERN,
                "<{a,b} --> {c,d}>",
                "<{%1,b} --> {c,%2}>",
                true);
    }

    @Test
    public void pattern_trySubs_set3_1_statement_of_specific_commutatives_reverse() {
        test(Op.VAR_PATTERN,
                "<{%1,b} --> {c,%2}>",
                "<{a,b} --> {c,d}>",
                true);
    }

    @Test
    public void pattern_trySubs_set3_1_c() {
        test(Op.VAR_PATTERN,
                "{a,b,c}",
                "{%1,%2,c}",
                true);
    }

    @Test
    public void pattern_trySubs_set3_1_c_reverse() {
        test(Op.VAR_PATTERN,
                "{%1,%2,c}",
                "{a,b,c}",
                true);
    }

    @Test
    public void pattern_trySubs_set4() {
        test(Op.VAR_PATTERN,
                "{%1,%2,%3,%4}",
                "{a,b,c,d}",
                true);
    }

    //not valid test anymore
//    @Test public void diffVarTypes1()  {
//        test(Op.VAR_DEP,
//                "(a,$1)",
//                "(#1,$2)",
//                true);
//    }
//    @Test public void diffVarTypes1Reverse()  {
//        test(Op.VAR_DEP,
//                "(#1,$1)",
//                "(a,$1)",
//                true);
//    }
    @Test
    public void impossibleMatch1() {
        test(Op.VAR_DEP,
                "(a,#1)",
                "(b,b)",
                false);
    }

    @Test
    public void posNegQuestion() {
        //((p1, (--,p1), task("?")), (p1, (<BeliefNegation --> Truth>, <Judgment --> Punctuation>)))
        //  ((a:b, (--,a:b), task("?")), (a:b, (<BeliefNegation --> Truth>, <Judgment --> Punctuation>)))
        RuleTest.get(new TestNAR(NARS.shell()),
                "a:b?", "(--,a:b).",
                "a:b.",
                0, 0, 0.9f, 0.9f);
    }

    @Test
    public void patternSimilarity1() {
        test(Op.VAR_PATTERN,
                "<%1 <-> %2>",
                "<a <-> b>",
                true);
    }

    @Test
    public void patternNAL2Sample() {
        test(Op.VAR_PATTERN,
                "(<%1 --> %2>, <%2 --> %1>)",
                "(<bird --> {?1}>, <bird --> swimmer>)",
                false);
    }

    @Test
    public void patternNAL2SampleSim() {
        test(Op.VAR_PATTERN,
                "(<%1 <-> %2>, <%2 <-> %1>)",
                "(<bird <-> {?1}>, <bird <-> swimmer>)",
                false);
    }

    @Test
    public void patternLongSeq_NO_1() {
        test(Op.VAR_PATTERN,
                "(a,b,c,d,e,f,g,h,j)",
                "(x,b,c,d,e,f,g,h,j)",
                false);
    }

    @Test
    public void patternLongSeq_NO_2() {
        test(Op.VAR_PATTERN,
                "(a,b,c,d,e,f,g,h,j)",
                "(a,b,c,d,e,f,g,h,x)",
                false);
    }

//   @Test
//    public void patternLongSeq_YES() {
//        test(Op.VAR_PATTERN,
//                "(a,b,c,d,e,f,g,h,j)",
//                "(a,b,c,d,e,f,g,h,j)",
//                true);
//    }

    @Test
    public void ellipsisCommutive1a() {
        test(Op.VAR_PATTERN,
                "{%X..+}",
                "{a}", true);
    }

    @Test
    public void ellipsisCommutive1b() {
        test(Op.VAR_PATTERN,
                "{a, %X..+}",
                "{a}", false);
    }

    @Test
    public void ellipsisCommutive1c() {
        test(Op.VAR_PATTERN,
                "{a, %X..*}",
                "{a}", true);
    }

    @Test
    public void ellipsisCommutive2a() {

        test(Op.VAR_PATTERN,
                "{a, %X..+}",
                "{a, b}", true);
    }

    @Test
    public void ellipsisCommutive2b() {
        test(Op.VAR_PATTERN,
                "{%X..+, a}",
                "{a, b, c, d}", true);
    }

    @Test
    public void ellipsisCommutive2c() {
        test(Op.VAR_PATTERN,
                "{a, %X..+, e}",
                "{a, b, c, d}", false);
    }

    @Test
    public void ellipsisLinearOneOrMoreAll() {
        test(Op.VAR_PATTERN,
                "(%X..+)",
                "(a)", true);
    }

    @Test
    public void ellipsisLinearOneOrMoreSuffix() {
        test(Op.VAR_PATTERN,
                "(a, %X..+)",
                "(a, b, c, d)", true);
    }

    @Test
    public void ellipsisLinearOneOrMoreSuffixNoneButRequired() {
        test(Op.VAR_PATTERN,
                "(a, %X..+)",
                "(a)", false);
    }

    @Test
    public void ellipsisLinearOneOrMorePrefix() {
        test(Op.VAR_PATTERN,
                "(%X..+, a)",
                "(a, b, c, d)", false);
    }

    @Test
    public void ellipsisLinearOneOrMoreInfix() {
        test(Op.VAR_PATTERN,
                "(a, %X..+, a)",
                "(a, b, c, d)", false);
    }

    @Test
    public void ellipsisLinearZeroOrMore() {
        test(Op.VAR_PATTERN,
                "(a, %X..*)",
                "(a)", true);
    }


    @Test
    public void ellipsisLinearRepeat1() {
        test(Op.VAR_PATTERN,
                "((a, %X..+), %X..+)",
                "((a, b, c, d), b, c, d)", true);
    }

    @Test
    public void ellipsisLinearRepeat2() {
        test(Op.VAR_PATTERN,
                "((a, %X..+), (z, %X..+))",
                "((a, b, c, d), (z, b, c, d))", true);
    }


    @Test
    public void ellipsisCommutiveRepeat2_a() {
        //b, c, d -  which can match exactly in both
        test(Op.VAR_PATTERN,
                "{{a, %X..+}, {z, %X..+}}",
                "{{a, b, c, d}, {z, b, c, d}}", true);
    }

    @Test
    public void ellipsisCommutiveRepeat2_aa() {
        //no X which can match exactly in both
        test(Op.VAR_PATTERN,
                "{{a, %X..+}, {z, b, %X..+}}",
                "{{a, b, c, d}, {z, b, c, d}}", false);
    }

    @Test
    public void ellipsisCommutiveRepeat2_set() {
        test(Op.VAR_PATTERN,
                "{{a, %X..+, %B}, {z, %X..+, %A}}",
                "{{a, b, c, d}, {z, b, c, d}}", true);
    }

    @Test
    public void ellipsisCommutiveRepeat2_product() {
        test(Op.VAR_PATTERN,
                "({a, %X..+, %B}, {z, %X..+, %A})",
                "({a, b, c, d}, {z, b, c, d})", true);
    }

    @Test
    public void ellipsisCommutiveRepeat2_c() {
        //X and Y are different so they can match
        test(Op.VAR_PATTERN,
                "{{a, %X..+}, {b, %Y..+}}",
                "{{a, b, c}, {d, b, c}}", true);
    }

    @Test
    public void ellipsisCommutiveRepeat2_cc() {
        //X and Y are different so they can match
        test(Op.VAR_PATTERN,
                "{{a, %X..+}, {b, %Y..+}}",
                "{{a, b, c, d}, {z, b, c, d}}", true);
    }

    @Test
    public void ellipsisLinearInner() {

        //TODO - lower priority
        test(Op.VAR_PATTERN,
                "(a, %X..+, d)",
                "(a, b, c, d)", true);
    }
//
//    @Test
//    public void patternImage() {
//        test(Op.VAR_DEP,
//                "<A --> (/, n, _, #X)>",
//                "<A --> (/, n, _, B)>", true);
//
//        test(Op.VAR_DEP,
//                "<A --> (/, #X, _, n)>",
//                "<A --> (/, B, _, n)>", true);
//
//        test(Op.VAR_DEP,
//                "<A --> (/, x, #X, _)>",
//                "<A --> (/, x, _, B)>", false);
//
//
////        test(Op.VAR_PATTERN,
////                "(&&,<F --> A>,<%X --> (/,E, _,C,D)>)",
////                "(&&,<F --> A>,<E --> (/,E,_,C,D)>)", true);
////        test(Op.VAR_PATTERN,
////                "(&&,<F --> A>,<%X --> (/,C,D,_)>)",
////                "(&&,<F --> A>,<E --> (/,C,D,_)>)", true);
////        test(Op.VAR_PATTERN,
////                "(&&,<F --> A>,<D --> (/,C,%X, _)>)",
////                "(&&,<F --> A>,<D --> (/,C,E, _)>)", true);
//
//    }
//
//    @Test
//    public void testImage2ShouldNotMatch() {
//        test(Op.VAR_PATTERN,
//                "(/, %X, _)",
//                "(/, _, A)", false);
//    }
//
//    @Test
//    public void ellipsisImage1() {
//        test(Op.VAR_PATTERN,
//                "<A --> (/, z, _, %X..+)>",
//                "<A --> (/, z, _, B)>", true);
//    }
//
//    @Test
//    public void ellipsisImage2() {
//        test(Op.VAR_PATTERN,
//                "<A --> (/, z, _, %X..+)>",
//                "<A --> (/, z, _, B,C)>", true);
//
//    }
//
//    @Test
//    public void testEllipsisImage2a() {
//        test(Op.VAR_PATTERN,
//                "(/,_, B, %X..+)",
//                "(/,_, B, C, D)", true);
//    }
//
//    @Test
//    public void testEllipsisImage2b() {
//        test(Op.VAR_PATTERN,
//                "(/,_, %X..+)",
//                "(/,_, B, C, D)", true);
//    }
//
//    @Test
//    public void testEllipsisImage2c() {
//        test(Op.VAR_PATTERN,
//                "<A --> (/, z, _, B, %X..+)>",
//                "<A --> (/, z, _, B, C, D)>", true);
//    }
//
//    @Test
//    public void testEllipsisImage2d() {
//        test(Op.VAR_PATTERN,
//                "<A --> (/, z, _, E, %X..+)>",
//                "<A --> (/, z, _, B, C, D)>", false);
//    }
//
//    @Test
//    public void testEllipsisImage2e() {
//        test(Op.VAR_PATTERN,
//                "<A --> (/, B, _, %X..+)>",
//                "<A --> (/, B, _, C, D)>", true);
//    }
//
//
//    @Disabled
//    @Test
//    public void testImageRelationAfterEllipsis() {
//        test(Op.VAR_PATTERN,
//                "<A --> (/, B, %X..+, _)>",
//                "<A --> (/, B, C, D, _)>", true);
//        test(Op.VAR_PATTERN,
//                "<A --> (/, B, %X..+, _)>",
//                "<A --> (/, B, C, _, D)>", false);
//    }
//
//    @Disabled
//    @Test
//    public void testInnerEllipsis() {
//        test(Op.VAR_PATTERN,
//                "<A --> (/, B, %X..+, E, _)>",
//                "<A --> (/, B, C, D, E, _)>", true);
//    }

    @Test
    public void ellipsisSequence() {
        //TODO test for inclusion of intervals in matching
    }


    @Test
    public void testA() {
        String somethingIsBird = "bird:$x";
        String somethingIsAnimal = "animal:$x";
        testIntroduction(somethingIsBird, Op.IMPL, somethingIsAnimal, "bird:robin", "animal:robin");
    }


    void testIntroduction(String subj, Op relation, String pred, String belief,  String concl) {

        NAR n = NARS.shell();

        new TestNAR(n)
                .believe('(' + subj + ' ' + relation + ' ' + pred + ')')
                .believe(belief)
                .mustBelieve(4, concl, 0.81f);
        //.next()
        //.run(1).assertTermLinkGraphConnectivity();

    }

//    @Test
//    public void testIndVarConnectivity() throws Narsese.NarseseException {
//
//        String c = "(<$x --> bird> ==> <$x --> animal>).";
//
//        NAR n = new Default();
//        n.nal(6);
//
//        n.input(c);
//        n.run(1);
//
//        //n.forEachActiveConcept(x -> x.get().print());
//
//        TermLinkGraph g = new TermLinkGraph(n);
//        assertTrue("termlinks form a fully connected graph:\n" + g, g.isConnected());
//
//    }

    /**
     * this case is unrealistic as far as appearing in rules but it would be nice to get working
     */
    @Test
    public void ellipsisCommutiveRepeat() {
        test(Op.VAR_PATTERN,
                "{{a, %X..+}, %X..+}",
                "{{a, b, c, d}, b, c, d}", true);
    }

    @Test
    public void patternMatchesQuery1() {
        test(Op.VAR_PATTERN,
                "(<%1 <-> %2>, <%3 <-> %2>)",
                "(<x <-> ?1>, <y <-> ?1>)",
                true);
    }

    @Test
    public void patternMatchesQuery2() {
        test(Op.VAR_PATTERN,
                "(<%1 <-> %2>, <%3 <-> %2>)",
                "(<bird <-> {?1}>, <bird <-> {?1}>)",
                true);
    }

    @Test
    public void varDep2() {
        test(Op.VAR_DEP,
                "t:(#x | {#y})",
                "t:(x | {y})",
                true);
    }

    /*
    //// second level variable handling rules ////////////////////////////////////////////////////////////////////////////////////
    //second level variable elimination (termlink level2 growth needed in order for these rules to work)

    (B --> K), (&&,(#X --> L),(($Y --> K) ==> A)) |- substitute((&&, (#X --> L), A), $Y,B), (Truth:Deduction)
    (B --> K), (&&,(#X --> L),(($Y --> K) ==> (&&,A..+))) |- substitute((&&,(#X --> L),A..+),$Y,B), (Truth:Deduction)
     */


    
    Subst testUnify( Compound a,  Compound b, boolean matches) {

        AtomicBoolean matched = new AtomicBoolean(false);

        Unify f = new Unify(Op.VAR_QUERY, new XorShift128PlusRandom(1), Param.UnificationStackMax, 128) {

            @Override
            public void tryMatch() {

                assertTrue(matches);

                matched.set(true);

                //identifier: punctuation, mapA, mapB
                assertEquals("{?1=a}", xy.toString());

                //output
                assertEquals(
                        "(a-->b) (?1-->b) -?>",
                        a + " " + b + " -?>"  /*+ " remaining power"*/);

            }
        };

        f.unify(b, a, true);

        assertEquals(matched.get(), matches);

        return f;
    }
}