package spacegraph.space2d.widget.meta;

import com.jogamp.opengl.GL2;
import jcog.event.Off;
import jcog.service.Service;
import jcog.service.Services;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.ScrollXY;
import spacegraph.space2d.container.grid.DynGrid;
import spacegraph.space2d.container.grid.GridModel;
import spacegraph.space2d.container.grid.GridRenderer;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.video.Draw;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class ServicesTable extends Gridding implements GridModel, GridRenderer {

    private final Services context;
    private Off updater;
    private DynGrid grid;

    
    private final List<Service> services = new CopyOnWriteArrayList<>();

    public ServicesTable(Services<?,?> s) {
        super();

        this.context = s;
        set(new ScrollXY<>(new DynGrid<>(this, this)).view(0, 0, 2, 16));
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
    public void start(DynGrid x) {
        this.grid = x;
    }

    @Override
    public void stop(DynGrid x) {
        this.grid = null;
    }

    private void update() {
        synchronized (this) {
            services.clear();
            context.stream().collect(Collectors.toCollection(()-> services));
        }
//        grid.refresh();
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
        protected void paintIt(GL2 gl, SurfaceRender r) {
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
