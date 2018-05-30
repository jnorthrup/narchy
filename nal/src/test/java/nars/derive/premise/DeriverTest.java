package nars.derive.premise;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.derive.deriver.MatrixDeriver;
import nars.term.Term;
import nars.term.Termed;
import nars.test.TestNAR;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.PrintStream;
import java.util.List;

import static nars.Op.QUEST;
import static nars.derive.Deriver.derivers;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 12/12/15.
 */
public class DeriverTest {

    public static void print(NAR n, PrintStream p) {
        derivers(n).forEach(d -> {
            p.println(d);
            d.rules.print(p);
            p.println();
        });
    }


    @Test
    public void printTrie() {

        print(NARS.tmp(), System.out);
    }














    @Test
    public void testConclusionWithXTERNAL() {
        NAR n = NARS.shell();
        PremisePatternIndex idx = new PremisePatternIndex(n) {
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



        PremiseDeriver d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(idx,
                "Y, Y |- (?1 &&+0 Y), ()",
                "X, X |- (?1 &&+- X), ()"
        ), null);

        System.out.println();

        d.printRecursive();

        System.out.println(d);

        String ds = d.toString();
        assertTrue(ds.contains("?2&|"));
        assertTrue(ds.contains("?2 &&+-"));


        

        
        
    }


    static PremiseDeriverRuleSet testCompile(String... rules) {
        return testCompile(false, rules);
    }

    static PremiseDeriverRuleSet testCompile(boolean debug, String... rules) {

        assertNotEquals(0, rules.length);


        PremiseDeriverRuleSet src = new PremiseDeriverRuleSet(NARS.shell(), rules);
        assertNotEquals(0, src.size());
        PremiseDeriver d = PremiseDeriverCompiler.the(src, null);

        if (debug) {
            
            
        }



























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

        TestNAR t = test(
                "(A --> B), C, task(\"?\") |- (A --> C), (Punctuation:Question)",
                "(A --> B), C, task(\"?\") |- (A ==> C), (Punctuation:Question)"
        );



        t.ask("(a-->b)").mustQuestion(64, "(a==>b)");

    }

    @Test
    public void testDeriveQuest() throws Narsese.NarseseException {

        @NotNull TestNAR t = test("(P --> S), (S --> P), task(\"?\") |- (P --> S),   (Punctuation:Quest)")
                .ask("b:a")
                .believe("a:b")
                .mustOutput(16, "b:a", QUEST);

        




    }






    private final List<TestNAR> tests = $.newArrayList();

    protected TestNAR test(String... rules) {
        NAR n = new NARS().get();
        new MatrixDeriver(new PremiseDeriverRuleSet(n, rules));
        TestNAR t = new TestNAR(n);
        tests.add(t);
        return t;
    }

    @AfterEach
    public void runTests() {
        tests.forEach(TestNAR::test);
    }





























































































































































































































































































































}