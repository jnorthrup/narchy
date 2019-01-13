package alice.tuprolog;

import alice.util.JavaDynamicClassLoader;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JavaDynamicClassLoader Test Case
 *
 * @author Alessio Mercurio
 */
public class JavaDynamicClassLoaderTestCase {

    final static int PATHS_NUMBER = 2;
    String[] paths = new String[PATHS_NUMBER];

    @Test
    public void ConstructorTest() throws IOException {
        JavaDynamicClassLoader loader = new JavaDynamicClassLoader();
        assertNotNull(loader);

        setPath(true);
        URL[] urls = getURLsFromStringArray(paths);
        loader = new JavaDynamicClassLoader(urls, this.getClass().getClassLoader());
        assertEquals(2, loader.getURLs().length);
    }

    @Disabled
    @Test
    public void LoadClassTest() throws
            IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        JavaDynamicClassLoader loader = null;
        setPath(true);
        URL[] urls = getURLsFromStringArray(paths);
        loader = new JavaDynamicClassLoader(urls, this.getClass().getClassLoader());
        assertEquals(2, loader.getURLs().length);

        Class<?> cl = loader.loadClass("Counter");
        assertNotNull(cl);
        Method m = cl.getMethod("inc");
        m.setAccessible(true);
        Object obj = cl.newInstance();
        m.invoke(obj);
        Method m1 = cl.getMethod("getValue");
        m1.setAccessible(true);
        Object res_obj = m1.invoke(obj);
        int res = new Integer(res_obj.toString());
        assertEquals(1, res);
    }

    @Test
    public void LoadClassNotFoundTest() {
        assertThrows(ClassNotFoundException.class, () -> {
            JavaDynamicClassLoader loader = null;
            setPath(true);
            URL[] urls = getURLsFromStringArray(paths);
            loader = new JavaDynamicClassLoader(urls, this.getClass().getClassLoader());
            loader.loadClass("ClassNotFound");
        });
    }

    @Test
    public void InvalidPathTest() {
        assertThrows(ClassNotFoundException.class, () -> {
            JavaDynamicClassLoader loader = null;
            URL url = new File(".").toURI().toURL();
            loader = new JavaDynamicClassLoader(new URL[]{url}, this.getClass().getClassLoader());
            loader.loadClass("Counter");
        });
    }

    @Disabled
    @Test
    public void URLHandling() throws ClassNotFoundException, IOException {
        JavaDynamicClassLoader loader = null;
        URL url = new File(".").toURI().toURL();
        loader = new JavaDynamicClassLoader(new URL[]{url}, this.getClass().getClassLoader());
        assertEquals(1, loader.getURLs().length);
        loader.removeURL(url);
        assertEquals(0, loader.getURLs().length);
        setPath(true);
        loader.addURLs(getURLsFromStringArray(paths));
        assertEquals(2, loader.getURLs().length);
        loader.loadClass("Counter");
        assertEquals(1, loader.getLoadedClasses().length);
    }

    @Test
    public void TestNestedPackage() {
        assertThrows(ClassNotFoundException.class, () -> {

            JavaDynamicClassLoader loader = null;
            File file = new File(".");
            String tempPath = file.getCanonicalPath()
                    + File.separator + "test"
                    + File.separator + "unit"
                    + File.separator + "TestURLClassLoaderNestedPackage.jar";
            URL[] urls = getURLsFromStringArray(tempPath);
            loader = new JavaDynamicClassLoader(urls, this.getClass().getClassLoader());
            Class<?> cl = loader.loadClass("acme.corp.Counter");
            assertNotNull(cl);
            cl = loader.loadClass("java.lang.String");
            assertNotNull(cl);
            loader.removeAllURLs();
            cl = loader.loadClass("Counter");
        });
    }

    private void setPath(boolean valid) throws IOException {
        File file = new File(".");
        
        if (valid) {
            paths[0] = file.getCanonicalPath()
                    + File.separator + "test"
                    + File.separator + "unit"
                    + File.separator + "TestURLClassLoader.jar";
        }
        paths[1] = file.getCanonicalPath();
    }

    private URL[] getURLsFromStringArray(String... paths) throws MalformedURLException {
        URL[] urls = new URL[paths.length];

        for (int i = 0; i < paths.length; i++) {
            File directory = new File(paths[i]);
            urls[i] = (directory.toURI().toURL());
        }
        return urls;
    }
}
