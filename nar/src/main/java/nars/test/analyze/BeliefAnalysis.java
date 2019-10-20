package nars.test.analyze;

import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.table.EmptyBeliefTable;
import nars.term.Term;
import nars.term.util.TermedDelegate;
import nars.time.Tense;
import nars.truth.TruthWave;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** utility class for analyzing the belief/goal state of a concept */
public class BeliefAnalysis implements TermedDelegate {

	public final Term term;
	public final NAR nar;

	public BeliefAnalysis(NAR n, Term term) {
		this.nar = n;
		this.term = term;
	}

	public BeliefAnalysis(@NotNull NAR n, @NotNull String term) throws Narsese.NarseseException {
        this( n, $.$(term));
	}

	@Override
	public Term term() {
		return term;
	}


	public @NotNull BeliefAnalysis goal(float freq, float conf) {
		nar.want(term, freq, conf);
		return this;
	}

	public @NotNull BeliefAnalysis believe(float freq, float conf) {
		nar.believe(term, freq, conf);
		return this;
	}

	public @NotNull BeliefAnalysis believe(float freq, float conf, @NotNull Tense present) {
		nar.believe(term, present, freq, conf);
		return this;
	}
	public @NotNull BeliefAnalysis believe(float pri, float freq, float conf, long when) {
		nar.believe(pri, term, when, freq, conf);
		return this;
	}

	public @Nullable TaskConcept concept() {
		return (TaskConcept) nar.concept(term);
	}

	public @Nullable BeliefTable beliefs() {
		Concept c = concept();
		if (c == null)
			return EmptyBeliefTable.Empty;
		return c.beliefs();
	}
	public @Nullable BeliefTable goals() {
		Concept c = concept();
		if (c == null)
			return EmptyBeliefTable.Empty;
		return c.goals();
	}

	public @NotNull TruthWave wave() {
		return new TruthWave(beliefs());
	}

	public @NotNull BeliefAnalysis run(int frames) {
		nar.run(frames);
		return this;
	}

	public void print() {
		print(true);
	}
	public void print(boolean beliefOrGoal) {
        BeliefTable table = table(beliefOrGoal);
		System.out.println((beliefOrGoal ? "Beliefs" : "Goals") + "[@" + nar.time() + "] " + table.taskCount());
		table.print(System.out);
		
	}

	public int size(boolean beliefOrGoal) {
		return table(beliefOrGoal).taskCount();
	}

	public @Nullable BeliefTable table(boolean beliefOrGoal) {
		return beliefOrGoal ? beliefs() : goals();
	}


//	/** sum of priorities of the belief table */
//	public float priSum() {
//		return (float) beliefs().taskStream().mapToDouble(Prioritized::priElseZero).sum();
//	}

	public @NotNull BeliefAnalysis input(boolean beliefOrGoal, float f, float c) {
		if (beliefOrGoal)
			believe(f, c);
		else
			goal(f, c);
		return this;
	}


	public long time() {
		return nar.time();
	}

}
