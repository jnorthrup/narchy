package jcog.grammar.parse;

import jcog.grammar.parse.tokens.CaselessLiteral;
import jcog.grammar.parse.tokens.Num;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AlternationTest extends AbstractParsingTest {

	Alternation alternation;

	@BeforeEach
	public void init() {
		alternation = new Alternation();
	}

	@Test
	public void noMatch() {
		alternation.get(new CaselessLiteral("abc"));
		assertNoMatch("def");
	}

	@Test
	public void fullMatch() {
		alternation.get(new CaselessLiteral("abc"));
		alternation.get(new Num());
		alternation.get(new Empty());
		assertCompleteMatch("abc");
		assertCompleteMatch("2.3");
		assertCompleteMatch("");
		assertNoCompleteMatch("def");
	}

	@Test
	public void children() {
		alternation.get(new CaselessLiteral("abc"));
		alternation.get(new Num());
		Assertions.assertEquals(2, RepetitionTest.size(getParser().children()));
	}

	@Test
	public void leftChildren() {
		alternation.get(new CaselessLiteral("abc"));
		alternation.get(new Num());
		Assertions.assertEquals(2, RepetitionTest.size(getParser().leftChildren()));
	}

	@Override
	protected Parser getParser() {
		return alternation;
	}
}
