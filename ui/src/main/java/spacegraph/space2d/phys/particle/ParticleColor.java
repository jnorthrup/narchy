package spacegraph.space2d.phys.particle;

import spacegraph.space2d.phys.common.Color3f;
import spacegraph.util.math.Color4f;

/**
 * Small color object for each particle
 *
 * @author dmurph
 */
@Deprecated public class ParticleColor {
    public byte r;
    public byte g;
    public byte b;
    public byte a;

    public ParticleColor() {
        r = (byte) 127;
        g = (byte) 127;
        b = (byte) 127;
        a = (byte) 50;
    }


    public ParticleColor(float r, float g, float b, float a) {
        set(new Color4f(r, g, b, a));
    }

    public ParticleColor(byte r, byte g, byte b, byte a) {
        set(r, g, b, a);
    }

    public ParticleColor(Color4f color) {
        set(color);
    }
    public ParticleColor(Color3f color) {
        set(color);
    }


    private void set(Color4f color) {
        r = (byte) (127.0F * color.x);
        g = (byte) (127.0F * color.y);
        b = (byte) (127.0F * color.z);
        a = (byte) (127.0F * color.w);
    }

    private void set(Color3f color) {
        r = (byte) (127.0F * color.x);
        g = (byte) (127.0F * color.y);
        b = (byte) (127.0F * color.z);
        a = (byte) 127;
    }

    public void set(ParticleColor color) {
        r = color.r;
        g = color.g;
        b = color.b;
        a = color.a;
    }

    public boolean isZero() {
        return (int) r == 0 && (int) g == 0 && (int) b == 0 && (int) a == 0;
    }

    private void set(byte r, byte g, byte b, byte a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }
}
