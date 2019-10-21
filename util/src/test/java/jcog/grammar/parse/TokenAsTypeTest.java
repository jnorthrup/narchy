package jcog.grammar.parse;

import jcog.grammar.parse.tokens.Token;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenAsTypeTest {

	private Token token;

	@Test
    void word() {
		token = new Token("hello");
		assertEquals("hello", token.asType(String.class));
		assertEquals(BigDecimal.ZERO, token.asType(Number.class));
		assertEquals(BigDecimal.ZERO, token.asType(BigDecimal.class));
		assertEquals(BigInteger.ZERO, token.asType(BigInteger.class));
		assertEquals(new Float(0.0), token.asType(Float.class));
		assertEquals(new Double(0.0), token.asType(Double.class));
		assertEquals(new Long(0), token.asType(Long.class));
	}

	@Test
    void number() {
		token = new Token(new BigDecimal("1.5"));
		assertEquals("1.5", token.asType(String.class));
		assertEquals(new BigDecimal("1.5"), token.asType(Number.class));
		assertEquals(new BigDecimal("1.5"), token.asType(BigDecimal.class));
		assertEquals(BigInteger.ONE, token.asType(BigInteger.class));
		assertEquals(new Float(1.5), token.asType(Float.class));
		assertEquals(new Double(1.5), token.asType(Double.class));
		assertEquals(new Long(1), token.asType(Long.class));
	}

	@Test
    void asUnknownType() {
		assertThrows(RuntimeException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                token = new Token("hello");
                token.asType(List.class);
            }
        });
	}

}
