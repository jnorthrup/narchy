package spacegraph.video;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.google.common.util.concurrent.MoreExecutors;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import jcog.io.bzip2.BZip2InputStream;
import jcog.io.tar.TarEntry;
import jcog.io.tar.TarInputStream;
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

/** general purpose texture cache */
public class ImageTexture extends Tex {

    private static final String fa_prefix = "fontawesome://";

    private static ImmutableMap<String, byte[]> fontAwesomeIcons;

    /**
     * pair(gl context, texture id)
     */
    private static final LoadingCache<Pair<GLContext, String>, TextureData> textureCache =
            //new SoftMemoize<>(cu -> {
            Caffeine.newBuilder().softValues().executor(MoreExecutors.directExecutor()).
                    removalListener((RemovalListener<Pair<GLContext, String>, TextureData>)
                            (c, t, cause) -> {
                                if (t!=null)
                                    t.destroy();
                            })
                    .build(cu -> {
                        try {
                            String u = cu.getTwo();
                            GLProfile profile = cu.getOne().getGL().getGLProfile();
                            if (u.startsWith(fa_prefix)) {
                                String icon = u.substring(fa_prefix.length());
                                byte[] b = fontAwesomeIcons.get("x128/" + icon + "-fs8.png");
                                if (b != null) {
                                    InputStream in = new ByteArrayInputStream(b);
                                    return TextureIO.newTextureData(profile, in, true, "png");
                                } else {
                                    throw new UnsupportedOperationException("unrecognized FontAwesome icon: " + u);
                                }
                            } else {
                                //return TextureIO.newTexture(new URL(u), true, null);
                                return TextureIO.newTextureData(profile, new URL(u), true, null);
                            }

                        } catch (IOException e) {
                            return null;
                        }
                    });

    static {
        //synchronized (ImageTexture.class) {
        {
            MutableMap<String, byte[]> fontAwesomeIcons = new UnifiedMap(1024);
//            final int bufferSize = 512 * 1024;
            ClassLoader classLoader = ImageTexture.class.getClassLoader();
            try (TarInputStream fa = new TarInputStream(new BZip2InputStream(true, classLoader.getResourceAsStream("fontawesome_128.bzip2")))) {
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


    private final String u;
    private final AtomicBoolean loading = new AtomicBoolean(false);
    TextureData textureData = null;

    private ImageTexture(URL path) {
        this(path.toString());
    }

    public ImageTexture(String path) {
        this.u = path;
        inverted = true;
    }

    /**
     * http://fontawesome.com/icons?d=gallery&m=free
     */
    public static ImageTexture awesome(String icon) {
        return new ImageTexture("fontawesome://" + icon);
    }

    public void paint(GL2 gl, RectFloat bounds, float repeatScale, float alpha) {
        if (texture == null) {
            if (loading.compareAndSet(false, true)) {

                //Exe.invokeLater(() -> {

                try {
                    textureData = textureCache.get(Tuples.pair(gl.getContext(), u));
                    if (textureData == null)
                        throw new NullPointerException(); //TODO logger.warn
                } catch (Throwable e) {
                    e.printStackTrace(); //TODO
                }

                loading.set(false);
            }
            if (textureData != null) {
                texture = TextureIO.newTexture(gl, textureData);
            } else {
                return;
            }
        }

        super.paint(gl, bounds, repeatScale, alpha);
    }

}
