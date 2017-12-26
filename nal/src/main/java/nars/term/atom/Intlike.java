package nars.term.atom;

import com.google.common.collect.Range;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

import java.util.function.IntConsumer;

public interface Intlike extends Atomic {

    Range range();


    void forEachInt(IntConsumer c);

//    @Override
//    default Term eval(TermContext context) {
//        return this;
//    }
//
//    @Override
//    default Term evalSafe(TermContext context, int remain) {
//        return this;
//    }

}
