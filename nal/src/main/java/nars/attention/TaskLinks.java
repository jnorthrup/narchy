package nars.attention;

import jcog.data.NumberX;
import jcog.data.list.FasterList;
import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.pri.Forgetting;
import jcog.pri.PriMap;
import jcog.pri.bag.Sampler;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.bag.impl.hijack.PriHijackBag;
import jcog.pri.op.PriMerge;
import jcog.pri.op.PriReturn;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.derive.Derivation;
import nars.link.AtomicTaskLink;
import nars.link.TaskLink;
import nars.link.TermLinker;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** essentially a wrapper for a TaskLink bag for use as a self-contained attention set */
public class TaskLinks implements Sampler<TaskLink> {

    /**
     * short target memory, TODO abstract and remove, for other forms of attention that dont involve TaskLinks or anything like them
     */
    public nars.link.TaskLinkBag links = null;

    /** tasklink forget rate */
    public final FloatRange decay = new FloatRange(0.5f,  0, 1f /* 2f */);

    /** (post-)Amp: tasklink propagation rate */
    public final FloatRange amp = new FloatRange(0.5f,  0, 2f /* 2f */);

    /** tasklink retention rate:
     *  0 = deducts all propagated priority from source tasklink (full resistance)
     *  1 = deducts no propagated priority (superconductive)
     **/
    public final FloatRange sustain = new FloatRange(0.5f,  0, 1f );



    /** capacity of the links bag */
    public final IntRange linksMax = new IntRange(256, 0, 8192) {
        @Override
        @Deprecated protected void changed() {
            nars.link.TaskLinkBag a = links;
            if (a != null)
                a.setCapacity(intValue());
        }
    };



    private PriMerge merge = Param.tasklinkMerge;


    public TaskLinks(/*TODO bag as parameter */) {
        int c = linksMax.intValue();

        links = new nars.link.TaskLinkBag(
                new TaskLinkArrayBag(c, merge)
                //new TaskLinkHijackBag(c, 5)
        );

        links.setCapacity(c);
    }


    /** prevents multiple threads from commiting the bag at once */
    private final AtomicBoolean busy = new AtomicBoolean(false);

    /** updates */
    public final void commit() {
        if (busy.compareAndSet(false,true)) {
            try {
                links.commit(
                        Forgetting.forget(links, decay.floatValue())
                );
            } finally {
                busy.set(false);
            }
        }
    }

    @Override
    public void sample(Random rng, Function<? super TaskLink, Sampler.SampleReaction> each) {
        links.sample(rng, each);
    }

    /** resolves and possibly sub-links a link target
     * TODO abstract this as one possible strategy
     * */
    @Nullable public Term term(TaskLink link, Task task, Derivation d) {

        Term t = link.to();

        if (t.op().conceptualizable) {

            Concept ct;
            byte punc = task.punc();

            float p =
                    link.priPunc(punc);
            //task.priElseZero();

            float sustain = this.sustain.floatValue();

            NAR nar = d.nar();

//            boolean self = s.equals(t);

            ct = nar.conceptualize(t);
            if (ct != null) {
                t = ct.term();

                Term u = null;
                TermLinker linker = ct.linker();
                if (linker != TermLinker.NullLinker && !((FasterList) linker).isEmpty())
                    //grow-ahead: s -> t -> u
                    u = linker.sample(d.random);
                else {
                    //loopback
                    if (t instanceof Atom) {
                        //why is this necessary
                        float probability =
                                0.5f;
                        //1f / (1 + link.from().volume()/2f);
                        //1f / (1 + link.from().volume());
                        //1 - 1f / (1 + s.volume());
                        //1 - 1f / (1 + t.volume());

                        if (d.random.nextFloat() <= probability) {
                            //sample active tasklinks for a tangent match to the atom
                            Predicate<TaskLink> filter =
                                    x -> !link.equals(x);

                            return links.atomTangent(ct, punc, filter, d.time(),
                                    d.dur() * Param.belief.REMEMBER_REPEAT_THRESH_DURS /* repurposed */, d.random);

//                        if (u!=null && u.equals(s)) {
////                            u = links.atomTangent(ct, ((TaskLink x)->!link.equals(x)), d.time, 1, d.random);//TEMPORARY
//                            throw new WTF();
//                        }

//                        } else {
//
//
//                            //link(t, s, punc, p*subRate); //reverse echo
//                        }
                        }
                    }

                }


                if (u != null && !t.equals(u)) {


//                //TODO abstact activation parameter object
//                float subRate =
//                        1f;
//                //1f/(t.volume());
//                //(float) (1f/(Math.sqrt(s.volume())));
//
//
//                float inflation = 1; //TODO test inflation<1
//                float want = p * subRate / 2;
//                float p =
//                        inflation < 1 ? Util.lerp(inflation, link.take(punc, want*inflation), want) : want;

                    float pAmp = p * amp.floatValue();
                    final Term s = link.from();

                    //CHAIN pattern
                    link(s, u, punc, pAmp); //forward (hop)
                    //link(u, s, punc, pp); //reverse (hop)
                    //link(t, u, punc, pp); //forward (adjacent)
                    //link(u, t, punc, pp); //reverse (adjacent)


                    if (sustain < 1) {
                        link.take(punc, pAmp * (1 - sustain));
                    }


                    //link(s, t, punc, ); //redundant
                    //link(t, s, punc, pp); //reverse echo

//                if (self)
//                    t = u;

                } else {
//                int n = 1;
//                float pp = p * conductance / n;
//
//                link(t, s, punc, pp); //reverse echo

                }
            }
        }
        //link.take(punc, pp*n);

        //System.out.println(s + "\t" + t + "\t" + u);

        return t;
    }

    private void link(Term s, Term u, byte punc, float p) {
        link(new AtomicTaskLink(s, u, punc, p));
    }

    public void link(TaskLink x) {
        links.putAsync(x);
    }

    public void link(TaskLink... xx) {
        for (TaskLink x : xx)
            link(x);
    }

    /** initial tasklink activation for an input task
     * @return*/
    public boolean link(Task task, @Nullable Concept taskConcept, NAR n) {

        Termed cc = taskConcept == null ? task : taskConcept;
        Concept c =
                n.conceptualize(cc);
                //n.activate(cc, pri, true);
        if (c == null)
            return false;

        float pri = task.pri();
        if (pri!=pri)
            return false;


        link(new AtomicTaskLink(c.term(), task.punc(), pri));


        ((TaskConcept) c).value(task, n);

        return true;
    }

    @Deprecated public Stream<Term> terms() {
        return links.stream()
                .flatMap(x -> Stream.of(x.from(), x.to()))
                .distinct();
    }

    public final Stream<Concept> concepts(NAR n) {
        return terms()
            .map(n::concept)
            .filter(Objects::nonNull)
        ;
    }

    public final void clear() {
        links.clear();
    }

    public final Iterator<TaskLink> iterator() {
        return links.iterator();
    }


    private static class TaskLinkArrayBag extends ArrayBag<TaskLink, TaskLink> {

        public TaskLinkArrayBag(int initialCapacity, PriMerge merge) {
            super(merge, initialCapacity, PriMap.newMap(false));
        }

        @Override
        protected float merge(TaskLink existing, TaskLink incoming, float incomingPri) {
            return existing.merge(incoming, merge(), PriReturn.Overflow);
        }
        //        @Override
//        protected float sortedness() {
//            return 0.33f;
//        }

        @Override
        public TaskLink key(TaskLink value) {
            return value;
        }

    }

    private static class TaskLinkHijackBag extends PriHijackBag<TaskLink, TaskLink> {

        public TaskLinkHijackBag(int initialCap, int reprobes) {
            super(initialCap, reprobes);
        }

        @Override
        public TaskLink key(TaskLink value) {
            return value;
        }

        @Override
        protected TaskLink merge(TaskLink existing, TaskLink incoming, NumberX overflowing) {
            float o = existing.merge(incoming, merge(), PriReturn.Overflow);
            if (overflowing!=null)
                overflowing.add(o);
            return existing;
        }
    }


}
