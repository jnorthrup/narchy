package nars.term.anon;

import nars.Op;
import nars.Idempotent;
import nars.term.Neg;
import nars.term.Term;
import nars.term.atom.Atomic;

public abstract class IntrinAtomic extends Atomic implements Idempotent {
    /** meant to be a perfect hash among all normalized variables */
    public final short i;

	protected IntrinAtomic(Op type, byte num) {
        this(termToId(type, num));
    }

    protected IntrinAtomic(short id) {
        this.i = id;
    }

	static short termToId(Op o, byte id) {
		short group;

		switch (o) {
			case ATOM:
				group = Intrin.ANOMs;
				break;
			case VAR_DEP:
				group = Intrin.VARDEPs;
				break;
			case VAR_INDEP:
				group = Intrin.VARINDEPs;
				break;
			case VAR_QUERY:
				group = Intrin.VARQUERYs;
				break;
			case VAR_PATTERN:
				group = Intrin.VARPATTERNs;
				break;
			case IMG:
				group = Intrin.IMGs;
				break;

			default:
				throw new UnsupportedOperationException();
		}

		return (short)((group<<8) | id);
	}

	@Override
	public final short intrin() {
		return i;
	}

	@Override
	public final Term anon() {
		return this;
	}

	@Override
	public Term neg() {
		return new Neg.NegIntrin(this.i);
	}

	@Override
	public final int hashCode() {
		return i;
	}

	@Override
	public final boolean equals(Object x) {
		return x == this;
//                   ||
//              (obj instanceof AnonID) && id==((AnonID)obj).id;
	}

	@Override
	public final boolean equalsRoot(Term x) {
		return x == this;
	}

	@Deprecated public final byte id() {
		return (byte) (i & 0xff);
	}
}
