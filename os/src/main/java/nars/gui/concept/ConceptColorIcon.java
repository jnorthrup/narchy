package nars.gui.concept;

import com.jogamp.opengl.GL2;
import nars.NAR;
import nars.concept.Concept;
import nars.term.Termed;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.phys.common.Color3f;
import spacegraph.space2d.widget.text.BitmapLabel;
import spacegraph.video.Draw;

import java.util.function.BiConsumer;

public class ConceptColorIcon extends ConceptView {

    final Color3f color = new Color3f(0.5f, 0.5f, 0.5f);
    private final BiConsumer<Concept, Color3f> colorize;

    /** @colorize should accept null concept */
    public ConceptColorIcon(Termed t, NAR n, BiConsumer<Concept,Color3f> colorize) {
        super(t, n);

        this.colorize = colorize;

        set(new BitmapLabel(t.term().toString()));
    }

    @Override
    protected void paintIt(GL2 gl, ReSurface rd) {
        gl.glColor3f(color.x, color.y, color.z);
        Draw.rect(bounds, gl);
    }

    @Override
    protected void update() {
        Concept c = concept();
        colorize.accept(c, color);
    }
}
