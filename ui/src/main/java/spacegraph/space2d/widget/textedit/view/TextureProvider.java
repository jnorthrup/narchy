package spacegraph.space2d.widget.textedit.view;

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

public final class TextureProvider {
    private final int FONT_SIZE = 64;
    private final int FONT_BITMAP_WIDTH = 48;
    private final int FONT_BITMAP_HEIGHT = FONT_SIZE;

    private final LoadingCache<String, Tex> textureCache = CacheBuilder.newBuilder().maximumSize(10000)
            .build(new CacheLoader<>() {
                @Override
                public Tex load(String c) {
                    //return AWTTextureIO.newTexture(gl.getGLProfile(), getTexture(c, FONT_SIZE), true);
                    return new Tex();
                    //return TextureIO.newTexture(gl.getGLProfile(), charTex, true);
                }
            });



    @Deprecated public static final TextureProvider the;
    static {
        the = new TextureProvider();
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

            Tex tt = textureCache.getUnchecked(c);
            Texture ttt = tt.texture;
            if (ttt == null) {
                synchronized (tt) {
                    //HACK
                    if ((ttt = tt.texture) == null) {
                        BufferedImage charTex = getTexture(c, FONT_BITMAP_WIDTH, FONT_BITMAP_HEIGHT);
                        tt.commit(gl);
                        tt.set(charTex);
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


    private float rawGetWidth(String singleCharString) {
        BufferedImage image = new BufferedImage(FONT_SIZE, FONT_SIZE, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        FontMetrics fm = g2d.getFontMetrics(font);
        return fm.charWidth(singleCharString.codePointAt(0)) / FONT_SIZE;
    }

    private BufferedImage getTexture(String c, int sizeX, int sizeY) {
        BufferedImage image = new BufferedImage(sizeX, sizeY, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        FontRenderContext fontRenderContext = g2d.getFontRenderContext();
        GlyphVector gv = font.createGlyphVector(fontRenderContext, c.toCharArray());

        FontMetrics fm = g2d.getFontMetrics(font);


        g2d.drawGlyphVector(gv, 0 /*(FONT_SIZE - fm.charWidth(c.codePointAt(0)))*/, fm.getMaxAscent());
        return image;
    }
}
