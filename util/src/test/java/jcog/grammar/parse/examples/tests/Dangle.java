package jcog.grammar.parse.examples.tests;

import jcog.grammar.parse.Alternation;
import jcog.grammar.parse.Parser;
import jcog.grammar.parse.Sequence;
import jcog.grammar.parse.tokens.Literal;
import jcog.grammar.parse.tokens.Num;
import jcog.grammar.parse.tokens.Symbol;
import jcog.grammar.parse.tokens.Word;

/**
 * This class provides an ambiguous parser in its <code>
 * statement</code> method, which serves to show that
 * the test classes can find ambiguity.
 * <p>
 * The grammar this class supports is:
 * <blockquote><pre> 
 *
 *     statement     = iff | ifelse | callCustomer | sendBill;
 *     iff           = "if" comparison statement;
 *     ifelse        = "if" comparison statement 
 *                     "else" statement;
 *     comparison    = '(' expression operator expression ')';
 *     expression    = Word | Num;
 *     operator      = '<' | '>' | '=' | "<=" | ">=" | "!=";
 *     optionalElse  = "else" statement | Empty;
 *     callCustomer  = "callCustomer" '('')' ';';
 *     sendBill      = "sendBill" '('')' ';';
 * </pre></blockquote>
 * 
 * @author Steven J. Metsker
 * 
 * @version 1.0 
 */
public class Dangle {
	protected static Alternation statement;

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *     callCustomer = "callCustomer" '(' ')' ';';
	 */
	public static Parser callCustomer() {
		Sequence s = new Sequence("<callCustomer>");
		s.get(new Literal("callCustomer"));
		s.get(new Symbol('('));
		s.get(new Symbol(')'));
		s.get(new Symbol(';'));
		return s;
	}

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *     comparison   = '(' expression operator expression ')';
	 */
	public static Parser comparison() {
		Sequence s = new Sequence("<comparison>");
		s.get(new Symbol('('));
		s.get(expression());
		s.get(operator());
		s.get(expression());
		s.get(new Symbol(')'));
		return s;
	}

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *     expression   = Word | Num;
	 */
	public static Parser expression() {
		Alternation a = new Alternation("<expression>");
		a.get(new Word());
		a.get(new Num());
		return a;
	}

	/*
	 * Return a parser that recognizes the grammar:
	 *
	 *     ifelse = "if" comparison statement "else" statement;
	 */
	public static Parser ifelse() {
		Sequence s = new Sequence("<ifelse>");
		s.get(new Literal("if"));
		s.get(comparison());
		s.get(statement());
		s.get(new Literal("else"));
		s.get(statement());
		return s;
	}

	/*
	 * Return a parser that recognizes the grammar:
	 *
	 *     iff = "if" comparison statement;
	 */
	public static Parser iff() {
		Sequence s = new Sequence("<iff>");
		s.get(new Literal("if"));
		s.get(comparison());
		s.get(statement());
		return s;
	}

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *     operator     = '<' | '>' | '=' | "<=" | ">=" | "!=";
	 */
	public static Parser operator() {
		Alternation a = new Alternation("<operator>");
		a.get(new Symbol('<'));
		a.get(new Symbol('>'));
		a.get(new Symbol('='));
		a.get(new Symbol("<="));
		a.get(new Symbol(">="));
		a.get(new Symbol("!="));
		return a;
	}

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *     sendBill     = "sendBill" '('')' ';';
	 */
	public static Parser sendBill() {
		Sequence s = new Sequence("<sendBill>");
		s.get(new Literal("sendBill"));
		s.get(new Symbol('('));
		s.get(new Symbol(')'));
		s.get(new Symbol(';'));
		return s;
	}

	/**
	 * Return a parser that recognizes the grammar:
	 *
	 * <blockquote><pre>
	 *     statement    = "if" comparison statement optionalElse |
	 *                     callCustomer | sendBill;
	 * </pre></blockquote>
	 *
	 *
	 * @return a parser that recognizes a statement
	 */
	public static Parser statement() {
		if (statement == null) {
			statement = new Alternation("<statement>");
			statement.get(iff());
			statement.get(ifelse());
			statement.get(callCustomer());
			statement.get(sendBill());
		}
		return statement;
	}
}
