package nars.attention;

import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.link.TaskLink;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** proxies to another What */
public class ProxyWhat extends What {

    public final What what;

    public ProxyWhat(What what) {
        super(what.id, what.in);
        this.what = what;
    }

    @Override
    public float dur() {
        return what.dur();
    }

    @Override
    protected void commit(NAR nar) {
        what.commit(nar);
    }

    @Override
    public void clear() {
        what.clear();
    }

    @Override
    public void link(TaskLink t) {
        what.link(t);
    }

    @Override
    public TaskLink link(Task t, float pri) {
        return what.link(t, pri);
    }

    @Override
    public Stream<Concept> concepts() {
        return what.concepts();
    }

    @Override
    public void writeExternal(ObjectOutput objectOutput) throws IOException {
        what.writeExternal(objectOutput);
    }

    @Override
    public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
        what.readExternal(objectInput);
    }

    @Override
    public Iterator<TaskLink> iterator() {
        return what.iterator();
    }

    @Override
    public void sample(Random rng, Function<? super TaskLink, SampleReaction> each) {
        what.sample(rng, each);
    }

    @Override
    public void sampleUnique(Random rng, Predicate<? super TaskLink> predicate) {
        what.sampleUnique(rng, predicate);
    }
}
