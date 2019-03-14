package nars.test.analyze;

import jcog.pri.Prioritized;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.term.TermedDelegate;
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


	@NotNull
	public BeliefAnalysis goal(float freq, float conf) {
		nar.want(term, freq, conf);
		return this;
	}

	@NotNull
	public BeliefAnalysis believe(float freq, float conf) {
		nar.believe(term, freq, conf);
		return this;
	}

	@NotNull
	public BeliefAnalysis believe(float freq, float conf, @NotNull Tense present) {
		nar.believe(term, present, freq, conf);
		return this;
	}
	@NotNull
	public BeliefAnalysis believe(float pri, float freq, float conf, long when) {
		nar.believe(pri, term, when, freq, conf);
		return this;
	}

	@Nullable
	public TaskConcept concept() {
		return (TaskConcept) nar.concept(term);
	}

	@Nullable
	public BeliefTable beliefs() {
		Concept c = concept();
		if (c == null)
			return BeliefTable.Empty;
		return c.beliefs();
	}
	@Nullable
	public BeliefTable goals() {
		Concept c = concept();
		if (c == null)
			return BeliefTable.Empty;
		return c.goals();
	}

	@NotNull
	public TruthWave wave() {
		return new TruthWave(beliefs());
	}

	@NotNull
	public BeliefAnalysis run(int frames) {
		nar.run(frames);
		return this;
	}

	public void print() {
		print(true);
	}
	public void print(boolean beliefOrGoal) {
		BeliefTable table = table(beliefOrGoal);
		System.out.println((beliefOrGoal ? "Beliefs" : "Goals") + "[@" + nar.time() + "] " + table.size());
		table.print(System.out);
		
	}

	public int size(boolean beliefOrGoal) {
		return table(beliefOrGoal).size();
	}

	@Nullable
	public BeliefTable table(boolean beliefOrGoal) {
		return beliefOrGoal ? beliefs() : goals();
	}


	/** sum of priorities of the belief table */
	public float priSum() {
		return (float) beliefs().streamTasks().mapToDouble(Prioritized::priElseZero).sum();
	}

	@NotNull
	public BeliefAnalysis input(boolean beliefOrGoal, float f, float c) {
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
