package nars.term.atom;

import nars.term.anon.Intrin;

/** optimized intrinsic ASCII character Atoms */
public final class AtomChar extends Atom {
	static final nars.term.atom.AtomChar[] chars = new nars.term.atom.AtomChar[256];
	static {
		for (char i = 0; i < 256; i++) {
			chars[i] = new nars.term.atom.AtomChar(i);
		}
	}

	private final short intrin;

	public AtomChar(char c) {
		super(String.valueOf(c));
		this.intrin = (short)((Intrin.CHARs << 8) | c);
	}

	@Override
	public short intrin() {
		return intrin;
	}
}
