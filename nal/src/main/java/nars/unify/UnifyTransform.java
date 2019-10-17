package nars.unify;

import nars.Op;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/** unify + transform */
public class UnifyTransform extends SubUnify {

	private boolean strict;

	public UnifyTransform(Random rng) {
		super(rng, Op.Variable);
	}

	@Override
	public Unify clear() {
		this.strict = false;
		this.result = this.transformed = null;
		return super.clear();
	}

	public UnifyTransform clear(int varBits, boolean strict) {
		clear(varBits);
		this.strict = strict;
		return this;
	}

	@Override
	protected boolean accept(Term result) {
		return !strict || !result.equals(transformed);
	}

	@Nullable
	Term unifySubst(Term x, Term y, @Nullable Term transformed) {
		this.transformed = transformed;
		this.result = null;

		unify(x, y);

		return result;
	}

	public @Nullable Term unifySubst(Term x, Term y, Term transformed, int var, boolean strict) {
		clear(var, strict);
		return unifySubst(x, y, transformed);
	}
}
