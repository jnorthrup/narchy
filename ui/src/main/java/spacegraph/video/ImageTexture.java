package spacegraph.video;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import jcog.exe.Exe;
import jcog.io.bzip2.BZip2InputStream;
import jcog.io.tar.TarEntry;
import jcog.io.tar.TarInputStream;
import jcog.memoize.Memoize;
import jcog.memoize.SoftMemoize;
import jcog.tree.rtree.rect.RectFloat;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.tuple.Tuples;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import static jcog.data.map.CustomConcurrentHashMap.STRONG;

public class ImageTexture extends Tex {

    private static final String fa_prefix = "fontawesome://";

    private static ImmutableMap<String, byte[]> fontAwesomeIcons;
    static  {
        synchronized (fa_prefix) {
            MutableMap<String, byte[]> fontAwesomeIcons = new UnifiedMap(1024);
//            final int bufferSize = 512 * 1024;
            ClassLoader classLoader = ImageTexture.class.getClassLoader();
            try(TarInputStream fa = new TarInputStream(new BZip2InputStream(true, classLoader.getResourceAsStream("fontawesome_128.bzip2")))) {
                TarEntry e;
                while ((e = fa.getNextEntry()) != null) {
                    if (!e.isDirectory())
                        fontAwesomeIcons.put(e.getName(), fa.readAllBytes());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            ImageTexture.fontAwesomeIcons = fontAwesomeIcons.toImmutable();
        }
    }

    /** pair(gl context, texture name) */
    private static final Memoize<Pair<GLContext,String>, TextureData> textureCache = new SoftMemoize<>((cu) -> {
        try {
            String u = cu.getTwo();
            GLProfile profile = cu.getOne().getGL().getGLProfile();
            if (u.startsWith(fa_prefix)) {
                String icon = u.substring(fa_prefix.length());
                byte[] b = fontAwesomeIcons.get("x128/" + icon + "-fs8.png");
                if (b != null) {
                    InputStream in = new ByteArrayInputStream(b);
                    return TextureIO.newTextureData(profile, in, true, "png");
                } else{
                    throw new RuntimeException("unrecognized FontAwesome icon: " + u);
                }
            } else {
                //return TextureIO.newTexture(new URL(u), true, null);
                return TextureIO.newTextureData(profile, new URL(u), true, null);
            }

        } catch (IOException e) {
            return null;
        }
    }, 1024, STRONG);



    private final String u;
    private final AtomicBoolean loading = new AtomicBoolean(false);


    private ImageTexture(URL path) {
        this(path.toString());
    }

    public ImageTexture(String path) {
        this.u = path;
        inverted = true; 
    }

    /** http://fontawesome.com/icons?d=gallery&m=free */
    public static ImageTexture awesome(String icon) {
        return new ImageTexture("fontawesome://" + icon );
    }

    TextureData textureData = null;

    public void paint(GL2 gl, RectFloat bounds, float repeatScale, float alpha) {
        if (texture == null) {
            if (loading.compareAndSet(false, true)) {

                Pair<GLContext, String> key = Tuples.pair(gl.getContext(), u);

                Exe.invokeLater(() -> {

                    textureData = textureCache.apply(key);

                    if (textureData == null)
                        throw new NullPointerException(); //TODO logger.warn

                });

            }
            if (textureData!=null) {
                texture = TextureIO.newTexture(gl, textureData);
            } else {
                return;
            }
        }

        super.paint(gl, bounds, repeatScale, alpha);
    }

}
