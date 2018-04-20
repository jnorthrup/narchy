package nars;

import jcog.Service;
import jcog.Services;
import jcog.event.On;
import jcog.exe.Loop;
import nars.gui.Vis;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.grid.GridModel;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.grid.ScrollGrid;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.AutoSurface;
import spacegraph.space2d.widget.meta.OmniBox;
import spacegraph.space2d.widget.windo.Dyn2DSurface;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * main UI entry point
 */
public class GUI {

    static final Logger logger = LoggerFactory.getLogger(GUI.class);


    public static class ServicesWidget<S extends Service> extends Gridding implements GridModel, ScrollGrid.GridRenderer {

        private final Services<?, ?, S> context;
        private On updater;
        private ScrollGrid grid;

        //final List keys = new FasterList();
        final List<S> services = new CopyOnWriteArrayList();

        public ServicesWidget(Services<?,?,S> s) {
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

    public static void main(String[] args) {
        Dyn2DSurface w = SpaceGraph.wall(800, 600);

        NAR nar = NARchy.ui();

        Loop loop = nar.startFPS(10f); //10hz alpha
        //((NARLoop) loop).throttle.set(0.1f);


        //1. try to open a Spacegraph openGL window
        logger.info("start SpaceGraph UI");

        //            window(new ConsoleTerminal(new TextUI(nar).session(8f)) {
        //                {
        //                    Util.pause(50); term.addInput(KeyStroke.fromString("<pageup>")); //HACK trigger redraw
        //                }
        //            }, 800, 600);




        Loop.invokeLater(()->{
            //((ZoomOrtho) w.root()).scaleMin = 100f;
            //((ZoomOrtho) w.root()).scaleMax = 1500;

            w.put(new ServicesWidget(nar.services), 5,4);
            w.put(new Gridding(new OmniBox()), 6, 1);
            w.put(Vis.top(nar), 4,4);
        });

        //nar.inputNarsese(new FileInputStream("/home/me/d/sumo_merge.nal"));


    }

}
