package jcog.grammar.parse;

import jcog.grammar.parse.tokens.CaselessLiteral;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static jcog.grammar.parse.RepetitionTest.size;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CaselessLiteralTest extends AbstractParsingTest {

	private CaselessLiteral caselessLiteral;

	@BeforeEach
    void init() {
		caselessLiteral = new CaselessLiteral("hello");
	}

	@Test
    void noMatch() {
		assertNoMatch("abc");
	}

	@Test
    void match() {
		assertCompleteMatch("hello");
		assertCompleteMatch("HELLO");
	}

	@Test
    void noChildren() {
		assertTrue(size(getParser().children())==0);
	}

	@Override
	protected Parser getParser() {
		return caselessLiteral;
	}
}
