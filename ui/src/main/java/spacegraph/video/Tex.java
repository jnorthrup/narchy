package spacegraph.video;


import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import jcog.TODO;
import jcog.tree.rtree.rect.RectFloat;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.unit.AspectAlign;

import java.awt.image.*;
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
    @Deprecated public GL2 gl;
    /**
     * weird rotation correction.. dunno why yet
     */
    boolean inverted = false;
    private boolean mipmap = false;
    private TextureData data;
    private volatile Object src;
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
        var t = commit(gl);
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
    public @Nullable Texture commit(GL2 gl) {

        ready(gl);

        var data = this.data;
        if (data != null) {
            var texture = this.texture;
            if (texture == null) {
                texture = this.texture = TextureIO.newTexture(gl, data);
            }
            if (texture != null) {
                if (updated.compareAndSet(true, false)) {
                    texture.updateImage(gl, data);
                }
            }
        }

        return texture;
    }

    private void ready(GL2 gl) {
        this.gl = gl;
    }

    public boolean set(BufferedImage i, GL2 gl) {
       ready(gl);
       if (set(i)) {
           commit(gl);
           return true;
       }
       return false;
    }

    public boolean set(BufferedImage i) {

        var gl = this.gl;
        if (gl!=null) {
            var x = i.getRaster().getDataBuffer();

            Object y = x instanceof DataBufferInt ?
                ((DataBufferInt) x).getData() :
                ((DataBufferByte)x).getData();

            var data = this.data;
            if (src != y || (data!=null && (data.getWidth()!=i.getWidth() || data.getHeight()!=i.getHeight()) )) {
                _set(y, i.getWidth(), i.getHeight(), i.getColorModel(), gl);
                this.src = y;
            }
        }

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

    private synchronized void _set(Object x, int width, int height, ColorModel color, GL2 gl) {




        Buffer buffer = x instanceof int[] ? IntBuffer.wrap((int[]) x) : ByteBuffer.wrap((byte[]) x);
            /*if (this.data != null) {
                data.setWidth(width);
                data.setHeight(height);
                data.setBuffer(buffer);
            } else */{

            var profile = gl.getGLProfile();
                if (color.getNumColorComponents()==1) {
                    //grayscale

                    if (x instanceof byte[]) {
                        data = new TextureData(profile, GL_LUMINANCE,
                                width, height,
                                0 /* border */,
                                GL_LUMINANCE, GL_UNSIGNED_BYTE,
                                mipmap,
                                false,
                                false,
                                buffer, null
                        );
                    } else {
                        throw new TODO();
                    }
                } else {

                    //assume RGB/RGBA

                    if (x instanceof int[]) {
                        var alpha = color.hasAlpha();

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
        return gl != null;
    }

    public void stop(Surface x) {
//        Zoomed r = (Zoomed) x.root();
//        if (r != null) {
//            JoglDisplay s = r.space;
//            if (s != null) {
//                if (texture != null) {
//                    //TODO if texure is shared, dont?
//                    this.texture.destroy(s.gl());
//                    this.texture = null;
//                }
//            }
//        }
    }

    public void delete() {
        var tt = this.texture;
        var gl = this.gl;
        if (gl != null && tt != null) {
            tt.destroy(gl);
        }
        this.texture = null;
        this.gl = null;
        this.data = null;
        this.src = null;
    }


    @Deprecated
    private static class MyTexSurface extends TexSurface {
        private final BufferedImage b;

        MyTexSurface(BufferedImage b) {
            this.b = b;
        }

        @Override
        protected void paint(GL2 gl, ReSurface reSurface) {
            var t = this.tex;
            if (t != null && t.data == null)
                t.set(b);
            super.paint(gl, reSurface);
        }

		@Override
		public boolean delete() {
        	this.tex.delete();
			return super.delete();
		}
	}
}
