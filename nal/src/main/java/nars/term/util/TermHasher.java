package nars.term.util;

import jcog.bloom.hash.DynBytesHasher;
import jcog.data.byt.DynBytes;
import nars.io.TermIO;
import nars.term.Term;

public class TermHasher extends DynBytesHasher<Term> {

	public TermHasher() {
		super(1024);
	}

	@Override
	protected void write(Term t, DynBytes d) {
		TermIO.the.write(t, d);
	}
}
