package jcog.grammar.parse;


import jcog.grammar.parse.tokens.Token;
import jcog.grammar.parse.tokens.TokenAssembly;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Stack;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserMatchedAssemblerTest {

	private BiConsumer<List, Stack> matchedRule;
	private ParserMatchedAssembler assembler;
	private Assembly assembly;

	@BeforeEach
    void Init() {
		
		assembler = new ParserMatchedAssembler(matchedRule);
		assembly = new TokenAssembly("");
	}

































	@Test
    void matchedRuleCanManipulateStack() {
		assembly.push("a");
		assembly.announceMatchingStart();
		assembly.push(new Token("b"));

		matchedRule = new BiConsumer<List, Stack>() {
            @Override
            public void accept(List tokens, Stack stack) {
                String result = stack.pop() + ((Token) tokens.get(0)).sval();
                stack.push(result);
            }
        };

		assembler = new ParserMatchedAssembler(matchedRule);
		assembler.accept(assembly);

		assertTrue("ab".equals(assembly.getStack().peek()));
	}

}
