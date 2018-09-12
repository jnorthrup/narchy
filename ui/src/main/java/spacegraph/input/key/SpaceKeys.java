package spacegraph.input.key;

import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import jcog.event.Off;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;
import spacegraph.video.JoglSpace;
import spacegraph.video.JoglWindow;

import java.util.function.Consumer;


abstract class SpaceKeys extends KeyAdapter implements Consumer<JoglWindow> {

    final JoglSpace space;

    
    private RoaringBitmap queue = new RoaringBitmap();

    private final IntObjectHashMap<FloatProcedure> _keyPressed = new IntObjectHashMap<>();
    private final MutableIntObjectMap<FloatProcedure> keyPressed = _keyPressed.asSynchronized();
    private final IntObjectHashMap<FloatProcedure> _keyReleased = new IntObjectHashMap();
    private final MutableIntObjectMap<FloatProcedure> keyReleased = _keyReleased.asSynchronized();
    private final Off on;

    SpaceKeys(JoglSpace g) {
        this.space = g;


        on = g.onUpdate(this);
    }

    @Override
    public void accept(JoglWindow j) {

        RoaringBitmap queue = this.queue;
        if (!queue.isEmpty()) {
            float dt = j.dtS;
            synchronized (on) {
                this.queue = new RoaringBitmap();
            }
            queue.forEach((int k) -> {
                boolean s = k >= 0; 
                FloatProcedure f = ((s) ? keyPressed : keyReleased).get(Math.abs(k));
                if (f != null)
                    f.value(dt);
            });
        }
    }

    void watch(int keyCode, @Nullable FloatProcedure ifPressed, @Nullable FloatProcedure ifReleased) {
        if (ifPressed != null) {
            keyPressed.put(keyCode, ifPressed);
        }
        if (ifReleased != null) {
            keyReleased.put(keyCode, ifReleased);
        }
    }

    

    @Override
    public void keyReleased(KeyEvent e) {
        setKey(e, false);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        setKey(e, true);
    }

    private void setKey(KeyEvent e, boolean pressOrRelease) {
        if (e.isConsumed())
            return;

        if (setKey(e.getKeyCode(), pressOrRelease)) {
            e.setConsumed(true);
        }
    }

    private boolean setKey(int c, boolean state) {
        if ((state ? keyPressed : keyReleased).containsKey(c)) {
            synchronized (on) {
                queue.add(state ? c : -c);
                return true;
            }
        }
        return false;
    }
}
