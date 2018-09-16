package nars.derive;

import nars.$;
import nars.NARS;
import nars.Narsese;
import nars.derive.premise.DeriverRules;
import nars.derive.premise.PremiseDeriverCompiler;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.derive.premise.PremiseRuleSource;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 7/7/15.
 */
class PremiseRuleTest {


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
            



            PremiseRuleSource x = PremiseRuleSource.parse("A, A |- (A,A), (Belief:Intersection)");
            assertNotNull(x);
            
            
        }


        int vv = 19;
        {
            



            PremiseRuleSource x = PremiseRuleSource.parse("<A --> B>, <B --> A> |- <A <-> B>, (Belief:Intersection, Goal:Intersection)");
            
            assertEquals(vv, x.ref.volume());
            

        }
        {
            



            PremiseRuleSource x = PremiseRuleSource.parse("<A --> B>, <B --> A> |- <A <-> nonvar>, (Belief:Intersection, Goal:Intersection)");
            
            assertEquals(vv, x.ref.volume()); 
            
        }
        {
            



            PremiseRuleSource x = PremiseRuleSource.parse(" <A --> B>, <B --> A>, task(\"!\") |- <A <-> (A,B)>,  (Belief:Conversion, Punctuation:Question)");
            
            assertEquals(25, x.ref.volume());
            
        }








        
        



        PremiseRuleSource x = PremiseRuleSource.parse("(S --> M), (P --> M) |- (P <-> S), (Belief:Comparison,Goal:Desire)");
        
        
        assertEquals(vv, x.ref.volume());

    }















    @Test
    void testMinSubsRulePredicate() {
        

        DeriverRules d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(), "(A-->B),B,is(B,\"[\"),subsMin(B,2) |- (A-->dropAnySet(B)), (Belief:StructuralDeduction)"));
        d.printRecursive();
        assertNotNull(d);
    }

    @Test
    void testDoubleOnlyTruthAddsRequiresDoubleBelief() {

        DeriverRules d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(),
                "X,Y |- (X&&Y), (Belief:Intersection)"));

        d.printRecursive();
        assertEquals("((\".\"-->task),DoublePremise(\".\",(),()),can({0}))", d.what.toString());
    }
    @Test
    void testDoubleOnlyTruthAddsRequiresDoubleGoal() {

        DeriverRules d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(),
                "X,Y |- (X&&Y), (Goal:Intersection)"));

        d.printRecursive();
        assertEquals("((\"!\"-->task),DoublePremise((),\"!\",()),can({0}))", d.what.toString());
    }
    @Test
    void testDoubleOnlyTruthAddsRequiresDoubleBeliefOrGoal() {

        DeriverRules d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(),
                "X,Y |- (X&&Y), (Belief:Intersection,Goal:Intersection)"));

        d.printRecursive();
        assertEquals("((\".!\"-->task),DoublePremise(\".\",(),()),DoublePremise((),\"!\",()),can({0}))", d.what.toString());
    }
    @Test
    void testDoubleOnlyTruthAddsRequiresDoubleQuestionOverride() {

        DeriverRules d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(),
                "X,Y,task(\"?\") |- (X&&Y), (Punctuation:Belief,Belief:Intersection)"));

        d.printRecursive();
        assertEquals("((\"?\"-->task),DoublePremise((),(),\"?@\"),can({0}))", d.what.toString());
    }
    @Test
    void testInferQuestionPunctuationFromTaskRequirement() {

        DeriverRules d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(),
            "Y, Y, task(\"?\") |- (?1 &| Y), (Punctuation:Question)"
                ));
        d.printRecursive();
        assertEquals("((\"?\"-->task),can({0}))", d.what.toString());
    }

    @Test
    void testSubIfUnifyPrefilter() {

        DeriverRules d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(),
                "X,Y |- subIfUnifiesAny(what,X,Y), (Belief:Intersection)"));

        d.printRecursive();
        assertTrue(d.what.toString().contains("unifyPreFilter(")); //TODO this and other cases
    }

    @Test
    void testOpIsPreFilter() {
        DeriverRules d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(),
                "X,Y,is(X,\"*\") |- (X,Y), (Belief:Intersection)"));
        String s = d.what.toString();
        assertTrue( s.contains("Is(taskTerm,\"*\")"), ()->s);
    }

    @Test
    void testOpIsPreFilterSubPath() {
        DeriverRules d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(),
                "(Z,X),Y,is(X,\"*\") |- (X,Y), (Belief:Intersection)"));
        assertTrue( d.what.toString().contains("Is(taskTerm,\"*\",(1))"), ()-> d.what.toString());
    }
    @Test
    void testOpIsPreFilterSubPathNot() {
        DeriverRules d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(),
                "((Z),X),Y, --is(X,\"{\") |- (X,Y), (Belief:Intersection)"));
        String s = d.what.toString();
        assertTrue( s.contains("(--,Is(taskTerm,\"{\",(1)))"), ()->s);
    }
    @Test
    void testOpIsPreFilterSubPathRepeatIsOKButChooseShortestPath() {
        DeriverRules d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(),
                "((X),X),Y,is(X,\"*\") |- (X,Y), (Belief:Intersection)"));
        String s = d.what.toString();
        assertTrue( s.contains("Is(taskTerm,\"*\",(1))"), ()->s); //and not: (0,0)
    }

    @Test
    void testSubMinSuper() {
        DeriverRules d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(),
                "((X),X),Y,subsMin(Y,2) |- (X,Y), (Belief:Intersection)"));
        String s = d.what.toString();
        assertTrue( s.contains("SubsMin(beliefTerm,2)"), ()->s); //and not: (0,0)
    }

    @Test
    void testSubMinSub() {
        DeriverRules d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(),
                "((X),Z),Y,subsMin(X,2) |- (X,Y), (Belief:Intersection)"));
        String s = d.what.toString();
        assertTrue( s.contains("SubsMin(taskTerm,2,(0,0))"), ()->s); //and not: (0,0)
    }



    @Test
    void testTryFork() {

        DeriverRules d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(), "X,Y |- (X&&Y), (Belief:Intersection)", "X,Y |- (||,X,Y), (Belief:Union)"));
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
    void testConjWithEllipsisIsXternal() {

        DeriverRules d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(), "X,Y |- (&&,X,%A..+), (Belief:Analogy)", "X,Y |- (&&,%A..+), (Belief:Analogy)"));
            d.printRecursive();
    }
    @Test
    void printTermRecursive() throws Narsese.NarseseException {
        



        Compound y = (Compound) PremiseRuleSource.parse("(S --> P), --%S |- (P --> S), (Belief:Conversion)").ref;
        Terms.printRecursive(System.out, y);
    }













































































}