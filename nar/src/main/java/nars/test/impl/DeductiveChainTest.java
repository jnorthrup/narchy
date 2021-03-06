package nars.test.impl;

import nars.$;
import nars.NAR;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.test.TestNAR;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class DeductiveChainTest  {

    public final @NotNull Term q;
    public final @NotNull Term[] beliefs;

    @FunctionalInterface
    public interface IndexedStatementBuilder {
        @NotNull
        Term apply(int x, int y);
    }

    public static final @Nullable IndexedStatementBuilder inh = new IndexedStatementBuilder() {
        @Override
        public @NotNull Term apply(int x, int y) {
            return $.INSTANCE.inh(a(x), a(y));
        }
    };
    public static final @Nullable IndexedStatementBuilder sim = new IndexedStatementBuilder() {
        @Override
        public @NotNull Term apply(int x, int y) {
            return $.INSTANCE.sim(a(x), a(y));
        }
    };
    public static final @Nullable IndexedStatementBuilder impl = new IndexedStatementBuilder() {
        @Override
        public @NotNull Term apply(int x, int y) {
            return $.INSTANCE.impl(a(x), a(y));
        }
    };



    public DeductiveChainTest(@NotNull NAR n, int length, int timeLimit, @NotNull IndexedStatementBuilder b) {
        this(new TestNAR(n), length, timeLimit, b);
    }

    public DeductiveChainTest(@NotNull TestNAR n, int length, int timeLimit, @NotNull IndexedStatementBuilder b) {

        beliefs = new Compound[length];
        for (int x = 0; x < length; x++) {
            beliefs[x] = b.apply(x, x+1);
        }

        q = b.apply(0, length);

        for (@NotNull Term belief : beliefs) {
            n.nar.believe(belief);
        }
        n.nar.question( q );

        n.mustBelieve((long) timeLimit, q.toString(), 1f, 1f, 0.01f, 1f);

    }


    static @NotNull Atomic a(int i) {
        return $.INSTANCE.the((byte)((int) 'a' + i));
    }




























































































}
