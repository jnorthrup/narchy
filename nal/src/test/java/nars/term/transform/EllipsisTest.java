package nars.term.transform;

import jcog.math.random.XorShift128PlusRandom;
import nars.$;
import nars.Narsese;
import nars.Op;
import nars.Param;
import nars.derive.match.Ellipsis;
import nars.derive.match.EllipsisMatch;
import nars.derive.match.EllipsisOneOrMore;
import nars.derive.match.EllipsisZeroOrMore;
import nars.derive.rule.PremiseRule;
import nars.index.term.PatternIndex;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Set;

import static nars.$.$;
import static nars.Op.*;
import static nars.derive.match.Ellipsis.firstEllipsis;
import static nars.time.Tense.DTERNAL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 12/12/15.
 */
public class EllipsisTest {


    interface EllipsisTestCase {
        Compound getPattern();

        Term getResult() throws Narsese.NarseseException;

        @Nullable
        Term getMatchable(int arity) throws Narsese.NarseseException;

        default Set<Term> test(int arity, int repeats) throws Narsese.NarseseException {
            Set<Term> selectedFixed = $.newHashSet(arity);

            Term y = getMatchable(arity);
            assertTrue(!(y instanceof Bool));
            assertNotNull(y);
            assertTrue(y.isNormalized());
            //no variables in Y
            assertEquals(0, y.vars());
            assertEquals(0, y.varPattern());

            Term r =
                    //getResult();
                    new PatternIndex().pattern( getResult() );
            //assertTrue(r.isNormalized());

            Compound x = getPattern();


            //no unmatched variables
            ///x.forEach(xx -> { assertFalse(xx.toString() + " is var", xx instanceof Variable ); });

            Term ellipsisTerm = firstEllipsis(x);
            assertNotNull(ellipsisTerm);


            for (int seed = 0; seed < Math.max(1, repeats) /* enough chances to select all combinations */; seed++) {

                //AtomicBoolean matched = new AtomicBoolean(false);

                System.out.println(seed + ": " + x + " unify " + y + " => " + r);

                Unify f = new Unify(VAR_PATTERN, new XorShift128PlusRandom(1 + seed), Param.UnificationStackMax, 128) {

                    @Override
                    public void tryMatch() {
                        //System.out.println(x + "\t" + y + "\t" + this);


                        Term a = xy(ellipsisTerm);
                        if (a instanceof EllipsisMatch) {
                            EllipsisMatch varArgs = (EllipsisMatch) a;

                            //matched.set(true);

                            assertEquals(getExpectedUniqueTerms(arity), varArgs.subs());

                            Term u = xy(varArgs);
                            if (u == null) {
                                u = varArgs;
                            }

                            Set<Term> varArgTerms = $.newHashSet(1);
                            if (u instanceof EllipsisMatch) {
                                EllipsisMatch m = (EllipsisMatch) u;
                                m.forEach(varArgTerms::add);
                            } else {
                                varArgTerms.add(u);
                            }

                            assertEquals(getExpectedUniqueTerms(arity), varArgTerms.size());

                            testFurther(selectedFixed, this, varArgTerms);

                        } else {
                            assertNotNull(a);
                            //assertEquals("?", a);
                        }


                        //2. test substitution
                        Term s = Termed.termOrNull(r.transform(this));
                        if (s != null) {
                            //System.out.println(s);
                            if (s.varPattern() == 0)
                                selectedFixed.add(s);

                            assertEquals(0, s.varPattern(), s + " should be all subbed by " + this.xy);
                        }

                    }
                };

                f.unify(x, y, true);

//                assertTrue(f.toString() + " " + matched,
//                        matched.get());

            }


            return selectedFixed;

        }

        int getExpectedUniqueTerms(int arity);

        default void testFurther(Set<Term> selectedFixed, Unify f, Set<Term> varArgTerms) {

        }

        default void test(int arityMin, int arityMax, int repeats) throws Narsese.NarseseException {
            for (int arity = arityMin; arity <= arityMax; arity++) {
                test(arity, repeats);
            }
        }
    }

    abstract static class CommutiveEllipsisTest implements EllipsisTestCase {
        protected final String prefix;
        protected final String suffix;
        @NotNull
        protected final Compound p;
        public final String ellipsisTerm;

        protected CommutiveEllipsisTest(String ellipsisTerm, String prefix, String suffix) throws Narsese.NarseseException {
            this.prefix = prefix;
            this.suffix = suffix;
            this.ellipsisTerm = ellipsisTerm;
            p = (Compound) new PatternIndex().pattern(
                    getPattern(prefix, suffix)
            );
        }


        static String termSequence(int arity) {
            StringBuilder sb = new StringBuilder(arity * 3);
            for (int i = 0; i < arity; i++) {
                sb.append((char) ('a' + i));
                if (i < arity - 1)
                    sb.append(',');
            }
            return sb.toString();
        }

        @Nullable
        protected abstract Compound getPattern(String prefix, String suffix) throws Narsese.NarseseException;


        @NotNull
        @Override
        public Compound getPattern() {
            return p;
        }

        @Override
        public @Nullable Term getMatchable(int arity) throws Narsese.NarseseException {
            String s = termSequence(arity);
            String p = this.prefix;
            if ((arity == 0) && (p.endsWith(",")))
                p = p.substring(0, p.length()-1);
            return $(p + s + suffix);
        }
    }

    public static class CommutiveEllipsisTest1 extends CommutiveEllipsisTest {

        static final Term fixedTerm = $.varPattern(1);


        public CommutiveEllipsisTest1(String ellipsisTerm, String[] openClose) throws Narsese.NarseseException {
            super(ellipsisTerm, openClose[0], openClose[1]);
        }

        @Override
        public Set<Term> test(int arity, int repeats) throws Narsese.NarseseException {
            Set<Term> selectedFixed = super.test(arity, repeats);

            /** should have iterated all */
            assertEquals(arity, selectedFixed.size(), selectedFixed::toString);
            return selectedFixed;
        }

        @Override
        public int getExpectedUniqueTerms(int arity) {
            return arity - 1;
        }

        @Override
        public void testFurther(Set<Term> selectedFixed, @NotNull Unify f, @NotNull Set<Term> varArgTerms) {
            assertEquals(2, f.xy.size());
            Term fixedTermValue = f.xy(fixedTerm);
            assertNotNull(fixedTermValue, f.toString());
            assertTrue(fixedTermValue instanceof Atomic);
            assertFalse(varArgTerms.contains(fixedTermValue));
        }


        @NotNull
        @Override
        public Compound getPattern(String prefix, String suffix) throws Narsese.NarseseException {
            PatternIndex pi = new PatternIndex();
            Compound pattern = (Compound) Narsese.term(prefix + "%1, " + ellipsisTerm + suffix, true).term();
            return pattern;
        }


        @Override
        public @NotNull Term getResult() throws Narsese.NarseseException {
            final PatternIndex pi = new PatternIndex();
            return Narsese.term("<%1 --> (" + ellipsisTerm + ")>", true).normalize().term();
        }

    }

    /**
     * for testing zero-or-more matcher
     */
    static class CommutiveEllipsisTest2 extends CommutiveEllipsisTest {

        public CommutiveEllipsisTest2(String ellipsisTerm, String[] openClose) throws Narsese.NarseseException {
            super(ellipsisTerm, openClose[0], openClose[1]);
        }

        @Override
        public Set<Term> test(int arity, int repeats) throws Narsese.NarseseException {
            Set<Term> s = super.test(arity, repeats);
            Term the = s.isEmpty() ? null : s.iterator().next();
            assertNotNull(the);
            assertTrue(!the.toString().substring(1).isEmpty(), () -> the.toString() + " is empty");
            assertTrue(the.toString().substring(1).charAt(0) == 'Z', () -> the.toString() + " does not begin with Z");
            return s;
        }

        @Nullable
        @Override
        public Compound getPattern(String prefix, String suffix) throws Narsese.NarseseException {
            return $(prefix + ellipsisTerm + suffix);
        }


        @Override
        public @NotNull Term getResult() throws Narsese.NarseseException {
            String s = prefix + "Z, " + ellipsisTerm + suffix;
            Compound c = $(s);
            assertNotNull(c, s + " produced null compound");
            return c;
        }

        @Override
        public int getExpectedUniqueTerms(int arity) {
            return arity;
        }
    }

//    @Test
//    public void testEllipsisEqualityWithPatternVariable() {
//
//        @NotNull Ellipsis tt = Ellipsis.EllipsisPrototype.make(1,1);
//        @NotNull Ellipsis uu = Ellipsis.EllipsisPrototype.make(1,1);
//
//        assertEquals(tt, uu);
//        assertEquals(tt, $.v(VAR_PATTERN, 1));
//        assertNotEquals(tt, $.v(VAR_PATTERN, 2));
//        assertNotEquals(tt, $.v(VAR_DEP, 1));
//    }

    @Test
    public void testEllipsisOneOrMore() throws Narsese.NarseseException {
        String s = "%prefix..+";
        Term t = $(s);
        assertNotNull(t);
        //assertEquals(s, t.toString());
        //assertEquals("%prefix", t.target.toString());
        assertEquals(EllipsisOneOrMore.class, t.normalize((byte) 1).getClass());

        //assertEquals(t.target, $("%prefix")); //equality between target and itself
    }

    @Test
    public void testEllipsisZeroOrMore() throws Narsese.NarseseException {
        String s = "%prefix..*";
        Term t = $(s);
        assertNotNull(t);
        assertEquals(s, t.toString());
        //assertEquals("%prefix", t.target.toString());
        Term tn = t.normalize((byte) 1);
        assertEquals(EllipsisZeroOrMore.class, tn.getClass());
        assertEquals("%1..*", tn.toString());
        assertNotEquals($.varPattern(1), tn);
    }


//    @Test public void testEllipsisExpression() {
//        //TODO
//    }

    @NotNull
    public static String[] p(String a, String b) {
        return new String[]{a, b};
    }

    @Disabled @Test
    public void testVarArg0() throws Narsese.NarseseException {
        //String rule = "(%S --> %M), ((|, %S, %A..+ ) --> %M) |- ((|, %A, ..) --> %M), (Belief:DecomposePositiveNegativeNegative)";
        String rule = "(%S ==> %M), ((&&,%S,%A..+) ==> %M) |- ((&&,%A..+) ==> %M), (Belief:DecomposeNegativePositivePositive, Order:ForAllSame, SequenceIntervals:FromBelief)";

        Compound _x = $.$('<' + rule + '>');
        assertTrue(_x instanceof PremiseRule, _x.toString());
        PremiseRule x = (PremiseRule) _x;
        //System.out.println(x);
        x = x.normalize(new PatternIndex());
        //System.out.println(x);

        assertEquals(
                "(((%1==>%2),((%1&&%3..+)==>%2)),(((&&,%3..+)==>%2),((DecomposeNegativePositivePositive-->Belief),(ForAllSame-->Order),(FromBelief-->SequenceIntervals))))",
                x.toString()
        );

    }

    @Test public void testEllipsisMatchCommutive1_0a() throws Narsese.NarseseException {
        testSect("|");
    }
    @Test public void testEllipsisMatchCommutive1_0b() throws Narsese.NarseseException {
        testSect("&");
    }

    void testSect(String o) throws Narsese.NarseseException {
        new CommutiveEllipsisTest1("%2..+", p("(" + o + ",", ")")).test(2, 2, 4);
    }


    @Test
    public void testEllipsisMatchCommutive1_1() throws Narsese.NarseseException {
        new EllipsisTest.CommutiveEllipsisTest1("%2..+", p("{", "}")).test(2, 4, 4);
    }

    @Test
    public void testEllipsisMatchCommutive1_2() throws Narsese.NarseseException {
        new EllipsisTest.CommutiveEllipsisTest1("%2..+", p("[", "]")).test(2, 4, 4);
    }

    @Test
    public void testEllipsisMatchCommutive1_3() throws Narsese.NarseseException {
        new EllipsisTest.CommutiveEllipsisTest1("%2..+", p("(&&,", ")")).test(2, 4, 4);
    }

    @Test
    public void testEllipsisMatchCommutive1_3with() throws Narsese.NarseseException {
        new EllipsisTest.CommutiveEllipsisTest1("%2..+", p("(&&,x,", ")")).test(2, 4, 4);
    }


    @Test
    public void testEllipsisMatchCommutive2one_sete() throws Narsese.NarseseException {
        new EllipsisTest.CommutiveEllipsisTest2("%1..+", p("{", "}")).test(1, 5, 0);
    }

    @Test
    public void testEllipsisMatchCommutive2one_seti() throws Narsese.NarseseException {
        new EllipsisTest.CommutiveEllipsisTest2("%1..+", p("[", "]")).test(1, 5, 0);
    }

    @Test
    public void testEllipsisMatchCommutive2one_prod() throws Narsese.NarseseException {
        new EllipsisTest.CommutiveEllipsisTest2("%1..+", p("(Z,", ")")).test(1, 5, 0);
    }

    @Test
    public void testEllipsisMatchCommutive2empty_prod() throws Narsese.NarseseException {
        new EllipsisTest.CommutiveEllipsisTest2("%1..*", p("(Z,", ")")).test(0, 2, 0);
    }

    static void testCombinations(Compound _X, @NotNull Compound Y, int expect) {
        Compound X = (Compound) new PatternIndex().pattern(_X);

        for (int seed = 0; seed < 3 /*expect*5*/; seed++) {

            Set<String> results = $.newHashSet(0);

            Random rng = new XorShift128PlusRandom(seed);
            Unify f = new Unify(VAR_PATTERN, rng, Param.UnificationStackMax, 128) {
                @Override
                public void tryMatch() {
                    results.add(xy.toString());
                }
            };

            f.unify(X, Y, true);

            results.forEach(System.out::println);
            assertEquals(expect, results.size(), ()->"insufficient permutations for: " + X + " .. " + Y);
        }

    }

    @Test
    public void testEllipsisCombinatorics1() throws Narsese.NarseseException {
        //rule: ((&&,M,A..+) ==> C), ((&&,A,..) ==> C) |- M, (Truth:Abduction, Order:ForAllSame)
        testCombinations(
                $("(&&, %1..+, %2)"),
                $("(&&, x, y, z)"),
                3);
    }

    @Test
    public void testMatchAll2() throws Narsese.NarseseException {
        testCombinations(
                $("((|,%1,%2) --> (|,%2,%3))"),
                $("((|,bird,swimmer)-->(|,animal,swimmer))"),
                1);
    }

    @Test
    public void testMatchAll3() throws Narsese.NarseseException {
        testCombinations(
                $("((|,%X,%Z,%A) --> (|,%Y,%Z,%A))"),
                $("((|,bird,man, swimmer)-->(|,man, animal,swimmer))"),
                2);
    }

    @Test
    public void testRepeatEllipsisAWithoutEllipsis() throws Narsese.NarseseException {
        testCombinations(
                $("((|,%X,%Y) --> (|,%Y,%Z))"),
                $("((|,bird,swimmer)-->(|,animal,swimmer))"),
                1 /* weird */);
    }

    @Test
    public void testRepeatEllipsisA() throws Narsese.NarseseException {

        //should match the same with ellipsis
        testCombinations(
                $("((|,%X,%A..+) --> (|,%Y,%A..+))"),
                $("((|,bird,swimmer)-->(|,animal,swimmer))"),
                1);
    }

    @Test
    public void testRepeatEllipsisA2() throws Narsese.NarseseException {

        testCombinations(
                $("((%X,%A..+) --> (%Y,%A..+))"),
                $("((bird,swimmer)-->(animal,swimmer))"),
                1);
    }

    @Test
    public void testRepeatEllipsisA0() throws Narsese.NarseseException {
        testCombinations(
                $("((%A, %X) --> (%B, %X))"),
                $("((bird,swimmer)-->(animal,swimmer))"),
                1);
    }

    @Test
    public void testRepeatEllipsisB() throws Narsese.NarseseException {

        //should match the same with ellipsis
        testCombinations(
                $("((|,%X,%A..+) --> (|,%X,%B..+))"),
                $("((|,bird,swimmer)-->(|,animal,swimmer))"),
                1);
    }

    @Test
    public void testIntersection1() throws Narsese.NarseseException {
        testCombinations(
                $("(%M --> (|,%S,%A..+))"),
                $("(m-->(|,s,a))"),
                2);
        testCombinations(
                $("(%M --> (&,%S,%A..+))"),
                $("(m-->(&,s,a))"),
                2);
    }


    @Test
    public void testEllipsisInMinArity() {
        Atomic a = Atomic.the("a");
        Ellipsis b = new EllipsisOneOrMore($.varPattern(1));

        for (Op o : Op.values()) {
            if (o.minSize <= 1) continue;

            if (o.statement) continue;

            if (o != DIFFe && o != DIFFi) {
                assertEquals(a, o.the(DTERNAL, a), o + " with normal term");
            } else {
                assertEquals(Null, o.the(DTERNAL, a));
            }

            assertEquals(o.statement ? VAR_PATTERN : o,
                    o.the(DTERNAL, b).op(),
                    o + " with ellipsis not reduced");
        }
    }

    //TODO case which actually needs the ellipsis and not single term:
}