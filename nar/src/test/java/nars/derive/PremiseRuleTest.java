package nars.derive;

import nars.$;
import nars.NARS;
import nars.Narsese;
import nars.derive.premise.PatternTermBuilder;
import nars.derive.rule.DeriverRules;
import nars.derive.rule.PremiseRule;
import nars.derive.rule.PremiseRuleCompiler;
import nars.derive.rule.PremiseRuleSet;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
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


            PremiseRule x = new PremiseRule("A, A |- (A,A), (Belief:Intersection)");
            assertNotNull(x);


        }


        int vv = 19;
        {


            PremiseRule x = new PremiseRule("<A --> B>, <B --> A> |- <A <-> B>, (Belief:Intersection, Goal:Intersection)");

            assertEquals(vv, x.ref.volume());


        }
        {


            PremiseRule x = new PremiseRule("<A --> B>, <B --> A> |- <A <-> nonvar>, (Belief:Intersection, Goal:Intersection)");

            assertEquals(vv, x.ref.volume());

        }
//        {
//
//
//
//
//            PremiseRuleSource x = PremiseRuleSource.parse(" <A --> B>, <B --> A>, task(\"!\") |- <A <-> (A,B)>,  (Belief:Intersection, Punctuation:Question)");
//
//            assertEquals(25, x.ref.volume());
//
//        }


        PremiseRule x = new PremiseRule("(S --> M), (P --> M) |- (P <-> S), (Belief:Comparison,Goal:Desire)");


        assertEquals(vv, x.ref.volume());

    }


    @Test
    void testMinSubsRulePredicate() {


        DeriverRules d = PremiseRuleCompiler.the(new PremiseRuleSet(NARS.shell(), "(A-->B),B,is(B,\"[\"),subsMin(B,2) |- (A-->dropAnySet(B)), (Belief:StructuralDeduction)"));
        d.printRecursive();
        assertNotNull(d);
    }

    @Test void testMissingPatternVar() {
        assertThrows(Throwable.class,
                ()->new PremiseRule("X,Y |- (X,Z), (Belief:Analogy)"));
    }

    @Test
    void testAutoXternalInConj() throws Narsese.NarseseException {
        assertConcPattern("X,Y |- (X && Y), (Belief:Analogy)", "(%1 &&+- %2)");
        assertConcPattern("X,Y |- (X,(X && Y)), (Belief:Analogy)", "(%1,(%1 &&+- %2))");
        assertConcPattern("(X,%A..+),Y |- (&&,X,%A..+), (Belief:Analogy)", "(%1 &&+- %2..+)");
        assertEquals(
            "( &&+- ,%1..+)", new PremiseRule("(%A..+),Y |- (&&,%A..+), (Belief:Analogy)").conclusion().toString());
    }


    @Test void testNoXternalInSect() throws Narsese.NarseseException {
        assertConcPattern("(X,Y), Z |- (Z-->(X&&Y)), (Belief:Intersection)", "(%3-->(%1&&%2))");
        assertConcPattern("(X,Y), Z |- (Z-->(X||Y)), (Belief:Intersection)", "(--,(%3-->((--,%1)&&(--,%2))))");

        assertConcPattern("(X,Y), Z |- (((X&&Y)-->Z),X,Y), (Belief:Intersection)", "(((%1&&%2)-->%3),%1,%2)");
        assertConcPattern("(X,Y), Z |- (((X||Y)-->Z),X,Y), (Belief:Intersection)", "(--,(((--,%1)&&(--,%2)))-->%3),%1,%2))");

    }

    private static void assertConcPattern(String r, String s) throws Narsese.NarseseException {
        assertEq(s, new PremiseRule(r).conclusion());
    }

    @Test
    void testDoubleOnlyTruthAddsRequiresDoubleBelief() {

        DeriverRules d = PremiseRuleCompiler.the(new PremiseRuleSet(NARS.shell(),
                "X,Y |- (X&&Y), (Belief:Intersection)"));

        d.printRecursive();
        assertTrue(d.what.toString().contains("DoublePremise"));
    }

    @Test
    void testDoubleOnlyTruthAddsRequiresDoubleGoal() {

        DeriverRules d = PremiseRuleCompiler.the(new PremiseRuleSet(NARS.shell(),
                "X,Y |- (X&&Y), (Goal:Intersection)"));

        d.printRecursive();
        assertTrue(d.what.toString().contains("DoublePremise"));
    }

    @Test
    void testDoubleOnlyTruthAddsRequiresDoubleBeliefOrGoal() {

        DeriverRules d = PremiseRuleCompiler.the(new PremiseRuleSet(NARS.shell(),
                "X,Y |- (X&&Y), (Belief:Intersection,Goal:Intersection)"));

        d.printRecursive();
        assertTrue(d.what.toString().contains("DoublePremise"));
    }

    @Test
    void testDoubleOnlyTruthAddsRequiresDoubleQuestionOverride() {

        DeriverRules d = PremiseRuleCompiler.the(new PremiseRuleSet(NARS.shell(),
                "X,Y,task(\"?\") |- (X&&Y), (Punctuation:Belief,Belief:Intersection)"));

        d.printRecursive();
        assertTrue(d.what.toString().contains("DoublePremise"));
    }

    @Test
    void testInferQuestionPunctuationFromTaskRequirement() {

        DeriverRules d = PremiseRuleCompiler.the(new PremiseRuleSet(NARS.shell(),
                "Y, Y, task(\"?\") |- (?1 &| Y), (Punctuation:Question)"
        ));
        d.printRecursive();
//        assertEquals("((\"?\"-->task),can({0}))", d.what.toString());
        assertTrue(d.what.toString().contains("PuncMap((0,0,\"?\",0))"));
    }

    @Test
    void testSubIfUnifyPrefilter() {

        DeriverRules d = PremiseRuleCompiler.the(new PremiseRuleSet(NARS.shell(),
                "X,Y |- unisubst(what,X,Y), (Belief:Intersection)"));

        d.printRecursive();
        String s = d.what.toString();
        assertTrue(s.contains("Unifiability("), () -> s); //TODO this and other cases
    }

    @Test
    void testOpIsPreFilter() {
        DeriverRules d = PremiseRuleCompiler.the(new PremiseRuleSet(NARS.shell(),
                "X,Y,is(X,\"*\") |- (X,Y), (Belief:Intersection)"));
        String s = d.what.toString();
        assertTrue(s.contains("Is(taskTerm,\"*\")"), () -> s);
    }

    @Test
    void testOpIsPreFilterSubPath() {
        DeriverRules d = PremiseRuleCompiler.the(new PremiseRuleSet(NARS.shell(),
                "(Z,X),Y,is(X,\"*\") |- (X,Y), (Belief:Intersection)"));
        assertTrue(d.what.toString().contains("IsHas"), () -> d.what.toString());
    }

    @Test
    void testOpIsPreFilterSubPathNot() {
        DeriverRules d = PremiseRuleCompiler.the(new PremiseRuleSet(NARS.shell(),
                "((Z),X),Y, --is(X,\"{\") |- (X,Y), (Belief:Intersection)"));
        String s = d.what.toString();
        assertTrue(s.contains("(--,Is("), () -> s);
    }

    @Test
    void testOpIsPreFilterSubPathRepeatIsOKButChooseShortestPath() {
        DeriverRules d = PremiseRuleCompiler.the(new PremiseRuleSet(NARS.shell(),
                "((X),X),Y,is(X,\"*\") |- (X,Y), (Belief:Intersection)"));
        String s = d.what.toString();
        assertTrue(s.contains("Is("), () -> s); //and not: (0,0)
    }

    @Test
    void testSubMinSuper() {
        DeriverRules d = PremiseRuleCompiler.the(new PremiseRuleSet(NARS.shell(),
                "((X),X),Y,subsMin(Y,2) |- (X,Y), (Belief:Intersection)"));
        String s = d.what.toString();
        assertTrue(s.contains("SubsMin(beliefTerm,2)"), () -> s); //and not: (0,0)
    }

    @Test
    void testSubMinSub() {
        DeriverRules d = PremiseRuleCompiler.the(new PremiseRuleSet(NARS.shell(),
                "((X),Z),Y,subsMin(X,2) |- (X,Y), (Belief:Intersection)"));
        String s = d.what.toString();
        assertTrue(s.contains("SubsMin("), () -> s); //and not: (0,0)
    }


    @Test
    void testTryFork() {

        DeriverRules d = PremiseRuleCompiler.the(new PremiseRuleSet(NARS.shell(), "X,Y |- (X&&Y), (Belief:Intersection)", "X,Y |- (||,X,Y), (Belief:Union)"));
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
        d.printRecursive();

    }


    @Test
    void printTermRecursive() throws Narsese.NarseseException {
        Compound y = (Compound) new PremiseRule("(S --> P), S |- (P --> S), (Belief:Conversion)").ref;
        Terms.printRecursive(System.out, y);
    }


}