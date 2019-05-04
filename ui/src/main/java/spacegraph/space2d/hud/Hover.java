package spacegraph.space2d.hud;

import jcog.tree.rtree.rect.RectFloat;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.Fingering;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Stacking;
import spacegraph.video.OrthoSurfaceGraph;

import java.util.function.Function;

public class Hover<X extends Surface, Y extends Surface> extends Fingering {

    public final HoverModel model;
    final X source;
    private final Function<X, Y> targetBuilder;
    @Nullable
    volatile Surface target = null;
    private RectFloat tgtBoundsPx;
    private long startTime;

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
        boolean focused = f.focused();
        if (focused && source.showing() && f.touching() == source) {

            float hoverTimeS = (float) ((System.nanoTime() - startTime) / 1.0E9);
            model.set(source, f, hoverTimeS);
            tgtBoundsPx = model.pos();

            Surface t = this.target;
            if (t != null) {
                if (tgtBoundsPx != null) {
                    updatePos(t);
                } else
                    t.hide();
            }

            return (tgtBoundsPx != null);
        } else {
            hide();
            return false;
        }
    }

    public RectFloat sourceBounds(Finger f) {
        return f.globalToPixel(source.bounds);
    }


    protected boolean show() {
        Stacking root = ((OrthoSurfaceGraph) source.root()).layers;

        Surface t = target = targetBuilder.apply(source);

        if (t != null) {
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

        model.set(null, null, 0);
        if (target != null) {
            target.delete();
            target = null;
        }

    }

    @Override
    public void stop(Finger finger) {
        hide();
        super.stop(finger);
    }
}
