package nars;


import com.google.common.collect.Streams;
import com.google.common.primitives.Longs;
import jcog.*;
import jcog.data.byt.DynBytes;
import jcog.event.ByteTopic;
import jcog.event.ListTopic;
import jcog.event.Off;
import jcog.event.Topic;
import jcog.exe.Cycled;
import jcog.func.TriConsumer;
import jcog.math.MutableInteger;
import jcog.pri.Prioritized;
import jcog.thing.Part;
import nars.Narsese.NarseseException;
import nars.attention.What;
import nars.concept.Concept;
import nars.concept.NodeConcept;
import nars.concept.Operator;
import nars.concept.PermanentConcept;
import nars.concept.util.ConceptBuilder;
import nars.control.*;
import nars.control.channel.CauseChannel;
import nars.eval.Evaluation;
import nars.eval.Evaluator;
import nars.eval.Facts;
import nars.exe.Exec;
import nars.exe.NARLoop;
import nars.io.IO;
import nars.memory.Memory;
import nars.subterm.Subterms;
import nars.table.BeliefTable;
import nars.table.BeliefTables;
import nars.table.TaskTable;
import nars.task.NALTask;
import nars.task.util.TaskException;
import nars.term.*;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.util.TermException;
import nars.time.ScheduledTask;
import nars.time.Tense;
import nars.time.Time;
import nars.time.event.WhenClear;
import nars.time.event.WhenCycle;
import nars.time.event.WhenInternal;
import nars.time.event.WhenTimeIs;
import nars.time.part.DurLoop;
import nars.time.part.DurNARConsumer;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.HdrHistogram.Histogram;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static nars.$.*;
import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static org.fusesource.jansi.Ansi.ansi;


/**
 * Non-Axiomatic Reasoner
 * <p>
 * Instances of this represent a reasoner connected to a Memory, and set of Input and Output channels.
 * <p>
 * All state is contained within   A NAR is responsible for managing I/O channels and executing
 * memory operations.  It executes a series sof cycles in two possible modes:
 * * step mode - controlled by an outside system, such as during debugging or testing
 * * thread mode - runs in a pausable closed-loop at a specific maximum framerate.
 */
public final class NAR extends NAL<NAR> implements Consumer<Task>, NARIn, NAROut, Cycled {

    static final String VERSION = "NARchy v?.?";
    static final Logger logger = Log.logger(NAR.class);

    public final Exec exe;
    public final NARLoop loop;
    public final Memory memory;
//    @Deprecated public final MemoryExternal memoryExternal = new MemoryExternal(this);
    public final ConceptBuilder conceptBuilder;
    public final Emotion emotion;
    private final Function<Term, What> whatBuilder;

    public final PartBag<What> what = new PartBag<>(NAL.WHATS_CAPACITY);

    public final Topic<NAR> eventClear = new ListTopic<>();
    public final Topic<NAR> eventCycle = new ListTopic<>();
    public final Control control;

    /**
     * id of this NAR's self; ie. its name
     */
    final Term self;

    public final What main;


    public NAR(Memory memory, Exec exe, Time time, Supplier<Random> rng, ConceptBuilder conceptBuilder, Function<Term, What> whatBuilder) {
        super(exe, time, rng);

        this.memory = memory;
        this.exe = exe;
        this.control = new Control(this);
        this.whatBuilder = whatBuilder;
        this.self = NAL.randomSelf();
        this.conceptBuilder = conceptBuilder;

        eventOnOff.on(this::indexPartChange);

        memory.start(this);


        onCycle(this.emotion = new Emotion(this));

        Builtin.init(this);

        this.loop = this.exe.concurrent() ? new NARLoop.NARLoopAsync(this) : new NARLoop.NARLoopSync(this);
        add(exe);

        synch();

        fork(main = whatBuilder.apply(self));

    }

    static void outputEvent(Appendable out, String previou, String chan, Object v) throws IOException {


        if (!chan.equals(previou)) {
            out.append(chan).append(": ");

        } else {

            int n = chan.length() + 2;
            for (int i = 0; i < n; i++)
                out.append(' ');
        }

        if (v instanceof Object[]) {
            v = Arrays.toString((Object[]) v);
        } else if (v instanceof Task) {
            Task tv = ((Task) v);
            float tvp = tv.priElseZero();
            v = ansi()

                    .a(tvp >= 0.25f ?
                            Ansi.Attribute.INTENSITY_BOLD :
                            Ansi.Attribute.INTENSITY_FAINT)
                    .a(tvp > 0.75f ? Ansi.Attribute.NEGATIVE_ON : Ansi.Attribute.NEGATIVE_OFF)
                    .fg(Prioritized.Companion.budgetSummaryColor(tv))
                    .a(
                            tv.toString(true)
                    )
                    .reset()
                    .toString();
        }

        out.append(v.toString()).append('\n');
    }

    public final void proofPrint(Task t) {
        proof(t, System.out);
    }

	public void proof(Task t, Appendable out) {
        try {
            out.append(t.toString());
            out.append('\n');
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Term tc = t.why();
		if (tc!=null) {
		    Why.forEachUnique(t.why(), new ShortProcedure() {
                @Override
                public void value(short s) {
                    Cause c = control.why.get((int) s);
                    try {
                        out.append(c.toString());
                        out.append('\n');
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
		}
	}

	/**
     * updates indexes when a part is added or removed
     *
     * @param change a change event emitted by Parts
     */
    private void indexPartChange(ObjectBooleanPair<Part<NAR>> change) {
        Part<NAR> p = change.getOne();
        if (p instanceof What) {
            PartBag b = what;
            if (change.getTwo()) {
                b.putAsync(p); //TODO handle rejection, eviction etc
            } else {
                boolean removed = b.remove(((PriNARPart) p).id) != null;
                assert (removed);
            }
        }
    }

    /**
     * dynamic axiom resolver
     */
    public final Functor axioms(Atom term) {
        Termed x = concept(term);
        if (x instanceof NodeConcept.PermanentNodeConcept)
            x = x.term();
        return x instanceof Functor ? (Functor) x : null;
    }

    public final ThreadLocal<Evaluator> evaluator = ThreadLocal.withInitial(new Supplier<Evaluator>() {
                                                                                @Override
                                                                                public Evaluator get() {
                                                                                    return new Evaluator(NAR.this::axioms);
                                                                                }
                                                                            }
    );



    /**
     * creates a snapshot statistics object
     * TODO extract a Method Object holding the snapshot stats with the instances created below as its fields
     */
    public synchronized SortedMap<String, Object> stats(boolean concepts, boolean emotions) {
        SortedMap<String, Object> x = new TreeMap();

        if (concepts) {
            LongSummaryStatistics beliefs = new LongSummaryStatistics();
            LongSummaryStatistics goals = new LongSummaryStatistics();
            LongSummaryStatistics questions = new LongSummaryStatistics();
            LongSummaryStatistics quests = new LongSummaryStatistics();


            HashBag clazz = new HashBag();
            HashBag rootOp = new HashBag();

            Histogram volume = new Histogram(1L, (long) term.COMPOUND_VOLUME_MAX, 3);

            {
                concepts().filter(new Predicate<Concept>() {
                    @Override
                    public boolean test(Concept xx) {
                        return !(xx instanceof Functor);
                    }
                }).forEach(new Consumer<Concept>() {
                    @Override
                    public void accept(Concept c) {

                        Term ct = c.term();
                        volume.recordValue((long) ct.volume());
                        rootOp.add(ct.op());
                        clazz.add(ct.getClass().toString());

                        beliefs.accept(c.beliefs().taskCount());
                        goals.accept(c.goals().taskCount());
                        questions.accept(c.questions().taskCount());
                        quests.accept(c.quests().taskCount());
                    }
                });


                if (loop.isRunning()) {
                    loop.stats("loop", x);
                }

                x.put("time", time());


                x.put("concept count", memory.size());
            }

            x.put("belief count", ((double) beliefs.getSum()));
            x.put("goal count", ((double) goals.getSum()));

            Util.toMap(rootOp, "concept op", x::put);

            Texts.INSTANCE.histogramDecode(volume, "concept volume", 4, x::put);

            Util.toMap(clazz, "concept class", x::put);

        }

        if (emotions) {
            emotion.commit(x::put);
        }

        return x;

    }

    /**
     * Reset the system with an empty memory and reset clock.  Event handlers
     * will remain attached but enabled parts will have been deactivated and
     * reactivated, a signal for them to empty their state (if necessary).
     */
    @Deprecated
    public void reset() {

        synchronized (exe) {

            boolean running = loop.isRunning();
            float fps = running ? loop.getFPS() : -1.0F;

            stop();

            clear();
            exe.clear(this);
            time.reset();

            add(exe);

            if (running)
                loop.setFPS(fps);
        }

        logger.info("reset");

    }

    /**
     * deallocate as completely as possible
     */
    public void delete() {
        logger.debug("delete {}", self());
        synchronized (exe) {

            stop();

            what.clear();

            //clear();
            memory.clear();

            super.delete();
        }
    }


//    public final NAR become(Term self) {
//        return become(self, whatBuilder);
//    }

//    public final NAR become(String self, Function<Term, What> builder) {
//        return become(Atomic.atom(self), builder);
//    }
//
//    public final NAR become(Term nextSelf, Function<Term, What> builder) {
//        synchronized (self) { //HACK
//            Term prev;
//            if (!Objects.equals(prev = self.getAndSet(nextSelf), nextSelf)) {
//                What prevWhat = this.getIn();
//                if (prevWhat !=null)
//                    stop(prevWhat);
//
//                if (nextSelf!=null) {
//                    start(this.in = builder.apply(nextSelf));
//                } else
//                    this.in = null; //HACK
//            }
//        }
//        return this;
//    }

    /**
     * the clear event is a signal indicating that any active memory or processes
     * which would interfere with attention should be stopped and emptied.
     * <p>
     * this does not indicate the NAR has stopped or reset itself.
     */
    @Deprecated public void clear() {
        logger.info("clear");
        eventClear.emit(this);
    }

    /**
     * parses one and only task
     */

    public <T extends Task> T inputTask(String taskText) throws Narsese.NarseseException {
        return (T) inputTask(Narsese.task(taskText, (this)));
    }

    public List<Task> input(String text) throws NarseseException, TaskException {
        List<Task> l = Narsese.tasks(text, this);
        switch (l.size()) {
            case 0:
                return Collections.EMPTY_LIST;
            case 1:
                input(l.get(0));
                return l;
            default:
                input(l);
                return l;
        }
    }

    /**
     * gets a concept if it exists, or returns null if it does not
     */
    public final @Nullable Concept conceptualize(String conceptTerm) throws NarseseException {
        return conceptualize(INSTANCE.$(conceptTerm));
    }

    /**
     * ask question
     */
    public Task question(String termString) throws NarseseException {
        return question(INSTANCE.$(termString));
    }

    /**
     * ask question
     */
    public Task question(Term c) {
        return que(c, QUESTION);
    }

    public Task quest(Term c) {
        return que(c, QUEST);
    }

    public @Nullable Task want(String goalTermString, Tense tense, float freq, float conf) throws NarseseException {
        return want(INSTANCE.$(goalTermString), tense, freq, conf);
    }

    public Task want(Term goal, Tense tense, float freq) {
        return want(goal, tense, freq, confDefault(GOAL));
    }

    /**
     * desire goal
     */
    public Task want(Term goalTerm, Tense tense, float freq, float conf) {
        long now = time(tense);
        return want(
                priDefault(GOAL),
                goalTerm, now, now, freq, conf);
    }

    public Task believe(Term term, Tense tense, float freq, float conf) {
        return believe(term, time(tense), freq, conf);
    }

    public Task believe(Term term, long when, float freq, float conf) {
        return believe(priDefault(BELIEF), term, when, freq, conf);
    }

    public Task believe(Term term, Tense tense, float freq) {
        return believe(term, tense, freq, confDefault(BELIEF));
    }

    public Task believe(Term term, long when, float freq) {
        return believe(term, when, freq, confDefault(BELIEF));
    }

    public Task believe(Term term, float freq, float conf) {
        return believe(term, Tense.Eternal, freq, conf);
    }

    public Task want(Term term, float freq, float conf) {
        return want(term, Tense.Eternal, freq, conf);
    }

    public NAR believe(String term, Tense tense, float freq, float conf) {
        try {
            believe(priDefault(BELIEF), INSTANCE.$(term), time(tense), freq, conf);
        } catch (NarseseException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Deprecated public long time(Tense tense) {
        return time.relativeOccurrence(tense);
    }

    public NAR believe(String termString, float freq, float conf) {
        return believe(termString, freq, conf, ETERNAL, ETERNAL);
    }

    public NAR believe(String termString, float freq, float conf, long start, long end) {
        believe(INSTANCE.$$(termString), start, end, freq, conf);
        return this;
    }

    public Task want(String termString) {
        return want(INSTANCE.$$(termString), true);
    }

    public NAR believe(String... tt) throws NarseseException {

        for (String b : tt)
            believe(b, true);

        return this;
    }

    public NAR believe(String termString, boolean isTrue) throws NarseseException {
        believe(INSTANCE.$(termString), isTrue);
        return this;
    }

    public Task want(String termString, boolean isTrue) throws NarseseException {
        return want(INSTANCE.$(termString), isTrue);
    }

    public Task believe(Term term) {
        return believe(term, true);
    }

    public Task believe(Term term, boolean trueOrFalse) {
        return believe(term, trueOrFalse, confDefault(BELIEF));
    }

    public Task want(Term term) {
        return want(term, true);
    }

    public Task want(Term term, boolean trueOrFalse) {
        return want(term, trueOrFalse, confDefault(BELIEF));
    }

    public Task believe(Term term, boolean trueOrFalse, float conf) {
        return believe(term, trueOrFalse ? 1.0f : 0f, conf);
    }

    public Task want(Term term, boolean trueOrFalse, float conf) {
        return want(term, trueOrFalse ? 1.0f : 0f, conf);
    }

    public NAR believe(Term term, long occurrenceTime) throws TaskException {
        return believe(term, occurrenceTime, occurrenceTime);
    }

    public NAR believe(Term term, long start, long end) throws TaskException {
        input(priDefault(BELIEF), term, BELIEF, start, end, 1f, (double) confDefault(BELIEF));
        return this;
    }

    public Task believe(Term term, long start, long end, float freq, float conf) throws TaskException {
        return believe(priDefault(BELIEF), term, start, end, freq, conf);
    }

    public Task believe(float pri, Term term, long occurrenceTime, float freq, float conf) throws TaskException {
        return believe(pri, term, occurrenceTime, occurrenceTime, freq, conf);
    }

    public Task believe(float pri, Term term, long start, long end, float freq, float conf) throws TaskException {
        return input(pri, term, BELIEF, start, end, freq, (double) conf);
    }

    public Task want(float pri, Term goal, long start, long end, float freq, float conf) throws TaskException {
        return input(pri, goal, GOAL, start, end, freq, (double) conf);
    }

    public Task input(float pri, Term term, byte punc, long occurrenceTime, float freq, float conf) throws TaskException {
        return input(pri, term, punc, occurrenceTime, occurrenceTime, freq, (double) conf);
    }

    public Task input(float pri, Term term, byte punc, long start, long end, float freq, double conf) throws TaskException {

        PreciseTruth truth = $.INSTANCE.t(freq, conf);

        Term c = Task.taskValid(term, punc, truth, false);

        Task z = NALTask.the(c, punc, truth, time(), start, end, evidence());

        z.pri(pri);
        input(z);

        return z;
    }

    /**
     * ¿qué?  que-stion or que-st
     */
    public Task que(Term term, byte questionOrQuest) {
        return que(term, questionOrQuest, ETERNAL);
    }

//    public NAR logWhen(Appendable out) {
//
//        if (past && present && future)
//            return log(out);
//
//        return log(out, v -> {
//            if (v instanceof Task) {
//                Task t = (Task) v;
//                long now = time();
//                return
//
//                                (future && t.isDuring(now));
//            }
//            return false;
//        });
//    }

    /**
     * ¿qué?  que-stion or que-st
     */
    public Task que(Term term, byte punc, long when) {
        return que(term, punc, when, when);
    }

    public Task que(Term term, byte punc, long start, long end) {
        assert (((int) punc == (int) QUESTION) || ((int) punc == (int) QUEST));

        return inputTask(
                NALTask.the(term.unneg(), punc, null, time(), start, end, new long[]{time.nextStamp()}).budget(this)
        );
    }

    /**
     * logs tasks and other budgeted items with a summary exceeding a threshold
     */
    public Off logPriMin(Appendable out, float priThresh) {
        return log(out, new Predicate() {
            @Override
            public boolean test(Object v) {
                Prioritized b = null;
                if (v instanceof Prioritized) {
                    b = ((Prioritized) v);
                } else if (v instanceof Twin) {
                    if (((Pair) v).getOne() instanceof Prioritized) {
                        b = (Prioritized) ((Pair) v).getOne();
                    }
                }
                return b != null && b.priElseZero() >= priThresh;
            }
        });
    }

    @Override
    public final void input(Task t) {
        main.accept(t);
    }

    @Override
    public final void input(Task... t) {
        main.acceptAll(t);
    }

    @Override
    public final void accept(Task task) {
        input(task);
    }

    @Override
    public final @Nullable Term term(Part<NAR> p) {
        return ((NARPart) p).id;
    }

    public final boolean add(NARPart p) {
        return add(p.id, p, true);
    }

    public final boolean add(NARPart p, boolean autoStart) {
        return add(p.id, p, autoStart);
    }
    public final void add(Functor f) {
        memory.set(f);
    }
    public final void add(Operator o) {
        memory.set(o);
    }

    public final NARPart add(Class<? extends NARPart> p, boolean autoStart) {
        return add(null, p, autoStart);
    }

    public final NARPart add(Class<? extends NARPart> p) {
        return add(p, true);
    }


    public final NARPart add(@Nullable Term key, Class<? extends NARPart> p) {
        return add(key, p, true);
    }

    public final NARPart add(@Nullable Term key, Class<? extends NARPart> p, boolean autoStart) {
        NARPart pp = null;
        if (key != null)
            pp = (NARPart) parts.get(key);

        if (pp == null) {
            //HACK
            //TODO make sure this is atomic
            pp = build(p).get();
            if (key == null)
                key = pp.id;
            if (parts.get(key) == pp) {
                return pp; //already added in its constructor HACK
            }
        } else {
            if (p.isAssignableFrom(pp.getClass()))
                return pp; //ok
            else {
                remove(key);
            }
        }

        if (autoStart) {
            boolean ok = add(key, pp);
            assert (ok);
        }
        return pp;
    }

    /**
     * simplified wrapper for use cases where only the arguments of an operation task, and not the task itself matter
     */
    public final void addOpN(String atom, BiConsumer<Subterms, NAR> exe) {
        setOp(Atomic.atom(atom), new BiFunction<Task, NAR, Task>() {
            @Override
            public Task apply(Task task, NAR nar) {
                exe.accept(task.term().sub(0).subterms(), nar);
                return null;
            }
        });
    }

    public final Operator addOp1(String atom, BiConsumer<Term, NAR> exe) {
        return setOp(Atomic.atom(atom), new BiFunction<Task, NAR, Task>() {
            @Override
            public Task apply(Task task, NAR nar) {

                Subterms ss = task.term().sub(0).subterms();
                if (ss.subs() == 1)
                    exe.accept(ss.sub(0), nar);
                return null;
            }
        });
    }

    public final void addOp2(String atom, TriConsumer<Term, Term, NAR> exe) {
        setOp(Atomic.atom(atom), new BiFunction<Task, NAR, Task>() {
            @Override
            public Task apply(Task task, NAR nar) {

                Subterms ss = task.term().sub(0).subterms();
                if (ss.subs() == 2)
                    exe.accept(ss.sub(0), ss.sub(1), nar);
                return null;
            }
        });
    }

    /**
     * registers an operator
     */
    public final Operator setOp(Atom name, BiFunction<Task, NAR, Task> exe) {
        Operator op = Operator.simple(name, exe);
        memory.set(op);
        return op;
    }

    /**
     * the default time constant of the system
     */
    @Override
    public final float dur() {
        return time.dur();
    }


    public final @Nullable Truth truth(Termed concept, byte punc, long when) {
        return truth(concept, punc, when, when);
    }


    /**
     * returns concept belief/goal truth evaluated at a given time
     */
    public final @Nullable Truth truth(Termed concept, byte punc, long start, long end) {

        boolean neg = concept.term().op()==NEG;
        if (neg)
            concept = concept.term().unneg();

        @Nullable BeliefTable table = (BeliefTable) tableDynamic(concept, punc);
        return table != null ? Truth.negIf(table.truth(start, end, concept instanceof Term ? ((Term) concept) : null, null, this),neg) : null;
    }

    public final @Nullable Truth beliefTruth(String concept, long when) throws NarseseException {
        return truth(INSTANCE.$(concept), BELIEF, when);
    }

    public final @Nullable Truth goalTruth(String concept, long when) throws NarseseException {
        return truth(INSTANCE.$(concept), GOAL, when);
    }

    public final @Nullable Truth beliefTruth(Termed concept, long when) {
        return truth(concept, BELIEF, when);
    }

    public final @Nullable Truth beliefTruth(Termed concept, long start, long end) {
        return truth(concept, BELIEF, start, end);
    }

    /* Print all statically known events (discovered via reflection)
     *  for this reasoner to a stream
     * */

    public final @Nullable Truth goalTruth(Termed concept, long when) {
        return truth(concept, GOAL, when);
    }

    public final @Nullable Truth goalTruth(Termed concept, long start, long end) {
        return truth(concept, GOAL, start, end);
    }

    /**
     * Exits an iteration loop if running
     */
    public NAR stop() {

        synchronized (exe) {

            loop.stop();

            synch();

            stop(exe);
        }

        return this;
    }

    /**
     * steps 1 frame forward. cyclesPerFrame determines how many cycles this frame consists of
     */
    @Override
    public final void run() {
        loop.run();
    }

    public Off trace(Appendable out, Predicate<String> includeKey) {
        return trace(out, includeKey, null);
    }

    /** the current context's eventTask */
    public final ByteTopic<Task> eventTask() {
        return main.eventTask;
    }

    public Off trace(Appendable out, Predicate<String> includeKey, @Nullable Predicate includeValue) {

        return eventTask().on(new Consumer<>() {

            String previous = null;

            @Override
            public void accept(Task v) {

                if (includeValue != null && !includeValue.test(v))
                    return;

                try {
                    outputEvent(out, previous, "task", v);
                } catch (IOException e) {
                    logger.error("outputEvent: {}", e.toString());
                }
                previous = "task";
            }

            {
                Topic.all(this, (k, v) -> {

                    if (includeValue != null && !includeValue.test(v))
                        return;

                    try {
                        outputEvent(out, previous, k, v);

                    } catch (IOException e) {
                        logger.error("outputEvent: {}", e.toString());
                    }
                    previous = k;
                }, includeKey);
            }
        });
    }

    public Off trace(Appendable out) {
        return trace(out, new Predicate<String>() {
            @Override
            public boolean test(String k) {
                return true;
            }
        });
    }

    public Off log() {
        return log(System.out);
    }

    public Off log(Appendable out) {
        return log(out, null);
    }

    public Off log(Appendable out, Predicate includeValue) {
        return trace(out, new Predicate<String>() {
            @Override
            public boolean test(String x) {
                return true;
            }
        }, includeValue);
    }

    /**
     * Runs until stopped, at a given delay period between frames (0= no delay). Main loop
     *
     * @param initialDelayMS in milliseconds
     */
    @Override
    public final NARLoop startPeriodMS(int initialDelayMS) {
        loop.setPeriodMS(initialDelayMS);
        return loop;
    }


    public NAR input(String... ss) throws NarseseException {
        for (String s : ss)
            input(s);
        return this;
    }

    public NAR inputNarsese(URL url) throws IOException, NarseseException {
        return inputNarsese(url.openStream());
    }

    public NAR inputNarsese(InputStream inputStream) throws IOException, NarseseException {
        input(new String(inputStream.readAllBytes()));
        return this;
    }

    @Deprecated public NAR inputAt(long time, String... tt) {

//        assert (tt.length > 0);
//        FasterList<Task> yy = new FasterList<>(tt.length);
//        for (String s : tt) {
//            try {
//                yy.addAll(Narsese.tasks(s, this));
//            } catch (NarseseException e) {
//                logger.error("{} for: {}", e, s);
//                e.printStackTrace();
//            }
//        }

//        int size = yy.size();
//        if (size <= 0)
//            throw new WTF/*NarseseException*/("no tasks parsed from input: " + Joiner.on("\n").join(tt));
//
////            assert(yy.allSatisfyWith((y,t)->y.start()==t, time));
//        yy.replaceAll(t -> {
//            if (!t.isEternal() && t.start() != time) {
//                return new SpecialOccurrenceTask(t, time, time + (t.range() - 1));
//            } else
//                return t;
//        });

        runAt(time, new Consumer<NAR>() {
            @Override
            public void accept(NAR nn) {
                //nn.input(yy.toArray(new Task[size]))
                try {
                    nn.input(tt);
                } catch (NarseseException e) {
                    e.printStackTrace();
                }
            }
        });

        return this;
    }

    /**
     * TODO use a scheduling using r-tree
     */
    public void inputAt(long when, Task... x) {
        runAt(when, new Consumer<NAR>() {
            @Override
            public void accept(NAR nn) {
                nn.input(x);
            }
        });
    }

    public final void runAt(long whenOrAfter, Consumer<NAR> t) {
        runAt(WhenTimeIs.then(whenOrAfter, t));
    }

    /**
     * schedule a task to be executed no sooner than a given NAR time
     *
     * @return
     */
    public final ScheduledTask runAt(long whenOrAfter, Runnable t) {
        return runAt(WhenTimeIs.then(whenOrAfter, t));
    }



    public final ScheduledTask runAt(ScheduledTask t) {
        exe.runAt(t);
        return t;
    }


    public final void runLater(Runnable t) {
        runLater(WhenTimeIs.then(time(), t));
    }

    /**
     * adds a task to the queue of task which will be executed in batch
     * after the end of the current frame before the next frame.
     */
    public final void runLater(ScheduledTask t) {
        exe.runAt(t);
    }

    /**
     * tasks in concepts
     */

    public Stream<Task> tasks(boolean includeConceptBeliefs, boolean includeConceptQuestions,
                              boolean includeConceptGoals, boolean includeConceptQuests) {
        return concepts().flatMap(new Function<Concept, Stream<? extends Task>>() {
            @Override
            public Stream<? extends Task> apply(Concept c) {
                return c.tasks(includeConceptBeliefs, includeConceptQuestions, includeConceptGoals, includeConceptQuests);
            }
        });
    }

    public void tasks(boolean includeConceptBeliefs, boolean includeConceptQuestions, boolean includeConceptGoals,
                      boolean includeConceptQuests, BiConsumer<Concept, Task> each) {
        concepts().forEach(new Consumer<Concept>() {
                               @Override
                               public void accept(Concept c) {
                                   c.tasks(includeConceptBeliefs,
                                           includeConceptQuestions,
                                           includeConceptGoals,
                                           includeConceptQuests).forEach(new Consumer<Task>() {
                                       @Override
                                       public void accept(Task t) {
                                           each.accept(c, t);
                                       }
                                   });
                               }
                           }
        );
    }

    public Stream<Task> tasks() {
        return tasks(true, true, true, true);
    }

    /**
     * resolves a target or concept to its currrent Concept
     */
    public final @Nullable Concept concept(Termed x) {
        return concept(x, false);
    }

    public final @Nullable Concept concept(String term) {
        return concept(INSTANCE.$$(term), false);
    }

    /**
     * resolves a target to its Concept; if it doesnt exist, its construction will be attempted
     */
    public final @Nullable Concept conceptualize(/**/ Termed termed) {
        return concept(termed, true);
    }

    public final @Nullable Concept concept(/**/Termed _x, boolean createIfMissing) {
//        if (Param.DEBUG) {
//            int v = x.target().volume();
//            int max = termVolumeMax.intValue();
//            if (v > max) {
//                //return null; //too complex
//                logger.warn("too complex for conceptualization ({}/{}): " + x, v, max);
//            }
//        }

        if (_x instanceof Concept && memory.elideConceptGets()) {
            Concept cx = (Concept) _x;
            if (!(cx.isDeleted())) return cx;
        }

        Term xt = _x.term();
        if (!xt.op().conceptualizable)
            throw new TermException("not conceptualizable", xt);


//        if (NAL.DEBUG) { if (!xt.the()) throw new TermException("not immutable", xt); }

//        if (xt instanceof Compound && !xt.isNormalized()) { //pre-test
//            Term xtn = xt.normalize();
//            if (xt.op()!=xtn.op()) //if (!xt.equals(xtn))
//                throw new TermException("concept term not normalized", xt);
//            xt = xtn;
//        }

        Term x = xt.concept();

        return memory.get(x, createIfMissing);
    }

    public final Stream<Concept> concepts() {
        return memory.stream();
    }

    /**
     * warning: the condition will be tested each cycle so it may affect performance
     */
    public NAR stopIf(BooleanSupplier stopCondition) {
        onCycle(new Consumer<NAR>() {
            @Override
            public void accept(NAR n) {
                if (stopCondition.getAsBoolean())
                    NAR.this.stop();
            }
        });
        return this;
    }

    /**
     * a frame batches a burst of multiple cycles, for coordinating with external systems in which multiple cycles
     * must be run per control frame.
     */
    public final Off onCycle(Consumer<NAR> each) {
        return eventCycle.on(each);
    }

//    /**
//     * avoid using lambdas with this, instead use an interface implementation of the class that is expected to be garbage collected
//     */
//    public final Off onCycleWeak(Consumer<NAR> each) {
//        return eventCycle.onWeak(each);
//    }

    public final Off onCycle(Runnable each) {
        return onCycle(new Consumer<NAR>() {
            @Override
            public void accept(NAR ignored) {
                each.run();
            }
        });
    }

    public final DurLoop onDur(Runnable on) {
        return onDur(on, true);
    }

    public final DurLoop onDur(Runnable on, boolean autostart) {
        DurLoop.DurRunnable r = new DurLoop.DurRunnable(on);
        if (autostart)
            add(r);
        return r;
    }

    /*
      public static DurLoop onWhile(NAR nar, Predicate<NAR> r) {
        return new DurLoop(nar) {
            @Override
            protected void run(NAR n, long dt) {
                if (!r.test(n)) {
                    off();
                }
            }

            @Override
            public String toString() {
                return r.toString();
            }
        };
    }
     */

    public final DurLoop onDur(Consumer<NAR> on) {
        DurNARConsumer r = new DurNARConsumer(on);
        add(r);
        return r;
    }

    public final Off onTask(Consumer<Task> listener) {
        return eventTask().on(listener);
    }
//TODO
//    public final Off onTaskWeak(Consumer<Task> listener, byte... punctuations) {
//        return eventTask.onWeak(listener, punctuations);
//    }

    @Deprecated public final Off onTask(Consumer<Task> listener, byte... punctuations) {
        return main.onTask(listener, punctuations);
    }

    public NAR trace() {
        trace(System.out);
        return this;
    }

    /**
     * if this is an Iterable<Task> , it can be more efficient to use the inputTasks method to bypass certain non-NALTask conditions
     */
    @Deprecated
    public void input(Iterable<? extends Task> tasks) {
        main.acceptAll(tasks);
    }

    @Deprecated
    public final void input(Stream<? extends Task> tasks) {
        main.acceptAll(tasks);
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj;
    }

    public NAR believe(Term c, Tense tense) {
        believe(c, tense, 1f);
        return this;
    }

    /**
     * activate/"turn-ON"/install a concept in the index and activates it, used for setup of custom concept implementations
     * implementations should apply active concept capacity policy
     */
    public final <P extends PermanentConcept> P add(P c) {

        memory.set(c);

        conceptBuilder.start(c);

        return c;
    }

    @Override
    public final long time() {
        return time.now();
    }

    public NAR inputBinary(File input) throws IOException {
        return inputBinary(new GZIPInputStream(new BufferedInputStream(new FileInputStream(input), IO.streamBufferBytesDefault), IO.gzipWindowBytesDefault));
    }

    public NAR outputBinary(File f) throws IOException {
        return outputBinary(f, false);
    }

    public final NAR outputBinary(File f, boolean append) throws IOException {
        return outputBinary(f, append, (Predicate<Task>) Function.identity());
    }

    public final NAR outputBinary(File f, boolean append, Predicate<Task> each) throws IOException {
        return outputBinary(f, append, new UnaryOperator<Task>() {
            @Override
            public Task apply(Task t) {
                return each.test(t) ? t : null;
            }
        });
    }

    public NAR outputBinary(File f, boolean append, UnaryOperator<Task> each) throws IOException {
        FileOutputStream f1 = new FileOutputStream(f, append);
        OutputStream ff = new GZIPOutputStream(f1, IO.gzipWindowBytesDefault);
        outputBinary(ff, each);
        return this;
    }

    public final NAR outputBinary(OutputStream o) {
        return outputBinary(o, new UnaryOperator<Task>() {
            @Override
            public Task apply(Task x) {
                return x;
            }
        });
    }

    public final NAR outputBinary(OutputStream o, Predicate<Task> filter) {
        return outputBinary(o, new UnaryOperator<Task>() {
            @Override
            public Task apply(Task x) {
                return filter.test(x) ? x : null;
            }
        });
    }

    /**
     * byte codec output of matching concept tasks (blocking)
     * <p>
     * the each function allows transforming each task to an optional output form.
     * if this function returns null it will not output that task (use as a filter).
     */
    public NAR outputBinary(OutputStream o, UnaryOperator<Task> each) {

        Util.time(logger, "outputBinary", new Runnable() {
            @Override
            public void run() {

                DataOutputStream oo = new DataOutputStream(o);

                MutableInteger total = new MutableInteger(0), wrote = new MutableInteger(0);

                DynBytes d = new DynBytes(128);

                NAR.this.tasks().map(each).filter(Objects::nonNull).distinct().forEach(new Consumer<Task>() {
                    @Override
                    public void accept(Task x) {

                        total.increment();

                        try {
                            @Nullable byte[] b = IO.taskToBytes(x, d);
                            oo.write(b);
                            wrote.increment();
                        } catch (IOException e) {
                            if (NAL.DEBUG)
                                throw new RuntimeException(e);
                            else
                                logger.warn("output binary: {}", e);
                        }

                    }
                });

                logger.info("{} output {}/{} tasks ({} bytes uncompressed)", o, wrote, total, oo.size());


                try {
                    oo.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        return this;
    }

    public NAR outputText(OutputStream o, UnaryOperator<Task> each) {


        PrintStream ps = new PrintStream(o);

        MutableInteger total = new MutableInteger(0);

        StringBuilder sb = new StringBuilder();
        tasks().map(each).filter(Objects::nonNull).forEach(new Consumer<Task>() {
            @Override
            public void accept(Task x) {

                total.increment();

                if (x.truth() != null && x.conf() < confMin.floatValue())
                    return;

                sb.setLength(0);
                ps.println(x.appendTo(sb, true));
            }
        });

        try {
            o.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this;
    }

    public NAR output(File o, boolean binary) throws FileNotFoundException {
        return output(new BufferedOutputStream(new FileOutputStream(o), IO.outputBufferBytesDefault), binary);
    }

    public NAR output(File o, UnaryOperator<Task> f) throws FileNotFoundException {
        return outputBinary(new BufferedOutputStream(new FileOutputStream(o), IO.outputBufferBytesDefault), f);
    }

    public NAR output(OutputStream o, boolean binary) {
        return binary ? outputBinary(o, new UnaryOperator<Task>() {
            @Override
            public Task apply(Task x) {
                return x.isDeleted() ? null : x;
            }
        }) : outputText(o, new UnaryOperator<Task>() {
            @Override
            public Task apply(Task x) {
                return x.isDeleted() ? null : x;
            }
        });
    }

    /**
     * byte codec input stream of tasks, to be input after decode
     * TODO use input(Stream<Task>..</Task>
     */
    public NAR inputBinary(InputStream i) throws IOException {

        int count = IO.readTasks(i, this::input);


        logger.info("input {} tasks from {}", count, i);


        return this;
    }

    /**
     * The id/name of the reasoner
     */
    public final Term self() {
        return self;
    }

    /**
     * strongest matching belief for the target time
     */
    public @Nullable Task belief(Termed c, long start, long end) {
        return answer(c, BELIEF, start, end);
    }

    public final Task belief(String c, long when) throws NarseseException {
        return belief(INSTANCE.$(c), when);
    }

    public final Task belief(Termed c, long when) {
        return belief(c, when, when);
    }

    public final Task belief(Term c) {
        return belief(c, ETERNAL);
    }

    public final @Nullable Task answer(Term c, byte punc, long when) {
        return answer(c, punc, when, when);
    }

    public @Nullable Task answer(Termed t, byte punc, long start, long end) {
        assert ((int) punc == (int) BELIEF || (int) punc == (int) GOAL);


        Term tt = t.term();
        boolean negate = tt instanceof Neg;
        if (negate)
            t = tt = tt.unneg();


        BeliefTable table = (BeliefTable) tableDynamic(!negate ? t : tt, punc);
        if (table == null)
            return null;

        Task answer = table.matchExact(start, end, tt, null, dur(), this);

        return answer!=null ? Task.negIf(answer, negate) : null;
    }

    public SortedMap<String, Object> stats(boolean concepts, boolean emotions, Appendable out) {

        SortedMap<String, Object> stat = stats(concepts, emotions);
        for (Map.Entry<String, Object> entry : stat.entrySet()) {
            String k = entry.getKey();
            Object v = entry.getValue();
            try {
                out.append(k.replace(" ", "/")).append(" \t ").append(v.toString()).append('\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            out.append('\n');
        } catch (IOException e) {
        }

        return stat;
    }

    /**
     * deletes any task with a stamp containing the component
     */
    public void retract(long stampComponent) {
        tasks().filter(new Predicate<Task>() {
            @Override
            public boolean test(Task x) {
                return Longs.contains(x.stamp(), stampComponent);
            }
        }).forEach(Task::delete);
    }

//    /**
//     * computes an evaluation amplifier factor, in range 0..2.0.
//     * VALUE -> AMP
//     * -Infinity -> amp=0
//     * 0     -> amp=1
//     * +Infinity -> amp=2
//     */
//    public float amp(short[] effect) {
//        return control.amp(effect);
//    }

//    public final float amp(Task task) {
//        return control.amp(task);
//    }

    public static float value(Term why) {
        throw new TODO();
    }

    @Deprecated public float value(short[] effect) {
        return control.value(effect);
    }



    public Cause newCause(Object name) {
        return control.newCause(name);
    }

    /**
     * automatically adds the cause id to each input
     */
    public CauseChannel<Task> newChannel(Object id) {
        return control.newChannel(id);
    }

    public <C extends Cause> C newCause(ShortToObjectFunction<C> idToChannel) {
        return control.newCause(idToChannel);
    }


    /**
     * invokes any pending tasks without advancing the clock
     */
    public final NAR synch() {
        exe.synch();
        return this;
    }

    public final Task answerBelief(Term x, long when) {
        return answerBelief(x, when, when);
    }

    public final Task answerBelief(Term x, long start, long end) {
        return answer(x, BELIEF, start, end);
    }

    public final Task answerGoal(Term x, long when) {
        return answerGoal(x, when, when);
    }

    public final Task answerGoal(Term x, long start, long end) {
        return answer(x, GOAL, start, end);
    }

    /**
     * creates a view for resolving unifiable terms with 'x'
     * above provided absolute expectation.  negative terms can be returned negative
     */
    public Function<Term, Stream<Term>> facts(float expMin, boolean beliefsOrGoals) {
        return new Facts(this, expMin, beliefsOrGoals);
    }


    /**
     * stream of all (explicitly and inferrable) internal events
     */
    public Stream<? extends WhenInternal> when() {
        return Streams.concat(
                //TODO Streams.stream(eventTask).map(t -> ), // -> AtTask events
                Streams.stream(eventCycle).map(WhenCycle::new),
                Streams.stream(eventClear).map(WhenClear::new),
                this.partStream()
                        .map(new Function<Part<NAR>, WhenInternal>() {
                            @Override
                            public WhenInternal apply(Part<NAR> s) {
                                return ((NARPart) s).event();
                            }
                        }).filter(Objects::nonNull),
                exe.events()
                        .filter(new Predicate<ScheduledTask>() {
                            @Override
                            public boolean test(ScheduledTask t) {
                                return !(t instanceof DurLoop.WhenDur);
                            }
                        }) //HACK (these should already be included in service's events)
//            causes.stream(),
        );
    }

    /**
     * map of internal events organized by category
     */
    public final Map<Term, List<WhenInternal>> whens() {
        return when().collect(Collectors.groupingBy(WhenInternal::category));
    }

    /**
     * stream of all registered services
     */
    public final <X> Stream<X> parts(Class<? extends X> nAgentClass) {
        return this.partStream().filter(new Predicate<Part<NAR>>() {
            @Override
            public boolean test(Part<NAR> x) {
                return nAgentClass.isAssignableFrom(x.getClass());
            }
        }).map(new Function<Part<NAR>, X>() {
            @Override
            public X apply(Part<NAR> x) {
                return (X) x;
            }
        });
    }


    public final What fork(Term id) {
        return fork(id, true);
    }

    /**
     * TODO persistent cache
     */
    public What fork(Term id, boolean createAndStartIfMissing) {
        What w;
        synchronized (what) {
            w = what.get(id);
            if (w!=null || !createAndStartIfMissing)
                return w;

            w = what.put(this.whatBuilder.apply(id));
            w.nar = this;
        }
        add(w);
        return w;
    }

//    public NAR the(What w) {
//        synchronized (what) {
//            What existing = what.put(w);
//            if (existing!=null)
//                throw new RuntimeException(/*TODO*/);
//
//            w.nar = this; //HACK
//        }
//        start(w);
//        return this;
//    }


    public final What what() {
        return main;
//        What w = active.get();
//        if (w == null) {
//            Term id = $.identity(Thread.currentThread());
//            fork(w = the(id, true));
//        }
//        return w;
    }

    public final <W extends What> W fork(W next) {
        return (W) fork(next, null);
    }

    /**
     * this allows forking the curent 'what' context, while also applying an optional reprioritization to
     * the previous context (if any).  the reprioritization function can also delete the previous context
     * by returning NaN
     */
    public final What fork(What next, @Nullable FloatFunction<What> reprioritizeCurrent) {
//        What prev = active.get();
//        if (next == prev)
//            return next;
        //float delta = 0;
//        if (reprioritizeCurrent != null && prev != null) {
//            float prevPriNext = reprioritizeCurrent.floatValueOf(prev);
//            if (prevPriNext != prevPriNext) {
//                //NaN to discard
//                what.remove(prev.id);
//            } else {
//                prev.pri(prevPriNext);
//            }
//        }

        if (logger.isDebugEnabled())
            logger.debug("fork {} {}" /* (+{})"*/, Thread.currentThread(), next);

        What removed = what.put(next, null);
        if (removed!=null)
            stop(removed);
        add(next);
//        active.set(next);
        return next;
    }

    @Deprecated public Term eval(Term x) {
        if (x instanceof Compound) {
            Term y = Evaluation.solveFirst(x, this::axioms);
            return y == null ? x : y;
        } else {
            return x;
        }
    }

    /**
     * conceptualize a target if dynamic truth is possible; otherwise return concept if exists
     * try to use beliefTableDynamic(Termed concept) to avoid unnecessary Concept construction
     */
    @Deprecated public final Concept conceptualizeDynamic(Termed concept) {

        Concept x = concept(concept);
        if (x != null)
            return x;

        Term ct = concept.term();
        if (ct instanceof Compound) {
            if (ct.volume() > termVolMax.intValue())
                return null; //too complex to analyze for dynamic

            if (ConceptBuilder.dynamicModel((Compound) ct) != null) { //HACK
                //try conceptualizing the dynamic
                //create temporary dynamic concept
                return NAL.CONCEPTUALIZE_DYNAMIC_TRANSIENT ? conceptBuilder.construct(ct) : conceptualize(concept);

            }
        }

        return null;
    }

    public @Nullable TaskTable tableDynamic(Termed concept, byte punc) {
        switch (punc) {
            case BELIEF:
                return tableDynamic(concept, true);
            case GOAL:
                return tableDynamic(concept, false);
            default:
                Concept exist = concept(concept);
                return (exist != null) ? exist.table(punc) : null;
        }
    }

    public @Nullable BeliefTable tableDynamic(Termed concept, boolean beliefOrGoal) {
        //heavyweight, concept construction involved
        if (!concept.term().op().conceptualizable) {
            if (NAL.DEBUG)
                throw new WTF();
            return null;
        }
//        Concept c = conceptualizeDynamic(su);
//        if (!(c instanceof TaskConcept))
//            return null;
//        BeliefTable table = (BeliefTable) c.table((beliefOrGoal ? BELIEF : GOAL));
//        return table;


        Concept c = concept(concept);
        if (c != null)
            return (BeliefTable) c.table(beliefOrGoal ? BELIEF : GOAL); //concept exists, use its table

        Term ct = concept.term();
        if (ct instanceof Compound && ct.volume() < termVolMax.intValue()) {
            @Nullable ObjectBooleanToObjectFunction<Term, BeliefTable[]> dmt = ConceptBuilder.dynamicModel((Compound) ct);
            if (dmt != null) {
                BeliefTable[] dmtb = dmt.valueOf(ct, beliefOrGoal);
                switch (dmtb.length) {
                    case 0: return null; //does this happen?
                    case 1: return dmtb[0];
                    default: return new BeliefTables(dmtb);
                }
            }
        }

        return null;
    }
}
