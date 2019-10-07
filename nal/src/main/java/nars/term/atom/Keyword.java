package nars.term.atom;

import jcog.Util;
import nars.Op;
import nars.The;
import nars.term.Term;

/** special atoms which are universally singleton */
public abstract class Keyword extends Atomic implements The {
	protected final String label;
	protected final byte[] bytes;
	protected final int hash;
	private final Op op;
	private final int opID;

	public Keyword(Op op, String label, byte[] bytes) {
		this.op = op;
		this.opID = op.id;
		this.label = label;
		this.bytes = bytes;
		this.hash = Util.hash(bytes);
	}

	@Override
	public final int opID() {
		return opID;
	}

	@Override
	public final Op op() {
		return op;
	}

	@Override
	public final byte[] bytes() {
		return bytes;
	}

	@Override
	public final int hashCode() {
		return hash;
	}

	@Override
	public final boolean equals(Object u) {
		return u == this;
	}

	@Override
	public final String toString() {
		return label;
	}

	@Override
	public final Term concept() {
		throw new UnsupportedOperationException();
	}
}
