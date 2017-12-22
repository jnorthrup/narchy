package nars.nal;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.control.Derivation;
import nars.control.Deriver;
import nars.derive.DeriverRoot;
import nars.derive.TrieDeriver;
import nars.derive.instrument.DebugDerivationPredicate;
import nars.derive.rule.PremiseRule;
import nars.derive.rule.PremiseRuleSet;
import nars.index.term.PatternIndex;
import nars.term.Term;
import nars.term.Termed;
import nars.term.pred.PrediTerm;
import nars.test.TestNAR;
import net.byteseek.utils.collections.IdentityHashSet;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static nars.Op.QUEST;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 12/12/15.
 */
public class TrieDeriverTest {


    @Test
    public void printTrie() {

        Deriver.print(NARS.tmp(), System.out);
    }

//    @Test public void testRuleSerialization() {
//        byte[] x = IO.termToBytes( NARS.tmp(1).derivation().deriver );
//        assertTrue(x.length > 128 );
//        System.out.println(x.length + " bytes");
//
//        Term y = IO.termFromBytes(x);
//        assertTrue(y.volume() > 64 );
//        //System.out.println(y);
//
////        z = new PremiseRuleSet.rules()
//        //TrieDeriver.print(y);
//    }

    @Test
    public void testConclusionWithXTERNAL() {
        PatternIndex idx = new PatternIndex() {
            @Override
            public @Nullable Termed get(@NotNull Term x, boolean create) {
                Termed u = super.get(x, create);
                assertNotNull(u);
                if (u != x) {
                    System.out.println(x + " (" + x.getClass() + ")" + " -> " + u + " (" + u.getClass() + ")");
                    if (u.equals(x) && u.getClass().equals(x)) {
                        fail("\t ^ same class, wasteful duplicate");
                    }
                }
                return u;
            }
        };


        DeriverRoot d = the(new PremiseRuleSet(idx, NARS.shell(),
                "Y, Y |- (?1 &&+0 Y), ()",
                "X, X |- (?1 &&+- X), ()"
        ));

        System.out.println();

        d.printRecursive();

        System.out.println(d);

        String ds = d.toString();
        assertTrue(ds.contains("?2&|"));
        assertTrue(ds.contains("&&+- ?2"));


        //assertTrue("something at least got stored in the index", idx.size() > 16);

        //test that A..+ survives as an ellipsis
        //assertTrue(d.trie.getSummary().contains("..+"));
    }

    static DeriverRoot the(PremiseRuleSet r) {
        return TrieDeriver.the(r, (x) -> x);
    }


    public static PremiseRuleSet testCompile(String... rules) {
        return testCompile(false, rules);
    }

    public static PremiseRuleSet testCompile(boolean debug, String... rules) {

        assertNotEquals(0, rules.length);

        @NotNull PatternIndex pi = new PatternIndex();

        Stream<Pair<PremiseRule, String>> parsed = PremiseRuleSet.parse(Stream.of(rules));


        PremiseRuleSet src =
                new PremiseRuleSet(parsed, pi, NARS.shell());
        assertNotEquals(0, src.size());
        DeriverRoot d = the(src);

        if (debug) {
            //d.printRecursive();
            PrediTerm<Derivation> dd = d.what.transform(DebugDerivationPredicate::new);
        }

        Set<Term> byEquality = new HashSet();
        Set<Term> byIdentity = new IdentityHashSet();
        Consumer<Term> adder = a -> {
            byEquality.add(a);
            byIdentity.add(a);
        };
        d.what.recurseTerms(adder);
        d.can.recurseTerms(adder);

//        if (debug) {
//            System.out.println("           volume: " + d.volume());
//            System.out.println("       complexity: " + d.complexity());
//            System.out.println("terms by equality: " + byEquality.size());
//            System.out.println("terms by identity: " + byIdentity.size());
//
//            System.out.println("  values: " + n.causes.size());
//            n.causes.forEach(System.out::println);
//        }
//        assertTrue(n.causes.size() > 0);
//
//        if (debug) {
//            System.out.println();
//            TrieDeriver.print(d, System.out);
//        }

        return src;
    }

    @Test
    public void testCompile() {
        testCompile(
                "(A --> B), (B --> C), neqRCom(A,C) |- (A --> C), (Belief:Deduction, Goal:Strong)"
        );

    }

    @Test
    public void testCompilePatternOpSwitch() {
        testCompile(
                "(A --> B), C, task(\"?\") |- (A --> C), (Punctuation:Question)",
                "(A ==> B), C, task(\"?\") |- (A ==> C), (Punctuation:Question)"
        );

    }

    @Test
    public void testConclusionFold() throws Narsese.NarseseException {

        TestNAR t = test(64,
                "(A --> B), C, task(\"?\") |- (A --> C), (Punctuation:Question)",
                "(A --> B), C, task(\"?\") |- (A ==> C), (Punctuation:Question)"
        );
//        PrediTerm<Derivation> d = t.nar.derivation().deriver;
//        TrieDeriver.print(d);

        t.log().ask("(a-->b)").mustQuestion(64, "(a==>b)");

    }

    @Test
    public void testDeriveQuest() throws Narsese.NarseseException {

        @NotNull TestNAR t = test(64, "(P --> S), (S --> P), task(\"?\") |- (P --> S),   (Punctuation:Quest)")
                .log()
                .ask("b:a")
                .believe("a:b")
                .mustOutput(16, "b:a", QUEST);

        //ensure the fast conclusion substitute term was applied since the conclusion pattern is the task term
//        PrediTerm<Derivation> d = t.nar.derivation().deriver;
//        System.out.println(d);
//        TrieDeriver.print(d);
//        assertTrue(d.containsRecursively(Derivation._taskTerm));
    }

//    public static Set<Task> testDerivation(String[] rules, String task, String belief, int ttlMax) throws Narsese.NarseseException {
//        return testDerivation(rules, task, belief, ttlMax, false);
//    }


    private final List<TestNAR> tests = $.newArrayList();

    @AfterEach
    public void runTests() {
        tests.forEach(TestNAR::test);
    }

    public TestNAR test(int tlMax, String... rules) {
        return test(tlMax, false, rules);
    }

    public TestNAR test(int tlMax, boolean debug, String... rules) {
        NAR n = new NARS().deriverAddRules(rules).get();
        TestNAR t = new TestNAR(n);
        tests.add(t);
        return t;
    }

//    public static Set<Task> testDerivation(String[] rules, String task, String belief, int ttlMax, boolean debug) throws Narsese.NarseseException {
//        NAR n = NARS.tmp();
//
//        PrediTerm<Derivation> d = testCompile(debug, rules);
//        if (debug)
//            d = d.transform(DebugDerivationPredicate::new);
//
//        Set<Task> tasks = new LinkedHashSet();
//        n.onTask(tasks::add);
//        n.log();
//
//        Task t = Narsese.parse().task(task, n);
//        assertNotNull(t);
//        Term b = $.$(belief);
//        assertNotNull(b);
//
//        PrediTerm<Derivation> dd = d;
//        Iterable<? extends ITask> derived = new Premise(t, b, 0.5f, Collections.emptySet()) {
//            @Override
//            protected Derivation derivation(@NotNull NAR n) {
//                return n.derivation(dd);
//            }
//        }.run(n);
//        if (derived != null) {
//            derived.forEach(x ->
//                    tasks.add((Task) x)
//            );
//        }
//
//        return tasks;
//    }

//    @Test
//    public void testConstraints() {
//
//        String s = "B, (A ==> C), neq(A,B), notImpl(B) |- subIfUnifiesAny(C,A,B,strict), (Belief:DeductionRecursive)";
//
//        NAR n = NARS.tmp();
//        PrediTerm<Derivation> d = testCompile(false, s);
//        TrieDeriver.print(d, System.out);
//
//        assertTrue(d.subs(x -> x instanceof MatchConstraint) > 0);
//        assertTrue(d.subs(x -> x instanceof Taskify) > 0);
////        assertTrue("last element should be unify, not constraints or anything else: " + AndCondition.last(d),
////                AndCondition.last(d) instanceof UnifyTerm.UnifySubtermThenConclude);
//    }

//    @Test
//    public void testSubstIfUnifies1() throws Narsese.NarseseException {
//
//
//        TestNAR tester = test(64, false,
//                "(B --> K), (($X --> L) ==> (&&,(#Y --> K),%A..+)) |- substitute((($X --> L) ==>+- (&&,%A..+)),#Y,B), (Belief:AnonymousAnalogy)");
//
//        tester.believe("(&&,<#x --> lock>,(key:$y ==> open($y,#x)))"); //en("There is a lock that can be opened by every key.");
//        tester.believe("lock:{lock1}"); //en("Lock-1 is a lock.");
//        tester.log();
//        tester.mustBelieve(500, "<<$1 --> key> ==> open($1,{lock1})>", 1.00f,
//                0.81f);
//    }
//
//    @Test
//    public void testContrapositionWierdness() {
//
//        String s = "( (--,%S) ==> P), ( (--,%S) ==> P) |- ( (--,%P) ==>+- S),       (Belief:Contraposition)";
//
//        NAR n = NARS.tmp();
//        PrediTerm<Derivation> d = testCompile(false, s);
//        TrieDeriver.print(d, System.out);
//
//    }
//    @Test public void printRuleSet() {
//
////        List<PremiseRule> rr = d.rules.rules;
////        System.out.println(rr.size() + " rules");
////        rr.forEach(r -> {
////            System.out.println(r);
////        });
//
//        d.trie.costAnalyze((t) -> 1, System.out);
//    }


//    @Test
//    public void testNAL3Rule() {
//
//        NAR x = testRuleInputs(r1, r1Case);
//
//        assertEquals(1, ((TrieDeriver) (((Default) x).premiser.derv)).roots.length);
//
//        x.log().run(4);
//    }

//    @Test
//    public void testTriePreconditions0() {
//        TrieDeriver d = testRule(r0);
//        TrieDeriver e = testRule(r1);
//        TrieDeriver f = testRule(rN);
//
////        assertEquals(1, d.roots.length);
////        assertEquals(2, d.rules.size());
//
////
////        out.println(d.trie);
////
////        d.trie.printSummary();
////        d.derivationLinks.entries().forEach( System.out::println/*("\t" + k + "\t" + v)*/);
////
////        for (Term p : d.roots)
////            out.println(p);
//    }
//
//    @NotNull
//    public Default testRuleInputs(String rule, String... inputs) {
//        return testRuleInputs(new TrieDeriver(rule), inputs);
//    }

//    @NotNull
//    public Default testRuleInputs(@NotNull TrieDeriver d, String... inputs) {
//        return (Default) new Default() {
//            @NotNull
//            @Override
//            protected Deriver newDeriver() {
//                return d;
//            }
//        }.input(inputs);
//    }
//
//    @NotNull
//    public TrieDeriver testRule(String... rules) {
//        TrieDeriver d = new TrieDeriver(rules);
//        new Default() {
//            @NotNull
//            @Override
//            protected Deriver newDeriver() {
//                return d;
//            }
//        };
//        return d;
//    }


//    @Test public void testEllipsisRule() {
//        TrieDeriver d = testRule(
//            "(&&, A..+, X), B |- substituteIfUnifies((&&,A..+),\"#\",X,B), (Belief:AnonymousAnalogy, Goal:Strong, Order:ForAllSame, SequenceIntervals:FromTask)\n"
//        );
//        //test that A..+ survives as an ellipsis
//        assertTrue(d.trie.getSummary().contains("..+"));
//    }
//
//    @Test public void testConditionalAbductionRule() {
//
//        //test that ellipsis survives as an ellipsis after normalization no matter where it occurrs in a premise pattern
//
//        assertTrue(testRule(
//            "((X --> R) ==> Z), ((&&,A..+,(#Y --> B),(#Y --> R)) ==> Z) |- (X --> B), (Belief:Abduction)"
//        ).trie.getSummary().contains("..+"));
//        assertTrue(testRule(
//            "((X --> R) ==> Z), ((&&,A..*,(#Y --> B),(#Y --> R)) ==> Z) |- (X --> B), (Belief:Abduction)"
//        ).trie.getSummary().contains("..*"));
//        assertTrue(testRule(
//            "((X --> R) ==> Z), ((&&,(#Y --> B),(#Y --> R),A..*) ==> Z) |- (X --> B), (Belief:Abduction)"
//        ).trie.getSummary().contains("..*"));
//
//
//    }


//    @Test public void testBackwardsRules() {
//
//        TrieDeriver d = testRule(
//                "(A --> B), (B --> C), neq(A,C) |- (A --> C), (Truth:Deduction, Desire:Strong, Derive:AllowBackward)",
//                "(A --> B), (A --> C), neq(B,C) |- (C --> B), (Truth:Abduction, Desire:Weak, Derive:AllowBackward)",
//                "(A --> C), (B --> C), neq(A,B) |- (B --> A), (Truth:Induction, Desire:Weak, Derive:AllowBackward)",
//                "(A --> B), (B --> C), neq(C,A) |- (C --> A), (Truth:Exemplification, Desire:Weak, Derive:AllowBackward)"
//        );
//        d.trie.printSummary();
//
//        Default n = testRuleInputs(d,
//                "<bird --> swimmer>.", "<?1 --> swimmer>?"
//        );
//        n.log();
//        n.frame(64);
//
//        //TODO write test
//    }


//    @Test
//    public void testRuleStatistics() {
//        List<PremiseRule> R = d.rules.rules;
//        int registeredRules = R.size();
//
//
//        Frequency f = new Frequency();
//        R.forEach(f::addValue);
//        Iterator<Map.Entry<Comparable<?>, Long>> ii = f.entrySetIterator();
//        while (ii.hasNext()) {
//            Map.Entry<Comparable<?>, Long> e = ii.next();
//            if (e.getValue() > 1) {
//                System.err.println("duplicate: " + e);
//            }
//        }
//        out.println("total: " + f.getSumFreq() + ", unique=" + f.getUniqueCount());
//
//        HashSet<PremiseRule> setRules = Sets.newHashSet(R);
//
//        assertEquals("no duplicates", registeredRules, setRules.size());
//
//        Set<BoolPredicate> preconds = new HashSet();
//        int totalPrecond = 0;
//
//        out.println("total precondtions = " + totalPrecond + ", unique=" + preconds.size());
//
//        //preconds.forEach(p -> System.out.println(p));
//
//
//        //Set<TaskBeliefPair> ks = d.ruleIndex.keySet();
////        System.out.println("Patterns: keys=" + ks.size() + ", values=" + d.ruleIndex.size());
////        for (TaskBeliefPair pp : ks) {
////            System.out.println(pp + " x " + d.ruleIndex.get(pp).size());
////
////        }
//
//
//    }
//
////    @Test public void testPostconditionSingletons() {
//////        System.out.println(PostCondition.postconditions.size() + " unique postconditions " + PostCondition.totalPostconditionsRequested);
//////        for (PostCondition p : PostCondition.postconditions.values()) {
//////            System.out.println(p);
//////        }
////
////    }
//
//    @Test public void testPatternIndexContainsNoConcepts() {
//        PatternTermIndex p = d.rules.patterns;
//        assertTrue(ConceptBuilder.Null == p.conceptBuilder());
////        //out.println(p.data);
////        //out.println(p.atoms);
////        p.forEach(t -> {
////
////            if (!(t instanceof TransformConcept))
////                assertFalse( t instanceof Concept);
////
////            //test all subterms are in the pattern index too
////            t.term().recurseTerms((s, parent)->{
////                if (s instanceof Variable)
////                    return;
////
////
////                Termed sub = p.concept(s, false);
////                if (sub == null) {
////                    System.out.println("subterm " + s + " of " + parent + " not in PatternIndex");
////                }
////                assertNotNull(sub);
////            });
////
////            //out.println(t);
////        });
////        //System.out.println("compounds: "+ p.internings + " internings, " + p.size() + " unique");
//
//    }
//
//
//

//
//        FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

//
//        List<PremiseRule> R = Deriver.getDefaultDeriver().rules.rules;
//        for (PremiseRule r : R) {
//            byte barray[] = conf.asByteArray(r);
//            System.out.println(r + " "+ barray.length + " bytes");
//
//            Object decoded = conf.asObject(barray);
//            System.out.println("\t " + decoded);
//        }
//    }

//    @Test
//    public void testDerivationComparator() {
//
//        NARComparator c = new NARComparator(
//                new Default(),
//                new Default()
//        ) {
//
//
//        };
//        c.input("<x --> y>.\n<y --> z>.\n");
//
//
//
//        int cycles = 64;
//        for (int i = 0; i < cycles; i++) {
//            if (!c.areEqual()) {
//
//                /*System.out.println("\ncycle: " + c.time());
//                c.printTasks("Original:", c.a);
//                c.printTasks("Rules:", c.b);*/
//
////                System.out.println(c.getAMinusB());
////                System.out.println(c.getBMinusA());
//            }
//            c.frame(1);
//        }
//
//        System.out.println("\nDifference: " + c.time());
//        System.out.println("Original - Rules:\n" + c.getAMinusB());
//        System.out.println("Rules - Original:\n" + c.getBMinusA());
//
//    }

}