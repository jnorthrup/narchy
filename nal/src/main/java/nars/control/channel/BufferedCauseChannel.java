package nars.control.channel;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import jcog.util.ArrayIterator;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class BufferedCauseChannel implements Consumer {

    private final LinkedHashSet buffer;
    private final CauseChannel target;

    public BufferedCauseChannel(CauseChannel c) {
        target = c;
        buffer = new LinkedHashSet();
    }


    public final void input(Object x) {
        boolean uniqueAdded = buffer.add(x);
//        if (!uniqueAdded) {
//            System.out.println("duplicate detected");
//        }
    }


    public void input(Object... xx) {
        Collections.addAll(buffer, xx);
    }


    public void input(Stream x) {
        x.forEach(buffer::add);
    }

    public void input(Iterator xx) {
        Iterators.addAll(buffer, xx);
    }

    public void input(Iterable xx) {
        Iterables.addAll(buffer, xx);
    }

    public void input(Collection xx) {
        Collections.addAll(buffer, xx);
    }

    public int commit() {
        int size = buffer.size();
        if (size > 0) {
            target.input(ArrayIterator.get(buffer.toArray()));
            buffer.clear();
        }
        return size;
    }

    @Override
    public final void accept(Object o) {
        input(o);
    }
}
