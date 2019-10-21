package spacegraph.input.key;

import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import jcog.data.list.MetalConcurrentQueue;
import jcog.event.Off;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.map.primitive.MutableShortObjectMap;
import org.eclipse.collections.impl.map.mutable.primitive.ShortObjectHashMap;
import org.jetbrains.annotations.Nullable;
import spacegraph.video.JoglDisplay;
import spacegraph.video.JoglWindow;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Predicate;


public class SpaceKeys extends KeyAdapter implements Consumer<JoglWindow> {

    final JoglDisplay space;

    private final Queue<Consumer<SpaceKeys>> pending = new ConcurrentLinkedQueue<>();
    private final MetalConcurrentQueue<Short> queue = new MetalConcurrentQueue<>(64);


    private final MutableShortObjectMap<FloatProcedure> keyPressed = new ShortObjectHashMap<>();
    private final MutableShortObjectMap<FloatProcedure> keyReleased = new ShortObjectHashMap<>();

    public SpaceKeys(JoglDisplay g) {
        this.space = g;


        Off on = g.video.onUpdate(this);
    }

    @Override
    public void accept(JoglWindow j) {
        if (!pending.isEmpty()) {
            synchronized(this) {
                pending.removeIf(new Predicate<Consumer<SpaceKeys>>() {
                    @Override
                    public boolean test(Consumer<SpaceKeys> x) {
                        x.accept(SpaceKeys.this);
                        return true;
                    }
                });
            }
        }

        if (!queue.isEmpty()) {
            float dt = j.dtS;
            queue.clear(new Consumer<Short>() {
                @Override
                public void accept(Short k) {
                    FloatProcedure f = ((k >= 0) ? keyPressed : keyReleased).get((short) Math.abs(k));
                    if (f != null)
                        f.value(dt);
                }
            });
        }
    }

    /** add a handler */
    public void on(int keyCode, @Nullable FloatProcedure ifPressed, @Nullable FloatProcedure ifReleased) {
        pending.add(new Consumer<SpaceKeys>() {
            @Override
            public void accept(SpaceKeys k) {
                if (ifPressed != null) {
                    k.keyPressed.put((short) keyCode, ifPressed);
                }
                if (ifReleased != null) {
                    k.keyReleased.put((short) keyCode, ifReleased);
                }
            }
        });
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

    private boolean setKey(short c, boolean state) {
        if ((state ? keyPressed : keyReleased).containsKey(c)) {
            queue.push(state ? c : (short)-(int) c);
            return true;
        }
        return false;
    }
}
