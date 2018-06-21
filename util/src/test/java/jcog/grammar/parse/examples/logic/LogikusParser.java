package jcog.grammar.parse.examples.logic;

import jcog.grammar.parse.*;
import jcog.grammar.parse.examples.engine.*;
import jcog.grammar.parse.examples.mechanics.LowercaseWord;
import jcog.grammar.parse.examples.mechanics.UppercaseWord;
import jcog.grammar.parse.examples.track.SeqEx;
import jcog.grammar.parse.tokens.Num;
import jcog.grammar.parse.tokens.QuotedString;
import jcog.grammar.parse.tokens.Symbol;
import jcog.grammar.parse.tokens.Token;

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
 * <p>
 * The following program and query use most of the features of
 * the Logikus grammar:
 *
 * <blockquote><pre>
 *     
 *     member(X, [X | Rest]);
 *     member(X, [Y | Rest]) :- member(X, Rest);
 *     primes([2, 3, 5, 7, 11, 13]);
 *     factor(X, P, Q) :-
 *         primes(Primes),
 *         member(P, Primes), member(Q, Primes), =(P*Q, X);
 * <br>
 *     
 *     factor(91, A, B)
 * <br>
 *     
 *     A = 7.0, B = 13.0
 *     A = 13.0, B = 7.0
 *     no
 * </pre></blockquote>
 * <p>
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
 * @version 1.0
 */
public class LogikusParser {
    private Seq structure;
    private CollectionParser expression;
    private SeqEx list;

    /*
     * Using the given parser, this method composes a new
     * parser with the grammar:
     *
     *     commaList(p) = p (',' p)*;
     *
     * The Logikus language uses this construction several
     * times.
     */
    private static Seq commaList(Parser p) {
        return new Seq(p, new Repetition(
                new SeqEx().see(',').get(p))
        );
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
     *    arg = expression | functor;
     */
    private Parser arg() {
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
        Seq s = new Seq("axiom");

        s.get(structure());
        Alternation a = new Alternation();
        a.get(ruleDef());
        a.get(new Empty());
        s.get(a);

        s.put(new AxiomAssembler());
        return s;
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
    private Parser comparison() {
        return new SeqEx("comparison")
                .get(operator())
                .see('(').get(arg()).see(',').get(arg()).see(')')
                .put(a -> {
                    /**
                     * Pops two comparison terms and an operator, builds
                     * the comparison, and pushes it.
                     *
                     * @param  Assembly  the assembly to work on
                     */
                    ComparisonTerm second = (ComparisonTerm) a.pop();
                    ComparisonTerm first = (ComparisonTerm) a.pop();
                    Token t = (Token) a.pop();
                    a.push(new Comparison(t.sval(), first, second));
                });
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
    private Alternation condition() {
        return new Alternation("condition",
                structure(), not(), evaluation(), comparison(), list());
    }

    /*
     * Return a parser that recognizes the grammar:
     *
     *    divideFactor = '/' factor;
     */
    private Parser divideFactor() {
        return new Seq("divideFactor").see('/').get(factor()).put(new ArithmeticAssembler('/'));
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
    private Parser evaluation() {
        return new SeqEx("evaluation")
                .see('#').see('(')
                .get(arg())
                .see(',')
                .get(arg())
                .see(')').push(a -> {
                    /**
                     * Pops two terms, constructs an Evaluation from these terms,
                     * and pushes it.
                     *
                     * @param  Assembly  the assembly to work on
                     */
                    Term second = (Term) a.pop();
                    Term first = (Term) a.pop();
                    return new Evaluation(first, second);
                });
    }

    /*
     * Return a parser that recognizes the grammar:
     *
     *    expression = phrase ('+' phrase | '-' phrase)*;
     */
    private Parser expression() {
        /*
         * This use of a static variable avoids the infinite
         * recursion inherent in the language definition.
         */
        if (expression == null) {
            expression = new Seq("expression");
            expression.get(phrase())
                    .get(new Repetition(new Alternation(plusPhrase(),minusPhrase())));
        }
        return expression;
    }

    /*
     * Return a parser that recognizes the grammar:
     *
     *    factor = '(' expression ')' | Num | variable;
     */
    private Parser factor() {
        return new Alternation("factor",
                new Seq().see('(').get(expression()).see(')'),
                num(),
                variable());
    }

    /*
     * Return a parser that recognizes the grammar:
     *
     *    functor = '.' | LowercaseWord | QuotedString;
     */
    private Parser functor() {
        return new Alternation("functor",
                new Symbol('.'), new LowercaseWord(), new QuotedString());
    }

    /**
     * Return a parser that recognizes the grammar:
     *
     * <blockquote><pre>
     *    list = '[' (listContents | Empty) ']';
     * </pre></blockquote>
     * <p>
     * The class comment gives the complete grammar for lists,
     * as part of the Logikus grammar.
     *
     * @return a parser that recognizes a list
     */
    private Parser list() {

        if (list == null) {
            list = new SeqEx("list");

            list.get('[') 
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
    private Parser listContents() {
        return commaList(term()).get(listTail());
    }

    /*
     * Return a parser that recognizes the grammar:
     *
     *    listTail = ('|' (variable | list)) | Empty;
     */
    private Parser listTail() {
        return new Alternation(
                new SeqEx("bar tail").see('|').or(
                        variable(), list()
                ).push(a -> {
                    
                    

                    Term tail = (Term) a.pop();

                    Term[] termsToLast = StructureWithTermsAssembler.vectorReversedIntoTerms(
                            AssemblerHelper.elementsAbove(a, new Token('['))
                    );

                    return Structure.list(termsToLast, tail);
                }),
                new Empty().put(new ListAssembler())
        );
    }

    /*
     * Return a parser that recognizes the grammar:
     *
     *    minusPhrase = '-' phrase;
     */
    private Parser minusPhrase() {
        return new Seq("minusPhrase").see('-').get(phrase()).put(new ArithmeticAssembler('-'));
    }

    /*
     * Return a parser that recognizes the grammar:
     *
     *     not = "not" structure;
     */
    private Parser not() {
        return new SeqEx("not").see("not").get(structure()).put(a -> {
            /**
             * Pops a structure from the top of the stack and pushes a Not
             * version of it.
             *
             * @param  Assembly  the assembly to work on
             */
            a.push(new Not((Structure) a.pop()));
        });
    }

    /*
     * Return a parser that recognizes a number and
     * stacks a corresponding atom.
     */
    private Parser num() {
        return new Num().put(new AtomAssembler());
    }

    /*
     * Return a parser that recognizes the grammar:
     *
     *     operator = '<' | '>' | '=' | "<=" | ">=" | "!=" ;
     */
    private Parser operator() {
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
    private Parser phrase() {
        return new Seq("phrase").get(factor())
                .get(new Repetition(
                        new Alternation(timesFactor(), divideFactor())));
    }

    /*
     * Return a parser that recognizes the grammar:
     *
     *    plusPhrase = '+' phrase;
     */
    private Parser plusPhrase() {
        return new Seq("plusPhrase").see('+').get(phrase()).put(new ArithmeticAssembler('+'));
    }

    /*
     * Return a parser that recognizes the grammar:
     *
     *    ruleDef = ":-" commaList(condition);
     */
    private Parser ruleDef() {
        return new SeqEx("rule definition")
                .see(":-").get(commaList(condition()));
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
    private Parser structure() {
        if (structure == null) {
            structure = new Seq("structure");
            structure.get(functor()).get(new Alternation(
                    new SeqEx("list in parens")
                            .get('(') 
                            .get(commaList(term()))
                            .see(')')
                            .put(new StructureWithTermsAssembler()),
                    new Empty().put(new AtomAssembler())
            ));
        }
        return structure;
    }

    /*
     * Return a parser that recognizes the grammar:
     *
     *    term = structure | Num | list | variable;
     */
    private Parser term() {
        return new Alternation("term",
                structure(), num(), list(), variable());
    }

    /*
     * Return a parser that recognizes the grammar:
     *
     *    timesFactor = '*' factor;
     */
    private Parser timesFactor() {
        return new Seq("timesFactor").see('*').get(factor()).put(
                new ArithmeticAssembler('*'));
    }

    /*
     * Return a parser that recognizes the grammar:
     *
     *    variable = UppercaseWord | '_';
     *
     * The underscore represents and will translate to an
     * anonymous variable.
     */
    private Parser variable() {
        return new Alternation(
                new UppercaseWord().push(a -> {
                    /**
                     * Pops a string like "X" or "Person" from an assembly's stack
                     * and pushes a variable with that name.
                     *
                     * @param  Assembly  the assembly to work on
                     */
                    return new Variable(((Token) a.pop()).sval());
                }),
                new Symbol('_').ok().push(a -> {
                    /**
                     * Pushes an anonymous variable onto an assembly's stack.
                     *
                     * @param  Assembly  the assembly to work on
                     */
                    return new Anonymous();
                }));
    }
}
