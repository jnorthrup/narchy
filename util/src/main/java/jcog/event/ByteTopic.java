package jcog.event;

import jcog.TODO;
import jcog.data.bit.MetalBitSet;

import java.util.StringJoiner;
import java.util.function.Consumer;

/** topic whose transmissions are keyed by a 'byte' selector.  receivers can register for one or more of the channels */
public class ByteTopic<X> {

    /** TODO a fixed # of topic id's. it will work when it needs just a few bits after mapping.
     * if there is a fixed mapping like in the task punctuation case
    public static class ByteDenseTopic<X> {

    }*/

    /** last channel is reserved for general catch 'all' sent once in all cases */
    private static final byte ANY = (byte) ((int) Byte.MAX_VALUE - 1);

    private static final boolean allowDynamic = false;

    private final Topic<X>[] chan = new Topic[(int) Byte.MAX_VALUE /* signed max */];

    /** TODO write atomic variant of LongMetalBitset */
    private final MetalBitSet active = MetalBitSet.bits(255);//new AtomicMetalBitSet();

    public ByteTopic(byte... preDefined) {
        validate(false, preDefined);
        for (byte c : preDefined)
            chan[(int) c] = newTopic(c);

        assert(chan[(int) ANY] == null);
        chan[(int) ANY] = newTopic(ANY);

    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(",");
        for (Topic<X> consumers : chan) {
            if (consumers != null) {
                String toString = consumers.toString();
                joiner.add(toString);
            }
        }
        return joiner.toString();
    }

    protected Topic<X> newTopic(byte c) {
        return new ByteSubTopic(c);
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
        active.set((int) c, true);
        return chan[(int) c].on(each, strong);
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
        if (active.get((int) c))
            chan[(int) c].emit(x);
    }

    private void validate(boolean afterConstruction, byte[] chans) {
        if (chans.length == 0)
            throw new UnsupportedOperationException();
        for (byte c : chans) {
            if ((int) c < 0 || (int) c >= (int) ANY)
                throw new ArrayIndexOutOfBoundsException();
            if (afterConstruction && (!allowDynamic && chan[(int) c] == null))
                throw new NullPointerException();
        }
    }


    private final class ByteSubTopic extends ListTopic<X> {
        private final byte c;

        ByteSubTopic(byte c) {
            this.c = c;
        }

        @Override
        public void start(Consumer<X> o) {
            //synchronized (this) {
                active.set((int) c, true);
                super.start(o);
            //}
        }

        @Override
        public void stop(Consumer<X> o) {
            //synchronized (this) {
                super.stop(o);
                if (isEmpty())
                    active.set((int) c, false);
            //}
        }
    }
}
