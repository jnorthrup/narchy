package nars.term.util.transform;

import jcog.random.XoRoShiRo128PlusRandom;
import jcog.random.XorShift128PlusRandom;
import nars.$;
import nars.NAL;
import nars.Narsese;
import nars.Op;
import nars.derive.premise.PatternTermBuilder;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.anon.Anon;
import nars.unify.Unify;
import nars.unify.UnifyAny;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.*;


public class UnifyTest {

    private static final int INITIAL_TTL = 512;

    @Test
    void testCommonStructureAllVariables() {
        Unify u = new UnifyAny(new XoRoShiRo128PlusRandom(1));
//        assertTrue(
//            Terms.commonStructureTest( $$("(#1,$2,?3)").subterms(), $$("(#3,$2,?1)").subterms(), u)
//        );
        assertTrue(
                Subterms.possiblyUnifiable($$("(#1,$2,?3)").subterms(), $$("(#3,$2,?1)").subterms(), u)
        );
    }

    @Test
    void testFindSubst1() throws Narsese.NarseseException {
        testUnify($.$("<a-->b>"), $.$("<?C-->b>"), true);
        testUnify($.$("(--,(a))"), $.$("<?C-->(b)>"), false);
    }


    public static /**/ Term pattern(/**/ String s) throws Narsese.NarseseException {
        return PatternTermBuilder.rule(Narsese.term(s, false));
    }


    static private void test(/**/ Op type, String s1, String s2, boolean shouldUnify) {
        for (int seed : new int[]{1, 2, 3, 4}) {
            try {
                test(seed, type, s1, s2, shouldUnify, false, false);
                test(seed, type, s1, s2, shouldUnify, true, true);
            } catch (Narsese.NarseseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test void testEllipsisContainingTermNotEqual() {
        assertNotEquals( $$("{a, %X}"), $$("{a, %X..+}"));
    }

    static private Unify test(int rngSeed, /**/ Op type, String s1, String s2, boolean shouldSub, boolean anon1, boolean anon2) throws Narsese.NarseseException {


        Anon a = new Anon();


        Term t2 = Narsese.term(s2, true);
        if (anon2) t2 = a.put(t2).normalize();

        Term t1;
        if (type == Op.VAR_PATTERN && s1.contains("%")) {
            t1 = Narsese.term(s1, false);
            t1 = PatternTermBuilder.rule((anon1 ? a.put(t1) : t1).normalize());
        } else {
            t1 = Narsese.term(s1, true);
            if (anon1) t1 = a.put(t1).normalize();
        }


        assertNotNull(t1);
        assertNotNull(t2);


        Set<Term> vars = ((Compound) t1).recurseSubtermsToSet(type);
        vars.addAll(((Compound) t2).recurseSubtermsToSet(type));
        int n1 = vars.size();


        final boolean[] termuted = {false};
        AtomicBoolean subbed = new AtomicBoolean(false);

        Unify sub = new Unify(type, new XorShift128PlusRandom(rngSeed), NAL.unify.UNIFICATION_STACK_CAPACITY) {

            @Override
            protected void matches() {
                if (!termutes.isEmpty())
                    termuted[0] = true;
                super.matches();
            }

            @Override
            public boolean match() {

                if (shouldSub) {
                    final int[] matched = {0};
                    this.xy.forEachVersioned((k, v) -> {
                        if (var(k.op())) {
                            assertNotNull(v);
                            matched[0]++;
                        }
                        return true;
                    });

                    if (matched[0] == n1) {
                        subbed.set(true);

                    } /*else {
                            System.out.println("incomplete:\n\t" + xy);
                        }*/


                } else {

                    assertTrue(n1 > xy.size(), "why matched?: " + xy);

                }

                return true;
            }
        };


        //System.out.println("unify: " + t1 + " , \t" + t2);
        sub.setTTL(INITIAL_TTL);
        boolean u = sub.unify(t1, t2);
        if (!termuted[0])
            assertEquals(shouldSub, u);

        assertEquals(shouldSub, subbed.get());

        return sub;


    }


    @Test
    void unificationP0() {
        test(Op.VAR_PATTERN,
                "<%A ==> %B>",
                "<<a --> A> ==> <b --> B>>",
                true
        );
    }

    @Test
    void unificationP1() {
        test(Op.VAR_DEP,
                "<(#1,#1) --> wu>",
                "<(a,b) --> wu>",
                false
        );
    }

    @Test
    void unificationP2() {
        test(Op.VAR_DEP,
                "<(#1,c) --> wu>",
                "<(a,b) --> wu>",
                false
        );
    }

    @Test
    void unificationP3() {
        test(Op.VAR_PATTERN,
                "<(%1,%1,#1) --> wu>",
                "<(lol,lol2,#1) --> wu>",
                false
        );
    }

    @Test
    void unificationQ3() {
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
    void unificationP5() {
        test(Op.VAR_DEP,
                "<#x --> lock>",
                "<{lock1} --> lock>",
                true
        );
    }


    @Test
    void pattern_trySubs_Dep_Var() {
        test(Op.VAR_PATTERN,
                "<%A ==> %B>",
                "<<#1 --> A> ==> <?1 --> B>>",
                true);
    }

    @Test
    void pattern_trySubs_Var_2_parallel() {
        test(Op.VAR_QUERY,
                "(&&,<(?1,x) --> on>,<(SELF,#2) --> at>)",
                "(&&,<({t002},x) --> on>,<(SELF,#1) --> at>)",
                true);
    }

    @Test
    void pattern_trySubs_Var_2_product_and_common_depvar_bidirectional() {
        test(Op.VAR_DEP,
                "(<(#1,x) --> on>,<(SELF,x) --> at>)",
                "(<(SELF,x) --> on>,<(#1,x) --> at>)",
                true);


    }

    @Test
    void pattern_trySubs_2_product() {
        test(Op.VAR_QUERY,
                "(on(?1,x),     at(SELF,x))",
                "(on({t002},x), at(SELF,x))",
                true);
    }

    @Test
    void pattern_trySubs_Dep_Var_2_product() {
        test(Op.VAR_DEP,
                "(<(#1,x) --> on>,<(SELF,x) --> at>)",
                "(<({t002},x) --> on>,<(SELF,x) --> at>)",
                true);
    }

    @Test
    void pattern_trySubs_Indep_Var_2_set() {
        test(Op.VAR_DEP,
                "{<(#1,x) --> on>,<(SELF,x) --> at>}",
                "{<({t002},x) --> on>,<(SELF,x) --> at>}",
                true);
    }

    @Test
    void pattern_trySubs_Indep_Var_2_set2() {
        test(Op.VAR_DEP,
                "{<(#1,x) --> on>,<(SELF,x) --> at>}",
                "{<(z,x) --> on>,<(SELF,x) --> at>}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setSimple() {
        test(Op.VAR_PATTERN,
                "{%1,y}",
                "{z,y}",
                true);

    }


    @Test
    void pattern_trySubs_Pattern_Var_2_setSimpler() {
        test(Op.VAR_PATTERN,
                "{%1}",
                "{z}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>,y}",
                "{<(z,x) --> on>,y}",
                true);
    }


    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_1() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>,<x-->y>}",
                "{<(z,x) --> on>,<x-->y>}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_2() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>,(a,b)}",
                "{<(z,x) --> on>,(a,b)}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_3() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>, c:(a)}",
                "{<(z,x) --> on>, c:(a)}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_4() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>, c:a:b}",
                "{<(z,x) --> on>, c:a:b}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_5() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>, c:(a,b)}",
                "{<(z,x) --> on>, c:(a,b)}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_5_n() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>, c:(a,b)}",
                "{<(z,x) --> on>, c:(b,a)}",
                false);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_5_1() {
        test(Op.VAR_PATTERN,
                "{ on(%1,x), c:(a && b)}",
                "{ on(z,x), c:(a && b) }",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_5_c() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>, c:{a,b}}",
                "{<(z,x) --> on>, c:{a,b}}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_5_c1() {
        test(Op.VAR_PATTERN,
                "{<(z,%1) --> on>, c:{a,b}}",
                "{<(z,x) --> on>, c:{a,b}}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_5_c2() {
        test(Op.VAR_PATTERN,
                "{<(%1, z) --> on>, w:{a,b,c}}",
                "{<(x, z) --> on>, w:{a,b,c}}",
                true);
    }


    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_5_s() {

        test(Op.VAR_PATTERN,
                "{<{%1,x} --> on>, c:{a,b}}",
                "{<{z,x} --> on>, c:{a,b}}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_5_r() {
        test(Op.VAR_PATTERN,
                "{on:{%1,x}, c}",
                "{on:{z,x}, c}",
                true);
    }


    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex1() {
        test(Op.VAR_PATTERN,
                "{%1,<(SELF,x) --> at>}",
                "{z,<(SELF,x) --> at>}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex2() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>,<(SELF,x) --> at>}",
                "{<(z,x) --> on>,<(SELF,x) --> at>}",
                true);
    }

    @Test
    void pattern_trySubs_Dep_Var_2_set() {
        test(Op.VAR_DEP,
                "{<(#1,x) --> on>,<(SELF,x) --> at>}",
                "{<({t002},x) --> on>,<(SELF,x) --> at>}",
                true);
    }


    @Test
    void pattern_trySubs_Indep_Var_32() {
        test(Op.VAR_PATTERN,
                "<%A ==> <(SELF,$1) --> reachable>>",
                "<(&&,<($1,#2) --> on>,<(SELF,#2) --> at>) ==> <(SELF,$1) --> reachable>>",
                true);
    }

    @Test
    void pattern_trySubs_set3() {
        test(Op.VAR_PATTERN,
                "{%1,%2,%3}",
                "{a,b,c}",
                true);
    }


    @Test
    void pattern_trySubs_set2_1() {
        test(Op.VAR_PATTERN,
                "{%1,b}", "{a,b}",
                true);
    }

    @Test
    void pattern_trySubs_set2_2() {
        test(Op.VAR_PATTERN,
                "{a,%1}", "{a,b}",
                true);
    }

    @Test
    void pattern_trySubs_set3_1_b() {
        test(Op.VAR_PATTERN,
                "{a,b,c}",
                "{%1,b,%2}",
                true);
    }

    @Test
    void pattern_trySubs_set3_1_b_reverse() {
        test(Op.VAR_PATTERN,
                "{%1,b,%2}",
                "{a,b,c}",
                true);
    }

    @Test
    void pattern_trySubs_set3_1_b_commutative_inside_statement() {
        test(Op.VAR_PATTERN,
                "({a,b,c} --> d)",
                "({%1,b,%2} --> %3)",
                true);
    }

    @Test
    void pattern_trySubs_set3_1_statement_of_specific_commutatives() {
        test(Op.VAR_PATTERN,
                "<{a,b} --> {c,d}>",
                "<{%1,b} --> {c,%2}>",
                true);
    }

    @Test
    void pattern_trySubs_set3_1_statement_of_specific_commutatives_reverse() {
        test(Op.VAR_PATTERN,
                "<{%1,b} --> {c,%2}>",
                "<{a,b} --> {c,d}>",
                true);
    }

    @Test
    void pattern_trySubs_set3_1_c() {
        test(Op.VAR_PATTERN,
                "{a,b,c}",
                "{%1,%2,c}",
                true);
    }

    @Test
    void pattern_trySubs_set3_1_c_reverse() {
        test(Op.VAR_PATTERN,
                "{%1,%2,c}",
                "{a,b,c}",
                true);
    }

    @Test
    void pattern_trySubs_set4() {
        test(Op.VAR_PATTERN,
                "{%1,%2,%3,%4}",
                "{a,b,c,d}",
                true);
    }


    @Test
    void impossibleMatch1() {
        test(Op.VAR_DEP,
                "(a,#1)",
                "(b,b)",
                false);
    }

    @Test
    void notUnifyingType1() {
        test(Op.VAR_INDEP, "(x,$1)", "(x,y)", true);

        test(Op.VAR_DEP, "(x,$1)", "(x,$1)", true);
        test(Op.VAR_DEP, "(x,$1)", "(x,y)", false);
        test(Op.VAR_DEP, "(x,y)", "(x,$1)", false);
    }


    @Test
    void patternSimilarity1() {
        test(Op.VAR_PATTERN,
                "<%1 <-> %2>",
                "<a <-> b>",
                true);
    }

    @Test
    void patternNAL2Sample() {
        test(Op.VAR_PATTERN,
                "(<%1 --> %2>, <%2 --> %1>)",
                "(<bird --> {?1}>, <bird --> swimmer>)",
                false);
    }

    @Test
    void patternNAL2SampleSim() {
        test(Op.VAR_PATTERN,
                "(<%1 <-> %2>, <%2 <-> %1>)",
                "(<bird <-> {?1}>, <bird <-> swimmer>)",
                false);
    }

    @Test
    void patternLongSeq_NO_1() {
        test(Op.VAR_PATTERN,
                "(a,b,c,d,e,f,g,h,j)",
                "(x,b,c,d,e,f,g,h,j)",
                false);
    }

    @Test
    void patternLongSeq_NO_2() {
        test(Op.VAR_PATTERN,
                "(a,b,c,d,e,f,g,h,j)",
                "(a,b,c,d,e,f,g,h,x)",
                false);
    }


    @Test
    void ellipsisCommutive1a() {
        test(Op.VAR_PATTERN,
                "{%X..+}",
                "{a}", true);
    }

    @Test
    void ellipsisCommutive1b() {
        test(Op.VAR_PATTERN,
                "{a, %X..+}",
                "{a}", false);
    }

    @Test
    void ellipsisCommutive1c() {
        test(Op.VAR_PATTERN,
                "{a, %X..*}",
                "{a}", true);
    }

    @Test
    void ellipsisCommutive2a() {

        test(Op.VAR_PATTERN,
                "{a, %X..+}",
                "{a, b}", true);
    }

    @Test
    void ellipsisCommutive2b() {
        test(Op.VAR_PATTERN,
                "{%X..+, a}",
                "{a, b, c, d}", true);
    }

    @Test
    void ellipsisCommutive2c() {
        test(Op.VAR_PATTERN,
                "{a, %X..+, e}",
                "{a, b, c, d}", false);
    }

    @Test
    void ellipsisLinearOneOrMoreAll() {
        test(Op.VAR_PATTERN,
                "(%X..+)",
                "(a)", true);
    }

    @Test
    void ellipsisLinearOneOrMoreSuffix() {
        test(Op.VAR_PATTERN,
                "(a, %X..+)",
                "(a, b, c, d)", true);
    }

    @Test
    void ellipsisLinearOneOrMoreSuffixNoneButRequired() {
        test(Op.VAR_PATTERN,
                "(a, %X..+)",
                "(a)", false);
    }

    @Test
    void ellipsisLinearOneOrMorePrefix() {
        test(Op.VAR_PATTERN,
                "(%X..+, a)",
                "(a, b, c, d)", false);
    }

    @Test
    void ellipsisLinearOneOrMoreInfix() {
        test(Op.VAR_PATTERN,
                "(a, %X..+, a)",
                "(a, b, c, d)", false);
    }

    @Test
    void ellipsisLinearZeroOrMore() {
        test(Op.VAR_PATTERN,
                "(a, %X..*)",
                "(a)", true);
    }


    @Test
    void ellipsisLinearRepeat1() {
        test(Op.VAR_PATTERN,
                "((a, %X..+), %X..+)",
                "((a, b, c, d), b, c, d)", true);
    }

    @Test
    void ellipsisLinearRepeat2() {
        test(Op.VAR_PATTERN,
                "((a, %X..+), (z, %X..+))",
                "((a, b, c, d), (z, b, c, d))", true);
    }


    @Test
    void ellipsisCommutiveRepeat2_a() {

        test(Op.VAR_PATTERN,
                "{{a, %X..+}, {z, %X..+}}",
                "{{a, b, c, d}, {z, b, c, d}}", true);
    }

    @Test
    void ellipsisCommutiveRepeat2_aa() {

        test(Op.VAR_PATTERN,
                "({a, %X..+}, {z, %X..+})",
                "({a, b, c, d}, {z, b, c, d})", true);
    }

    @Test
    void ellipsisCommutiveRepeat2_aa_mismatch() {
        test(Op.VAR_PATTERN,
                "({a, %X..+}, {z, b, %X..+})",
                "({a, b, c, d}, {z, b, c, d})", false);
    }

    @Test
    void ellipsisCommutiveRepeat2_set() {
        test(Op.VAR_PATTERN,
                "({a, %X..+, %B}, {z, %X..+, %A})",
                "({a, b, c, d}, {z, b, c, d})", true);

        test(Op.VAR_PATTERN,
                "{{a, %X..+, %B}, {z, %X..+, %A}}",
                "{{a, b, c, d}, {z, b, c, d}}", true);
    }

    @Test
    void ellipsisCommutiveRepeat2_product() {
        test(Op.VAR_PATTERN,
                "({a, %X..+, %B}, {z, %X..+, %A})",
                "({a, b, c, d}, {z, b, c, d})", true);
    }

    @Test
    void ellipsisCommutiveRepeat2_c() {

        test(Op.VAR_PATTERN,
                "{{a, %X..+}, {b, %Y..+}}",
                "{{a, b, c}, {d, b, c}}", true);
    }

    @Test
    void ellipsisCommutiveRepeat2_cc() {

        test(Op.VAR_PATTERN,
                "{{a, %X..+}, {b, %Y..+}}",
                "{{a, b, c, d}, {z, b, c, d}}", true);
    }

    @Test
    void ellipsisLinearInner() {


        test(Op.VAR_PATTERN,
                "(a, %X..+, d)",
                "(a, b, c, d)", true);
    }


    @Test
    void ellipsisSequence() {

    }



    /**
     * this case is unrealistic as far as appearing in rules but it would be nice to get working
     */
    @Test
    void ellipsisCommutiveRepeat() {
        test(Op.VAR_PATTERN,
                "{{a, %X..+}, %X..+}",
                "{{a, b, c, d}, b, c, d}", true);
    }

    @Test
    void patternMatchesQuery1() {
        test(Op.VAR_PATTERN,
                "(<%1 <-> %2>, <%3 <-> %2>)",
                "(<x <-> ?1>, <y <-> ?1>)",
                true);
    }

    @Test
    void patternMatchesQuery2() {
        test(Op.VAR_PATTERN,
                "(<%1 <-> %2>, <%3 <-> %2>)",
                "(<bird <-> {?1}>, <bird <-> {?1}>)",
                true);
    }

    @Test
    void varDep2() {
        test(Op.VAR_DEP,
                "t:(#x | {#y})",
                "t:(x | {y})",
                true);
    }

    @Test
    void implXternal_pattern_var() {
        test(Op.VAR_PATTERN,
                "((--,%1) ==>+- %2)",
                "((--,(_1&&_2))==>_3)",
                true);
    }
    @Test
    void implXternal() {
        test(Op.VAR_PATTERN,
                "(x ==>+- y)",
                "(x ==>+1 y)",
                true);
    }
    @Test
    void conjXternal() {
        test(Op.VAR_PATTERN,
                "(x &&+- y)",
                "(x &&+1 y)",
                true);
    }

    @Test void testConjInConjConstantFail() {
        test(Op.VAR_PATTERN,
                "((_1&|_2) &&+5 ((--,_1)&|(--,_2)))",
                "(_1 &&+5 ((--,_1)&|_2))",
                false);
    }
    @Test void testConjInConjConstantFail2() {
        for (int a : new int[] { 5 }) {
            for (int b : new int[]{0, 5}) {
                Term x = $$("((_1 &&+5 ((--,_1)&|_2)) &&+5 (((--,_2)&|_3) &&+" + a + " (--,_3)))");
                Term y = $$("(_1 &&+" + b + " ((--,_1)&|_2))");
                UnifyAny u = new UnifyAny();
                boolean r = x.unify(y, u);
                assertFalse(r, ()->x + " " + y);
            }
        }
    }

    @Test
    void testConjInConj() {
        test(Op.VAR_PATTERN,
                "((_2(_1,%1) &&+- _3(%1)) &&+1 _4(%1))",
                "((_2(_1,_1) &| _3(_1)) &&+- _4(_1))",
                true);
    }

    @Test
    void testConSeqAgainstVarFwdRev() {

        test(Op.VAR_DEP,
                "(#1 &&+1 (x,y))",
                "((a) &&+1 (x,y))",
                true);

        test(Op.VAR_DEP,
                "((x,y) &&+1 #1)",
                "((a) &&+1 (x,y))",
                false);

        test(Op.VAR_DEP,
                "(#1 &&+- (x,y))",
                "((a) &&+1 (x,y))",
                true);

        test(Op.VAR_DEP,
                "((x,y) &&+1 #1)",
                "((x,y) &&+1 (a))",
                true);


        test(Op.VAR_DEP,
                "(#1 &&+1 (x,y))",
                "((x,y) &&+1 (a))",
                false);
        test(Op.VAR_DEP,
                "((x,y) &&+1 #1)",
                "((x,y) &&+1 (a))",
                true);
    }

    @Test
    void implConjInConjFail() {
        test(Op.VAR_PATTERN,
                "((_2(_1,%1)&|_3(%1)) &&+1 _4(%1))",
                "(_2(_1,_1) &&+- _4(_1))",
                false);
    }

    /*
    
    

    (B --> K), (&&,(#X --> L),(($Y --> K) ==> A)) |- substitute((&&, (#X --> L), A), $Y,B), (Truth:Deduction)
    (B --> K), (&&,(#X --> L),(($Y --> K) ==> (&&,A..+))) |- substitute((&&,(#X --> L),A..+),$Y,B), (Truth:Deduction)
     */


    private Unify testUnify(Compound a, Compound b, boolean matches) {

        AtomicBoolean matched = new AtomicBoolean(false);

        Unify f = new Unify(Op.VAR_QUERY, new XorShift128PlusRandom(1), NAL.unify.UNIFICATION_STACK_CAPACITY, 128) {

            @Override
            public boolean match() {

                assertTrue(matches);

                matched.set(true);


                assertEquals("{?1=a}", xy.toString());


                assertEquals(
                        "(a-->b) (?1-->b) -?>",
                        a + " " + b + " -?>"  /*+ " remaining power"*/);

                return true;
            }
        };

        f.unify(b, a);

        assertEquals(matched.get(), matches);

        return f;
    }

    @Test void testVariableOrdering() {
        UnifyAny u = new UnifyAny();
        Term a = $$("#1"), b = $$("(--,%1)");
        assertTrue( u.unify(a, b) );
        assertEquals("{%1=(--,#1)}", u.xy.toString()); //WRONG: "{#1=(--,%1)}",
    }
    @Test void testVariableOrderingReverseA() {
        UnifyAny u = new UnifyAny();
        Term a = $$("(--,#1)"), b = $$("%1");
        assertTrue( u.unify(a, b) );
        assertEquals("{%1=(--,#1)}", u.xy.toString()); //WRONG: "{#1=(--,%1)}",
    }

    @Test void testVariableOrderingReverseB() {
        UnifyAny u = new UnifyAny();
        Term a = $$("#1"), b = $$("(--,%1)");
        assertTrue( u.unify(b, a) );
        assertEquals("{%1=(--,#1)}", u.xy.toString()); //WRONG: "{#1=(--,%1)}",
    }

    //testUnifyNegativeMobiusStrip
}