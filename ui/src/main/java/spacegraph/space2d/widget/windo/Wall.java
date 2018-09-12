package spacegraph.space2d.widget.windo;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Animating;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.grid.MutableMapContainer;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.text.BitmapLabel;

import java.util.Set;

/**
 * a wall (virtual surface) contains zero or more windows;
 * anchor region for Windo's to populate
 * <p>
 * TODO move active window to top of child stack
 */
public class Wall<S extends Surface> extends MutableMapContainer<S, Windo> {

    public Wall() {
        super();
        clipBounds = false;
    }

    public final Windo add(S x) {
        Windo w = computeIfAbsent(x, (xx) -> {
            Windo ww = new Windo(new MetaFrame(xx));
            ww.start(this);
            return ww;
        }).value;
        return w;
    }

    @Override
    public Windo remove(S key) {
        Windo w = super.remove(key);
        if (w!=null) {
            w.stop();
            return w;
        }
        return null;
    }

    public final Windo add(S x, float w, float h) {
        Windo y = add(x);
        y.size(w, h);
        return y;
    }

    public Set<S> children() {
        return cells.map.keySet();
    }

    @Override
    public void doLayout(int dtMS) {
        forEachValue(w -> {
            //w.fence(bounds);
            w.layout();
        });
    }
    public @Nullable Windo get(S t) {
        return getValue(t);
    }


    public Animating<Debugger> debugger() {
        Debugger d = new Debugger();
        return new Animating<>(d, d::update, 0.25f);
    }

    class Debugger extends Gridding {

        private final BitmapLabel boundsInfo, children;

        {
            add(boundsInfo = new BitmapLabel());
            add(children = new BitmapLabel());

        }

        void update() {
            boundsInfo.text(Wall.this.bounds.toString());

            children.text(Joiner.on("\n").join(Iterables.transform(
                    Wall.this.keySet(), t -> info(t, Wall.this.get(t)))));
        }

        protected String info(Surface x, Windo w) {
            return x + "\n  " + (w != null ? w.bounds : "?");
        }

    }

}




































































































