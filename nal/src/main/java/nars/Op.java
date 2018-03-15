package nars;


import jcog.Util;
import jcog.data.ArrayHashSet;
import jcog.data.bit.MetalBitSet;
import jcog.list.FasterList;
import jcog.memoize.HijackMemoize;
import jcog.pri.AbstractPLink;
import nars.derive.match.Ellipsis;
import nars.derive.match.EllipsisMatch;
import nars.derive.match.Ellipsislike;
import nars.op.mental.AliasConcept;
import nars.subterm.Neg;
import nars.subterm.Subterms;
import nars.subterm.TermVector;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.compound.CachedCompound;
import nars.term.compound.CachedUnitCompound;
import nars.term.compound.util.ConjEvents;
import nars.term.var.UnnormalizedVariable;
import nars.time.Tense;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.map.mutable.primitive.LongByteHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Arrays.copyOfRange;
import static nars.term.Terms.sorted;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.XTERNAL;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * NAL symbol table
 */
public enum Op {


    ATOM(".", Op.ANY_LEVEL, OpType.Other),

    NEG("--", 1, Args.One) {
        @Override
        public Term the(int dt, Term[] u, boolean intern) {
            return compound(dt, u);
        }

        @Override
        public Term the(int dt, Term... u) {
            return compound(dt, u);
        }

        @Override
        protected Term compound(int dt, Term[] u, boolean intern) {
            return compound(dt, u);
        }

        protected Term compound(int dt, Term[] u) {
            //assert(u.length == 1);
            if (u.length != 1) //assert (dt == DTERNAL || dt == XTERNAL);
                throw new RuntimeException("negation requires one subterm");
            return Neg.the(u[0]);
        }

        @Override
        protected boolean internable(int dt, Term[] u) {
            return false;
        }
    },

    INH("-->", 1, OpType.Statement, Args.Two),
    SIM("<->", true, 2, OpType.Statement, Args.Two),

    /**
     * extensional intersection
     */
    SECTe("&", true, 3, Args.GTETwo) {
        @Override
        public Term compound(int dt, Term[] u) {
            return intersect(/*Int.intersect*/(u),
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
        public Term compound(int dt, Term[] u) {
            return intersect(/*Int.intersect*/(u),
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
        public Term compound(int dt, Term[] u) {
            return differ(this, u);
        }
    },

    /**
     * intensional difference
     */
    DIFFi("-", false, 3, Args.Two) {
        @Override
        public Term compound(int dt, Term[] u) {
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
            return (t.length == 0) ? ZeroProduct : super.the(dt, t);
        }
    },


    /**
     * conjunction
     */
    CONJ("&&", true, 5, Args.GTETwo) {
        @Override
        public Term compound(int dt, Term[] u) {
            final int n = u.length;
            switch (n) {

                case 0:
                    return True;

                case 1:
                    Term only = u[0];
                    if (only instanceof EllipsisMatch) {
                        return a(dt, ((EllipsisMatch) only).arrayShared()); //unwrap
                    } else {

                        //preserve unitary ellipsis for patterns etc
                        return only instanceof Ellipsislike ?
                                new CachedUnitCompound(CONJ, only) //special; preserve the surrounding conjunction
                                :
                                only;
                    }

            }

            boolean cdt = concurrent(dt);

            int trues = 0; //# of trues to ignore
            for (Term t : u) {
                if (t == Op.Null || t == False)
                    return t; //short-circuit
                else if (t == Op.True)
                    trues++;
            }

            if (trues > 0) {
                //filter out all boolean terms
                int sizeAfterTrueRemoved = u.length - trues;
                switch (sizeAfterTrueRemoved) {
                    case 0:
                        //nothing remains, this term vaporizes to an innocuous True
                        return True;
                    case 1: {
                        //find and return the only non-True term
                        for (Term uu : u) {
                            if (uu != True) {
                                assert (!(uu instanceof Ellipsislike)) : "if this happens, TODO";
                                return uu;
                            }
                        }
                        throw new RuntimeException("should have found non-True term to return");
                    }
                    default: {
                        Term[] y = new Term[sizeAfterTrueRemoved];
                        int j = 0;
                        for (int i = 0; j < y.length; i++) {
                            Term uu = u[i];
                            if (uu != True)
                                y[j++] = uu;
                        }
                        assert (j == y.length);
                        //return CONJ.the(dt, y);
                        u = y;
                    }
                }
            }


            Term ci;
            switch (dt) {
                case DTERNAL:
                case 0:
                    if (u.length == 2 && u[0].equalsNeg(u[1])) //fast conegation check, rather than the exhaustive multi-term one ahead
                        return False;
                    ci = junctionFlat(dt, u);
                    break;



                case XTERNAL:
                    //sequence or xternal
                    //assert (n == 2) : "invalid non-commutive conjunction arity!=2, arity=" + n;

                    //rebalance and align
                    //convention: left align all sequences
                    //ex: (x &&+ (y &&+ z))
                    //      becomes
                    //    ((x &&+ y) &&+ z)

                    int ul = u.length;
                    if (ul > 1) {
                        boolean unordered = false;
                        for (int i = 0; i < ul - 1; i++) {
                            if (u[i].compareTo(u[i + 1]) > 0) {
                                unordered = true;
                                break;
                            }
                        }
                        if (unordered) {
                            u = u.clone();
                            if (ul == 2) {
                                //just swap
                                Term u0 = u[0];
                                u[0] = u[1];
                                u[1] = u0;
                            } else {
                                Arrays.sort(u); //dont use Terms.sorted which will de-duplicate and remove (x &&+1 x) cases.
                            }
                        }

                    }

                    switch (u.length) {
                        case 0:
                            return True;

                        case 1:
                            return u[0];

                        case 2: {


                            Term a = u[0];
                            if (a.op() == CONJ && a.dt() == XTERNAL && a.subs() == 2) {
                                Term b = u[1];

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
                            break;
                        }

                        default:
                            break;
                    }

                    if (u.length > 1) {
                        return instance(CONJ, XTERNAL, u);
                    } else {
                        return u[0];
                    }

                default: {
                    if (n != 2) {
                        return Null;
                    }

                    ci = conjMerge(u[0], u[1], dt);
                }
            }


            //(NOT (x AND y)) AND (NOT x) == NOT X
            if (ci.op() == CONJ && ci.hasAny(NEG)) {
                Subterms cci;
                if ((cci = ci.subterms()).hasAny(CONJ)) {
                    int ciDT = ci.dt();
                    if (ciDT == 0 || ciDT == DTERNAL) {
                        int s = cci.subs();
                        RoaringBitmap ni = null, nc = null;
                        for (int i = 0; i < s; i++) {
                            Term cii = cci.sub(i);
                            if (cii.op() == NEG) {
                                if (cii.unneg().op() == CONJ) {
                                    if (nc == null) nc = new RoaringBitmap();
                                    nc.add(i);
                                } else {
                                    if (ni == null) ni = new RoaringBitmap();
                                    ni.add(i);
                                }
                            }
                        }
                        if (nc != null && ni != null) {
                            int[] bb = ni.toArray();
                            MetalBitSet toRemove = MetalBitSet.bits(bb.length);
                            PeekableIntIterator ncc = nc.getIntIterator();
                            while (ncc.hasNext()) {
                                int nccc = ncc.next();
                                for (int j = 0; j < bb.length; j++) {
                                    Term NC = cci.sub(nccc).unneg();
                                    Term NX = cci.sub(bb[j]).unneg();
                                    if (NC.contains(NX)) {
                                        toRemove.set(nccc);
                                    }
                                }
                            }
                            if (toRemove.getCardinality() > 0) {
                                return CONJ.the(ciDT, cci.termsExcept(toRemove));
                            }
                        }


                    }
                }
            }

            return implInConjReduce(ci);
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
    public static int ConstantAtomics = Op.ATOM.bit | Op.INT.bit;

    public final boolean indepVarParent;
    public final boolean depVarParent;

    public static final Compound ZeroProduct = CachedCompound.the(Op.PROD, Subterms.Empty);

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
     * tautological absolute false
     */
    public static final Bool False = new BoolFalse();

    /**
     * tautological absolute true
     */
    public static final Bool True = new BoolTrue();

    /**
     * specifier for any NAL level
     */
    private static final int ANY_LEVEL = 0;
    public static final int SectBits = or(Op.SECTe, Op.SECTi);
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

    /**
     * TODO option for instantiating CompoundLight base's in the bottom part of this
     */
    public static Term dt(Compound base, int nextDT) {

        //if (base.dt() == nextDT) return base;

        return base.op().the(nextDT, base.arrayShared());

//        if (nextDT == XTERNAL) {
//            return new CompoundDTLight(base, XTERNAL);
//        } else {
//            Subterms subs = base.subterms();
//            int ns = subs.subs();
////                if (nextDT == DTERNAL && ns == 2 && !subs.sub(0).unneg().equals(subs.sub(1).unneg()))
////                    return base; //re-use base only if the terms are inequal
//
//            /*@NotNull*/
//            if (ns > 2 && !concurrent(nextDT))
//                return Null; //tried to temporalize what can only be commutive
//
//
//            Term[] ss = subs.arrayShared();
//
//            Op o = base.op();
//            assert (o.temporal);
//            if (o.commutative) {
//
//                if (ss.length == 2) {
//                    //must re-arrange the order to lexicographic, and invert dt
//                    return o.the(nextDT != DTERNAL ? -nextDT : DTERNAL, ss[1], ss[0]);
//                } else {
//                    return o.the(nextDT, ss);
//                }
//            } else {
//                return o.the(nextDT, ss);
//            }
//        }
    }

    /**
     * returns null if wasnt contained, Null if nothing remains after removal
     */
    @Nullable
    public static Term conjDrop(NAR nar, Term conj, Term _event, boolean earlyOrLate) {
        if (conj.op() != CONJ || conj.impossibleSubTerm(_event))
            return null;

        Term event = _event.root();

        int dt = conj.dt();
        if (dt != DTERNAL && dt != 0) {


            FasterList<LongObjectPair<Term>> events = conj.eventList();
            Comparator<LongObjectPair<Term>> c = Comparator.comparingLong(LongObjectPair::getOne);
            int eMax = events.maxIndex(earlyOrLate ? c.reversed() : c);

            LongObjectPair<Term> ev = events.get(eMax);
            if (ev.getTwo().equalsRoot(event)) {
                if (events.size() == 1)
                    return Null; //emptied

                events.remove(eMax);
                return Op.conj(events);
            } else {
                return null;
            }

        } else {
            return Op.without(conj, (t) -> t.equalsRoot(event), nar.random());
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


    public final Term the(Subterms s) {
        return the(s.arrayShared());
    }

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

    /**
     * 1 << op.ordinal
     */
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


    public static Term conjMerge(Term a, Term b, int dt) {
        return (dt >= 0) ?
                conjMerge(a, 0, b, +dt + a.dtRange()) :
                conjMerge(b, 0, a, -dt + b.dtRange());
    }


    /**
     * merge a set of terms with dt=0
     */
    static public Term conjMerge(Term... x) {
        assert (x.length > 1);
        ArrayHashSet<LongObjectPair<Term>> xx = new ArrayHashSet();
        for (int i = 0; i < x.length; i++) {
            xx.addAll(x[i].eventList(0, 1, true, true));
        }
        return conj((FasterList)xx.list);
    }


    static public Term conjMerge(Term a, @Deprecated long aStart, Term b, long bStart) {
        FasterList<LongObjectPair<Term>> ae = a.eventList(aStart, 1);
        FasterList<LongObjectPair<Term>> be = b.eventList(bStart, 1);
        ae.addAll(be);
        return conj(ae);
    }

//    /*@NotNull*/
//    static public Term conjMerge0(Term a, @Deprecated long aStart, Term b, long bStart) {
//
//        int ae = a.eventCount();
//        int be = b.eventCount();
//        if (ae == 1 && be == 1) {
//            long bo = bStart - aStart;
//            assert (bo < Integer.MAX_VALUE);
//            return conjSeqFinal((int) bo, a, b);
//        }
//
//        LongObjectHashMap<Collection<Term>> eventSets = new LongObjectHashMap(ae + be);
//
//        LongObjectPredicate<Term> insert = (long w, Term xb) -> {
//            Collection<Term> ab = eventSets.updateValue(w,
//                    () -> {
//                        Collection<Term> x = new UnifiedSet<>(1);
//                        x.add(xb);
//                        return x;
//                    },
//                    (Collection<Term> xx) -> {
//                        if (xx.add(xb)) {
//                            Op o = xb.op();
//                            if (o == NEG) {
//                                if (xx.contains(xb.unneg()))
//                                    return null;
//                            } else {
//                                //if (xa.contains(xb.neg())) return null;
//                                for (Term z : xx) {
//                                    if (z.op() == NEG && z.sub(0).equals(xb))
//                                        return null;
//                                }
//                            }
//                        }
//                        return xx;
//                    });
//            return ab != null;
//        };
//
//        if (!(a.eventsWhile(insert, aStart) && b.eventsWhile(insert, bStart)))
//            return False;
//
//        LongObjectHashMap<Term> eventSet = new LongObjectHashMap<>();
//        Term[] cut = {null};
//        if (!eventSets.keyValuesView().allSatisfy((ws) -> {
//            Term[] sps = sorted(ws.getTwo());
//            assert (sps.length > 0);
//            Term pp;
//            if (sps.length == 1)
//                pp = sps[0];
//            else {
//                pp = implInConjReduce(instance(CONJ, 0, sps)); //direct
//            }
//            if (pp instanceof Bool) {
//                cut[0] = pp;
//                return false;
//            }
//            eventSet.put(ws.getOne(), pp);
//            return true;
//        })) {
//            return cut[0];
//        }
//
//
//        FasterList<LongObjectPair<Term>> events = new FasterList<>(eventSet.size());
//        eventSet.forEachKeyValue((w, t) -> events.add(PrimitiveTuples.pair(w, t)));
//        return conj(events);
//
////        //group all parallel clusters
////        LongObjectPair<Term> e0 = events.get(0);
////
////        long headAt = e0.getOne();
////        int groupStart = -1;
////        for (int i = 1; i <= ee; i++) {
////            long nextAt = (i != ee) ? events.get(i).getOne() : ETERNAL;
////            if (nextAt == headAt) {
////                if (groupStart == -1) groupStart = i - 1;
////            } else {
////                if (groupStart != -1) {
////                    int groupEnd = i;
////                    Term[] p = new Term[groupEnd - groupStart];
////                    assert (p.length > 1);
////                    long when = events.get(groupStart).getOne();
////                    for (int k = 0, j = groupStart; j < groupEnd; j++) {
////                        p[k++] = events.get(groupStart).getTwo();
////                        events.remove(groupStart);
////                        i--;
////                        ee--;
////                    }
////                    Term replacement = p.length > 1 ? CONJ.the(0, p) : p[0];
////                    if (events.isEmpty()) {
////                        //got them all here
////                        return replacement;
////                    }
////                    events.add(i, PrimitiveTuples.pair(when, replacement));
////                    i++;
////                    ee++;
////                    groupStart = -1; //reset
////                }
////            }
////            headAt = nextAt;
////        }
//
//    }

    public static Term conj(FasterList<LongObjectPair<Term>> events) {
        ConjEvents ce = new ConjEvents();

        for (LongObjectPair<Term> o : events) {
            if (!ce.add(o.getTwo(), o.getOne())) {
                break;
            }
        }

        return ce.term();
    }

    /**
     * constructs a correctly merged conjunction from a list of events
     * note: this modifies the event list
     */
    public static Term conj0(FasterList<LongObjectPair<Term>> events) {

        final int ee = events.size();
        switch (ee) {
            case 0:
                return True;
            case 1:
                return events.get(0).getTwo();
            default:
                events.sortThisByLong(LongObjectPair::getOne);

                LongHashSet times = new LongHashSet(ee); //TODO lazy alloc only after a duplicate sequence time has been detected
                LongByteHashMap collides = null;

                for (int i = 0; i < ee; i++) {
                    LongObjectPair<Term> p = events.get(i);
                    long pt = p.getOne();
                    if (!times.add(pt)) {
                        if (collides == null)
                            collides = new LongByteHashMap(2);

                        byte n = collides.getIfAbsentPut(pt, (byte) 1);
                        collides.addToValue(pt, (byte) 1);
                    }
                }
                if (collides != null) {
                    ListIterator<LongObjectPair<Term>> ii = events.listIterator();
                    int batchRemain = 0;
                    Term[] batch = null;
                    while (ii.hasNext()) {
                        LongObjectPair<Term> p = ii.next();

                        if (batchRemain == 0) {
                            long pt = p.getOne();
                            batchRemain = collides.removeKeyIfAbsent(pt, (byte) 0);
                            assert (batchRemain == 0 || batchRemain > 1) : "batchRemain=" + batchRemain;
                            if (batchRemain > 0) {
                                ii.remove();
                                batch = new Term[batchRemain--];
                                batch[0] = p.getTwo();
                            }
                        } else {
                            ii.remove();
                            batch[batch.length - batchRemain--] = p.getTwo();

                            if (batchRemain == 0) {
                                Term b = CONJ.the(0, batch);
                                if (b == False)
                                    return False;
                                if (b == Null)
                                    return Null;
                                if (b != True)
                                    ii.add(pair(p.getOne(), b));
                                batch = null;
                            }
                        }

                    }
                }
//                //TODO optimize this
//                ListIterator<LongObjectPair<Term>> ii = events.listIterator();
//                long prevtime = TIMELESS;
//                int prevtimeStart = 0;
//                while (ii.hasNext()) {
//                    LongObjectPair<Term> x = ii.next();
//                    Term xt = x.getTwo();
//                    if (xt == True) {
//                        ii.remove(); //skip
//                    } else if (xt == False) {
//                        return False;
//                    } else if (xt == Null) {
//                        return Null;
//                    }
//
//                    long now = x.getOne();
//
//                    if (now == ETERNAL)
//                        throw new TODO();
//                    else if (now == TIMELESS)
//                        throw new RuntimeException();
//                    int at = ii.previousIndex();
//
//                    boolean hasNext = ii.hasNext();
//                    if (prevtime == now && hasNext) {
//                        //continue, will be grouped when the # changes
//                    } else {
//                        if ((hasNext && prevtimeStart < at - 1) || (!hasNext && prevtime == now)) {
//                            Term[] s = new Term[at - prevtimeStart + (!hasNext ? 1 : 0)];
//                            assert (s.length > 1);
//                            int j = 0;
//                            if (!hasNext) {
//                                s[j++] = xt; //include current
//                                ii.remove();
//                            } else {
//                                ii.previous();
//                            }
//                            for (; j < s.length; ) {
//                                LongObjectPair<Term> e = ii.previous();
//                                ii.remove();
//                                s[j++] = e.getTwo();
//                            }
//                            Term xyt = CONJ.the(0, s);
//                            if (xyt == Null) return Null;
//                            if (xyt == False) return False;
//                            if (xyt != True) {
//                                LongObjectPair<Term> xy = pair(prevtime, xyt);
//                                ii.add(xy);
//                                if (hasNext)
//                                    ii.next();
//                            }
//                            prevtimeStart = ii.previousIndex();
//                        } else {
//                            prevtimeStart = at;
//                        }
//                        prevtime = now;
//                    }
//                }
                int e = events.size();
                switch (e) {
                    case 0:
                        return True;
                    case 1:
                        return events.get(0).getTwo();
                    default:
                        return ConjEvents.conjSeq(events);
                }

        }
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
    @Deprecated
    public static Term implInConjReduce(final Term conj /* possibly a conjunction */) {
        return conj;
    }
//        if (conj.op() != CONJ)
//            return conj;
//
//        if (!conj.hasAny(IMPL))
//            return conj; //fall-through
//
//        int conjDT = conj.dt();
////        if (conjDT!=0 && conjDT!=DTERNAL)
////            return conj; //dont merge temporal
////        assert (conjDT != XTERNAL);
//
//        //if there is only one implication subterm (first layer only), then fold into that.
//        int whichImpl = -1;
//        int conjSize = conj.subs();
//
//
//        boolean implIsNeg = false;
//        for (int i = 0; i < conjSize; i++) {
//            Term conjSub = conj.sub(i);
//            Op conjSubOp = conjSub.op();
//            if (conjSubOp == IMPL) {
//                //only handle the first implication in this iteration
////                if (implDT == XTERNAL) {
////                    //dont proceed any further if XTERNAL
////                    //return conj;
////                }
//                whichImpl = i;
//                break;
//            } else if (conjSubOp == NEG) {
//                if (conjSub.unneg().op() == IMPL) {
//                    whichImpl = i;
//                    implIsNeg = true;
//                    break;
//                }
//            }
//        }
//
//        if (whichImpl == -1)
//            return conj;
//
//        Term implication = conj.sub(whichImpl).unneg();
//        Term implicationPolarized = conj.sub(whichImpl);
//
//        Term other;
//        if (conjSize == 2) {
//            other = conj.sub(1 - whichImpl);
//        } else {
//            //more than 2; group them as one term
//            Subterms cs = conj.subterms();
//            SortedSet<Term> ss = cs.toSetSorted();
//            boolean removed = ss.remove(implicationPolarized);
//            assert removed : "must have removed something";
//
//            Term[] css = sorted(ss);
//            if (conj.dt() == conjDT && cs.equalTerms(css))
//                return conj; //prevent recursive loop
//            else
//                other = CONJ.the(conjDT, css /* assumes commutive since > 2 */);
//        }
//
//        if (other.op() == IMPL) {
//            //TODO if other is negated impl
//            if ((other.dt() == DTERNAL) && other.sub(1).equals(implicationPolarized.sub(1))) {
//                //same predicate
//                other = other.sub(0);
//            } else {
//                //cant be combined
//                return conj;
//            }
//        }
//        int implDT = implication.dt();
//
//        Term conjInner;
//        Term ours = implication.sub(0);
//        int cist;
//        if (conjDT == DTERNAL) {
//            conjInner = CONJ.the(other, ours);
//            cist = 0;
//        } else if ((conjDT < 0 ? (1 - whichImpl) : whichImpl) == 1) {
//            conjInner = conjMerge(other, 0, ours, -conjDT);
//            cist = -conjDT;
//        } else {
//            conjInner = conjMerge(ours, 0, other, conjDT);
//            cist = 0;
//        }
//
//
//        if (conjInner instanceof Bool)
//            return conjInner;
//
//
//        if (implDT != DTERNAL) {
////            int cist = conjInner.dt() == DTERNAL ? 0 : conjInner.subTimeSafe(ours); //HACK
////            if (cist == DTERNAL)
////                throw new TODO();
//
//            implDT -= (conjInner.dtRange() - cist) - (ours.dtRange());
//        }
//
//        return IMPL.the(implDT, conjInner, implication.sub(1)).negIf(implIsNeg);
//
//    }

    static boolean hasNull(Term[] t) {
        for (Term x : t)
            if (x == Null)
                return true;
        return false;
    }

    private static Term differ(/*@NotNull*/ Op op, Term... t) {


//        if (hasNull(t))
//            return Null;

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
                        new CachedUnitCompound(op, single) :
                        Null;
            case 2:
                Term et0 = t[0], et1 = t[1];

                //false tautology - the difference of something with itself will always be nothing, ie. freq=0, ie. False
                if (et0.equals(et1))
                    return False;

//                //true tautology - the difference of something with its negation will always be everything, ie. freq==1 : True
                if (et1.neg().equals(et0))
                    return True;

//                if (et0 == True && et1 == False) //TRUE - FALSE
//                    return True;

                //null tautology - incomputable comparisons with truth
                if (isTrueOrFalse(et0) || isTrueOrFalse(et1))
                    return Null;


                if (et0.containsRecursively(et1, false, recursiveCommonalityDelimeterWeak)
                        || et1.containsRecursively(et0, false, recursiveCommonalityDelimeterWeak))
                    return Null; //TODO handle this better, there may be detectable or iteratively simplified reductions

                if (op == DIFFe && et0 instanceof Int.IntRange && et1.op() == INT) {
                    Term simplified = ((Int.IntRange) et0).subtract(et1);
                    if (simplified != Null)
                        return simplified;
                }

                if ((et0.op() == set && et1.op() == set))
                    return difference(set, et0, et1);
                else
                    return Op.instance(op, t);


        }

        throw new Term.InvalidTermException(op, t, "diff requires 2 terms");

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
     * direct constructor
     * no reductions or validatios applied
     * use with caution
     */
    public static Term instance(Op op, Term... subterms) {
        return instance(op, DTERNAL, subterms);
    }


    /**
     * direct constructor
     * no reductions or validatios applied
     * use with caution
     */
    public static Term instance(Op o, int dt, Term... u) {
        assert (!o.atomic) : o + " is atomic, with subterms: " + (u);

        boolean hasEllipsis = false;

        for (Term x : u) {
            if (!hasEllipsis && x instanceof Ellipsis)
                hasEllipsis = true;
            if (x == Null)
                return Null;
        }


        int s = u.length;
        assert (o.maxSize >= s) :
                "subterm overflow: " + o + ' ' + (u);
        assert (o.minSize <= s || hasEllipsis) :
                "subterm underflow: " + o + ' ' + (u);

        if (s == 1) {
            switch (o) {
                case NEG:
                    return Neg.the(u[0]);
                case PROD:
                    //use full CachedCompound for PROD
                    //use default below
                    break;
                default:
                    return new CachedUnitCompound(o, u[0]);
            }


        }

        Subterms ss;
        if (o != PROD) {
            //cache.get ?
            ss = Subterms.subtermsInterned(u);
        } else {
            ss = Subterms.subtermsInstance(u);
        }

        return CachedCompound.the(o, dt, ss);
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
    final static int relationDelimeterWeak = Op.or(Op.PROD, Op.SETe, Op.CONJ, Op.NEG);
    public static final Predicate<Term> recursiveCommonalityDelimeterWeak =
            c -> !c.isAny(relationDelimeterWeak);
    final static int relationDelimeterStrong = Op.or(Op.PROD, Op.SETe, Op.NEG);
    public static final Predicate<Term> recursiveCommonalityDelimeterStrong =
            c -> !c.isAny(relationDelimeterStrong);

//    public static final Predicate<Term> onlyTemporal =
//            c -> concurrent(c.dt());
    //c.op()!=CONJ || concurrent(c.dt()); //!c.op().temporal || concurrent(c.dt());

    private static final int InvalidImplicationSubj = or(IMPL);

    /*@NotNull*/
    static Term statement(/*@NotNull*/ Op op, int dt, final Term subject, final Term predicate) {

        if (subject == Null || predicate == Null)
            return Null;

        boolean dtConcurrent = concurrent(dt);
        if (dtConcurrent) {
            //if (subject.equals(predicate))
            //return True;
            if (subject.equalsRoot(predicate))
                return True;
        }

        if (op == INH || op == SIM) {
            if (isTrueOrFalse(subject)) {
                //nothing is or is the intension of true or false
                return Null;
            }
            if (op == SIM && isTrueOrFalse(predicate)) {
                // similarity with truth is prevented since it being a commutive operator,
                //      then at best, no information can gained
                // but inheritance is allowed and this can link term-level truth and task-level truth
                return Null;
            }
        }

        if (op == IMPL) {

            //special case for implications: reduce to --predicate if the subject is False

            //if (dtConcurrent) { //no temporal basis
            if (subject == True)
                return predicate; //true tautology
            else if (subject == False)
                return predicate.neg(); //false tautology
            else if (predicate instanceof Bool)
                return Null; //nothing is the "cause" of tautological trueness or falseness

            if (subject.hasAny(InvalidImplicationSubj))
                return Null; //throw new InvalidTermException(op, dt, "Invalid equivalence subject", subject, predicate);

            if (predicate.op() == NEG) {
                //negated predicate gets unwrapped to outside
                return IMPL.the(dt, subject, predicate.unneg()).neg();
            }


//                //factor out any common subterms iff concurrent
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
            switch (predicate.op()) {
                case IMPL: {
                    return IMPL.the(predicate.dt(),
                            CONJ.the(dt /*caDT */, subject, predicate.sub(0)),
                            predicate.sub(1));
                }
//                case CONJ: {
//                    // (C ==>+- (A &&+ B))   <<==>>  ((C &&+- A) ==>+ B)
//                    // only if (A &&+ B) is temporal and A is the earlier of the two events
//                    int pdt = predicate.dt();
//                    if (pdt != 0 && pdt != XTERNAL && pdt != DTERNAL) {
//                        int e = conjEarlyLate(predicate, true);
//                        Term early = predicate.sub(e);
//                        Term late = predicate.sub(1 - e);
//                        return IMPL.the(pdt,
//                                CONJ.the(dt /*caDT */, subject, early),
//                                late);
//                    }
//
//                }
            }


            //filter duplicate events and detect contradictions

            if (dt != XTERNAL && subject.dt() != XTERNAL && predicate.dt() != XTERNAL) {

                ArrayHashSet<LongObjectPair<Term>> se = new ArrayHashSet<>(4);
                subject.eventsWhile((w, t) -> {
                    se.add(PrimitiveTuples.pair(w, t));
                    return true;
                }, 0, true, true, false, 0);

                FasterList<LongObjectPair<Term>> pe = new FasterList(4);
                int pre = subject.dtRange();
                boolean dtNotDternal = dt != DTERNAL;
                int edt = pre + (dtNotDternal ? dt : 0);

                final boolean[] peChange = {false};

                //if there is DT between subject and predicate,
                // dont decompose parallel to keep a parallel grouping in the predicate intact. otherwise it will get arbitrarily split
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
                    return False;

                //merge or contradict any DTERNAL predicate components occurring
                // at same time
                if ((dt == DTERNAL || dt == 0)) {
                    for (ListIterator<LongObjectPair<Term>> pi = pe.listIterator(); pi.hasNext(); ) {
                        LongObjectPair<Term> pex = pi.next();
                        Term pext = pex.getTwo();
                        if (pext.op() == CONJ) {
                            int pdt = pext.dt();
                            if (pdt == DTERNAL || pdt == 0) {
                                long at = pex.getOne();

                                RoaringBitmap pextRemovals = null;
                                Subterms subPexts = pext.subterms();
                                int subPextsN = subPexts.subs();

                                for (ListIterator<LongObjectPair<Term>> si = se.listIterator(); si.hasNext(); ) {
                                    LongObjectPair<Term> sse = si.next();
                                    if (sse.getOne() == at) {

                                        //determine if each component either is absorbed or contradicted
                                        //note: true or false are propagated upward the conjunction stack, it may get eliminated
                                        Term sset = sse.getTwo();

                                        for (int i = 0; i < subPextsN; i++) {
                                            Term subPext = subPexts.sub(i);
                                            Term merge = CONJ.the(dt, sset, subPext);
                                            if (merge == Null) return Null; //invalid
                                            else if (merge == False) {
                                                //contradict
                                                return False;
                                            } else if (merge.equals(sset)) {
                                                //unchanged, just drop it
                                                if (pextRemovals == null)
                                                    pextRemovals = new RoaringBitmap();
                                                pextRemovals.add(i);
                                            } else {
                                                //?? leave in predicate
                                            }
                                        }
                                    }
                                }
                                if (pextRemovals != null) {
                                    if (pextRemovals.getCardinality() == subPextsN) {
                                        //completely absorbed
                                        pi.remove();
                                    } else {
                                        pi.set(pair(at, CONJ.the(pdt, subPexts.termsExcept(pextRemovals))));
                                    }
                                    peChange[0] = true;
                                }
                            }
                        }
                    }
                }


                if (pe.isEmpty())
                    return True; //fully reduced

//                int pes = pe.size();
//                switch (pes) {
//                    case 0:
//                        return True;
//                    case 1:
                        if (peChange[0]) {
                            //change occurred, duplicates were removed, reconstruct new predicate
                            int ndt = dtNotDternal ? (int) pe.minBy(LongObjectPair::getOne).getOne() - pre : DTERNAL;
                            Term newPredicate;
                            if (pe.size()==1) {
                                newPredicate = pe.getOnly().getTwo();
                            } else if (predicate.dt()==DTERNAL) {
                                //construct && from the subterms since it was originally && otherwise below will construct &|
                                ConjEvents c = new ConjEvents();
                                for (int i = 0, peSize = pe.size(); i < peSize; i++) {
                                    if (!c.add(pe.get(i).getTwo(), ETERNAL)) //override as ETERNAL
                                        break;
                                }
                                newPredicate = c.term();
                            } else {
                                newPredicate = Op.conj(pe);
                            }

                            return IMPL.the(ndt, subject, newPredicate);
                        }
//                        break;
//                    default: {
                        //TODO if pred has >1 events, and dt is temporal, pull all the events except the last into a conj for the subj then impl the final event

//                        if (dt != DTERNAL) {
//                            long finalEventTime = pe.maxBy(LongObjectPair::getOne).getOne();
//                            Term finalEvent = null;
//                            int moved = 0;
//                            for (int i = 0; i < pes; i++) {
//                                LongObjectPair<Term> m = pe.get(i);
//                                if (m.getOne() != finalEventTime) {
//                                    se.add(m);
//                                    moved++;
//                                } else {
//                                    if (finalEvent == null) finalEvent = m.getTwo();
//                                    else finalEvent = CONJ.the(0, finalEvent, m.getTwo());
//                                }
//                            }
//                            if (moved > 0 || !finalEvent.equals(predicate)) {
//                                long ndt = dtNotDternal ?
//                                        finalEventTime - ((FasterList<LongObjectPair<Term>>) se.list).maxBy(LongObjectPair::getOne).getOne() :
//                                        DTERNAL;
//                                assert (ndt < Integer.MAX_VALUE);
//                                return IMPL.the((int) ndt,
//                                        Op.conjEvents(new FasterList(se)),
//                                        finalEvent
//                                );
//                            }
//                        }


//                    }
//              }

            }


//            if (op == INH || op == SIM || dt == 0 || dt == DTERNAL) {
//                if ((subject instanceof Compound && subject.varPattern() == 0 && subject.containsRecursively(predicate)) ||
//                        (predicate instanceof Compound && predicate.varPattern() == 0 && predicate.containsRecursively(subject))) {
//                    return False; //self-reference
//                }
//            }

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


        return instance(op, dt, subject, predicate);
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


//    public static boolean equalsOrContainEachOther(Term x, Term y) {
//        return x.unneg().equals(y.unneg()) || containEachOther(x, y);
//    }
//    public static boolean containEachOther(Term x, Term y) {
//        return containEachOther(x, y, recursiveCommonalityDelimeterStrong);
//    }

    public static boolean containEachOther(Term x, Term y, Predicate<Term> delim) {
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


    private static Term intersect(Term[] t, /*@NotNull*/ Op intersection, /*@NotNull*/ Op setUnion, /*@NotNull*/ Op setIntersection) {

        int trues = 0;
        for (Term x : t) {
            if (x == True) {
                //everything intersects with the "all", so remove this TRUE below
                trues++;
            } else if (x == Null || x == False) {
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
                        new CachedUnitCompound(intersection, single) :
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
        else {
            return instance(intersection, args.toArray(new Term[aaa]));
        }
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
        return commutative && size > 1 && Op.concurrent(dt);
    }


    public final Term the(int dt, /*@NotNull*/ Collection<Term> sub) {
        int s = sub.size();
        return the(dt, sub.toArray(new Term[s]));
    }

    /*@NotNull*/
    public Term the(int dt, Term... u) {

        if (Op.hasNull(u))
            return Null;

        return compound(dt, commute(dt, u.length) ? sorted(u) : u, true);
    }

    /**
     * alternate method args order for 2-term w/ infix DT
     */
    public final Term the(Term a, int dt, Term b) {
        return the(dt, a, b);
    }

    /**
     * syntax shortcut for non-interned (heap) term construction
     */
    public final Term a(Term... u) {
        return the(DTERNAL, u, false);
    }

    /**
     * syntax shortcut for non-interned (heap) term construction
     */
    public final Term a(int dt, Term... u) {
        return the(dt, u, false);
    }

    public final Term a(Term x, int dt, Term y) {
        return the(dt, x, y);
    }

    public Term the(int dt, Term[] u, boolean intern) {
        return compound(dt, commute(dt, u.length) ? sorted(u) : u, intern);
    }

    protected boolean internable(int dt, Term[] u) {
//        if (dt ... )
//            return false;

        return internable(u);
    }

    public static boolean internable(Term[] u) {
        if (u.length == 0)
            return false;

        boolean cache = true;
        for (Term x : u) {
            if (!(x instanceof The)) {
                //HACK caching these interferes with unification.  instead fix unification then allow caching of these
                cache = false;
                break;
            }
        }
        return cache;
    }

    final static TermCache cache = new TermCache(256 * 1024, 4, false);
    final static TermCache cacheTemporal = new TermCache(192 * 1024, 4, false);

    protected Term compound(int dt, Term[] u, boolean intern) {
        return (intern && internable(dt, u)) ?
                (dt == DTERNAL ? cache : cacheTemporal).apply(new InternedCompound(this, dt, u)) :
                compound(dt, u);
    }

    /**
     * entry point into the term construction process
     */
    protected Term compound(int dt, Term[] u) {

        if (statement) {
            if (u.length == 1) { //similarity has been reduced
                assert (this == SIM);
                return u[0] == Null ? Null : True;
            } else {
                return statement(this, dt, u[0], u[1]);
            }
        } else {
            return instance(this, dt, u);
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


    /**
     * returns null if not found, and Null if no subterms remain after removal
     */
    @Nullable
    public static Term without(Term container, Predicate<Term> filter, Random rand) {


        Subterms cs = container.subterms();

        int i = cs.indexOf(filter, rand);
        if (i == -1)
            return null;


        switch (cs.subs()) {
            case 1:
                return Null; //removed itself
            case 2:
                return cs.sub(1 - i); //shortcut: return the other
            default:
                return container.op().the(container.dt(), cs.termsExcept(i));
        }

    }

    public static int conjEarlyLate(Term x, boolean earlyOrLate) {
        assert (x.op() == CONJ);
        int dt = x.dt();
        switch (dt) {
            case DTERNAL:
            case XTERNAL:
            case 0:
                return earlyOrLate ? 0 : 1;

            default: {
//                int d = x.sub(0).compareTo(x.sub(1));
//                if (d > 0)
//                    throw new RuntimeException();
//                if (dt < 0) earlyOrLate = !earlyOrLate;
//                return (d <= 0 ? (earlyOrLate ? 0 : 1) : (earlyOrLate ? 1 : 0));
                return (dt < 0) ? (earlyOrLate ? 1 : 0) : (earlyOrLate ? 0 : 1);
            }
        }
    }

    public boolean isAny(int bits) {
        return ((bit & bits) != 0);
    }

    public static Term conjEternalize(FasterList<LongObjectPair<Term>> events, int start, int end) {
        if (end - start == 1)
            return events.get(start).getTwo();
        else
            return CONJ.the(DTERNAL, Util.map(start, end, (i) -> events.get(i).getTwo(), Term[]::new));
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
        public boolean equalsNeg(Term t) {
            return false;
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
        public boolean equalsNeg(Term t) {
            return t == True;
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
        public boolean equalsNeg(Term t) {
            return t == False;
        }
        @Override
        public Term unneg() {
            return True;
        } //doesnt change
    }


    /**
     * flattening conjunction builder, for (commutive) multi-arg conjunction and disjunction (dt == 0 ar DTERNAL)
     * see: https://en.wikipedia.org/wiki/Boolean_algebra#Monotone_laws
     */
    /*@NotNull*/
    static Term junctionFlat(int dt, final Term[] u) {

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


//        ObjectByteHashMap<Term> s = new ObjectByteHashMap<>(u.length);
//
//        Term uu = flatten(CONJ, u, dt, s);
//        if (uu != null) {
//            assert (uu instanceof Bool);
//            return uu;
//        }
//
//        int os = s.size();
//        if (os == 0) {
//            return True;  //? does this happen
//        }
//
//        Set<Term> outer = os > 1 ? new HashSet(os) : null /* unnecessary for the one element case */;
//
//        for (ObjectBytePair<Term> xn : s.keyValuesView()) {
//            Term oi = xn.getOne().negIf(xn.getTwo() < 0);
//            if (os == 1)
//                return oi; //was the only element
//            else
//                outer.add(oi);
//        }
//
//
//
//        Term[] scs = sorted(outer);
//        if (scs.length == 1) {
//            return scs[0];
//        } else {
//            return instance(CONJ, dt, scs);
//        }

        ConjEvents c = new ConjEvents();
        long sdt = dt==DTERNAL ? ETERNAL : 0;
        for (Term x : u) {
            if (!c.add(x, sdt))
                break;
        }
        return c.term();
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


    final static class InternedCompound extends AbstractPLink<Term> implements HijackMemoize.Computation<InternedCompound, Term> {
        //X
        public final Op op;
        public final int dt;
        public Term[] subs;

        private final int hash;

        //Y
        public Term y = null;

        InternedCompound(Op o, int dt, Term... subs) {
            super();
            this.op = o;
            this.dt = dt;
            this.subs = subs;

            int h = Util.hashCombine(o.bit, dt);
            for (Term x : subs)
                h = Util.hashCombine(h, x.hashCode());

            this.hash = h;
        }

        @Override
        public Term get() {
            return y;
        }

        //        /**
//         * if accepted into the interner, it can call this with a resolver function to fully intern this
//         * by resolving the subterm values which are now present in the index
//         */
//        public void compact(Function<InternedCompound, Term> intern) {
////            for (int i = 0, subsLength = subs.length; i < subsLength; i++) {
////                Term x = subs[i];
////                if (x instanceof Compound) {
////                    Term y = intern.apply(key(x));
////                    if (y != null && y != x) {
////                        subs[i] = y;
////                    }
////                }
////            }
//        }

//        private InternedCompound key(Term x) {
//            return new InternedCompound(x.op(), x.subterms().arrayShared());
//        }


        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            InternedCompound p = (InternedCompound) obj;
            return hash == p.hash && op == p.op && dt == p.dt && Arrays.equals(subs, p.subs);
        }

        public float value() {
            return 0.5f;
//            float f;
//            switch (dt) {
//                case DTERNAL:
//                    f = 1f;
//                    break;
//                case XTERNAL:
//                case 0:
//                    f = 0.75f;
//                    break;
//                default:
//                    f = 0.25f;
//                    break;
//            }
//            return f;
            //return f / (1 + subs.length / 10f); //simple policy: prefer shorter
        }

        @Override
        public InternedCompound x() {
            return this;
        }

        public void set(Term y) {
            this.y = y;

            //HACK extended interning
            int n = subs.length;
            if (y != null && y.subs() == n) {
                if (n > 1) {
                    Subterms ys = y.subterms();
                    if (ys instanceof TermVector) {
                        Term[] yy = ys.arrayShared();
                        if (subs != yy && Arrays.equals(subs, yy)) {
                            subs = yy;
                        }
                    }
                } else if (n == 1) {
                    Term y0 = y.sub(0);
                    Term s0 = subs[0];
                    if (s0 != y0 && s0.equals(y0))
                        subs[0] = y0;
                }
            }
        }
    }


    public static class TermCache/*<I extends InternedCompound>*/ extends HijackMemoize<InternedCompound, Term> {

        public TermCache(int capacity, int reprobes, boolean soft) {
            super(x -> x.op.compound(x.dt, x.subs), capacity, reprobes, soft);
        }

        @Override
        public float value(InternedCompound x) {
            return DEFAULT_VALUE * x.value();
        }

        @Override
        public Computation<InternedCompound, Term> computation(InternedCompound xy, Term y) {
            xy.set(y);
            xy.priSet(value(xy));
            return xy;
        }

        //        @Override
//        protected void onIntern(InternedCompound x) {
//            x.compact(this::getIfPresent);
//        }

//        @Override
//        public void onRemove(Computation<InternedCompound, Term> x) {
//        }
    }
}
