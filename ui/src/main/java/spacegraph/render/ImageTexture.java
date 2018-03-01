package spacegraph.render;

import com.google.common.io.Resources;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import jcog.memoize.Memoize;
import jcog.memoize.SoftMemoize;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.container.Gridding;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class ImageTexture extends Tex {

    private static JarFile fontawesome = null;
    private static final String fa_prefix = "fontawesome://";

    static {
        try {
            fontawesome = new JarFile(new File(Resources.getResource("fontawesome_128.jar").toURI()));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    static final Memoize<String,Texture> textureCache = new SoftMemoize<>((u) -> {
        try {

            if (fontawesome!=null && u.startsWith(fa_prefix)) {
                String icon = u.substring(fa_prefix.length());
                ZipEntry e = fontawesome.getEntry("x128/" + icon + "-fs8.png");
                return TextureIO.newTexture(fontawesome.getInputStream(e), true, "png");
            } else {
                return TextureIO.newTexture(new URL(u), true, null);
            }

        } catch (IOException e) {
            return null;
        }
    }, 512, true);

    final String u;

    public ImageTexture(File f) throws MalformedURLException {
        this(f.toURL());
    }

    public ImageTexture(URL path) {
        this(path.toString());
    }
    public ImageTexture(String path) {
        this.u = path;
    }

    public void paint(GL2 gl, RectFloat2D bounds, float repeatScale, float alpha) {
        if (texture == null) {

            texture = textureCache.apply(u);
            if (texture == null)
                throw new NullPointerException();

        }
        super.paint(gl, bounds, repeatScale, alpha);
    }

    public static void main(String[] args) throws MalformedURLException {
        //pngquant 2 wrench.png --speed 1 --quality 0 --nofs

        String file = "/home/me/Font-Awesome-SVG-PNG/white/png/x128/wrench-fs8.png";
        SpaceGraphFlat.window(
                new Gridding(
//                    new ImageTexture(new File(file)).view(),
                    new ImageTexture("fontawesome://wrench").view(),
                        new ImageTexture("fontawesome://feed").view(),
                        new ImageTexture("fontawesome://space-shuttle").view(),
                        new ImageTexture("fontawesome://youtube").view(),
                    new ImageTexture(new File(file)).view()
                ),
                500, 500);
    }

}
