package jcog.grammar.parse;

import jcog.grammar.parse.tokens.CaselessLiteral;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static jcog.grammar.parse.RepetitionTest.size;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CaselessLiteralTest extends AbstractParsingTest {

	CaselessLiteral caselessLiteral;

	@BeforeEach
	public void init() {
		caselessLiteral = new CaselessLiteral("hello");
	}

	@Test
	public void noMatch() {
		assertNoMatch("abc");
	}

	@Test
	public void match() {
		assertCompleteMatch("hello");
		assertCompleteMatch("HELLO");
	}

	@Test
	public void noChildren() {
		assertTrue(size(getParser().children())==0);
	}

	@Override
	protected Parser getParser() {
		return caselessLiteral;
	}
}
