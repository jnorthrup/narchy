package jcog.grammar.parse.examples.logic;

import jcog.grammar.parse.*;
import jcog.grammar.parse.examples.mechanics.LowercaseWord;
import jcog.grammar.parse.examples.mechanics.UppercaseWord;
import jcog.grammar.parse.examples.track.Track;
import jcog.grammar.parse.tokens.Num;
import jcog.grammar.parse.tokens.QuotedString;
import jcog.grammar.parse.tokens.Symbol;

/**
 * This class provides a parser for Logikus, a logic 
 * language similar to Prolog.
 * <p>
 * The grammar this class supports is:
 * <blockquote><pre> 
 * 
 *     axiom        = structure (ruleDef | Empty);
 *     structure    = functor ('(' commaList(term) ')' | Empty);
 *     functor      = '.' | LowercaseWord | QuotedString;
 *     term         = structure | Num | list | variable;
 *     variable     = UppercaseWord | '_';
 * <br>
 *     ruleDef      = ":-" commaList(condition);
 *     condition    = structure | not | evaluation | 
 *                    comparison | list;
 * <br>
 *     not          = "not" structure ;
 * <br>
 *     evaluation   =      '#' '(' arg ',' arg ')';
 *     comparison   = operator '(' arg ',' arg ')';
 *     arg          = expression | functor;
 *     operator     = '<' | '>' | '=' | "<=" | ">=" | "!=" ;
 *     expression   = phrase ('+' phrase | '-' phrase)*;
 *     phrase       = factor ('*' factor | '/' factor)*;
 *     factor       = '(' expression ')' | Num | variable;
 * <br>
 *     list         = '[' (listContents | Empty) ']';
 *     listContents = commaList(term) listTail;
 *     listTail     = ('|' (variable | list)) | Empty;
 * <br>
 *     commaList(p) = p (',' p)*;
 * </pre></blockquote>
 * 
 * The following program and query use most of the features of 
 * the Logikus grammar:
 *
 * <blockquote><pre>
 *     // program
 *     member(X, [X | Rest]);
 *     member(X, [Y | Rest]) :- member(X, Rest);
 *     primes([2, 3, 5, 7, 11, 13]);
 *     factor(X, P, Q) :- 
 *         primes(Primes), 
 *         member(P, Primes), member(Q, Primes), =(P*Q, X); 
 * <br>
 *     // query
 *     factor(91, A, B)
 * <br>
 *     // results
 *     A = 7.0, B = 13.0
 *     A = 13.0, B = 7.0
 *     no
 * </pre></blockquote>
 *
 * The class <code>LogikusFacade</code> simplifies the 
 * construction of <code>Program</code> and <code>Query</code> 
 * objects from the text given above. A Java program can prove 
 * the query to generate the results. 
 * 
 * <p>
 * The class <code>LogikusIde</code> is an example of using the 
 * <code>Logikus</code> parser in practice. It uses 
 * <code>LogikusFacade</code> to create a <code>Query</code>, 
 * proves the query, and displays the query's variables for 
 * each proof. As in Prolog, the Logikus development 
 * environment prints "no" when no further proofs remain.
 *
 * @author Steven J. Metsker
 *
 * @version 1.0 
 */
public class LogikusParser {
	protected Sequence structure;
	protected Sequence expression;
	protected Track list;

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *    arg = expression | functor;
	 */
	protected Parser arg() {
		Alternation a = new Alternation();
		a.get(expression());
		a.get(functor().put(new AtomAssembler()));
		return a;
	}

	/**
	 * Return a parser that recognizes the grammar:
	 *
	 * <blockquote><pre>
	 *    axiom = structure (ruleDef | Empty);
	 * </pre></blockquote>
	 *
	 * @return a parser that recognizes an axiom
	 */
	public Parser axiom() {
		Sequence s = new Sequence("axiom");

		s.get(structure());
		Alternation a = new Alternation();
		a.get(ruleDef());
		a.get(new Empty());
		s.get(a);

		s.put(new AxiomAssembler());
		return s;
	}

	/*
	 * Using the given parser, this method composes a new
	 * parser with the grammar:
	 * 
	 *     commaList(p) = p (',' p)*;
	 *
	 * The Logikus language uses this construction several 
	 * times.
	 */
	protected static Sequence commaList(Parser p) {
		return new Sequence(p, new Repetition(
				new Track().see(',').get(p))
		);
	}

	/**
	 * Return a parser that recognizes the grammar:
	 *
	 * <blockquote><pre>
	 *    comparison = operator '(' arg ',' arg ')';
	 * </pre></blockquote>
	 *
	 * @return a parser that recognizes a comparison
	 */
	public Parser comparison() {
		return new Track("comparison")
			.get(operator())
			.see('(').get(arg()).see(',').get(arg()).see(')')
			.put(new ComparisonAssembler());
	}

	/**
	 * Return a parser that recognizes the grammar:
	 *
	 * <blockquote><pre>
	 *    condition = structure | not | evaluation | comparison | 
	 *                list;
	 * </pre></blockquote>
	 *
	 * @return a parser that recognizes a condition
	 */
	public Alternation condition() {
		return new Alternation("condition",
			structure(), not(), evaluation(), comparison(), list());
	}

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *    divideFactor = '/' factor;
	 */
	protected Parser divideFactor() {
		Sequence s = new Sequence("divideFactor");
		s.get(new Symbol('/').ok());
		s.get(factor());
		s.put(new ArithmeticAssembler('/'));
		return s;
	}

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *     evaluation = '#' '(' arg ',' arg ')';
	 *
	 * For example, this parser will recognize 
	 * "#(X, 12321/111)", translating it to an Evaluation 
	 * object. When asked to prove itself, the Evaluation 
	 * object will unify its first term with the value of 
	 * its second term.
	 */
	protected Parser evaluation() {
		return new Track("evaluation")
				.see('#').see('(')
				.get(arg())
				.see(',')
				.get(arg())
				.see(')').put(new EvaluationAssembler());
	}

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *    expression = phrase ('+' phrase | '-' phrase)*; 
	 */
	protected Parser expression() {
		/*
		 * This use of a static variable avoids the infinite 
		 * recursion inherent in the language definition.
		 */
		if (expression == null) {
			expression = new Sequence("expression");
			expression.get(phrase());
			Alternation a = new Alternation();
			a.get(plusPhrase());
			a.get(minusPhrase());
			expression.get(new Repetition(a));
		}
		return expression;
	}

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *    factor = '(' expression ')' | Num | variable;
	 */
	protected Parser factor() {
		return new Alternation("factor",
			new Sequence().see('(').get(expression()).see(')'),
			num(),
			variable());
	}

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *    functor = '.' | LowercaseWord | QuotedString;
	 */
	protected Parser functor() {
		Alternation a = new Alternation("functor");
		a.get(new Symbol('.'));
		a.get(new LowercaseWord());
		a.get(new QuotedString());
		return a;
	}

	/**
	 * Return a parser that recognizes the grammar:
	 *
	 * <blockquote><pre>
	 *    list = '[' (listContents | Empty) ']';
	 * </pre></blockquote>
	 *
	 * The class comment gives the complete grammar for lists,
	 * as part of the Logikus grammar.
	 *
	 * @return a parser that recognizes a list
	 */
	public Parser list() {

		if (list == null) {
			list = new Track("list");

			list.get('[') // push this, as a fence
				.or(
					listContents(),
					new Empty().put(new ListAssembler())
				).see(']');
		}
		return list;
	}

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *    listContents = commaList(term) listTail;
	 */
	protected Parser listContents() {
		return commaList(term()).get(listTail());
	}

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *    listTail = ('|' (variable | list)) | Empty;
	 */
	protected Parser listTail() {
		return new Alternation(
			new Track("bar tail").see('|').or(
					variable(), list()
				).put(new ListWithTailAssembler()),
			new Empty().put(new ListAssembler())
		);
	}

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *    minusPhrase = '-' phrase;
	 */
	protected Parser minusPhrase() {
		return new Sequence("minusPhrase").see('-').get(phrase()).put(new ArithmeticAssembler('-'));
	}

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *     not = "not" structure;
	 */
	protected Parser not() {
		return new Track("not").see("not").get(structure()).put(new NotAssembler());
	}

	/*
	 * Return a parser that recognizes a number and
	 * stacks a corresponding atom.
	 */
	public Parser num() {
		return new Num().put(new AtomAssembler());
	}

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *     operator = '<' | '>' | '=' | "<=" | ">=" | "!=" ;
	 */
	protected Parser operator() {
		return new Alternation("operator",
				new Symbol('<'),
				new Symbol('>'),
				new Symbol('='),
				new Symbol("<="),
				new Symbol(">="),
				new Symbol("!="));
	}

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *    phrase = factor ('*' factor | '/' factor)*;
	 */
	protected Parser phrase() {
		return new Sequence("phrase").get(factor())
				.get(new Repetition(
						new Alternation(timesFactor(), divideFactor())));
	}

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *    plusPhrase = '+' phrase;
	 */
	protected Parser plusPhrase() {
		Sequence s = new Sequence("plusPhrase");
		s.get(new Symbol('+').ok());
		s.get(phrase());
		s.put(new ArithmeticAssembler('+'));
		return s;
	}

	/**
	 * Return a parser that recognizes the grammar:
	 *
	 * <blockquote><pre>
	 *    query = commaList(condition);
	 * </pre></blockquote>
	 *
	 * @return a parser that recognizes a query
	 */
	public static Parser query() {
		return commaList(new LogikusParser().condition()).put(new AxiomAssembler());
	}

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *    ruleDef = ":-" commaList(condition); 
	 */
	protected Parser ruleDef() {
		return new Track("rule definition")
				.see(":-").get(commaList(condition()));
	}

	/**
	 * Return a parser that recognizes the grammar:
	 *
	 * <blockquote><pre>
	 *    axiom = condition (ruleDefinition | empty);
	 * </pre></blockquote>
	 *
	 * @return a parser that recognizes an axiom
	 */
	public static Parser start() {
		return new LogikusParser().axiom();
	}

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *    structure = functor ( '(' commaList(term) ')' | Empty);
	 *
	 * This definition of structure accounts for normal-looking 
	 * structures that have a string as a functor. Strictly 
	 * speaking, numbers and lists are also structures. The 
	 * definition for <code>term</code> includes these.
	 */
	protected Parser structure() {
		if (structure == null) {
			structure = new Sequence("structure");
			structure.get(functor());

			Track t = new Track("list in parens");
			t.get(new Symbol('(')); // push this as a fence
			t.get(commaList(term()));
			t.get(new Symbol(')').ok());

			Alternation a = new Alternation();
			a.get(t.put(new StructureWithTermsAssembler()));
			a.get(new Empty().put(new AtomAssembler()));

			structure.get(a);
		}
		return structure;
	}

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *    term = structure | Num | list | variable;
	 */
	protected Parser term() {
		return new Alternation("term",
				structure(),num(), list(),variable());
	}

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *    timesFactor = '*' factor;
	 */
	protected Parser timesFactor() {
		return new Sequence("timesFactor").see('*').get(factor()).put(new ArithmeticAssembler('*'));
	}

	/*
	 * Return a parser that recognizes the grammar:
	 * 
	 *    variable = UppercaseWord | '_';
	 *
	 * The underscore represents and will translate to an 
	 * anonymous variable.
	 */
	protected Parser variable() {
		return new Alternation(
			new UppercaseWord().put(new VariableAssembler()),
			new Symbol('_').ok().put(new AnonymousAssembler()));
	}
}
