package jcog.grammar.parse;

import jcog.grammar.parse.tokens.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class GrammarTest {

    Grammar grammar;
    private boolean called = false;

    @BeforeEach
    public void init() {
        grammar = new Grammar("Test");
    }

    @Test
    public void name() {
        assertEquals("Test", grammar.getName());
    }

    @Test
    public void firstRuleIsStartRuleByDefault() {
        assertCheckFails();
        grammar.addRule("mystart", new Empty());
        grammar.check();
        assertTrue(grammar.parse("").isCompleteMatch());
    }

    private void assertCheckFails() {
        try {
            grammar.check();
            fail();
        } catch (GrammarException expected) {
        }
    }

    @Test
    public void otherStartruleThanFirst() {
        grammar.addRule("nostart", new Empty());
        grammar.addRule("mystart", new RuleReference("nostart", grammar));
        grammar.markAsStartRule("mystart");
        grammar.check();
    }

    @Test
    public void grammarWithSingleRule() {
        assertNull(grammar.getRule("mystart"));
        grammar.addRule("mystart", new Empty());
        assertTrue(grammar.getRule("mystart") instanceof Empty);

        assertTrue(grammar.parse("").isCompleteMatch());
        assertFalse(grammar.parse("hello").isCompleteMatch());
    }

    @Test
    public void addingAssemblers() {
        class MyTarget implements PubliclyCloneable<MyTarget> {
            @Override
            public MyTarget clone() {
                return this;
            }
        }
        final MyTarget target = new MyTarget();
        assertNull(grammar.getRule("mystart"));
        grammar.addRule("mystart", new Empty());
        grammar.addAssembler("mystart", new IAssembler() {
            public void accept(Assembly a) {
                a.setTarget(target);
            }
        });

        assertSame(target, grammar.parse("").getTarget());
    }


    @Test
    public void assemblersCanOnlyBeAddedToExistingRules() {
        assertThrows(GrammarException.class, () -> {
            grammar.addAssembler("mystart", new IAssembler() {
                public void accept(Assembly a) {
                }
            });
        });
    }

    @Test
    public void resultStack() {
        grammar.addRule("mystart", new Literal("myliteral"));
        IParsingResult result = grammar.parse("myliteral");
        assertEquals(new Token("myliteral"), result.getStack().peek());
    }

    @Test
    public void ruleReference() {
        grammar.addRule("mystart", new RuleReference("referenced", grammar));
        grammar.addRule("referenced", new Empty());
        assertTrue(grammar.parse("").isCompleteMatch());
        assertFalse(grammar.parse("something").isCompleteMatch());

        grammar.addRule("referenced", new Word());
        assertTrue(grammar.parse("Hello").isCompleteMatch());
        assertFalse(grammar.parse("12.34").isCompleteMatch());

        grammar.addRule("referenced", new Repetition(new CaselessLiteral("hello")));
        assertTrue(grammar.parse("HELLO hello HellO").isCompleteMatch());
        assertFalse(grammar.parse("hello 12.34").isCompleteMatch());

        Sequence sequence = new Sequence();
        sequence.get(new CaselessLiteral("hello"));
        sequence.get(new Num());
        grammar.addRule("referenced", sequence);
        assertTrue(grammar.parse("HELLO 12.34").isCompleteMatch());
        assertFalse(grammar.parse("hello you").isCompleteMatch());
    }

    @Test
    public void ruleReferenceMustReferenceExistingClause() {
        grammar.addRule("mystart", new RuleReference("nothing", grammar));
        assertCheckFails();

        grammar.addRule("nothing", new Empty());
        grammar.check();
    }

    @Test
    public void allClausesMustBeAccessibleFromStartParser() {
        grammar.addRule("mystart", new Empty());
        grammar.addRule("unused", new Empty());
        assertCheckFails();

        Sequence startClause = new Sequence();
        startClause.get(new Int());
        startClause.get(new RuleReference("unused", grammar));
        grammar.addRule("mystart", startClause);
        grammar.check();

        //Add recursive clause reference
        startClause.get(new Repetition(new RuleReference("mystart", grammar)));
        grammar.check();

        grammar.addRule("more unused", new Empty());
        assertCheckFails();
    }

    @Test
    public void textualRules() {
        String ruleName = grammar.defineRule("command = \"go\"");
        assertEquals("command", ruleName);
        assertTrue(grammar.parse("go").isCompleteMatch());
    }


    @Test
    public void textualRuleWithGroovyClosure() {
        final List<Object> expectedMatches = new ArrayList<Object>();
        expectedMatches.add(new Token("test"));
        String ruleName = grammar.defineRule("mystart = \"test\"", (matches, stack) -> {
            assertEquals(expectedMatches, matches);
            assertTrue(stack.isEmpty());
            called = true;
        });
        assertEquals("mystart", ruleName);
        grammar.parse("test");
        assertTrue(called);
    }

    @Test
    public void readFromReader() throws Exception {
        String text = "start = '<' more;\nmore = '>';";
        StringReader reader = new StringReader(text);
        grammar.addRulesFrom(reader);
        reader.close();
        assertTrue(grammar.parse("< >").isCompleteMatch());
    }

    @Test
    public void leftRecursivenessCheckerIsPluggedIn() {
        assertThrows(GrammarException.class, () -> {
            grammar.defineRule("r = r '>'");
            grammar.check();
        });
    }


    @Test
    public void complexScenario() {
        defineTrackRobotGrammar();
        String[] sentences = new String[]{"pick carrier from LINE_IN", "place carrier at DB101_IN", "scan DB101_OUT"};
        for (String sentence : sentences) {
            assertTrue(grammar.parse(sentence).isCompleteMatch());
        }
        StringWriter sw = new StringWriter();
        grammar.printOn(new PrintWriter(sw));
        System.out.println(sw);
    }

    @Test
    public void performanceTest() {
        defineTrackRobotGrammar();
        grammar.check(); //Checking performance will not be measured
        String[] sentences = new String[]{"pick carrier from LINE_IN", "place carrier at DB101_IN", "scan DB101_OUT"};
        long before = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            for (String sentence : sentences) {
                assertTrue(grammar.parse(sentence).isCompleteMatch());
            }
        }
        long after = System.currentTimeMillis();
        //assertTrue("Time in msecs: " + (after - before), (after - before) < 1000L);
    }

    @Test
    public void defaultTerminalTypes() {
        assertTrue(grammar.terminal("Num") instanceof Num);
        assertTrue(grammar.terminal("Int") instanceof Int);
        assertTrue(grammar.terminal("Word") instanceof Word);
        assertTrue(grammar.terminal("QuotedString") instanceof QuotedString);
    }

    @Test
    public void registeringTerminalTypes() {
        grammar.registerTerminal(UpperCaseWord.class);
        grammar.registerTerminal("UCW", UpperCaseWord.class);
        assertTrue(grammar.terminal("UpperCaseWord") instanceof UpperCaseWord);
        assertTrue(grammar.terminal("UCW") instanceof UpperCaseWord);
    }

    @Test
    public void automaticConstantsDiscard() {
        assertFalse(grammar.areAllConstantsDiscarded());
        grammar.discardAllConstants();
        assertTrue(grammar.areAllConstantsDiscarded());
    }

    private void defineTrackRobotGrammar() {
        //From BPWJ, pp. 50
        Alternation command = new Alternation();
        command.get(new RuleReference("pickCommand", grammar));
        command.get(new RuleReference("placeCommand", grammar));
        command.get(new RuleReference("scanCommand", grammar));
        grammar.addRule("command", command);

        Sequence pickCommand = new Sequence();
        pickCommand.get(new CaselessLiteral("pick"));
        pickCommand.get(new CaselessLiteral("carrier"));
        pickCommand.get(new CaselessLiteral("from"));
        pickCommand.get(new RuleReference("location", grammar));
        grammar.addRule("pickCommand", pickCommand);

        Sequence placeCommand = new Sequence();
        placeCommand.get(new CaselessLiteral("place"));
        placeCommand.get(new CaselessLiteral("carrier"));
        placeCommand.get(new CaselessLiteral("at"));
        placeCommand.get(new RuleReference("location", grammar));
        grammar.addRule("placeCommand", placeCommand);

        Sequence scanCommand = new Sequence();
        scanCommand.get(new CaselessLiteral("scan"));
        scanCommand.get(new RuleReference("location", grammar));
        grammar.addRule("scanCommand", scanCommand);

        grammar.addRule("location", new Word());
        grammar.check();
    }

    //	@Test
    //	public void parameterizedRule() {
    //		Sequence startClause = new Sequence();
    //
    //		grammar.addRule("mystart", startClause);
    //		grammar.defineStartRule("mystart");
    //
    //		grammar.addRule("parameterized", new ParameterizedRule(new Repetition()));
    //
    //		assertTrue(grammar.parse("").isCompleteMatch());
    //		assertFalse(grammar.parse("hello").isCompleteMatch());
    //	}

}

//TODO convert from Mockito to Junit
//    @Test
//    public void assemblyInitialization() {
//        InitTokenizer initializer = mock(InitTokenizer.class);
//        grammar.initTokenizer(initializer);
//        grammar.addRule("mystart", new Empty());
//        grammar.parse("");
//        verify(initializer).init(any(Tokenizer.class));
//    }
//    @Test
//    public void textualRuleWithParserMatched() {
//        BiConsumer<List<Object>, Stack<Object>> matched = mock(BiConsumer.class);
//        String ruleName = grammar.defineRule("mystart = \"test\"", matched);
//        assertEquals("mystart", ruleName);
//        grammar.parse("test");
//        List<Object> expectedMatches = new ArrayList<Object>();
//        expectedMatches.add(new Token("test"));
//        verify(matched).accept(expectedMatches, new Stack<Object>());
//    }
//
//    @Test
//    public void addRuleTogetherWithAssembler() {
//        IAssembler assembler = mock(IAssembler.class);
//        grammar.addRule("mystart", new Literal("test"), assembler);
//        grammar.parse("test");
//        verify(assembler).workOn(any(Assembly.class));
//    }
//
//    @Test
//    public void assemblersWorkWithRuleReference() {
//        grammar.addRule("mystart", new RuleReference("ref", grammar));
//        IAssembler assembler = mock(IAssembler.class);
//        grammar.addRule("ref", new Word());
//        grammar.addAssembler("ref", assembler);
//        assertTrue(grammar.parse("aWord").isCompleteMatch());
//        verify(assembler).workOn(any(Assembly.class));
//    }
//
//    @Test
//    public void assemblersWorkWithRuleReferenceInSequence() {
//        Sequence mystart = new Sequence();
//        mystart.add(new RuleReference("ref", grammar));
//        grammar.addRule("mystart", mystart);
//        IAssembler assembler = mock(IAssembler.class);
//        grammar.addRule("ref", new Word());
//        grammar.addAssembler("ref", assembler);
//        assertTrue(grammar.parse("aWord").isCompleteMatch());
//        verify(assembler).workOn(any(Assembly.class));
//    }