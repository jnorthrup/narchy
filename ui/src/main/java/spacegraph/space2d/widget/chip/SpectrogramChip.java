package spacegraph.space2d.widget.chip;

import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.widget.meter.Spectrogram;
import spacegraph.space2d.widget.port.TypedPort;
import spacegraph.space2d.widget.port.Wire;
import spacegraph.video.Draw;

public class SpectrogramChip extends TypedPort<float[]> {

    transient private Spectrogram s = null;

    int history = 128;

    public SpectrogramChip() {
        super(float[].class);
    }

    @Override
    protected void paintWidget(RectFloat bounds, GL2 gl) {
        //super.paintWidget(bounds, gl);
    }

    @Override
    public boolean recv(Wire from, float[] row) {
        if (super.recv(from, row)) {
            Spectrogram s = this.s;
            if (s == null || s.N.intValue()!=row.length) {
                //s = new Spectrogram()
                set(s = this.s = new Spectrogram(true, history, row.length));
            }

            s.next((c)-> Draw.rgbInt(0, row[c], 0));

            return true;
        }
        return false;
    }
}
