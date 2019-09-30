package nars.derive.action;

import jcog.WTF;
import nars.NAL;
import nars.Op;
import nars.Task;
import nars.attention.TaskLinkWhat;
import nars.derive.Derivation;
import nars.derive.adjacent.AdjacentConcepts;
import nars.derive.premise.AbstractPremise;
import nars.term.Neg;
import nars.term.Term;
import nars.unify.constraint.TermMatcher;
import nars.unify.constraint.VolumeCompare;

import static jcog.util.ArrayUtil.EMPTY_BYTE_ARRAY;

public class AdjacentLinks extends TaskAction {

	private final AdjacentConcepts adj;

	public AdjacentLinks(AdjacentConcepts adj) {

		this.adj = adj;


		taskPunc(true,true,true,true);

		constraints.add(new VolumeCompare(TheTask, TheBelief, false, -1).neg()); //belief <= task
		//bigger(TheTask, TheBelief); //belief < task

		//containsRecursively(TheTask,TheBelief);


		//belief term must be conceptualizable
		match(false, EMPTY_BYTE_ARRAY, new TermMatcher.Is(Op.Conceptualizable), true);
	}

	@Override
	protected void accept(Task y, Derivation d) {


		Term to = d._beliefTerm;

		if (!to.op().conceptualizable)
			return; //HACK the matcher isnt 100% in case of INT beliefTerm, since premiseKey erases it

		Task task = d._task;
		Term from = task.term();
		Term tgt = adj.adjacent(from.root(), to.root(), task.punc(), ((TaskLinkWhat)d.what).links, d);

		if (tgt != null) {
			assert(!(tgt instanceof Neg));
			assert (tgt.op().conceptualizable);
			if (NAL.DEBUG) {
				if (tgt.equals(from)) throw new WTF();
				if (tgt.equals(to)) throw new WTF();
			}

			//extra links: dont seem necessary
			//links.grow(link, link.from(), reverse, task.punc());

			d.add(new AbstractPremise(task, tgt));
		}

	}

	@Override
	public float pri(Derivation d) {
		//return 1;
		return (float) (0.5f/Math.pow(d.beliefTerm.volume(), 2));
	}
}
