package spacegraph.video;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import jcog.io.bzip2.BZip2InputStream;
import jcog.io.tar.TarEntry;
import jcog.io.tar.TarInputStream;
import jcog.memoize.Memoize;
import jcog.memoize.SoftMemoize;
import jcog.tree.rtree.rect.RectFloat2D;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ImageTexture extends Tex {

    static final Map<String, byte[]> fontAwesomeIcons = new HashMap();
    //private static JarFile fontawesome = null;
    private static final String fa_prefix = "fontawesome://";
    static final Memoize<String, Texture> textureCache = new SoftMemoize<>((u) -> {
        try {


            if (u.startsWith(fa_prefix)) {
                String icon = u.substring(fa_prefix.length());
                byte[] b = fontAwesomeIcons.get("x128/" + icon + "-fs8.png");
                if (b != null) {
                    try (InputStream in = new ByteArrayInputStream(b)) {
                        Texture t = TextureIO.newTexture(in, true, "png");
                        return t;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else{
                    throw new RuntimeException("unreocgnized FontAwesome icon: " + u);
                }
            } else {
                return TextureIO.newTexture(new URL(u), true, null);
            }

        } catch (IOException e) {
            return null;
        }
    }, 512, true);

    static {
        try {
            ClassLoader classLoader = ImageTexture.class.getClassLoader();
//            File file = new File(classLoader.getResource("fontawesome_128.jar").getFile());
//            fontawesome = new JarFile(file);


            InputStream cin =
                    new BufferedInputStream(classLoader.getResourceAsStream("fontawesome_128.bzip2"),
                    255660+1 //fully buffer
                    );
            TarInputStream fa = new TarInputStream(new BZip2InputStream(true, cin));
            TarEntry e;
            while ((e = fa.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                fontAwesomeIcons.put(e.getName(), fa.readAllBytes());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

//    public static void main(String[] args) throws MalformedURLException {
//        //pngquant 2 wrench.png --speed 1 --quality 0 --nofs
//
//        String file = "/home/me/Font-Awesome-SVG-PNG/white/png/x128/wrench-fs8.png";
//        SpaceGraph.window(
//                new Gridding(
////                    new ImageTexture(new File(file)).view(),
//                        new ImageTexture("fontawesome://wrench").view(),
//                        new ImageTexture("fontawesome://feed").view(),
//                        new ImageTexture("fontawesome://space-shuttle").view(),
//                        new ImageTexture("fontawesome://youtube").view(),
//                        new ImageTexture(new File(file)).view()
//                ),
//                500, 500);
//    }

}
