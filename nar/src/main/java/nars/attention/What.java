package nars.attention;

import jcog.Paper;
import jcog.event.ByteTopic;
import jcog.event.Off;
import jcog.math.FloatRange;
import jcog.pri.bag.Sampler;
import jcog.util.ConsumerX;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.control.How;
import nars.control.PartBag;
import nars.control.PriNARPart;
import nars.control.op.Perceive;
import nars.derive.pri.DefaultDerivePri;
import nars.derive.pri.DerivePri;
import nars.link.TaskLink;
import nars.task.util.PriBuffer;
import nars.term.Term;
import nars.util.Timed;
import org.jetbrains.annotations.Nullable;

import java.io.Externalizable;
import java.io.PrintStream;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.lang.System.nanoTime;

/**
 * What?  an attention context described in terms of a prioritized distribution over a subset of Memory.
 * a semi-self-contained context-specific memory.
 * <p>
 * <p>
 * designed to be useful constructs for users, both at design and runtime,
 * and at the same time accessible to any learned metaprogramming desire formed by the system itself.
 * <p>
 * attentions will receive some share of the mental burden in proportion
 * to their relative prioritization.  this priority distribution of WHATs (consisting of priorities themselves)
 * can form a product with the priority distribution of HOWs determining the overall
 * runtime dynamics of a system.
 * <p>
 * the need for such bags can be illustrated in a chemistry analogy.  a NAR memory, undifferentiated, is like a
 * reaction vessel (Memory) into which everything gets dumped.  YET the chemicals (Tasks) in this process are
 * so reactive (combinatorically explosive) that only a small amount (> 1) of a few different chemical
 * types (Operators) is needed to completely fill the vessel with garbage.
 * <p>
 * instead what we want is a laboratory with plenty of compartmentalized reaction vessels where chemicals (Tasks)
 * can carefully be mixed in controlled ways to form reaction graphs determining the transfer of products
 * from one to another, including the timing, quantities, backpressure and overflow conditions,
 * filtration (selection queries), etc.
 * <p>
 * differentiated organs in biological systems function similarly in the above chemical analogy.
 * <p>
 * <p>
 * <p>
 * attentions are meant to be serializable, snapshottable, restoreable, live edited,
 * filtered, cloned, etc.  they are like directories and their contents (ex: TaskLinks) are like files.
 * <p>
 * attentions share a memory of concepts (and thus their beliefs and questions) but only become
 * "acquainted" with the content of another by receiving a reference to foreign "concepts".  this allows
 * the formation of compartmentalized hierarchies of "mental organs" which hide and concentrate their localized
 * work without fully contaminating the attention and resources of other parts.
 * <p>
 * the attention hierarchy is meant to be fully dynamic, adaptive, and metaprogrammable by the system at runtime
 * through a minimal API.  thus Attention's are referred to by a Term so that operations upon them may
 * be conceptualized and self-executed.
 */
@Paper
abstract public class What extends PriNARPart implements Sampler<TaskLink>, Iterable<TaskLink>, Externalizable, ConsumerX<Task>, Timed, BooleanSupplier {


	/**
	 * input bag
	 */
	public final PriBuffer<Task> in;
	public final ByteTopic<Task> eventTask = new ByteTopic<>(Op.Punctuation);
	/**
	 * present-moment perception duration, in global clock cycles,
	 * specific to this What, and freely adjustable
	 */
	public final FloatRange dur = new FloatRange(1, 1, 1024);
	public final FloatRange commitDurs = new FloatRange(1, 0.5f, 4);
	final ConsumerX<Task> out = new ConsumerX<>() {

		@Override
		public int concurrency() {
			return What.this.concurrency();
		}

		@Deprecated /* HACK */
		@Override
		public void accept(Task x) {
			Perceive.perceive(x, What.this);
		}
	};
	final AtomicLong nextUpdate = new AtomicLong(Long.MIN_VALUE);
	//new DefaultPuncWeightedDerivePri(); //<- extreme without disabling either pre or post amp
	//new DirectDerivePri();
	private final Consumer<Task> _in;
	public PartBag<How> how;
	/**
	 * advised deriver pri model
	 * however, each deriver instance can also be configured individually and dynamically.
	 */
	public DerivePri derivePri =
		new DefaultDerivePri();
	private float _durPhysical = 1;
	private transient long deadline = Long.MIN_VALUE;


	protected What(Term id, PriBuffer<Task> in) {
		super(id);
		this.in = in;
		this._in = in instanceof PriBuffer.DirectTaskBuffer ? this.out : in;
	}

	@Override
	protected void starting(NAR nar) {
		how = nar.how; //new PartBag<>(NAL.HOWS_CAPACITY)
		super.starting(nar);
		_durPhysical = nar.dur();
		in.start(out, nar);
	}

	@Override
	protected void stopping(NAR nar) {
		in.stop();
		super.stopping(nar);
	}

	/**
	 * perceptual duration (cycles)
	 */
	@Override
	public float dur() {
		return dur.floatValue();
	}

	/**
	 * internal physiological duration
	 */
	public float durPhysical() {
		return _durPhysical;
	}

	@Override
	public final int concurrency() {
		return nar.exe.concurrency();
		//return 1;
	}

	/**
	 * called periodically, ex: per duration, for maintenance such as gradual forgetting and merging new input.
	 * only one thread will be in this method at a time guarded by an atomic guard
	 */
	abstract protected void commit(NAR nar);

	/**
	 * explicitly return the attention to a completely or otherwise reasonably quiescent state.
	 * how exactly can be decided by the implementation.
	 */
	abstract public void clear();



	/* TODO other temporal focus parameters */

	public final TaskLink sample() {
		return sample(random());
	}

	public abstract Stream<Concept> concepts();

	public void emit(Task t) {
		eventTask.emit(t, t.punc());
	}

	@Override
	public final long time() {
		return nar.time();
	}

	@Override
	public final Random random() {
		return nar.random();
	}

	@Override
	public final void accept(Task x) {
		_in.accept(x);
	}

	public abstract void link(TaskLink t);

	public final TaskLink link(Task task) {
		return link(task, task.pri());
	}

	public abstract TaskLink link(Task t, float pri);

	public final Off onTask(Consumer<Task> listener, byte... punctuations) {
		return eventTask.on(listener, punctuations);
	}

	public void printPerf(PrintStream out) {
		for (How h : how) {
			h.printPerf(out);
		}
	}

	public void nextSynch() {
		tryCommit();
		how.forEach(h -> h.next(this, () -> false));
	}

	@Deprecated
	public final void next(long _start, long useNS) {

        long stop = _start + useNS;

        int n = how.size();
        long howNS = useNS / n; //TODO refine

        tryCommit();

		Random r = random();
		long now;

		int runs = 0;

		do {
			@Nullable How h = how.sample(r);
			long start = nanoTime();
			if (h.isOn()) {


				deadline = start + howNS;
				try {
					h.next(this, this);
					runs++;
				} catch (Throwable t) {
					logger.error("{} {}", t, this);
					//t.printStackTrace();
				}

				now = nanoTime();
				h.use(howNS, now - start);
			} else
				now = start;

		} while (now < stop);

		//System.out.println(this.id + " runs=" + runs + " total=" + Texts.timeStr(useNS) + " each=" + Texts.timeStr(howNS));

		//long end = System.nanoTime();
		//use(estTime, end - start);
	}

	@Override
	public final boolean getAsBoolean() {
		return nanoTime() < deadline;
	}


	private boolean tryCommit() {
		long nextUpdate = this.nextUpdate.getOpaque();
		long now = time();
		if (now > nextUpdate) {
			long nextNextUpdate = now + (long) Math.ceil(_durPhysical * commitDurs.floatValue());
			if (this.nextUpdate.compareAndSet(nextUpdate, nextNextUpdate)) {

				_durPhysical = nar.dur();
				commit(nar);
				return true;
			}
		}
		return false;
	}
}
