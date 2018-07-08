package nars.derive;

import jcog.Util;
import jcog.bag.Bag;
import jcog.pri.PriReference;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.Activate;
import nars.control.Cause;
import nars.derive.budget.DefaultDeriverBudgeting;
import nars.derive.premise.PremiseDeriver;
import nars.derive.premise.PremiseDeriverCompiler;
import nars.derive.premise.PremiseDeriverProto;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.exe.Attention;
import nars.exe.Causable;
import nars.link.TaskLink;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.Op.*;

/**
 * an individual deriver process: executes a particular Deriver model
 * specified by a set of premise rules.
 * <p>
 * runtime intensity is metered and throttled by causal feedback
 *
 * this is essentially a Function<Premise, Stream<DerivedTask>> but
 * the current level of code complexity makes this non-obvious
 */
abstract public class Deriver extends Causable {

    @Deprecated private static final AtomicInteger serial = new AtomicInteger();

    public static final ThreadLocal<Derivation> derivation = ThreadLocal.withInitial(Derivation::new);

    public final PremiseDeriver rules;

    /**
     * source of concepts supplied to this for this deriver
     */
    protected final Consumer<Predicate<Activate>> source;

    private final Consumer<Collection<Task>> target;

    public DeriverBudgeting prioritize =
            new DefaultDeriverBudgeting();
            

    public Deriver(NAR nar, String... rules) {
        this(new PremiseDeriverRuleSet(nar, rules));
    }

    public Deriver(PremiseDeriverRuleSet rules) {
        this(rules.nar.attn, rules, rules.nar);
    }


    public Deriver(Attention attn, Set<PremiseDeriverProto> rules, NAR nar) {
        this(attn::fire, nar::input, rules, nar);
    }

    public Deriver(Consumer<Predicate<Activate>> source, Consumer<Collection<Task>> target, PremiseDeriverRuleSet rules) {
        this(source, target, rules, rules.nar);
    }

    public Deriver(Consumer<Predicate<Activate>> source, Consumer<Collection<Task>> target, Set<PremiseDeriverProto> rules, NAR nar) {
        this(source, target, PremiseDeriverCompiler.the(rules), nar);
        if (rules.isEmpty())
            throw new RuntimeException("rules empty");
    }

    public Deriver(Consumer<Predicate<Activate>> source, Consumer<Collection<Task>> target, PremiseDeriver rules, NAR nar) {
        super(
            $.func("deriver", $.the(serial.getAndIncrement())) 
        );
        this.rules = rules;
        this.source = source;
        this.target = target;

        nar.on(this);
    }

    public static Stream<Deriver> derivers(NAR n) {
        return n.services().filter(Deriver.class::isInstance).map(Deriver.class::cast);
    }

    @Override
    protected int next(NAR n, final int iterations) {
//        if (n == null || !(iterations > 0))
//            throw new WTF();


        Derivation d = derivation.get().cycle(n, this);


        derive(n, iterations, d);

        int derived = d.flush(target);

        //System.out.println(derivations + " -> " + iterations + " -> " + derived);
        //io.out(derived);

        //return derived;
        return iterations;
    }



    abstract protected void derive(NAR n, int iterations, Derivation d);


    /** returns false if no tasklinks are present */
    static protected boolean commit(NAR nar, Bag<?, TaskLink> tasklinks, @Nullable Bag<Term, PriReference<Term>> termlinks) {
        float linkForgetting = nar.forgetRate.floatValue();
        tasklinks.commit(tasklinks.forget(linkForgetting));
        int ntasklinks = tasklinks.size();
        if (ntasklinks == 0)
            return false;


        if (termlinks!=null)
            termlinks.commit(termlinks.forget(linkForgetting));

        return true;

    }

    /** punctuation equalizer: value factor for the conclusion punctuation type [0..1.0] */
    public float puncFactor(byte conclusion) {


        switch (conclusion) {
            case BELIEF: return 1f;
            case GOAL: return 1f;
            case QUESTION: return 1f;
            case QUEST: return 1f;
            default:
                throw new UnsupportedOperationException();
        }
    }


    /** unifier TTL used for matching in premise formation */
    protected int matchTTL() {
        return Param.TTL_MIN * 4;
    }


    @Override
    public final boolean singleton() {
        return false;
    }

    @Override
    public float value() {
        //TODO cache this between cycles
        return Util.sum(Cause::value, rules.causes());
    }


    //public final FloatRange sustain = new FloatRange(0f, 0f, 0.99f);
    public int dur() {
        //return Math.round((nar.dur() * (1/(1- sustain.floatValue()))));
        return nar.dur();
    }
}

















































































































