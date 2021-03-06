package org.gridkit.vicluster.telecontrol;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//


import jcog.data.list.FasterList;
import jcog.data.map.ConcurrentFastIteratingHashSet;
import jcog.util.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.ref.WeakReference;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.*;
import java.util.stream.Stream;

/**
 * override for jdk9 compatibility
 */
public class Classpath {
    private static final Logger LOGGER = LoggerFactory.getLogger(Classpath.class);
    private static final String DIGEST_ALGO = "SHA-1";

    //TODO use better cache
    private static final Map<ClassLoader, List<Classpath.ClasspathEntry>> CLASSPATH_CACHE =
            Collections.synchronizedMap(
                new WeakHashMap()
            );
    private static final WeakHashMap<URL, WeakReference<Classpath.ClasspathEntry>> CUSTOM_ENTRIES = new WeakHashMap();
    private static final URL JRE_ROOT = getJreRoot();


    public static List<Classpath.ClasspathEntry> getClasspath(ClassLoader classloader) {


        return CLASSPATH_CACHE.computeIfAbsent(classloader, new Function<ClassLoader, List<ClasspathEntry>>() {
            @Override
            public List<ClasspathEntry> apply(ClassLoader cl) {

                List<Classpath.ClasspathEntry> classpath = new FasterList(512);

                if (classloader instanceof URLClassLoader) {
                    fillClasspath(classpath, listCurrentClasspath((URLClassLoader) classloader));
                } else if (classloader == Thread.currentThread().getContextClassLoader()) {
                    //HACK
                    String[] jars = System.getProperty("java.class.path").split(":");
                    URL[] urls = new URL[jars.length];
                    int i = 0;
                    for (String x : jars) {
                        try {
                            urls[i++] = new URL("file://" + x);//classloader.getResource(x);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                    }
                    fillClasspath(classpath, listCurrentClasspath(urls));
                }

                return /*Collections.unmodifiableList*/(classpath);
            }
        });

    }

    public static Collection<URL> listCurrentClasspath(URLClassLoader classLoader) {
        return listCurrentClasspath(classLoader.getURLs());
    }

    public static Collection<URL> listCurrentClasspath(URL[] uu) {
        Set<URL> result = new ConcurrentFastIteratingHashSet<>(ArrayUtil.EMPTY_URL_ARRAY);

        Stream.of(uu).parallel().forEach(new Consumer<URL>() {
            @Override
            public void accept(URL u) {
                addEntriesFromManifest(result, u);
            }
        });

//        for (int i = 0; i < uu.length; ++i)
//
//
//
////            ClassLoader cls = classLoader.getParent();
////            if (!(cls instanceof URLClassLoader) || cls.getClass().getName().endsWith("$ExtClassLoader")) {
////                return new ArrayList(result);
////            }
////
////            classLoader = (URLClassLoader) cls;
//        //}
        //return new ArrayList(result);
        return result;
    }

    private static final ConcurrentMap<String, String> MISSING_URL = new ConcurrentHashMap(64, 0.75F, 1);

    private static void addEntriesFromManifest(Set<URL> list, URL url) {
        if (!list.contains(url)) {
            try {
                InputStream is;
                try {
                    is = url.openStream();
                } catch (Exception var12) {
                    String path = url.toString();
                    if (MISSING_URL.put(path, path) == null) {
                        LOGGER.warn("URL not found and will be excluded from classpath: " + path);
                    }

                    throw var12;
                }

                if (is != null) {
                    list.add(url);
                } else {
                    String path = url.toString();
                    if (MISSING_URL.put(path, path) == null) {
                        LOGGER.warn("URL not found and will be excluded from classpath: " + path);
                    }
                }

                JarInputStream jar = new JarInputStream(is);
                Manifest mf = jar.getManifest();
                jar.close();
                if (mf == null) {
                    return;
                }

                String cp = mf.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
                if (cp != null) {
                    String[] arr$ = cp.split("\\s+");
                    int len$ = arr$.length;

                    for (int i$ = 0; i$ < len$; ++i$) {
                        String entry = arr$[i$];

                        try {
                            URL ipath = new URL(url, entry);
                            addEntriesFromManifest(list, ipath);
                        } catch (Exception var11) {
                        }
                    }
                }
            } catch (Exception var13) {
            }

        }
    }

    public static Classpath.ClasspathEntry getLocalEntry(String path) throws IOException {
        synchronized (Classpath.class) {
            try {
                URL url = (new File(path)).toURI().toURL();
                WeakReference<ClasspathEntry> wr = CUSTOM_ENTRIES.get(url);
                Classpath.ClasspathEntry entry;
                if (wr != null) {
                    entry = wr.get();
                } else {
                    entry = newEntry(url);
                    CUSTOM_ENTRIES.put(url, new WeakReference(entry));
                }
                return entry;
            } catch (MalformedURLException | URISyntaxException var4) {
                throw new IOException(var4);
            }
        }
    }

    public static synchronized FileBlob createBinaryEntry(String name, byte[] data) {
        return new Classpath.ByteBlob(name, data);
    }

    private static void fillClasspath(List<Classpath.ClasspathEntry> classpath, Collection<URL> urls) {

        for (URL url : urls) {
            if (!isIgnoredJAR(url)) {
                try {
                    ClasspathEntry entry = newEntry(url);
                    if (entry == null) {
                        LOGGER.warn("Cannot copy URL content: " + url);
                    } else {
                        classpath.add(entry);
                    }
                } catch (Exception var5) {
                    LOGGER.warn("Cannot copy URL content: " + url, var5);
                }
            }
        }

    }

    private static boolean isIgnoredJAR(URL url) {
        try {
            if ("file".equals(url.getProtocol())) {
                File f = new File(url.toURI());
                if (f.isFile()) {
                    if (belongs(JRE_ROOT, url)) {
                        return true;
                    }

                    if (f.getName().startsWith("surefire") && isManifestOnly(f)) {
                        return true;
                    }
                }
            }
        } catch (URISyntaxException var2) {
        }

        return false;
    }

    private static boolean isManifestOnly(File f) {
        JarFile jar = null;

        boolean var4;
        try {
            boolean var3;
            try {
                jar = new JarFile(f);
                Enumeration<JarEntry> en = jar.entries();
                if (!en.hasMoreElements()) {
                    var3 = false;
                    return var3;
                }

                JarEntry je = en.nextElement();
                if ("META-INF/".equals(je.getName())) {
                    if (!en.hasMoreElements()) {
                        var4 = false;
                        return var4;
                    }

                    je = en.nextElement();
                }

                if ("META-INF/MANIFEST.MF".equals(je.getName())) {
                    var4 = !en.hasMoreElements();
                    return var4;
                }

                var4 = false;
            } catch (IOException var17) {
                var3 = false;
                return var3;
            }
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException var16) {
                }
            }

        }

        return var4;
    }

    private static boolean belongs(URL base, URL derived) {
        return derived.toString().startsWith(base.toString());
    }

    private static URL getJavaHome() throws MalformedURLException {
        return (new File(System.getProperty("java.home"))).toURI().toURL();
    }

    private static URL getJreRoot() {
        try {
            String jlo = ClassLoader.getSystemResource("java/lang/Object.class").toString();
            if (jlo.startsWith("jar:")) {
                String root = jlo.substring("jar:".length());
                int n = root.indexOf(33);
                root = root.substring(0, n);
                if (root.endsWith("/rt.jar")) {
                    root = root.substring(0, root.lastIndexOf(47));
                    if (root.endsWith("/lib")) {
                        root = root.substring(0, root.lastIndexOf(47));
                        return new URL(root);
                    }
                }
            }

            return getJavaHome();
        } catch (MalformedURLException var3) {
            return null;
        }
    }

    private static Classpath.ClasspathEntry newEntry(URL url) throws IOException, URISyntaxException {
        ClasspathEntry entry = new Classpath.ClasspathEntry();
        entry.url = url;
        File file = uriToFile(url.toURI());
        String fileName = file.getName();
        if (file.isFile()) {
            entry.file = file;
            entry.filename = fileName;
        } else {
            String lname = fileName;
            File parent = file.getParentFile();
            if ("classes".equals(lname)) {
                lname = parent.getName();
            }

            if ("target".equals(lname)) {
                lname = parent.getParentFile().getName();
            }

            if (lname.startsWith(".")) {
                lname = lname.substring(1);
            }

            lname += ".jar";
            entry.file = file;
            entry.filename = lname;
            if (isEmpty(file)) {
                LOGGER.warn("Classpath entry is empty: " + file.getCanonicalPath());
                return null;
            }

            entry.lazyJar.set(true);
        }

        return entry;
    }

    private static boolean isEmpty(File file) {
        File[] files = file.listFiles();
        if (files == null) {
            return true;
        } else {
            File[] arr$ = files;
            int len$ = files.length;

            for (int i = 0; i < len$; i++) {
                File c = arr$[i];
                if (c.isFile() || !isEmpty(c)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static File uriToFile(URI uri) {
        if ("file".equals(uri.getScheme())) {
            if (uri.getAuthority() == null) {
                return new File(uri);
            } else {

                try {
                    String path = "file:////" + uri.getAuthority() + '/' + uri.getPath();
                    return new File(new URI(path));
                } catch (URISyntaxException var3) {
                    return new File(uri);
                }
            }
        } else {
            return new File(uri);
        }
    }

    static class ByteBlob implements FileBlob {
        private final String filename;
        private final String hash;
        private final byte[] data;

        public ByteBlob(String filename, byte[] data) {
            this.filename = filename;
            this.data = data;
            this.hash = StreamHelper.digest(data, "SHA-1");
        }

        public File getLocalFile() {
            return null;
        }

        public String getFileName() {
            return this.filename;
        }

        public String getContentHash() {
            return this.hash;
        }

        public InputStream getContent() {
            return new ByteArrayInputStream(this.data);
        }

        public long size() {
            return (long) this.data.length;
        }
    }

    public static class ClasspathEntry implements FileBlob {
        private URL url;
        private String filename;
        private String hash;
        private File file;
        private final AtomicBoolean lazyJar = new AtomicBoolean(false);
        private volatile byte[] data;
        private Map<String, Object> marks;

        public ClasspathEntry() {
        }

        public synchronized void setMark(String key, Object value) {
            if (this.marks == null)
                this.marks = new HashMap(1);

            this.marks.put(key, value);
        }

//        public synchronized <T> T getMark(String key) {
//            return this.marks == null ? null : this.marks.get(key);
//        }

        public URL getUrl() {
            return this.url;
        }

        public File getLocalFile() {
            return this.file;
        }

        public String getFileName() {
            return this.filename;
        }

        public synchronized String getContentHash() {
            if (this.hash == null) {
                this.hash = StreamHelper.digest(this.getData(), "SHA-1");
            }

            return this.hash;
        }

        public synchronized InputStream getContent() {
            this.ensureData();

            try {
                return this.data != null ? new ByteArrayInputStream(this.data) : new FileInputStream(this.file);
            } catch (FileNotFoundException var2) {
                throw new RuntimeException(var2.getMessage());
            }
        }

        private void ensureData() {

            if (this.lazyJar.compareAndSet(true, false)) {
                if (this.data==null) {
                    synchronized (this) {
                        try {
                            this.data = ClasspathUtils.jarFiles(this.file.getPath());
                        } catch (IOException var2) {
                            throw new RuntimeException(var2);
                        }
                    }
                }
            }
        }

        public long size() {
            this.ensureData();
            return this.data != null ? (long) this.data.length : this.file.length();
        }

        public synchronized byte[] getData() {
            this.ensureData();
            return this.data != null ? this.data : StreamHelper.readFile(this.file);
        }

        public String toString() {
            return this.filename;
        }
    }
}
