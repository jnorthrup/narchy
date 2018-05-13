package nars;

import com.github.fge.grappa.annotations.Cached;
import com.github.fge.grappa.matchers.MatcherType;
import com.github.fge.grappa.matchers.base.AbstractMatcher;
import com.github.fge.grappa.parsers.BaseParser;
import com.github.fge.grappa.rules.Rule;
import com.github.fge.grappa.run.context.MatcherContext;
import com.github.fge.grappa.stack.ArrayValueStack;
import com.github.fge.grappa.support.Var;
import jcog.Texts;
import jcog.list.FasterList;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.obj.QuantityTerm;
import nars.term.var.UnnormalizedVariable;
import nars.truth.PreciseTruth;
import nars.unify.match.Ellipsis;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tec.uom.se.AbstractQuantity;

import java.util.List;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL_ETERNAL;

public class NarseseParser extends BaseParser<Object> implements Narsese.INarseseParser {

    @Override
    public Rule Input() {
        return sequence(
                zeroOrMore(

                        s(),
                        firstOf(
                                LineComment(),
                                Task(),
                                TermCommandTask()
                        )


                ),
                s()
        );
    }

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

    public Rule TermCommandTask() {
        return sequence(
                Term(),
                s(),
                eof(),
                push(newTask(1f,';', the(pop()), null, ETERNAL_ETERNAL))
        );
    }

    public Rule Task() {

        Var<Float> budget = new Var();
        Var<Character> punc = new Var(Op.COMMAND);
        Var<Object> truth = new Var();
        Var<Object> occurr = new Var(ETERNAL_ETERNAL);

        return sequence(

                optional(Budget(budget)),

                Term(),

                s(),

                SentencePunctuation(punc), s(),

                optional(
                    seq( firstOf(

                        seq(OccurrenceTime(), "..", OccurrenceTime(),
                            occurr.set(new Object[] { pop(1), pop() })),

                        seq(OccurrenceTime(), occurr.set(pop()))

                    ), s() )
                ),

                optional(Truth(truth), s()),

                push(newTask(budget.get(), punc.get(),  the(pop()), truth.get(), occurr.get()))

        );
    }

    static Object newTask(Float budget, Character punc, Term term, Object truth, Object occ) {
        return new Object[]{budget, term, punc, truth, occ };
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

//    public Rule Tense(Var<Tense> tense) {
//        return firstOf(
//                sequence(TENSE_PRESENT, tense.set(Tense.Present)),
//                sequence(TENSE_PAST, tense.set(Tense.Past)),
//                sequence(TENSE_FUTURE, tense.set(Tense.Future))
//        );
//    }

    public Rule Truth(Var<Object> truth) {
        return sequence(

                TRUTH_VALUE_MARK,

                ShortFloat(), //Frequency

                firstOf(

                    sequence(

                            //TruthTenseSeparator(VALUE_SEPARATOR, tense), // separating ;,|,/,\
                            ";",

                            ShortFloat(), //Conf

                            optional(TRUTH_VALUE_MARK), //tailing '%' is optional

                            swap() && truth.set(new PreciseTruth((float) pop(), (float) pop()))
                    ),

                    seq(TRUTH_VALUE_MARK, truth.set(pop()))
                )
                        /*,

                        sequence(
                                TRUTH_VALUE_MARK, //tailing '%'

                                truth.set(new DefaultTruth((float) pop() ))
                        )*/
                //)
        );
    }

    public Rule TruthTenseSeparator(char defaultChar, Var<Tense> tense) {
        return firstOf(
                defaultChar,
                sequence('|', tense.set(Tense.Present)),
                sequence('\\', tense.set(Tense.Past)),
                sequence('/', tense.set(Tense.Future))
        );
    }


    public Rule ShortFloat() {
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


    public Rule SentencePunctuation(Var<Character> punc) {

        return sequence(trie(".", "?", "!", "@", ";"), punc.set(matchedChar()));
        //return sequence(anyOf(".?!@;"), punc.set(matchedChar()));
    }


    @Override
    public Rule Term() {
        return Term(true, true);
    }

    //    Rule nothing() {
    //        return new NothingMatcher();
    //    }


    @Cached
    public Rule Term(boolean oper, boolean temporal) {
        /*
                 <term> ::= <word>                             // an atomic constant term
                        | <variable>                         // an atomic variable term
                        | <compound-term>                    // a term with internal structure
                        | <statement>                        // a statement can serve as a term
        */

        return seq( s(),

                firstOf(

                        QuotedAtom(),

                        seq(oper, ColonReverseInheritance()),



                        //TODO match Ellipsis as an optional continuation of the prefix variable that was already parsed.\
                        //popping the pushed value should be all that's needed to do this
                        //and it should reduce the redundancy and need to run Ellipsis first
                        //same for ColonReverseInheritance, just continue and wrap


                        Ellipsis(),


                        seq(SETe.str,

                                MultiArgTerm(SETe, SET_EXT_CLOSER, false, false)

                        ),

                        seq(SETi.str,

                                MultiArgTerm(SETi, SET_INT_CLOSER, false, false)

                        ),

                        //Functional form of an Operation, ex: operate(p1,p2), TODO move to FunctionalOperationTerm() rule
                        seq(oper, Function()),

                        seq(temporal, TemporalRelation()),

                        seq(COMPOUND_TERM_OPENER, s(),
                                firstOf(

                                        //                                        sequence(Term(), s(), "-{-", s(), Term(), s(), COMPOUND_TERM_CLOSER, push($.inst((Term) popTerm(null), (Term) popTerm(null)))),
                                        //                                        sequence(Term(), s(), "-]-", s(), Term(), s(), COMPOUND_TERM_CLOSER, push($.prop((Term) popTerm(null), (Term) popTerm(null)))),
                                        //                                        sequence(Term(), s(), "{-]", s(), Term(), s(), COMPOUND_TERM_CLOSER, push($.instprop(the(peek()).sub(1), the(pop()).sub(0)))),

                                        sequence(
                                                COMPOUND_TERM_CLOSER, push(EmptyProduct)
                                        ),

                                        CompoundPrefix(),

                                        CompoundInfix(),

                                        MultiArgTerm(null, COMPOUND_TERM_CLOSER, true, false),

                                        //default to product if no operator specified in ( )
                                        MultiArgTerm(null, COMPOUND_TERM_CLOSER, false, false)


                                )

                        ),

                        NumberAtom(),

                        AtomStr(),

                        Variable(),

                        //negation shorthand
                        seq(NEG.str, Term(), push(($.the(pop())).neg())),

                        //deprecated form: <a --> b>
                        seq(OLD_STATEMENT_OPENER,
                                MultiArgTerm(null, OLD_STATEMENT_CLOSER, false, true)
                        )


                ),

                s()
        );
    }

    public Rule Function() {
        return seq(

                AtomStr(),
                //Term(false, false), //<-- allows non-atom terms for operator names
                //Atom(), //push(nonNull($.oper((String)pop()))), // <-- allows only atoms for operator names, normal

                COMPOUND_TERM_OPENER, s(),

                firstOf(
                        seq(COMPOUND_TERM_CLOSER, push(EmptyProduct)),// nonNull($.exec((Term)pop())) )),
                        MultiArgTerm(PROD, COMPOUND_TERM_CLOSER, false, false)
                ),

                push(INH.compound(DTERNAL, new Term[]{(Term) pop(), $.the(pop())}))

        );
    }

    public Rule seq(Object rule, Object rule2,
                    Object... moreRules) {
        return sequence(rule, rule2, moreRules);
    }



    @Deprecated
    public Rule TemporalRelation() {

        return seq(

                COMPOUND_TERM_OPENER,
                s(),
                Term(),
                s(),

                seq(OpTemporal(), TimeDelta()),


                s(),
                Term(),
                s(),
                COMPOUND_TERM_CLOSER,


                push(TemporalRelationBuilder(the(pop()) /* pred */,
                        pop() /*cycleDelta*/, (Op) pop() /*relation*/, the(pop()) /* subj */))
        );
    }

    @Nullable
    static Term TemporalRelationBuilder(Term pred, Object timeDelta, Op o, Term subj) {
        if (subj == null || subj == Null || pred == null || pred == Null)
            return null;
        else {
            if (timeDelta instanceof Integer) {
                return o.compound((int)timeDelta, subj, pred);
            } else {
                //encode as a term that will be dynamically decoded as a functor by the receiving NAR according to its clock
                QuantityTerm q = (QuantityTerm)timeDelta;
                return $.func("term", o.strAtom, $.pFast(subj, pred), q);
            }
        }
    }

    public final static String invalidCycleDeltaString = Integer.toString(Integer.MIN_VALUE);

    public Rule TimeDelta() {
        return

                    firstOf(
                            TimeUnit(),

                            seq("+-", push(Tense.XTERNAL)),
                            seq('+', oneOrMore(digit()),
                                    push(Texts.i(matchOrDefault(invalidCycleDeltaString)))
                            ),
                            seq('-', oneOrMore(digit()),
                                    push(-Texts.i(matchOrDefault(invalidCycleDeltaString)))
                            )
                    );

    }

    public Rule TimeUnit() {
        return firstOf(
            seq("-", TimeUnit(true)),
            seq("+", TimeUnit(false))
        );
    }

    public Rule TimeUnit(boolean negate) {
        return
            seq(oneOrMore(anyOf(".0123456789")), push(match()),
                oneOrMore(alpha()), push(1,match()),
                push(new QuantityTerm(
                    AbstractQuantity.parse(
                        pop() + " " +  timeUnitize((String)pop())
                    ).multiply(negate? -1 : +1))
                ))
        ;
    }

    /** translate shortcuts for time units */
    protected static String timeUnitize(String u) {
        switch (u) {
            case "years": return "year";
            case "months": return "month";
            case "weeks": return "week";
            case "days": return "day";
            case "hours": return "h"; //hour
            case "hr": return "h"; //hour
            case "m": return "min"; //min
            case "mins": return "min"; //min
            case "sec": return "s"; //min
            default:
                return u;
        }
    }

    public Rule OccurrenceTime() {
        return
                firstOf(
                        seq(firstOf("now", "|", ":|:"), push(Tense.Present)), //shorthand
                        //seq("now", push(Tense.Present)),
                        TimeUnit(),
                        seq("-", oneOrMore(digit()), push(-Texts.i(match()))),
                        seq("+", oneOrMore(digit()), push(Texts.i(match())))
//                        seq("tomorrow", push("Tomorrow")),
//                        seq("yesterday", push("Yesterday"))
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
    public Rule AtomStr() {
        return seq(
                ValidAtomCharMatcher,
                push(match())
        );
    }

    public Rule NumberAtom() {
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

            while (count < max && Atom.isValidAtomChar(context.getCurrentChar())) {
                context.advanceIndex(1);
                count++;
            }

            return count > 0;
        }
    };


    /**
     * MACRO: y:x    becomes    <x --> y>
     */
    public Rule ColonReverseInheritance() {
        return sequence(
                Term(false, false),
                ':',
                Term(),

                push($.inh(the(pop()), the(pop())))
                ///*push(Compound.class), */push(the(pop())), push(the(pop())),
                // popTerm(Op.INH)

        );
    }


    public Rule QuotedAtom() {
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


    public Rule Ellipsis() {
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

//    Rule AnyStringExceptQuote() {
//        //TODO handle \" escape
//        return zeroOrMore(noneOf("\""));
//    }
//
//    Rule AnyString() {
//        return zeroOrMore(ANY);
//    }

    Rule Variable() {
        return firstOf(
                seq("_", push(Op.Imdex)),
                seq(Op.VAR_INDEP.ch, Variable(VAR_INDEP)),
                seq(Op.VAR_DEP.ch, Variable(VAR_DEP)),
                seq(Op.VAR_QUERY.ch, Variable(VAR_QUERY)),
                seq(Op.VAR_PATTERN.ch, Variable(VAR_PATTERN))
            );
    }

    Rule Variable(Op varType) {
        return seq(
            AtomStr(),
            push($.v(varType, (String) pop()))
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

                push(Op.theIfPresent(match()))
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


    Rule sepArgSep() {
        return firstOf(
                seq(s(), /*optional*/(ARGUMENT_SEPARATOR), s()),
                ss()
        );
    }


    private static final Object functionalForm = new Object();

    /**
     * list of terms prefixed by a particular compound term operate
     */
    @Deprecated Rule MultiArgTerm(@Nullable Op defaultOp, char close, boolean initialOp, boolean allowInternalOp) {

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
    @Cached Rule CompoundPrefix() {

        return sequence(

                firstOf(
                    Op.DISJstr,
                        "&|",
                        Op.SECTe.str
                ), push(match()),

                push(Compound.class),
                push(Op.PROD),

                oneOrMore(sequence(
                        sepArgSep(),
                        Term()
                )),

                s(),

                COMPOUND_TERM_CLOSER,

                push(buildCompound(popTerms(new Op[] { PROD } /* HACK */), (String)pop()))
        );
    }

    static Term buildCompound(List<Term> subs, String op) {
        switch (op) {
            case DISJstr:
                subs.replaceAll(Term::neg);
                return CONJ.the(DTERNAL, subs).neg();
            case "&|":
                return CONJ.the(0, subs);
            case "=|>":
                return IMPL.the(0, subs);
            case "-{-":
                return subs.size() != 2 ? Null : $.inst(subs.get(0), subs.get(1));
            case "-]-":
                return subs.size() != 2 ? Null : $.prop(subs.get(0), subs.get(1));
            case "{-]":
                return subs.size() != 2 ? Null : $.instprop(subs.get(0), subs.get(1));
            default: {
                Op o = Op.stringToOperator.get(op);
                if (o == null)
                    throw new UnsupportedOperationException();
                return o.the(subs);
            }
        }
    }

    @Cached Rule CompoundInfix() {

        return sequence(


                push(Compound.class),

                Term(),
                s(),
                firstOf(
                        Op.DISJstr,
                        Op.SECTi.str,
                        Op.SECTe.str,
                        Op.INH.str,
                        Op.SIM.str,
                        Op.IMPL.str,
                        Op.DIFFi.str,
                        Op.DIFFe.str,
                        Op.PROD.str,
                        Op.CONJ.str,
                        "&|",
                        "=|>",
                        "-{-",
                        "-]-",
                        "{-]"
                    ), push(2, match()),
                s(),
                Term(),
                s(),
                COMPOUND_TERM_CLOSER,

                push(buildCompound(popTerms(new Op[] { PROD } /* HACK */), (String)pop()))
        );
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
        if (vectorterms == null)
            return Null;

        op = opp[0];

        if (op == null)
            op = PROD;

        return op.the(DTERNAL, vectorterms);
    }

    FasterList<Term> popTerms(Op[] op /* hint */) {

        FasterList<Term> tt = new FasterList(8);

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
                if (p == Null) {
                    stack.clear();
                    return new FasterList(1).addingAll(Null);
                }
                tt.add((Term)p);
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
}
