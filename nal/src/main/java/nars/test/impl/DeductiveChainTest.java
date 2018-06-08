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

    @NotNull
    public final Term q;
    @NotNull
    public final Term[] beliefs;

    @FunctionalInterface
    public interface IndexedStatementBuilder {
        @NotNull
        Term apply(int x, int y);
    }

    @Nullable
    public static final IndexedStatementBuilder inh = (int x, int y) ->
            $.inh(a(x), a(y));
    @Nullable
    public static final IndexedStatementBuilder sim = (int x, int y) ->
            $.sim(a(x), a(y));
    @Nullable
    public static final IndexedStatementBuilder impl = (int x, int y) ->
            $.impl(a(x), a(y));



    public DeductiveChainTest(@NotNull NAR n, int length, int timeLimit, @NotNull IndexedStatementBuilder b) {
        this(new TestNAR(n), length, timeLimit, b);
    }

    public DeductiveChainTest(@NotNull TestNAR n, int length, int timeLimit, @NotNull IndexedStatementBuilder b) {

        beliefs = new Compound[length];
        for (int x = 0; x < length; x++) {
            beliefs[x] = b.apply(x, x+1);
        }

        q = b.apply(0, length);

        for (Term belief : beliefs) {
            n.nar.believe(belief);
        }
        n.nar.question( q );

        n.mustBelieve(timeLimit, q.toString(), 1f, 1f, 0.01f, 1f);

    }


    @NotNull
    static Atomic a(int i) {
        return $.the((byte)('a' + i));
    }




























































































}
