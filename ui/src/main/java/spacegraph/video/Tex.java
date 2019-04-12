package spacegraph.video;


import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import jcog.tree.rtree.rect.RectFloat;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.unit.AspectAlign;
import spacegraph.space2d.hud.Zoomed;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV;

/**
 * https://www.khronos.org/opengl/wiki/Image_Format
 */
public class Tex {

    private final AtomicBoolean updated = new AtomicBoolean(false);
    public com.jogamp.opengl.util.texture.Texture texture;
    public GLProfile profile;
    /**
     * weird rotation correction.. dunno why yet
     */
    boolean inverted = false;
    private boolean mipmap = false;
    private TextureData data;
    private Object src;
//    @Deprecated private GL2 gl;

    public static TexSurface view(BufferedImage b) {
        return new MyTexSurface(b);
    }

    public Tex mipmap(boolean mipmap) {
        this.mipmap = mipmap;
        return this;
    }

    public final void paint(GL2 gl, RectFloat bounds) {
        paint(gl, bounds, 1f);
    }

    public final void paint(GL2 gl, RectFloat bounds, float alpha) {
        paint(gl, bounds, -1, alpha);
    }

    void paint(GL2 gl, RectFloat bounds, float repeatScale, float alpha) {
        Texture t = commit(gl);
        if (t != null)
            Draw.rectTex(gl, t,
                    bounds.x, bounds.y, bounds.w, bounds.h,
                    0, repeatScale,
                    alpha, mipmap,
                    inverted);
    }

    /**
     * try to commit
     */
    @Nullable
    public Texture commit(GL2 gl) {
        if (profile == null)
            profile = gl.getGLProfile();

        if (data != null) {
            if (texture == null) {
                texture = TextureIO.newTexture(gl, data);
            }
        }
        if (texture != null && data != null) {
            if (updated.compareAndSet(true, false)) {
                texture.updateImage(gl, data);
            }
        }

        return texture;
    }

    public boolean set(BufferedImage iimage) {

        if (!ready())
            return false;

        DataBuffer b = iimage.getRaster().getDataBuffer();
//        if (b instanceof DataBufferInt)
        Object o = b instanceof DataBufferInt ? ((DataBufferInt) b).getData() : ((DataBufferByte)b).getData();

        int W = iimage.getWidth(), H = iimage.getHeight();
        _set(o, W, H, iimage.getColorModel().hasAlpha());
//        else if (b instanceof DataBufferByte) {
//            _set(((DataBufferByte) b).getData(), W, H);
//        } else
//            throw new TODO();


        updated.set(true);
        return true;
    }

//    public void set(int[] iimage, int width, int height) {
//        if (!ready())
//            return;
//
//        if (data == null) {
//            _set(iimage, width, height, true);
//        }
//
//        updated.set(true);
//    }

//    private void _set(byte[] iimage, int width, int height) {
//
//        this.src = iimage;
//
//        ByteBuffer buffer = ByteBuffer.wrap(iimage);
//
//
//    }

    private void _set(Object iimage, int width, int height, boolean alpha) {

        if (src!=iimage) {

            this.src = iimage;

            Buffer buffer = iimage instanceof int[] ? IntBuffer.wrap((int[])iimage) : ByteBuffer.wrap((byte[])iimage);
            if (this.data != null) {
                data.setBuffer(buffer);
            } else {

                if (iimage instanceof int[]) {
                    data = new TextureData(profile, alpha ? GL_RGBA : GL_RGB,
                            width, height,
                            0 /* border */,
                            GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV,
                            mipmap,
                            false,
                            false,
                            buffer, null
                    );
                } else {
                    data = new TextureData(profile, GL_RGB,
                            width, height,
                            0 /* border */,
                            GL_RGB,
                            GL_UNSIGNED_BYTE,
                            mipmap,
                            false,
                            false,
                            buffer, null
                    );
                }
            }
        }

    }

    public TexSurface view() {
        return new TexSurface(this);
    }

    public Surface view(float aspect) {
        return new AspectAlign(view(), aspect);
    }

    /**
     * less efficient than: b = update(x, b)
     */
    public BufferedImage set(GrayU8 x) {
        return set(x, null);
    }

    public BufferedImage set(GrayU8 x, BufferedImage b) {
        this.src = x;

        if (data == null) {
            if (b == null || b.getWidth() != x.width || b.getHeight() != x.height)
                b = new BufferedImage(x.width, x.height, BufferedImage.TYPE_INT_ARGB);

            set(ConvertBufferedImage.convertTo(x, b));
        }

        return b;


    }

    public final boolean ready() {
        return profile != null;
    }

    public void stop(Surface x) {
        Zoomed r = (Zoomed) x.root();
        if (r != null) {
            JoglDisplay s = r.space;
            if (s != null) {
                if (texture != null) {
                    //TODO if texure is shared, dont?
                    this.texture.destroy(s.gl());
                    this.texture = null;
                }
            }
        }
    }

    public void delete(GL2 gl) {
        texture.destroy(gl);
        profile = null;
        src = null;
    }


    @Deprecated
    private static class MyTexSurface extends TexSurface {
        private final BufferedImage b;

        MyTexSurface(BufferedImage b) {
            this.b = b;
        }

        @Override
        protected void paint(GL2 gl, ReSurface reSurface) {
            Tex t = this.tex;
            if (t != null && t.data == null)
                t.set(b);
            super.paint(gl, reSurface);
        }
    }
}
