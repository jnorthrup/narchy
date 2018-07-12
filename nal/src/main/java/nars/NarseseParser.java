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
import nars.term.Variable;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.obj.QuantityTerm;
import nars.time.Tense;
import nars.truth.PreciseTruth;
import nars.unify.match.Ellipsis;
import org.jetbrains.annotations.Nullable;
import tec.uom.se.AbstractQuantity;

import java.util.List;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;

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
                "//",
                zeroOrMore(noneOf("\n")),
                firstOf("\n", eof() /* may not have newline at end of file */)
        );
    }

    public Rule TermCommandTask() {
        return sequence(
                Term(),
                s(),
                eof(),
                push(newTask(1f,';', the(pop()), null, new long[] { ETERNAL, ETERNAL }))
        );
    }

    public Rule Task() {

        Var<Float> budget = new Var();
        Var<Character> punc = new Var(Op.COMMAND);
        Var<Object> truth = new Var();
        Var<Object> occurr = new Var(new long[] { ETERNAL, ETERNAL });

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

                
                
                
                BudgetPriority(budget),
                

                optional(BUDGET_VALUE_MARK)
        );
    }

    boolean BudgetPriority(Var<Float> budget) {
        return budget.set((Float) (pop()));
    }

    
    
    
    
    
    

    
    
    
    
    
    









    public Rule Truth(Var<Object> truth) {
        return sequence(

                TRUTH_VALUE_MARK,

                ShortFloat(), 

                firstOf(

                    sequence(

                            
                            ";",

                            ShortFloat(), 

                            optional(TRUTH_VALUE_MARK), 

                            swap() && truth.set(new PreciseTruth((float) pop(), (float) pop()))
                    ),

                    seq(TRUTH_VALUE_MARK, truth.set(pop()))
                )
                        /*,

                        sequence(
                                TRUTH_VALUE_MARK, 

                                truth.set(new DefaultTruth((float) pop() ))
                        )*/
                
        );
    }

//    public Rule TruthTenseSeparator(char defaultChar, Var<Tense> tense) {
//        return firstOf(
//                defaultChar,
//                sequence('|', tense.set(Tense.Present)),
//                sequence('\\', tense.set(Tense.Past)),
//                sequence('/', tense.set(Tense.Future))
//        );
//    }


    public Rule ShortFloat() {
        return sequence(
                sequence(
                        optional(digit()),
                        optional('.', oneOrMore(digit()))
                ),
                push(Texts.f(matchOrDefault("NaN"), 0, 1.0f))
        );
    }


    
    
    
    
    
    

    
    
    
    
    
    
    
    
    
    
    


    public Rule SentencePunctuation(Var<Character> punc) {

        return sequence(trie(".", "?", "!", "@", ";"), punc.set(matchedChar()));
        
    }


    @Override
    public Rule Term() {
        return Term(true, true);
    }

    
    
    


    @Cached
    public Rule Term(boolean oper, boolean temporal) {
        /*
                 <term> ::= <word>                             
                        | <variable>                         
                        | <compound-term>                    
                        | <statement>                        
        */

        return seq( s(),

                firstOf(

                        QuotedAtom(),
                        URIAtom(),




                        
                        
                        
                        


                        Ellipsis(),


                        seq(SETe.str,

                                MultiArgTerm(SETe, SET_EXT_CLOSER, false, false)

                        ),

                        seq(SETi.str,

                                MultiArgTerm(SETi, SET_INT_CLOSER, false, false)

                        ),

                        
                        seq(oper, Function()),

                        seq(temporal, TemporalRelation()),

                        seq(COMPOUND_TERM_OPENER, s(),
                                firstOf(

                                        
                                        
                                        

                                        sequence(
                                                COMPOUND_TERM_CLOSER, push(EmptyProduct)
                                        ),

                                        CompoundPrefix(),

                                        CompoundInfix(),

                                        MultiArgTerm(null, COMPOUND_TERM_CLOSER, true, false),

                                        
                                        MultiArgTerm(null, COMPOUND_TERM_CLOSER, false, false)


                                )

                        ),

                        seq(oper, temporal, ColonReverseInheritance()),

                        NumberAtom(),

                        AtomStr(),

                        Variable(),

                        
                        seq(NEG.str, Term(), push(($.the(pop())).neg())),

                        
                        seq(OLD_STATEMENT_OPENER,
                                MultiArgTerm(null, OLD_STATEMENT_CLOSER, false, true)
                        )


                ),

                s()
        );
    }

    public Rule URIAtom() {
        return seq(
            //https://stackoverflow.com/questions/163360/regular-expression-to-match-urls-in-java#163398
            regex("^[a-z]+://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"),
            push($.quote(match()))
        );
    }

    public Rule Function() {
        return seq(

                AtomStr(),
                
                

                COMPOUND_TERM_OPENER, s(),

                firstOf(
                        seq(COMPOUND_TERM_CLOSER, push(EmptyProduct)),
                        MultiArgTerm(PROD, COMPOUND_TERM_CLOSER, false, false)
                ),

                push(INH.the((Term) pop(), $.the(pop())))

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
                return o.the((int)timeDelta, subj, pred);
            } else {
                
                QuantityTerm q = (QuantityTerm)timeDelta;
                return $.func("term", o.strAtom, $.p(subj, pred), q);
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
            case "hours": return "h"; 
            case "hr": return "h"; 
            case "m": return "min"; 
            case "mins": return "min"; 
            case "sec": return "s"; 
            default:
                return u;
        }
    }

    public Rule OccurrenceTime() {
        return
                firstOf(
                        seq(firstOf("now", "|", ":|:"), push(Tense.Present)), 
                        
                        TimeUnit(),
                        seq("-", oneOrMore(digit()), push(-Texts.i(match()))),
                        seq("+", oneOrMore(digit()), push(Texts.i(match())))


                )
                ;
    }

    
    
    
    
    
    


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
        return seq(
                Term(false, false),
                ':',
                Term(),
                push(INH.the(the(pop()), the(pop())))
        );
    }


    public Rule QuotedAtom() {
        return sequence(
                dquote(), 
                firstOf(
                        
                        seq(regex("\"\"[\\s\\S]+\"\"\""), push(Atomic.the('\"' + match()))),

                        
                        seq(
                                
                                regex("(?:[^\"\\\\]|\\\\.)*\""),
                                push(Atomic.the('\"' + match())))
                )
        );
    }


    public Rule Ellipsis() {
        return sequence(
                Variable(), "..",
                firstOf(

                        seq("+",
                                push(new Ellipsis.EllipsisPrototype(Op.VAR_PATTERN, (Variable) pop(), 1))
                        ),
                        seq("*",
                                push(new Ellipsis.EllipsisPrototype(Op.VAR_PATTERN, (Variable) pop(), 0))
                        )
                )
        );
    }










    Rule Variable() {
        return firstOf(
                seq('_', push(Op.VarAuto)),
                seq('\\', push(Op.ImgInt)),
                seq('/', push(Op.ImgExt)),
                seq(Op.VAR_INDEP.ch, Variable(VAR_INDEP)),
                seq(Op.VAR_DEP.ch, Variable(VAR_DEP)),
                seq(Op.VAR_QUERY.ch, Variable(VAR_QUERY)),
                seq(Op.VAR_PATTERN.ch, Variable(VAR_PATTERN))
            );
    }

    Rule Variable(Op varType) {
        return
            seq(
                //unnormalized
                AtomStr(),
                push($.v(varType, (String) pop()))
            );
    }

    
        /*
         <compound-term> ::= "{" <term> {","<term>} "}"         
                        | "[" <term> {","<term>} "]"         
                        | "(&," <term> {","<term>} ")"       
                        | "(|," <term> {","<term>} ")"       
                        | "(*," <term> {","<term>} ")"       
                        | "(/," <term> {","<term>} ")"       
                        | "(\," <term> {","<term>} ")"       
                        | "(||," <term> {","<term>} ")"      
                        | "(&&," <term> {","<term>} ")"      
                        | "(&/," <term> {","<term>} ")"      
                        | "(&|," <term> {","<term>} ")"      
                        | "(--," <term> ")"                  
                        | "(-," <term> "," <term> ")"        
                        | "(~," <term> "," <term> ")"        

        */

    

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

    
    
    
    
    
    
    
    
    
    
    
    
    

    Rule AnyOperatorOrTerm() {
        return firstOf(Op(), Term());
    }


    @Nullable
    static Term the(@Nullable Object o) {
        if (o instanceof Term) return (Term) o;
        if (o == null) return null; 
        if (o instanceof String) {
            String s = (String) o;
            
            return Atomic.the(s);

            
            
            
            
            
            
            
            
            
            
            
            
            

            
        }
        throw new RuntimeException(o + " is not a term");
    }

    /**
     * produce a term from the terms (& <=1 NALOperator's) on the value stack
     */
    @Nullable
    @Deprecated
    final Term popTerm(Op op /*default */) {

        

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


        
        



        while (!stack.isEmpty()) {
            Object p = pop();

            if (p instanceof Object[]) {
                
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

            if (p == Compound.class) break; 


            if (p instanceof String) {
                
                
                tt.add(Atomic.the((String) p));
            } else if (p instanceof Term) {
                if (p == Null) {
                    stack.clear();
                    return new FasterList(1).addingAll(Null);
                }
                tt.add((Term)p);
            } else if (p instanceof Op) {

                
                
                
                
                
                

                if (op != null)
                    op[0] = (Op) p;
            }
        }

        tt.reverse();


        return tt;
    }


    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    


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

    
    
    

    
    
    
    
    
    
    
    


    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    


    
    
    
    
    
    


    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    


    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
}
