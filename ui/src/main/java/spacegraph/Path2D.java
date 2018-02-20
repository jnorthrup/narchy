package spacegraph;

import com.jogamp.opengl.GL2;
import jcog.Util;
import org.eclipse.collections.api.block.procedure.primitive.FloatFloatProcedure;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import spacegraph.math.Tuple2f;
import spacegraph.math.v2;

import java.util.concurrent.ThreadLocalRandom;

/**
 * pairs of x,y coordiates stored in linear array
 * <p>
 * useful for iterative trajectory construction and compact storage
 */
public class Path2D extends FloatArrayList {

    public Path2D(int capacity) {
        super(capacity);
    }

    public void add(float x, float y) {
        addAll(x, y);
    }

    public void add(Tuple2f p) {
        addAll(p.x, p.y);
    }

    //TODO addIfFurtherThan(float x, float y, float minDistance)

    /**
     * adds the point if the total size is below the maxPoints limit,
     * otherwise it simplifies the current set of points and sets the
     * end point to the specified value
     */
    public synchronized void add(Tuple2f p, int maxPoints) {
        assert (maxPoints > 3);
        if (points() < maxPoints) {
            add(p);
        } else {
            //TODO path simplification
            //make subclass which stores triples of values:
            //x,y,[distance to next vertex]
            //total distance can be stored in the final unused triple
            //the distance value will help determine what points to interpolate during simplification

            //HACK remove point at random
            int toRemove = 1 + ThreadLocalRandom.current().nextInt(maxPoints - 2);
            float rx = removeAtIndex(toRemove * 2);
            float ry = removeAtIndex(toRemove * 2); //called twice at the same index


            //interpolate 33% with after
            items[toRemove * 2 + 0] = Util.lerp(items[toRemove * 2 + 0], rx, 0.33f);
            items[toRemove * 2 + 1] = Util.lerp(items[toRemove * 2 + 1], ry, 0.33f);


            if (toRemove > 1) {
                //interpolate 33% with prev
                items[toRemove * 2 - 2] = Util.lerp(items[toRemove * 2 - 2], rx, 0.33f);
                items[toRemove * 2 - 1] = Util.lerp(items[toRemove * 2 - 1], ry, 0.33f);
            }

            add(p);
        }
    }

    public void setEnd(Tuple2f p) {
        float[] ii = items;
        int s = size;
        ii[s - 2] = p.x;
        ii[s - 1] = p.y;
    }

    public void addAll(Tuple2f... pp) {
        ensureCapacity(size + pp.length * 2);
        for (Tuple2f p : pp)
            addAll(p.x, p.y);
    }


    public v2 start() {
        assert (size > 0);
        float[] ii = items;
        return new v2(ii[0], ii[1]);
    }

    public v2 end() {
        int s = size;
        assert (s > 0);
        float[] ii = items;
        return new v2(ii[s - 2], ii[s - 1]);
    }


    public void forEach(FloatFloatProcedure each) {
        int s = size;
        for (int i = 0; i < s; ) {
            each.value(items[i++], items[i++]);
        }
    }

    public void vertex2f(GL2 gl) {
        forEach(gl::glVertex2f);
    }

//    @Override
//    public int size() {
//        throw new UnsupportedOperationException();
//    }

    public int points() {
        return size / 2;
    }
}
