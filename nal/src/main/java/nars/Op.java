package nars;


import jcog.Paper;
import jcog.Skill;
import nars.subterm.ArrayTermVector;
import nars.subterm.DisposableTermList;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.util.SetSectDiff;
import nars.term.util.TermException;
import nars.term.util.builder.HeapTermBuilder;
import nars.term.util.builder.TermBuilder;
import nars.term.util.builder.TermConstructor;
import nars.term.util.conj.Conj;
import nars.term.var.Img;
import nars.term.var.UnnormalizedVariable;
import nars.time.Tense;
import org.apache.lucene.util.MathUtil;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.impl.factory.Maps;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.term.atom.Bool.True;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * NAL symbol table
 */
public enum Op {


    ATOM(".", Op.ANY_LEVEL, Args.Zero),

    NEG("--", 1, Args.One) {
        public Term the(Term u) {
            return Neg.neg(u);
        }

        public Term the(TermBuilder b, int dt, Term[] u) {

            if (u.length != 1)
                throw new TermException("negation requires one subterm", NEG, dt, u);
            if (dt != DTERNAL)
                throw new TermException("negation has no temporality", NEG, dt, u);

            return the(u[0]);
        }

    },

    INH("-->", 1, Args.Two) {
        @Override
        public Term the(TermBuilder b, int dt, Term[] u) {
            return b.statement(this, dt, u);
        }
    },
    SIM("<->", true, 2, Args.Two) {
        @Override
        public Term the(TermBuilder b, int dt, Term[] u) {
            return b.statement(this, dt, u);
        }
    },

//    /**
//     * extensional intersection
//     */
//    @Deprecated SECTe("&", true, 3, Args.GTETwo) {
//        @Override
//        public Term the(TermBuilder b, int dt, Term[] u) {
//            return CONJ.the(b, dt, u);
//            //throw new WTF();
//            //return SetSectDiff.intersect(b, SECTe, u);
//        }
//    },
//
//    /**
//     * intensional intersection
//     */
//    @Deprecated SECTi("|", true, 3, Args.GTETwo) {
//        @Override
//        public Term the(TermBuilder b, int dt, Term[] u) {
//            return CONJ.the(b, dt, $.neg(u)).neg(); //DISJ
//            //throw new WTF();
//            //return SetSectDiff.intersect(b, SECTi, u);
//        }
//    },


    /**
     * PRODUCT
     * classically this is considered NAL4 but due to the use of functors
     * it is much more convenient to classify it in NAL1 so that it
     * along with inheritance (INH), which comprise the functor,
     * can be used to compose the foundation of the system.
     */
    PROD("*", 1, Args.GTEZero),


    /**
     * conjunction
     *   &&   parallel                (a && b)          <= (b && a)
     *   &&+  sequence forward        (a &&+1 b)
     *   &&-  sequence reverse        (a &&-1 b)        =>      (b &&+1 a)
     *   &&+- variable                (a &&+- b)        <=      (b &&+- a)
     *   &|   joined at the start     (x &| (a &&+1 b)) => ((a&&x) &&+1 b)
     *   |&   joined at the end       (x |& (a &&+1 b)) => (     a &&+1 (b&&x))   //TODO
     *
     * disjunction
     *   ||   parallel                (a || b)          => --(--a &&   --b)
     *   ||+- variable                (a ||+- b)        => --(--a &&+- --b)
     */
    CONJ("&&", true, 5, Args.GTETwo) {
        @Override
        public Term the(TermBuilder b, int dt, Term[] u) {
            return b.conj(dt, u);
        }
    },


    /**
     * intensional setAt
     */
    SETi("[", true, 2, Args.GTEOne) {

        @Override
        public Term the(TermBuilder b, int dt, Term... u) {
            return SetSectDiff.intersectSet(b, SETi, u);
        }

        //        @Override
//        public final Term the(TermBuilder b, int dt, Collection<Term> sub) {
//            return b.theCompound(this, dt, Terms.sorted(sub)); //already sorted
//        }
    },

    /**
     * extensional setAt
     */
    SETe("{", true, 2, Args.GTEOne) {

        @Override
        public Term the(TermBuilder b, int dt, Term... u) {
            return SetSectDiff.intersectSet(b, SETe, u);
        }

//        @Override
//        public final Term the(TermBuilder b, int dt, Collection<Term> sub) {
//            return b.theCompound(this, dt, Terms.sorted(sub)); //already sorted
//        }
    },


    /**
     * implication
     */
    IMPL("==>", 5, Args.Two) {
        @Override
        public Term the(TermBuilder b, int dt, Term... u) {
            return b.statement(this, dt, u);
        }
    },


    /**
     * $ most specific, least globbing
     */
    VAR_PATTERN('%', Op.ANY_LEVEL),
    VAR_QUERY('?', Op.ANY_LEVEL),
    VAR_INDEP('$', 5),
    VAR_DEP('#', 5),




    /**
     * % least specific, most globbing
     */

    INT("+", Op.ANY_LEVEL),

    BOOL("B", Op.ANY_LEVEL),

    IMG("/", 4),

    /**
     * used for direct term/subterm construction.  supporting ellipsis and other macro transforms.
     * functions like a PROD
     */
    FRAG("`", Op.ANY_LEVEL, Args.GTEZero)


    /**
     * for ellipsis, when seen as a target
     */

    ;


    /**
     * does this help?  Op.values() bytecode = INVOKESTATIC
     * but accessing this is GETSTATIC
     */
    private static final Op[] ops = Op.values();

    public static final String DISJstr = "||";
    public static final int StatementBits = Op.or(Op.INH, Op.SIM, Op.IMPL);
    public static final int FuncBits = Op.or(Op.ATOM, Op.INH, Op.PROD);
    public static final int FuncInnerBits = Op.or(Op.ATOM, Op.PROD);
    public static final byte BELIEF = '.';
    public static final byte QUESTION = '?';

    /** https://en.wikipedia.org/wiki/Is%E2%80%93ought_problem
     *  "But how exactly can an "ought" be derived from an "is"?"
     * */
    @Paper
    public static final byte GOAL = '!';

    public static final byte QUEST = '@';
    public static final byte COMMAND = ';';
    public static final byte[] Punctuation = new byte[]{BELIEF, QUESTION, GOAL, QUEST, COMMAND};
//    public static final String TENSE_PAST = ":\\:";
//    public static final String TENSE_PRESENT = ":|:";
//    public static final String TENSE_FUTURE = ":/:";
//    public static final String TENSE_ETERNAL = ":-:";
//    public static final String TASK_RULE_FWD = "|-";
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
    public static final int varBits = Op.or(VAR_PATTERN, VAR_DEP, VAR_QUERY, VAR_INDEP);

    public final static char VarAutoSym = '_';

    /** anonymous depvar */
    @Skill("Prolog") public static final Atomic VarAuto =
        new UnnormalizedVariable(Op.VAR_DEP, String.valueOf(VarAutoSym));

    public static final char NullSym = '☢';

    public static final char imIntSym = '\\';
    public static final char imExtSym = '/';

    public static final int AtomicConstant = Op.ATOM.bit | Op.INT.bit | Op.BOOL.bit;

    public static final Img ImgInt = new Img((byte) '\\');
    public static final Img ImgExt = new Img((byte) '/');
    public static final int Set = or(Op.SETe, Op.SETi);

    /**
     * events are defined as the non-conjunction sub-components of conjunctions, or the target itself if it is not a conj
     */
    public static final int Temporal = or(Op.CONJ, Op.IMPL);
    public static final int Variable = or(Op.VAR_PATTERN, Op.VAR_INDEP, Op.VAR_DEP, Op.VAR_QUERY);

    public static final Atom Belief = (Atom) Atomic.the(String.valueOf((char) BELIEF));
    public static final Atom Goal = (Atom) Atomic.the(String.valueOf((char) GOAL));
    public static final Atom Question = (Atom) Atomic.the(String.valueOf((char) QUESTION));
    public static final Atom Quest = (Atom) Atomic.the(String.valueOf((char) QUEST));
    public static final Atom Que = (Atom) Atomic.the(String.valueOf((char) QUESTION) + (char) QUEST);

    public static final Term[] EmptyTermArray = new Term[0];
    public static final Subterms EmptySubterms = new ArrayTermVector(EmptyTermArray);
    public static final Compound EmptyProduct = TermBuilder.newCompound(Op.PROD, EmptySubterms);
    private static final int[] NALLevelEqualAndAbove = new int[8 + 1];


    public static final ImmutableMap<String, Op> stringToOperator;



    /**
     * specifier for any NAL level
     */
    private static final int ANY_LEVEL = 0;



    /** re-initialized in NAL */
    public static TermBuilder terms = HeapTermBuilder.the;


    /**
     * True wrapped in a subterm as the only element
     */
    public static final Subterms TrueSubterm = HeapTermBuilder.the.subterms(True);

    /**
     * False wrapped in a subterm as the only element
     */
    public static final Subterms FalseSubterm = HeapTermBuilder.the.subterms(Bool.False);

    public static final Compound[] EmptyCompoundArray = new Compound[0];


    static {
        for (Op o : Op.values()) {
            int l = o.minLevel;
            if (l < 0) l = 0;
            for (int i = l; i <= 8; i++)
                NALLevelEqualAndAbove[i] |= o.bit;
        }

        final Map<String, Op> _stringToOperator = new HashMap<>(values().length * 2);


        for (Op r : Op.values()) {
            _stringToOperator.put(r.toString(), r);

        }
        stringToOperator = Maps.immutable.ofMap(_stringToOperator);


    }

    public final Atom strAtom;
    public final boolean indepVarParent;
    public final boolean depVarParent;
    /**
     * whether it is a special or atomic target that isnt conceptualizable.
     * negation is an exception to this, being unconceptualizable itself
     * but it will have conceptualizable=true.
     */
    public final boolean conceptualizable, taskable;
    public final boolean beliefable, goalable;
    /**
     * string representation
     */
    public final String str;
    /**
     * character representation if symbol has length 1; else ch = 0
     */
    public final char ch;

    /**
     * arity limits, range is inclusive >= <=
     * TODO replace with an IntPredicate
     */
    public final int minSubs, maxSubs;
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

    /*
    used only by Termlike.hasAny
    public static boolean hasAny(int existing, int possiblyIncluded) {
        return (existing & possiblyIncluded) != 0;
    }*/
    public final byte id;

    /** whether the target of this op is valid, by tiself, as an event or condition */
    public boolean eventable;
    public boolean set;


    Op(char c, int minLevel) {
        this(c, minLevel, Args.Zero);
    }


    Op(char c, int minLevel, IntIntPair size) {
        this(Character.toString(c), minLevel, size);
    }


    Op(String string, int minLevel) {
        this(string, false /* non-commutive */, minLevel, Args.Zero);
    }

    Op(String string, int minLevel, IntIntPair size) {
        this(string, false /* non-commutive */, minLevel, size);
    }

    Op(String string, boolean commutative, int minLevel, IntIntPair size) {

        this.id = (byte) (ordinal());
        this.str = string;
        this.ch = string.length() == 1 ? string.charAt(0) : 0;
        this.strAtom = ch != '.' ? (Atom) Atomic.the('"' + str + '"') : null /* dont compute for ATOM, infinite loops */;

        this.commutative = commutative;
        this.minLevel = minLevel;


        this.minSubs = size.getOne();
        this.maxSubs = size.getTwo();

        this.var = java.util.Set.of("$", "#", "?", "%").contains(str);

        boolean isImpl = str.equals("==>");
        this.statement = str.equals("-->") || isImpl || str.equals("<->");
        boolean isConj = str.equals("&&");
        this.temporal = isConj || isImpl;


        this.hasNumeric = temporal;


        this.bit = (1 << ordinal());

        this.atomic = var || java.util.Set.of(".", "+", "B", "/").contains(str);

        boolean isBool = str.equals("B");
        boolean isInt = str.equals("+");
        boolean isNeg = str.equals("--");
        boolean isImg = str.equals("/");
        boolean isSect = str.equals("|") || str.equals("&");
        boolean isFrag = str.equals("`");

        conceptualizable =
                !var &&
                        !isBool &&
                        !isImg &&
                        !isFrag &&
                        (!isInt || NAL.term.INT_CONCEPTUALIZABLE)
        //!isNeg && //<- HACK technically NEG cant be conceptualized but in many cases this is assumed. so NEG must not be included in conceptualizable for it to work currently
        ;

        taskable = conceptualizable && !isInt && !isNeg && !isSect;

        eventable = taskable || isNeg || var;

        beliefable = taskable;
        goalable = taskable && !isImpl;

        indepVarParent = isImpl;
        depVarParent = isConj;

        set = str.equals("{") || str.equals("[");
    }



    public static boolean hasAny(int existing, int possiblyIncluded) {
        return (existing & possiblyIncluded) != 0;
    }

    public static boolean hasAll(int existing, int possiblyIncluded) {
        return ((existing | possiblyIncluded) == existing);
    }

//    public static boolean isTrueOrFalse(Term x) {
//        return x == Bool.True || x == Bool.False;
//    }


//    public static boolean hasNull(Term[] t) {
//        for (Term x : t)
//            if (x == Bool.Null)
//                return true;
//        return false;
//    }


//    static boolean in(int needle, int haystack) {
//        return (needle & haystack) == needle;
//    }

    public static int or(/*@NotNull*/ Op... o) {
        int bits = 0;
        for (Op n : o)
            bits |= n.bit;
        return bits;
    }


    public static final Predicate<Term> statementLoopyContainer = (x) -> x.op()!=PROD;

    public static boolean statementLoopy(Term x, Term y) {
        if (!(x instanceof Atomic) && !(y instanceof Atomic))
            return false;

//        boolean xByComponnet = x instanceof Compound && x.op()==CONJ;
//        boolean yByComponent = y instanceof Compound && y.op()==CONJ;
//        if (!xByComponnet && !yByComponent) {
            return _statementLoopy(x, y);
//        } else if (xByComponnet && !yByComponent) {
//            return x.subterms().ORwith(Op::_statementLoopy, y);
//        } else if (yByComponent && !xByComponnet) {
//            return y.subterms().ORwith(Op::_statementLoopy, x);
//        } else {
//            if (x.volume() >= y.volume())
//                return x.subterms().ORwith((xx,Y) -> Y.subterms().ORwith(Op::_statementLoopy, xx), y);
//            else
//                return y.subterms().ORwith((yy,X) -> X.subterms().ORwith(Op::_statementLoopy, yy), x);
//        }

    }
    private static boolean _statementLoopy(Term x, Term y) {

        int xv = x.volume(), yv = y.volume();
        boolean root = false;
        if (xv == yv) {
            return x.equals(y);
            //probably impossible:
//            boolean z = Term.commonStructure(x, y) &&
//                    (x.containsRecursively(y, root, delim) || y.containsRecursively(x, root, delim));
//            if (z)
//                throw new WTF();
//            return z;
        } else if (xv > yv)
            return x.containsRecursively(y, root, statementLoopyContainer);
        else
            return y.containsRecursively(x, root, statementLoopyContainer);
    }


    @Nullable
    public static Op the(String s) {
        return stringToOperator.get(s);
    }

    public static Object theIfPresent(String s) {
        //HACK TEMPORARY
        if (s.equals("&")) return CONJ;

        Op x = stringToOperator.get(s);
        return x != null ? x : s;
    }


    /**
     * encodes a structure vector as a human-readable target.
     * if only one bit is set then the Op's strAtom is used instead of the binary
     * representation.
     * TODO make an inverse decoder
     */
    public static Term strucTerm(int struct) {
        int bits = Integer.bitCount(struct);
        switch (bits) {
            case 0:
                throw new UnsupportedOperationException("no bits");
            case 1: {
                Op op = Op.the(MathUtil.log(Integer.highestOneBit(struct), 2));
                return op.strAtom;
            }
            default: {
                return $.quote(Integer.toBinaryString(struct)/*.substring(Op.ops.length)*/);
            }
        }
    }

    public static boolean has(int haystack, int needle, boolean allOrAny) {
        return allOrAny ? Op.hasAll(haystack, needle) : Op.hasAny(haystack, needle);
    }

    public static Term DISJ(int dt, Term... x) {
        return DISJ(Op.terms, dt, x);
    }

    public static Term DISJ(TermBuilder b, Term... x) {
        return DISJ(b, DTERNAL, x);
    }

    /** build disjunction (consisting of negated conjunction of the negated subterms, ie. de morgan's boolean law ) */
    public static Term DISJ(TermBuilder b, int dt, Term... x) {
        switch (x.length) {
            case 0:
                return True;
            case 1:
                return x[0];
            default:
                Term[] xx = x.clone();
                $.neg(xx);
                return CONJ.the(b, dt, xx).neg();
        }
    }

    public static Term DISJ(Term... x) {
        return DISJ(Op.terms, DTERNAL, x);
    }

    public static int unique() {
        return ops.length;
    }

    public static Stream<Op> all() {
        return Stream.of(ops);
    }


    public final Term the(Subterms s) {
        return the(DTERNAL, s);
    }

    public final Term the(int dt, Subterms s) {
        return the(terms, dt, s);
    }

    public final Term the(TermBuilder b, int dt, Subterms s) {
        return the(b, dt, s instanceof DisposableTermList ? ((TermList)s).arrayKeep() : s.arrayShared());
    }

    public final Term the(/*@NotNull*/ Term... u) {
        return the(DTERNAL, u);
    }

    public Term the(/*@NotNull*/ Term onlySubterm) {
        return the(DTERNAL, onlySubterm);
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

            String s;
            switch (this) {
                case CONJ:
                    s = ("&|");
                    break;
                case IMPL:
                    s = ("=|>");
                    break;
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

            if (dt > 0)
                w.append('+');

            if (dt == XTERNAL)
                w.append('-');
            else
                w.append(Integer.toString(dt));

            w.append(' ');

        }
    }

    public final Term[] sortedIfNecessary(int dt, Term[] u) {
        return commutative && u.length > 1 && Conj.concurrentInternal(dt) ? Terms.commute(u) : u;
    }
    public final Subterms sortedIfNecessary(int dt, Subterms u) {
        return commutative && Conj.concurrentInternal(dt) && u.subs() > 1 ? u.commuted() : u;
    }

    public final Term the(/*@NotNull*/ Collection<Term> sub) {
        return the(DTERNAL, sub);
    }

    public final Term the(TermBuilder b, Collection<Term> sub) {
        return the(b, DTERNAL, sub);
    }

    public Term the(TermBuilder b, int dt, /*@NotNull*/ Collection<Term> sub) {
        return the(b, dt, sub.toArray(EmptyTermArray));
    }

    public final Term the(int dt, /*@NotNull*/ Collection<Term> sub) {
        return the(terms, dt, sub.toArray(EmptyTermArray));
    }

    /**
     * alternate method args order for 2-target w/ infix DT
     */
    public final Term the(Term a, int dt, Term b) {
        return the(dt, a, b);
    }

    /**
     * entry point into the target construction process.
     * this call tree eventually ends by either:
     * - instance(..)
     * - reduction to another target or True/False/Null
     */
    public final Term the(int dt, Term... u) {
        return the(terms,dt, u);
    }

    public final Term the(TermBuilder b, Term... u) {
        return the(b, DTERNAL, u);
    }

    public Term the(TermBuilder b, int dt, Term... u) {
        return compound(b, this, dt, u);
    }

    public static Term compound(TermConstructor b, Op o, int dt, Subterms u) {
        return compound(b, o, dt, u.arrayShared());
    }

    public static Term compound(Op o, int dt, Subterms u) {
        return compound(o, dt, u.arrayShared());
    }

    public static Op the(int id) {
        return ops[id];
    }

    /**
     * direct constructor
     * no reductions or validations applied
     * use with caution
     */
    public static Term compound(Op o, int dt, Term... u) {
        return compound(terms, o, dt, u);
    }

    public static Term compound(TermConstructor b, Op o, Term... u) {
        return compound(b, o, DTERNAL, u);
    }

    public static Term compound(TermConstructor b, Op o, int dt, Term... u) {
        return b.compound(o, dt, u);
    }

    public static Term compound(Op o, Term... u) {
        return compound(o, DTERNAL, u);
    }

    public boolean isAny(int bits) {
        return ((bit & bits) != 0);
    }

    enum Args {
        ;
        static final IntIntPair Zero = pair(0, 0);
        static final IntIntPair One = pair(1, 1);
        static final IntIntPair Two = pair(2, 2);

        static final IntIntPair GTEZero = pair(0, NAL.term.SUBTERMS_MAX);
        static final IntIntPair GTEOne = pair(1, NAL.term.SUBTERMS_MAX);
        static final IntIntPair GTETwo = pair(2, NAL.term.SUBTERMS_MAX);

    }

    public static class InvalidPunctuationException extends RuntimeException {
        public InvalidPunctuationException(byte c) {
            super("Invalid punctuation: " + c);
        }
    }


    @Deprecated public static final String DIFFe = "~";
    public static final String DIFFi = "-";
}
