package spacegraph.space2d.widget.textedit.view;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.jogamp.opengl.GL2;
import spacegraph.video.Tex;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.IOException;

public final class TextureProvider {
	public static final String DEFAULT_FONT_PATH = "font/CourierPrimeCode.ttf";
	@Deprecated
	public static final TextureProvider the = new TextureProvider();
	private static final int FONT_SIZE = 64;
    private static final int FONT_BITMAP_WIDTH = 48;
	private final Font font;
	private final LoadingCache<String, BufferedImage> glyphCache = CacheBuilder.newBuilder().maximumSize(2048)
		.build(new CacheLoader<>() {
			@Override public BufferedImage load(String c) {
				//return AWTTextureIO.newTexture(gl.getGLProfile(), getTexture(c, FONT_SIZE), true);
				//return TextureIO.newTexture(gl.getGLProfile(), charTex, true);
                return getTexture(c, FONT_BITMAP_WIDTH, FONT_SIZE);
			}
		});

	private TextureProvider() {
		try {
			font = Font.createFont(Font.PLAIN,
				this.getClass().getClassLoader().getResourceAsStream(DEFAULT_FONT_PATH)).deriveFont(
				(float) FONT_SIZE);
		} catch (FontFormatException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	Tex getTexture(GL2 gl, String c) {

		Tex tt = new Tex();
		tt.commit(gl);

		BufferedImage charTex = glyphCache.getUnchecked(c);
		tt.set(charTex);
		tt.commit(gl); //HACK  repeat commit() necessary for some reason

		return tt;
	}


//    private float rawGetWidth(String singleCharString) {
//        BufferedImage image = new BufferedImage(FONT_SIZE, FONT_SIZE, BufferedImage.TYPE_BYTE_GRAY);
//        Graphics2D g2d = (Graphics2D) image.getGraphics();
//        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
//        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//        FontMetrics fm = g2d.getFontMetrics(font);
//        return fm.charWidth(singleCharString.codePointAt(0)) / FONT_SIZE;
//    }

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
