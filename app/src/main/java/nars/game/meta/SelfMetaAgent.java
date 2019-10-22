package nars.game.meta;

import jcog.Util;
import jcog.math.FloatNormalized;
import jcog.thing.Part;
import nars.$;
import nars.Emotion;
import nars.NAR;
import nars.Op;
import nars.control.MetaGoal;
import nars.derive.pri.DefaultPuncWeightedDerivePri;
import nars.game.Game;
import nars.game.GameTime;
import nars.game.action.GoalActionConcept;
import nars.term.Term;

import static nars.$.$$;

/**
 * core metavisor
 */
public class SelfMetaAgent extends MetaAgent {

	public SelfMetaAgent(NAR nar) {
		this(nar, 2 /* nyquist sampling rate - to ensure activity is accurately sampled in the feedback stats */);
	}

	public SelfMetaAgent(NAR nar, float durs) {
		super($.inh(nar.self(), $$("meta")), GameTime.durs(durs), nar);
		NAR n = nar;

		Term SELF = n.self();


		Emotion e = n.emotion;

		sense($.inh(SELF, $$("busy")), new FloatNormalized(e.busyVol));
		sense($.inh(SELF, $$("premiseRun")), new FloatNormalized(e.premiseRun));
		sense($.inh(SELF, $$("deriveTask")), new FloatNormalized(e.deriveTask));
		sense($.inh(SELF, $$("lag")), new FloatNormalized(e.durLoopLag));

		DefaultPuncWeightedDerivePri dPri = new DefaultPuncWeightedDerivePri();
		actionUnipolar($.inh(SELF,$.p("derive", "complexity")), (float v)->dPri.simplicityImportance.set(1-v));
		actionUnipolar($.inh(SELF,$.p("derive", "polarize")), (float v)->dPri.polarityImportance.set(v));
		actionUnipolar($.inh(SELF,$.p("derive", belief)), (float v)->dPri.beliefPri = Util.lerp(v, 0.1f, 1f));
		actionUnipolar($.inh(SELF,$.p("derive", goal)), (float v)->dPri.goalPri = Util.lerp(v, 0.1f, 1f));
		actionUnipolar($.inh(SELF,$.p("derive", question)), (float v)->dPri.questionPri = Util.lerp(v, 0.1f, 1f));
		actionUnipolar($.inh(SELF,$.p("derive", quest)), (float v)->dPri.questPri = Util.lerp(v, 0.1f, 1f));
		for (Op o : Op.values()) {
			if (o.taskable)
				actionUnipolar($.inh(SELF, $.p("derive", $.quote(o.str))), (float v) -> dPri.opPri[o.id] = Util.lerp(v, 0.1f, 1f));
		}

		for (MetaGoal mg : MetaGoal.values()) {
			GoalActionConcept a = actionUnipolar($.inh(SELF, $.the(mg.name())), (x) -> {
				nar.emotion.want(mg,
					x >= 0.5f ?
						(float) Util.lerp(Math.pow((x - 0.5f) * 2, 1 /* 2 */), 0, +1) //positive (0.5..1)
						:
						Util.lerp((x) * 2, -0.02f, 0) //negative (0..0.5): weaker
				);
			});
//                a.resolution(0.1f);
		}

//        float maxPri = Math.max(n.beliefPriDefault.amp.floatValue(), n.goalPriDefault.amp.floatValue());
//        float dynamic = 10; //ratio max to min pri
//        actionCtl($.inh(SELF, beliefPri), n.beliefPriDefault.amp.subRange(maxPri/dynamic, maxPri));
//        actionCtl($.inh(SELF, goalPri), n.goalPriDefault.amp.subRange(maxPri/dynamic, maxPri));

		actionUnipolar($.inh(SELF, precise), (x) -> {
			float x1 = x;
			float y;
			if (x1 >= 0.75f) {
				x1 = 0.01f;
				y = 1;
			} else if (x1 >= 0.5f) {
				x1 = 0.05f;
				y = 0.66f;
			} else if (x1 >= 0.25f) {
				x1 = 0.10f;
				y = 0.33f;
			} else {
				x1 = 0.20f;
				y = 0;
			}
			nar.freqResolution.set(x1);
			return y;
		});

//			actionUnipolar($.inh(SELF, careful), (x) -> {
//				float x1 = x;
//				float y;
//				if (x1 >= 0.75f) {
//					x1 = 0.01f;
//					y = 1f;
//				} else if (x1 >= 0.5f) {
//					x1 = 0.02f;
//					y = 0.66f;
//				} else if (x1 >= 0.25f) {
//					x1 = 0.03f;
//					y = 0.33f;
//				} else {
//					x1 = 0.04f;
//					y = 0;
//				}
//				nar.confResolution.set(x1);
//				return y;
//			});

//			//potentially dangerous, may become unable to convince itself to un-ignore things
//			actionUnipolar($.inh(SELF, ignore), (x) -> {
//				float y;
//				if (x >= 0.75f) {
//					x = 0.01f;
//					y = (1f+0.75f)/2;
//				} else if (x >= 0.5f) {
//					x = 0.005f;
//					y = (0.75f+0.5f)/2;
//				} else if (x >= 0.25f) {
//					x = 0.001f;
//					y = (0.5f+0.25f)/2;
//				} else {
//					x = 0.0f;
//					y = 0.25f/2;
//				}
//				nar.confResolution.set(x);
//				return y;
//			});

		//top-level priority controls of other NAR components
		nar.what.stream()
			.peek(w -> w.derivePri = dPri)
			.filter(w -> w != this.what()).forEach(w -> priAction(w.pri));


		//shouldnt be necessary, manipulate the downstream PriNodes instead and leave these constant
//			priAction($.inh(SELF, $.p(belief, pri)), nar.beliefPriDefault);
//			priAction($.inh(SELF, $.p(goal, pri)), nar.goalPriDefault);

		//actionCtl($.inh(SELF, $.p(belief,conf)), nar.beliefConfDefault);
		//actionCtl($.inh(SELF, $.p(goal,conf)), nar.goalConfDefault);

//                .subRange(
//                Math.max(nar.goalPriDefault.amp() /* current value */ * priFactorMin, ScalarValue.EPSILON),
//                nar.goalPriDefault.amp() /* current value */ * priFactorMax)::setProportionally);

		float durMeasured = 0;

		reward($.inh(SELF, $.p(happy, now)), () -> {
			return happiness(nowPercept.start, nowPercept.end, durMeasured, nar);
		});

		/** past happiness ~= gradient momentum / echo effect */
		float emotionalMomentumDurs = 4;
		reward($.inh(SELF, $.p(happy, past)), () -> {
			return happiness(Math.round(nowPercept.start - dur() * emotionalMomentumDurs), nowPercept.start, durMeasured, nar);
		});

		/** optimism */
		reward($.inh(SELF, $.p(happy, future)), () -> {
			return happiness(nowPercept.end, Math.round(nowPercept.end + dur() * emotionalMomentumDurs), durMeasured, nar);
		});

//        ThreadCPUTimeTracker.getCPUTime()
//        reward("lazy", 1, ()->{
//            return 1-nar.loop.throttle.floatValue();
//        });
	}

	float happiness(long start, long end, float dur, NAR nar) {
		return (float) nar.parts(Game.class)
			.filter(g -> g != SelfMetaAgent.this)
			.filter(Part::isOn)
			.mapToDouble(g ->
				(((g.happiness(start, end, dur) - 0.5f) * 2f
					* g.what().pri() //weighted by current priority
				) / 2f) + 0.5f
			).average().orElse(0);
	}
}
