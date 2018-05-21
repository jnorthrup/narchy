package jcog.grammar.parse;

import jcog.grammar.parse.tokens.Int;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static jcog.grammar.parse.RepetitionTest.size;

public class IntTest extends AbstractParsingTest {

	Int intTerminal;

	@BeforeEach
	public void init() {
		intTerminal = new Int();
	}

	@Test
	public void noMatch() {
		assertNoMatch("abc");
		assertNoMatch("1.23");
	}

	@Test
	public void match() {
		Assembly result = completeMatch("1000");
		assertEquals(new BigInteger("1000"), popValueFromAssembly(result));
	}

	@Test
	public void noChildren() {
		assertTrue(size(getParser().children())==0);
	}

	@Override
	protected Parser getParser() {
		return intTerminal;
	}
}
