package nars.index.concept;

import jcog.pri.PLink;
import jcog.pri.PLinkHashCached;
import jcog.pri.bag.impl.hijack.PLinkHijackBag;
import nars.NAR;
import nars.concept.PermanentConcept;
import nars.control.DurService;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Created by me on 2/20/17.
 */
public class HijackConceptIndex extends MaplikeConceptIndex {

    private final PLinkHijackBag<Termed> table;


    private int forgetEveryDurs = 32;
    private float forgetTemperature = 0.5f;

    /**
     * how many items to visit during update
     */
    private final float initial = 0.5f;
    private final float getBoost = 0.02f;

    //    private long now;
//    private int dur;
    private DurService onDur;


    public HijackConceptIndex(int capacity, int reprobes) {
        super();

        this.table = new PLinkHijackBag<>(capacity, reprobes) {

            {
                resize(capacity);
            }


            @Override
            public Termed key(PLinkHashCached<Termed> value) {
                return value.id.term();
            }

            @Override
            protected boolean attemptRegrowForSize(int s) {
                return false;
            }

            @Override
            protected boolean keyEquals(Object k, PLinkHashCached<Termed> p) {
                return k.hashCode() == p.hash && p.id.equals(k);
            }

            @Override
            protected boolean replace(float incomingPri, PLinkHashCached<Termed> existing) {

                boolean existingPermanent = existing.id instanceof PermanentConcept;
                if (existingPermanent) {
                    return false;
                }

                return super.replace(incomingPri, existing);
            }


        };

    }

    @Override
    public void init(NAR nar) {
        super.init(nar);
        onDur = DurService.on(nar, this::commit);
        onDur.durs(forgetEveryDurs);
    }

    /**
     * measures accesses for eviction, so do not elide
     */
    @Override
    protected final boolean elideConceptGets() {
        return false;
    }

    private void commit() {
        table.commit(table.forget(forgetTemperature));
    }

    @Override
    public @Nullable Termed get(Term key, boolean createIfMissing) {
        @Nullable PLink<Termed> x = table.get(key);
        if (x != null) {


            boost(x, getBoost);
            return x.id;

        }

        if (createIfMissing) {
            Termed kc = nar.conceptBuilder.apply(key, null);
            if (kc != null) {
                PLink<Termed> inserted = table.put(new PLinkHashCached<>(kc, initial));
                if (inserted != null) {
                    return kc;


                } else {
                    table.put(new PLinkHashCached<>(kc, initial));
                    return null;
                }
            }
        }

        return null;
    }

    private void boost(PLink<Termed> x, float boost) {
        x.priAdd(boost);
        table.pressurize(boost);
    }


    @Override
    public void set(Term src, Termed target) {
        remove(src);
        PLink<Termed> inserted = table.put(new PLinkHashCached<>(target, 1f));
        if (inserted == null && target instanceof PermanentConcept) {
            throw new RuntimeException("unresolvable hash collision between PermanentConcepts: " + inserted + ' ' + target);
        }
    }

    @Override
    public void clear() {
        table.clear();
    }

    @Override
    public void forEach(Consumer<? super Termed> c) {

        table.forEachKey(c);

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
    public Termed remove(Term entry) {
        PLinkHashCached<Termed> e = table.remove(entry);
        return e != null ? e.id : null;
    }

    @Override
    public Stream<Termed> stream() {
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
