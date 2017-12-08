package nars;

import com.github.fge.grappa.annotations.Cached;
import com.github.fge.grappa.matchers.MatcherType;
import com.github.fge.grappa.matchers.base.AbstractMatcher;
import com.github.fge.grappa.matchers.base.Matcher;
import com.github.fge.grappa.parsers.BaseParser;
import com.github.fge.grappa.rules.Rule;
import com.github.fge.grappa.run.ParseRunner;
import com.github.fge.grappa.run.ParsingResult;
import com.github.fge.grappa.run.context.MatcherContext;
import com.github.fge.grappa.stack.ArrayValueStack;
import com.github.fge.grappa.stack.ValueStack;
import com.github.fge.grappa.support.Var;
import com.github.fge.grappa.transform.ParserTransformer;
import jcog.Texts;
import jcog.list.FasterList;
import nars.derive.match.Ellipsis;
import nars.task.TaskBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.var.UnnormalizedVariable;
import nars.time.Tense;
import nars.truth.DiscreteTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static nars.$.neg;
import static nars.Op.*;
import static nars.term.Term.nullIfNull;
import static nars.time.Tense.DTERNAL;

/**
 * NARese, syntax and language for interacting with a NAR in NARS.
 * https://code.google.com/p/open-nars/wiki/InputOutputFormat
 */
public class Narsese extends BaseParser<Object> {


    public static final String NARSESE_TASK_TAG = "Narsese";

    static class MyParseRunner extends ParseRunner {

        /**
         * Constructor
         *
         * @param rule the rule
         */
        public MyParseRunner(Rule rule) {
            super(rule);
        }

        @Override
        public boolean match(MatcherContext context) {
            final Matcher matcher = context.getMatcher();

            //final PreMatchEvent<T> preMatchEvent = new PreMatchEvent<>(context);
            //bus.post(preMatchEvent);

//            if (throwable != null)
//                throw new GrappaException("parsing listener error (before match)",
//                        throwable);

            // FIXME: is there any case at all where context.getMatcher() is null?
            @SuppressWarnings("ConstantConditions") final boolean match = matcher.match(context);
//
//            final MatchContextEvent<T> postMatchEvent = match
//                    ? new MatchSuccessEvent<>(context)
//                    : new MatchFailureEvent<>(context);

            //bus.post(postMatchEvent);

//            if (throwable != null)
//                throw new GrappaException("parsing listener error (after match)",
//                        throwable);

            return match;
        }

    }

    //These should be set to something like RecoveringParseRunner for performance
    private final ParseRunner inputParser = new MyParseRunner(Input());

    //private final ParseRunner singleTaskRuleParser = new ListeningParseRunner3(TaskRule());

    //private final Map<String,Term> termCache = new HashMap();

    static final Class parser;

    static {
        Class p;
        try {
            p = ParserTransformer.extendParserClass(Narsese.class).getExtendedClass();
        } catch (Exception e) {
            e.printStackTrace();
            p = null;
        }
        parser = p;
    }

    static final ThreadLocal<Narsese> parsers = ThreadLocal.withInitial(
            //() -> Grappa.createParser(Narsese.class)
            () -> {
                try {
                    return (Narsese) parser.getConstructor().newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
    );

    static final ThreadLocal<ParseRunner> singleTermParsers = ThreadLocal.withInitial(
            //() -> Grappa.createParser(Narsese.class)
            () -> {
                Narsese n = parsers.get();
                return new MyParseRunner(n.Term());
            }
    );
//    static final ThreadLocal<Map<Pair<Op, List>, Term>> vectorTerms = ThreadLocal.withInitial(() ->
//            new CapacityLinkedHashMap<Pair<Op, List>, Term>(512));


    public static Narsese parse() {
        return parsers.get();
    }


    public Rule Input() {
        return sequence(
                zeroOrMore(

                        firstOf(
                                LineComment(),
                                Task()
                        )


                ),
                s() //optional trailing whitespace
        );
    }

//    /**
//     * {Premise1,Premise2} |- Conclusion.
//     */
//    public Rule TaskRule() {
//
//        //use a var to count how many rule conditions so that they can be pulled off the stack without reallocating an arraylist
//        return sequence(
//                STATEMENT_OPENER, s(),
//                push(PremiseRule.class),
//
//                Term(), //cause
//
//                zeroOrMore(sepArgSep(), Term()),
//                s(), TASK_RULE_FWD, s(),
//
//                push(PremiseRule.class), //stack marker
//
//                Term(), //effect
//
//                zeroOrMore(sepArgSep(), Term()),
//                s(), STATEMENT_CLOSER, s(),
//
//                eof(),
//
//                push(popTaskRule())
//        );
//    }


//    @Nullable
//    public PremiseRule popTaskRule() {
//        //(Term)pop(), (Term)pop()
//
//        List<Term> r = $.newArrayList(16);
//        List<Term> l = $.newArrayList(16);
//
//        Object popped;
//        while ((popped = pop()) != PremiseRule.class) { //lets go back till to the start now
//            r.add(the(popped));
//        }
//        if (r.isEmpty()) //empty premise list is invalid
//            return null;
//
//        while ((popped = pop()) != PremiseRule.class) {
//            l.add(the(popped));
//        }
//        if (l.isEmpty()) //empty premise list is invalid
//            return null;
//
//
//        Collections.reverse(l);
//        Collections.reverse(r);
//
//        Compound premise = $.p(l);
//        Compound conclusion = $.p(r);
//
//        return new PremiseRule(premise, conclusion);
//    }

    public Rule LineComment() {
        return sequence(
                s(),
                //firstOf(
                "//",
                //"'",
                //sequence("***", zeroOrMore('*')), //temporary
                //"OUT:"
                //),
                //sNonNewLine(),
                //LineCommentEchoed(),
                zeroOrMore(noneOf("\n")),
                firstOf("\n", eof() /* may not have newline at end of file */)
        );
    }

//    public Rule LineCommentEchoed() {
//        //return Atom.the(Utf8.toUtf8(name));
//
//        //return $.the('"' + t + '"');
//
////        int olen = name.length();
////        switch (olen) {
////            case 0:
////                throw new RuntimeException("empty atom name: " + name);
////
//////            //re-use short term names
//////            case 1:
//////            case 2:
//////                return theCached(name);
////
////            default:
////                if (olen > Short.MAX_VALUE/2)
////                    throw new RuntimeException("atom name too long");
//
//        //  }
//        return sequence(
//                zeroOrMore(noneOf("\n")),
//                push(ImmediateOperator.command(echo.class, $.quote(match())))
//        );
//    }

//    public Rule PauseInput() {
//        return sequence( s(), IntegerNonNegative(),
//                push( PauseInput.pause( (Integer) pop() ) ), sNonNewLine(),
//                "\n" );
//    }


//    public Rule TermEOF() {
//        return sequence( s(), Term(), s(), eof() );
//    }
//    public Rule TaskEOF() {
//        return sequence( s(), Task(), s(), eof() );
//    }

    public Rule Task() {

        Var<Float> budget = new Var();
        Var<Character> punc = new Var(Op.COMMAND);
        Var<Term> term = new Var();
        Var<Truth> truth = new Var();
        Var<Tense> tense = new Var(Tense.Eternal);

        return sequence(
                s(),

                optional(Budget(budget)),

                Term(true, false, true), term.set(the(pop())),

                s(),

                SentencePunctuation(punc), s(),

                optional(Tense(tense), s()),

                optional(Truth(truth, tense), s()),

                push(new Object[]{budget.get(), term.get(), punc.get(), truth.get(), tense.get()})
                //push(getTask(budget, term, punc, truth, tense))

        );
    }


    public Rule Budget(Var<Float> budget) {
        return sequence(
                BUDGET_VALUE_MARK,

                ShortFloat(),

//                firstOf(
//                        BudgetPriorityDurabilityQuality(budget),
//                        BudgetPriorityDurability(budget),
                BudgetPriority(budget),
//                ),

                optional(BUDGET_VALUE_MARK)
        );
    }

    boolean BudgetPriority(Var<Float> budget) {
        return budget.set((Float) (pop()));
    }

//    public Rule BudgetPriorityDurability(Var<float[]> budget) {
//        return sequence(
//                VALUE_SEPARATOR, ShortFloat(),
//                budget.set(new float[]{(float) pop(), (float) pop()}) //intermediate representation
//        );
//    }

//    public Rule BudgetPriorityDurabilityQuality(Var<float[]> budget) {
//        return sequence(
//                VALUE_SEPARATOR, ShortFloat(), VALUE_SEPARATOR, ShortFloat(),
//                budget.set(new float[]{(float) pop(), (float) pop(), (float) pop()}) //intermediate representation
//        );
//    }

    public Rule Tense(Var<Tense> tense) {
        return firstOf(
                sequence(TENSE_PRESENT, tense.set(Tense.Present)),
                sequence(TENSE_PAST, tense.set(Tense.Past)),
                sequence(TENSE_FUTURE, tense.set(Tense.Future))
        );
    }

    public Rule Truth(Var<Truth> truth, Var<Tense> tense) {
        return sequence(

                TRUTH_VALUE_MARK,

                ShortFloat(), //Frequency

                //firstOf(

                sequence(

                        TruthTenseSeparator(VALUE_SEPARATOR, tense), // separating ;,|,/,\

                        ShortFloat(), //Conf

                        optional(TRUTH_VALUE_MARK), //tailing '%' is optional

                        swap() && truth.set(new DiscreteTruth((float) pop(), (float) pop()))
                )
                        /*,

                        sequence(
                                TRUTH_VALUE_MARK, //tailing '%'

                                truth.set(new DefaultTruth((float) pop() ))
                        )*/
                //)
        );
    }

    Rule TruthTenseSeparator(char defaultChar, Var<Tense> tense) {
        return firstOf(
                defaultChar,
                sequence('|', tense.set(Tense.Present)),
                sequence('\\', tense.set(Tense.Past)),
                sequence('/', tense.set(Tense.Future))
        );
    }


    Rule ShortFloat() {
        return sequence(
                sequence(
                        optional(digit()),
                        optional('.', oneOrMore(digit()))
                ),
                push(Texts.f(matchOrDefault("NaN"), 0, 1.0f))
        );
    }


//    Rule IntegerNonNegative() {
//        return sequence(
//                oneOrMore(digit()),
//                push(Integer.parseInt(matchOrDefault("NaN")))
//        );
//    }

//    Rule Number() {
//
//        return sequence(
//                sequence(
//                        optional('-'),
//                        oneOrMore(digit()),
//                        optional('.', oneOrMore(digit()))
//                ),
//                push(Float.parseFloat(matchOrDefault("NaN")))
//        );
//    }


    Rule SentencePunctuation(Var<Character> punc) {

        return sequence(trie(".", "?", "!", "@", ";"), punc.set(matchedChar()));
        //return sequence(anyOf(".?!@;"), punc.set(matchedChar()));
    }


    public Rule Term() {
        return Term(true, true, true);
    }

//    Rule nothing() {
//        return new NothingMatcher();
//    }


    @Cached
    Rule Term(boolean oper, boolean meta, boolean temporal) {
        /*
                 <term> ::= <word>                             // an atomic constant term
                        | <variable>                         // an atomic variable term
                        | <compound-term>                    // a term with internal structure
                        | <statement>                        // a statement can serve as a term
        */

        return seq(
                s(),
                firstOf(

                        QuotedAtom(),

                        seq(oper, ColonReverseInheritance()),

                        //negation shorthand
                        seq(NEG.str, Term(true, false, true), push(($.the(pop())).neg())),


                        //TODO match Ellipsis as an optional continuation of the prefix variable that was already parsed.\
                        //popping the pushed value should be all that's needed to do this
                        //and it should reduce the redundancy and need to run Ellipsis first
                        //same for ColonReverseInheritance, just continue and wrap


                        seq(meta, Ellipsis()),


                        seq(SETe.str,

                                MultiArgTerm(SETe, SET_EXT_CLOSER, false, false)

                        ),

                        seq(SETi.str,

                                MultiArgTerm(SETi, SET_INT_CLOSER, false, false)

                        ),


                        //Functional form of an Operation, ex: operate(p1,p2), TODO move to FunctionalOperationTerm() rule
                        seq(oper,

                                Atom(),
                                //Term(false, false), //<-- allows non-atom terms for operator names
                                //Atom(), //push(nonNull($.oper((String)pop()))), // <-- allows only atoms for operator names, normal

                                COMPOUND_TERM_OPENER, s(),

                                //push((pop())),

                                firstOf(
                                        seq(COMPOUND_TERM_CLOSER, push(ZeroProduct)),// nonNull($.exec((Term)pop())) )),
                                        MultiArgTerm(PROD, COMPOUND_TERM_CLOSER, false, false)
                                ),

                                push(INH.the(DTERNAL, (Term) pop(), $.the(pop())))

                        ),

                        seq(temporal, TemporalRelation()),

                        seq(COMPOUND_TERM_OPENER, s(),
                                firstOf(

//                                        sequence(Term(), s(), "-{-", s(), Term(), s(), COMPOUND_TERM_CLOSER, push($.inst((Term) popTerm(null), (Term) popTerm(null)))),
//                                        sequence(Term(), s(), "-]-", s(), Term(), s(), COMPOUND_TERM_CLOSER, push($.prop((Term) popTerm(null), (Term) popTerm(null)))),
//                                        sequence(Term(), s(), "{-]", s(), Term(), s(), COMPOUND_TERM_CLOSER, push($.instprop(the(peek()).sub(1), the(pop()).sub(0)))),

                                        sequence(
                                                COMPOUND_TERM_CLOSER, push(ZeroProduct)
                                        ),

                                        MultiArgTerm(null, COMPOUND_TERM_CLOSER, true, false),
                                        Disj(),

                                        //default to product if no operator specified in ( )
                                        MultiArgTerm(null, COMPOUND_TERM_CLOSER, false, false),

                                        MultiArgTerm(null, COMPOUND_TERM_CLOSER, false, true),

                                        ConjunctionParallel()

                                )

                        ),


                        NumberAtom(),

                        Atom(),

                        Variable(),

                        //deprecated form: <a --> b>
                        seq(OLD_STATEMENT_OPENER,
                                MultiArgTerm(null, OLD_STATEMENT_CLOSER, false, true)
                        )


                ),

                firstOf(eof(), s()),

                //ATOM
                push((pop()))

        );
    }

    public Rule seq(Object rule, Object rule2,
                    Object... moreRules) {
        return sequence(rule, rule2, moreRules);
    }


    //TODO not working right
    public Rule ConjunctionParallel() {
        return seq(

                "&|", s(), ",", s(),

                Term(true, false, true),
                oneOrMore(sequence(

                        sepArgSep(),

                        Term(true, false, true)
                )),
                s(),
                COMPOUND_TERM_CLOSER,

                push(CONJ.the(0, popTerms(null)) /* HACK construct a dt=0 copy */)
        );
    }

    @Deprecated
    public Rule TemporalRelation() {
        return seq(

                COMPOUND_TERM_OPENER,
                s(),
                Term(true, false, true),
                s(),
                firstOf(
                        seq(OpTemporal(), CycleDelta()),
                        seq(OpTemporalParallel(), push(0) /* dt=0 */)
                ),
                s(),
                Term(true, false, true),
                s(),
                COMPOUND_TERM_CLOSER,


                push(TemporalRelationBuilder(the(pop()) /* pred */,
                        (Integer) pop() /*cycleDelta*/, (Op) pop() /*relation*/, the(pop()) /* subj */))
        );
    }

    @Nullable
    static Term TemporalRelationBuilder(Term pred, int cycles, Op o, Term subj) {
        if (subj == null || subj == Null || pred == null || pred == Null)
            return null;
        return o.the(cycles, subj, pred);
    }

    public final static String invalidCycleDeltaString = Integer.toString(Integer.MIN_VALUE);

    public Rule CycleDelta() {
        return
                firstOf(
                        seq("+-", push(Tense.XTERNAL)),
                        seq('+', oneOrMore(digit()),
                                push(Integer.parseInt(matchOrDefault(invalidCycleDeltaString)))
                        ),
                        seq('-', oneOrMore(digit()),
                                push(-Integer.parseInt(matchOrDefault(invalidCycleDeltaString)))
                        )
                )
                ;
    }

//    public Rule Operator() {
//        return sequence(OPER.ch,
//                Atom(), push($.oper((String)pop())));
//                //Term(false, false),
//                //push($.operator(pop().toString())));
//    }


    /**
     * an atomic term, returns a String because the result may be used as a Variable name
     */
    Rule Atom() {
        return seq(
                ValidAtomCharMatcher,
                push(match())
        );
    }

    Rule NumberAtom() {
        return seq(

                seq(
                        optional('-'),
                        oneOrMore(digit()),
                        optional('.', oneOrMore(digit()))
                ),

                push($.the(Float.parseFloat(matchOrDefault("NaN"))))
        );
    }


    static final AbstractMatcher ValidAtomCharMatcher = new AbstractMatcher("'ValidAtomChar'") {


        @NotNull
        @Override
        public MatcherType getType() {
            return MatcherType.TERMINAL;
        }

        @Override
        public <V> boolean match(MatcherContext<V> context) {
            int count = 0;
            int max = context.getInputBuffer().length() - context.getCurrentIndex();

            while (count < max && isValidAtomChar(context.getCurrentChar())) {
                context.advanceIndex(1);
                count++;
            }

            return count > 0;
        }
    };

    public static boolean isValidAtomChar(char x) {

        //TODO replace these with Symbols. constants
        switch (x) {
            case ' ':
            case ARGUMENT_SEPARATOR:
            case BELIEF:
            case GOAL:
            case QUESTION:
            case QUEST:
            case COMMAND:

            case '^':

            case '<':
            case '>':

            case '~':
            case '=':

            case '+':
            case '-':
            case '*':

            case '|':
            case '&':
            case '(':
            case ')':
            case '[':
            case ']':
            case '{':
            case '}':
            case '%':
            case '#':
            case '$':
            case ':':
            case '`':

            case '\"':
            case '\'':

            case '\t':
            case '\n':
            case '\r':
            case 0:
                return false;
        }
        return true;
    }


    /**
     * MACRO: y:x    becomes    <x --> y>
     */
    Rule ColonReverseInheritance() {
        return sequence(
                Term(false, false, false),
                ':',
                Term(true, false, true),

                push($.inh(the(pop()), the(pop())))
                ///*push(Compound.class), */push(the(pop())), push(the(pop())),
                // popTerm(Op.INH)

        );
    }


    Rule QuotedAtom() {
        return sequence(
                dquote(), //leading quote
                firstOf(
                        //multi-line TRIPLE quotes
                        seq(regex("\"\"[\\s\\S]+\"\"\""), push(Atomic.the('\"' + match()))),

                        //one quote
                        seq(
                                //regex("[\\s\\S]+\""),
                                regex("(?:[^\"\\\\]|\\\\.)*\""),
                                push(Atomic.the('\"' + match())))
                )
        );
    }


    Rule Ellipsis() {
        return sequence(
                Variable(), "..",
                firstOf(

//                        seq("_=", Term(false, false), "..+",
//                                swap(2),
//                                push(new Ellipsis.EllipsisTransformPrototype(/*Op.VAR_PATTERN,*/
//                                        (Variable) pop(), Op.Imdex, (Term) pop()))
//                        ),
//                        seq(Term(false, false), "=_..+",
//                                swap(2),
//                                push(new Ellipsis.EllipsisTransformPrototype(/*Op.VAR_PATTERN,*/
//                                        (Variable) pop(), (Term) pop(), Op.Imdex))
//                        ),
                        seq("+",
                                push(new Ellipsis.EllipsisPrototype(Op.VAR_PATTERN, (UnnormalizedVariable) pop(), 1))
                        ),
                        seq("*",
                                push(new Ellipsis.EllipsisPrototype(Op.VAR_PATTERN, (UnnormalizedVariable) pop(), 0))
                        )
                )
        );
    }

    Rule AnyStringExceptQuote() {
        //TODO handle \" escape
        return zeroOrMore(noneOf("\""));
    }


    Rule AnyString() {
        return zeroOrMore(ANY);
    }


    Rule Variable() {
        /*
           <variable> ::= "$"<word>                          // independent variable
                        | "#"[<word>]                        // dependent variable
                        | "?"[<word>]                        // query variable in question
                        | "%"[<word>]                        // pattern variable in rule
        */
        return
                firstOf(
                        seq("_", push(Op.Imdex)),
                        seq(
                                anyOf(new char[]{
                                        Op.VAR_INDEP.ch,
                                        Op.VAR_DEP.ch,
                                        Op.VAR_QUERY.ch,
                                        Op.VAR_PATTERN.ch
                                }),
                                push(match()),
                                Atom(),
                                swap(),
                                push($.v(((CharSequence) pop()).charAt(0), (String) pop()))
                        )
                );
    }

    //Rule CompoundTerm() {
        /*
         <compound-term> ::= "{" <term> {","<term>} "}"         // extensional set
                        | "[" <term> {","<term>} "]"         // intensional set
                        | "(&," <term> {","<term>} ")"       // extensional intersection
                        | "(|," <term> {","<term>} ")"       // intensional intersection
                        | "(*," <term> {","<term>} ")"       // product
                        | "(/," <term> {","<term>} ")"       // extensional image
                        | "(\," <term> {","<term>} ")"       // intensional image
                        | "(||," <term> {","<term>} ")"      // disjunction
                        | "(&&," <term> {","<term>} ")"      // conjunction
                        | "(&/," <term> {","<term>} ")"      // (sequential events)
                        | "(&|," <term> {","<term>} ")"      // (parallel events)
                        | "(--," <term> ")"                  // negation
                        | "(-," <term> "," <term> ")"        // extensional difference
                        | "(~," <term> "," <term> ")"        // intensional difference
        
        */

    //}

    Rule Op() {
        return sequence(
                trie(
                        SECTe.str, SECTi.str,
                        DIFFe.str, DIFFi.str,
                        PROD.str,

                        INH.str,

                        SIM.str,


                        NEG.str,

                        IMPL.str,

                        CONJ.str


                ),

                push(Op.the(match()))
        );
    }

    Rule OpTemporal() {
        return sequence(
                trie(
                        IMPL.str,
                        CONJ.str
                ),
                push(Op.the(match()))
        );
    }

    Rule OpTemporalParallel() {
        return firstOf(
//                seq("<|>", push(EQUI)),
                seq("=|>", push(IMPL)),
                seq("&|", push(CONJ))
        );
    }

    Rule sepArgSep() {
        return firstOf(
                seq(s(), optional(ARGUMENT_SEPARATOR), s()),
                ss()
        );
    }


    private static final Object functionalForm = new Object();

    /**
     * list of terms prefixed by a particular compound term operate
     */
    Rule MultiArgTerm(@Nullable Op defaultOp, char close, boolean initialOp, boolean allowInternalOp) {

        return sequence(

                /*operatorPrecedes ? *OperationPrefixTerm()* true :*/

//                operatorPrecedes ?
//                        push(new Object[]{pop(), functionalForm})
//                        :
                push(Compound.class),

                initialOp ? Op() : Term(),

                allowInternalOp ?

                        sequence(s(), Op(), s(), Term())

                        :

                        zeroOrMore(sequence(
                                sepArgSep(),
                                allowInternalOp ? AnyOperatorOrTerm() : Term()
                        )),

                s(),

                close,

                push(popTerm(defaultOp))
        );
    }

    /**
     * HACK
     */
    Rule Disj() {

        return sequence(

                push(Compound.class),

                "||", push(Op.PROD),

                zeroOrMore(sequence(
                        sepArgSep(),
                        Term()
                )),

                s(),

                ')',

                push(conj2disj(popTerm(CONJ)))
        );
    }

    static Term conj2disj(Term x) {
        System.out.println(x);
        Term[] t = x.subterms().arrayShared();

        neg(t);
        return CONJ.the(DTERNAL, t).neg();
    }
//    /**
//     * operation()
//     */
//    Rule EmptyOperationParens() {
//        return sequence(
//
//                OperationPrefixTerm(),
//
//                /*s(),*/ COMPOUND_TERM_OPENER, s(), COMPOUND_TERM_CLOSER,
//
//                push(popTerm(OPERATOR, false))
//        );
//    }

    Rule AnyOperatorOrTerm() {
        return firstOf(Op(), Term());
    }


    @Nullable
    static Term the(@Nullable Object o) {
        if (o instanceof Term) return (Term) o;
        if (o == null) return null; //pass through
        if (o instanceof String) {
            String s = (String) o;
            //return s;
            return Atomic.the(s);

//        int olen = name.length();
//        switch (olen) {
//            case 0:
//                throw new RuntimeException("empty atom name: " + name);
//
////            //re-use short term names
////            case 1:
////            case 2:
////                return theCached(name);
//
//            default:
//                if (olen > Short.MAX_VALUE/2)
//                    throw new RuntimeException("atom name too long");

            //  }
        }
        throw new RuntimeException(o + " is not a term");
    }

    /**
     * produce a term from the terms (& <=1 NALOperator's) on the value stack
     */
    @Nullable
    @Deprecated
    final Term popTerm(Op op /*default */) {

        //System.err.println(getContext().getValueStack());

        Op[] opp = new Op[1];
        opp[0] = op;
        FasterList<Term> vectorterms = popTerms(opp);
        op = opp[0];

        if (op == null)
            op = PROD;

        return op.the(DTERNAL, vectorterms);
    }

    FasterList<Term> popTerms(Op[] op /* hint */) {

        FasterList tt = new FasterList(1);

        ArrayValueStack<Object> stack = (ArrayValueStack) getContext().getValueStack();

//        if (stack.isEmpty())
//            return null;


        while (!stack.isEmpty()) {
            Object p = pop();

            if (p instanceof Object[]) {
                //it's an array so unpack by pushing everything back onto the stack except the last item which will be used as normal below
                Object[] pp = (Object[]) p;
                if (pp.length > 1) {
                    for (int i = pp.length - 1; i >= 1; i--) {
                        stack.push(pp[i]);
                    }
                }

                p = pp[0];
            }


            if (p == functionalForm) {
                op[0] = ATOM;
                break;
            }

            if (p == Compound.class) break; //beginning of stack frame for this term


            if (p instanceof String) {
                //throw new RuntimeException("string not expected here");
                //Term t = $.the((String) p);
                tt.add(Atomic.the((String) p));
            } else if (p instanceof Term) {
                tt.add(p);
            } else if (p instanceof Op) {

//                if (op != null) {
//                    //if ((!allowInternalOp) && (!p.equals(op)))
//                    //throw new RuntimeException("Internal operator " + p + " not allowed here; default op=" + op);
//
//                    throw new NarseseException("Too many operators involved: " + op + ',' + p + " in " + stack + ':' + vectorterms);
//                }

                if (op != null)
                    op[0] = (Op) p;
            }
        }

        tt.reverse();

        return tt;
    }


//    @Nullable
//    public static final Function<Pair<Op, List>, Term> popTermFunction = (x) -> {
//        Op op = x.getOne();
//        List vectorterms = x.getTwo();
//        Collections.reverse(vectorterms);
//
//        for (int i = 0, vectortermsSize = vectorterms.size(); i < vectortermsSize; i++) {
//            Object x1 = vectorterms.get(i);
//            if (x1 instanceof String) {
//                //string to atom
//                vectorterms.set(i, $.the(x1));
//            }
//        }
////        if ((op == null || op == PRODUCT) && (vectorterms.get(0) instanceof Operator)) {
////            op = NALOperator.OPERATION;
////        }
//
//
////        switch (op) {
//////            case OPER:
//////                return $.inh(
//////                        $.p(vectorterms.subList(1, vectorterms.size())),
//////                        $.the(vectorterms.get(0).toString())
//////                );
////            default:
//                return $.compound(op, vectorterms);
////        }
//    };


    /**
     * whitespace, optional
     */
    public Rule s() {
        return zeroOrMore(whitespace());
    }

    /**
     * whitespace, requried
     */
    public Rule ss() {
        return oneOrMore(whitespace());
    }

    public Rule whitespace() {
        return anyOf(" \t\f\n\r");
    }

//    Rule sNonNewLine() {
//        return zeroOrMore(anyOf(" \t\f"));
//    }

//    public static NarseseParser newParser(NAR n) {
//        return newParser(n.memory);
//    }
//
//    public static NarseseParser newParser(Memory m) {
//        NarseseParser np = ;
//        return np;
//    }


    /**
     * returns number of tasks created
     */
    public static void tasks(String input, Collection<Task> c, NAR m) throws NarseseException {
        tasks(input, c::add, m);
    }

    public static List<Task> tasks(String input, NAR m) throws NarseseException {
        List<Task> result = $.newArrayList(1);
        tasks(input, result, m);

        return result;
    }

    /**
     * gets a stream of raw immutable task-generating objects
     * which can be re-used because a Memory can generate them
     * ondemand
     */
    static void tasks(String input, Consumer<Task> c, NAR m) throws NarseseException {
        Narsese p = parse();

        int parsedTasks = 0;

        ParsingResult r = p.inputParser.run(input);

        int size = r.getValueStack().size();

        for (int i = size - 1; i >= 0; i--) {
            Object o = r.getValueStack().peek(i);

            Object[] y;
            if (o instanceof Task) {
                y = (new Object[]{o});
            } else if (o instanceof Object[]) {
                y = ((Object[]) o);
            } else {
                throw new NarseseException("Parse error: " + input);
            }

            Task t = decodeTask(m, y);

            c.accept(t);
            parsedTasks++;

        }

        if (parsedTasks == 0)
            throw new NarseseException("nothing parsed: " + input);


    }


    /**
     * parse one task
     */
    @NotNull
    static public Task task(String input, NAR n) throws NarseseException {
        List<Task> tt = tasks(input, n);
        if (tt.size() != 1)
            throw new NarseseException(tt.size() + " tasks parsed in single-task parse: " + input);
        return tt.get(0);
    }

    /**
     * returns null if the Task is invalid (ex: invalid term)
     */
    @NotNull
    static Task decodeTask(NAR m, Object[] x) {
        if (x.length == 1 && x[0] instanceof Task) {
            return (Task) x[0];
        }

        Term content = ((Term) x[1]).normalize();
        /*if (!(content instanceof Compound)) {
            throw new NarseseException("Task term unnormalizable: " + contentRaw);
            //return Command.task($.func("log", content));
        } else */

        Object px = x[2];

        byte punct =
                px instanceof Byte ?
                        (Byte) x[2]
                        :
                        (byte) (((Character) x[2]).charValue());

        Truth t = (Truth) x[3];
        if (t != null && !Float.isFinite(t.conf()))
            t = $.t(t.freq(), m.confDefault(punct));


        if (t == null && (punct == BELIEF || punct == GOAL)) {
            t = $.t(1, m.confDefault(punct));
        }

        if (content.op() == NEG) {
            content = content.unneg();
            if (t != null)
                t = t.neg();
        }

        TaskBuilder ttt =
                new TaskBuilder(content, punct, t)
                        .time(
                                m.time(), //creation time
                                Tense.getRelativeOccurrence(
                                        (Tense) x[4],
                                        m
                                ));

        long st = ttt.start();
//        if (ttt.start() != ETERNAL && content.op() == CONJ) {
//            int dtRange = content.dtRange();
//            if (dtRange > 0) {
//                ttt.time(ttt.creation(), st, st + dtRange); //extend
//            }
//        }

        if (x[0] == null)  /* do not set, Memory will apply defaults */
            ttt.priSet(m.priDefault(punct));
        else
            ttt.priSet((Float) x[0]);


        return ttt.apply(m).log(NARSESE_TASK_TAG);
    }

//    final static Map<String,byte[]> termCache =
//            //new FileHashMap();
//            new ConcurrentHashMap<>();
//
//
//    @NotNull
//    public static Term term(@NotNull String ss) throws NarseseException {
//        if (ss.length() < 16) {
//            return _term(ss);
//        }
//
//        Term[] t = new Term[1];
//        final NarseseException[] e = new NarseseException[1];
//        byte[] b = termCache.computeIfAbsent(ss, (s) -> {
//            try {
//                t[0] = _term(s);
//            } catch (NarseseException e1) {
//                e[0] = e1;
//                t[0] = Null;
//            }
//            return IO.termToBytes(Null);
//        });
//        if (e[0]!=null)
//            throw e[0];
//        if (t[0]!=null)
//            return t[0];
//        return IO.termFromBytes(b);
//    }

    /**
     * parse one term NOT NORMALIZED
     */
    public static Term term(String s) throws NarseseException {

        Exception ee;
        try {
            //Term x = singleTerms.get(s);

            ParsingResult r = singleTermParsers.get().run(s);

            ValueStack stack = r.getValueStack();

            if (stack.size() == 1) {
                Object x = stack.pop();

                if (x instanceof String)
                    return Atomic.the((String) x);
                else if (x instanceof Term)
                    return (Term) x;
            }

            return Null;

        } catch (Exception e) {
            ee = e;
        }
        throw new NarseseException(s, null, ee);
    }

//    static LoadingCache<String, Term> singleTerms = Caffeine.newBuilder().maximumSize(32 * 1024)
//            .builder((s) -> {
//                ParsingResult r = singleTermParsers.get().run(s);
//
//                ValueStack stack = r.getValueStack();
//
//                if (stack.size() == 1) {
//                    Object x = stack.pop();
//
//                    if (x instanceof String)
//                        return Atomic.the((String) x);
//                    else if (x instanceof Term)
//                        return (Term) x;
//                }
//
//                return Null;
//            });


    public static Term term(String s, boolean normalize) throws NarseseException {

        Term y = term(s);
        if (normalize) {
            return nullIfNull(y.normalize());
        } else {
            return y;
//            Termed existing = index.get(y, false);
//            if (existing == null)
//                return y;
//            else
//                return existing.term();
        }


    }

//    public TaskRule taskRule(String input) {
//        Term x = termRaw(input, singleTaskRuleParser);
//        if (x==null) return null;
//
//        return x.normalizeDestructively();
//    }


//    @Nullable
//    public <T extends Term> T termRaw(CharSequence input) throws NarseseException {
//
//        ParsingResult r = singleTermParser.run(input);
//
//        DefaultValueStack stack = (DefaultValueStack) r.getValueStack();
//        FasterList sstack = stack.stack;
//
//        switch (sstack.size()) {
//            case 1:
//
//
//                Object x = sstack.get(0);
//
//                if (x instanceof String)
//                    x = $.$((String) x);
//
//                if (x != null) {
//
//                    try {
//                        return (T) x;
//                    } catch (ClassCastException cce) {
//                        throw new NarseseException("Term mismatch: " + x.getClass(), cce);
//                    }
//                }
//                break;
//            case 0:
//                return null;
//            default:
//                throw new RuntimeException("Invalid parse stack: " + sstack);
//        }
//
//        return null;
//    }


//	/* The main method! */
//	public static void main(final String... args) {
//		/* The class of our parser */
//		final Class<Narsese> parserClass = Narsese.class;
//
//		/* The constructor repository for our parser */
//		final ParseNodeConstructorProvider repository
//				= new ParseNodeConstructorProvider(parserClass);
//
//		/* The grappa parser! */
//        Narsese parser = Narsese.parsers.get();
//
//
//		/* The runner that listens for events from the parser */
////		final ParseRunner runner
////				= new ParseRunner(parser.Term());
//
//		/* The class that will builder the parse tree */
//		final ParseTreeBuilder listener
//				= new ParseTreeBuilder(repository);
//
////		/* Register the parse tree builder to the runner. This must be done before you run. */
////		runner.registerListener(listener);
////		/* Run on the given input. */
////		runner.run("a:b");
//
//		/* Get the root node of the parse tree built. */
//		final ParseNode rootNode = listener.getTree();
//		System.out.println(rootNode);
//
//		/* Create a visitor runner, and provide the root node to start visiting from. */
//		//VisitorRunner visitorRunner = new VisitorRunner(rootNode);
//
////		/* Create a visitor */
////		ExampleVisitor v = new ExampleVisitor();
////
////		/* Register your visitor. */
////		visitorRunner.registerVisitor(v);
//
//		/* Run the visitors on the parse tree using a defined traversal order. The default is a
//		post order traversal, here we specify a pre order traversal. A third option is breadth
//		first traversal. */
//		//visitorRunner.run(VisitOrder.PREORDER);
//
//		/* Done! */
////		System.out.println(v.getSillySentence());
//	}

    //    /**
//     * interactive parse test
//     */
//    public static void main(String[] args) {
//        NAR n = new NAR(new Default());
//        NarseseParser p = NarseseParser.newParser(n);
//
//        Scanner sc = new Scanner(System.in);
//
//        String input = null; //"<a ==> b>. %0.00;0.9%";
//
//        while (true) {
//            if (input == null)
//                input = sc.nextLine();
//
//            ParseRunner rpr = new ListeningParseRunner<>(p.Input());
//            //TracingParseRunner rpr = new TracingParseRunner(p.Input());
//
//            ParsingResult r = rpr.run(input);
//
//            //p.printDebugResultInfo(r);
//            input = null;
//        }
//
//    }

//    public void printDebugResultInfo(ParsingResult r) {
//
//        System.out.println("valid? " + (r.isSuccess() && (r.getParseErrors().isEmpty())));
//        r.getValueStack().iterator().forEachRemaining(x -> System.out.println("  " + x.getClass() + ' ' + x));
//
//        for (Object e : r.getParseErrors()) {
//            if (e instanceof InvalidInputError) {
//                InvalidInputError iie = (InvalidInputError) e;
//                System.err.println(e);
//                if (iie.getErrorMessage() != null)
//                    System.err.println(iie.getErrorMessage());
//                for (MatcherPath m : iie.getFailedMatchers()) {
//                    System.err.println("  ?-> " + m);
//                }
//                System.err.println(" at: " + iie.getStartIndex() + " to " + iie.getEndIndex());
//            } else {
//                System.err.println(e);
//            }
//
//        }
//
//        System.out.println(printNodeTree(r));
//
//    }


    /**
     * Describes an error that occurred while parsing Narsese
     */
    public static class NarseseException extends Exception {

        @Nullable
        public final ParsingResult result;

        /**
         * An invalid addInput line.
         *
         * @param message type of error
         */
        public NarseseException(String message) {
            super(message);
            this.result = null;
        }

        public NarseseException(String input, Throwable cause) {
            this(input, null, cause);
        }

        public NarseseException(String input, ParsingResult result, Throwable cause) {
            super(input + '\n' + result, cause);
            this.result = result;
        }
    }


}
