package nars.control.channel;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import jcog.data.ArrayHashSet;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class BufferedCauseChannel implements Consumer {

    public final Set buffer;
    private final CauseChannel target;

    public BufferedCauseChannel(CauseChannel c) {
        target = c;
        buffer = new ArrayHashSet();
    }


    public final void input(Object x) {
        boolean uniqueAdded = buffer.add(x);



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
            target.input(buffer.iterator());
            buffer.clear();
        }
        return size;
    }

    @Override
    public final void accept(Object o) {
        input(o);
    }
}
