package nars.memory;

import jcog.pri.HashedPLink;
import jcog.pri.PLink;
import jcog.pri.bag.impl.hijack.PriHijackBag;
import nars.NAR;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.concept.TaskConcept;
import nars.term.Term;
import nars.time.part.DurLoop;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Created by me on 2/20/17.
 */
public class HijackMemory extends Memory {

    private final PriHijackBag<Term,PLink<Concept>> table;


    /** eliding is faster but records less accurate access statistics.
     *  but if eliding, cache statistics will not reflect the full amount of access otherwise would be necessary */
    private static final boolean ElideGets = false;

    /**
     * how many items to visit during update
     */
    private final float initialTask;
    private final float initialNode;
    private final float getBoost;


    public HijackMemory(int capacity, int reprobes) {
        super();

        initialTask = 1f/ (float) (reprobes + 1);
        initialNode = initialTask / 2.0F; //(non-task) node concepts less valuable

        getBoost = (float) (1 /Math.sqrt((double) capacity));

        this.table = new PriHijackBag<>(capacity, reprobes) {

            {
                resize(capacity);
            }

            @Override
            public Term key(PLink<Concept> value) {
                return value.get().term();
            }

            @Override
            protected boolean regrowForSize(int s, int sp) {
                return false;
            }

            @Override
            protected boolean reshrink(int length) {
                return false;
            }

            @Override
            public int spaceMin() {
                return capacity();
            }

            @Override
            protected boolean replace(float incoming, PLink<Concept> existing, float existingPri) {
				return existing.get() instanceof PermanentConcept ? false : super.replace(incoming, existing, existingPri);
            }
//
//            @Override
//            public void onAdd(PLink<Concept> conceptPLink) {
//                super.onAdd(conceptPLink);
//            }

            @Override
            public void onRemove(PLink<Concept> value) {
                HijackMemory.this.onRemove(value.get());
            }
        };

    }

    @Override
    public void start(NAR nar) {
        super.start(nar);
        //    private long now;
        //    private int dur;
        DurLoop onDur = nar.onDur(this::commit);
        int forgetPeriodDurs = 64;
        onDur.durs((float) forgetPeriodDurs);
    }

    /**
     * measures accesses for eviction, so do not elide
     */
    @Override
	public final boolean elideConceptGets() {
        return ElideGets;
    }

    private void commit() {
        float forgetTemperature = 0.1f;
        table.commit(table.forget(forgetTemperature));
    }

    @Override
    public Concept get(Term key, boolean createIfMissing) {
        PLink<Concept> x = table.get(key);
        if (x != null) {
            boost(x, getBoost);
            return x.get();
        } else {
            return createIfMissing ? create(key) : null;
        }
    }

    public @Nullable Concept create(Term key) {
        Concept kc = nar.conceptBuilder.apply(key);
        PLink<Concept> inserted = table.put(new HashedPLink<>(kc, priPut(key, kc)));
        if (inserted == null) {
            return kc;
        } else {
            boost(inserted, getBoost);
            return inserted.get();
        }
    }

    private float priPut(Term key, Concept kc) {
        return kc instanceof TaskConcept ? initialTask : initialNode;
    }

    private void boost(PLink<Concept> x, float boost) {
        x.priAdd(boost);
        table.pressurize(boost);
    }


    @Override
    public void set(Term key, Concept value) {
        PLink<Concept> existing = table.get(key);
        if (existing==null || (existing.get()!=value && !(existing.get() instanceof PermanentConcept))) {
//            remove(key);
            PLink<Concept> inserted = table.put(new HashedPLink<>(value, initialTask));
            /* other */
            if (inserted == null && (value instanceof PermanentConcept)) {
                throw new RuntimeException("unresolvable hash collision between PermanentConcepts: " + null + ' ' + value);
            }
        }
    }

    @Override
    public void clear() {
        table.clear();
    }

    @Override
    public void forEach(Consumer<? super Concept> c) {

        for (PLink<Concept> k : table) {
            c.accept(k.get());
        }

    }

    @Override
    public int size() {
        return table.size(); /** approx since permanent not considered */
    }

    @Override
    public String summary() {
        return table.size() + " concepts";
    }

    @Override
    public @Nullable Concept remove(Term entry) {
        PLink<Concept> e = table.remove(entry);
        return e != null ? e.get() : null;
    }

    @Override
    public Stream<Concept> stream() {
        return table.stream().map(Supplier::get);
    }

//    protected float score(Concept c) {
//        float beliefConf = w2c((float) c.beliefs().streamTasks().mapToDouble(t -> t.evi(now, dur)).average().orElse(0));
//        float goalConf = w2c((float) c.goals().streamTasks().mapToDouble(t -> t.evi(now, dur)).average().orElse(0));
//        float talCap = c.tasklinks().size() / (1f + c.tasklinks().capacity());
//        float telCap = c.termlinks().size() / (1f + c.termlinks().capacity());
//        return Util.or(((talCap + telCap) / 2f), (beliefConf + goalConf) / 2f) /
//                (1 + ((c.complexity() + c.volume()) / 2f) / nar.termVolumeMax.intValue());
//    }
//
//    protected void forget(PLink<Termed> x, Concept c, float amount) {
//
//        c.tasklinks().setCapacity(Math.round(c.tasklinks().capacity() * (1f - amount)));
//        c.termlinks().setCapacity(Math.round(c.termlinks().capacity() * (1f - amount)));
//
//        x.priMult(1f - amount);
//    }

}
