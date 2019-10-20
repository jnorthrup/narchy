package nars.term.atom;

import jcog.Skill;
import nars.Op;
import nars.term.Term;

import static nars.Op.BOOL;


/** special/reserved/keyword representing fundamental absolute boolean truth states:
 *      True - absolutely true
 *      False - absolutely false
 *      Null - absolutely nonsense
 *
 *  these represent an intrinsic level of truth that exist within the context of
 *  an individual target.  not to be confused with Task-level Truth
 *
 *  Implements "Unknown-state logic" (https://en.wikipedia.org/wiki/Ternary_computer)
 */
@Skill("Ternary_computer")
public abstract class theBool extends Keyword {

    /**
     * absolutely nonsense
     */
    public static final theBool Null = new theBool(String.valueOf(Op.NullSym), ((byte)-1) ) {

        @Override
        public Term neg() {
            return this;
        }

        @Override
        public boolean equalsNeg(Term t) {
            return false;
        }

    };
    /**
     * tautological absolute false
     */
    public static final theBool False = new theBool("false", (byte)0) {

        @Override
        public Term neg() {
            return True;
        }

        @Override
        public Term unneg() {
            return True;
        }


        @Override
        public boolean equalsNeg(Term t) {
            return t == True;
        }
    };
    /**
     * tautological absolute true
     */
    public static final theBool True = new theBool("true", (byte)1) {

        @Override
        public Term neg() {
            return False;
        }

        @Override
        public boolean equalsNeg(Term t) {
            return t == False;
        }

    };

    public static final Term[] Null_Array = { Null };
    public static final Term[] False_Array = { False };


    private theBool(String label, byte code) {
        super(BOOL, label, new byte[] { BOOL.id, code });
    }


    @Override
    public abstract boolean equalsNeg(Term t);


}
