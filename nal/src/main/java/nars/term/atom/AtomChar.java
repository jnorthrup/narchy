package nars.term.atom;

import nars.term.anon.Intrin;

/** optimized intrinsic ASCII character Atoms */
public final class AtomChar extends Atom {
	static final nars.term.atom.AtomChar[] chars = new nars.term.atom.AtomChar[256];
	static {
		for (char i = (char) 0; (int) i < 256; i++) {
			chars[(int) i] = new nars.term.atom.AtomChar(i);
		}
	}

	private final short intrin;

	public AtomChar(char c) {
		super(String.valueOf(c));
		this.intrin = (short)(((int) Intrin.CHARs << 8) | (int) c);
	}

	@Override
	public short intrin() {
		return intrin;
	}
}
