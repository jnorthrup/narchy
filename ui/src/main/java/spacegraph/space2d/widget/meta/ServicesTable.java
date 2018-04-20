package spacegraph.space2d.widget.meta;

import jcog.Service;
import jcog.Services;
import jcog.event.On;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.grid.GridModel;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.grid.ScrollGrid;
import spacegraph.space2d.widget.button.PushButton;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ServicesTable<S extends Service> extends Gridding implements GridModel, ScrollGrid.GridRenderer {

    private final Services<?, ?, S> context;
    private On updater;
    private ScrollGrid grid;

    //final List keys = new FasterList();
    final List<S> services = new CopyOnWriteArrayList();

    public ServicesTable(Services<?,?,S> s) {
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

    protected void update() {
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
            S s = services.get(y);
            switch (x) {
                case 0: {
                    return new PushButton(s.toString());
                }
                case 1: {
                    return new AutoSurface(s);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    @Override
    public Surface apply(int x, int y, Object value) {
        return (Surface)value;
    }
}
