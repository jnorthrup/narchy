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
import nars.derive.match.Ellipsis;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.var.UnnormalizedVariable;
import nars.time.Tense;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;

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



    public Rule TermCommandTask() {
        return sequence(
                Term(),
                s(),
                eof(),
                push(newTask(1f,';', the(pop()), null, Tense.Eternal))
        );
    }

    public Rule Task() {

        Var<Float> budget = new Var();
        Var<Character> punc = new Var(Op.COMMAND);
        Var<Truth> truth = new Var();
        Var<Tense> tense = new Var(Tense.Eternal);

        return sequence(

                optional(Budget(budget)),

                Term(),

                s(),

                SentencePunctuation(punc), s(),

                optional(Tense(tense), s()),

                optional(Truth(truth, tense), s()),

                push(newTask(budget.get(), punc.get(),  the(pop()), truth.get(), tense.get()))

        );
    }

    public static Object newTask(Float budget, Character punc, Term term, Truth truth, Tense tense) {
        return new Object[]{budget, term, punc, truth, tense };
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

                        swap() && truth.set(new PreciseTruth((float) pop(), (float) pop()))
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
                        seq(COMPOUND_TERM_CLOSER, push(ZeroProduct)),// nonNull($.exec((Term)pop())) )),
                        MultiArgTerm(PROD, COMPOUND_TERM_CLOSER, false, false)
                ),

                push( INH.the(DTERNAL, (Term) pop(), $.the(pop())))

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

                Term(),
                oneOrMore(sequence(

                        sepArgSep(),

                        Term()
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
                Term(),
                s(),
                firstOf(
                        seq(OpTemporal(), CycleDelta()),
                        seq(OpTemporalParallel(), push(0) /* dt=0 */)
                ),
                s(),
                Term(),
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

            while (count < max && Narsese.isValidAtomChar(context.getCurrentChar())) {
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
                seq(s(), /*optional*/(ARGUMENT_SEPARATOR), s()),
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

                "||", s(),push(Op.PROD),

                oneOrMore(sequence(
                        sepArgSep(),
                        Term()
                )),

                s(),

                ')',

                push(conj2disj(popTerms(new Op[] { PROD } /* HACK */)))
        );
    }

    static Term conj2disj(List<Term> subterms) {
        subterms.replaceAll(Term::neg);
        return CONJ.the(DTERNAL, subterms).neg();
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

        FasterList tt = new FasterList(8);

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
