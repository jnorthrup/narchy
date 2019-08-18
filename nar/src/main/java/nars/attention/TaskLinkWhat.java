package nars.attention;

import jcog.TODO;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.link.TaskLink;
import nars.link.TaskLinks;
import nars.task.util.PriBuffer;
import nars.term.Term;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * implements attention with a TaskLinks graph.
 * the standard attention context design currently.
 */
public class TaskLinkWhat extends What {

    public final TaskLinks links;

    public TaskLinkWhat(Term id, int capacity, PriBuffer<Task> in) {
        this(id, new TaskLinks(), in);
        //new TaskLinks.AtomCachingTangentTaskLinks();
        //TaskLinks.DirectTangentTaskLinks.the;
        //new TaskLinks.NullTangentTaskLinks();
        links.linksMax.set(capacity);
    }

    public TaskLinkWhat(Term id, TaskLinks links, PriBuffer<Task> in) {
        super(id, in);
        this.links = links;
    }


    @Override
    protected void starting(NAR nar) {
        float narDUR = nar.dur();

        this.dur.set(narDUR); //initializes value

        super.starting(nar);
    }

    @Override
    protected void commit(NAR nar) {
        links.commit(this);
    }

    @Override
    public void link(TaskLink t) {
        links.link(t);
    }


    @Override
    public TaskLink link(Task t, float p) {
        return links.link(t, p);
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
    public void sampleUnique(Random rng, Predicate<? super TaskLink> each) {
        links.sampleUnique(rng, each);
    }

    @Override
    public final void writeExternal(ObjectOutput objectOutput) {
        throw new TODO();
    }

    @Override
    public final void readExternal(ObjectInput objectInput) {
        throw new TODO();
    }




}
