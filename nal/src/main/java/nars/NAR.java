package nars;


import com.google.common.primitives.Longs;
import jcog.Services;
import jcog.Util;
import jcog.event.ListTopic;
import jcog.event.On;
import jcog.event.Topic;
import jcog.exe.Cycler;
import jcog.util.ArrayIterator;
import jcog.list.FasterList;
import jcog.math.MutableInteger;
import jcog.pri.Pri;
import jcog.pri.Prioritized;
import jcog.util.TriConsumer;
import nars.Narsese.NarseseException;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.concept.builder.ConceptBuilder;
import nars.concept.state.ConceptState;
import nars.control.*;
import nars.exe.Exec;
import nars.index.term.ConceptIndex;
import nars.op.Operator;
import nars.subterm.Subterms;
import nars.table.BeliefTable;
import nars.task.ITask;
import nars.task.NALTask;
import nars.task.util.InvalidTaskException;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.time.Tense;
import nars.time.Time;
import nars.truth.DiscreteTruth;
import nars.truth.Truth;
import nars.util.Cycles;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.ShortCountsHistogram;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static nars.$.$;
import static nars.Op.*;
import static nars.term.Functor.f;
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
public class NAR extends Param implements Consumer<ITask>, NARIn, NAROut, Cycles<NAR>, Cycler {



    protected Logger logger;

    static final Set<String> logEvents = Set.of("eventTask");
    static final String VERSION = "NARchy v?.?";

    public final Exec exe;
    public final transient Topic<NAR> eventClear = new ListTopic<>();
    public final transient Topic<NAR> eventCycle = new ListTopic<>();
    public final transient Topic<Task> eventTask = new ListTopic<>();

    public final Services<Term, NAR> services;

    public final Emotion emotion;

    public final Time time;

    public final ConceptIndex concepts;
    public final NARLoop loop = new NARLoop(this);
    /**
     * table of values influencing reasoner heuristics
     */
    public final FasterList<Cause> causes = new FasterList(256);

    protected final Random random;


    private final AtomicReference<Term> self;


    public NAR(@NotNull ConceptIndex concepts, @NotNull Exec exe, @NotNull Time time, @NotNull Random rng, @NotNull ConceptBuilder conceptBuilder) {

        this.random = rng;

        this.concepts = concepts;

        this.time = time;
        time.reset();

        this.exe = exe;

        self = new AtomicReference<>(null);
        setSelf(Param.randomSelf());

        services = new Services(this, exe);

        this.emotion = new Emotion(this);



        newCauseChannel("input"); //generic non-self source of input

        //if (concepts.nar == null) { //HACK dont reinitialize if already initialized, for sharing
            concepts.init(this);
            Builtin.init(this);
        //}


        exe.start(this);
    }

    static void outputEvent(Appendable out, String previou, String chan, Object v) throws IOException {
//        //indent each cycle
//        if (!"eventCycle".equals(chan)) {
//            out.append("  ");
//        }

        if (!chan.equals(previou)) {
            out
                    //.append(ANSI.COLOR_CONFIG)
                    .append(chan)
                    //.append(ANSI.COLOR_RESET )
                    .append(": ");
            //previou = chan;
        } else {
            //indent
            for (int i = 0; i < chan.length() + 2; i++)
                out.append(' ');
        }

        if (v instanceof Object[]) {
            v = Arrays.toString((Object[]) v);
        } else if (v instanceof Task) {
            Task tv = ((Task) v);
            float tvp = tv.priElseZero();
            v = ansi()
                    //.a(tv.originality() >= 0.33f ?
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
     * creates a snapshot statistics object
     * TODO extract a Method Object holding the snapshot stats with the instances created below as its fields
     */
    public SortedMap<String, Object> stats() {

        LongSummaryStatistics beliefs = new LongSummaryStatistics();
        LongSummaryStatistics goals = new LongSummaryStatistics();
        LongSummaryStatistics questions = new LongSummaryStatistics();
        LongSummaryStatistics quests = new LongSummaryStatistics();
        Histogram termlinkCount = new Histogram(1);
        Histogram tasklinkCount = new Histogram(1);
        //Frequency complexity = new Frequency();
        HashBag clazz = new HashBag();
        HashBag policy = new HashBag();
        HashBag rootOp = new HashBag();

        ShortCountsHistogram volume = new ShortCountsHistogram(2);

        //AtomicInteger i = new AtomicInteger(0);


        //        LongSummaryStatistics termlinksCap = new LongSummaryStatistics();
        //        LongSummaryStatistics tasklinksCap = new LongSummaryStatistics();

        SortedMap<String, Object> x = new TreeMap();

        synchronized (exe) {

            concepts().filter(xx -> !(xx instanceof Functor)).forEach(c -> {


                //complexity.addValue(c.complexity());
                volume.recordValue(c.volume());
                rootOp.add(c.op());
                clazz.add(c.getClass().toString());

                ConceptState p = c.state();
                policy.add(p != null ? p.toString() : "null");

                //termlinksCap.accept(c.termlinks().capacity());
                termlinkCount.recordValue(c.termlinks().size());

                //tasklinksCap.accept(c.tasklinks().capacity());
                tasklinkCount.recordValue(c.tasklinks().size());

                beliefs.accept(c.beliefs().size());
                goals.accept(c.goals().size());
                questions.accept(c.questions().size());
                quests.accept(c.quests().size());
            });


            //x.put("time real", new Date());
            if (loop.isRunning()) {
                loop.stats("loop", x);
            }

            x.put("time", time());


            //x.put("term index", terms.summary());

            x.put("concept count", concepts.size());
        }

        x.put("belief count", ((double) beliefs.getSum()));
        x.put("goal count", ((double) goals.getSum()));

        Util.decode(tasklinkCount, "tasklink count", 4, x::put);
        //x.put("tasklink usage", ((double) tasklinkCount.getTotalCount()) / tasklinksCap.getSum());
        x.put("tasklink total", ((double) tasklinkCount.getTotalCount()));
        Util.decode(termlinkCount, "termlink count", 4, x::put);
        //x.put("termlink usage", ((double) termlinkCount.getTotalCount()) / termlinksCap.getSum());
        x.put("termlink total", ((double) termlinkCount.getTotalCount()));

        //        DoubleSummaryStatistics pos = new DoubleSummaryStatistics();
        //        DoubleSummaryStatistics neg = new DoubleSummaryStatistics();
        //        causes.forEach(c -> pos.accept(c.pos()));
        //        causes.forEach(c -> neg.accept(c.neg()));
        //        x.put("value count", pos.getCount());
        //        x.put("value pos mean", pos.getAverage());
        //        x.put("value pos min", pos.getMin());
        //        x.put("value pos max", pos.getMax());
        //        x.put("value neg mean", neg.getAverage());
        //        x.put("value neg min", neg.getMin());
        //        x.put("value neg max", neg.getMax());


        //x.put("volume mean", volume.);
        //
        //        x.put("termLinksCapacity", termlinksCap);
        //        x.put("taskLinksUsed", tasklinksUsed);
        //        x.put("taskLinksCapacity", tasklinksCap);

        Util.toMap(policy, "concept state", x::put);

        Util.toMap(rootOp, "concept op", x::put);

        Util.decode(volume, "concept volume", 4, x::put);

        Util.toMap( clazz, "concept class", x::put);

        x.put("term cache (eternal)", Op.cache.summary());
        x.put("term cache (temporal)", Op.cacheTemporal.summary());

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

            stop();

            time.clear(this);
            time.reset();

            exe.start(this);

            logger.info("reset");

        }

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


    public final void setSelf(String self) {
        setSelf(Atomic.the(self));
    }


    public final void setSelf(Term self) {
        synchronized (exe) {
            this.self.set(self);
            this.logger =
                    LoggerFactory.getLogger("NAR:" + self);
        }
    }

    /**
     * parses one and only task
     */
    
    public <T extends Task> T inputTask(String taskText) throws Narsese.NarseseException {
        return (T) inputTask(Narsese.the().task(taskText, (this)));
    }

    
    public List<Task> input(String text) throws NarseseException, InvalidTaskException {
        List<Task> l = Narsese.the().tasks(text, this);
        input(l);
        return l;
    }

    /**
     * gets a concept if it exists, or returns null if it does not
     */
    @Nullable
    public final Concept conceptualize(String conceptTerm) throws NarseseException {
        return conceptualize($.$(conceptTerm));
    }

    /**
     * ask question
     */
    public Task question( String termString) throws NarseseException {
        //TODO remove '?' if it is attached at end
        return question($.$(termString));
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
    public Task goal(String goalTermString, Tense tense, float freq, float conf) throws NarseseException {
        return goal($(goalTermString), tense, freq, conf);
    }

    public Task goal(Term goal, Tense tense, float freq) {
        return goal(goal, tense, freq, confDefault(GOAL));
    }

    /**
     * desire goal
     */
    @Nullable
    public Task goal( Term goalTerm,  Tense tense, float freq, float conf) {
        return goal(
                priDefault(GOAL),
                goalTerm, time(tense), freq, conf);
    }

    
    public Task believe( Term term,  Tense tense, float freq, float conf) {
        return believe(term, time(tense), freq, conf);
    }

    
    public Task believe( Term term, long when, float freq, float conf) {
        return believe(priDefault(BELIEF), term, when, freq, conf);
    }

    
    public Task believe( Term term,  Tense tense, float freq) {
        return believe(term, tense, freq, confDefault(BELIEF));
    }

    
    public Task believe( Term term, long when, float freq) {
        return believe(term, when, freq, confDefault(BELIEF));
    }

    
    public Task believe( Term term, float freq, float conf) {
        return believe(term, Tense.Eternal, freq, conf);
    }

    
    public Task goal( Term term, float freq, float conf) {
        return goal(term, Tense.Eternal, freq, conf);
    }

    
    public NAR believe( String term,  Tense tense, float freq, float conf) {
        try {
            believe(priDefault(BELIEF), $.$(term), time(tense), freq, conf);
        } catch (NarseseException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public long time(Tense tense) {
        return Tense.getRelativeOccurrence(tense, this);
    }

    public NAR believe(String termString, float freq, float conf) throws NarseseException {
        believe($.$(termString), freq, conf);
        return this;
    }

    public Task goal(String termString) {
        try {
            return goal($.$(termString), true);
        } catch (NarseseException e) {
            throw new RuntimeException(e);
        }
    }

    public NAR believe(String... tt) throws NarseseException {

        for (String b : tt)
            believe(b, true);

        return this;
    }

    
    public NAR believe( String termString, boolean isTrue) throws NarseseException {
        believe($.$(termString), isTrue);
        return this;
    }

    
    public Task goal( String termString, boolean isTrue) throws NarseseException {
        return goal($.$(termString), isTrue);
    }

    
    public Task believe( Term term) {
        return believe(term, true);
    }

    
    public Task believe( Term term, boolean trueOrFalse) {
        return believe(term, trueOrFalse, confDefault(BELIEF));
    }

    
    public Task goal( Term term) {
        return goal(term, true);
    }

    
    public Task goal( Term term, boolean trueOrFalse) {
        return goal(term, trueOrFalse, confDefault(BELIEF));
    }

    
    public Task believe( Term term, boolean trueOrFalse, float conf) {
        return believe(term, trueOrFalse ? 1.0f : 0f, conf);
    }

    
    public Task goal( Term term, boolean trueOrFalse, float conf) {
        return goal(term, trueOrFalse ? 1.0f : 0f, conf);
    }

    @Nullable
    public Task believe(float pri,  Term term, long occurrenceTime, float freq, float conf) throws InvalidTaskException {
        return input(pri, term, BELIEF, occurrenceTime, freq, conf);
    }

    @Nullable
    public Task goal(float pri,  Term goal, long when, float freq, float conf) throws InvalidTaskException {
        return input(pri, goal, GOAL, when, when, freq, conf);
    }

    @Nullable
    public Task goal(float pri,  Term goal, long start, long end, float freq, float conf) throws InvalidTaskException {
        return input(pri, goal, GOAL, start, end, freq, conf);
    }

    @Nullable
    public Task input(float pri,  Term term, byte punc, long occurrenceTime, float freq, float conf) throws InvalidTaskException {
        return input(pri, term, punc, occurrenceTime, occurrenceTime, freq, conf);
    }

    @Nullable
    public Task input(float pri, Term term, byte punc, long start, long end, float freq, float conf) throws InvalidTaskException {

        ObjectBooleanPair<Term> b = Task.tryContent(term, punc, false);

        term = b.getOne();
        if (b.getTwo())
            freq = 1f - freq;

        DiscreteTruth tr = new DiscreteTruth(freq, conf, confMin.floatValue());

        Task y = new NALTask(term, punc, tr, time(), start, end, new long[]{time.nextStamp()});
        y.priSet(pri);

        input(y);

        return y;
    }

    /**
     * ¿qué?  que-stion or que-st
     */
    public Task que(@NotNull Term term, byte questionOrQuest) {
        return que(term, questionOrQuest, ETERNAL);
    }


    /**
     * ¿qué?  que-stion or que-st
     */
    public Task que(@NotNull Term term, byte punc, long when) {


        //TODO use input method like believe uses which avoids creation of redundant Budget instance
        assert ((punc == QUESTION) || (punc == QUEST)); //throw new RuntimeException("invalid punctuation");

        return inputTask(
                new NALTask(term.unneg(), punc, null,
                        time(), when, when,
                        new long[]{time.nextStamp()}
                ).budget(this)
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
            return b != null && b.pri() > priThresh;
        });
    }

    public NAR logWhen(Appendable out, boolean past, boolean present, boolean future) {

        if (past == true && present == true && future == true)
            return log(out);

        return log(out, v -> {
            if (v instanceof Task) {
                Task t = (Task) v;
                long now = time();
                return
                    (past && t.isBefore(now)) ||
                    (present && t.isAfter(now)) ||
                    (future && t.isDuring(now));
            }
            return false;
        });
    }

    /**
     * main task entry point
     */
    @Override
    public final void input(ITask... t) {
        if (t == null)
            return;
        switch (t.length) {
            case 0:
                break;
            case 1:
                exe.execute(t[0]);
                break;
            default:
                exe.execute((Iterator) new ArrayIterator<>(t));
                break;

        }
    }

    @Override
    public final void accept(ITask task) {
        input(task);
    }

    /**
     * asynchronously adds the service
     */
    public final void on(NARService s) {
        runLater(() -> services.add(s.term(), s, true));
    }
    public final void off(NARService s) {
        runLater(() -> services.add(s.term(), s, false));
    }


    /**
     * simplified wrapper for use cases where only the arguments of an operation task, and not the task itself matter
     */
    public final void onOpN(@NotNull String atom, @NotNull BiConsumer<Subterms, NAR> exe) {
        onOp(atom, (task, nar) -> {
            exe.accept(task.term().sub(0).subterms(), nar);
            return null;
        });
    }

    public final void onOp1(@NotNull String atom, @NotNull BiConsumer<Term, NAR> exe) {
        onOp(atom, (task, nar) -> {
            Subterms ss = task.term().sub(0).subterms();
            if (ss.subs() == 1)
                exe.accept(ss.sub(0), nar);
            return null;
        });
    }

    public final void onOp2(@NotNull String atom, @NotNull TriConsumer<Term, Term, NAR> exe) {
        onOp(atom, (task, nar) -> {
            Subterms ss = task.term().sub(0).subterms();
            if (ss.subs() == 2)
                exe.accept(ss.sub(0), ss.sub(1), nar);
            return null;
        });
    }

//    /**
//     * simplified wrapper which converts the term result of the supplied lambda to a log event
//     */
//    public final void onOpLogged(@NotNull String a, @NotNull BiFunction<Task, NAR, Term> exe) {
//        onOp(a, (BiFunction<Task, NAR, Task>) (t, n) -> Operator.log(exe.apply(t, n)));
//    }

    /**
     * registers an operator
     */
    public final void onOp(@NotNull String a, @NotNull BiConsumer<Task, NAR> exe) {
        onOp(a, (task, nar) -> {
            exe.accept(task, nar);
            return null;
        });
    }

    /**
     * registers an operator
     */
    public final Operator onOp(@NotNull String a, @NotNull BiFunction<Task, NAR, Task> exe) {
        return onOp((Atom) $.the(a), exe);
    }

    /**
     * registers an operator
     */
    public final Operator onOp(@NotNull Atom a, @NotNull BiFunction<Task, NAR, Task> exe) {
        Operator op;
        concepts.set(op = new Operator(a, exe, this));
        return op;
    }

    public final int dur() {
        return time.dur();
    }

    /**
     * provides a Random number generator
     */
    public final Random random() {
        return random;
    }

    @Nullable
    public final Truth truth(Termed concept, byte punc, long when) {
        return truth(concept, punc, when, when);
    }

    @Nullable
    public final BeliefTable truths(Termed concept, byte punc) {
        assert (punc == BELIEF || punc == GOAL);
        @Nullable Concept c = conceptualize(concept);
        if (c == null)
            return null;
        return (BeliefTable) c.table(punc);
    }

    /**
     * returns concept belief/goal truth evaluated at a given time
     */
    @Nullable
    public Truth truth(Termed concept, byte punc, long start, long end) {

        assert (concept.op().conceptualizable) : "asking for truth of unconceptualizable: " + concept; //filter NEG etc

        @Nullable Concept c = conceptualize(concept);
        return c != null ? ((BeliefTable) c.table(punc)).truth(start, end, this) : null;
    }

    @Nullable
    public final Truth beliefTruth(String concept, long when) throws NarseseException {
        return truth(conceptualize(concept), BELIEF, when);
    }

    @Nullable
    public final Truth goalTruth(String concept, long when) throws NarseseException {
        return truth(conceptualize(concept), GOAL, when);
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

    @Nullable
    public final Truth goalTruth(Termed concept, long start, long end) {
        return truth(concept, GOAL, start, end);
    }

    /**
     * Exits an iteration loop if running
     */
    public NAR stop() {

        synchronized (exe) {

            services.stop();

            loop.stop();

            time.synch(this);

            exe.stop();

        }

        return this;
    }

    private final AtomicBoolean busy = new AtomicBoolean(false);

    /**
     * steps 1 frame forward. cyclesPerFrame determines how many cycles this frame consists of
     */
    @Override
    public final void run() {
        if (!busy.compareAndSet(false, true))
            return; //already in cycle

        try {

            emotion.cycle();

            time.cycle(this);

            if (exe.concurrent()) {
                eventCycle.emitAsync(this, exe, ()->busy.set(false) );
            } else {
                eventCycle.emit(this);
                busy.set(false);
            }

        } catch (Throwable t) {
            busy.set(false);
            throw t;
        }
    }


    public NAR trace(@NotNull Appendable out, Predicate<String> includeKey) {
        return trace(out, includeKey, null);
    }

    /* Print all statically known events (discovered via reflection)
     *  for this reasoner to a stream
     * */

    public NAR trace(Appendable out, Predicate<String> includeKey, @Nullable Predicate includeValue) {


        String[] previous = {null};

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
        return trace(out, NAR.logEvents::contains, includeValue);
    }


    /**
     * Runs until stopped, at a given delay period between frames (0= no delay). Main loop
     *
     * @param initialDelayMS in milliseconds
     */
    @Override
    @NotNull
    public final NARLoop startPeriodMS(int initialDelayMS) {
        loop.setPeriodMS(initialDelayMS);
        return loop;
    }

    /** enqueues a task (avoids the scheduler, so may be executed before the end of the cycle) */
    public final void run(Consumer<NAR> t) {
        exe.execute(t);
    }
    /** enqueues a task (avoids the scheduler, so may be executed before the end of the cycle) */
    public final void run(Runnable t) {
        exe.execute(t);
    }

    /**
     * adds a task to the queue of task which will be executed in batch
     * after the end of the current frame before the next frame.
     */
    public final void runLater(Runnable t) {
        time.at(time(), t);
    }

    /**
     * adds a task to the queue of task which will be executed in batch
     * after the end of the current frame before the next frame.
     */
    public final void runLater(Consumer<NAR> t) {
        time.at(time(), t);
    }

    @NotNull
    @Override
    public String toString() {
        return self() + ":" + getClass().getSimpleName();
    }

    @NotNull
    public NAR input(@NotNull String... ss) throws NarseseException {
        for (String s : ss)
            input(s);
        return this;
    }

    public NAR inputNarsese(@NotNull URL url) throws IOException, NarseseException {
        return inputNarsese(url.openStream());
    }

    public NAR inputNarsese(@NotNull InputStream inputStream) throws IOException, NarseseException {
        String x = new String(inputStream.readAllBytes());
        input(x);
        return this;
    }

    /**
     * TODO this needs refactoring to use a central scheduler
     */
    public NAR inputAt(long time, @NotNull String... tt) {

        assert (tt.length > 0);

        at(time, () -> {
            List<Task> yy = $.newArrayList(tt.length);
            for (String s : tt) {
                try {
                    yy.addAll(Narsese.the().tasks(s, this));
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
    public void inputAt(long when, @NotNull ITask... x) {
        long now = time();
        if (when <= now) {
            //past or current cycle
            input(x);
        } else {
            at(when, () -> input(x));
        }
    }

    /**
     * schedule a task to be executed no sooner than a given NAR time
     */
    public final void at(long whenOrAfter, Runnable then) {
        if (whenOrAfter <= time())
            run(then);
        else
            time.at(whenOrAfter, then);
    }

    /**
     * tasks in concepts
     */
    @NotNull
    public Stream<Task> tasks(boolean includeConceptBeliefs, boolean includeConceptQuestions, boolean includeConceptGoals, boolean includeConceptQuests) {
        return concepts().flatMap(c -> c.tasks(includeConceptBeliefs, includeConceptQuestions, includeConceptGoals, includeConceptQuests));
    }

    @NotNull
    public Stream<Task> tasks() {
        return tasks(true, true, true, true);
    }

    /**
     * resolves a term or concept to its currrent Concept
     */
    @Nullable
    public final Concept concept(Termed termed) {
        return concept(termed, false);
    }

    /**
     * resolves a term to its Concept; if it doesnt exist, its construction will be attempted
     */
    @Nullable
    public final Concept conceptualize(/*@NotNull*/ Termed termed) {
        return concept(termed, true);
    }

    @Nullable
    public final Concept concept(/*@NotNull */Termed x, boolean createIfMissing) {
        return concepts.concept(x, createIfMissing);
    }


//    /**
//     * activate the concept and other features (termlinks, etc)
//     *
//     * @param link whether to activate termlinks recursively
//     */
//    @Nullable
//    public abstract Concept activate(@NotNull Termed<?> termed, @Nullable Activation activation);
//
//    @Nullable
//    final public Concept activate(@NotNull Termed<?> termed, @NotNull Budgeted b) {
//        return activate(termed, new Activation(b, 1f));
//    }

    public Stream<Activate> conceptsActive() {
        return exe.active();
    }

    public Stream<Concept> concepts() {
        return concepts.stream()/*.filter(Concept.class::isInstance)*/.map(Concept.class::cast);
    }

    /**
     * warning: the condition will be tested each cycle so it may affect performance
     */
    @NotNull
    public NAR stopIf(@NotNull BooleanSupplier stopCondition) {
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
    @Override
    @NotNull
    public final On onCycle(@NotNull Consumer<NAR> each) {
        return eventCycle.on(each);
    }

    /**
     * avoid using lambdas with this, instead use an interface implementation of the class that is expected to be garbage collected
     */
    @NotNull
    public final On onCycleWeak(@NotNull Consumer<NAR> each) {
        return eventCycle.onWeak(each);
    }

    @NotNull
    public final On onCycle(@NotNull Runnable each) {
        return onCycle((ignored) -> each.run());
    }

    @NotNull
    @Deprecated
    public NAR eachCycle(@NotNull Consumer<NAR> each) {
        onCycle(each);
        return this;
    }

    @NotNull
    public NAR trace() {
        trace(System.out);
        return this;
    }

    public void input(Iterable<? extends ITask> tasks) {
        if (tasks == null) return;
        exe.execute(tasks);
    }

    public final void input(Stream<? extends ITask> tasks) {
        //if (tasks == null) return;
        exe.execute(tasks.filter(Objects::nonNull));
    }

    @Override
    public final boolean equals(Object obj) {
        //TODO compare any other stateful values from NAR class in addition to Memory
        return this == obj;
    }

    @NotNull
    public On onTask(@NotNull Consumer<Task> o) {
        return eventTask.on(o);
    }

    public @NotNull NAR believe(@NotNull Term c, @NotNull Tense tense) {
        believe(c, tense, 1f);
        return this;
    }

    /**
     * activate/"turn-ON"/install a concept in the index and activates it, used for setup of custom concept implementations
     * implementations should apply active concept capacity policy
     */
    @NotNull
    public final Concept on(@NotNull Concept c) {

        Concept existing = concept(c);
        if ((existing != null) && (existing != c))
            throw new RuntimeException("concept already indexed for term: " + c.term());

        c.state(conceptBuilder.awake());
        concepts.set(c);

        return c;
    }

    /**
     * registers a term rewrite functor
     */
    @NotNull
    public final Concept on(@NotNull String termAtom, @NotNull Function<Subterms, Term> f) {
        return on(f(termAtom, f));
    }
    @NotNull
    public final Concept on1(@NotNull String termAtom, @NotNull Function<Term, Term> f) {
        return on(termAtom, (Subterms s)->s.subs()==1 ? f.apply(s.sub(0)) : null);
    }

    public final long time() {
        return time.now();
    }

    static final int FILE_STREAM_BUFFER_SIZE = 16 * 1024;

    public NAR inputBinary(@NotNull File input) throws IOException {
        return inputBinary(new GZIPInputStream(new FileInputStream(input), FILE_STREAM_BUFFER_SIZE));
    }

    public NAR outputBinary(@NotNull File f) throws IOException {
        return outputBinary(f, false);
    }

    public final NAR outputBinary(@NotNull File f, boolean append) throws IOException {
        return outputBinary(f, append, ((Task t) -> t));
    }

    public final NAR outputBinary(@NotNull File f, boolean append, Predicate<Task> each) throws IOException {
        return outputBinary(f, append, (Task t) -> each.test(t) ? t : null);
    }

    public NAR outputBinary(@NotNull File f, boolean append, Function<Task, Task> each) throws IOException {
        FileOutputStream f1 = new FileOutputStream(f, append);
        OutputStream ff = new GZIPOutputStream(f1, FILE_STREAM_BUFFER_SIZE);
        outputBinary(ff, each);
        ff.close();
        return this;
    }

    public final NAR outputBinary(OutputStream o) {
        return outputBinary(o, (Task x)->x);
    }
    public final NAR outputBinary(OutputStream o, Predicate<Task> filter) {
        return outputBinary(o, (Task x)-> filter.test(x) ? x : null);
    }

    /**
     * byte codec output of matching concept tasks (blocking)
     * <p>
     * the each function allows transforming each task to an optional output form.
     * if this function returns null it will not output that task (use as a filter).
     */
    public NAR outputBinary(OutputStream o, Function<Task, Task> each) {

        //runLater(() -> {
            DataOutputStream oo = new DataOutputStream(o);

            MutableInteger total = new MutableInteger(0), wrote = new MutableInteger(0);

            tasks().map(each).filter(Objects::nonNull).forEach(x -> {

                total.increment();

                byte[] b = IO.taskToBytes(x);
                if (Param.DEBUG) {
                    //HACK temporary until this is debugged
                    Task xx = IO.taskFromBytes(b);
                    if (xx == null || !xx.equals(x)) {
                        throw new RuntimeException("task serialization problem: " + x + " != " + xx);
                    }
                }

                try {
                    oo.write(b);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                wrote.increment();
            });

            logger.debug("{} output {}/{} tasks ({} bytes)", o, wrote, total, oo.size());
        //});

        return this;
    }

    @NotNull
    public NAR outputText(@NotNull OutputStream o, @NotNull Function<Task, Task> each) {

        //runLater(() -> {
            //SnappyFramedOutputStream os = new SnappyFramedOutputStream(o);

            PrintStream ps = new PrintStream(o);

            MutableInteger total = new MutableInteger(0), wrote = new MutableInteger(0);

            StringBuilder sb = new StringBuilder();
            tasks().map(each).filter(Objects::nonNull).forEach(x -> {

                total.increment();

                if (x.truth() != null && x.conf() < confMin.floatValue())
                    return; //ignore task if it is below confMin

                sb.setLength(0);
                ps.println(x.appendTo(sb, true));
            });
        //});

        return this;
    }

    @NotNull
    public NAR output(@NotNull File o, boolean binary) throws FileNotFoundException {
        return output(new FileOutputStream(o), binary);
    }

    @NotNull
    public NAR output(@NotNull File o, Function<Task, Task> f) throws FileNotFoundException {
        return outputBinary(new FileOutputStream(o), f);
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

    public final Termed get(Term x) {
        return get(x, false);
    }

    public final Termed get(Term x, boolean createIfAbsent) {
        return concepts.get(x, createIfAbsent);
    }

    /**
     * strongest matching belief for the target time
     */
    @Nullable
    public Task belief(Termed c, long start, long end) {
        return match(c, BELIEF, start, end);
    }

    public final Task belief(Termed c, long when) {
        return belief(c, when, when);
    }

    public final Task belief(Term c) {
        return belief(c, time());
    }

    /**
     * strongest matching goal for the target time
     */
    @Nullable
    public final Task goal(Termed c, long start, long end) {
        return match(c, GOAL, start, end);
    }

    public final Task goal(Termed c, long when) {
        return goal(c, when, when);
    }

    @Nullable
    public Task match(Term c, byte punc, long when) {
        return match(c, punc, when, when);
    }

    @Nullable
    public Task match(Termed c, byte punc, long start, long end) {
        assert (punc == BELIEF || punc == GOAL);
        Concept concept = conceptualize(c);
        if (!(concept instanceof TaskConcept))
            return null;

        return ((BeliefTable) concept.table(punc)).answer(start, end, c.term(), this);
    }

    public SortedMap<String, Object> stats(Appendable out) {

        SortedMap<String, Object> stat = stats();
        stat.forEach((k, v) -> {
            try {
                out.append(k.replace(" ", "/")).append(" \t ").append(v.toString()).append('\n');
            } catch (IOException e) {
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
        return Math.max(Pri.EPSILON, 1f + Util.tanhFast(value(effect)));
    }

    public final float amp(Task task) {
        return amp(task.cause());
    }

    public float value(short[] effect) {
        return MetaGoal.privaluate(causes, effect);
    }

//    /**
//     * bins a range of values into N equal levels
//     */
//    public static class ChannelRange<X extends Priority> {
//        public final float min, max;
//        public final Cause[] levels;
//        transient private final float range; //cache
//
//        public ChannelRange(String id, int levels, Function<Object, CauseChannel<X>> src, float min, float max) {
//            this.min = min;
//            this.max = max;
//            assert (max > min);
//            this.range = max - min;
//            assert (range > Prioritized.EPSILON);
//            this.levels = Util.map(0, levels, (l) -> src.apply(id + l), Cause[]::new);
//        }
//
//        public Cause get(float value) {
//            return levels[Util.bin(Util.unitize((value - min) / range), levels.length)];
//        }
//    }
//
////    public final ImplicitTaskCauses taskCauses = new ImplicitTaskCauses(this);
////
////    static class ImplicitTaskCauses {
////
////        public final Cause causeBelief, causeGoal, causeQuestion, causeQuest;
////        public final Cause causePast, causePresent, causeFuture, causeEternal;
////        //public final ChannelRange causeConf, causeFreq;
////        public final NAR nar;
////
////        ImplicitTaskCauses(NAR nar) {
////            this.nar = nar;
////            causeBelief = nar.newChannel(String.valueOf((char) BELIEF));
////            causeGoal = nar.newChannel(String.valueOf((char) GOAL));
////            causeQuestion = nar.newChannel(String.valueOf((char) QUESTION));
////            causeQuest = nar.newChannel(String.valueOf((char) QUEST));
////            causeEternal = nar.newChannel("Eternal");
////            causePast = nar.newChannel("Past");
////            causePresent = nar.newChannel("Present");
////            causeFuture = nar.newChannel("Future");
//////            causeConf = new ChannelRange("Conf", 7 /* odd */, nar::newChannel, 0f, 1f);
//////            causeFreq = new ChannelRange("Freq", 7 /* odd */, nar::newChannel, 0f, 1f);
////        }
////
////        /**
////         * classifies the implicit / self-evident causes a task
////         */
////        public short[] get(Task x) {
////            //short[] ii = ArrayPool.shorts().getExact(8);
////
////            short time;
////            if (x.isEternal())
////                time = causeEternal.id;
////            else {
////                long now = nar.time();
////                long then = x.nearestTimeTo(now);
////                if (Math.abs(now - then) <= nar.dur())
////                    time = causePresent.id;
////                else if (then > now)
////                    time = causeFuture.id;
////                else
////                    time = causePast.id;
////            }
////
////            short punc;
////            switch (x.punc()) {
////                case BELIEF:
////                    punc = causeBelief.id;
////                    break;
////                case GOAL:
////                    punc = causeGoal.id;
////                    break;
////                case QUESTION:
////                    punc = causeQuestion.id;
////                    break;
////                case QUEST:
////                    punc = causeQuest.id;
////                    break;
////                default:
////                    throw new UnsupportedOperationException();
////            }
//////            if (x.isBeliefOrGoal()) {
//////                short freq = causeFreq.get(x.freq()).id;
//////                short conf = causeConf.get(x.conf()).id;
//////                return new short[]{time, punc, freq, conf};
//////            } else {
////            return new short[]{time, punc};
//////            }
////        }
////
////    }


    /**
     * automatically adds the cause id to each input
     */
    public CauseChannel<ITask> newCauseChannel(Object id) {

        synchronized (causes) {

//            final short[] sharedOneElement = {ci};
            final short ci = (short) (causes.size());
            CauseChannel c = new CauseChannel.TaskChannel(this, ci, id, (x) -> {
                if (x instanceof NALTask) {
                    NALTask t = (NALTask) x;
                    int tcl = t.cause.length;
                    if (tcl == 0) {
//                        assert (sharedOneElement[0] == ci);
                        t.cause = new short[]{ci}; //sharedOneElement;
                    } else {
                        if (tcl == 1 && t.cause[0] == ci)
                            return; //already equivalent
                        else {
                            //concat
                            t.cause = Arrays.copyOf(t.cause, tcl + 1);
                            t.cause[tcl] = ci;
                        }
                    }
                }
            });
            causes.add(c);
            return c;
        }
    }

//    public CauseChannel<Task> newChannel(Object x, Consumer<ITask> target) {
//        return newCause((next)-> {
//            return new CauseChannel(next, x, target);
//        });
//    }

    public <C extends Cause> C newCause(ShortToObjectFunction<C> idToChannel) {
        synchronized (causes) {
            short next = (short) (causes.size());
            C c = idToChannel.valueOf(next);
            causes.add(c);
            return c;
        }
    }


    public Concept activate(Termed t, float activationApplied) {
        Concept c = concept(t, false /* true */);
        if (c != null)
            exe.activate(c, activationApplied);
        return c;
    }

    public Stream<Services.Service<NAR>> services() {
        return services.stream();
    }


    public void conceptualize(Term term, Consumer<Concept> with) {
//        if (exe.concurrent()) {
//            terms.conceptAsync(term, true, with);
//        } else {
            Concept x = conceptualize(term);
            if (x != null) {
                with.accept(x);
            } else {
                //TODO
            }
//        }
    }

    public final void out(Object x) {
        eventTask.emit(Operator.log(time(), x));
    }


}
