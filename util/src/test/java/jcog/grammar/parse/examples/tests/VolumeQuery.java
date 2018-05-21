package jcog.grammar.parse.examples.tests;

import jcog.grammar.parse.Alternation;
import jcog.grammar.parse.Parser;
import jcog.grammar.parse.Repetition;
import jcog.grammar.parse.Sequence;
import jcog.grammar.parse.tokens.Literal;
import jcog.grammar.parse.tokens.Symbol;
import jcog.grammar.parse.tokens.Word;

/**
 * This class provides an ambiguous parser in its <code>
 * query</code> method, which serves to show that
 * the test classes can find ambiguity.
 * <p>
 * The grammar this class supports is:
 * <blockquote><pre> 
 *
 *     query  = (Word | volume)* '?';
 *     volume = "cups" | "gallon" | "liter";
 * </pre></blockquote>
 * 
 * @author Steven J. Metsker
 * 
 * @version 1.0 
 */
public class VolumeQuery {
	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *     query = (Word | volume)* '?';
	 */
	public static Parser query() {
		Parser a = new Alternation().add(new Word()).add(volume());
		Parser s = new Sequence().add(new Repetition(a)).add(new Symbol('?'));
		return s;
	}

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *     volume = "cups" | "gallon" | "liter";
	 */
	public static Parser volume() {
		Parser a = new Alternation().add(new Literal("cups")).add(new Literal("gallon")).add(new Literal("liter"));
		return a;
	}
}
