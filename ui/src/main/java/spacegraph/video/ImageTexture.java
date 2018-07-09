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
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

public class ImageTexture extends Tex {

    private static final String fa_prefix = "fontawesome://";

    private static ImmutableMap<String, byte[]> fontAwesomeIcons;
    static  {
        synchronized (fa_prefix) {
            MutableMap<String, byte[]> fontAwesomeIcons = new UnifiedMap(1024);
            final int bufferSize = 512 * 1024;
            ClassLoader classLoader = ImageTexture.class.getClassLoader();
            try(TarInputStream fa = new TarInputStream(new BufferedInputStream(new BZip2InputStream(true, classLoader.getResourceAsStream("fontawesome_128.bzip2")), bufferSize))) {
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
    private static final Memoize<LongObjectPair<String>, Texture> textureCache = new SoftMemoize<>((cu) -> {
        try {
            String u = cu.getTwo();
            if (u.startsWith(fa_prefix)) {
                String icon = u.substring(fa_prefix.length());
                byte[] b = fontAwesomeIcons.get("x128/" + icon + "-fs8.png");
                if (b != null) {
                    InputStream in = new ByteArrayInputStream(b);
                    return TextureIO.newTexture(in, true, "png");
                } else{
                    throw new RuntimeException("unrecognized FontAwesome icon: " + u);
                }
            } else {
                return TextureIO.newTexture(new URL(u), true, null);
            }

        } catch (IOException e) {
            return null;
        }
    }, 4096, true /* weak */);



    private final String u;





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

    public void paint(GL2 gl, RectFloat2D bounds, float repeatScale, float alpha) {
        if (texture == null) {

            //TODO async load

            texture = textureCache.apply(pair(gl.getContext().getHandle(), u));
            if (texture == null) {
                
                throw new NullPointerException();
            }

        }
        super.paint(gl, bounds, repeatScale, alpha);
    }

















}
