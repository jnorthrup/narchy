package spacegraph.space2d.widget.chip;

import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.widget.meter.Spectrogram;
import spacegraph.space2d.widget.port.TypedPort;

public class SpectrogramChip extends TypedPort<float[]> {

    transient private Spectrogram s = null;

    int history = 128;

    public SpectrogramChip() {

        super(float[].class);
        on((float[] row)->{
            Spectrogram s = this.s;
            if (s == null || s.N.intValue()!=row.length) {
                //s = new Spectrogram()
                set(s = this.s = new Spectrogram(true, history, row.length));
            }
        });
    }

    @Override
    protected void paintWidget(RectFloat bounds, GL2 gl) {
    }

}
