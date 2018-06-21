package jcog.grammar.parse.examples.query;

import jcog.grammar.parse.Alternation;
import jcog.grammar.parse.Parser;
import jcog.grammar.parse.Repetition;
import jcog.grammar.parse.Seq;
import jcog.grammar.parse.examples.logic.ArithmeticAssembler;
import jcog.grammar.parse.examples.logic.AtomAssembler;
import jcog.grammar.parse.tokens.Num;
import jcog.grammar.parse.tokens.QuotedString;
import jcog.grammar.parse.tokens.Symbol;
import jcog.grammar.parse.tokens.Word;

/**
 * This utility class provides support to the Jaql 
 * parser, specifically for <code>expression()</code>
 * and <code>comparison()</code> subparsers.
 *
 * The grammar this class supports is:
 *
 * <blockquote><pre>	
 *     comparison = arg operator arg;
 *     arg        = expression | QuotedString;
 *     expression = term ('+' term | '-' term)*;
 *     term       = factor ('*' factor | '/' factor)*;
 *     factor     = '(' expression ')' | Num | variable;
 *     variable   = Word;
 *     operator   = "<" | ">" | "=" | "<=" | ">=" | "!=";
 * </pre></blockquote>
 * 
 * @author Steven J. Metsker
 *
 * @version 1.0 
 */
public class ComparisonParser {
	private Seq expression;
	private Speller speller;

	/**
	 * Construct a ComparisonParser that will consult the
	 * given speller for the proper spelling of variable
	 * names.
	 */
	public ComparisonParser(Speller speller) {
		this.speller = speller;
	}

	/**
	 * Returns a parser that will recognize a comparison
	 * argument.
	 */
    private Parser arg() {

		

		Alternation a = new Alternation();
		a.get(expression());
		a.get(new QuotedString().put(new AtomAssembler()));
		return a;
	}

	/**
	 * Returns a parser that will recognize a comparison.
	 */
	public Parser comparison() {
		Seq s = new Seq("comparison");
		s.get(arg());
		s.get(operator());
		s.get(arg());
		s.put(new ComparisonAssembler());
		return s;
	}

	/*
	 * Recognize '/' followed by a factor.
	 */
    private Parser divideFactor() {
		Seq s = new Seq("divideFactor");
		s.get(new Symbol('/').ok());
		s.get(factor());
		s.put(new ArithmeticAssembler('/'));
		return s;
	}

	/**
	 * Returns a parser that will recognize an arithmetic
	 * expression.
	 */
	public Parser expression() {
		/*
		 * This use of a static variable avoids the infinite 
		 * recursion inherent in the language definition.
		 */
		if (expression == null) {

			
			expression = new Seq("expression");
			expression.get(term());

			
			Alternation a = new Alternation();
			a.get(plusTerm());
			a.get(minusTerm());
			expression.get(new Repetition(a));
		}
		return expression;
	}

	/*
	 * Recognize an expression in parens, or a number, or a
	 * variable.
	 */
    private Parser factor() {
		
		Alternation factor = new Alternation("factor");

		
		Seq s = new Seq();
		s.get(new Symbol('(').ok());
		s.get(expression());
		s.get(new Symbol(')').ok());
		factor.get(s);

		
		factor.get(new Num().put(new AtomAssembler()));
		factor.get(variable());

		return factor;
	}

	/*
	 * Recognize '-' followed by a term.
	 */
    private Parser minusTerm() {
		Seq s = new Seq("minusTerm");
		s.get(new Symbol('-').ok());
		s.get(term());
		s.put(new ArithmeticAssembler('-'));
		return s;
	}

	/*
	 * Recognize an operator.
	 */
    private Parser operator() {
		Alternation a = new Alternation("operator");
		a.get(new Symbol('<'));
		a.get(new Symbol('>'));
		a.get(new Symbol('='));
		a.get(new Symbol("<="));
		a.get(new Symbol(">="));
		a.get(new Symbol("!="));
		return a;
	}

	/*
	 * Recognize '+' followed by a term.
	 */
    private Parser plusTerm() {
		Seq s = new Seq("plusTerm");
		s.get(new Symbol('+').ok());
		s.get(term());
		s.put(new ArithmeticAssembler('+'));
		return s;
	}

	/*
	 * Recognize a "term", per the language definition.
	 */
    private Parser term() {
		
		Seq term = new Seq("term");

		
		term.get(factor());

		
		Alternation a = new Alternation();
		a.get(timesFactor());
		a.get(divideFactor());

		term.get(new Repetition(a));
		return term;
	}

	/*
	 * Recognize '*' followed by a factor.
	 */
    private Parser timesFactor() {
		Seq s = new Seq("timesFactor");
		s.get(new Symbol('*').ok());
		s.get(factor());
		s.put(new ArithmeticAssembler('*'));
		return s;
	}

	/*
	 * Recognizes any word.
	 */
    private Parser variable() {
		return new Word().put(new VariableAssembler(speller));
	}
}
