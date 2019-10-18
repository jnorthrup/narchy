package spacegraph.space2d.widget.chip;

import jcog.signal.buffer.CircularFloatBuffer;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import spacegraph.space2d.widget.meter.WaveBitmap;
import spacegraph.space2d.widget.port.TypedPort;

public class WaveViewChip extends TypedPort<ObjectIntPair<float[]>> {

    final CircularFloatBuffer buffer = new CircularFloatBuffer(44100 * 2);
    final WaveBitmap wave = new WaveBitmap(buffer, 600, 400);

    public WaveViewChip() {
        super(ObjectIntPair.class);

        on(nextBuffer ->{
            float[] b = nextBuffer.getOne();
            buffer.freeHead(b.length);
            buffer.write(b);
            wave.update();
        });
        int c = buffer.capacity();
        for (int i = 0; i < c; i++) //HACK TODO use bulk array fill method
            buffer.write(new float[] { 0 });


        set(wave);
    }

}
