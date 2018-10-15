package nars.derive.impl;

import jcog.data.set.ArrayHashSet;
import jcog.math.IntRange;
import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
import jcog.pri.bag.Sampler;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.derive.*;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.link.Activate;
import nars.link.TaskLink;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.function.*;

/**
 * samples freely from concept, termlink, and tasklink bags without any buffering of premises
 */
public class SimpleDeriver extends Deriver {

    /**
     * controls concentration per concept
     */
    public final IntRange tasklinksPerConcept = new IntRange(2, 1, 32);
    public final IntRange termlinksPerConcept = new IntRange(2, 1, 32);

    final BiFunction<Concept, Derivation, LinkModel> linking;

    public SimpleDeriver(PremiseDeriverRuleSet rules) {
        this(rules.nar.attn::fire, rules);
    }

    public SimpleDeriver(Consumer<Predicate<Activate>> source, PremiseDeriverRuleSet rules) {
        this(source, rules, ConceptTermLinker);
    }

    public SimpleDeriver(Consumer<Predicate<Activate>> source, PremiseDeriverRuleSet rules,
                         BiFunction<Concept, Derivation, LinkModel> linking) {
        super(source, rules);
        this.linking = linking;
    }

    /** randomly samples from list of concepts */
    public static SimpleDeriver forConcepts(NAR n, PremiseDeriverRuleSet rules, List<Concept> concepts) {

        return forConcepts(n, rules, concepts, GlobalTermLinker);
    }


    public static SimpleDeriver forConcepts(NAR n, PremiseDeriverRuleSet rules, List<Concept> concepts, List<Term> terms) {
        return forConcepts(n, rules, concepts, ListTermLinker(terms));
    }

    public static SimpleDeriver forConcepts(NAR n, PremiseDeriverRuleSet rules, List<Concept> concepts, BiFunction<Concept, Derivation, LinkModel> linker) {
        int cc = concepts.size();
        assert(cc>0);
        Random rng = n.random();
        Consumer<Predicate<Activate>> forEach = x -> {
            Concept c = concepts.get(rng.nextInt(cc));
            if (c==null)
                return;
            while (x.test(new Activate(c, 1f))) ;
        };


        return new SimpleDeriver(forEach, rules, linker);
    }

    public static SimpleDeriver forTasks(NAR n, List<Task> tasks) {
        int tt = tasks.size();
        assert(tt>0);
        Random rng = n.random();
        Consumer<Predicate<Activate>> forEach = x -> {
            while (x.test(new Activate(n.conceptualize(tasks.get(rng.nextInt(tt))), 1f))) ;
        };
        PremiseDeriverRuleSet rules = Derivers.nal(n, 1, 8);

        return new SimpleDeriver(forEach, rules, GlobalTermLinker);
    }

    @Override
    protected void derive(Derivation d, BooleanSupplier kontinue) {






        int deriveTTL = d.nar.deriveBranchTTL.intValue();
        int matchTTL = matchTTL();

        source.accept(a -> {
            assert(a!=null);

            Concept concept = a.id;



            LinkModel model = linking.apply(concept, d);

            d.firedTaskLinks.clear();
            ArrayHashSet<TaskLink> fired = model.tasklinks(tasklinksPerConcept.intValue(), d.firedTaskLinks);
            Supplier<Term> beliefTerms = model.beliefTerms();

            int termlinks = /*Util.lerp(cPri, 1, */termlinksPerConcept.intValue();
//            float taskPriSum = 0;


            fired.forEach(tasklink -> {



                Task task = tasklink.get(nar);
                if (task != null) {

                    for (int z = 0; z < termlinks; z++) {

                        Term b = beliefTerms.get();
                        if (b != null) {
                            new Premise(task, b).derive(d, matchTTL, deriveTTL);
                        }

                    }

                }
            });

            //System.out.println((((DerivedTasks.DerivedTasksBag)d.deriver.derived).tasks.map.values()));

            concept.linker().link(a, d);

            return kontinue.getAsBoolean();
        });

    }

    interface LinkModel {
        /** buffer is not automatically cleared here, do that first if neceessary */
        ArrayHashSet<TaskLink> tasklinks(int max, ArrayHashSet<TaskLink> buffer);

        Supplier<Term> beliefTerms();
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
        public ArrayHashSet<TaskLink> tasklinks(int max, ArrayHashSet<TaskLink> buffer) {

            Bag<?, TaskLink> tl = c.tasklinks();
            tl.sample(rng, Math.min(max, tl.size()), x -> {
                if (x!=null) buffer.add(x);
            });

            return buffer;

        }

        @Override
        public Supplier<Term> beliefTerms() {
            Sampler<PriReference<Term>> ct = c.termlinks();
            return () -> {
                @Nullable PriReference<Term> t = ct.sample(rng);
                return t != null ? t.get() : null;
            };
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
        public ArrayHashSet<TaskLink> tasklinks(int max, ArrayHashSet<TaskLink> buffer) {
            c.tasklinks().sample(rng, max, x -> {
                if (x!=null) buffer.add(x);
            });
            return buffer;
        }

        @Override
        public Supplier<Term> beliefTerms() {
            return () -> {
                Activate a = n.attn.fire();
                return a != null ? a.term() : null;
            };
        }
    };

    public static BiFunction<Concept, Derivation, LinkModel> ListTermLinker(List<Term> terms) {
        return (c, d) -> new LinkModel() {

            final NAR n = d.nar;
            final Random rng = d.random;

            {
                Deriver.commit(n, c.tasklinks(), null);
            }

            @Override
            public ArrayHashSet<TaskLink> tasklinks(int max, ArrayHashSet<TaskLink> buffer) {
                c.tasklinks().sample(rng, max, x -> {
                    if (x != null) buffer.add(x);
                });
                return buffer;
            }

            @Override
            public Supplier<Term> beliefTerms() {
                return () -> {
                    return terms.get(rng.nextInt(terms.size()));
                };
            }
        };
    }


}
