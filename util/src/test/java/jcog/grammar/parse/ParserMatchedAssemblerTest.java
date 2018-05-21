package jcog.grammar.parse;


import jcog.grammar.parse.Assembly;
import jcog.grammar.parse.ParserMatchedAssembler;
import jcog.grammar.parse.tokens.Token;
import jcog.grammar.parse.tokens.TokenAssembly;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Stack;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParserMatchedAssemblerTest {

	private BiConsumer<List, Stack> matchedRule;
	private ParserMatchedAssembler assembler;
	private Assembly assembly;

	@BeforeEach
	public void Init() {
		//matchedRule = mock(BiConsumer.class);
		assembler = new ParserMatchedAssembler(matchedRule);
		assembly = new TokenAssembly("");
	}
//
//	@Test
//	public void emptyMatch() {
//		assembler.workOn(assembly);
//		verify(matchedRule).accept(Collections.emptyList(), new Stack<Object>());
//	}
//
//	@Test
//	public void oneElementMatch() {
//		assembly.push(new Token("hello"));
//		assembler.workOn(assembly);
//		List<Object> expectedMatches = new ArrayList<Object>();
//		expectedMatches.add(new Token("hello"));
//		verify(matchedRule).accept(expectedMatches, new Stack<Object>());
//	}
//
//	@Test
//	public void severalElementsMatch() {
//		assembly.push("a result");
//		assembly.announceMatchingStart();
//		assembly.push(new Token("b"));
//		assembly.push(new Token("c"));
//
//		assembler.workOn(assembly);
//
//		List<Object> expectedMatches = new ArrayList<Object>();
//		expectedMatches.add(new Token("b"));
//		expectedMatches.add(new Token("c"));
//		Stack<Object> expectedStack = new Stack<Object>();
//		expectedStack.add("a result");
//		verify(matchedRule).accept(expectedMatches, expectedStack);
//	}

	@Test
	public void matchedRuleCanManipulateStack() {
		assembly.push("a");
		assembly.announceMatchingStart();
		assembly.push(new Token("b"));

		matchedRule = (tokens, stack) -> {
			String result = stack.pop() + ((Token) tokens.get(0)).sval();
			stack.push(result);
		};

		assembler = new ParserMatchedAssembler(matchedRule);
		assembler.workOn(assembly);

		assertTrue("ab".equals(assembly.getStack().peek()));
	}

}
