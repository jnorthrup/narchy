package nars.op;

import jcog.Util;
import jcog.bag.impl.PLinkArrayBag;
import jcog.io.FSWatch;
import jcog.pri.PLink;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.NARS;
import nars.Task;
import nars.control.NARService;
import nars.op.java.Opjects;
import org.eclipse.collections.api.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NoteFS extends NARService {



    static class PathState extends PLink<Path> {

        public PathState(Path path, float p) {
            super(path, p);
        }

        //unixtime this path was last updated
        private long updated = Long.MIN_VALUE;

        private boolean changed = false;

    }

    public static class PathWatch {

        final Map<Path,PLink<Path>> paths = new ConcurrentHashMap<>();
        final PLinkArrayBag<Path> active = new PLinkArrayBag<Path>(PriMerge.plus, 128);

        public PathWatch() {

        }

        public void add(String path) {

        }
        public void change(String path) {

        }
        public void remove(String path) {

        }

        protected void accept(Pair<Path, WatchEvent.Kind> event) {
            System.out.println(event);

            Path path = event.getOne().toAbsolutePath();
            switch (event.getTwo().name()) {
                case "ENTRY_CREATE": {
                    PLink<Path> q = paths.computeIfAbsent(path, p -> new PathState(p, 1f));
                    active.put(q);
                    add(path.toString());
                    break;
                }
                case "ENTRY_DELETE": {
                    paths.remove(path);
                    active.remove(path);
                    remove(path.toString());
                    break;
                }
                case "ENTRY_MODIFY": {
                    PLink<Path> m = paths.get(path);
                    assert(m!=null);
                    m.priAdd(0.5f);
                    active.put(m);
                    change(path.toString());
                    break;
                }

            }


        }
    }

    final static int FPS = 2;

    static final Logger logger = LoggerFactory.getLogger(NoteFS.class);
    final FSWatch fs;

    private final PathWatch watch;

    public NoteFS(String path, NAR n) throws IOException {
        super();
        this.watch = new Opjects(n).a(path, PathWatch.class);
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

    public static void main(String[] args) throws IOException {
        NAR n = NARS.tmp();
        NoteFS fs = new NoteFS("/var/log", n);

        n.log();
        n.startFPS(4f);

        while (true) {
            Util.sleep(100);
        }
    }
}
