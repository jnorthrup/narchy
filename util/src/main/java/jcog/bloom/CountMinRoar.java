package jcog.bloom;

import org.eclipse.collections.api.block.predicate.primitive.IntIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ShortIntPredicate;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

/** fusion of count-min-sketch with roaring bitmap for fast
 *  iteration of the measured values
 *  TODO not finished
 */
public class CountMinRoar extends CountMinSketch {
    final RoaringBitmap set = new RoaringBitmap();

    public void add(byte x) {
        set.add(x); super.add(x);
    }

    public void add(int x) {
        set.add(x); super.add(x);
    }

    public void add(short x) {
        set.add(x); super.add(x);
    }

    public void whileEachInt(IntIntPredicate each) {
        PeekableIntIterator s = set.getIntIterator();
        int next;
        while (s.hasNext() && each.accept(next = s.next(), count(next)));
    }
    public void whileEachShort(ShortIntPredicate each) {
        PeekableIntIterator s = set.getIntIterator();
        short next;
        while (s.hasNext() && each.accept(next = (short) s.next(), count(next)));
    }

    int uniques() {
        return set.getCardinality();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(uniques() * 6 /* estimate */);
        whileEachInt((i, c) -> {
            sb.append(i).append('x').append(c).append(',');
            return true;
        });
        return sb.substring(0, sb.length()-1);
    }

    public String summary() {
        return set.getSizeInBytes() + w *this.depth()*4 + " bytes"; /* int 32bits */
    }
}
