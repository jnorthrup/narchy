package nars.game.meta;

import jcog.Util;
import jcog.pri.ScalarValue;
import nars.$;
import nars.NAR;
import nars.attention.TaskLinkWhat;
import nars.attention.What;
import nars.game.Game;
import nars.game.sensor.AbstractSensor;
import nars.game.sensor.GameLoop;
import nars.game.sensor.VectorSensor;
import nars.task.util.PriBuffer;
import nars.term.Term;
import nars.time.ScheduledTask;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;

import static nars.$.$$;

public class GameMetaAgent extends MetaAgent {

	/**
	 * in case it forgets to unpause
	 */
	private static final long autoResumePeriod = 256;

	public GameMetaAgent(Game g, boolean allowPause) {
		super($.inh(g.what().id, $$("meta")), g.time.chain(2 /* nyquist */), g.nar);

		What w = g.what();

		Term gid = w.id; //$.p(w.nar.self(), w.id);
		//this.what().accept(new EternalTask($.inh(aid,this.id), BELIEF, $.t(1f, 0.9f), nar));


		//actionCtlPriNodeRecursive(g.sensorPri, g.nar.control.graph);
		priAction(g.sensorPri);
		priAction(g.rewardPri);
		priAction(g.actionPri);


		floatAction($.inh(gid, forget), ((TaskLinkWhat) w).links.decay);
		floatAction($.inh(gid, grow), ((TaskLinkWhat) w).links.grow);
		//actionCtl($.inh(gid, remember), ((TaskLinkWhat) w).links.sustain);

		//actionCtl($.inh(gid, amplify), ((TaskLinkWhat) w).links.amp);


//        float priMin = 0.1f, priMax = 1;
//        actionCtl($.inh(gid, PRI), w.priAsFloatRange());

//            float curiMin = 0.005f, curiMax = 0.05f;
//            actionCtl($.inh(gid, CURIOSITY), g.curiosity.rate.subRange(curiMin, curiMax));

		float initialDur = w.dur();
//			FloatRange durRange = new FloatRange(initialDur, Math.max(nar.dtDither(), initialDur / 4), initialDur * 16) {
//				@Override
//				public float get() {
//					super.set(((TaskLinkWhat) w).dur);
//					return super.get();
//				}
//
//				@Override
//				public void set(float value) {
//					super.set(value);
//					value = super.get();
//					float nextDur = Math.max(1, value);
//					//logger.info("{} dur={}" , w.id, nextDur);
//					((TaskLinkWhat) w).dur.set(nextDur);
//					//assert(nar.dur()==nextDur);
//				}
//			};


		actionUnipolar($.inh(gid, duration), (x) -> {
			float ditherDT = nar.dtDither();
			float nextDur =
				//(float) Math.pow(initialDur, Util.sqr((x + 0.5f)*2));
				//ditherDT + initialDur * (float) Math.pow(2, ((x - 0.5f)*2));
				(float) (ditherDT + Util.lerp(Math.pow(x, 4), 0, initialDur * 16));
			//System.out.println(x + " dur=" + nextDur);
			((TaskLinkWhat) w).dur.set(Math.max(1, nextDur));
		});

		if (w.inBuffer instanceof PriBuffer.BagTaskBuffer)
			floatAction($.inh(gid, input), ((PriBuffer.BagTaskBuffer) (w.inBuffer)).valve);


		reward($.inh(gid, happy), () -> {
			return g.isOn() ? (float) g.happiness(nowPercept.start, nowPercept.end, 0) : Float.NaN;
		});


		rewardNormalized($.inh(gid, dex), 1, 0, ScalarValue.EPSILON,
			() -> {
////            float p = a.proficiency();
////            float hp = Util.or(h, p);
//            //System.out.println(h + " " + p + " -> " + hp);
////            return hp;
				return g.isOn() ? (float) g.dexterity() : Float.NaN;
			});

		for (GameLoop s : g.sensors) {
			if (s instanceof VectorSensor)
				floatAction($.inh(((AbstractSensor) s).id, pri), ((VectorSensor) s).pri.amp);
		}


		//TODO other Emotion sensors

//        Term agentPriTerm =
//                $.inh(a.id, PRI);
//                //$.inh(a.id, id /* self */);
//        GoalActionConcept agentPri = actionUnipolar(agentPriTerm, (FloatConsumer)a.attn.factor::set);
//
//


		if (allowPause) {
			float playThresh = 0.25f;
			actionPushButton($.inh(gid, MetaAgent.play), new BooleanProcedure() {

				private volatile int autoResumeID = 0;
				private volatile ScheduledTask autoResume;
				private volatile Runnable resume = null;

				@Override
				public void value(boolean e) {

					//enableAction = n.actionToggle($.func(enable, n.id), (e)->{
					//TODO integrate and threshold, pause for limited time
					synchronized (this) {
						if (e) {
							tryResume();
						} else {
							tryPause();
						}
					}

				}

				void tryPause() {

					if (resume == null) {

						resume = g.pause();
						NAR n = nar();

						int a = autoResumeID;
						autoResume = n.runAt(Math.round(n.time() + autoResumePeriod * n.dur()), () -> {
							if (autoResumeID == a)
								tryResume();
							//else this one has been cancelled
						});

					}
				}

				void tryResume() {

					if (resume != null) {
						autoResumeID++;
						resume.run();
						autoResume = null;
						resume = null;
					}

				}
			}, () -> playThresh);
		}


//        Reward enableReward = reward("enable", () -> enabled.getOpaque() ? +1 : 0f);

	}
}
