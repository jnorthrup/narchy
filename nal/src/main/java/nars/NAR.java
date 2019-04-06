package nars;


import com.google.common.collect.Streams;
import com.google.common.primitives.Longs;
import jcog.Texts;
import jcog.Util;
import jcog.WTF;
import jcog.data.byt.DynBytes;
import jcog.event.ListTopic;
import jcog.event.Off;
import jcog.event.Topic;
import jcog.exe.Cycled;
import jcog.func.TriConsumer;
import jcog.math.MutableInteger;
import jcog.pri.Prioritized;
import jcog.service.Part;
import nars.Narsese.NarseseException;
import nars.attention.TaskLinkBagAttention;
import nars.concept.Concept;
import nars.concept.Operator;
import nars.concept.PermanentConcept;
import nars.concept.TaskConcept;
import nars.concept.util.ConceptBuilder;
import nars.control.Cause;
import nars.control.Control;
import nars.control.NARPart;
import nars.control.channel.CauseChannel;
import nars.eval.Evaluator;
import nars.eval.Facts;
import nars.exe.Exec;
import nars.exe.NARLoop;
import nars.index.concept.Memory;
import nars.io.IO;
import nars.subterm.Subterms;
import nars.table.BeliefTable;
import nars.task.ITask;
import nars.task.NALTask;
import nars.task.util.TaskBuffer;
import nars.task.util.TaskException;
import nars.task.util.TaskTopic;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.time.ScheduledTask;
import nars.time.Tense;
import nars.time.Time;
import nars.time.event.AtClear;
import nars.time.event.AtCycle;
import nars.time.event.AtTime;
import nars.time.event.InternalEvent;
import nars.time.part.DurLoop;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import nars.util.Timed;
import org.HdrHistogram.Histogram;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.util.Set;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static nars.$.$;
import static nars.$.$$;
import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static nars.truth.func.TruthFunctions.c2w;
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
public class NAR extends Param implements Consumer<ITask>, NARIn, NAROut, Cycled, Timed {

    static final String VERSION = "NARchy v?.?";
    private static final Set<String> loggedEvents = java.util.Set.of("eventTask");
    public final Exec exe;
    public final NARLoop loop;
    public final Time time;
    public final Memory memory;
    public final MemoryExternal memoryExternal = new MemoryExternal(this);
    public final ConceptBuilder conceptBuilder;
    public final Emotion feel;
    public final TaskLinkBagAttention attn;
    public final TaskBuffer in;  //perception?
    public final Topic<NAR> eventClear = new ListTopic<>();
    public final Topic<NAR> eventCycle = new ListTopic<>();
    public final TaskTopic eventTask = new TaskTopic();
    public final Control control;
    public final Evaluator evaluator = new Evaluator(this::axioms);
    protected final Supplier<Random> random;
    /**
     * atomic for thread-safe schizophrenia
     */
    private final AtomicReference<Term> self = new AtomicReference<>(null);


    public NAR(Memory memory, Exec exe, TaskLinkBagAttention attn, Time time, TaskBuffer in, Supplier<Random> rng, ConceptBuilder conceptBuilder) {
        super(exe);

        this.random = rng;

        (this.time = time).reset();

        this.memory = memory;

        named(Param.randomSelf());

        this.attn = attn;

        //TODO extract to a method which can be used to change .in at runtime
        this.in = in;
        Exec target = exe;
        if (!in.async(target)) {
            Consumer<NAR> p = nn -> in.commit(nn.time(), target);
            //if (cycleOrDur)
            onCycle(p);
//                DurService.on(this, p)
        }

        this.exe = exe;

        this.conceptBuilder = conceptBuilder;
        memory.start(this);

        this.feel = new Emotion(this);

        start(this.attn);

        this.control = new Control(this);


        Builtin.init(this);

        this.loop = new NARLoop(this);

        exe.start(this);
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
                    .fg(Prioritized.budgetSummaryColor(tv))
                    .a(
                            tv.toString(true)
                    )
                    .reset()
                    .toString();
        }

        out.append(v.toString()).append('\n');
    }

    /**
     * dynamic axiom resolver
     */
    public final Functor axioms(Atom term) {
        Termed x = concept(term);
        return x instanceof Functor ? (Functor) x : null;
    }

    /**
     * creates a snapshot statistics object
     * TODO extract a Method Object holding the snapshot stats with the instances created below as its fields
     */
    public SortedMap<String, Object> stats() {

        LongSummaryStatistics beliefs = new LongSummaryStatistics();
        LongSummaryStatistics goals = new LongSummaryStatistics();
        LongSummaryStatistics questions = new LongSummaryStatistics();
        LongSummaryStatistics quests = new LongSummaryStatistics();


        HashBag clazz = new HashBag();
        HashBag rootOp = new HashBag();

        Histogram volume = new Histogram(1, Param.COMPOUND_VOLUME_MAX, 3);


        SortedMap<String, Object> x = new TreeMap();


        {

            concepts().filter(xx -> !(xx instanceof Functor)).forEach(c -> {

                Term ct = c.term();
                volume.recordValue(ct.volume());
                rootOp.add(ct.op());
                clazz.add(ct.getClass().toString());

                beliefs.accept(c.beliefs().taskCount());
                goals.accept(c.goals().taskCount());
                questions.accept(c.questions().taskCount());
                quests.accept(c.quests().taskCount());
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

        Texts.histogramDecode(volume, "concept volume", 4, x::put);

        Util.toMap(clazz, "concept class", x::put);

        feel.commit(x::put);

        return x;

    }

    /**
     * Reset the system with an empty memory and reset clock.  Event handlers
     * will remain attached but enabled plugins will have been deactivated and
     * reactivated, a signal for them to empty their state (if necessary).
     */
    public void reset() {

        synchronized (exe) {

            boolean running = loop.isRunning();
            float fps = running ? loop.getFPS() : -1;

            stop();

            clear();
            time.clear(this);
            time.reset();

            exe.start(this);

            if (running)
                loop.setFPS(fps);
        }

        logger.info("reset");

    }

    /**
     * the clear event is a signal indicating that any active memory or processes
     * which would interfere with attention should be stopped and emptied.
     * <p>
     * this does not indicate the NAR has stopped or reset itself.
     */
    public void clear() {
        synchronized (exe) {

            eventClear.emit(this);

            logger.info("cleared");
        }
    }

    public final NAR named(String self) {
        return named(Atomic.the(self));
    }

    public final NAR named(Term nextSelf) {
        this.self.updateAndGet((prevSelf) -> {
            return nextSelf;
        });
        return this;
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
                return List.of();
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
    @Nullable
    public final Concept conceptualize(String conceptTerm) throws NarseseException {
        return conceptualize($(conceptTerm));
    }

    /**
     * ask question
     */
    public Task question(String termString) {

        return question($$(termString));
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

    @Nullable
    public Task want(String goalTermString, Tense tense, float freq, float conf) throws NarseseException {
        return want($(goalTermString), tense, freq, conf);
    }

    public Task want(Term goal, Tense tense, float freq) {
        return want(goal, tense, freq, confDefault(GOAL));
    }

    /**
     * desire goal
     */
    @Nullable
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
            believe(priDefault(BELIEF), $(term), time(tense), freq, conf);
        } catch (NarseseException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public long time(Tense tense) {
        return Tense.getRelativeOccurrence(tense, this);
    }

    public NAR believe(String termString, float freq, float conf) {
        return believe(termString, freq, conf, ETERNAL, ETERNAL);
    }

    public NAR believe(String termString, float freq, float conf, long start, long end) {
        believe($$(termString), start, end, freq, conf);
        return this;
    }

    public Task want(String termString) {
        return want($$(termString), true);
    }

    public NAR believe(String... tt) throws NarseseException {

        for (String b : tt)
            believe(b, true);

        return this;
    }

    public NAR believe(String termString, boolean isTrue) throws NarseseException {
        believe($(termString), isTrue);
        return this;
    }

    public Task want(String termString, boolean isTrue) throws NarseseException {
        return want($(termString), isTrue);
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
        input(priDefault(BELIEF), term, BELIEF, start, end, 1f, confDefault(BELIEF));
        return this;
    }

    public Task believe(Term term, long start, long end, float freq, float conf) throws TaskException {
        return believe(priDefault(BELIEF), term, start, end, freq, conf);
    }

    public Task believe(float pri, Term term, long occurrenceTime, float freq, float conf) throws TaskException {
        return believe(pri, term, occurrenceTime, occurrenceTime, freq, conf);
    }

    public Task believe(float pri, Term term, long start, long end, float freq, float conf) throws TaskException {
        return input(pri, term, BELIEF, start, end, freq, conf);
    }

    public Task want(float pri, Term goal, long start, long end, float freq, float conf) throws TaskException {
        return input(pri, goal, GOAL, start, end, freq, conf);
    }

    public Task input(float pri, Term term, byte punc, long occurrenceTime, float freq, float conf) throws TaskException {
        return input(pri, term, punc, occurrenceTime, occurrenceTime, freq, conf);
    }

    public Task input(float pri, Term term, byte punc, long start, long end, float freq, float conf) throws TaskException {

        PreciseTruth tr = Truth.theDithered(freq, c2w(conf), this);
        @Nullable Task z = Task.tryTask(term, punc, tr, (c, truth) -> {
            Task y = NALTask.the(c, punc, truth, time(), start, end, evidence());
            y.pri(pri);
            return y;
        }, false);

        input(z);

        return z;
    }

    /**
     * ¿qué?  que-stion or que-st
     */
    public Task que(Term term, byte questionOrQuest) {
        return que(term, questionOrQuest, ETERNAL);
    }

    /**
     * ¿qué?  que-stion or que-st
     */
    public Task que(Term term, byte punc, long when) {
        return que(term, punc, when, when);
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

    public Task que(Term term, byte punc, long start, long end) {
        assert ((punc == QUESTION) || (punc == QUEST));

        return inputTask(
                NALTask.the(term.unneg(), punc, null, time(), start, end, new long[]{time.nextStamp()}).budget(this)
        );
    }

    /**
     * logs tasks and other budgeted items with a summary exceeding a threshold
     */
    public NAR logPriMin(Appendable out, float priThresh) {
        return log(out, v -> {
            Prioritized b = null;
            if (v instanceof Prioritized) {
                b = ((Prioritized) v);
            } else if (v instanceof Twin) {
                if (((Pair) v).getOne() instanceof Prioritized) {
                    b = (Prioritized) ((Pair) v).getOne();
                }
            }
            return b != null && b.priElseZero() > priThresh;
        });
    }

    @Override
    public final void input(ITask t) {
        in.put(t);
        //exe.input(t);
    }

    @Override
    public final void input(ITask... t) {
        for (ITask x : t)
            in.put(x);
//        switch (t.length) {
//            case 0:
//                break;
//            case 1:
//                input(t[0]);
//                break;
//            default:
//                in.input((Iterator) new ArrayIterator<>(t));
//                break;
//        }
    }

    @Override
    public final void accept(ITask task) {
        input(task);
    }


    public final boolean start(NARPart p) {
        return start(NARPart.id(null, p), p);
    }

    public final boolean add(NARPart p) {
        return add(NARPart.id(null, p), p);
    }


    public final NARPart start(Class<? extends NARPart> p) {
        return start(null, p);
    }

    public final NARPart start(@Nullable Term key, Class<? extends NARPart> p) {
        NARPart pp = null;
        if (key!=null)
            pp = (NARPart) parts.get(key);

        if (pp == null) {
            //HACK
            //TODO make sure this is atomic
            pp = build(p).get();
            if (parts.get(key==null ? NARPart.id(null, pp) : key)==pp) {
                return pp; //already added in its constructor HACK
            }
        } else {
            if (p.isAssignableFrom(pp.getClass()))
                return (NARPart)pp; //ok
            else {
                stop(key);
            }
        }

        if (key!=null ? start(key, pp) : start(pp))
            return pp;
        else
            throw new WTF();
    }



    /**
     * simplified wrapper for use cases where only the arguments of an operation task, and not the task itself matter
     */
    public final void addOpN(String atom, BiConsumer<Subterms, NAR> exe) {
        addOp(Atomic.atom(atom), (task, nar) -> {
            exe.accept(task.term().sub(0).subterms(), nar);
            return null;
        });
    }

    public final Operator addOp1(String atom, BiConsumer<Term, NAR> exe) {
        return addOp(Atomic.atom(atom), (task, nar) -> {

            Subterms ss = task.term().sub(0).subterms();
            if (ss.subs() == 1)
                exe.accept(ss.sub(0), nar);
            return null;
        });
    }

    public final void addOp2(String atom, TriConsumer<Term, Term, NAR> exe) {
        addOp(Atomic.atom(atom), (task, nar) -> {

            Subterms ss = task.term().sub(0).subterms();
            if (ss.subs() == 2)
                exe.accept(ss.sub(0), ss.sub(1), nar);
            return null;
        });
    }


    /**
     * registers an operator
     */
    public final Operator addOp(Atom name, BiFunction<Task, NAR, Task> exe) {
        Operator op = Operator.simple(name, exe);
        memory.set(op);
        return op;
    }

    @Override
    public final int dur() {
        return time.dur();
    }

    /**
     * provides a Random number generator
     */
    @Override
    public final Random random() {
        return random.get();
    }

    @Nullable
    public final Truth truth(Termed concept, byte punc, long when) {
        return truth(concept, punc, when, when);
    }

    @Nullable
    public final BeliefTable table(Termed concept, byte punc) {
        assert (punc == BELIEF || punc == GOAL);
        @Nullable Concept c = conceptualizeDynamic(concept);
        return c != null ? (BeliefTable) c.table(punc) : null;
    }

    /**
     * returns concept belief/goal truth evaluated at a given time
     */
    @Nullable
    public final Truth truth(Termed concept, byte punc, long start, long end) {
        @Nullable BeliefTable table = table(concept, punc);
        return table != null ? table.truth(start, end, concept instanceof Term ? ((Term) concept) : null, null, this) : null;
    }

    @Nullable
    public final Truth beliefTruth(String concept, long when) throws NarseseException {
        return truth($(concept), BELIEF, when);
    }

    @Nullable
    public final Truth goalTruth(String concept, long when) throws NarseseException {
        return truth($(concept), GOAL, when);
    }

    @Nullable
    public final Truth beliefTruth(Termed concept, long when) {
        return truth(concept, BELIEF, when);
    }

    @Nullable
    public final Truth beliefTruth(Termed concept, long start, long end) {
        return truth(concept, BELIEF, start, end);
    }

    @Nullable
    public final Truth goalTruth(Termed concept, long when) {
        return truth(concept, GOAL, when);
    }

    /* Print all statically known events (discovered via reflection)
     *  for this reasoner to a stream
     * */

    @Nullable
    public final Truth goalTruth(Termed concept, long start, long end) {
        return truth(concept, GOAL, start, end);
    }

    /**
     * Exits an iteration loop if running
     */
    public NAR stop() {

        synchronized (exe) {

            pause();

            exe.stop();

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

    public NAR trace(Appendable out, Predicate<String> includeKey) {
        return trace(out, includeKey, null);
    }

    public NAR trace(Appendable out, Predicate<String> includeKey, @Nullable Predicate includeValue) {


        String[] previous = {null};

        eventTask.on((v) -> {

            if (includeValue != null && !includeValue.test(v))
                return;

            try {
                outputEvent(out, previous[0], "task", v);
            } catch (IOException e) {
                logger.error("outputEvent: {}", e.toString());
            }
            previous[0] = "task";
        });
        Topic.all(this, (k, v) -> {

            if (includeValue != null && !includeValue.test(v))
                return;

            try {
                outputEvent(out, previous[0], k, v);
            } catch (IOException e) {
                logger.error("outputEvent: {}", e.toString());
            }
            previous[0] = k;
        }, includeKey);

        return this;
    }

    public NAR trace(Appendable out) {
        return trace(out, k -> true);
    }

    public NAR log() {
        return log(System.out);
    }

    public NAR log(Appendable out) {
        return log(out, null);
    }

    public NAR log(Appendable out, Predicate includeValue) {
        return trace(out, NAR.loggedEvents::contains, includeValue);
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

    /**
     * executes task. if executor is concurrent it will be async, otherwise it will be executed synch (current thread)
     */
    public final void run(Consumer<NAR> t) {
        exe.input(t);
    }

    /**
     * executes task. if executor is concurrent it will be async, otherwise it will be executed synch (current thread)
     */
    public final void run(Runnable t) {
        exe.execute(t);
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
        String x = new String(inputStream.readAllBytes());
        input(x);
        return this;
    }

    public NAR inputAt(long time, String... tt) {

        assert (tt.length > 0);

        runAt(time, () -> {
            List<Task> yy = $.newArrayList(tt.length);
            for (String s : tt) {
                try {
                    yy.addAll(Narsese.tasks(s, this));
                } catch (NarseseException e) {
                    logger.error("{} for: {}", e, s);
                    e.printStackTrace();
                }
            }


            int size = yy.size();
            if (size > 0)
                input(yy.toArray(new Task[size]));

        });

        return this;
    }

    /**
     * TODO use a scheduling using r-tree
     */
    public void inputAt(long when, ITask... x) {
        long now = time();
        if (when <= now) {

            input(x);
        } else {
            runAt(when, () -> input(x));
        }
    }

    /**
     * schedule a task to be executed no sooner than a given NAR time
     * @return
     */
    public final ScheduledTask runAt(long whenOrAfter, Runnable then) {
        return runAt(new AtTime(whenOrAfter, then));
    }

    public final ScheduledTask runAt(ScheduledTask t) {
        if (t.start() <= time())
            exe.execute(t); //immediate
        else
            time.runAt(t);
        return t;
    }

    /**
     * adds a task to the queue of task which will be executed in batch
     * after the end of the current frame before the next frame.
     */
    public final void runLater(Runnable t) {
        runAt(time() + 1, t);
    }

    /**
     * tasks in concepts
     */

    public Stream<Task> tasks(boolean includeConceptBeliefs, boolean includeConceptQuestions,
                              boolean includeConceptGoals, boolean includeConceptQuests) {
        return concepts().flatMap(c ->
                c.tasks(includeConceptBeliefs, includeConceptQuestions, includeConceptGoals, includeConceptQuests));
    }

    public void tasks(boolean includeConceptBeliefs, boolean includeConceptQuestions, boolean includeConceptGoals,
                      boolean includeConceptQuests, BiConsumer<Concept, Task> each) {
        concepts().forEach(c ->

                c.tasks(includeConceptBeliefs,
                        includeConceptQuestions,
                        includeConceptGoals,
                        includeConceptQuests).forEach(t -> each.accept(c, t))
        );
    }

    public Stream<Task> tasks() {
        return tasks(true, true, true, true);
    }

    /**
     * resolves a target or concept to its currrent Concept
     */
    @Nullable
    public final Concept concept(Termed x) {
        return concept(x, false);
    }

    @Nullable
    public final Concept concept(String term) {
        return concept($$(term), false);
    }

    /**
     * resolves a target to its Concept; if it doesnt exist, its construction will be attempted
     */
    @Nullable
    public final Concept conceptualize(/**/ Termed termed) {
        return concept(termed, true);
    }

    @Nullable
    public final Concept concept(/**/Termed x, boolean createIfMissing) {
//        if (Param.DEBUG) {
//            int v = x.target().volume();
//            int max = termVolumeMax.intValue();
//            if (v > max) {
//                //return null; //too complex
//                logger.warn("too complex for conceptualization ({}/{}): " + x, v, max);
//            }
//        }

        return memory.concept(x, createIfMissing);
    }

    public final Stream<Concept> concepts() {
        return memory.stream()/*.filter(Concept.class::isInstance)*/.map(Concept.class::cast);
    }

    /**
     * warning: the condition will be tested each cycle so it may affect performance
     */
    public NAR stopIf(BooleanSupplier stopCondition) {
        onCycle(n -> {
            if (stopCondition.getAsBoolean())
                stop();
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

    public final Off onCycle(Runnable each) {
        return onCycle((ignored) -> each.run());
    }

    /**
     * avoid using lambdas with this, instead use an interface implementation of the class that is expected to be garbage collected
     */
    public final Off onCycleWeak(Consumer<NAR> each) {
        return eventCycle.onWeak(each);
    }

    public final Off onTask(Consumer<Task> listener) {
        return eventTask.on(listener);
    }

    public final Off onTask(Consumer<Task> listener, byte... punctuations) {
        return eventTask.on(listener, punctuations);
    }

    public NAR trace() {
        trace(System.out);
        return this;
    }

    /**
     * if this is an Iterable<Task> , it can be more efficient to use the inputTasks method to bypass certain non-NALTask conditions
     */
    public void input(Iterable<? extends ITask> tasks) {
        //exe.input(tasks);
        tasks.forEach(in::put);
    }

    public final void input(Stream<? extends ITask> tasks) {
        //exe.input(tasks.filter(Objects::nonNull));
        tasks.forEach(in::put);
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
    public final <Permanent_Concept extends PermanentConcept> Permanent_Concept add(Permanent_Concept c) {

        Termed existing = memory.remove(c.term());
        if ((existing != null)) {
            if (existing != c) {

                if (!(c instanceof PermanentConcept)) {
                    throw new RuntimeException("concept already indexed for target: " + c.term());
                }
            }
        }

        memory.set(c);

        conceptBuilder.start(c);

//        if (c instanceof Conceptor) {
//            conceptBuilder.on((Conceptor) c);
//        }

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
        return outputBinary(f, append, ((Task t) -> t));
    }

    public final NAR outputBinary(File f, boolean append, Predicate<Task> each) throws IOException {
        return outputBinary(f, append, (Task t) -> each.test(t) ? t : null);
    }

    public NAR outputBinary(File f, boolean append, Function<Task, Task> each) throws IOException {
        FileOutputStream f1 = new FileOutputStream(f, append);
        OutputStream ff = new GZIPOutputStream(f1, IO.gzipWindowBytesDefault);
        outputBinary(ff, each);
        return this;
    }

    public final NAR outputBinary(OutputStream o) {
        return outputBinary(o, (Task x) -> x);
    }

    public final NAR outputBinary(OutputStream o, Predicate<Task> filter) {
        return outputBinary(o, (Task x) -> filter.test(x) ? x : null);
    }

    /**
     * byte codec output of matching concept tasks (blocking)
     * <p>
     * the each function allows transforming each task to an optional output form.
     * if this function returns null it will not output that task (use as a filter).
     */
    public NAR outputBinary(OutputStream o, Function<Task, Task> each) {

        Util.time(logger, "outputBinary", () -> {

            DataOutputStream oo = new DataOutputStream(o);

            MutableInteger total = new MutableInteger(0), wrote = new MutableInteger(0);

            DynBytes d = new DynBytes(128);

            tasks().map(each).filter(Objects::nonNull).distinct().forEach(x -> {

                total.increment();

                try {
                    byte[] b = IO.taskToBytes(x, d);
                    oo.write(b);
                    wrote.increment();
                } catch (IOException e) {
                    if (Param.DEBUG)
                        throw new RuntimeException(e);
                    else
                        logger.warn("{}", e);
                }

            });

            logger.info("{} output {}/{} tasks ({} bytes uncompressed)", o, wrote, total, oo.size());


            try {
                oo.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return this;
    }

    public NAR outputText(OutputStream o, Function<Task, Task> each) {


        PrintStream ps = new PrintStream(o);

        MutableInteger total = new MutableInteger(0);

        StringBuilder sb = new StringBuilder();
        tasks().map(each).filter(Objects::nonNull).forEach(x -> {

            total.increment();

            if (x.truth() != null && x.conf() < confMin.floatValue())
                return;

            sb.setLength(0);
            ps.println(x.appendTo(sb, true));
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

    public NAR output(File o, Function<Task, Task> f) throws FileNotFoundException {
        return outputBinary(new BufferedOutputStream(new FileOutputStream(o), IO.outputBufferBytesDefault), f);
    }

    public NAR output(OutputStream o, boolean binary) {
        if (binary) {
            return outputBinary(o, (Task x) -> x.isDeleted() ? null : x);
        } else {
            return outputText(o, x -> x.isDeleted() ? null : x);
        }
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
        return self.get();
    }

    /**
     * strongest matching belief for the target time
     */
    @Nullable
    public Task belief(Termed c, long start, long end) {
        return answer(c, BELIEF, start, end);
    }

    public final Task belief(String c, long when) throws NarseseException {
        return belief($(c), when);
    }

    public final Task belief(Termed c, long when) {
        return belief(c, when, when);
    }

    public final Task belief(Term c) {
        return belief(c, time());
    }


    @Nullable
    public final Task answer(Term c, byte punc, long when) {
        return answer(c, punc, when, when);
    }

    @Nullable
    public Task answer(Termed t, byte punc, long start, long end) {
        assert (punc == BELIEF || punc == GOAL);

        boolean negate;
        Term tt = t.term();
        if (tt.op() == NEG) {
            negate = true;
            t = tt = tt.unneg();
        } else {
            negate = false;
        }

        Concept concept = conceptualizeDynamic(t);
        if (!(concept instanceof TaskConcept))
            return null;

        Task answer = concept.table(punc).matchExact(start, end,
                tt, null, dur(), this);

//        if (answer != null && !answer.isDeleted())
//            input(answer);

        return Task.negIf(answer, negate);
    }

    public SortedMap<String, Object> stats(Appendable out) {

        SortedMap<String, Object> stat = stats();
        stat.forEach((k, v) -> {
            try {
                out.append(k.replace(" ", "/")).append(" \t ").append(v.toString()).append('\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
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
        tasks().filter(x -> Longs.contains(x.stamp(), stampComponent)).forEach(Task::delete);
    }

    /**
     * computes an evaluation amplifier factor, in range 0..2.0.
     * VALUE -> AMP
     * -Infinity -> amp=0
     * 0     -> amp=1
     * +Infinity -> amp=2
     */
    public float amp(short[] effect) {
        return control.amp(effect);
    }

    public final float amp(Task task) {
        return control.amp(task);
    }

    public float value(short[] effect) {
        return control.value(effect);
    }


    /**
     * creates a new evidence stamp (1-element array)
     */
    public final long[] evidence() {
        return new long[]{time.nextStamp()};
    }

    public Cause newCause(Object name) {
        return control.newCause(name);
    }

    /**
     * automatically adds the cause id to each input
     */
    public CauseChannel<ITask> newChannel(Object id) {
        return control.newChannel(id);
    }

    public <C extends Cause> C newCause(ShortToObjectFunction<C> idToChannel) {
        return control.newCause(idToChannel);
    }


    public Stream<Part<NAR>> plugins() {
        return this.partStream();
    }

    public void conceptualize(Term term, Consumer<Concept> with) {


        Concept x = conceptualize(term);
        if (x != null) {
            with.accept(x);
        } else {

        }

    }


    /**
     * invokes any pending tasks without advancing the clock
     */
    public final NAR synch() {
        synchronized (exe) {
            time.synch(this);
            exe.synch();
        }
        return this;
    }

    public final boolean pause() {
        if (loop.stop()) {
            synch();
            return true;
        }
        return false;
    }

    /**
     * conceptualize a target if dynamic truth is possible; otherwise return concept if exists
     */
    public final Concept conceptualizeDynamic(Termed concept) {

        Concept x = concept(concept);
        if (x != null)
            return x;

        Term ct = concept.term();

        if (ct.volume() > termVolumeMax.intValue())
            return null; //too complex to analyze for dynamic

        if (ct instanceof Compound && ConceptBuilder.dynamicModel((Compound) ct) != null) {
            //try conceptualizing the dynamic

            if (Param.DYNAMIC_CONCEPT_TRANSIENT) {

                //create temporary dynamic concept
                Concept c = conceptBuilder.construct(ct);
                if (c != null)
                    c.delete(this); //flyweight start deleted and unallocated (in-capacit-ated) since it isnt actually in memory

                return c;
            } else {
                //permanent dynamic concept
                return conceptualize(concept);
            }

        }

        return null;
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

//    public final Task is(Term content, long start, long end) {
//        return answer(content, BELIEF, start, end);
//    }

//    public final Task wants(Term content, long start, long end) {
//        return answer(content, GOAL, start, end);
//    }

    /**
     * stream of all (explicitly and inferrable) internal events
     */
    public Stream<? extends InternalEvent> at() {
        return Streams.concat(
                //TODO Streams.stream(eventTask).map(t -> ), // -> AtTask events
                Streams.stream(eventCycle).map(AtCycle::new),
                Streams.stream(eventClear).map(AtClear::new),
                this.partStream()
                        .map((s) -> ((NARPart) s).event()).filter(Objects::nonNull),
                time.events()
                        .filter(t -> !(t instanceof DurLoop.AtDur)) //HACK (these should already be included in service's events)
//            causes.stream(),
        );
    }

    /**
     * map of internal events organized by category
     */
    public final Map<Term, List<InternalEvent>> atMap() {
        return at().collect(Collectors.groupingBy(InternalEvent::category));
    }

    /**
     * stream of all registered services
     */
    public final <X> Stream<X> plugins(Class<? extends X> nAgentClass) {
        return plugins().filter(x -> nAgentClass.isAssignableFrom(x.getClass()))
                .map(x -> (X) x);
    }


    public final DurLoop onDur(Runnable on) {
        DurLoop.DurRunnable r = new DurLoop.DurRunnable(on);
        start(r);
        return r;
    }

    public final DurLoop onDur(Consumer<NAR> on) {
        DurLoop.DurNARConsumer r = new DurLoop.DurNARConsumer(on);
        start(r);
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
}
