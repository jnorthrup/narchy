package nars.truth.func;

import nars.term.Term;
import nars.term.atom.Atomic;
import nars.truth.func.TruthFunction.RepolarizedTruth;
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
 *    DD		  - depolarize task & belief
 *    DP,DN	      - depolarize task
 *    PD,ND		  - depolarize belief
 *
 *    X           - task and belief arguments swapped.  this is applied to the above and will always appear as the final suffix modifier
 */
public class TruthModel {

	/** used during construction */
	private transient MutableMap<Term, TruthFunction> _table = new UnifiedMap<>(256);

	private final ImmutableMap<Term, TruthFunction> table;


	public TruthModel(NALTruth[] values) {
		add(values);
		this.table = _table.toImmutable();
		_table = null; //not needed any more
	}

	public final @Nullable TruthFunction get(Term a) {
		return table.get(a);
	}

	protected void add(TruthFunction... values) {
		for (TruthFunction t : values)
			add(t);
	}

	protected void add(TruthFunction t) {

		_add(t); //default, no modifiers
		_add(t, "PP"); //PP

		_add(new RepolarizedTruth(t, -1, +1, "N"));
		_add(new RepolarizedTruth(t, -1, +1, "NP"));

		_add(new RepolarizedTruth(t, 0, 0, "DD"));

		if (!t.single()) {

			_add(new RepolarizedTruth(t, +1, -1, "PN"));
			_add(new RepolarizedTruth(t, -1, -1, "NN"));

			_add(new RepolarizedTruth(t, +1, 0, "PD"));
			_add(new RepolarizedTruth(t, -1, 0, "ND"));
			_add(new RepolarizedTruth(t, 0, +1, "DP"));
			_add(new RepolarizedTruth(t, 0, -1, "DN"));
		}
	}

	protected void _add(TruthFunction t) {
		_add(t, "");
	}

	/** adds it and the swapped */
	protected void _add(TruthFunction t, String postfix) {
        String name = t + postfix;
		__add(name, t);
		__add(name + "X",
			t instanceof RepolarizedTruth ?  ((RepolarizedTruth)t).swapped() : new TruthFunction.SwappedTruth(t));
	}

	private void __add(String name, TruthFunction t) {
		_table.put(Atomic.the(name), t);
	}
}
