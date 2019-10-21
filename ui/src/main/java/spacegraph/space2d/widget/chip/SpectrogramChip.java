package spacegraph.space2d.widget.chip;

import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.widget.meter.Spectrogram;
import spacegraph.space2d.widget.port.TypedPort;

import java.util.function.Consumer;

public class SpectrogramChip extends TypedPort<float[]> {

    private transient Spectrogram s = null;

    int history = 128;

    public SpectrogramChip() {

        super(float[].class);
        on(new Consumer<float[]>() {
            @Override
            public void accept(float[] row) {
                Spectrogram s = SpectrogramChip.this.s;
                if (s == null || s.N.intValue() != row.length) {
                    //s = new Spectrogram()
                    SpectrogramChip.this.set(s = SpectrogramChip.this.s = new Spectrogram(true, history, row.length));
                }
            }
        });
    }

    @Override
    protected void paintWidget(RectFloat bounds, GL2 gl) {
    }

}
