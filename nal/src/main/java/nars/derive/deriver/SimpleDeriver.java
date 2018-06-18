package nars.derive.deriver;

import jcog.bag.Bag;
import jcog.list.FasterList;
import jcog.math.IntRange;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.control.Activate;
import nars.derive.Derivation;
import nars.derive.Deriver;
import nars.derive.Derivers;
import nars.derive.Premise;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.link.TaskLink;
import nars.term.Term;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * samples freely from concept, termlink, and tasklink bags without any buffering of premises
 */
public class SimpleDeriver extends Deriver {

    /**
     * iterations -> premises multiplier
     */
    public final IntRange power = new IntRange(1, 1, 512);
    /**
     * controls concentration per concept
     */
    public final IntRange tasklinksPerConcept = new IntRange(2, 1, 32);
    public final IntRange termlinksPerConcept = new IntRange(2, 1, 32);

    final BiFunction<Concept, Derivation, LinkModel> linking;

    public SimpleDeriver(PremiseDeriverRuleSet rules) {
        this(rules.nar.exe::fire, rules.nar::input, rules);
    }

    public SimpleDeriver(Consumer<Predicate<Activate>> source, Consumer<Collection<Task>> target, PremiseDeriverRuleSet rules) {
        this(source, target, rules, ConceptTermLinker);
    }

    public SimpleDeriver(Consumer<Predicate<Activate>> source, Consumer<Collection<Task>> target, PremiseDeriverRuleSet rules, BiFunction<Concept, Derivation, LinkModel> linking) {
        super(source, target, rules);
        this.linking = linking;
    }

    /** randomly samples from list of concepts */
    public static SimpleDeriver forConcepts(NAR n, List<Concept> concepts, Consumer<Collection<Task>> target) {
        int cc = concepts.size();
        assert(cc>0);
        Random rng = n.random();
        Consumer<Predicate<Activate>> forEach = x -> {
            Concept c = concepts.get(rng.nextInt(cc));
            if (c==null)
                return;
            while (x.test(new Activate(c, 1f))) ;
        };
        PremiseDeriverRuleSet rules = Derivers.nal(n, 1, 8);

        return new SimpleDeriver(forEach, target, rules, GlobalTermLinker);
    }

    public static SimpleDeriver forTasks(NAR n, List<Task> tasks) {
        int tt = tasks.size();
        assert(tt>0);
        Random rng = n.random();
        Consumer<Predicate<Activate>> forEach = x -> {
            while (x.test(new Activate(tasks.get(rng.nextInt(tt)).concept(n, true), 1f))) ;
        };
        PremiseDeriverRuleSet rules = Derivers.nal(n, 1, 8);

        return new SimpleDeriver(forEach, n::input, rules, GlobalTermLinker);
    }

    @Override
    protected void derive(NAR n, int iterations, Derivation d) {


        final int[] ii = {iterations * power.intValue()};


        int deriveTTL = n.deriveBranchTTL.intValue();
        int matchTTL = deriveTTL / 4;

        source.accept(a -> {
            if (a == null)
                return true;

            Concept concept = a.id;

            Concept[] templates = templates(concept, nar);

            LinkModel model = linking.apply(concept, d);

            Iterable<TaskLink> tasklinks = model.tasklinks(tasklinksPerConcept.intValue());
            Supplier<PriReference<Term>> termlinker = model.termlinks();

            int termlinks = /*Util.lerp(cPri, 1, */termlinksPerConcept.intValue();
            float taskPriSum = 0;
            for (TaskLink tasklink : tasklinks) {


                Task task = tasklink.get(nar);
                if (task != null) {

                    taskPriSum += task.priElseZero();

                    activate(tasklink, templates, d.random);

                    for (int z = 0; z < termlinks; z++) {

                        PriReference<Term> termlink = termlinker.get();
                        if (termlink != null) {

                            Premise premise = new Premise(task, termlink);
                            if (premise.match(d, matchTTL))
                                if (rules.derivable(d))
                                    d.derive(deriveTTL);


                        }
                    }

                }

            }

            if (taskPriSum > 0)
                concept.templates().linkAndActivate(concept, taskPriSum, nar);
            else {
                
                
                //concept.templates().linkAndActivate(concept, a.pri(), nar);
            }

            return ii[0]-- > 0;
        });

    }

    interface LinkModel {
        Iterable<TaskLink> tasklinks(int max);

        Supplier<PriReference<Term>> termlinks();
    }

    /**
     * termlinks sampled from the derived concept
     */
    public static final BiFunction<Concept, Derivation, LinkModel> ConceptTermLinker = (c, d) -> new LinkModel() {

        private final Random rng = d.random;

        {
            Deriver.commit(d.nar, c.tasklinks(), c.termlinks());
        }

        @Override
        public Iterable<TaskLink> tasklinks(int max) {
            List<TaskLink> t = new FasterList<>(max);
            Bag<?, TaskLink> tl = c.tasklinks();
            tl.sample(rng, Math.min(max, tl.size()), x -> {
                if (x!=null) t.add(x);
            });
            return t;
        }

        @Override
        public Supplier<PriReference<Term>> termlinks() {
            Bag<Term, PriReference<Term>> ct = c.termlinks();
            return () -> ct.sample(rng);
        }
    };

    /**
     * virtual termlinks sampled from concept index
     */
    public static final BiFunction<Concept, Derivation, LinkModel> GlobalTermLinker = (c, d) -> new LinkModel() {

        final NAR n = d.nar;
        final Random rng = d.random;

        {
            Deriver.commit(n, c.tasklinks(), null);
        }

        @Override
        public Iterable<TaskLink> tasklinks(int max) {
            List<TaskLink> t = new FasterList<>(max);
            c.tasklinks().sample(rng, max, x -> {
                if (x!=null) t.add(x);
            });
            return t;
        }

        @Override
        public Supplier<PriReference<Term>> termlinks() {
            return () -> {
                Activate a = n.exe.fire();
                return a != null ? new PLink(a.term(), a.pri()) : null;
            };
        }
    };


}
