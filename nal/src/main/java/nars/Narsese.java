package nars;

import com.github.fge.grappa.buffers.InputBuffer;
import com.github.fge.grappa.buffers.LineCounter;
import com.github.fge.grappa.matchers.base.Matcher;
import com.github.fge.grappa.rules.Rule;
import com.github.fge.grappa.run.MatchHandler;
import com.github.fge.grappa.run.ParsingResult;
import com.github.fge.grappa.run.context.DefaultMatcherContext;
import com.github.fge.grappa.run.context.MatcherContext;
import com.github.fge.grappa.stack.ArrayValueStack;
import com.github.fge.grappa.stack.ValueStack;
import com.github.fge.grappa.support.IndexRange;
import com.github.fge.grappa.support.Position;
import com.github.fge.grappa.transform.ParserTransformer;
import com.github.fge.grappa.transform.base.ParserClassNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import com.google.common.util.concurrent.Futures;
import jcog.Util;
import nars.task.CommandTask;
import nars.task.NALTask;
import nars.task.util.TaskException;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.obj.QuantityTerm;
import nars.time.Tense;
import nars.truth.Truth;
import nars.util.SoftException;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static nars.Op.*;
import static nars.term.Term.nullIfNull;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.TIMELESS;

/**
 * NARese, syntax and language for interacting with a NAR in NARS.
 * https:
 */
public class Narsese {

    static final String NARSESE_TASK_TAG = "Narsese";
    private static final Class parser;
    private static final ThreadLocal<Narsese> parsers;

    static {

        try {

            ParserClassNode node = ParserTransformer.extendParserClass(NarseseParser.class);
            parser = node.getExtendedClass();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        parsers = ThreadLocal.withInitial(() -> {
                    try {
                        return new Narsese((INarseseParser) (parser.getConstructor().newInstance()));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    private final MyParseRunner inputParser, termParser;

    public Narsese(INarseseParser p) {
        this.inputParser = new MyParseRunner(p.Input());
        this.termParser = new MyParseRunner(p.Term());
    }

    public static Narsese the() {
        return parsers.get();
    }

    /**
     * returns number of tasks created
     */
    public static void tasks(String input, Collection<Task> c, NAR m) throws NarseseException {
        tasks(input, c::add, m);
    }

    public static List<Task> tasks(String input, NAR m) throws NarseseException {
        List<Task> result = $.newArrayList(1);
        tasks(input, result, m);

        return result;
    }

    /**
     * gets a stream of raw immutable task-generating objects
     * which can be re-used because a Memory can generate them
     * ondemand
     */
    static void tasks(String input, Consumer<Task> c, NAR m) throws NarseseException {
        Narsese p = the();

        int parsedTasks = 0;

        ParsingResult r = p.inputParser.run(input);

        int size = r.getValueStack().size();

        for (int i = size - 1; i >= 0; i--) {
            Object o = r.getValueStack().peek(i);

            Object[] y;
            if (o instanceof Task) {
                y = (new Object[]{o});
            } else if (o instanceof Object[]) {
                y = ((Object[]) o);
            } else {
                throw new NarseseException("Parse error: " + input);
            }

            Task t = decodeTask(m, y);

            c.accept(t);
            parsedTasks++;

        }

        if (parsedTasks == 0)
            throw new NarseseException("nothing parsed: " + input);


    }

    /**
     * parse one task
     */
    static public Task task(String input, NAR n) throws NarseseException {
        List<Task> tt = tasks(input, n);
        if (tt.size() != 1)
            throw new NarseseException(tt.size() + " tasks parsed in single-task parse: " + input);
        return tt.get(0);
    }

    static Task decodeTask(NAR nar, Object[] x) {
        if (x.length == 1 && x[0] instanceof Task) {
            return (Task) x[0];
        }

        Term content = ((Term) x[1]).normalize();
            /*if (!(content instanceof Compound)) {
                throw new NarseseException("Task term unnormalizable: " + contentRaw);

            } else */

        Object px = x[2];

        byte punct =
                px instanceof Byte ?
                        (Byte) x[2]
                        :
                        (byte) (((Character) x[2]).charValue());

        if (punct == COMMAND) {

            return new CommandTask(content);
        }

        Object _t = x[3];
        Truth t;

        if (_t instanceof Truth) {
            t = (Truth) _t;
        } else if (_t instanceof Float) {

            t = $.t((Float) _t, nar.confDefault(punct));
        } else {
            t = null;
        }


        if (t == null && (punct == BELIEF || punct == GOAL)) {
            t = $.t(1, nar.confDefault(punct));
        }

        Task y = Task.tryTask(content, punct, t, (C, tr) -> {


            long[] occ = occurrence(nar, x[4]);

            Task yy = new NALTask(C, punct, tr, nar.time(), occ[0], occ[1], nar.evidence());
            yy.pri(x[0] == null ? nar.priDefault(punct) : (Float) x[0]);
            yy.log(NARSESE_TASK_TAG);
            return yy;
        });

        if (y == null) {
            throw new TaskException(content, "input: " + Arrays.toString(x));
        }

        return y;

    }

    private static long[] occurrence(NAR nar, Object O) {
        if (O == null)
            return new long[]{ETERNAL, ETERNAL};
        else if (O instanceof Tense) {
            long o = Tense.getRelativeOccurrence((Tense) O, nar);
            return new long[]{o, o};
        } else if (O instanceof QuantityTerm) {
            long qCycles = nar.time.toCycles(((QuantityTerm) O).quant);
            long o = nar.time() + qCycles;
            return new long[]{o, o};
        } else if (O instanceof Integer) {

            long o = nar.time() + (Integer) O;
            return new long[]{o, o};
        } else if (O instanceof Object[]) {
            long[] start = occurrence(nar, ((Object[]) O)[0]);
            if (start[0] != start[1] || start[0] == ETERNAL || start[0] == TIMELESS)
                throw new UnsupportedOperationException();
            long[] end = occurrence(nar, ((Object[]) O)[1]);
            if (end[0] != end[1] || end[0] == ETERNAL || end[0] == TIMELESS)
                throw new UnsupportedOperationException();
            if (start[0] <= end[0]) {
                start[1] = end[0];
                return start;
            } else {
                end[1] = start[0];
                return end;
            }
        } else if (O instanceof long[]) {
            return (long[]) O;
        } else {
            throw new UnsupportedOperationException("unrecognized occurrence: " + O);
        }
    }

    public static Term term(String s, boolean normalize) throws NarseseException {

        Term y = term(s);
        if (normalize) {
            return nullIfNull(y.normalize());
        } else {
            return y;


        }


    }

    public static Term term(String s) throws NarseseException {
        return Narsese.the()._term(s);
    }

    /**
     * parse one term NOT NORMALIZED
     */
    Term _term(String s) throws NarseseException {

        Exception ee = null;
        try {


            ParsingResult r = termParser.run(s);

            ValueStack stack = r.getValueStack();

            if (stack.size() == 1) {
                Object x = stack.pop();

                if (x instanceof String)
                    return Atomic.the((String) x);
                else if (x instanceof Term)
                    return (Term) x;
            } else {
                Object[] x = Util.map(0, stack.size(), stack::peek, Object[]::new);
                ee = new SoftException("incomplete parse: " + Arrays.toString(x));
            }


        } catch (Exception e) {
            ee = e;
        }
        throw new NarseseException(s, null, ee);
    }


    public interface INarseseParser {
        Rule Input();

        Rule Term();
    }

    static class MyParseRunner<V> implements MatchHandler {

        private final Matcher rootMatcher;
        private ValueStack<V> valueStack;

        /**
         * Constructor
         *
         * @param rule the rule
         */
        MyParseRunner(Rule rule) {
            rootMatcher = (Matcher) rule;
        }

        @Override
        public boolean match(MatcherContext context) {
            return context.getMatcher().match(context);
        }

        public ParsingResult run(String input) {
            return run(new CharSequenceInputBuffer(input));
        }

        public ParsingResult<V> run(final InputBuffer inputBuffer) {
            //Objects.requireNonNull(inputBuffer, "inputBuffer");
            resetValueStack();

            final MatcherContext<V> context = createRootContext(inputBuffer, this);

            return createParsingResult(context.runMatcher(), context);
        }

        private void resetValueStack() {
            // TODO: write a "memoizing" API
            valueStack = new ArrayValueStack<>();
        }

        @VisibleForTesting
        MatcherContext<V> createRootContext(
                final InputBuffer inputBuffer, final MatchHandler matchHandler) {
            return new DefaultMatcherContext<>(inputBuffer, valueStack,
                    matchHandler, rootMatcher);
        }

        @VisibleForTesting
        ParsingResult<V> createParsingResult(final boolean matched,
                                             final MatcherContext<V> context) {
            return new ParsingResult<>(matched, valueStack, context);
        }


    }

    static final class CharSequenceInputBuffer
            implements InputBuffer {

        final Future<LineCounter> lineCounter = null;
        private final char[] charSequence;

        public CharSequenceInputBuffer(@Nonnull final CharSequence charSequence) {
            int length = charSequence.length();

            this.charSequence = new char[length];
            for (int i = 0; i < length; i++) {
                this.charSequence[i] = charSequence.charAt(i);
            }
        }

        @Override
        public char charAt(final int index) {
            return charSequence[index];
        }

        @SuppressWarnings("ImplicitNumericConversion")
        @Override
        public int codePointAt(final int index) {
            int length = charSequence.length;
            if (index >= length)
                return -1;

            final char c = charAt(index);
            if (!Character.isHighSurrogate(c))
                return c;
            if (index == length - 1)
                return c;
            final char c2 = charAt(index + 1);
            return Character.isLowSurrogate(c2) ? Character.toCodePoint(c, c2) : c;
        }

        @Override
        public String subSequence(final int start, final int end) {
            //return charSequence.subSequence(start, end);
            return new String(charSequence, start, (end - start));
        }

        @Override
        public String extract(final int start, final int end) {
            final int realStart = Math.max(start, 0);
            final int realEnd = Math.min(end, charSequence.length);
            return subSequence(realStart, realEnd);
        }

        @Override
        public String extract(final IndexRange range) {
            return extract(range.start, range.end);
        }

        @Override
        public Position getPosition(final int index) {

            return new Position(0, 0); //HACK
            //return Futures.getUnchecked(lineCounter).toPosition(index);
        }

        @Override
        public String extractLine(final int lineNumber) {
            Preconditions.checkArgument(lineNumber > 0, "line number is negative");
            final LineCounter counter = Futures.getUnchecked(lineCounter);
            final Range<Integer> range = counter.getLineRange(lineNumber);
            final int start = range.lowerEndpoint();
            int end = range.upperEndpoint();
            if (charAt(end - 1) == '\n')
                end--;
            if (charAt(end - 1) == '\r')
                end--;
            return extract(start, end);
        }

        @SuppressWarnings("AutoUnboxing")
        @Override
        public IndexRange getLineRange(final int lineNumber) {
            final Range<Integer> range
                    = Futures.getUnchecked(lineCounter).getLineRange(lineNumber);
            return new IndexRange(range.lowerEndpoint(), range.upperEndpoint());
        }

        @Override
        public int getLineCount() {
            return Futures.getUnchecked(lineCounter).getNrLines();
        }

        @Override
        public int length() {
            return charSequence.length;
        }
    }

    /**
     * Describes an error that occurred while parsing Narsese
     */
    public static class NarseseException extends Exception {

        @Nullable
        public final ParsingResult result;

        /**
         * An invalid addInput line.
         *
         * @param message type of error
         */
        public NarseseException(String message) {
            super(message);
            this.result = null;
        }

        public NarseseException(String input, Throwable cause) {
            this(input, null, cause);
        }

        public NarseseException(String input, ParsingResult result, Throwable cause) {
            super(input + '\n' + (result != null ? result : cause), cause);
            this.result = result;
        }
    }

}
