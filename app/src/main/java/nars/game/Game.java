package nars.game;

import jcog.*;
import jcog.data.list.FastCoWList;
import jcog.event.ListTopic;
import jcog.event.Off;
import jcog.event.Topic;
import jcog.math.FloatClamped;
import jcog.math.FloatNormalized;
import jcog.math.FloatSupplier;
import jcog.thing.Part;
import jcog.util.ArrayUtil;
import nars.$;
import nars.NAR;
import nars.attention.PriNode;
import nars.attention.PriSource;
import nars.attention.What;
import nars.control.NARPart;
import nars.exe.NARLoop;
import nars.game.action.ActionSignal;
import nars.game.action.BiPolarAction;
import nars.game.sensor.GameLoop;
import nars.game.sensor.Signal;
import nars.game.sensor.UniSignal;
import nars.game.sensor.VectorSensor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.time.When;
import nars.truth.Truth;
import nars.util.Timed;
import org.slf4j.Logger;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;

/**
 * an integration of sensor concepts and motor functions
 * interfacing with an environment forming a sensori-motor loop.
 * <p>
 * these include all forms of problems including
 * optimization
 * reinforcement learning
 * etc
 * <p>
 * the name 'Game' is used, in the most general sense of the
 * word 'game'.
 */
@Paper
@Skill({"Game_studies", "Game_theory"})
public class Game extends NARPart /* TODO extends ProxyWhat -> .. and commit when it updates */ implements NSense, NAct, Timed, NARLoop.Pausing {

	static final Atom GAME = Atomic.atom(Game.class.getSimpleName().toLowerCase());
	private static final Logger logger = Log.logger(Game.class);
	private static final Atom ACTION = Atomic.atom("action");
	private static final Atom SENSOR = Atomic.atom("sensor");
	private static final Atom REWARD = Atomic.atom("reward");
	public final GameTime time;
	public final AtomicBoolean trace = new AtomicBoolean(false);
	public final FastCoWList<GameLoop> sensors = new FastCoWList<>(GameLoop[]::new);
	public final FastCoWList<ActionSignal> actions = new FastCoWList<>(ActionSignal[]::new);
	public final FastCoWList<Reward> rewards = new FastCoWList<>(Reward[]::new);
	public final AtomicInteger iteration = new AtomicInteger(0);
	public final Term id;
	public final When<What> nowPercept = new When();
	public final When<What> nowLoop = new When();

	private final Topic<NAR> eventFrame = new ListTopic();
	private final AtomicBoolean busy = new AtomicBoolean(false);
	private final NAgentCycle cycle =

		Cycles.Interleaved;
	public PriSource rewardPri;
	public PriSource actionPri;
	public PriSource sensorPri;
	public volatile long now = ETERNAL;
	public float confDefaultBelief;
	/**
	 * the context representing the experience of the game
	 */
	@Deprecated
	private What what;
	private transient float _freqRes = Float.NaN;
	private transient float _confRes = Float.NaN;


	public Game(String id) {
		this(id, GameTime.durs(1));
	}

	public Game(String id, GameTime time) {
		this($.$$(id), time);
	}

	/**
	 * TODO make final
	 */
	@Deprecated
	public Game(Term id, GameTime time, NAR n) {
		this(id, time);
		this.nar = n;


	}

	public Game(Term id, GameTime time) {
		super(env(id));

		this.id = id;
		this.time = time;

		add(time.clock(this));
	}

	static Term env(Term x) {
		return $.func(GAME, x);
	}

	public static FloatSupplier normalize(FloatSupplier rewardFunc, float min, float max) {
		return new FloatClamped(new FloatNormalized(rewardFunc, min, max, false), 0, 1);
	}

	@Override
	public final What what() {
		return what;
	}

	/**
	 * dexterity = mean(conf(action))
	 * evidence/confidence in action decisions, current measurement
	 */
	public double dexterity() {
		int a = actions.size();
		double result = a == 0 ? 0 : actions.sumBy(ActionSignal::dexterity);
		return a > 0 ? result / a : 0;
	}

	public double coherency() {
		int a = actions.size();
		double result = a == 0 ? 0 : actions.sumBy(ActionSignal::coherency);
		return a > 0 ? result / a : 0;
	}

	public final double happiness() {
		return happiness(nowPercept.start, nowPercept.end, dur());
	}

	/**
	 * avg reward satisfaction, current measurement
	 * happiness metric applied to all reward concepts
	 */
	@Paper
	public final double happiness(long start, long end, float dur) {
		return rewards.meanBy(rr -> rr.happiness(start, end, dur));
	}

	@Override
	public final <A extends ActionSignal> A addAction(A c) {
		Term ct = c.term();
		if (actions.OR(e -> e.term().equals(ct)))
			throw new RuntimeException("action exists with the ID: " + ct);
		actions.add(c);
		return c;
	}

	@Override
	public final <S extends GameLoop> S addSensor(S s) {
		Term st = s.term();
		if (sensors.OR(e -> e.term().equals(st)))
			throw new RuntimeException("sensor exists with the ID: " + st);

		sensors.add(s);
		return s;
	}

	private void init(PriNode target, Object s) {

		if (s instanceof VectorSensor) {
			nar.control.input(((VectorSensor) s).pri, target);
		} else if (s instanceof Signal) {

			if (s instanceof UniSignal)
				nar.control.input(((UniSignal) s).pri, target);
			else
				throw new TODO();

		} else if (s instanceof Reward) {
			nar.control.input(((Reward) s).pri, target);
			((Reward) s).init(this);
		} else if (s instanceof BiPolarAction)
			nar.control.input(((BiPolarAction) s).pri, target);
		else if (s instanceof PriNode)
			nar.control.input(((PriNode) s), target);
		else
			throw new TODO();

		if (s instanceof NARPart)
			nar.add(((NARPart) s));

		if (s instanceof ActionSignal)
			nar.add((ActionSignal) s);
	}

	public Random random() {
		What w = this.what;
		return w != null ? w.random() : ThreadLocalRandom.current();
	}

	@Override
	public long time() {
		return now;
	}

	public String summary() {
		return id +
			" dex=" + /*n4*/(dexterity()) +
			" hapy=" + /*n4*/(happiness()) +

			/*" var=" + n4(varPct(nar)) + */ '\t' + nar.memory.summary() + ' ' +
			nar.emotion.summary();
	}

	/**
	 * registers sensor, action, and reward concepts with the NAR
	 * TODO call this in the constructor
	 */

	protected void starting(NAR nar) {
		super.starting(nar);

		this.now = nar.time();

		this.what = nar.fork(id, true);

		nowPercept.the(what);
		nowLoop.the(what);
		nowPercept.end = (int) (now - what.dur() / 2);

		init();

		nar.control.add(actionPri = new PriSource($.inh(id, ACTION),
			nar.goalPriDefault.pri()));
		nar.control.add(sensorPri = new PriSource($.inh(id, SENSOR),
			nar.beliefPriDefault.pri()));
		nar.control.add(rewardPri = new PriSource($.inh(id, REWARD),
			Util.or(nar.beliefPriDefault.pri(), nar.goalPriDefault.pri())));

		for (GameLoop s : sensors) {
			init(sensorPri, s);
		}
		for (ActionSignal a : actions) {
			init(actionPri, a);
		}
		for (Reward r : rewards) {
			init(rewardPri, r);
		}
	}

	/**
	 * subclasses can safely add sensors, actions, rewards by implementing this.  NAR and What will be initialized prior
	 */
	protected void init() {

	}

	protected void stopping(NAR nar) {
		sensors.stream().filter(x -> x instanceof NARPart).forEach(s -> nar.remove((NARPart) s));

		nar.control.removeAll(actionPri, sensorPri, rewardPri);
		actionPri = null;
		rewardPri = null;
		sensorPri = null;

		this.what.close();
		this.what = null;

		this.nar = null;
	}

	public Reward reward(FloatSupplier rewardfunc) {
		return reward(rewardTerm("happy"), 1, rewardfunc);
	}

	public SimpleReward reward(String reward, FloatSupplier rewardFunc) {
		return reward(reward, 1f, rewardFunc);
	}

	public SimpleReward reward(Term reward, FloatSupplier rewardFunc) {
		return reward(reward, 1f, rewardFunc);
	}

	public SimpleReward reward(String reward, float freq, FloatSupplier rewardFunc) {
		return reward(rewardTerm(reward), freq, rewardFunc);
	}

	/**
	 * default reward target builder from String
	 */
	protected Term rewardTerm(String reward) {

		return $.inh(id, reward);
	}

	public Reward rewardNormalized(String reward, float min, float max, FloatSupplier rewardFunc) {
		return rewardNormalized(reward, 1, min, max, rewardFunc);
	}

	public Reward rewardNormalized(String reward, float freq, float min, float max, FloatSupplier rewardFunc) {
		return rewardNormalized(rewardTerm(reward), freq, min, max, rewardFunc);
	}

	public SimpleReward reward(Term reward, float freq, FloatSupplier rewardFunc) {
		SimpleReward r = new SimpleReward(reward, freq, rewardFunc, this);
		reward(r);
		return r;
	}

	/**
	 * set a default (bi-polar) reward supplier
	 */
	public Reward rewardNormalized(Term reward, float freq, float min, float max, FloatSupplier rewardFunc) {

		return reward(reward, freq, normalize(rewardFunc, min, max));
	}

	/**
	 * default reward module
	 */
	public final synchronized Reward reward(Reward r) {
		if (rewards.OR(e -> e.term().equals(r.term())))
			throw new RuntimeException("reward exists with the ID: " + r.term());

		rewards.add(r);
		return r;
	}

	/**
	 * perceptual duration
	 */
	public final float dur() {
		return nowPercept.dur;
	}

	/**
	 * physical/sensory duration
	 */
	public final float durLoop() {
		return time.dur();
	}

	public final float ditherFreq(float f, float res) {
		return Truth.freqSafe(f, Math.max(res, _freqRes));
	}

	public final float ditherConf(float c) {
		return (float) Truth.confSafe(c, _confRes);
	}

	public final void pri(float v) {
		what.pri(v);
	}

	@Override
	public synchronized void pause(boolean pause) {
		time.pause(pause);
	}

	/**
	 * iteration
	 */
	protected final void next() {

		if (!isOn() || !busy.compareAndSet(false, true))
			return;


		try {

			long now =
				nar.time();

			long prev = this.now;
			if (now < prev)
				return;

			time.next(this.now = now);

			float durLoop = durLoop();
			long lastEnd = nowPercept.end;
			long nextStart = Math.max(lastEnd + 1, (long) Math.floor(now - durLoop / 2));
			long nextEnd = Math.max(nextStart, Math.round(Math.ceil(now + durLoop / 2 - 1)));

			nowPercept.range(nextStart, nextEnd).dur(what.dur());
			nowLoop.range(nextStart, nextEnd).dur(durLoop);

			this.confDefaultBelief = nar.confDefault(BELIEF);
			_freqRes = nar.freqResolution.floatValue();
			_confRes = nar.confResolution.floatValue();

			cycle.next(this, iteration.getAndIncrement(), prev, now);

			if (trace.getOpaque())
				logger.info(summary());

		} finally {
			busy.set(false);
		}

	}

	@Override
	public final NAR nar() {
		return nar;
	}

	protected void act() {
		ActionSignal[] aaa = actions.array().clone();
		ArrayUtil.shuffle(aaa, random());

		for (ActionSignal a : aaa)
			update(a);
	}

	protected void sense() {
		sensors.forEach(this::update);
		rewards.forEach(this::update);
	}

	private void update(GameLoop s) {
		if (s instanceof Part) {
			if (!((Part) s).isOn())
				return;
		}
		s.accept(this);
	}

	private void nextFrame() {
		eventFrame.emit(nar);
	}

	public final int iterationCount() {
		return iteration.getOpaque();
	}

	public Off onFrame(Consumer/*<NAR>*/ each) {
		return eventFrame.on(each);
	}

	public Off onFrame(Runnable each) {
		return eventFrame.on((x) -> each.run());
	}

	public FastCoWList<ActionSignal> actions() {
		return actions;
	}

	public enum Cycles implements NAgentCycle {

		/**
		 * original, timing needs analyzed more carefully
		 */
		Interleaved() {
			@Override
			public void next(Game a, int iteration, long prev, long now) {
				a.sense();
				a.act();
				a.nextFrame();
			}

		},

		/**
		 * iterations occurr in read then act pairs
		 */
		Biphasic() {
			@Override
			public void next(Game a, int iteration, long prev, long now) {


				switch (iteration % 2) {
					case 0:

						a.sense();
						break;
					case 1:

						a.act();
						a.nextFrame();
						break;
				}
			}
		}
	}


	@FunctionalInterface
	public interface NAgentCycle {
		/**
		 * in each iteration,
		 * responsible for invoking some or all of the following agent operations, and
		 * supplying their necessary time bounds:
		 * <p>
		 * a.frame() - executes attached per-frame event handlers
		 * <p>
		 * a.reinforce(...) - inputs 'always' tasks
		 * <p>
		 * a.sense(...) - reads sensors
		 * <p>
		 * a.act(...) - reads/invokes goals and feedback
		 */
		void next(Game a, int iteration, long prev, long now);
	}
}