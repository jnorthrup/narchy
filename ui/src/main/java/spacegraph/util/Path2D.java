package spacegraph.util;

import com.jogamp.opengl.GL2;
import org.eclipse.collections.api.block.procedure.primitive.FloatFloatProcedure;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;

import static spacegraph.util.math.Simplify2D.collinear;
import static spacegraph.util.math.Simplify2D.collinearity;


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

    private void add(Tuple2f p) {
        addAll(p.x, p.y);
    }

    

    /**
     * adds the point if the total size is below the maxPoints limit,
     * otherwise it simplifies the current set of points and sets the
     * end point to the specified value
     */
    public synchronized void add(Tuple2f p, int maxPoints) {
        assert (maxPoints > 3);



            add(p);
            if (points() > maxPoints)
                collinearSimplifyNext();
            

























    }

    private void collinearSimplifyNext() {
        int n = points();
        int worst = -1;
        float minC = Float.POSITIVE_INFINITY;
        for (int i = 1; i < n-1; i++) {
            int prevId = i - 1;
            if (prevId < 0) prevId = n - 1;
            int nextId = i + 1;
            if (nextId >= n) nextId = 0;

            {

                v2 prev = point(prevId);
                v2 current = point(i);
                v2 next = point(nextId);

                float c = collinearity(prev, current, next);
                if (c < minC) {
                    worst = i;
                    minC = c;
                }
            }
        }
        assert(worst!=-1);
        removeAtIndex(worst*2); removeAtIndex(worst*2);
    }

    void collinearSimplify(float collinearityTolerance, int maxPoints) {
        
        assert(maxPoints >= 3);

        int n = points();
        for (int i = 0; n > maxPoints && i < n; ) {
            int prevId = i - 1;
            if (prevId < 0) prevId = n - 1;
            int nextId = i + 1;
            if (nextId >= n) nextId = 0;

            v2 prev = point(prevId);
            v2 current = point(i);
            v2 next = point(nextId);

            if (i > 0 && i < n-1 && collinear(prev, current, next, collinearityTolerance)) {
                removeAtIndex(i*2); removeAtIndex(i*2);
                n--;
            } else {
                i++;
            }
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
        
        return point(0);
    }

    private v2 point(int i) {
        float[] ii = items;
        return new v2(ii[i*2], ii[i*2+1]);
    }

    public v2 end() {
        int s = size;
        assert (s > 0);
        float[] ii = items;
        return new v2(ii[s - 2], ii[s - 1]);
    }


    private void forEach(FloatFloatProcedure each) {
        int s = size;
        for (int i = 0; i < s; ) {
            each.value(items[i++], items[i++]);
        }
    }

    public void vertex2f(GL2 gl) {
        forEach(gl::glVertex2f);
    }






    public int points() {
        return size / 2;
    }
}
