package nars.derive.meta;

import com.google.common.base.Joiner;
import nars.Narsese;
import nars.Op;
import nars.derive.rule.PremiseRule;
import nars.derive.rule.PremiseRuleSet;
import nars.index.term.PatternTermIndex;
import nars.term.Compound;
import org.junit.Test;

import java.util.Set;

import static nars.derive.rule.PremiseRuleSet.parse;
import static org.junit.Assert.*;

/**
 * Created by me on 7/7/15.
 */
public class PremiseRuleTest {


    static final Narsese p = Narsese.the();


    @Test
    public void testParser() throws Narsese.NarseseException {


        //NAR p = new NAR(new Default());

        assertNotNull("metaparser can is a superset of narsese", p.term("<A --> b>"));

        //

        assertEquals(0, p.term("#A").complexity());
        assertEquals(1, p.term("#A").volume());
        assertEquals(0, p.term("%A").complexity());
        assertEquals(1, p.term("%A").volume());

        assertEquals(3, p.term("<A --> B>").complexity());
        assertEquals(1, p.term("<%A --> %B>").complexity());

        {
            PremiseRule x = PremiseRule.rule("A, A |- A, (Belief:Revision, Goal:Weak)");
            assertNotNull(x);
            //assertEquals("((A,A),(A,((Revision-->Belief),(Weak-->Desire))))", x.toString());
            // assertEquals(12, x.getVolume());
        }


        int vv = 19;
        {
            PremiseRule x = PremiseRule.rule("<A --> B>, <B --> A> |- <A <-> B>, (Belief:Revision, Goal:Weak)");
            x = PremiseRule.rule(x);
            assertEquals(vv, x.volume());
            //assertEquals("(((%1-->%2),(%2-->%1)),((%1<->%2),((Revision-->Belief),(Weak-->Desire))))", x.toString());

        }
        {
            PremiseRule x = PremiseRule.rule("<A --> B>, <B --> A> |- <A <-> nonvar>, (Belief:Revision, Goal:Weak)");
            x = PremiseRule.rule(x);
            assertEquals(vv, x.volume()); //same volume as previous block
            //assertEquals("(((%1-->%2),(%2-->%1)),((nonvar<->%1),((Revision-->Belief),(Weak-->Desire))))", x.toString());
        }
        {
            PremiseRule x = PremiseRule.rule(" <A --> B>, <B --> A> |- <A <-> B>,  (Belief:Conversion, Punctuation:Belief)");
            x = PremiseRule.rule(x);
            assertEquals(vv, x.volume());
            //assertEquals("(((%1-->%2),(%2-->%1)),((%1<->%2),((Conversion-->Belief),(Judgment-->Punctuation))))", x.toString());
        }


//        {
//            TaskRule x = p.termRaw("<<A --> b> |- (X & y)>");
//            assertEquals("((<A --> b>), ((&, X, y)))", x.toString());
//            assertEquals(9, x.getVolume());
//        }

        //and the first complete rule:
        PremiseRule x = PremiseRule.rule("(S --> M), (P --> M) |- (P <-> S), (Belief:Comparison,Goal:Strong)");
        x = PremiseRule.rule(x);
        //assertEquals("(((%1-->%2),(%3-->%2)),((%1<->%3),((Comparison-->Belief),(Strong-->Desire))))", x.toString());
        assertEquals(vv, x.volume());

    }

    @Test
    public void testNotSingleVariableRule1() throws Narsese.NarseseException {
        //tests an exceptional case that should now be fixed

        PatternTermIndex i = new PatternTermIndex();

        String l = "((B,P) --> ?X) ,(B --> A), task(\"?\") |- ((B,P) --> (A,P)), (Belief:BeliefStructuralDeduction, Punctuation:Judgment)";
        Compound x = parse(l, i).normalizeRule(i);
        assertNotNull(x);
        assertNotNull(x.toString());
        assertTrue(!x.toString().contains("%B"));
    }

    @Test
    public void testPatternVarNormalization() throws Narsese.NarseseException {

        //Narsese p = Narsese.the();

        //TODO test combination of lowercase and uppercase pattern terms
//        TaskRule x = p.term("<<A --> b> |- (X & y)>");
//
//        assertEquals("((<%A --> b>), ((&, %X, y)))", x.toString());




        Compound y = PremiseRule.rule("(S --> P), (--,%S) |- (P --> S), (Belief:Conversion)");
        assertTrue(y.hasAny(Op.NEG));

        assertNotNull(y);

        PatternTermIndex i = new PatternTermIndex();
        y = ((PremiseRule) y).normalizeRule(i);
        assertNotNull(y);
        PremiseRule.printRecursive(y);

        //assertEquals("(((%1-->%2),(--,%1)),((%2-->%1),((Conversion-->Belief))))", y.toString());
        assertEquals(10, y.complexity());
        assertEquals(15, y.volume());
    }


    @Test
    public void printTermRecursive() throws Narsese.NarseseException {
        Compound y = PremiseRule.rule("(S --> P), --%S |- (P --> S), (Belief:Conversion, Info:SeldomUseful)");
        PremiseRule.printRecursive(y);
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

    static final PremiseRuleSet permuter = new PremiseRuleSet(new PremiseRule[]{}, true);

    @Test
    public void testBackwardPermutations() throws Narsese.NarseseException {

        Set<PremiseRule> s = permuter.permute(
                    PremiseRule.rule("(A --> B), (B --> C), neq(A,C) |- (A --> C), (Belief:Deduction, Goal:Strong, Permute:Backward, Permute:Swap)")
            );
            assertNotNull(s);
            System.out.println(Joiner.on('\n').join(s));

            //total variations from the one input:
            assertEquals(4 /* negations */, s.size());



            //TODO
            //String x = s.toString();
//            assertTrue(x.contains("(((%1-->%2),(%3-->%1),neq(%3,%2)),((%3-->%2),((DeductionX-->Belief),(StrongX-->Desire),(AllowBackward-->Derive))))"));
//            assertTrue(x.contains("(((%1-->%2),(%2-->%3),neq(%1,%3)),((%1-->%3),"));
//            //assertTrue(x.contains("(((%1-->%2),(%1-->%3),neq(%1,%2),task(\"?\")),((%3-->%2),"));
//            assertTrue(x.contains("(((%1-->%2),(%1-->%3),neq(%1,%3),task(\"?\")),((%2-->%3),"));
//            //assertTrue(x.contains("(((%1-->%2),(%3-->%2),neq(%3,%2),task(\"?\")),((%3-->%1),"));
//            assertTrue(x.contains("(((%1-->%2),(%3-->%2),neq(%1,%2),task(\"?\")),((%1-->%3),"));


    }

    @Test public void testSubstIfUnifies() throws Narsese.NarseseException {
        PremiseRule r = PremiseRule.rule("(Y --> L), ((Y --> S) ==> R), neq(L,S) |- substitute(((&&,(#X --> L),(#X --> S)) ==> R),Y,#X), (Belief:Induction, Goal:Induction)");
        System.out.println(r);
        System.out.println(r.source);
        Set<PremiseRule> s = permuter.permute(r);
        System.out.println(Joiner.on('\n').join(s));
    }

}