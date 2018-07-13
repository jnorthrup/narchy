package nars.op;

import jcog.Util;
import jcog.pri.bag.impl.PLinkArrayBag;
import jcog.io.FSWatch;
import jcog.pri.PLink;
import jcog.pri.op.PriMerge;
import nars.*;
import nars.control.NARService;
import nars.op.java.Opjects;
import nars.op.mental.Abbreviation;
import nars.op.stm.ConjClustering;
import org.eclipse.collections.api.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static nars.Op.BELIEF;

/** watches a particular file or directory for live changes, notified by the filesystem asynchronously */
public class FileWatch extends NARService {



    static class PathState extends PLink<Path> {

        public PathState(Path path, float p) {
            super(path, p);
        }

        //unixtime this path was last updated
        private long updated = Long.MIN_VALUE;

        private boolean changed = false;

    }


    public static class FSView {

        final Map<Path,PLink<Path>> paths = new ConcurrentHashMap<>();
        final PLinkArrayBag<Path> active = new PLinkArrayBag<Path>(PriMerge.plus, 128);

        public FSView() {

        }

        public void add(Path path) {

        }

        public void bytes(Path path) {

        }

        public void change(Path path) {

        }
        public void remove(Path path) {

        }

        protected void accept(Pair<Path, WatchEvent.Kind> event) {
            //System.out.println(event);


            Path path = event.getOne(); //.toAbsolutePath();

            switch (event.getTwo().name()) {
                case "ENTRY_CREATE": {
                    PLink<Path> q = paths.computeIfAbsent(path, p -> new PathState(p, 1f));
                    active.put(q);
                    add(path);
                    break;
                }
                case "ENTRY_DELETE": {
                    paths.remove(path);
                    active.remove(path);
                    remove(path);
                    break;
                }
                case "ENTRY_MODIFY": {
                    PLink<Path> m = paths.get(path);
                    assert(m!=null);
                    m.priAdd(0.5f);
                    active.put(m);
                    change(path);
                    break;
                }

            }


        }
    }

    final static int FPS = 2;

    static final Logger logger = LoggerFactory.getLogger(FileWatch.class);
    final FSWatch fs;

    private final FSView watch;

    public FileWatch(Path path, NAR n) throws IOException {
        super();

        this.watch = new Opjects(n).a(
                $.func("cpu", /* computer (more specific than "central") processing unit, short for computer */
                        $.the(InetAddress.getLocalHost().getHostName())),
                FSView.class);

        fs = new FSWatch(path, n.exe, watch::accept);
        n.on(this);
    }

    @Override
    protected void starting(NAR nar) {
        fs.runFPS(FPS);
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

    private boolean loadable(Path path) {
        return path.getFileName().toString().endsWith(".nal");
    }

    private List<Task> load(Path path, List<Task> tasks) {



        nar.input(tasks);
        return tasks;
    }

    private void unload(Path p, List<Task> toUnload) {
        logger.info("{} unload {} tasks", p, toUnload.size());
        toUnload.forEach(t -> {
            if (t.isEternal() && t.isInput())
                nar.retract(t.stamp()[0]);
        });
    }

    public static void main(String[] args) throws IOException, Narsese.NarseseException {
        NAR n = NARS.tmp(6);
        n.log();

        new ConjClustering(n, BELIEF, 4, 16);
        new Abbreviation(n, "z", 5, 10, 1f, 32);

        n.termVolumeMax.set(40);
        //FileWatch a = new FileWatch(Paths.get("/var/log"), n);
        //FileWatch b = new FileWatch(Paths.get("/boot"), n);
        //FileWatch c = new FileWatch(Paths.get("/tmp"), n);
        FileWatch c = new FileWatch(Paths.get("/home/me/n/docs/nal/sumo"), n);
        
        n.input("$1.0 (add($cpu,$file) ==> ({$file}-->$cpu)).");
        n.input("$1.0 (add(#cpu,file($directory,$filename)) ==> ({$filename}-->$directory)).");

        n.startFPS(8f);

        while (true) {
            Util.sleep(100);
        }
    }

    //public static class Note
}
