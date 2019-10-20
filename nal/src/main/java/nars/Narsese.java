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
import com.google.common.annotations.VisibleForTesting;
import jcog.Util;
import jcog.data.list.FasterList;
import nars.io.NarseseParser;
import nars.task.AbstractCommandTask;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.obj.QuantityTerm;
import nars.time.Tense;
import nars.time.Time;
import nars.truth.Truth;
import nars.util.SoftException;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import static nars.Op.*;
import static nars.term.Term.nullIfNull;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.TIMELESS;

/**
 * NARese, syntax and language for interacting with a NAL in NARS.
 * https:
 */
public final class Narsese {

    private static final ThreadLocal<Narsese> parsers;

    static {
        Class<? extends NarseseParser> parserClass;

        try {
//            ParserClassNode node = ParserTransformer.extendParserClass(NarseseParser.class);
//            parser = node.getExtendedClass();

            parserClass = ParserTransformer.transformParser(NarseseParser.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        parsers = ThreadLocal.withInitial(() -> {
                    try {
                        return new Narsese(parserClass.getConstructor().newInstance());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    private final INarseseParser parser;
    private MyParseRunner inputParser;
    private MyParseRunner termParser;

    private Narsese(INarseseParser p) {
        this.parser = p;
    }
    private MyParseRunner inputParser() {
        if (inputParser == null)
            this.inputParser = new MyParseRunner(parser.Input());
        return inputParser;
    }
    private MyParseRunner termParser() {
        if (termParser == null)
            this.termParser = new MyParseRunner(parser.Term());
        return termParser;
    }

    public static Narsese the() {
        return parsers.get();
    }

    /**
     * returns number of tasks created
     */
    public static void tasks(String input, Collection<Task> c, NAL m) throws NarseseException {
        var p = the();

        var parsedTasks = 0;

        var r = p.inputParser().run(input);
        var rv = r.getValueStack();

        var size = rv.size();

        for (var i = size - 1; i >= 0; i--) {
            var o = rv.peek(i);

            Object[] y;
            if (o instanceof Task) {
                y = (new Object[]{o});
            } else if (o instanceof Object[]) {
                y = ((Object[]) o);
            } else {
                throw new NarseseException("Parse error: " + input);
            }

            var t = decodeTask(m, y);

            c.add(t);
            parsedTasks++;

        }

        if (parsedTasks == 0)
            throw new NarseseException("nothing parsed: " + input);


    }

    public static List<Task> tasks(String input, NAL m) throws NarseseException {
        List<Task> result = new FasterList<>(1);
        tasks(input, result, m);

        return result;
    }

    /**
     * parse one task
     */
    public static Task task(String input, NAL n) throws NarseseException {
        var tt = tasks(input, n);
        if (tt.size() != 1)
            throw new NarseseException(tt.size() + " tasks parsed in single-task parse: " + input);
        return tt.get(0);
    }

    static Task decodeTask(NAL nar, Object[] x) {
        if (x.length == 1 && x[0] instanceof Task) {
            return (Task) x[0];
        }

        var content = ((Term) x[1]).normalize();
            /*if (!(content instanceof Compound)) {
                throw new NarseseException("Task target unnormalizable: " + contentRaw);

            } else */

        var px = x[2];

        var punct =
                px instanceof Byte ?
                        (Byte) x[2]
                        :
                        (byte) (((Character) x[2]).charValue());

        if (punct == COMMAND)
            return new AbstractCommandTask(content);

        var _t = x[3];
        Truth t;

        if (_t instanceof Truth)
            t = (Truth) _t;
        else if (_t instanceof Float)
            t = $.t((Float) _t, nar.confDefault(punct));
        else
            t = null;

        if (t == null && (punct == BELIEF || punct == GOAL))
            t = $.t(1, nar.confDefault(punct)); //HACK


        var occ = occurrence(nar.time, x[4]);

        var C = Task.taskValid(content, punct, t, false);

        Task y = NALTask.the(C, punct, t, nar.time(), occ[0], occ[1], nar.evidence());

        y.pri(x[0] == null ? nar.priDefault(punct) : (Float) x[0]);

        return y;

    }

    private static long[] occurrence(Time t, Object O) {
        if (O == null)
            return new long[]{ETERNAL, ETERNAL};
        else if (O instanceof Tense) {
            var o = t.relativeOccurrence((Tense)O);
            return new long[]{o, o};
        } else if (O instanceof QuantityTerm) {
            var qCycles = t.toCycles(((QuantityTerm) O).quant);
            var o = t.now() + qCycles;
            return new long[]{o, o};
        } else if (O instanceof Integer) {

            var o = t.now() + (Integer) O;
            return new long[]{o, o};
        } else if (O instanceof Object[]) {
            var start = occurrence(t, ((Object[]) O)[0]);
            if (start[0] != start[1] || start[0] == ETERNAL || start[0] == TIMELESS)
                throw new UnsupportedOperationException();
            var end = occurrence(t, ((Object[]) O)[1]);
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
        var y = term(s);
        return normalize ? nullIfNull(y.normalize()) : y;
    }

    public static Term term(String s) throws NarseseException {
        return Narsese.the()._term(s);
    }

    /**
     * parse one target NOT NORMALIZED
     */
    Term _term(String s) throws NarseseException {

        Exception ee = null;
        try {


            var r = termParser().run(s);

            var stack = r.getValueStack();

            if (stack.size() == 1) {
                var x = stack.pop();

                if (x instanceof String)
                    return Atomic.the((String) x);
                else if (x instanceof Term)
                    return (Term) x;
            } else {
                var x = Util.map(0, stack.size(), Object[]::new, stack::peek);
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

        public ParsingResult<V> run(InputBuffer inputBuffer) {
            //Objects.requireNonNull(inputBuffer, "inputBuffer");
            resetValueStack();

            var context = createRootContext(inputBuffer, this);

            return createParsingResult(context.runMatcher(), context);
        }

        private void resetValueStack() {
            // TODO: write a "memoizing" API
            valueStack = new ArrayValueStack<>();
        }

        @VisibleForTesting
        MatcherContext<V> createRootContext(
                InputBuffer inputBuffer, MatchHandler matchHandler) {
            return new DefaultMatcherContext<>(inputBuffer, valueStack,
                    matchHandler, rootMatcher);
        }

        @VisibleForTesting
        ParsingResult<V> createParsingResult(boolean matched,
                                             MatcherContext<V> context) {
            return new ParsingResult<>(matched, valueStack, context);
        }


    }

    static final class CharSequenceInputBuffer implements InputBuffer {

        final Future<LineCounter> lineCounter = null;
        private final char[] charSequence;

        CharSequenceInputBuffer(CharSequence charSequence) {
            var length = charSequence.length();

            this.charSequence = new char[length];
            for (var i = 0; i < length; i++) {
                this.charSequence[i] = charSequence.charAt(i);
            }
        }

        @Override
        public char charAt(int index) {
            return charSequence[index];
        }

        @SuppressWarnings("ImplicitNumericConversion")
        @Override
        public int codePointAt(int index) {
            var length = charSequence.length;
            if (index >= length)
                return -1;

            var c = charAt(index);
            if (!Character.isHighSurrogate(c))
                return c;
            if (index == length - 1)
                return c;
            var c2 = charAt(index + 1);
            return Character.isLowSurrogate(c2) ? Character.toCodePoint(c, c2) : c;
        }

        @Override
        public String subSequence(int start, int end) {
            //return charSequence.subSequence(start, end);
            return end > start ? new String(charSequence, start, (end - start)) : "";
        }

        @Override
        public String extract(int start, int end) {
            var realStart = Math.max(start, 0);
            var realEnd = Math.min(end, charSequence.length);
            return subSequence(realStart, realEnd);
        }

        @Override
        public String extract(IndexRange range) {
            return extract(range.start, range.end);
        }

        static final Position zero = new Position(0, 0);

        @Override
        public Position getPosition(int index) {


            return zero; //HACK
            //return Futures.getUnchecked(lineCounter).toPosition(index);
        }

        @Override
        public String extractLine(int lineNumber) {
            throw new UnsupportedOperationException();
//            Preconditions.checkArgument(lineNumber > 0, "line number is negative");
//            final LineCounter counter = lineCounter.get(); //Futures.getUnchecked(lineCounter);
//            final Range<Integer> range = counter.getLineRange(lineNumber);
//            final int start = range.lowerEndpoint();
//            int end = range.upperEndpoint();
//            if (charAt(end - 1) == '\n')
//                end--;
//            if (charAt(end - 1) == '\r')
//                end--;
//            return extract(start, end);
        }

        @Override
        public IndexRange getLineRange(int lineNumber) {
            throw new UnsupportedOperationException();
//            final Range<Integer> range
//                    = Futures.getUnchecked(lineCounter).getLineRange(lineNumber);
//            return new IndexRange(range.lowerEndpoint(), range.upperEndpoint());
        }

        @Override
        public int getLineCount() {
            throw new UnsupportedOperationException();
//            return Futures.getUnchecked(lineCounter).getNrLines();
        }

        @Override
        public int length() {
            return charSequence.length;
        }
    }

    /**
     * Describes an error that occurred while parsing Narsese
     */
    public static final class NarseseException extends Exception {

        public final @Nullable ParsingResult result;

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
