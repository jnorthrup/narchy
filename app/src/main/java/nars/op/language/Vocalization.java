package nars.op.language;

import com.google.common.collect.TreeBasedTable;
import jcog.data.list.FasterList;
import nars.$;
import nars.NAR;
import nars.control.NARPart;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.util.TruthAccumulator;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Consumer;

public class Vocalization extends NARPart {

    public static final Term PREPOSITION = $.INSTANCE.the("preposition");
    public static final Term PRONOUN = $.INSTANCE.the("pronoun");
    /**
     * when, word, truth
     */
    final TreeBasedTable<Long, Term, TruthAccumulator> vocalize = TreeBasedTable.create();
    private final Consumer<Term> speak;
    private final float durationsPerWord;
    private float energy;
    private static final float expectationThreshold = 0.5f;

    public Vocalization(NAR nar, float durationsPerWord, Consumer<Term> speak) {
        super();
        this.durationsPerWord = durationsPerWord;
        this.speak = speak;
        this.energy = (float) 0;
        nar.add(this);
    }

    @Override
    protected void starting(NAR nar) {
        on(
                nar.onDur(new Runnable() {
                    @Override
                    public void run() {
                        energy = Math.min(1f, energy + 1f / (Vocalization.this.durationsPerWord));
                        if (energy >= 1f) {
                            energy = (float) 0;
                            Vocalization.this.next();
                        }
                    }
                }),
                nar.eventClear.on(this::clear)
        );

    }

    public void speak(@Nullable Term word, long when, @Nullable Truth truth) {
        if (word == null)
            return;


//        if (when < nar.time() - nar.dur() * durationsPerWord) {
//            return;
//        }

        TruthAccumulator ta;
        synchronized (vocalize) {
            ta = vocalize.get(when, word);
            if (ta == null) {
                ta = new TruthAccumulator();
                vocalize.put(when, word, ta);
            }
        }

        ta.add(truth);

//        System.out.println(when + " " + word + " " + truth);

    }

    private void clear() {
        synchronized (vocalize) {
            vocalize.clear();
        }
    }

    public boolean next() {


        float dur = nar.dur() * durationsPerWord;
        long now = nar.time();
        long startOfNow = now - (long) (int) Math.ceil((double) dur);
        long endOfNow = now + (long) (int) Math.floor((double) dur);


        FasterList<Pair<Term, Truth>> pending = new FasterList<Pair<Term, Truth>>(0);
        synchronized (vocalize) {


            SortedSet<Long> tt = vocalize.rowKeySet().headSet(endOfNow);

            if (!tt.isEmpty()) {
                LongArrayList ll = new LongArrayList(tt.size());
                for (Long aLong : tt) {
                    ll.add(aLong);
                }

                ll.forEach(new LongProcedure() {
                    @Override
                    public void value(long t) {
                        Set<Map.Entry<Term, TruthAccumulator>> entries = vocalize.row(t).entrySet();
                        if (t >= startOfNow) {
                            for (Map.Entry<Term, TruthAccumulator> e : entries) {
                                Truth x = e.getValue().commitSum();
                                if (x.expectation() > expectationThreshold)
                                    pending.add(Tuples.pair(e.getKey(), x));
                            }
                        }
                        entries.clear();
                    }
                });
            }
        }
        if (pending.isEmpty())
            return true;


        Term spoken = decide(pending);
        if (spoken != null)
            speak.accept(spoken);


        return true;
    }

    /**
     * default greedy decider by truth expectation
     */
    private @Nullable
    static Term decide(FasterList<Pair<Term, Truth>> pending) {
        return pending.max(new Comparator<Pair<Term, Truth>>() {
            @Override
            public int compare(Pair<Term, Truth> a, Pair<Term, Truth> b) {
                float ta = a.getTwo().expectation();
                float tb = b.getTwo().expectation();
                if (ta > tb) {
                    int tab = Float.compare(ta, tb);
                    return tab;
                } else {
                    return a.getOne().compareTo(b.getOne());
                }
            }
        }).getOne();
    }
}
