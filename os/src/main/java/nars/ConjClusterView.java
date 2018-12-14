package nars;

import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat;
import nars.control.DurService;
import nars.op.stm.ConjClustering;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.widget.meter.Cluster2DView;
import spacegraph.video.Draw;

public class ConjClusterView extends Cluster2DView {

    private final ConjClustering conj;

    public ConjClusterView(ConjClustering c) {
        this.conj = c;
        DurService.on(c.nar(), () -> update(c.data.net));
    }

    @Override
    protected void paintIt(GL2 gl, SurfaceRender r) {
        conj.data.bag.forEach(l -> {
            Task t = l.get();
            RectFloat b = bounds(new double[]{t.mid(), t.priElseZero()}, 35);
            gl.glColor3f(0.5f, 0.25f, 0f);
            Draw.rect(b, gl);
        });
        super.paintIt(gl, r);
    }
}
