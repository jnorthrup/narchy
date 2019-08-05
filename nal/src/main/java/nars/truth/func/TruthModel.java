package nars.truth.func;

import nars.term.Term;
import nars.term.atom.Atomic;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

/**
 * manages permuting variants of the truth function terms,
 * indicated by attached suffix modifiers:
 *
 *    PP          - default; alias for default un-negated argument form*
 *    NP          - negate task truth
 *    N           - shorthand for NP when belief truth is irrelevant or assumed true
 *    PN          - negate belief truth
 *    NN          - negate task and belief truth
 *    Depolarized - automatically un-negates any negative truth inputs (freq < 0.5) (deprecated)
 *    DD		  - depolarize task & belief
 *    DP,DN	      - depolarize task
 *    PD,ND		  - depolarize belief
 *
 *    X           - task and belief arguments swapped.  this is applied to the above and will always appear as the final suffix modifier
 */
public class TruthModel {

	/** used during construction */
	transient private MutableMap<Term, TruthFunction> _table = new UnifiedMap<>(256);

	private final ImmutableMap<Term, TruthFunction> table;


	public TruthModel(NALTruth[] values) {
		add(values);
		this.table = _table.toImmutable();
		_table = null; //not needed any more
	}

	@Nullable
	public final TruthFunction get(Term a) {
		return table.get(a);
	}

	protected void add(TruthFunction... values) {
		for (TruthFunction t : values)
			add(t);
	}

	protected void add(TruthFunction t) {

		_add(t); //default, no modifiers
		_add(t, "PP"); //PP

		_add(new TruthFunction.RepolarizedTruth(t, -1, +1, "N"));
		_add(new TruthFunction.RepolarizedTruth(t, -1, +1, "NP"));

		if (!t.single()) {

			_add(new TruthFunction.RepolarizedTruth(t, +1, -1, "PN"));
			_add(new TruthFunction.RepolarizedTruth(t, -1, -1, "NN"));

			_add(new TruthFunction.RepolarizedTruth(t, +1, 0, "PD"));
			_add(new TruthFunction.RepolarizedTruth(t, -1, 0, "ND"));
			_add(new TruthFunction.RepolarizedTruth(t, 0, +1, "DP"));
			_add(new TruthFunction.RepolarizedTruth(t, 0, -1, "DN"));
		}

		_add(new TruthFunction.RepolarizedTruth(t, 0, 0, "DD"));
		_add(new TruthFunction.RepolarizedTruth(t, 0, 0, "Depolarized")); //deprecated


	}

	protected void _add(TruthFunction t) {
		_add(t, "");
	}

	/** adds it and the swapped */
	protected void _add(TruthFunction t, String postfix) {
		String name = t + postfix;
		__add(name, t);
		__add(name + "X", new TruthFunction.SwappedTruth(t));
	}

	private void __add(String name, TruthFunction t) {
		_table.put(Atomic.the(name), t);
	}
}
