package kashiki.view;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.Texture;
import jcog.WTF;
import spacegraph.video.Tex;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public final class TextureProvider {
    private final int FONT_SIZE = 64;

    private final LoadingCache<String, Tex> textureCache = CacheBuilder.newBuilder().maximumSize(10000)
            .build(new CacheLoader<String, Tex>() {
                @Override
                public Tex load(String c) {
                    //return AWTTextureIO.newTexture(gl.getGLProfile(), getTexture(c, FONT_SIZE), true);


                    Tex t = new Tex();
                    return t;
                    //return TextureIO.newTexture(gl.getGLProfile(), charTex, true);
                }
            });
    private final LoadingCache<String, Double> sizeCache = CacheBuilder.newBuilder().maximumSize(10000)
            .build(new CacheLoader<String, Double>() {

                @Override
                public Double load(String c) {
                    return rawGetWidth(c);
                }
            });

    private GL2 gl;

    private static TextureProvider INSTANCE = null;

    public static TextureProvider getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TextureProvider();
        }
        return INSTANCE;
    }

    private final Font font;

    private TextureProvider() {
        try {
            font =
                    Font.createFont(Font.PLAIN,
                            this.getClass().getClassLoader().getResourceAsStream("font/CourierPrimeCode.ttf")).deriveFont(
                            (float) FONT_SIZE);
        } catch (FontFormatException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Texture getTexture(GL2 gl, String c) {
        this.gl = gl;

            Tex tt = textureCache.getUnchecked(c);
            Texture ttt = tt.texture;
            if (ttt == null) {
                synchronized (tt) {
                    //HACK
                    if ((ttt = tt.texture) == null) {
                        BufferedImage charTex = getTexture(c, FONT_SIZE);
                        tt.commit(gl);
                        tt.update(charTex);
                        tt.commit(gl); //HACK
                        if (tt.texture == null)
                            throw new WTF();
                        return tt.texture;
                    }
                }
            }
            if (ttt == null)
                throw new WTF();
            return ttt;


    }

    public double getWidth(String c) {
        try {
            return sizeCache.get(c);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private double rawGetWidth(String singleCharString) {
        BufferedImage image = new BufferedImage(FONT_SIZE, FONT_SIZE, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        FontMetrics fm = g2d.getFontMetrics(font);
        return (double) fm.charWidth(singleCharString.codePointAt(0)) / FONT_SIZE;
    }

    private BufferedImage getTexture(String c, int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        FontRenderContext fontRenderContext = g2d.getFontRenderContext();
        GlyphVector gv = font.createGlyphVector(fontRenderContext, c.toCharArray());

        FontMetrics fm = g2d.getFontMetrics(font);

        g2d.drawGlyphVector(gv, (FONT_SIZE - fm.charWidth(c.codePointAt(0))) / 2f, fm.getMaxAscent());
        return image;
    }
}
