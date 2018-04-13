package nars.derive;

import nars.$;
import nars.NARS;
import nars.Narsese;
import nars.derive.rule.DeriveRuleSet;
import nars.derive.rule.DeriveRuleSource;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by me on 7/7/15.
 */
public class PremiseRuleTest {


    @Test
    public void testNoNormalization() throws Exception {

        String a = "<x --> #1>";
        String b = "<y --> #1>";
        Term p = $.p(
                Narsese.term(a),
                Narsese.term(b)
        );
        String expect = "((x-->#1),(y-->#1))";
        assertEquals(expect, p.toString());

//        Term pn = p.normalized();
//        assertEquals(expect, pn.toString());
//
//        p.set((Term)parse.term(a), parse.term(b));
//        assertEquals(expect, p.toString());
    }


    @Test
    public void testParser() throws Narsese.NarseseException {

        //NAR p = new NAR(new Default());

        assertNotNull(Narsese.term("<A --> b>"), "metaparser can is a superset of narsese");

        //

        assertEquals(0, Narsese.term("#A").complexity());
        assertEquals(1, Narsese.term("#A").volume());
        assertEquals(0, Narsese.term("%A").complexity());
        assertEquals(1, Narsese.term("%A").volume());

        assertEquals(3, Narsese.term("<A --> B>").complexity());
        assertEquals(1, Narsese.term("<%A --> %B>").complexity());

        {
            //        PremiseRule r = (PremiseRule) p.term(onlyRule);
//        return rule(
//                r
//        );
            DeriveRuleSource x = DeriveRuleSource.parse("A, A |- A, (Belief:Revision, Goal:Weak)");
            assertNotNull(x);
            //assertEquals("((A,A),(A,((Revision-->Belief),(Weak-->Desire))))", x.toString());
            // assertEquals(12, x.getVolume());
        }


        int vv = 19;
        {
            //        PremiseRule r = (PremiseRule) p.term(onlyRule);
//        return rule(
//                r
//        );
            DeriveRuleSource x = DeriveRuleSource.parse("<A --> B>, <B --> A> |- <A <-> B>, (Belief:Revision, Goal:Weak)");
            //x = PremiseRule.rule(x);
            assertEquals(vv, x.term().volume());
            //assertEquals("(((%1-->%2),(%2-->%1)),((%1<->%2),((Revision-->Belief),(Weak-->Desire))))", x.toString());

        }
        {
            //        PremiseRule r = (PremiseRule) p.term(onlyRule);
//        return rule(
//                r
//        );
            DeriveRuleSource x = DeriveRuleSource.parse("<A --> B>, <B --> A> |- <A <-> nonvar>, (Belief:Revision, Goal:Weak)");
            //x = PremiseRule.rule(x);
            assertEquals(vv, x.term().volume()); //same volume as previous block
            //assertEquals("(((%1-->%2),(%2-->%1)),((nonvar<->%1),((Revision-->Belief),(Weak-->Desire))))", x.toString());
        }
        {
            //        PremiseRule r = (PremiseRule) p.term(onlyRule);
//        return rule(
//                r
//        );
            DeriveRuleSource x = DeriveRuleSource.parse(" <A --> B>, <B --> A> |- <A <-> B>,  (Belief:Conversion, Punctuation:Belief)");
            //x = PremiseRule.rule(x);
            assertEquals(vv, x.term().volume());
            //assertEquals("(((%1-->%2),(%2-->%1)),((%1<->%2),((Conversion-->Belief),(Judgment-->Punctuation))))", x.toString());
        }


//        {
//            TaskRule x = p.termRaw("<<A --> b> |- (X & y)>");
//            assertEquals("((<A --> b>), ((&, X, y)))", x.toString());
//            assertEquals(9, x.getVolume());
//        }

        //and the first complete rule:
        //        PremiseRule r = (PremiseRule) p.term(onlyRule);
//        return rule(
//                r
//        );
        DeriveRuleSource x = DeriveRuleSource.parse("(S --> M), (P --> M) |- (P <-> S), (Belief:Comparison,Goal:Strong)");
        //x = PremiseRule.rule(x);
        //assertEquals("(((%1-->%2),(%3-->%2)),((%1<->%3),((Comparison-->Belief),(Strong-->Desire))))", x.toString());
        assertEquals(vv, x.term().volume());

    }

//    @Test
//    public void testNotSingleVariableRule1() throws Narsese.NarseseException {
//        //tests an exceptional case that should now be fixed
//
//        String l = "((B,P) --> ?X) ,(B --> A), task(\"?\") |- ((B,P) --> (A,P)), (Belief:BeliefStructuralDeduction, Punctuation:Judgment)";
//        new PremiseRuleSet(new PatternTermIndex(n), l).
//
//        Compound x = parse(l, i).normalizeRule(i);
//        assertNotNull(x);
//        assertNotNull(x.toString());
//        assertTrue(!x.toString().contains("%B"));
//    }


    @Test
    public void testMinSubsRulePredicate() {
        //test that the constraint on %2 being of size > 1 is testable in the Proto phase

        DeriveRules d = new DeriveRuleSet(NARS.shell(), "(A-->B),B,is(B,\"[\"),subsMin(B,2) |- (A-->dropAnySet(B)), (Belief:StructuralDeduction)").compile();
        d.printRecursive();
        assertNotNull(d);
    }

    @Test
    public void testDoubleOnlyTruthAddsRequiresBeliefPredicate() {

        DeriveRules d = new DeriveRuleSet(NARS.shell(), "X,Y |- (X&&Y), (Belief:Intersection)").compile();

        d.printRecursive();
        assertEquals("((\".!\"-->task),can({0}))", d.what.toString());
    }

    @Test
    public void testTryFork() {

        DeriveRules d = new DeriveRuleSet(NARS.shell(), "X,Y |- (X&&Y), (Belief:Intersection)", "X,Y |- (||,X,Y), (Belief:Union)").compile();
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

    @Test public void testConjWithEllipsisIsXternal() {

        DeriveRules d = new DeriveRuleSet(NARS.shell(), "X,Y |- (&&,X,%A..+), (Belief:Analogy)", "X,Y |- (&&,%A..+), (Belief:Analogy)").compile();
            d.printRecursive();
    }
    @Test
    public void printTermRecursive() throws Narsese.NarseseException {
        //        PremiseRule r = (PremiseRule) p.term(onlyRule);
//        return rule(
//                r
//        );
        Compound y = (Compound) DeriveRuleSource.parse("(S --> P), --%S |- (P --> S), (Belief:Conversion, Info:SeldomUseful)").term();
        Terms.printRecursive(System.out, y);
    }


//    @Test
//    public void testReifyPatternVariables() {
//        Default n = new Default(1024, 2, 3, 3);
//        //n.core.activationRate.setValue(0.75f);
//
//
//        Deriver.getDefaultDeriver().rules.reifyTo(n);
//        n.run(2);
//        n.forEachConcept(c -> {
//            assertEquals(0, c.term().varPattern());
//            c.term().recurseTerms((s, x) -> {
//                assertFalse(s.op() == Op.VAR_PATTERN);
//                //System.out.println(c + " " + s + " " + s.volume() + "," + s.getClass());
//            });
//            //System.out.println(c);
//        });
//
//    }

//    final PremiseRuleSet permuter = new PremiseRuleSet(new PatternIndex(n) { { deriverID = 0; }}, true);
//
//
//    @NotNull
//    public Set<PremiseRule> permute(PremiseRule preNorm, NAR nar) {
//
//        PatternIndex pi = new PatternIndex(nar);
//        Set<PremiseRule> ur;
//        permute(preNorm, "", pi, ur = $.newHashSet(1));
//        return ur;
//    }
//
//
//
//    @Test
//    public void testBackwardPermutations() throws Narsese.NarseseException {
//
//        //        PremiseRule r = (PremiseRule) p.term(onlyRule);
////        return rule(
////                r
////        );
//        Set<PremiseRule> s = permuter.permute(
//                parse("(A --> B), (B --> C), neq(A,C) |- (A --> C), (Belief:Deduction, Goal:Strong, Permute:Backward, Permute:Swap)")
//            );
//            assertNotNull(s);
//            System.out.println(Joiner.on('\n').join(s));
//
//            //total variations from the one input:
//            assertEquals(3, s.size());
//
//
//
//            //TODO
//            //String x = s.toString();
////            assertTrue(x.contains("(((%1-->%2),(%3-->%1),neq(%3,%2)),((%3-->%2),((DeductionX-->Belief),(StrongX-->Desire),(AllowBackward-->Derive))))"));
////            assertTrue(x.contains("(((%1-->%2),(%2-->%3),neq(%1,%3)),((%1-->%3),"));
////            //assertTrue(x.contains("(((%1-->%2),(%1-->%3),neq(%1,%2),task(\"?\")),((%3-->%2),"));
////            assertTrue(x.contains("(((%1-->%2),(%1-->%3),neq(%1,%3),task(\"?\")),((%2-->%3),"));
////            //assertTrue(x.contains("(((%1-->%2),(%3-->%2),neq(%3,%2),task(\"?\")),((%3-->%1),"));
////            assertTrue(x.contains("(((%1-->%2),(%3-->%2),neq(%1,%2),task(\"?\")),((%1-->%3),"));
//
//
//    }
//
//    @Test public void testSubstIfUnifies() throws Narsese.NarseseException {
//        //        PremiseRule r = (PremiseRule) p.term(onlyRule);
////        return rule(
////                r
////        );
//        PremiseRule r = parse("(Y --> L), ((Y --> S) ==> R), neq(L,S) |- substitute(((&&,(#X --> L),(#X --> S)) ==> R),Y,#X), (Belief:Induction, Goal:Induction)");
//        System.out.println(r);
//        System.out.println(r.source);
//        Set<PremiseRule> s = permuter.permute(r);
//        System.out.println(Joiner.on('\n').join(s));
//    }

}