package jcog.grammar.parse;

import java.util.Stack;

import jcog.grammar.parse.PubliclyCloneable;

public interface IParsingResult {

	boolean isCompleteMatch();

	Stack<Object> getStack();

	PubliclyCloneable<?> getTarget();

}
