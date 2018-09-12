package spacegraph.space2d.widget.meta;

import com.jogamp.opengl.GL2;
import jcog.event.Off;
import jcog.service.Service;
import jcog.service.Services;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.grid.GridModel;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.grid.ScrollGrid;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.video.Draw;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class ServicesTable extends Gridding implements GridModel, ScrollGrid.GridRenderer {

    private final Services context;
    private Off updater;
    private ScrollGrid grid;

    
    private final List<Service> services = new CopyOnWriteArrayList();

    public ServicesTable(Services<?,?> s) {
        super();

        this.context = s;
        set(new ScrollGrid(this, this).view(0, 0, 2, 16));
    }

    @Override
    public boolean start(SurfaceBase parent) {
        if (super.start(parent)) {
            updater = context.change.on(this::update);
            update();
            return true;
        }
        return false;
    }

    @Override
    public boolean stop() {
        if (super.stop()) {
            updater.off();
            updater = null;
            return true;
        }
        return false;
    }

    @Override
    public void start(ScrollGrid x) {
        this.grid = x;
    }

    @Override
    public void stop(ScrollGrid x) {
        this.grid = null;
    }

    private void update() {
        synchronized (this) {
            services.clear();
            context.stream().collect(Collectors.toCollection(()-> services));
        }
        grid.refresh();
    }

    @Override
    public int cellsX() {
        return 2;
    }

    @Override
    public int cellsY() {
        return services.size();
    }

    @Nullable
    @Override
    public Object get(int x, int y) {
        try {
            Service s = services.get(y);
            switch (x) {
                case 0: {
                    return new Bordering(
                            new VectorLabel(s.toString())
                    ).set(Bordering.W, new ServiceToggle(context, s));
                }
                case 1: {
                    return new ObjectSurface<>(s);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    static class ServiceToggle extends Gridding {

        final Service s;

        ServiceToggle(Services n, Service s) {
            this.s = s;
            set(
                new PushButton("On").click(()->s.start(n, ForkJoinPool.commonPool())),
                new PushButton("Off").click(()->s.stop(n, ForkJoinPool.commonPool(), ()->{}))
            );
        }

        @Override
        protected void paintBelow(GL2 gl) {
            if (s.isOff()) {
                Draw.rectRGBA(bounds, 1, 0, 0, 0.5f, gl);
            } else if (s.isOn()) {
                Draw.rectRGBA(bounds, 0, 1, 0, 0.6f, gl);
            } else {
              //..
            }
        }
    }
    @Override
    public Surface apply(int x, int y, Object value) {
        return (Surface)value;
    }
}
