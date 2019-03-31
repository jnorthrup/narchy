package nars.term.atom;

import nars.Op;
import nars.The;
import nars.term.Term;

import static nars.Op.BOOL;


/** special/reserved/keyword representing fundamental absolute boolean truth states:
 *      True - absolutely true
 *      False - absolutely false
 *      Null - absolutely nonsense
 *
 *  these represent an intrinsic level of truth that exist within the context of
 *  an individual target.  not to be confused with Task-level Truth
 */
abstract public class Bool extends AbstractAtomic implements The {

    /**
     * absolutely nonsense
     */
    public static final Bool Null = new Bool(String.valueOf(Op.NullSym), ((byte)-1) ) {

        final int rankBoolNull = Term.opX(BOOL, (short) 0);

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
    };
    /**
     * tautological absolute false
     */
    public static final Bool False = new Bool("false", (byte)0) {

        final int rankBoolFalse = Term.opX(BOOL, (short) 1);

        @Override
        public final int opX() {
            return rankBoolFalse;
        }

        @Override
        public boolean equalsNeg(Term t) {
            return t == True;
        }

//        @Override
//        public boolean equalsNegRoot(Term t) {
//            return t == True;
//        }

        @Override
        public Term neg() {
            return True;
        }

        @Override
        public Term unneg() {
            return True;
        }
    };
    /**
     * tautological absolute true
     */
    public static final Bool True = new Bool("true", (byte)1) {

        final int rankBoolTrue = Term.opX(BOOL, (short) 2);

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

//        @Override
//        public boolean equalsNegRoot(Term t) {
//            return t == False;
//        }

        @Override
        public Term unneg() {
            return True;
        }
    };

    public static final Term[] Null_Array = new Term[] { Null };
    public static final Term[] False_Array = new Term[] { False };


    private final String label;

    protected Bool(String label, byte code) {
        super(new byte[] { BOOL.id, code } );
        this.label = label;
    }

//    @Override
//    abstract public boolean equalsNegRoot(Term t);



    @Override
    public final boolean equals(Object u) {
        return u == this;
    }

    @Override
    public /*@NotNull*/ Op op() {
        return BOOL;
    }

    @Override
    public String toString() {
        return label;
    }

    @Override
    abstract public Term unneg();


    @Override
    abstract public boolean equalsNeg(Term t);

    @Override
    public final Term concept() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Term root() {
        throw new UnsupportedOperationException();
    }
}
