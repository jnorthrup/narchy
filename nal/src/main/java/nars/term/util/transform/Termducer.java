package nars.term.util.transform;

import jcog.data.set.ArrayHashSet;
import nars.$;
import nars.io.IO;
import nars.term.Term;
import nars.term.atom.IdempotInt;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.fst.Builder;
import org.apache.lucene.util.fst.ByteSequenceOutputs;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.Util;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.eclipse.collections.impl.tuple.Tuples;

import java.io.IOException;

import static org.apache.lucene.util.fst.Util.toIntsRef;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * using FST finite state transducer to transform terms (experiment)
 * see http://citeseerx.ist.psu.edu/viewdoc/download;jsessionid=433691F581BC2E624185F79465C5329?doi=10.1.1.24.3698&rep=rep1&type=pdf
 * <p>
 * not thread safe
 *
 * TODO use either this or the ByteSeek stuff
 */
public class Termducer {

    final ArrayHashSet<Pair<IntsRef, Twin<Term>>> xy = new ArrayHashSet<>();
    final IntsRefBuilder ib = new IntsRefBuilder();

    public void put(Term x, Term y) {
        xy.add(pair(ints(x), Tuples.twin(x, y)));
    }



    public FST build() throws IOException {
        Builder b = new Builder(FST.INPUT_TYPE.BYTE1, ByteSequenceOutputs.getSingleton());

        xy.list.sort(Comparators.byFunction(Pair::getOne));

        for (Pair<IntsRef, Twin<Term>> z : xy.list) {
            try {
                Term y = z.getTwo().getTwo();
                b.add(z.getOne(), bytes(y));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return b.finish();
    }

    private IntsRef ints(Term x) {
        return toIntsRef(bytes(x), ib).clone();
    }

    public static BytesRef bytes(Term y) {
        return new BytesRef(IO.termToBytes(y));
    }

    public static void main(String[] args) throws IOException {


        Termducer t = new Termducer();
        t.put($.INSTANCE.$$("(a-->b)"), $.INSTANCE.$$("(1-->2)"));
        t.put($.INSTANCE.$$("(a-->(b,c))"), $.INSTANCE.$$("(1-->(2,3))"));
        t.put($.INSTANCE.$$("add(1,1))"), IdempotInt.the(2));

        FST f = t.build();
        System.out.println("RAM bytes used: " + f.ramBytesUsed());
        for (Pair x : t.xy.list) {
            BytesRef y = (BytesRef) Util.get(f, (IntsRef) x.getOne());
            System.out.println(y + " " + IO.bytesToTerm(y.bytes));
        }
        System.out.println(f);
    }
}
