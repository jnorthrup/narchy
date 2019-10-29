package nars.derive;

import nars.NAR;
import nars.derive.action.*;
import nars.derive.adjacent.AdjacentIndexer;
import nars.derive.premise.Premise;
import nars.derive.rule.DeriverProgram;
import nars.derive.rule.PremiseRuleSet;
import nars.derive.time.NonEternalTaskOccurenceOrPresentDeriverTiming;
import nars.derive.util.TimeFocus;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

/**
 * an individual deriver process: executes a particular Deriver model
 * specified by a set of premise rules.
 * <p>
 * runtime intensity is metered and throttled by causal feedback
 */
public class Deriver {

	public static final FloatFunction<Premise> sorter = DeriverExecutor::pri;

	public final DeriverProgram program;
	/**
	 * determines the temporal focus of (TODO tasklink and ) belief resolution to be matched during premise formation
	 * input: premise Task, premise belief target
	 * output: long[2] time interval
	 **/
	public TimeFocus timing;

	public Deriver(PremiseRuleSet rules) {
		this(rules, new NonEternalTaskOccurenceOrPresentDeriverTiming());
	}

	public Deriver(PremiseRuleSet rules, TimeFocus timing) {
		this(rules
				//standard derivation behaviors
				.add(TaskResolve.the)
				.add(MatchBelief.the)
				.add(QuestionAnswering.the)
				.add(new CompoundDecompose.One(true))
				.add(new CompoundDecompose.Two(true))
				.add(new AdjacentLinks(new AdjacentIndexer()))
				.add(new ImageUnfold())
				//TODO functor evaluator
				.compile()//.print()
			,
			timing);
	}


	public Deriver(DeriverProgram program, TimeFocus timing) {
		super();
		this.program = program;
		this.timing = timing;
		program.nar.exe.deriver(this); //HACK
	}

	public final NAR nar() { return program.nar; }

}

















































































































