package spacegraph.input;

import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import jcog.event.On;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;
import spacegraph.SpaceGraph;
import spacegraph.render.JoglPhysics;

import java.util.function.Consumer;

/**
 * Created by me on 11/20/16.
 */
public abstract class SpaceKeys extends KeyAdapter implements Consumer<SpaceGraph> {

    public final JoglPhysics space;

    //TODO merge these into one Map
    RoaringBitmap queue = new RoaringBitmap();

    final IntObjectHashMap<FloatProcedure> _keyPressed = new IntObjectHashMap<>();
    final MutableIntObjectMap<FloatProcedure> keyPressed = _keyPressed.asSynchronized();
    final IntObjectHashMap<FloatProcedure> _keyReleased = new IntObjectHashMap();
    final MutableIntObjectMap<FloatProcedure> keyReleased = _keyReleased.asSynchronized();
    private final On on;

    protected SpaceKeys(SpaceGraph g) {
        this.space = g;


        on = g.onUpdate.on(this);
    }

    @Override
    public void accept(SpaceGraph j) {
        float dt = j.getLastFrameTime();

        RoaringBitmap queue = this.queue;
        if (!queue.isEmpty()) {
            synchronized (on) {
                this.queue = new RoaringBitmap();
            }
            queue.forEach((int k) -> {
                boolean s = k >= 0; //shouldnt ever be zero actually
                FloatProcedure f = ((s) ? keyPressed : keyReleased).get(Math.abs(k));
                if (f != null)
                    f.value(dt);
            });
        }
    }

    protected void watch(int keyCode, @Nullable FloatProcedure ifPressed, @Nullable FloatProcedure ifReleased) {
        if (ifPressed != null) {
            keyPressed.put(keyCode, ifPressed);
        }
        if (ifReleased != null) {
            keyReleased.put(keyCode, ifReleased);
        }
    }

    //TODO unwatch

    @Override
    public void keyReleased(KeyEvent e) {
        setKey((int) e.getKeyCode(), false);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        setKey((int) e.getKeyCode(), true);
    }

    protected void setKey(int c, boolean state) {
        if ((state ? keyPressed : keyReleased).containsKey(c)) {
            synchronized (on) {
                queue.add(state ? c : -c);
            }
        }
    }
}
