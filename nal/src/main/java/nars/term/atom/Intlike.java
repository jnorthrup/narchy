package nars.term.atom;

import com.google.common.collect.Range;

import java.util.function.IntConsumer;

public interface Intlike extends Atomic {

    Range range();


    void forEachInt(IntConsumer c);


}
