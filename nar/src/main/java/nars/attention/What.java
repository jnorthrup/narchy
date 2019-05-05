package nars.attention;

import jcog.TODO;
import jcog.math.IntRange;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.Prioritizable;
import jcog.pri.bag.Sampler;
import jcog.pri.bag.impl.PLinkArrayBag;
import jcog.pri.op.PriMerge;
import jcog.util.ConsumerX;
import nars.NAR;
import nars.Task;
import nars.attention.derive.DefaultDerivePri;
import nars.concept.Concept;
import nars.control.NARPart;
import nars.control.op.TaskEvent;
import nars.derive.Derivation;
import nars.derive.Premise;
import nars.exe.Exec;
import nars.link.TaskLink;
import nars.link.TaskLinks;
import nars.task.util.PriBuffer;
import nars.term.Term;
import nars.time.When;
import nars.time.event.WhenTimeIs;
import nars.time.part.DurLoop;
import nars.util.Timed;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 *  What?  an attention context described in terms of a prioritized distribution over a subset of Memory.
 *  a semi-self-contained context-specific memory.
 *
 *
 *  designed to be useful constructs for users, both at design and runtime,
 *  and at the same time accessible to any learned metaprogramming desire formed by the system itself.
 *
 *  attentions will receive some share of the mental burden in proportion
 *  to their relative prioritization.  this priority distribution of WHATs (consisting of priorities themselves)
 *  can form a product with the priority distribution of HOWs determining the overall
 *  runtime dynamics of a system.
 *
 *  the need for such bags can be illustrated in a chemistry analogy.  a NAR memory, undifferentiated, is like a
 *  reaction vessel (Memory) into which everything gets dumped.  YET the chemicals (Tasks) in this process are
 *  so reactive (combinatorically explosive) that only a small amount (> 1) of a few different chemical
 *  types (Operators) is needed to completely fill the vessel with garbage.
 *
 *  instead what we want is a laboratory with plenty of compartmentalized reaction vessels where chemicals (Tasks)
 *  can carefully be mixed in controlled ways to form reaction graphs determining the transfer of products
 *  from one to another, including the timing, quantities, backpressure and overflow conditions,
 *  filtration (selection queries), etc.
 *
 *  differentiated organs in biological systems function similarly in the above chemical analogy.
 *
 *
 *
 *  attentions are meant to be serializable, snapshottable, restoreable, live edited,
 *  filtered, cloned, etc.  they are like directories and their contents (ex: TaskLinks) are like files.
 *
 *  attentions share a memory of concepts (and thus their beliefs and questions) but only become
 *  "acquainted" with the content of another by receiving a reference to foreign "concepts".  this allows
 *  the formation of compartmentalized hierarchies of "mental organs" which hide and concentrate their localized
 *  work without fully contaminating the attention and resources of other parts.
 *
 *  the attention hierarchy is meant to be fully dynamic, adaptive, and metaprogrammable by the system at runtime
 *  through a minimal API.  thus Attention's are referred to by a Term so that operations upon them may
 *  be conceptualized and self-executed.
  */
abstract public class What extends NARPart implements Prioritizable, Sampler<TaskLink>, Iterable<TaskLink>, Externalizable, ConsumerX<Task>, Timed {

    public final PriNode pri;

    /** input bag */
    public final PriBuffer<Task> in;

    /** advised deriver pri model
     *      however, each deriver instance can also be configured individually and dynamically.
     * */
    public DerivePri derivePri =
            //new DirectDerivePri();
            new DefaultDerivePri();
            //new DefaultPuncWeightedDerivePri(); //<- extreme without disabling either pre or post amp

    final ConsumerX<Task> out = new ConsumerX<>() {

        @Override
        public int concurrency() {
            return What.this.concurrency();
        }

        @Deprecated /* HACK */ @Override public void accept(Task x) {
            Exec.run(x, What.this);
        }
    };


    protected What(Term id, PriBuffer<Task> in) {
        super(id);
        this.pri = new PriNode(this.id);
        this.in = in;

        add(new DurLoop.DurNARConsumer(
            !in.async(out) ? this::perceiveCommit : this::commit)
                .durs(0));
    }

    @Override
    public final int concurrency() {
        return nar.exe.concurrency();
        //return 1;
    }

    @Override
    public float pri() {
        return pri.asFloat();
    }

    @Override
    public float pri(float p) {
        return pri.pri(p);
    }

    /** perceive the next batch of input, for synchronously (cycle/duration/realtime/etc)
     *  triggered input buffers */
    private void perceive(NAR n) {
        in.commit(out, n);
    }

    private void perceiveCommit(NAR nar) {
        perceive(nar);
        commit(nar);
    }

    /** called periodically, ex: per duration, for maintenance such as gradual forgetting and merging new input.
     *  only one thread will be in this method at a time guarded by an atomic guard */
    abstract protected void commit(NAR nar);

    /** explicitly return the attention to a completely or otherwise reasonably quiescent state.
     *  how exactly can be decided by the implementation. */
    abstract public void clear();

    public final TaskLink sample() {
        return sample(nar.random());
    }

    public abstract Stream<Concept> concepts();

    public void emit(Task t) {
        TaskEvent.emit(t, nar);
    }

    /** cycles duration window */
    public abstract int dur();

    /* TODO other temporal focus parameters */

    @Override
    public final long time() {
        return nar.time();
    }

    @Override
    public final Random random() {
        return nar.random();
    }

    abstract public void hypothesize(int premisesPerIteration, int termlinksPerTaskLink, int matchTTL, int deriveTTL, Derivation d);




    /** proxies to another What */
    public static class ProxyWhat extends What {

        public final What what;

        public ProxyWhat(What what) {
            super(what.id, what.in);
            this.what = what;
        }

        @Override
        public int dur() {
            return what.dur();
        }

        @Override
        protected void commit(NAR nar) {
            what.commit(nar);
        }

        @Override
        public void clear() {
            what.clear();
        }

        @Override
        public Stream<Concept> concepts() {
            return what.concepts();
        }

        @Override
        public void writeExternal(ObjectOutput objectOutput) throws IOException {
            what.writeExternal(objectOutput);
        }

        @Override
        public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
            what.readExternal(objectInput);
        }

        @Override
        public Iterator<TaskLink> iterator() {
            return what.iterator();
        }

        @Override
        public void sample(Random rng, Function<? super TaskLink, SampleReaction> each) {
            what.sample(rng, each);
        }

        @Override
        public void hypothesize(int premisesPerIteration, int termlinksPerTaskLink, int matchTTL, int deriveTTL, Derivation d) {
            what.hypothesize(premisesPerIteration, termlinksPerTaskLink,matchTTL,deriveTTL,d);
        }
    }

    /** implements attention with a TaskLinks graph */
    public static class TaskLinkWhat extends What {

        /** in NAR time cycle units */
        public final IntRange dur = new IntRange(1, 1, 1000);

        public final TaskLinks links = new TaskLinks();

        public TaskLinkWhat(Term id, int capacity, PriBuffer<Task> in) {
            super(id, in);
            links.linksMax.set(capacity);
        }

        @Override
        protected void starting(NAR nar) {

            dur.set(nar.dur()); //initializes value

            super.starting(nar);
        }

        @Override
        public int dur() {
            return dur.intValue();
        }

        @Override
        protected void commit(NAR nar) {
            premises.commit(null);
            links.commit();
        }

        @Override
        public void clear() {
            links.clear();
        }

        @Override
        public Stream<Concept> concepts() {
            return links.concepts(nar);
        }

        @Override
        public final Iterator<TaskLink> iterator() {
            return links.iterator();
        }

        @Override
        public final void sample(Random rng, Function<? super TaskLink, SampleReaction> each) {
            links.sample(rng, each);
        }

        @Override
        public final void writeExternal(ObjectOutput objectOutput) {
            throw new TODO();
        }

        @Override
        public final void readExternal(ObjectInput objectInput) {
            throw new TODO();
        }

        /** TODO encapsulate as PremiseBuffer class */
        static final int premiseBufferCapacity = 32;
        float premiseSelectDecayRate = 0.5f;
        float fillFactor = 1f;
        final PLinkArrayBag<Premise> premises = new PLinkArrayBag<>(PriMerge.replace,premiseBufferCapacity);

        /**
         * samples premises
         * thread-safe, for use by multiple threads
         */
        public void hypothesize(int premisesPerIteration, int termlinksPerTaskLink, int matchTTL, int deriveTTL, Derivation d) {

            When<NAR> when = WhenTimeIs.now(d);


            this.sample(d.random, (int) Math.ceil(Math.max(1, ((float)premisesPerIteration)/termlinksPerTaskLink) * fillFactor), tasklink -> {

                Task task = tasklink.get(when);
                if (task != null && !task.isDeleted()) {
                    Term prevTerm = null;
                    for (int i = 0; i < termlinksPerTaskLink; i++) {
                        Term term = links.term(tasklink, task, d);
                        if (term != null && (prevTerm == null || !term.equals(prevTerm))) {
                            premises.put(new PLink<>(new Premise(task, term), tasklink.priPunc(task.punc())));
                        }
                        prevTerm = term;
                    }
                }

                return true;

            });

            for (int i = 0; i < premisesPerIteration; i++) {
                PriReference<Premise> pp = premises.sample(d.random);
                if (pp == null)
                    continue;

                pp.priMult(premiseSelectDecayRate);

                d.deriver.derive(pp.get(), d, matchTTL, deriveTTL);
            }

        }


    }

    @Override
    @Deprecated public final void accept(Task x) {
        in.put(x);
    }

    @Deprecated public final Task put(Task x) {
        return in.put(x);
    }


}
