package spacegraph.space2d.hud;

import jcog.tree.rtree.rect.RectFloat;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.Fingering;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Stacking;
import spacegraph.video.OrthoSurfaceGraph;

import java.util.function.Function;

public class Hover<X extends Surface,Y extends Surface> extends Fingering {








    //TODO delayNS

    final X source;
    volatile Surface target = null;
    private final Function<X, Y> targetBuilder;

    private RectFloat tgtBoundsPx;
    private long startTime;

    /** computes display position, in screen (pixel) coordinates */
    //final BiFunction<X,Finger,RectFloat> positioner;
    final HoverModel model;

    public Hover(X source, Function<X, Y> target, HoverModel model) {
        this.source = source;
        this.targetBuilder = target;
        this.model = model;
        this.target = null;

    }

    @Override
    protected boolean start(Finger f) {
        //TODO reset delay time

        startTime = System.nanoTime();
        if (update(f)) {
            return show();
        }
        return false;
    }

    @Override
    public boolean update(Finger f) {
        //update
        if (f.focused() && f.touching()==source) {

            float hoverTimeS = (float)((System.nanoTime() - startTime)/1.0E9);
            model.set(source, f);
            tgtBoundsPx = model.pos(hoverTimeS);

            Surface t = this.target;
            if (t != null) {
                if (tgtBoundsPx != null) {
                    updatePos(t);
                } else
                    t.hide();
            }

            return (tgtBoundsPx !=null);
        }
        return false;
    }

    public RectFloat sourceBounds(Finger f) {
        return f.globalToPixel(source.bounds);
    }


    protected boolean show() {
        Stacking root = ((OrthoSurfaceGraph) source.root()).layers;
        //synchronized (targetBuilder) {
        Surface t = target = targetBuilder.apply(source);
        //}
        if (t!=null) {
            t.hide();
            root.add(t);
            //updatePos();
            return true;
        }
        return false;
    }

    private void updatePos(Surface t) {
        //Exe.invokeLater(()->{
        t.pos(tgtBoundsPx); //HACK TODO dont allow root Stacking to maximize pos that this resets
        t.show();
        //});
    }

    @Override
    public boolean defer(Finger finger) {
        return true;
    }


    protected void hide() {
        //synchronized (targetBuilder) {
            if (target!=null) {
                target.delete();
                target = null;
            }
        //}
    }

    @Override
    public void stop(Finger finger) {
        hide();
        super.stop(finger);
    }
}
