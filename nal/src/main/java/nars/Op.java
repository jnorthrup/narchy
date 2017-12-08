package nars;


import jcog.TODO;
import jcog.list.FasterList;
import nars.derive.match.EllipsisMatch;
import nars.derive.match.Ellipsislike;
import nars.op.mental.AliasConcept;
import nars.term.*;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.compound.CachedCompound;
import nars.term.compound.UnitCompound1;
import nars.term.container.Subterms;
import nars.term.var.UnnormalizedVariable;
import nars.time.Tense;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.primitive.ObjectByteMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.api.tuple.primitive.ObjectBytePair;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Arrays.copyOfRange;
import static nars.term.Terms.flatten;
import static nars.term.Terms.sorted;
import static nars.time.Tense.*;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * NAL symbol table
 */
public enum Op {


    ATOM(".", Op.ANY_LEVEL, OpType.Other),

    NEG("--", 1, Args.One) {
        @Override
        public Term _the(int dt, Term[] u) {
            assert (u.length == 1); //assert (dt == DTERNAL || dt == XTERNAL);

            Term x = u[0];
            switch (x.op()) {
                case BOOL:
                    return x.neg();
                case NEG:
                    return x.unneg();
                default:
                    return The.compound(NEG, x);
            }
        }
    },

    INH("-->", 1, OpType.Statement, Args.Two),
    SIM("<->", true, 2, OpType.Statement, Args.Two),

    /**
     * extensional intersection
     */
    SECTe("&", true, 3, Args.GTETwo) {
        @Override
        public Term _the(int dt, Term[] u) {
            return intersect(Int.intersect(u),
                    SECTe,
                    SETe,
                    SETi);
        }
    },

    /**
     * intensional intersection
     */
    SECTi("|", true, 3, Args.GTETwo) {
        @Override
        public Term _the(int dt, Term[] u) {
            return intersect(Int.intersect(u),
                    SECTi,
                    SETi,
                    SETe);
        }
    },

    /**
     * extensional difference
     */
    DIFFe("~", false, 3, Args.Two) {
        @Override
        public Term _the(int dt, Term[] u) {
            return differ(this, u);
        }
    },

    /**
     * intensional difference
     */
    DIFFi("-", false, 3, Args.Two) {
        @Override
        public Term _the(int dt, Term[] u) {
            return differ(this, u);
        }
    },

    /**
     * PRODUCT
     * classically this is considered NAL4 but due to the use of functors
     * it is much more convenient to classify it in NAL1 so that it
     * along with inheritance (INH), which comprise the functor,
     * can be used to compose the foundation of the system.
     */
    PROD("*", 1, Args.GTEZero) {
        @Override
        public Term the(int dt, Term... t) {
            assert (dt == DTERNAL);
            return (t.length == 0) ? ZeroProduct : compound(PROD, DTERNAL, t);
        }
    },


    /**
     * conjunction
     */
    CONJ("&&", true, 5, Args.GTETwo) {
        @Override
        public Term _the(int dt, Term[] u) {

            final int n = u.length;
            switch (n) {

                case 0:
                    return True;

                case 1:
                    Term only = u[0];
                    if (only instanceof EllipsisMatch) {
                        return _the(dt, ((EllipsisMatch) only).arrayShared()); //unwrap
                    }

                    //preserve unitary ellipsis for patterns etc
                    return only instanceof Ellipsislike ?
                            new UnitCompound1(CONJ, only) //special; preserve the surrounding conjunction
                            :
                            only;

            }


            int absoluteness = 0, trues = 0;
            for (Term t : u) {
                if (t instanceof Bool) {
                    if (t == Null) return Null;
                    if (t == False) {
                        absoluteness = -1;
                    } else if (t == True) {
                        trues++;
                        if (absoluteness >= 0)
                            absoluteness = +1; //only if not false, so false overrides
                    }
                }
            }
            if (absoluteness == -1) return False;
            if (absoluteness == +1) {
                if (concurrent(dt)) {

                    //TODO special case where only one item is left, can avoid reconstructing

                    //filter out all boolean terms

                    int size = u.length - trues;
                    if (size == 0)
                        return True;

                    Term[] y = new Term[size];
                    int j = 0;
                    for (int i = 0; j < y.length; i++) {
                        Term uu = u[i];
                        if (uu != True) // && (!uu.equals(False)))
                            y[j++] = uu;
                    }

                    assert (j == y.length);

                    return CONJ.the(dt, y);
                } else {
                    //nothing we can really do. maybe insert a depvar
                    return Null;
                }
            }

            if (u.length == 2 && (dt == 0 || dt == DTERNAL)) {
                if (conegated(u[0], u[1]))
                    return False;
            }


            Term ci;
            switch (dt) {
                case 0:
                    ci = null;
                    for (int i = 0; i < u.length; i++) {

//                    //PROMOTE DTERNAL to ZERO
//                    if (u[i].op() == CONJ && u[i].dt() == DTERNAL) {
//                        u[i] = u[i].dt(0);
//                    }

                        //HACK cut to prevent infinite recursion due to impl conj reduction
//                    if (u[0].op() == IMPL && u[0].containsRecursively(u[1].unneg())) {
//                        Term ui = u[0];
//                        int id = ui.dt();
//
//                        {
//                            Term uis = ui.sub(0);
//                            if (uis.equals(u[1]))
//                                continue; //already absorbed into the subject
//                            if (uis.op() == CONJ) {
//                                LongObjectHashMap<Term> uism = uis.eventMap(0);
//                                Term uismNOW = uism.get(0);
//                                if (uismNOW.equals(u[1]))
//                                    return True;
//                                if (uismNOW.unneg().equals(u[1]))
//                                    return Null; //co-negation
//                            }
//                        }
//
//                        if (id == DTERNAL || id == 0) { //simultaneous with now
//                            Term uip = ui.sub(1);
//                            if (uip.equals(u[1]))
//                                return True;
//                            if (uip.op() == CONJ) {
//                                LongObjectHashMap<Term> uipm = uip.eventMap(0);
//                                Term uipmNOW = uipm.get(0);
//                                if (uipmNOW.equals(u[1]))
//                                    return True;
//                                if (uipmNOW.unneg().equals(u[1]))
//                                    return Null; //co-negation
//                            }
//                        }
//                    }

                        ci = i > 0 ? conjMerge(ci, 0, u[i], 0) : u[0];
                        if (ci instanceof Bool)
                            return ci;
                    }

                    break;
                case DTERNAL:

                    return junctionFlat(dt, u);

                default:

                    //sequence or xternal
                    //assert (n == 2) : "invalid non-commutive conjunction arity!=2, arity=" + n;

                    //rebalance and align
                    //convention: left align all sequences
                    //ex: (x &&+ (y &&+ z))
                    //      becomes
                    //    ((x &&+ y) &&+ z)

                    if (dt == XTERNAL) {
                        Arrays.sort(u); //pre-sort but should have been sorted already
                    }

                    Term a = u[0];
                    Term b = u[1];
//                int eventsLeft = a.eventCount();
//                int eventsRight = b.eventCount();
//                assert(eventsLeft > 0);
//                assert(eventsRight > 0);
//                boolean heavyLeft = (eventsLeft - eventsRight) > 1;
//                boolean heavyRight = (eventsRight - eventsLeft) > 0; // notice the difference in 0, 1. if the # of events is odd, left gets it


                    if (dt == XTERNAL) {
                        if (n == 2 && a.op() == CONJ && a.dt() == XTERNAL && a.subs() == 2) {

                            int va = a.volume();
                            int vb = b.volume();

                            if (va > vb) {
                                Term[] aa = a.subterms().arrayShared();
                                int va0 = aa[0].volume();
                                int va1 = aa[1].volume();
                                int vamin = Math.min(va0, va1);

                                //if left remains heavier by donating its smallest
                                if ((va - vamin) > (vb + vamin)) {
                                    int min = va0 <= va1 ? 0 : 1;
                                    return CONJ.the(XTERNAL,
                                            CONJ.the(XTERNAL, b, aa[min] /* a to b */),
                                            aa[1 - min]);
                                }
                            }

                        }
                        return compound(CONJ, XTERNAL, u);
                    } else {
                        ci = dt >= 0 ?
                                conjMerge(a, 0, b, +dt + a.dtRange()) :
                                conjMerge(b, 0, a, -dt + b.dtRange());
                    }

            }
            return implInConjReduction(ci);

        }


        /**
         * flattening conjunction builder, for (commutive) multi-arg conjunction and disjunction (dt == 0 ar DTERNAL)
         * see: https://en.wikipedia.org/wiki/Boolean_algebra#Monotone_laws
         */
        /*@NotNull*/
        private Term junctionFlat(int dt, final Term[] u) {

            //TODO if there are no negations in u then an accelerated construction is possible

            assert (u.length > 1 && (dt == 0 || dt == DTERNAL)); //throw new RuntimeException("should only have been called with dt==0 or dt==DTERNAL");


//            //simple accelerated case:
//            if (u.length == 2 && !u[0].hasAny(CONJ) && !u[1].hasAny(CONJ)) { //if it's simple
//
//                //already checked in callee
//                //if (u[0].unneg().equals(u[1].unneg()))
//                //    return False; //co-neg
//
//                return Op.implInConjReduction(compound(CONJ, dt, u));
//            }


            ObjectByteHashMap<Term> s = new ObjectByteHashMap<>(u.length);

            if (!flatten(CONJ, u, dt, s))
                return False;

            if (s.isEmpty())
                return True; //? does this happen

            SortedSet<Term> cs = junctionGroupNonDTSubterms(s);
            if (!cs.isEmpty()) {


                //annihilate common terms inside and outside of disjunction
                //      ex:
                //          -X &&  ( X ||  Y)
                //          -X && -(-X && -Y)  |-   -X && Y
                Iterator<Term> csi = cs.iterator();
                List<Term> csa = null;
                while (csi.hasNext()) {
                    Term x = csi.next();

                    if (x.op() == NEG) {
                        Term x0 = x.sub(0);
                        if (x0.op() == CONJ && commute(x0.dt(), x0.subs())) { //DISJUNCTION
                            Term disj = x.unneg();
                            SortedSet<Term> disjSubs = disj.subterms().toSortedSet();
                            //factor out occurrences of the disj's contents outside the disjunction, so remove from inside it
                            if (disjSubs.removeAll(cs)) {
                                //reconstruct disj if changed
                                csi.remove();

                                if (!disjSubs.isEmpty()) {
                                    if (csa == null)
                                        csa = $.newArrayList(1);
                                    csa.add(CONJ.the(disj.dt(), sorted(disjSubs)).neg());
                                }
                            }
                        }
                    }
                }
                if (csa != null)
                    cs.addAll(csa);

                if (cs.size() == 1)
                    return cs.first();

                Term[] scs = sorted(cs);

                boolean dtChange = false;

                //promote DTERNAL to ZERO if any components are ZERO
//                if (dt == DTERNAL) {
//                    for (Term x : scs) {
//                        if (x.op() == CONJ && x.dt() == 0) {
//                            dt = 0;
//                            dtChange = true;
//                            break;
//                        }
//                    }
//                }


                return (dtChange || !Arrays.equals(scs, u)) ?
                        CONJ.the(dt, scs) : //changed, recurse
                        Op.implInConjReduction(compound(CONJ, dt, scs));
            }

            return Null;
        }


//        /**
//         * array implementation of the conjunction true/false filter
//         */
//        @NotNull
//        private Term[] conjTrueFalseFilter(@NotNull Term... u) {
//            int trues = 0; //# of True subterms that can be eliminated
//            for (Term x : u) {
//                if (x == True) {
//                    trues++;
//                } else if (x == False) {
//
//                    //false subterm in conjunction makes the entire condition false
//                    //this will eventually reduce diectly to false in this method's only callee HACK
//                    return FalseArray;
//                }
//            }
//
//            if (trues == 0)
//                return u;
//
//            int ul = u.length;
//            if (ul == trues)
//                return TrueArray; //reduces to an Imdex itself
//
//            Term[] y = new Term[ul - trues];
//            int j = 0;
//            for (int i = 0; j < y.length; i++) {
//                Term uu = u[i];
//                if (!(uu == True)) // && (!uu.equals(False)))
//                    y[j++] = uu;
//            }
//
//            assert (j == y.length);
//
//            return y;
//        }

        /**
         * this is necessary to keep term structure consistent for intermpolation.
         * by grouping all non-sequence subterms into its own subterm, future
         * flattening and intermpolation is prevented from destroying temporal
         * measurements.
         *
         */
        private SortedSet<Term> junctionGroupNonDTSubterms(ObjectByteMap<Term> s) {

            TreeSet<Term> outer = new TreeSet<>();

            for (ObjectBytePair<Term> xn : s.keyValuesView()) {
                Term x = xn.getOne();
                outer.add(x.negIf(xn.getTwo() < 0));
            }
            return outer;


        }

    },


    /**
     * intensional set
     */
    SETi("[", true, 2, Args.GTEOne) {
        @Override
        public boolean isSet() {
            return true;
        }
    },

    /**
     * extensional set
     */
    SETe("{", true, 2, Args.GTEOne) {
        @Override
        public boolean isSet() {
            return true;
        }
    },


    /**
     * implication
     */
    IMPL("==>", 5, OpType.Statement, Args.Two),


    ///-----------------------------------------------------


    VAR_DEP('#', Op.ANY_LEVEL, OpType.Variable),
    VAR_INDEP('$', 5 /*NAL5..6 for Indep Vars */, OpType.Variable),
    VAR_QUERY('?', Op.ANY_LEVEL, OpType.Variable),
    VAR_PATTERN('%', Op.ANY_LEVEL, OpType.Variable),

    INT("+", Op.ANY_LEVEL, OpType.Other) {

    },

    BOOL("B", Op.ANY_LEVEL, OpType.Other) {

    },
    //SPACE("+", true, 7, Args.GTEOne),


//    //VIRTUAL TERMS
//    @Deprecated
//    INSTANCE("-{-", 2, OpType.Statement, Args.Two) {
//        @Override
//        @NotNull protected Term _the(int dt, Term[] u) {
//            assert (u.length == 2);
//            return INH.the(SETe.the(u[0]), u[1]);
//        }
//    },
//
//    @Deprecated
//    PROPERTY("-]-", 2, OpType.Statement, Args.Two) {
//        @Override
//        @NotNull protected Term _the(int dt, Term[] u) {
//            assert (u.length == 2);
//            return INH.the(u[0], SETi.the(u[1]));
//        }
//    },
//
//    @Deprecated
//    INSTANCE_PROPERTY("{-]", 2, OpType.Statement, Args.Two) {
//        @Override
//        @NotNull protected Term _the(int dt, Term[] u) {
//            assert (u.length == 2);
//            return INH.the(SETe.the(u[0]), SETi.the(u[1]));
//        }
//    },
//
//    @Deprecated
//    DISJ("||", true, 5, Args.GTETwo) {
//        @Override
//        @NotNull protected Term _the(int dt, Term[] u) {
//            assert (dt == DTERNAL);
//            if (u.length == 1 && u[0].op() != VAR_PATTERN)
//                return u[0];
//            return NEG.the(CONJ.the(Terms.neg(u)));
//        }
//    },

//    /**
//     * extensional image
//     */
//    IMGe("/", 4, Args.GTEOne),
//
//    /**
//     * intensional image
//     */
//    IMGi("\\", 4, Args.GTEOne),

    /**
     * for ellipsis, when seen as a term
     */
    //SUBTERMS("...", 1, OpType.Other)
    ;


    public static final String DISJstr = "||";

    public final boolean indepVarParent;
    public final boolean depVarParent;

    private static boolean conegated(Term a, Term b) {
        Term ua = a.unneg();
        if (ua != a && ua.equals(b)) return true; //co-negation
        Term ub = b.unneg();
        if (ub != b && ub.equals(a)) return true; //co-negation

        return false;
    }

    public static final Compound ZeroProduct = new CachedCompound(Op.PROD, Subterms.Empty);

    public static final int StatementBits = Op.or(Op.INH, Op.SIM, Op.IMPL);

    public static final int funcBits = Op.or(Op.ATOM, Op.INH, Op.PROD);
    public static final int funcInnerBits = Op.or(Op.ATOM, Op.PROD);

    public static final byte BELIEF = '.';
    public static final byte QUESTION = '?';
    public static final byte GOAL = '!';
    public static final byte QUEST = '@';
    public static final byte COMMAND = ';';

    public static final String TENSE_PAST = ":\\:";
    public static final String TENSE_PRESENT = ":|:";
    public static final String TENSE_FUTURE = ":/:";

    public static final String TENSE_ETERNAL = ":-:"; //ascii infinity symbol
    public static final String TASK_RULE_FWD = "|-";

    public static final char BUDGET_VALUE_MARK = '$';
    public static final char TRUTH_VALUE_MARK = '%';
    public static final char VALUE_SEPARATOR = ';';

    public static final char ARGUMENT_SEPARATOR = ',';

    public static final char SET_INT_CLOSER = ']';
    public static final char SET_EXT_CLOSER = '}';
    public static final char COMPOUND_TERM_OPENER = '(';
    public static final char COMPOUND_TERM_CLOSER = ')';

    @Deprecated
    public static final char OLD_STATEMENT_OPENER = '<';
    @Deprecated
    public static final char OLD_STATEMENT_CLOSER = '>';

    public static final char STAMP_OPENER = '{';
    public static final char STAMP_CLOSER = '}';
    public static final char STAMP_SEPARATOR = ';';
    public static final char STAMP_STARTER = ':';


    /**
     * bitvector of non-variable terms which can not be part of a goal term
     */
    public static final int NonGoalable = or(IMPL);
    public static final int varBits = Op.or(VAR_PATTERN, VAR_DEP, VAR_QUERY, VAR_INDEP);


    public final boolean allowsBool;

    /**
     * Image index ("imdex") symbol for products, and anonymous variable in products
     */
    public final static char ImdexSym = '_';
    public static final Atomic Imdex =
            new UnnormalizedVariable(Op.VAR_DEP, String.valueOf(ImdexSym)) {

                final int RANK = Term.opX(VAR_PATTERN, 20 /* different from normalized variables with a subOp of 0 */);

                @Override
                public int opX() {
                    return RANK;
                }
            };


    public static final char TrueSym = '†';
    public static final char FalseSym = 'Ⅎ';
    public static final char NullSym = '☢';

    /**
     * absolutely nonsense
     */
    public static final Bool Null = new BoolNull();

    /**
     * absolutely false
     */
    public static final Bool False = new BoolFalse();

    /**
     * absolutely true
     */
    public static final Bool True = new BoolTrue();

    /**
     * specifier for any NAL level
     */
    private static final int ANY_LEVEL = 0;
    public static final int SetBits = or(Op.SETe, Op.SETi);
    public static final int Temporal = or(Op.CONJ, Op.IMPL);
    public static final int VariableBits = or(Op.VAR_PATTERN, Op.VAR_INDEP, Op.VAR_DEP, Op.VAR_QUERY);
    public static final int[] NALLevelEqualAndAbove = new int[8 + 1]; //indexed from 0..7, meaning index 7 is NAL8, index 0 is NAL1


    /**
     * whether it is a special or atomic term that isnt conceptualizable.
     * negation is an exception to this, being unconceptualizable itself
     * but it will have conceptualizable=true.
     */
    public final boolean conceptualizable;

    public final boolean beliefable, goalable;

    public static Term dt(Compound base, int nextDT) {

        if (nextDT == XTERNAL) {
            return new GenericCompoundDT(base, XTERNAL);
        } else {
            Subterms subs = base.subterms();
            int ns = subs.subs();
//                if (nextDT == DTERNAL && ns == 2 && !subs.sub(0).unneg().equals(subs.sub(1).unneg()))
//                    return base; //re-use base only if the terms are inequal

            /*@NotNull*/
            if (!concurrent(nextDT) && ns > 2)
                return Null; //tried to temporalize what can only be commutive


            Term[] ss = subs.arrayShared();

            Op o = base.op();
            assert (o.temporal);
            if (o.commutative) {

                if (ss.length == 2) {
                    //must re-arrange the order to lexicographic, and invert dt
                    return o.the(nextDT != DTERNAL ? -nextDT : DTERNAL, ss[1], ss[0]);
                } else {
                    return o.the(nextDT, ss);
                }
            } else {
                return o.the(nextDT, ss);
            }
        }
    }

//    public interface TermInstancer {
//
//
//        default @NotNull Term compound(@NotNull NewCompound apc, int dt) {
//            return compound(apc.op(), apc.theArray()).dt(dt);
//        }
//
//        @NotNull Compound compound(Op o, Term[] subterms);
//
//        @NotNull TermContainer subterms(@NotNull Term... x);
//
//    }
//
//    /**
//     * memoization
//     */
//    public static class MemoizedTermInstancer implements TermInstancer {
//
//        final Function<ProtoCompound, Termlike> buildTerm = (C) -> {
//            try {
//
//                Op o = C.op();
//                if (o != null) {
//                    return compound(C);
//                } else
//                    return subterms(C.subterms());
//
//            } catch (InvalidTermException e) {
//                if (Param.DEBUG_EXTRA)
//                    logger.error("Term Build: {}, {}", C, e);
//                return Null;
//            } catch (Throwable t) {
//                logger.error("{}", t);
//                return Null;
//            }
//        };
//
//
//    public static boolean internable(@NotNull Term[] x) {
//        boolean internable = true;
//        for (Term y : x) {
//            if (y instanceof NonInternable) { //"must not intern non-internable" + y + "(" +y.getClass() + ")";
//                internable = false;
//                break;
//            }
//        }
//        return internable;
//    }
//        public static final Memoize<ProtoCompound, Termlike> cache =
//                new HijackMemoize<>(buildTerm, 128 * 1024 + 1, 3);
//        //CaffeineMemoize.builder(buildTerm, 128 * 1024, true /* Param.DEBUG*/);
//
//
//        @NotNull
//        @Override
//        public Term compound(NewCompound apc, int dt) {
//
//            if (apc.OR(x -> x instanceof NonInternable)) {
//                return compound(apc.op, apc, false).dt(dt);
//            } else {
//                Term x = (Term) cache.apply(apc.commit());
//
//                if (dt != DTERNAL && x instanceof Compound)
//                    return x.dt(dt);
//                else
//                    return x;
//            }
//        }
//
//        @Override
//        public @NotNull TermContainer subterms(@NotNull Term... x) {
////        if (s.length < 2) {
////            return _subterms(s);
////        } else {
//
//            boolean internable = internable(x);
//
//            if (internable) {
//                return (TermContainer) cache.apply(new NewCompound(null, x).commit());
//            } else {
//                return TermVector.the(x);
//            }
//
//        }
//    }


    public final Term the(/*@NotNull*/ Term... u) {
        return the(DTERNAL, u);
    }

    static final ImmutableMap<String, Op> stringToOperator;

    static {
        for (Op o : Op.values()) {
            int l = o.minLevel;
            if (l < 0) l = 0; //count special ops as level 0, so they can be detected there
            for (int i = l; i <= 8; i++) {
                NALLevelEqualAndAbove[i] |= o.bit;
            }
        }

        final Map<String, Op> _stringToOperator = new HashMap<>(values().length * 2);

        //Setup NativeOperator String index hashtable
        for (Op r : Op.values()) {
            _stringToOperator.put(r.toString(), r);

        }
        stringToOperator = Maps.immutable.ofMap(_stringToOperator);

        //System.out.println(Arrays.toString(byteSymbols));


//        //Setup NativeOperator Character index hashtable
//        for (Op r : Op.values()) {
//            char c = r.ch;
//            if (c!=0)
//                Op._charToOperator.put(c, r);
//        }
    }

    /**
     * string representation
     */
    public final String str;
    /**
     * character representation if symbol has length 1; else ch = 0
     */
    public final char ch;
    public final OpType type;

    /**
     * arity limits, range is inclusive >= <=
     * TODO replace with an IntPredicate
     */
    public final int minSize, maxSize;
    /**
     * minimum NAL level required to use this operate, or 0 for N/A
     */
    public final int minLevel;
    public final boolean commutative;
    public final boolean temporal;
    public final int bit;
    public final boolean var;
    public final boolean atomic;
    public final boolean statement;
    /**
     * whether this involves an additional numeric component: 'dt' (for temporals) or 'relation' (for images)
     */
    public final boolean hasNumeric;
    public final byte id;


    Op(char c, int minLevel, OpType type) {
        this(c, minLevel, type, Args.None);
    }

    /*
    used only by Termlike.hasAny
    public static boolean hasAny(int existing, int possiblyIncluded) {
        return (existing & possiblyIncluded) != 0;
    }*/


    Op(@NotNull String s, boolean commutative, int minLevel, @NotNull IntIntPair size) {
        this(s, commutative, minLevel, OpType.Other, size);
    }


    Op(char c, int minLevel, OpType type, @NotNull IntIntPair size) {
        this(Character.toString(c), minLevel, type, size);
    }

    Op(@NotNull String string, int minLevel, @NotNull IntIntPair size) {
        this(string, minLevel, OpType.Other, size);
    }

    Op(@NotNull String string, int minLevel, OpType type) {
        this(string, false /* non-commutive */, minLevel, type, Args.None);
    }


    Op(@NotNull String string, int minLevel, OpType type, @NotNull IntIntPair size) {
        this(string, false /* non-commutive */, minLevel, type, size);
    }


    Op(@NotNull String string, boolean commutative, int minLevel, OpType type, @NotNull IntIntPair size) {

        this.id = (byte) (ordinal());
        this.str = string;

        this.commutative = commutative;
        this.minLevel = minLevel;
        this.type = type;

        this.ch = string.length() == 1 ? string.charAt(0) : 0;

        this.minSize = size.getOne();
        this.maxSize = size.getTwo();

        this.var = (type == OpType.Variable);

        boolean isImpl = str.equals("==>");
        this.statement = str.equals("-->") || isImpl || str.equals("<->");
        boolean isConj = str.equals("&&");
        this.temporal = isConj || isImpl;
        //in(or(CONJUNCTION, IMPLICATION, EQUIV));

        this.hasNumeric = temporal;

        //negation does not contribute to structure vector
        this.bit = (1 << ordinal());

        final Set<String> ATOMICS = Set.of(".", "+", "B");
        this.atomic = var || ATOMICS.contains(str);


        switch (str) {
            case "==>":
            case "&&":
            case "*": //HACK necessary for ellipsematch content
            case "|":
            case "&":
                allowsBool = true;
                break;
            default:
                allowsBool = false;
                break;
        }

        conceptualizable = !(var ||
                str.equals("+") /* INT */ || str.equals("B") /* Bool */);

        goalable = conceptualizable && !isImpl;

        beliefable = conceptualizable;

        indepVarParent = isImpl;
        depVarParent = isConj;

    }

    public static boolean hasAny(int existing, int possiblyIncluded) {
        return (existing & possiblyIncluded) != 0;
    }

    public static boolean hasAll(int existing, int possiblyIncluded) {
        return ((existing | possiblyIncluded) == existing);
    }

    public static boolean isTrueOrFalse(Term x) {
        return x == True || x == False;
    }

    //CaffeineMemoize.builder(buildTerm, -1 /* softref */, true /* Param.DEBUG*/);
    //new NullMemoize<>(buildTerm);

    public static boolean concurrent(int dt) {
        return (dt == DTERNAL) || (dt == 0);
    }


    /*@NotNull*/
    static public Term conjMerge(Term a, long aStart, Term b, long bStart) {

        if (a.eventCount() == 1 && b.eventCount() == 1) {
            int dt = (int) (bStart - aStart);

            return implInConjReduction(conjSeqFinal(dt, a, b));
        }

        LongObjectHashMap<Collection<Term>> eventSets = new LongObjectHashMap();

        LongObjectPredicate<Term> insert = (long w, Term xb) -> {
            Collection<Term> ab = eventSets.updateValue(w,
                    () -> {
                        Collection<Term> x =
                                new UnifiedSet<>(1);
                        //new TreeSet();
                        x.add(xb);
                        return x;
                    },
                    (Collection<Term> xx) -> {
                        if (xx.add(xb)) {
                            Op o = xb.op();
                            if (o == NEG) {
                                if (xx.contains(xb.unneg()))
                                    return null;
                            } else {
                                //if (xa.contains(xb.neg())) return null;
                                for (Term z : xx) {
                                    if (z.op() == NEG && z.sub(0).equals(xb))
                                        return null;
                                }
                            }
                        }
                        return xx;
                    });
            return ab != null;
        };

        if (!(a.eventsWhile(insert, aStart) && b.eventsWhile(insert, bStart)))
            return False;

        LongObjectHashMap<Term> eventSet = new LongObjectHashMap<>();
        Term[] cut = {null};
        if (!eventSets.keyValuesView().allSatisfy((ws) -> {
            Term[] sps = sorted(ws.getTwo());
            assert (sps.length > 0);
            Term pp;
            if (sps.length == 1)
                pp = sps[0];
            else {
                pp = /*implInConjReduction*/(compound(CONJ, 0, sps)); //direct
                //pp = CONJ.the(0, sps);
            }
            if (pp instanceof Bool) {
                cut[0] = pp;
                return false;
            }
            eventSet.put(ws.getOne(), pp);
            return true;
        })) {
            return cut[0];
        }

//        int ee = eventSet.size();
//        assert (ee > 0);
//        if (ee == 1) {
//            return eventSet.values().iterator().next().get;
//        }

        List<LongObjectPair<Term>> events = new FasterList<>(eventSet.size());
        eventSet.forEachKeyValue((w, t) -> events.add(PrimitiveTuples.pair(w, t)));
        return conj(events);

//        //group all parallel clusters
//        LongObjectPair<Term> e0 = events.get(0);
//
//        long headAt = e0.getOne();
//        int groupStart = -1;
//        for (int i = 1; i <= ee; i++) {
//            long nextAt = (i != ee) ? events.get(i).getOne() : ETERNAL;
//            if (nextAt == headAt) {
//                if (groupStart == -1) groupStart = i - 1;
//            } else {
//                if (groupStart != -1) {
//                    int groupEnd = i;
//                    Term[] p = new Term[groupEnd - groupStart];
//                    assert (p.length > 1);
//                    long when = events.get(groupStart).getOne();
//                    for (int k = 0, j = groupStart; j < groupEnd; j++) {
//                        p[k++] = events.get(groupStart).getTwo();
//                        events.remove(groupStart);
//                        i--;
//                        ee--;
//                    }
//                    Term replacement = p.length > 1 ? CONJ.the(0, p) : p[0];
//                    if (events.isEmpty()) {
//                        //got them all here
//                        return replacement;
//                    }
//                    events.add(i, PrimitiveTuples.pair(when, replacement));
//                    i++;
//                    ee++;
//                    groupStart = -1; //reset
//                }
//            }
//            headAt = nextAt;
//        }

    }


    /**
     * constructs a correctly merged conjunction from a list of events
     */
    /*@NotNull*/
    public static Term conj(List<LongObjectPair<Term>> events) {

        int ee = events.size();
        if (ee > 1) {
            events.sort(LongObjectPair::compareTo);
            ListIterator<LongObjectPair<Term>> ii = events.listIterator();
            long prevtime = ETERNAL;
            while (ii.hasNext()) {
                LongObjectPair<Term> x = ii.next();
                long now = x.getOne();
                if (prevtime != ETERNAL && prevtime == now) {
                    ii.remove();
                    ee--;
                    LongObjectPair<Term> y = ii.previous();
                    Term xyt = CONJ.the(0, x.getTwo(), y.getTwo());
                    if (xyt == Null) return Null;
                    if (xyt == False) return False;
                    LongObjectPair<Term> xy = pair(now, xyt);
                    ii.set(xy);
                    ii.next();
                }
                prevtime = now;
            }
        }
        switch (ee) {
            case 0:
                return True;
            case 1:
                return events.get(0).getTwo();
            default:
                return implInConjReduction(conjSeq(events, 0, events.size()));
        }
    }

    /**
     * constructs a correctly merged conjunction from a list of events, in the sublist specified by from..to (inclusive)
     * all of the events should have distinct times before calling here.
     */
    private static Term conjSeq(List<LongObjectPair<Term>> events, int start, int end) {

        LongObjectPair<Term> first = events.get(start);
        int ee = end - start;
        switch (ee) {
            case 0:
                throw new NullPointerException("should not be called with empty events list");
            case 1:
                return first.getTwo();
            case 2:
                LongObjectPair<Term> second = events.get(end - 1);
                return conjSeqFinal(
                        (int) (second.getOne() - first.getOne()),
                        /* left */ first.getTwo(), /* right */ second.getTwo());
        }

        int center = start + (end - 1 - start) / 2;


        Term left = conjSeq(events, start, center + 1);
        if (left == Null) return Null;
        if (left == False) return False; //early fail shortcut

        Term right = conjSeq(events, center + 1, end);
        if (right == Null) return Null;
        if (right == False) return False; //early fail shortcut

        int dt = (int) (events.get(center + 1).getOne() - first.getOne() - left.dtRange());

        return conjSeqFinal(dt, left, right);
    }


    private static Term conjSeqFinal(int dt, Term left, Term right) {
        assert (dt != XTERNAL);
        if (left == False) return False;
        if (left == Null) return Null;

        if (right == False) return False;
        if (right == Null) return Null;

        if (left == True) return right;
        if (right == True) return left;

        if (dt == 0 || dt == DTERNAL) {
            if (left.equals(right)) return left;
            if (conegated(left, right)) return False;

            //return CONJ.the(dt, left, right); //send through again
        }


        //System.out.println(left + " " + right + " " + left.compareTo(right));
        //return CONJ.the(dt, left, right);
        if (left.compareTo(right) > 0) {
            //larger on left
            dt = -dt;
            Term t = right;
            right = left;
            left = t;
        }

        int ldt = left.dt();
        int rdt = right.dt();
        if (left.op() == CONJ && !concurrent(ldt) && ldt != XTERNAL &&
                right.op() == CONJ && !concurrent(rdt) && rdt != XTERNAL &&
                ((left.subs() > 1 + right.subs()) || (right.subs() > left.subs()))) {
            //seq imbalance
            return CONJ.the(dt, left, right); //send through again
        }


        return compound(CONJ, dt, left, right);
        //return CONJ.the(dt, left, right);

    }

//    static private Term implInConjReduction(final Term conj /* possibly a conjunction */) {
//        int cdt = conj.dt();
//        if (concurrent(cdt) || cdt == XTERNAL)
//            return implInConjReduction0(conj);
//        else
//            return conj;
//    }

    /**
     * precondition combiner: a combination nconjunction/implication reduction
     */
    static private Term implInConjReduction(final Term conj /* possibly a conjunction */) {


        if (!conj.hasAny(IMPL) || conj.op() != CONJ)
            return conj; //fall-through

        int conjDT = conj.dt();
        assert (conjDT != XTERNAL);

        //if there is only one implication subterm (first layer only), then fold into that.
        int whichImpl = -1;
        int conjSize = conj.subs();

        for (int i = 0; i < conjSize; i++) {
            if (conj.subIs(i, Op.IMPL)) {
                //only handle the first implication in this iteration
//                if (implDT == XTERNAL) {
//                    //dont proceed any further if XTERNAL
//                    //return conj;
//                }
                whichImpl = i;
                break;
            }
        }

        if (whichImpl == -1)
            return conj;

        Term implication = conj.sub(whichImpl);


        Term other;
        if (conjSize == 2) {
            other = conj.sub(1 - whichImpl);
        } else {
            //more than 2; group them as one term
            Subterms cs = conj.subterms();
            TreeSet<Term> ss = cs.toSortedSet();
            assert (ss.remove(implication)) : "must have removed something";

            Term[] css = sorted(ss);
            if (conj.dt() == conjDT && cs.equalTerms(css))
                return conj; //prevent recursive loop
            else
                other = CONJ.the(conjDT, css /* assumes commutive since > 2 */);
        }


        if (whichImpl == 0 && conjDT != DTERNAL) {
            conjDT = -conjDT; //reverse dt if impl is from the 0th subterm
        }

        Term conjInner =
                CONJ.the(conjDT, other, implication.sub(0) /* impl precond */);

        if (conjInner instanceof Bool)
            return conjInner;

        int preInInner = conjInner.subTimeSafe(implication.sub(0));
        if (preInInner == DTERNAL || preInInner == XTERNAL)
            preInInner = 0; //HACK

        int implDT = implication.dt();
        return IMPL.the(
                (implDT != DTERNAL && implDT != XTERNAL) ?
                        implDT + preInInner - conjInner.dtRange() :
                        implDT,
                conjInner, implication.sub(1));

    }


    private static Term differ(/*@NotNull*/ Op op, Term... t) {
        if (op == DIFFe && t.length == 2 && t[0] instanceof Int.IntRange && t[1].op() == INT) {
            Term simplified = ((Int.IntRange) t[0]).subtract(t[1]);
            if (simplified != Null)
                return simplified;
        }

        //TODO product 1D, 2D, etc unwrap
        //if (t.length >= 2 && Util.and((Term tt) -> tt.op() == PROD && tt.subs()==1, t)) {
        //return $.p(differ())
        //}

        //corresponding set type for reduction:
        Op set = op == DIFFe ? SETe : SETi;

        switch (t.length) {
            case 1:
                Term single = t[0];
                if (single instanceof EllipsisMatch) {
                    return differ(op, ((Subterms) single).arrayShared());
                }
                return single instanceof Ellipsislike ?
                        new UnitCompound1(op, single) :
                        Null;
            case 2:
                Term et0 = t[0], et1 = t[1];
                if (et0.equals(et1)
                        || et0.containsRecursively(et1, true, recursiveCommonalityDelimeterWeak)
                        || et1.containsRecursively(et0, true, recursiveCommonalityDelimeterWeak))

                    return Null;
                else if ((et0.op() == set && et1.op() == set))
                    return difference(set, et0, et1);
                else
                    return The.compound(op, t);


        }

        throw new InvalidTermException(op, t, "diff requires 2 terms");

    }

    public static Term difference(/*@NotNull*/ Term a, Term b) {
        Op o = a.op();
        assert (b.op() == o);
        return difference(o, a, b);
    }

    /*@NotNull*/
    public static Term difference(/*@NotNull*/ Op o, Term a, Term b) {
        //assert (!o.temporal) : "this impl currently assumes any constructed term will have dt=DTERNAL";

        if (a.equals(b))
            return Null; //empty set

        if (o == INT) {
            if (!(a instanceof Int.IntRange))
                return Null;
            else {
                Term aMinB = ((Int.IntRange) a).subtract(b);
                if (aMinB != Null) {
                    if (a.equals(aMinB))
                        return Null; //
                    return aMinB;
                }
            }
        }

//        //quick test: intersect the mask: if nothing in common, then it's entirely the first term
//        if ((a.structure() & b.structure()) == 0) {
//            return a;
//        }

        int size = a.subs();
        Collection<Term> terms = o.commutative ? new TreeSet() : $.newArrayList(size);

        for (int i = 0; i < size; i++) {
            Term x = a.sub(i);
            if (!b.contains(x)) {
                terms.add(x);
            }
        }

        int retained = terms.size();
        if (retained == size) { //same as 'a', quick re-use of instance
            return a;
        } else if (retained == 0) {
            return Null; //empty set
        } else {
            return o.the(DTERNAL, terms);
        }

    }

    /**
     * decode a term which may be a functor, return null if it isnt
     */
    @Nullable
    public static <X> Pair<X, Term> functor(Term maybeOperation, Function<Term, X> invokes) {
        if (maybeOperation.hasAll(Op.funcBits)) {
            Term c = maybeOperation;
            if (c.op() == INH) {
                Term s0 = c.sub(0);
                if (s0.op() == PROD) {
                    Term s1 = c.sub(1);
                    if (s1 instanceof Atomic /*&& s1.op() == ATOM*/) {
                        X i = invokes.apply(s1);
                        if (i != null)
                            return Tuples.pair(i, s0);
                    }
                }
            }
        }
        return null;
    }


    /**
     * last stage constructor: use with caution
     */
    /*@NotNull*/
    static Term compound(Op op, int dt, Term... subterms) {
        Term c = The.compound(op, subterms);
        if (dt != DTERNAL && (c instanceof Compound)) {
//            if (c.dt() == dt)
//                return c;

            return new GenericCompoundDT((Compound) c, dt);
        }
        return c;
    }


//        else
//            return compound(new NewCompound(op, subterms), dt);


    static boolean in(int needle, int haystack) {
        return (needle & haystack) == needle;
    }

    public static int or(/*@NotNull*/ Op... o) {
        int bits = 0;
        for (Op n : o)
            bits |= n.bit;
        return bits;
    }

    /**
     * ops across which reflexivity of terms is allowed
     */
    final static int relationDelimeterWeak = Op.or(Op.PROD, Op.CONJ, Op.NEG);
    public static final Predicate<Term> recursiveCommonalityDelimeterWeak =
            c -> !c.isAny(relationDelimeterWeak);
    final static int relationDelimeterStrong = Op.or(Op.PROD, Op.NEG);
    public static final Predicate<Term> recursiveCommonalityDelimeterStrong =
            c -> !c.isAny(relationDelimeterStrong);

//    public static final Predicate<Term> onlyTemporal =
//            c -> concurrent(c.dt());
    //c.op()!=CONJ || concurrent(c.dt()); //!c.op().temporal || concurrent(c.dt());

    private static final int InvalidImplicationSubj = or(IMPL);

    /*@NotNull*/
    static Term statement(/*@NotNull*/ Op op, int dt, /*@NotNull*/ Term subject, /*@NotNull*/ Term predicate) {

        if (subject == Null || predicate == Null)
            return Null;

        boolean dtConcurrent = concurrent(dt);
        if (dtConcurrent) {
            //if (subject.equals(predicate))
            //return True;
            if (subject.equalsRoot(predicate))
                return True;
        }

        switch (op) {

            case SIM:
            case INH:


                if (isTrueOrFalse(subject) || isTrueOrFalse(predicate))
                    return False; //$.the(subject.equals(predicate));

                break;


            case IMPL:

                //special case for implications: reduce to --predicate if the subject is False

                //if (dtConcurrent) { //no temporal basis
                if (subject == True)
                    return predicate;
                else if (subject == False)
                    return predicate.neg();
                else if (predicate instanceof Bool)
                    return Null;

                if (subject.hasAny(InvalidImplicationSubj))
                    return Null; //throw new InvalidTermException(op, dt, "Invalid equivalence subject", subject, predicate);

                if (predicate.op() == NEG) {
                    //negated predicate gets unwrapped to outside
                    return IMPL.the(dt, subject, predicate.unneg()).neg();
                }


                //factor out any common subterms iff concurrent
//                if (dtConcurrent) {
//
//                    //factor common events from subj/pred in concurrent impl
//
//                    boolean subjConj = subject.op() == CONJ;
//                    boolean predConj = predicate.op() == CONJ;
//                    boolean subjConjComm = subjConj && concurrent(subject.dt());
//                    boolean predConjComm = predConj && concurrent(predicate.dt());
//
//                    if (subjConjComm) {
//                        TermContainer subjs = subject.subterms();
//                        if (subjs.contains(predicate)) {
//                            //if (X and Y) then (X), yes obviously
//                            return True;
//                        }
//                        if (subjs.contains(predicate.neg())) {
//                            //if (--X and Y) then (X), no: contradiction
//                            return Null;
//                        }
//                    } else if (predConjComm) {
//                        TermContainer preds = predicate.subterms();
//                        if (preds.contains(subject)) {
//                            ////if X then (X and Y), no not necessarily
//                            return Null;
//                        }
//                        if (preds.contains(subject.neg())) {
//                            ////if X then (--X and Y), no: contradiction
//                            return Null;
//                        }
//                    }
//
////                    if (subjConjComm && predConjComm) {
////                        final Term csub = subject;
////                        TermContainer subjs = csub.subterms();
////                        final Term cpred = predicate;
////                        TermContainer preds = cpred.subterms();
////
////                        Term[] common = TermContainer.intersect(subjs, preds);
////                        if (common != null) {
////
////                            Set<Term> sss = subjs.toSortedSet();
////                            boolean modifiedS = false;
////                            for (Term cc : common)
////                                modifiedS |= sss.remove(cc);
////
////                            if (modifiedS) {
////                                int s0 = sss.size();
////                                switch (s0) {
////                                    case 0:
////                                        subject = True;
////                                        break;
////                                    case 1:
////                                        subject = sss.iterator().next();
////                                        break;
////                                    default:
////                                        subject = CONJ.the(/*DTERNAL?*/csub.dt(), sss);
////                                        break;
////                                }
////                            }
////
////                            @NotNull SortedSet<Term> ppp = preds.toSortedSet();
////                            boolean modifiedP = false;
////                            for (Term cc : common)
////                                modifiedP |= ppp.remove(cc);
////
////                            if (modifiedP) {
////                                int s0 = ppp.size();
////                                switch (s0) {
////                                    case 0:
////                                        predicate = True;
////                                        break;
////                                    case 1:
////                                        predicate = ppp.iterator().next();
////                                        break;
////                                    default:
////                                        predicate = CONJ.the(cpred.dt(), sorted(ppp));
////                                        break;
////                                }
////                            }
////
////
////                            return IMPL.the(dt, subject, predicate).negIf(!polarity);
////                        }
////
////                    }
//
//
//                }


                // (C ==>+- (A ==>+- B))   <<==>>  ((C &&+- A) ==>+- B)
                if (predicate.op() == IMPL) {
                    int abDT = predicate.dt();
                    //if (cprDT != XTERNAL) {
                    Term a = predicate.sub(0);


                    return IMPL.the(abDT, CONJ.the(dt /*caDT */, subject, a), predicate.sub(1));
                    //}
                }

                //filter duplicate events

                if (dt != XTERNAL && subject.dt() != XTERNAL && predicate.dt() != XTERNAL) {

                    MutableSet<LongObjectPair<Term>> se = new UnifiedSet();
                    subject.eventsWhile((w, t) -> {
                        se.add(PrimitiveTuples.pair(w, t));
                        return true;
                    }, 0, true, true, false, 0);

                    MutableSet<LongObjectPair<Term>> pe = new UnifiedSet();

                    int pre = subject.dtRange();
                    int edt = pre + (dt != DTERNAL ? dt : 0);

                    final boolean[] peChange = {false};
                    boolean contradiction = !predicate.eventsWhile((w, t) -> {
                        LongObjectPair<Term> x = PrimitiveTuples.pair(w, t);
                        if (se.contains(x)) {
                            //dont repeat it in the predicate
                            peChange[0] = true;
                        } else if (se.contains(pair(w, t.neg()))) {
                            return false; //contradiction
                        } else {
                            pe.add(x);
                        }
                        return true;
                    }, edt, true, true, false, 0);

                    if (contradiction)
                        return Null;

                    if (peChange[0]) {
                        //change occurred

                        if (pe.isEmpty()) {
                            return True; //<- TODO
                        } /*else if (se.isEmpty()) {
                            return Null;
                        } */ else {
                            //duplicates were removed, reconstruct new predicate
                            int ndt = dt != DTERNAL ? (int) pe.minBy(LongObjectPair::getOne).getOne() - pre : dt;
                            return IMPL.the(ndt,
                                    subject,
                                    /*subject.dt()!=DTERNAL ? Op.conj(se.toList()) :
                                        CONJ.the(DTERNAL, (Collection)se.collect(x->x.getTwo())),*/
                                    predicate.dt() != DTERNAL ? Op.conj(pe.toList()) :
                                            CONJ.the(DTERNAL, pe.collect(LongObjectPair::getTwo))
                            );
                        }
                    }
                }


//            if (op == INH || op == SIM || dt == 0 || dt == DTERNAL) {
//                if ((subject instanceof Compound && subject.varPattern() == 0 && subject.containsRecursively(predicate)) ||
//                        (predicate instanceof Compound && predicate.varPattern() == 0 && predicate.containsRecursively(subject))) {
//                    return False; //self-reference
//                }
//            }


                break;

            default:
                throw new UnsupportedOperationException();
        }


        if ((dtConcurrent || op != IMPL) && (subject.varPattern() == 0 && predicate.varPattern() == 0)) {
            Predicate<Term> delim = (op == IMPL && dtConcurrent) ?
                    recursiveCommonalityDelimeterStrong : Op.recursiveCommonalityDelimeterWeak;

            //apply to: inh, sim, and concurrent impl
            if ((containEachOther(subject, predicate, delim))) {
                //(!(su instanceof Variable) && predicate.contains(su)))
                return Null; //cyclic
            }
            boolean sa = subject instanceof AliasConcept.AliasAtom;
            if (sa) {
                Term sd = ((AliasConcept.AliasAtom) subject).target;
                if (sd.equals(predicate) || containEachOther(sd, predicate, delim))
                    return Null;
            }
            boolean pa = predicate instanceof AliasConcept.AliasAtom;
            if (pa) {
                Term pd = ((AliasConcept.AliasAtom) predicate).target;
                if (pd.equals(subject) || containEachOther(pd, subject, delim))
                    return Null;
            }
            if (sa && pa) {
                if (containEachOther(((AliasConcept.AliasAtom) subject).target, ((AliasConcept.AliasAtom) predicate).target, delim))
                    return Null;
            }

        }

        //already sorted here if commutive
//        if (op.commutative) {
//
////            //normalize co-negation
////            boolean sn = subject.op() == NEG;
////            boolean pn = predicate.op() == NEG;
////
//            if (/*(sn == pn) && */(subject.compareTo(predicate) > 0)) {
//                Term x = predicate;
//                predicate = subject;
//                subject = x;
//                if (dt != XTERNAL && !dtConcurrent)
//                    dt = -dt;
//            }
//
//            //assert (subject.compareTo(predicate) <= 0);
//            //System.out.println( "\t" + subject + " " + predicate + " " + subject.compareTo(predicate) + " " + predicate.compareTo(subject));
//
//        }


        return compound(op, dt, subject, predicate);
    }

//    private static Term conjDrop(Term conj, int i) {
//        TermContainer cs = conj.subterms();
//        if (cs.subs() == 2) {
//            return conj.sub(1 - i);
//        } else {
//            Term[] s = cs.theArray();
//            int sl = s.length;
//            Term[] t = new Term[sl - 1];
//            if (i > 0)
//                System.arraycopy(s, 0, t, 0, i);
//            if (i < s.length - 1)
//                System.arraycopy(s, i + 1, t, i, sl - 1 - i);
//            return CONJ.the(conj.dt(), t);
//        }
//    }

    private static boolean containEachOther(Term x, Term y, Predicate<Term> delim) {
        int xv = x.volume();
        int yv = y.volume();
        if (xv == yv)
            return x.containsRecursively(y, true, delim) || y.containsRecursively(x, true, delim);
        else if (xv > yv)
            return x.containsRecursively(y, true, delim);
        else
            return y.containsRecursively(x, true, delim);
    }

    @Nullable
    public static Op the(String s) {
        return stringToOperator.get(s);
    }

    /*@NotNull*/
    private static Term intersect(Term[] t, /*@NotNull*/ Op intersection, /*@NotNull*/ Op setUnion, /*@NotNull*/ Op setIntersection) {

        int trues = 0;
        for (Term x : t) {
            if (x == True) {
                //everything intersects with the "all", so remove this TRUE below
                trues++;
            } else if (x == False) {
                return Null;
            }
        }
        if (trues > 0) {
            if (trues == t.length) {
                return True; //all were true
            } else if (t.length - trues == 1) {
                //find the element which is not true and return it
                for (Term x : t) {
                    if (x != True)
                        return x;
                }
            } else {
                //filter the True statements from t
                Term[] t2 = new Term[t.length - trues];
                int yy = 0;
                for (Term x : t) {
                    if (x != True)
                        t2[yy++] = x;
                }
                t = t2;
            }
        }

        switch (t.length) {

            case 1:

                Term single = t[0];
                if (single instanceof EllipsisMatch) {
                    return intersect(((Subterms) single).arrayShared(), intersection, setUnion, setIntersection);
                }
                return single instanceof Ellipsislike ?
                        new UnitCompound1(intersection, single) :
                        single;

            case 2:
                return intersect2(t[0], t[1], intersection, setUnion, setIntersection);
            default:
                //HACK use more efficient way
                Term a = intersect2(t[0], t[1], intersection, setUnion, setIntersection);

                Term b = intersect(copyOfRange(t, 2, t.length), intersection, setUnion, setIntersection);

                return intersect2(a, b,
                        intersection, setUnion, setIntersection
                );
        }

    }

    /*@NotNull*/
    @Deprecated
    private static Term intersect2(Term term1, Term term2, /*@NotNull*/ Op intersection, /*@NotNull*/ Op setUnion, /*@NotNull*/ Op setIntersection) {

        if (term1.equals(term2))
            return term1;

        Op o1 = term1.op();
        Op o2 = term2.op();

        if ((o1 == setUnion) && (o2 == setUnion)) {
            //the set type that is united
            return Terms.union(setUnion, term1.subterms(), term2.subterms());
        }


        if ((o1 == setIntersection) && (o2 == setIntersection)) {
            //the set type which is intersected
            return Terms.intersect(setIntersection, term1.subterms(), term2.subterms());
        }

        if (o2 == intersection && o1 != intersection) {
            //put them in the right order so everything fits in the switch:
            Term x = term1;
            term1 = term2;
            term2 = x;
            o2 = o1;
            o1 = intersection;
        }

        //reduction between one or both of the intersection type

        TreeSet<Term> args = new TreeSet<>();
        if (o1 == intersection) {
            ((Iterable<Term>) term1).forEach(args::add);
            if (o2 == intersection)
                ((Iterable<Term>) term2).forEach(args::add);
            else
                args.add(term2);
        } else {
            args.add(term1);
            args.add(term2);
        }

        int aaa = args.size();
        if (aaa == 1)
            return args.first();

        return The.compound(intersection, args.toArray(new Term[aaa]));
    }

    public static boolean goalable(Term c) {
        return !c.hasAny(Op.NonGoalable);// && c.op().goalable;
    }


    @Override
    public String toString() {
        return str;
    }

    /**
     * writes this operator to a Writer in (human-readable) expanded UTF16 mode
     */
    public final void append(Compound c, Appendable w) throws IOException {
        append(c.dt(), w, false);
    }

    /**
     * writes this operator to a Writer in (human-readable) expanded UTF16 mode
     */
    public final void append(int dt, Appendable w, boolean invertDT) throws IOException {


        if (dt == 0) {
            //special case: parallel
            String s;
            switch (this) {
                case CONJ:
                    s = ("&|");
                    break;
                case IMPL:
                    s = ("=|>");
                    break;
//                case EQUI:
//                    s = ("<|>");
//                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            w.append(s);
            return;
        }

        boolean hasTime = dt != Tense.DTERNAL;
        if (hasTime)
            w.append(' ');

        char ch = this.ch;
        if (ch == 0)
            w.append(str);
        else
            w.append(ch);

        if (hasTime) {

            if (invertDT)
                dt = -dt;

            if (dt > 0) w.append('+');
            String ts;
            if (dt == XTERNAL)
                ts = "-";
            else
                ts = Integer.toString(dt);
            w.append(ts).append(' ');

        }
    }

    public final boolean commute(int dt, int size) {
        return size > 1 && commutative && Op.concurrent(dt);
    }


    public final Term the(int dt, /*@NotNull*/ Collection<Term> sub) {
        int s = sub.size();
        return _the(dt, commute(dt, s) ? sorted(sub) : sub.toArray(new Term[s]));
    }

    /*@NotNull*/
    public Term the(int dt, Term... u) {
        return _the(dt, commute(dt, u.length) ? sorted(u) : u);
    }

    /*@NotNull*/
    protected Term _the(int dt, Term[] u) {

        if (statement) {
            if (u.length == 1) { //similarity has been reduced
                assert (this == SIM);
                return u[0] == Null ? Null : True;
            } else {
                return statement(this, dt, u[0], u[1]);
            }
        } else {
            return compound(this, dt, u);
        }
    }

    /**
     * true if matches any of the on bits of the vector
     */
    public final boolean in(int vector) {
        return in(bit, vector);
    }

    public boolean isSet() {
        return false;
    }

    public static Term without(Term container, Term content) {

        Op co = container.op();
        if (co.commutative) {

            Subterms cs = container.subterms();

            int z = cs.subs();
            if (z > 1 && cs.contains(content)) {
                SortedSet<Term> s = cs.toSortedSet();
                if (s.remove(content)) {
                    int zs = s.size();
                    switch (zs) {
                        case 0:
                            return Null;
                        case 1:
                            return s.first();
                        default:
                            return co.the(container.dt(), s);
                    }
                }
            }
        } else {
            throw new TODO(); //this one is easy
        }
        return Null; //wasnt contained
    }

    public static int conjEarlyLate(Term x, boolean earlyOrLate) {
        assert (x.op() == CONJ);
        int d = x.sub(0).compareTo(x.sub(1));
        return (d >= 0 ? (earlyOrLate ? 0 : 1) : (earlyOrLate ? 1 : 0));
    }


    /**
     * top-level Op categories
     */
    public enum OpType {
        Statement,
        Variable,
        Other
    }

    enum Args {
        ;
        static final IntIntPair None = pair(0, 0);
        static final IntIntPair One = pair(1, 1);
        static final IntIntPair Two = pair(2, 2);

        static final IntIntPair GTEZero = pair(0, Param.COMPOUND_SUBTERMS_MAX);
        static final IntIntPair GTEOne = pair(1, Param.COMPOUND_SUBTERMS_MAX);
        static final IntIntPair GTETwo = pair(2, Param.COMPOUND_SUBTERMS_MAX);

    }

    public static class InvalidPunctuationException extends RuntimeException {
        public InvalidPunctuationException(byte c) {
            super("Invalid punctuation: " + c);
        }
    }

    private static class BoolNull extends Bool {
        public BoolNull() {
            super(String.valueOf(Op.NullSym));
        }

        final static int rankBoolNull = Term.opX(BOOL, 0);

        @Override
        public final int opX() {
            return rankBoolNull;
        }

        @Override
        public Term neg() {
            return this;
        }

        @Override
        public Term unneg() {
            return this;
        }
    }

    private static class BoolFalse extends Bool {
        public BoolFalse() {
            super(String.valueOf(Op.FalseSym));
        }

        final static int rankBoolFalse = Term.opX(BOOL, 1);

        @Override
        public final int opX() {
            return rankBoolFalse;
        }

        @Override
        public Term neg() {
            return True;
        }

        @Override
        public Term unneg() {
            return True;
        }
    }

    private static class BoolTrue extends Bool {
        public BoolTrue() {
            super(String.valueOf(Op.TrueSym));
        }

        final static int rankBoolTrue = Term.opX(BOOL, 2);

        @Override
        public final int opX() {
            return rankBoolTrue;
        }

        @Override
        public Term neg() {
            return False;
        }

        @Override
        public Term unneg() {
            return True;
        } //doesnt change
    }
}
