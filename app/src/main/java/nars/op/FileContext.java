package nars.op;

import jcog.io.FileFunc;
import jcog.pri.PLink;
import jcog.pri.bag.impl.PLinkArrayBag;
import jcog.pri.bag.impl.PriReferenceArrayBag;
import jcog.pri.op.PriMerge;
import nars.$;
import nars.NAR;
import nars.control.NARPart;
import nars.op.java.Opjects;
import org.apache.commons.vfs2.FileChangeEvent;
import org.apache.commons.vfs2.FileListener;
import org.apache.commons.vfs2.FileSystemException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;

/**
 * provides support for watching particular file or directory for live changes, notified by the filesystem asynchronously
 */
public class FileContext extends NARPart {

    static final Logger logger = LoggerFactory.getLogger(FileContext.class);

    static void log(Exception e) {
        logger.error("{} {}", FileFunc.class, e);
    }

    private transient FileFunc.FILE root;

    final String rootPath;
    private @Nullable FileSystemException error = null;



    /**
     * nobject belief view
     */
    public static class FileBelief {

        final PriReferenceArrayBag<URL,jcog.pri.PriReference<URL>> active = new PLinkArrayBag<>(PriMerge.plus, 128);

        public FileBelief() {

        }

        public void add(URL url) {
            active.put(new PLink<>(url, 0.5f));
        }

//        public void bytes(URL url) {
//
//        }

        public void change(URL url) {
            active.put(new PLink<>(url, 0.25f));
        }

        public void remove(URL u) {
            active.remove(u);
        }

    }

    private final FileBelief view;

    public FileContext(String path, NAR n) throws IOException {
        super();

        this.view = new Opjects(n).a(
                $.INSTANCE.func("cpu", /* computer (more specific than "central") processing unit, short for computer */
                        $.INSTANCE.the(InetAddress.getLocalHost().getHostName())),
                FileBelief.class);

        this.rootPath = path;

        n.add(this);
    }

    /**
     * returns the last error occurred during start, or null if none
     */
    public @Nullable Exception error() {
        return error;
    }

    @Override
    protected void starting(NAR nar) {

        try {
            root = FileFunc.get(rootPath);
            error = null;
        } catch (FileSystemException e) {
            error = e;
            log(e);
            return;
        }

        root.on(new FileListener() {
            @Override
            public void fileCreated(FileChangeEvent event) throws Exception {
                view.add(event.getFile().getURL());
            }

            @Override
            public void fileDeleted(FileChangeEvent event) throws Exception {
                view.remove(event.getFile().getURL());
            }

            @Override
            public void fileChanged(FileChangeEvent event) throws Exception {
                view.change(event.getFile().getURL());
            }
        });
    }

    @Override
    protected void stopping(NAR nar) {
        /** kill the weakrefs */
        root = null;
    }



//    private void reload(Pair<Path, WatchEvent.Kind> event) {
//
//        if (!loadable(path))
//            return;


//        paths.compute(path, (p, exists) -> {
//            synchronized(fs) {
//                if (exists != null)
//                    unload(p, exists);
//
//                try {
//                    List<Task> t = Narsese.tasks(Files.asCharSource(p.toFile(), Charset.defaultCharset()).read(), nar);
//                    logger.info("{} loaded {} tasks", p, t.size());
//                    return load(p, t);
//                } catch (FileNotFoundException e) {
//                    if (exists!=null) {
//
//                        logger.warn("{} {}", p, e.getMessage());
//                    } else {
//                        logger.error("{} {}", p, e);
//                    }
//                } catch (IOException | Narsese.NarseseException e) {
//                    logger.error("{} {}", p, e);
//                }
//            }
//
//            return null;
//        });
//    }
//
//    private boolean loadable(Path path) {
//        return path.getFileName().toString().endsWith(".nal");
//    }
//
//    private List<Task> load(Path path, List<Task> tasks) {
//
//
//
//        nar.input(tasks);
//        return tasks;
//    }
//
//    private void unload(Path p, List<Task> toUnload) {
//        logger.info("{} unload {} tasks", p, toUnload.size());
//        toUnload.forEach(t -> {
//            if (t.isEternal() && t.isInput())
//                nar.retract(t.stamp()[0]);
//        });
//    }

//    public static void main(String[] args) throws IOException, Narsese.NarseseException {
//        NAR n = NARS.tmp(6);
//        n.log();
//
//        new ConjClustering(n, BELIEF, 4, 16);
//        new Abbreviation("z", 5, 10, n);
//
//        n.termVolumeMax.set(40);
//        //FileSys a = new FileSys(Paths.get("/var/log"), n);
//        FileContext b = new FileContext("/boot", n);
//        //FileSys c = new FileSys(Paths.get("/tmp"), n);
//        //FileSys c = new FileSys("/home/me/n/docs/nal/sumo", n);
//
//        n.input("$1.0 (add($cpu,$file) ==> ({$file}-->$cpu)).");
//        n.input("$1.0 (add(#cpu,file($directory,$filename)) ==> ({$filename}-->$directory)).");
//
//        n.startFPS(8f);
//
//        while (true) {
//            Util.sleepMS(100);
//        }
//    }


//    static Term theComponent(Path file) {
//
//        int n = file.getNameCount();
//
//        Term[] t = new Term[n];
//        for (int i = 0; i < n; i++)
//            t[i] = $.the(file.getName(i).toString());
//
//        return $.pRecurse(true, t);
//    }


}
