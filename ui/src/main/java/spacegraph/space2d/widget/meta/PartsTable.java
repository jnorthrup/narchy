package spacegraph.space2d.widget.meta;

import com.jogamp.opengl.GL2;
import jcog.event.Off;
import jcog.service.Part;
import jcog.service.Parts;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.ScrollXY;
import spacegraph.space2d.container.grid.DynGrid;
import spacegraph.space2d.container.grid.GridModel;
import spacegraph.space2d.container.grid.GridRenderer;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.IconToggleButton;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.video.Draw;
import spacegraph.video.ImageTexture;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static spacegraph.SpaceGraph.window;

public class PartsTable extends Gridding implements GridModel, GridRenderer {

    private final Parts context;
    private final List<Map.Entry<?,Part>> partEntries = new CopyOnWriteArrayList<>();
    private Off updater;

    public PartsTable(Parts<?, ?> s) {
        super();

        this.context = s;
        set(new ScrollXY<>(new DynGrid<>(this, this)).scroll(0, 0, 2, 16));
    }

    @Override
    protected void starting() {
        super.starting();
        updater = context.eventAddRemove.on(this::update);
        update();
    }

    @Override
    protected void stopping() {
        updater.pause();
        updater = null;
        super.stopping();
    }

    private void update() {
        synchronized (this) {
            partEntries.clear();
            partEntries.clear();
            partEntries.addAll(context.partEntrySet());
        }
//        grid.refresh();
    }

    @Override
    public int cellsX() {
        return 2;
    }

    @Override
    public int cellsY() {
        return partEntries.size();
    }

    @Nullable
    @Override
    public Object get(int x, int y) {
        try {
            Map.Entry<?, Part> pe = partEntries.get(y);
            Object k = pe.getKey();
            Part p = pe.getValue();
            switch (x) {
                case 0: {
                    return new Bordering(
                            new PushButton(k.toString()).clicked(() -> window(p, 500, 500))
                    ).set(Bordering.W, new PartToggle(context, pe));
                }
                case 1: {
                    return new ObjectSurface<>(p);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage()); //TODO
        }
        return null;
    }

    @Override
    public Surface apply(int x, int y, Object value) {
        return (Surface) value;
    }

    static class PartToggle extends IconToggleButton {

        final Part part;
        private final Object key;

        PartToggle(Parts parts, Map.Entry<?,Part> p) {
            super(ImageTexture.awesome("gears"));
            this.key = p.getKey();
            this.part = p.getValue();
            on(part.isOn());
            on(state -> {
                //Exe.invokeLater(()->{
                    synchronized(part) {
                        if (state)
                            parts.start(key);
                        else
                            parts.stop(key);
                    }
                //});
            });
        }

        @Override
        protected void renderChildren(ReSurface r) {
            on(part.isOn()); //live update
            super.renderChildren(r);
        }

        @Override
        protected void paintIt(GL2 gl, ReSurface r) {
            if (part.isOff()) {
                Draw.rectRGBA(bounds, 1, 0, 0, 0.5f, gl);
            } else if (part.isOn()) {
                Draw.rectRGBA(bounds, 0, 1, 0, 0.6f, gl);
            } else {
                //..
            }
        }
    }
}
