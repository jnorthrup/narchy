package spacegraph.video;


import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.unit.AspectAlign;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV;

/**
 * https://www.khronos.org/opengl/wiki/Image_Format
 */
public class Tex {

    public com.jogamp.opengl.util.texture.Texture texture;


    private boolean mipmap = false;


    private final AtomicBoolean updated = new AtomicBoolean(false);
    public GLProfile profile;
    private TextureData data;

    /**
     * weird rotation correction.. dunno why yet
     */
    boolean inverted = false;

    private Object src;
//    @Deprecated private GL2 gl;

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

        commit(gl);

        Texture t = this.texture;
        if (t != null) {
            Draw.rectTex(gl, t, bounds.x, bounds.y, bounds.w, bounds.h, 0, repeatScale, alpha, mipmap, inverted);
        }

    }

    /** try to commit */
    public Tex commit(GL2 gl) {

        if (data != null) {

            if (texture == null) {
                texture = TextureIO.newTexture(gl, data);
            }
            if (updated.compareAndSet(true, false)) {
                    texture.updateImage(gl, data);

            }
        } else {
            //first step:
            if (profile == null)
                profile = gl.getGLProfile();
        }

        return this;
    }

    public static TexSurface view(BufferedImage b) {


        return new TexSurface() {
            @Override
            protected void paint(GL2 gl, ReSurface reSurface) {
                Tex t = this.tex;
                if (t !=null && t.data == null)
                    t.set(b);
                super.paint(gl, reSurface);
            }
        };
    }

    public boolean set(BufferedImage iimage) {
        if (iimage == null || profile == null)
            return false;

        if (data == null || this.src != iimage) {
            DataBuffer b = iimage.getRaster().getDataBuffer();
            int W = iimage.getWidth(), H = iimage.getHeight();
            if (b instanceof DataBufferInt)
                set(((DataBufferInt) b).getData(), W, H, iimage.getColorModel().hasAlpha());
            else if (b instanceof DataBufferByte) {
                set(((DataBufferByte) b).getData(), W, H);
            }
        }

        updated.set(true);
        return true;
    }

    private void set(byte[] iimage, int width, int height) {

        this.src = iimage;

        ByteBuffer buffer = ByteBuffer.wrap(iimage);
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

    void set(int[] iimage, int width, int height) {
        set(iimage, width, height, true);
    }

    void set(int[] iimage, int width, int height, boolean alpha) {

        this.src = iimage;
        //TODO if iimage is the same instance

        IntBuffer buffer = IntBuffer.wrap(iimage);
        data = new TextureData(profile, alpha ? GL_RGBA : GL_RGB,
                width, height,
                0 /* border */,
                GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV,
                mipmap,
                false,
                false,
                buffer, null
        );
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

        if (b == null || b.getWidth() != x.width || b.getHeight() != x.height)
            b = new BufferedImage(x.width, x.height, BufferedImage.TYPE_INT_ARGB);


        set(ConvertBufferedImage.convertTo(x, b));

        return b;


    }

    public final boolean ready() {
        return texture!=null;
    }

    public void stop(GL2 gl) {
        if (texture!=null) {
            texture.destroy(gl);
            texture = null;
        }
    }

}
