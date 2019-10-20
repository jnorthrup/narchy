package jcog.math;

import jcog.data.list.FasterList;
import jcog.util.ArrayUtil;

import java.util.Random;

/**
 * Created by me on 11/1/15.
 */
public class ShuffledPermutations extends Permutations {

    /** starting index of the current shuffle */
    byte[] shuffle;

    /** you probably want to supply your own RNG and use the other restart method */
    @Override public ShuffledPermutations restart(int size) {
        return restart(size, new Random());
    }

    public ShuffledPermutations restart(int size, Random random) {
        assert(size < 127);

        super.restart(size);


        var shuffle = this.shuffle;
        if (shuffle == null || shuffle.length < size)
            this.shuffle = shuffle = new byte[size];

        for (var i = 0; i < size; i++)
            shuffle[i] = (byte)i;
        ArrayUtil.shuffle(shuffle, size, random);


        return this;
    }

    @Override
    public final int permute(int index) {
        return ind[shuffle[index]];
    }

    int[] nextPermute(int[] target) {
        next();
        var l = size;
        for (var i = 0; i < l; i++)
            target[i] = permute(i);
        return target;
    }


    /** TODO improve the buffering characteristics of this
     *  TODO make re-shuffling optional on each new iteration, currently this will shuffle once and it will get repeated if re-iterated
     * */
    public static <X> Iterable<X> shuffle(Iterable<X> i, Random rng) {
        var f = new FasterList<X>(i);
        if (f.size() <= 1)
            return i; //unchanged
        else {
            f.trimToSize();
            f.shuffleThis(rng);
            return f;
        }

    }
}
