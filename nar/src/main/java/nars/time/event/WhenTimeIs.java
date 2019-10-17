package nars.time.event;

import nars.$;
import nars.NAR;
import nars.term.Term;
import nars.time.ScheduledTask;

import java.util.function.Consumer;

public abstract class WhenTimeIs extends ScheduledTask {

    public final long whenOrAfter;

    public static WhenTimeIs then(long whenOrAfter, Object then) {
        if (then instanceof Runnable)
            return new WhenTimeIs_Run(whenOrAfter, (Runnable)then);
        else
            return new WhenTimeIs_Consume(whenOrAfter, (Consumer)then);
    }

    private static final class WhenTimeIs_Consume extends WhenTimeIs {
        private final Consumer<NAR> then;

        private WhenTimeIs_Consume(long whenOrAfter, Consumer<NAR> then) {
            super(whenOrAfter);
            this.then = then;
        }

        @Override
        public void accept(NAR nar) {
            then.accept(nar);
        }

        @Override
        protected Object _id() {
            return then;
        }
    }

    private static final class WhenTimeIs_Run extends WhenTimeIs {
        private final Runnable then;

        private WhenTimeIs_Run(long whenOrAfter, Runnable then) {
            super(whenOrAfter);
            this.then = then;
        }


        @Override
        public void accept(NAR nar) {
            then.run();
        }

        @Override
        protected Object _id() {
            return then;
        }
    }

    WhenTimeIs(long whenOrAfter) {
        this.whenOrAfter = whenOrAfter;
    }

    @Override
    public long start() {
        return whenOrAfter;
    }

    @Override
    public Term term() {
        return $.p($.identity(_id()), $.the(whenOrAfter));
    }

    protected abstract Object _id();

}
