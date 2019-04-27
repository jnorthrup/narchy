package nars.derive;

import nars.$;
import nars.NAL;
import nars.NAR;
import nars.NARS;
import nars.derive.impl.BatchDeriver;
import nars.derive.op.Occurrify;
import nars.derive.premise.DeriverRules;
import nars.derive.premise.PremiseDeriverCompiler;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.test.TestNAR;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.PrintStream;
import java.util.List;

import static nars.$.$$;
import static nars.Op.QUEST;
import static nars.derive.Deriver.derivers;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 12/12/15.
 */
class DeriverTest {

    private final List<TestNAR> tests = $.newArrayList();

    private static void print(NAL<NAL<NAR>> n, PrintStream p) {
        derivers(n).forEach(d -> {
            p.println(d);
            d.rules.printRecursive(p);
            p.println();
        });
    }

    private static PremiseDeriverRuleSet testCompile(String... rules) {
        return testCompile(false, rules);
    }

    private static PremiseDeriverRuleSet testCompile(boolean debug, String... rules) {

        assertNotEquals(0, rules.length);


        PremiseDeriverRuleSet src = new PremiseDeriverRuleSet(NARS.shell(), rules);
        assertNotEquals(0, src.size());
        DeriverRules d = PremiseDeriverCompiler.the(src);

        if (debug) {


        }


        return src;
    }

    @Test
    void printTrie() {

        print(NARS.tmp(), System.out);
    }

//    @Test
//    void testConclusionWithXTERNAL() {
//        NAR n = NARS.shell();
//        PatternIndex idx = new PatternIndex(n) {
//            @Override
//            public @Nullable Termed get(@NotNull Term x, boolean create) {
//                Termed u = super.get(x, create);
//                assertNotNull(u);
//                if (u != x) {
//                    System.out.println(x + " (" + x.getClass() + ")" + " -> " + u + " (" + u.getClass() + ")");
//                    if (u.equals(x) && u.getClass().equals(x)) {
//                        fail("\t ^ same class, wasteful duplicate");
//                    }
//                }
//                return u;
//            }
//        };
//
//
//        PremiseDeriver d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(idx,
//                "Y, Y, task(\"?\") |- (?1 &| Y), (Punctuation:Question)",
//                "X, X, task(\"?\") |- (?1 &&+- X), (Punctuation:Question)"
//        ));
//
//
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        d.printRecursive(new PrintStream(baos));
//
//
//        String ds = new String(baos.toByteArray());
//        System.out.println(ds);
//        assertTrue(ds.contains("?2&|"));
//        assertTrue(ds.contains("?2 &&+-"));
//
//
//    }

    @Test
    void testAmbiguousPunctuation() {
        assertThrows(Throwable.class, () -> {
            PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(),
                    "Y, Y |- (?1 &| Y), ()"
            ));
        });
    }

    @Test
    void testCompile() {
        testCompile(
                "(A --> B), (B --> C), neqRCom(A,C) |- (A --> C), (Belief:Deduction, Goal:Desire)"
        );

    }

    @Test
    void testCompilePatternOpSwitch() {
        testCompile(
                "(A --> B), C, task(\"?\") |- (A --> C), (Punctuation:Question)",
                "(A ==> B), C, task(\"?\") |- (A ==> C), (Punctuation:Question)"
        );

    }

    @Test
    void testConclusionFold() {

        TestNAR t = test(
                "(A --> B), C, task(\"?\") |- (A --> C), (Punctuation:Question)",
                "(A --> B), C, task(\"?\") |- (A ==> C), (Punctuation:Question)"
        );


        t.ask("(a-->b)")
                .mustQuestion(64, "(a ==>+- b)")
                .mustQuestion(64, "(a ==>+- a)")
        ;

    }

    @Test
    void testDeriveQuest() {

        int cycles = 16;
        test("(P --> S), (S --> P), task(\"?\") |- (P --> S),   (Punctuation:Quest)")
                .ask("b:a")
                .believe("a:b")
                .mustOutput(cycles, "b:a", QUEST)
                .run(cycles)
                ;
    }

    private TestNAR test(String... rules) {
        NAR n = new NARS().get();
        new BatchDeriver(new PremiseDeriverRuleSet(n, rules));
        n.synch();
        TestNAR t = new TestNAR(n);
        tests.add(t);
        return t;
    }

    @AfterEach
    void runTests() {
        tests.forEach(TestNAR::test);
    }

    @Deprecated @Test void testTemporal() {
        assertFalse(Occurrify.temporal($$("(x==>y)")));
        assertTrue(Occurrify.temporal($$("(x ==>+1 y)")));
    }

}