package jcog.grammar.parse;

import jcog.grammar.parse.tokens.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

public class RuleGrammarTest {

	private Grammar targetGrammar;
	private RuleGrammar grammar;

	@BeforeEach
	public void init() {
		targetGrammar = new Grammar("test");
		grammar = new RuleGrammar(targetGrammar);
	}

	@Test
	public void emptyRuleFails() {
		IParsingResult result = grammar.parse("r = ");
		assertFalse(result.isCompleteMatch());
	}

	@Test
	public void caselessLiteralDefinition() {
		Parser rule = resultRule("\"cl\"");
		assertEquals(new CaselessLiteral("cl"), rule);
	}

	@Test
	public void resultStackShouldHaveRuleName() {
		IParsingResult result = grammar.parse("myRule = " + "\"cl\"");
		assertEquals(result.getStack().peek(), "myRule");
	}

	@Test
	public void symbolDefinition() {
		Parser rule = resultRule("'<'");
		assertEquals(new Symbol("<"), rule);
	}

	@Test
	public void ruleCanEndWithSemicolon() {
		assertTrue(grammar.parse("r = '<';").isCompleteMatch());
	}

	@Test
	public void additionalWhitespaceWillBeIgnored() {
		assertTrue(grammar.parse("  aRule  =  '<' ;   ").isCompleteMatch());
		assertEquals(new Symbol("<"), targetGrammar.getRule("aRule"));
	}

	@Test
	public void reference() {
		Parser rule = resultRule("other");
		assertEquals(new RuleReference("other", targetGrammar), rule);
	}

	@Test
	public void unknownTerminalType() {
		assertThrows(GrammarException.class, ()->{
		grammar.parse("r = UnknownXYZ");
		});
	}

	@Test
	public void builtInTerminalTypes() {
		Seq seq = (Seq) resultRule("Num Int Word QuotedString");
        assertTrue(seq.subparsers.get(0) instanceof Num);
        assertTrue(seq.subparsers.get(1) instanceof Int);
        assertTrue(seq.subparsers.get(2) instanceof Word);
        assertTrue(seq.subparsers.get(3) instanceof QuotedString);
	}

	@Test
	public void selfMadeTerminalTypes() {
		targetGrammar.registerTerminal(UpperCaseWord.class);
		targetGrammar.registerTerminal("UCW", UpperCaseWord.class);
		Seq seq = (Seq) resultRule("UpperCaseWord UCW");
        assertTrue(seq.subparsers.get(0) instanceof UpperCaseWord);
        assertTrue(seq.subparsers.get(1) instanceof UpperCaseWord);
	}

	@Test
	public void sequence() {
		Seq seq = (Seq) resultRule("\"a\" '<' \"b\"");
        assertEquals(new CaselessLiteral("a"), seq.subparsers.get(0));
        assertEquals(new Symbol("<"), seq.subparsers.get(1));
        assertEquals(new CaselessLiteral("b"), seq.subparsers.get(2));
	}

	@Test
	public void repetition() {
		Repetition rep = (Repetition) resultRule("a*");
		assertEquals(new RuleReference("a", targetGrammar), rep.getSubparser());
		assertEquals(0, rep.requiredMatches());
		assertTrue(grammar.parse("r = \"a\"*").isCompleteMatch());
		assertTrue(grammar.parse("r = '<' *").isCompleteMatch());
	}

	@Test
	public void alternation() {
		Alternation alt = (Alternation) resultRule("'<' | '>'");
        assertEquals(new Symbol("<"), alt.subparsers.get(0));
        assertEquals(new Symbol(">"), alt.subparsers.get(1));

		assertTrue(grammar.parse("r = \"a\" | b | '*'").isCompleteMatch());

		alt = (Alternation) resultRule("a* | '+' '*'");
        assertTrue(alt.subparsers.get(0) instanceof Repetition);
        assertTrue(alt.subparsers.get(1) instanceof Seq);
	}

	@Test
	public void parenthesesForUnnestedElements() {
		RuleReference ref = (RuleReference) resultRule("(a)");
		assertEquals(new RuleReference("a", targetGrammar), ref);

		assertParserType("(a b c)", Seq.class);
		assertParserType("(a*)", Repetition.class);
		assertParserType("(a|b)", Alternation.class);
	}

	@Test
	public void nestedParentheses() {
		assertParserType("(a|b) c", Seq.class);
		assertParserType("a (b c)*", Seq.class);
		assertParserType("(a b) | (c d)*", Alternation.class);
		assertParserType("(a b c d)*", Repetition.class);
		assertParserType("(a b c d)+", Repetition.class);
		printGrammar(targetGrammar);
	}

	@Test
	public void byDefaultConstantsAreNotDiscarded() {
		Seq ref = (Seq) resultRule("'<' \"a\"");
		assertFalse(((Terminal) ref.getChild(0)).isDiscarded());
		assertFalse(((Terminal) ref.getChild(1)).isDiscarded());
	}

	@Test
	public void switchOnDefaultDiscardOfConstants() {
		targetGrammar.discardAllConstants();
		Seq ref = (Seq) resultRule("'<' \"a\"");
		assertTrue(((Terminal) ref.getChild(0)).isDiscarded());
		assertTrue(((Terminal) ref.getChild(1)).isDiscarded());
	}

	@Test
	public void explicitConstantsDiscard() {
		Seq ref = (Seq) resultRule("#'<' #\"a\"");
		assertTrue(((Terminal) ref.getChild(0)).isDiscarded());
		assertTrue(((Terminal) ref.getChild(1)).isDiscarded());
	}

	@Test
	public void atLeastOne() {
		Repetition rep = (Repetition) resultRule("a+");
		assertEquals(new RuleReference("a", targetGrammar), rep.getSubparser());
		assertEquals(1, rep.requiredMatches());
		assertTrue(grammar.parse("r = \"a\"+").isCompleteMatch());
		assertTrue(grammar.parse("r = '<' +").isCompleteMatch());
	}

	@Test
	public void printing() {
		printGrammar(grammar);
	}

	private void printGrammar(Grammar grammar) {
		StringWriter sw = new StringWriter();
		grammar.printOn(new PrintWriter(sw));
		System.out.println(sw);
	}

	private void assertParserType(String body, Class<? extends Parser> parserType) {
		Parser parser = resultRule(body);
		assertTrue(parserType.isInstance(parser),()->"should be of type: " + parserType);
	}

	private Parser resultRule(String ruleBody) {
		IParsingResult result = grammar.parse("r = " + ruleBody);
		assertTrue(result.isCompleteMatch(),()->"should be complete match: " + result);
		Parser rule = targetGrammar.getRule("r");
		return rule;
	}
}
