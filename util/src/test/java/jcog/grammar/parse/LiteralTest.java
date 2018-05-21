package jcog.grammar.parse;

import jcog.grammar.parse.tokens.Literal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static jcog.grammar.parse.RepetitionTest.size;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LiteralTest extends AbstractParsingTest {

	Literal literal;

	@BeforeEach
	public void init() {
		literal = new Literal("hello");
	}

	@Test
	public void noMatch() {
		assertNoMatch("abc");
		assertNoMatch("HELLO");
	}

	@Test
	public void match() {
		assertCompleteMatch("hello");
	}

	@Test
	public void noChildren() {
		assertTrue(size(getParser().children())==0);
	}

	@Override
	protected Parser getParser() {
		return literal;
	}
}
