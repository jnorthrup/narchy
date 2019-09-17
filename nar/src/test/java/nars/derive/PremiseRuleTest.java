package nars.derive;

import nars.$;
import nars.NARS;
import nars.Narsese;
import nars.derive.action.NativePremiseAction;
import nars.derive.action.PatternPremiseAction;
import nars.derive.premise.PatternTermBuilder;
import nars.derive.rule.DeriverProgram;
import nars.derive.rule.PremiseRuleBuilder;
import nars.derive.rule.PremiseRuleSet;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.term.util.TermTest.assertEq;
import static nars.time.Tense.XTERNAL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 7/7/15.
 */
class PremiseRuleTest {

    @Test
    void testPatternCompoundWithXTERNAL() throws Narsese.NarseseException {
        Compound p = (Compound) PatternTermBuilder.patternify($.$("((x) ==>+- (y))")).term();
        assertEquals(XTERNAL, p.dt());

    }

    @Test
    void testPatternTermConjHasXTERNAL() {
        Term p = PatternTermBuilder.patternify($$("(x && y)"));
        assertEquals("(x &&+- y)", p.toString());

        Term r = PatternTermBuilder.patternify($$("(x || y)"));
        assertEquals(
                //"(--,((--,x) &&+- (--,y)))",
                "(x ||+- y)",
                //"(x ||+- y)", //TODO
                r.toString());
    }

    @Test
    void testNoNormalization() throws Exception {

        String a = "<x --> #1>";
        String b = "<y --> #1>";
        Term p = $.p(
                Narsese.term(a),
                Narsese.term(b)
        );
        String expect = "((x-->#1),(y-->#1))";
        assertEquals(expect, p.toString());


    }


    @Test
    void testParser() throws Narsese.NarseseException {


        assertNotNull(Narsese.term("<A --> b>"), "metaparser can is a superset of narsese");


        assertEquals(0, Narsese.term("#A").complexity());
        assertEquals(1, Narsese.term("#A").volume());
        assertEquals(0, Narsese.term("%A").complexity());
        assertEquals(1, Narsese.term("%A").volume());

        assertEquals(3, Narsese.term("<A --> B>").complexity());
        assertEquals(1, Narsese.term("<%A --> %B>").complexity());

        {


            PremiseRuleBuilder x = PatternPremiseAction.parse("A, A |- (A,A), (Belief:Intersection)");
            assertNotNull(x);


        }


        int vv = 19;
        {


            PremiseRuleBuilder x = PatternPremiseAction.parse("<A --> B>, <B --> A> |- <A <-> B>, (Belief:Intersection, Goal:Intersection)");

            assertEquals(vv, x.id.volume());


        }
        {


            PremiseRuleBuilder x = PatternPremiseAction.parse("<A --> B>, <B --> A> |- <A <-> nonvar>, (Belief:Intersection, Goal:Intersection)");

            assertEquals(vv, x.id.volume());

        }
//        {
//
//
//
//
//            PremiseRuleSource x = PremiseRuleSource.parse(" <A --> B>, <B --> A>, task(\"!\") |- <A <-> (A,B)>,  (Belief:Intersection, Punctuation:Question)");
//
//            assertEquals(25, x.id.volume());
//
//        }


        PremiseRuleBuilder x = PatternPremiseAction.parse("(S --> M), (P --> M) |- (P <-> S), (Belief:Comparison,Goal:Desire)");


        assertEquals(vv, x.id.volume());

    }


    @Test
    void testMinSubsRulePredicate() {


        DeriverProgram d = new PremiseRuleSet(NARS.shell(),
            "(A-->B),B,is(B,\"[\"),subsMin(B,2) |- (A-->dropAnySet(B)), (Belief:StructuralDeduction)").compile();
        assertNotNull(d);
    }

    @Test void MissingPatternVar() {
        assertThrows(Throwable.class,
                ()-> PatternPremiseAction.parse("X,Y |- (X,Z), (Belief:Analogy)").get());
    }

    @Test
    void testAutoXternalInConj() throws Narsese.NarseseException {
        assertConcPattern("X,Y |- (X && Y), (Belief:Analogy)", "(%1 &&+- %2)");
        assertConcPattern("X,Y |- (X,(X && Y)), (Belief:Analogy)", "(%1,(%1 &&+- %2))");
        assertConcPattern("(X,%A..+),Y |- (&&,X,%A..+), (Belief:Analogy)", "(%1 &&+- %2..+)");
        assertConcPattern("(%A..+),Y |- (&&,%A..+), (Belief:Analogy)", "( &&+- ,%1..+)");
    }

    @Test
    void deduplciateSame() throws Narsese.NarseseException {
        String s = "X,Y |- (X && Y), (Belief:Analogy)";
        PremiseRuleSet r = new PremiseRuleSet(NARS.shell(), s, s);
        assertEquals(1, r.rules.size());
    }

    @Test void NoXternalInSect() throws Narsese.NarseseException {
        assertConcPattern("(X,Y), Z |- (Z-->(X&&Y)), (Belief:Intersection)", "(%3-->(%1&&%2))");
        assertConcPattern("(X,Y), Z |- (Z-->(X||Y)), (Belief:Intersection)", "(--,(%3-->((--,%1)&&(--,%2))))");

        assertConcPattern("(X,Y), Z |- (((X&&Y)-->Z),X,Y), (Belief:Intersection)", "(((%1&&%2)-->%3),%1,%2)");
        assertConcPattern("(X,Y), Z |- (((X||Y)-->Z),X,Y), (Belief:Intersection)", "((--,(((--,%1)&&(--,%2))-->%3)),%1,%2)");

    }

    @Deprecated private static void assertConcPattern(String r, String s) throws Narsese.NarseseException {
        PatternPremiseAction p = PatternPremiseAction.parse(r);
        p.get();
        assertEq(s, p.termify.pattern);
    }

    @Test
    void testDoubleOnlyTruthAddsRequiresDoubleBelief() {

        DeriverProgram d = new PremiseRuleSet(NARS.shell(),
            "X,Y |- (X&&Y), (Belief:Intersection)").compile();

        assertTrue(d.what.toString().contains("DoublePremise"));
    }

    @Test
    void testDoubleOnlyTruthAddsRequiresDoubleGoal() {

        DeriverProgram d = new PremiseRuleSet(NARS.shell(),
            "X,Y |- (X&&Y), (Goal:Intersection)").compile();

        assertTrue(d.what.toString().contains("DoublePremise"));
    }

    @Test
    void testDoubleOnlyTruthAddsRequiresDoubleBeliefOrGoal() {
        assertRuleContains("X,Y |- (X&&Y), (Belief:Intersection,Goal:Intersection)",
                "DoublePremise(\".\",\"!\",())");
    }
    @Test
    void testDoubleOnlyForSinglePremiseQuestWithGoalPunc() {
        String r = "G, B, task(\"@\")  |- (polarize(G,task) && polarize(B,belief)), (Goal:DesireWeak, Punctuation:Goal)";
        assertRuleContains(r,
            "DoublePremise((),(),\"?@\")",
                "DoublePremise((),\"!\",())");

    }

    static void assertRuleContains(String r, String inc) {
        assertRuleContains(r, inc, null);
    }
    static void assertRuleContains(String r, @Nullable String inc, @Nullable String exc) {
        DeriverProgram d = new PremiseRuleSet(NARS.shell(), r).compile();
        //d.printRecursive();
        String rs = d.what.toString();
        if (inc!=null)
            assertTrue(rs.contains(inc), ()->rs);
        if (exc!=null)
            assertFalse(rs.contains(exc), ()->rs);
    }

    @Test
    void testDoubleOnlyTruthAddsRequiresDoubleQuestionOverride() {

        DeriverProgram d = new PremiseRuleSet(NARS.shell(),
            "X,Y,task(\"?\") |- (X&&Y), (Punctuation:Belief,Belief:Intersection)").compile();

        assertTrue(d.what.toString().contains("DoublePremise"));
    }

    @Test
    void testInferQuestionPunctuationFromTaskRequirement() {

        DeriverProgram d = new PremiseRuleSet(NARS.shell(),
            "Y, Y, task(\"?\") |- (?1 &| Y), (Punctuation:Question)"
        ).compile();
        //        assertEquals("((\"?\"-->task),can({0}))", d.what.toString());
        String w = d.what.toString();
        assertTrue(w.contains("PuncMap((0,0,\"?\",0))"));
    }

    @Test
    void testSubIfUnifyPrefilter() {

        DeriverProgram d = new PremiseRuleSet(NARS.shell(),
            "X,Y |- unisubst(what,X,Y), (Belief:Intersection)").compile();

        String s = d.what.toString();
        assertTrue(s.contains("Unifiability"), () -> s); //TODO this and other cases
    }

    @Test
    void testOpIsPreFilter() {
        DeriverProgram d = new PremiseRuleSet(NARS.shell(),
            "X,Y,is(X,\"*\") |- (X,Y), (Belief:Intersection)").compile();
        String s = d.what.toString();
        assertTrue(s.contains("Is(taskTerm,\"*\")"), () -> s);
    }

    @Test
    void testOpIsPreFilterSubPath() {
        DeriverProgram d = new PremiseRuleSet(NARS.shell(),
            "(Z,X),Y,is(X,\"*\") |- (X,Y), (Belief:Intersection)").compile();
        assertTrue(d.what.toString().contains("IsHas"), () -> d.what.toString());
    }

    @Test
    void testOpIsPreFilterSubPathNot() {
        DeriverProgram d = new PremiseRuleSet(NARS.shell(),
            "((Z),X),Y, --is(X,\"{\") |- (X,Y), (Belief:Intersection)").compile();
        String s = d.what.toString();
        assertTrue(s.contains("(--,Is("), () -> s);
    }

    @Test
    void testOpIsPreFilterSubPathRepeatIsOKButChooseShortestPath() {
        DeriverProgram d = new PremiseRuleSet(NARS.shell(),
            "((X),X),Y,is(X,\"*\") |- (X,Y), (Belief:Intersection)").compile();
        String s = d.what.toString();
        assertTrue(s.contains("Is("), () -> s); //and not: (0,0)
    }

    @Test
    void testSubMinSuper() {
        DeriverProgram d = new PremiseRuleSet(NARS.shell(),
            "((X),X),Y,subsMin(Y,2) |- (X,Y), (Belief:Intersection)").compile();
        String s = d.what.toString();
        assertTrue(s.contains("SubsMin(beliefTerm,2)"), () -> s); //and not: (0,0)
    }

    @Test
    void testSubMinSub() {
        DeriverProgram d = new PremiseRuleSet(NARS.shell(),
            "((X),Z),Y,subsMin(X,2) |- (X,Y), (Belief:Intersection)").compile();
        String s = d.what.toString();
        assertTrue(s.contains("SubsMin("), () -> s); //and not: (0,0)
    }


    @Test
    void testTryFork() {

        DeriverProgram d = new PremiseRuleSet(NARS.shell(),
            "X,Y |- (X&&Y), (Belief:Intersection)",
            "X,Y |- (||,X,Y), (Belief:Union)").compile();
/*
TODO - share unification state for different truth/conclusions
    TruthFork {
      (Union,_):
      (Intersection,_):
         (unify...
         (
      and {
        truth(Union,_)
        unify(task,%1)
        unify(belief,%2) {
          and {
            derive((||,%1,%2))
            taskify(3)
          }
        }
      }
      and {
        truth(Intersection,_)
        unify(task,%1)
        unify(belief,%2) {
 */

    }


    @Test
    void printTermRecursive() throws Narsese.NarseseException {
        Compound y = (Compound) PatternPremiseAction.parse("(S --> P), S |- (P --> S), (Belief:Conversion)").id;
        Terms.printRecursive(System.out, y);
    }

    @Test void testNativeDeriveAction_combined_with_MetaNAL() {

        DeriverProgram d = new PremiseRuleSet(NARS.shell())
            .add(
                new NativePremiseAction() {

                    {
                        taskPattern("(%A,%B,%C)");
                    }

                    @Override
                    protected void run(Derivation d) {
                        System.out.println("match: " + d);
                    }

                    @Override
                    protected float pri(Derivation d) {
                        return 0.5f;
                    }

                }
            )
            .add(
                "X,Y |- (X&&Y), (Belief:Intersection)",
                "{X},Y |- (X&&Y), (Belief:Deduction)",
                "{X},Y |- (||,X,Y), (Belief:Union)"
            ).compile();
        d.print();
    }

}