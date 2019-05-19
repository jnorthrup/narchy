package nars.attention;

import jcog.TODO;
import jcog.math.IntRange;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.derive.model.Derivation;
import nars.derive.premise.PremiseBuffer;
import nars.link.TaskLink;
import nars.link.TaskLinks;
import nars.task.util.PriBuffer;
import nars.term.Term;
import nars.time.event.WhenTimeIs;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;

/** implements attention with a TaskLinks graph.
 *  the standard attention context design currently. */
public class TaskLinkWhat extends What {

    /** present-moment perception duration, in global clock cycles,
     *  specific to this What, and freely adjustable */
    public final IntRange dur = new IntRange(1, 1, 1000);

    public final TaskLinks links =
            new TaskLinks.DirectTangentTaskLinks();
            //new TaskLinks.AtomCachingTangentTaskLinks();

    final PremiseBuffer premises = new PremiseBuffer();

    public TaskLinkWhat(Term id, int capacity, PriBuffer<Task> in) {
        super(id, in);
        links.linksMax.set(capacity);
    }

    @Override
    protected void starting(NAR nar) {

        dur.set(nar.dur()); //initializes value

        super.starting(nar);
    }

    @Override
    public int dur() {
        return dur.intValue();
    }

    @Override
    protected void commit(NAR nar) {
        premises.commit();
        links.commit();
    }

    @Override
    public void clear() {
        links.clear();
    }

    @Override
    public Stream<Concept> concepts() {
        return links.concepts(nar);
    }

    @Override
    public final Iterator<TaskLink> iterator() {
        return links.iterator();
    }

    @Override
    public final void sample(Random rng, Function<? super TaskLink, SampleReaction> each) {
        links.sample(rng, each);
    }

    @Override
    public final void writeExternal(ObjectOutput objectOutput) {
        throw new TODO();
    }

    @Override
    public final void readExternal(ObjectInput objectInput) {
        throw new TODO();
    }


    /**
     * samples premises
     * thread-safe, for use by multiple threads
     */
    public final void derive(int premisesPerIteration, int termlinksPerTaskLink, int matchTTL, int deriveTTL, Derivation d) {
        premises.derive(
                WhenTimeIs.now(d),
                premisesPerIteration,
                termlinksPerTaskLink,
                matchTTL, deriveTTL,
                links, d);
    }


}
