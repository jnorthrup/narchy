package jcog.event;

import jcog.TODO;
import jcog.data.bit.MetalBitSet;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** topic whose transmissions are keyed by a 'byte' selector.  receivers can register for one or more of the channels */
public class ByteTopic<X> {

    /** TODO a fixed # of topic id's. it will work when it needs just a few bits after mapping.
     * if there is a fixed mapping like in the task punctuation case
    public static class ByteDenseTopic<X> {

    }*/

    /** last channel is reserved for general catch 'all' sent once in all cases */
    private final static byte ANY = Byte.MAX_VALUE-1;

    private final boolean allowDynamic = false;

    private final Topic<X>[] chan = new Topic[Byte.MAX_VALUE /* signed max */];

    /** TODO write atomic variant of LongMetalBitset */
    private final MetalBitSet active = MetalBitSet.bits(255);//new AtomicMetalBitSet();

    public ByteTopic(byte... preDefined) {
        validate(false, preDefined);
        for (byte c : preDefined)
            chan[c] = newTopic(c);

        assert(chan[ANY] == null);
        chan[ANY] = newTopic(ANY);

    }

    @Override
    public String toString() {
        return Stream.of(chan).filter(Objects::nonNull).map(Object::toString).collect(Collectors.joining(","));
    }

    protected Topic<X> newTopic(byte c) {
        return new ByteSubTopic<>(c);
    }

//TODO
//    public final Off on(Consumer<X> each, boolean strong, byte... channelsRegistered) {
//    }

    /** if 0-length array of channels is provided, this means to register for the 'ANY' channel which
     * receives everything
     */
    public final Off on(Consumer<X> each, byte... channelsRegistered) {
        if (channelsRegistered.length == 0)
            return _on(each, ANY, true);
        else {
            validate(true, channelsRegistered);

            if (allowDynamic) {
                throw new TODO();//TODO synch-free version of this using atomic ops
            } else {
                RunThese o = new RunThese(channelsRegistered.length);
                for (byte c : channelsRegistered)
                    o.add(_on(each, c, true));
                return o;
            }
        }
    }

    private Off _on(Consumer<X> each, byte c, boolean strong) {
        active.set(c, true);
        return chan[c].on(each, strong);
    }

    public final void emit(X x, byte chan) {
        _emit(x, chan);
        _emit(x, ANY);
    }

    public final void emit(X x, byte... chans) {
        for (byte c : chans)
            _emit(x, c);
        _emit(x, ANY);
    }

    private void _emit(X x, byte c) {
        if (active.get(c))
            chan[c].emit(x);
    }

    private void validate(boolean afterConstruction, byte[] chans) {
        if (chans.length == 0)
            throw new UnsupportedOperationException();
        for (byte c : chans) {
            if (c < 0 || c >= ANY)
                throw new ArrayIndexOutOfBoundsException();
            if (afterConstruction && (!allowDynamic && chan[c] == null))
                throw new NullPointerException();
        }
    }


    private final class ByteSubTopic<X> extends ListTopic<X> {
        private final byte c;

        ByteSubTopic(byte c) {
            this.c = c;
        }

        @Override
        public void start(Consumer<X> o) {
            //synchronized (this) {
                active.set(c, true);
                super.start(o);
            //}
        }

        @Override
        public void stop(Consumer<X> o) {
            //synchronized (this) {
                super.stop(o);
                if (isEmpty())
                    active.set(c, false);
            //}
        }
    }
}
