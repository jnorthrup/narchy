package nars.concept;

import jcog.util.ArrayUtils;
import nars.*;
import nars.link.TermLinker;
import nars.subterm.Subterms;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

import static nars.Op.ATOM;
import static nars.Op.INH;

/**
 * Operator interface specifically for goal and command punctuation
 * Allows dynamic handling of argument list like a functor,
 * but at the task level
 * <patham9_> when a goal task is processed, the following happens: In order to decide on whether it is relevant for the current situation, at first it is projected to the current time, then it is revised with previous "desires", then it is checked to what extent this projected revised desire is already fullfilled (which revises its budget) , if its below satisfaction threshold then it is pursued, if its an operation it is additionally checked if
 * <patham9_> executed
 * <patham9_> the consequences of this, to give examples, are a lot:
 * <patham9_> 1 the system wont execute something if it has a strong objection against it. (example: it wont jump down again 5 meters if it previously observed that this damages it, no matter if it thinks about that situation again or not)
 * <patham9_> 2. the system wont lose time with thoughts about already satisfied goals (due to budget shrinking proportional to satisfaction)
 * <patham9_> 3. the system wont execute and pursue what is already satisfied
 * <patham9_> 4. the system wont try to execute and pursue things in the current moment which are "sheduled" to be in the future.
 * <patham9_> 5. the system wont pursue a goal it already pursued for the same reason (due to revision, it is related to 1)
 */
public final class Operator extends NodeConcept implements PermanentConcept, Atomic, The {

    private static final String LOG_FUNCTOR = String.valueOf(Character.valueOf((char) 8594));

    public final BiFunction<Task, NAR, Task> model;

    private Operator(Atom atom, BiFunction<Task, NAR, Task> model) {
        super(atom, TermLinker.NullLinker);
        this.model = model;
    }

    @Override
    public final Op op() {
        return ATOM;
    }

    public static Operator simple(Atom name, BiFunction<Task, NAR, Task> exe) {
         return new Operator(name, new SimpleOperatorModel(exe));
    }


    @Override
    public final Term term() {
        return this;
    }

    @Override
    public int opX() {
        return Atom.AtomString;
    }

    @Override
    public byte[] bytes() {
        return ((Atomic) term).bytes();
    }

    /**
     * returns the arguments of an operation (task or target)
     */
    public static Subterms args(Term operation) {
        assert (operation.op() == INH && operation.subIs(1, ATOM));
        return operation.sub(0).subterms();
    }

    public static Task error(Task x, Throwable error, long when) {
        
        
        return Operator.command("error", when, $.quote(x.toString()),
                
                error!=null ? $.quote(error.getMessage()!=null ? error.getMessage() : error.toString()) : $.the("?") 
        );
    }


    private static Task command(Term content, long when) {
        return NALTask.the(content, Op.COMMAND, null, when, when, when, ArrayUtils.EMPTY_LONG_ARRAY);
    }

    public static Task log(long when, Object content) {
        return Operator.command(LOG_FUNCTOR, when, $.the(content));
    }

    private static Task command(String func, long now, @NotNull Term... args) {
        return Operator.command($.func(func, args), now);
    }

    public static Term arg(Term operation, int sub) {
        return operation.sub(0).subterms().sub(sub);
    }


    static class SimpleOperatorModel implements BiFunction<Task, NAR, Task> {

        private final BiFunction<Task, NAR, Task> exe;

        SimpleOperatorModel(BiFunction<Task, NAR, Task> exe) {
            this.exe = exe;
        }

        @Override
        public Task apply(Task task, NAR nar) {
            //default handler
            if (task.isCommand())
                return exe.apply(task, nar);
            else {
                assert(task.isGoal());
                if (executeGoal(task)) {
                    long s = task.start();
                    long now = nar.time();
                    int dur = nar.dur();
                    if (s >= now - dur / 2) {
                        if (s > now + dur / 2) {
                            //delayed
                            nar.runAt(s, () -> nar.input(exe.apply(task, nar)));
                        } else {
                            return exe.apply(task, nar);
                        }
                    }
                }

                return null;
            }
        }

        static boolean executeGoal(Task goal) {
            return goal.expectation() > 0.5f;
        }
    }

}
