package nars.op.in;

import com.google.common.io.Files;
import jcog.io.FSWatch;
import nars.NAR;
import nars.Narsese;
import nars.Task;
import nars.control.NARService;
import org.eclipse.collections.api.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NoteFS extends NARService {

    static final Logger logger = LoggerFactory.getLogger(NoteFS.class);
    final FSWatch fs;
    final Map<Path,List<Task>> loaded = new ConcurrentHashMap<>();

    public NoteFS(String path, NAR n) throws IOException {
        super(n);
        fs = new FSWatch(path, n.exe, this::reload);
    }

    private void reload(Pair<Path, WatchEvent.Kind> event) {

        Path path = event.getOne().toAbsolutePath();

        if (!loadable(path))
            return;

        loaded.compute(path, (p, exists) -> {
            synchronized(fs) {
                if (exists != null)
                    unload(p, exists);

                try {
                    List<Task> t = Narsese.tasks(Files.asCharSource(p.toFile(), Charset.defaultCharset()).read(), nar);
                    logger.info("{} loaded {} tasks", p, t.size());
                    return load(p, t);
                } catch (FileNotFoundException e) {
                    if (exists!=null) {
                        
                        logger.warn("{} {}", p, e.getMessage());
                    } else {
                        logger.error("{} {}", p, e);
                    }
                } catch (IOException | Narsese.NarseseException e) {
                    logger.error("{} {}", p, e);
                }
            }

            return null; 
        });
    }

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

}
