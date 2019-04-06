package nars.attention;

import jcog.TODO;
import jcog.pri.Prioritizable;
import jcog.pri.bag.Sampler;
import nars.NAR;
import nars.attention.derive.DefaultPuncWeightedDerivePri;
import nars.control.NARPart;
import nars.link.TaskLink;
import nars.task.util.TaskBuffer;
import nars.term.Term;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Random;
import java.util.function.Function;

/**
 *  an attention prioritizes the components a specific aspect of experience.
 *
 *  what these sets of consist of, their priority,
 *  how these change, and can *be* changed with time.
 *
 *  they are meant to be helpful constructs to be created by developers, both at design and runtime,
 *  and at the same time accessible to any learned metaprogramming desire formed by the system itself.
 *
 *  these form semi-self-contained memories, with an input buffer for receiving
 *  input specific to it.  attentions will receive some share of the mental burden in proportion
 *  to their relative prioritization.  this priority distribution of WHATs (consisting of priorities themselves)
 *  can form a product with the priority distribution of HOWs determining the overall
 *  runtime dynamics of a system.
 *
 *  attentions are meant to be serializable, snapshottable, restoreable, live edited,
 *  filtered, cloned, etc.  they are like directories and their contents (ex: TaskLinks) are like files.
 *
 *  attentions share a memory of concepts (and thus their beliefs and questions) but only become
 *  "acquainted" with the content of another by receiving a reference to foreign "concepts".  this allows
 *  the formation of compartmentalized hierarchies of "mental organs" which hide and concentrate their localized
 *  work without fully contaminating the attention and resources of other parts.
 *
 *  the attention hierarchy is meant to be fully dynamic, adaptive, and metaprogrammable by the system at runtime
 *  through a minimal API.  thus Attention's are referred to by a Term so that operations upon them may
 *  be conceptualized and self-executed.
  */
abstract public class Attention extends NARPart implements Prioritizable, Sampler<TaskLink>, Externalizable {

    public final PriNode pri;

    /** input bag */
    public final TaskBuffer in;

    /** default deriver pri model
     *      however, each deriver instance can also be configured individually and dynamically.
     * */
    public DerivePri derivePri =
            //new DirectDerivePri();
            //new DefaultDerivePri();
            new DefaultPuncWeightedDerivePri();

    protected Attention(Term id, TaskBuffer in) {
        super(id);
        this.pri = new PriNode(this);
        this.in = in;
    }

    @Override
    protected void starting(NAR nar) {
        super.starting(nar);

        on(
                nar.eventClear.on(this::clear),
                nar.onDur(this::commit)
        );

    }

    /** called periodically, ex: per duration, for maintenance like gradual forgetting and merging new input.
     *  only one thread will be in this method at a time guarded by an atomic guard */
    abstract protected void commit();

    /** explicitly return the attention to a completely or otherwise reasonably quiescent state.
     *  how exactly can be decided by the implementation. */
    abstract protected void clear();

    /** implements attention with a TaskLinks graph */
    public static class TaskLinksAttention extends Attention {

        final TaskLinks links = new TaskLinks();

        protected TaskLinksAttention(Term id, TaskBuffer in) {
            super(id, in);
        }

        @Override
        protected void commit() {
            links.commit();
        }

        @Override
        protected void clear() {
            links.clear();
        }

        @Override
        public void sample(Random rng, Function<? super TaskLink, SampleReaction> each) {
            links.sample(rng, each);
        }

        @Override
        public void writeExternal(ObjectOutput objectOutput) throws IOException {
            throw new TODO();
        }

        @Override
        public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
            throw new TODO();
        }
    }
}
