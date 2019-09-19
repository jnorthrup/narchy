package nars.derive.action;

import nars.Op;
import nars.Task;
import nars.attention.TaskLinkWhat;
import nars.derive.Derivation;
import nars.derive.adjacent.AdjacentConcepts;
import nars.derive.premise.AbstractPremise;
import nars.term.Term;
import nars.unify.constraint.TermMatcher;

import static jcog.util.ArrayUtil.EMPTY_BYTE_ARRAY;

public class AdjacentLinks extends TaskAction {

	private final AdjacentConcepts adj;

	public AdjacentLinks(AdjacentConcepts adj) {

		this.adj = adj;

		//belief term must be conceptualizable
		taskPunc(true,true,true,true);
		match(false, EMPTY_BYTE_ARRAY, new TermMatcher.Is(Op.Conceptualizable), true);
	}

	@Override
	protected void accept(Task y, Derivation d) {

		Term from = d._task.term();
		Term to = d._beliefTerm.root();

		if (!to.op().conceptualizable)
			return; //HACK the matcher isnt 100% in case of INT beliefTerm, since premiseKey erases it


		Task task = d._task;

		Term reverse = adj.adjacent(from, to, task.punc(), ((TaskLinkWhat)d.what).links, d);

		if (reverse != null) {
			assert (!reverse.equals(from));
			assert (reverse.op().conceptualizable);

			//extra links: dont seem necessary
			//links.grow(link, link.from(), reverse, task.punc());

			d.add(new AbstractPremise(task, reverse));
		}

	}

	@Override
	protected float pri(Derivation d) {
		//return 2;
		return (float) (0.5f/Math.pow(d.beliefTerm.volume(), 2));
	}
}
