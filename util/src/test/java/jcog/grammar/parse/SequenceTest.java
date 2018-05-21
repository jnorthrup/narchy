package jcog.grammar.parse;

import jcog.grammar.parse.tokens.Num;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jcog.grammar.parse.tokens.CaselessLiteral;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static jcog.grammar.parse.RepetitionTest.contains;
import static jcog.grammar.parse.RepetitionTest.size;

public class SequenceTest extends AbstractParsingTest {

	Sequence sequence;

	@BeforeEach
	public void init() {
		sequence = new Sequence();
	}

	@Test
	public void noMatch() {
		sequence.add(new CaselessLiteral("abc"));
		assertNoMatch("def");
	}

	@Test
	public void fullMatch() {
		sequence.add(new CaselessLiteral("abc"));
		assertCompleteMatch("abc");

		sequence.add(new Num());
		Assembly result = completeMatch("abc 1.0");
		assertEquals(new BigDecimal("1.0"), popValueFromAssembly(result));
	}

	@Test
	public void partialMatch() {
		sequence.add(new CaselessLiteral("abc"));
		Assembly result = bestMatch("abc def");
		assertEquals(1, result.elementsRemaining());
		assertEquals(1, result.elementsConsumed());
		assertEquals("abc", popValueFromAssembly(result));
	}

	@Test
	public void children() {
		sequence.add(new CaselessLiteral("abc"));
		sequence.add(new Num());
		assertEquals(2, size(getParser().children()));
	}

	@Test
	public void leftChildren() {
		sequence.add(new CaselessLiteral("abc"));
		sequence.add(new Num());
		assertEquals(1, size(getParser().leftChildren()));
		assertTrue(contains(getParser().leftChildren(), new CaselessLiteral("abc")), ()->"" + getParser().leftChildren());
	}

	@Override
	protected Parser getParser() {
		return sequence;
	}
}
