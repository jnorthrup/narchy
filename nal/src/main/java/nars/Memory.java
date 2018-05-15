package nars;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import jcog.list.FasterList;
import jdk.internal.jline.internal.Nullable;
import nars.term.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static nars.Op.ATOM;

/**
 * high-level trans-personal memory interface
 * <p>
 * ```
 * high level memory control vocabulary
 * load(fromContext[,filter]) - effectively copy(from, SELF, ...)
 * save(toContext[,filter]) - effectively copy(SELF, to, ...)
 * copy(from, to[, filter]) - copy from one context into another.  this is more like a mixing operation since it can be partial
 * <p>
 * contexts are terms and act like URI's.
 * they define a namespace index offering access to entire sets of 
 * just like URI's, certain patterns may indicate it is accessed by a particular strategy:
 * RAM memory - side memories running parallel with the current SELF, which could theoreticaly be swapped to, or at least blended with through memory ops
 * File system - provides user with convenient access to data files, especially for development
 * File system /tmp - for data too large to fit in memory but not worth saving past the next reboot
 * Network - any remote internet address and protocol URI)
 * Spatiotemoral region - indicates its content is relevant to what is or was there. (lucene's 4D double range vector field allows r-tree (range) indexing in 4D: 3D space 1D time)
 * Database & Search/etc. Index
 * <p>
 * filters specify desired include/exclude criteria for the memory transfer process.
 * these may apply to individual items, like their contained characters, budgets, or complexities.
 * or they may apply in the aggregate by limiting the min/max amount of items processed.
 * 0 filters, 1 filter, or N-filters as they can be chained or parallelized like any effects processing
 * procedure, analog or digital.
 * <p>
 * the particular budget state of a set of tasks forming a "memory" (snapshot, as distinct from the
 * runtime Memory) should not be considered *the* memory state but a possible one, at least the default
 * <p>
 * for example,
 * the meaning of a group of tasks can change drastically by shifting the priorities among them.
 * so the budget state,
 * collected as one or more additional snapshots which can be applied or mixed.  these will
 * need to be identified by names as well.  these are not like versions, although you could capture
 * them at regular points in time to study how it evolves on its own. instead they provide access to
 * a dimension of alternate cognitive possibilities existing within the same (non-budgeting aspects of a)
 * collection of knowledge (tasks).
 * <p>
 * similar kinds of filters can shape runtime system parameters:
 * <p>
 * task(ctx, filter) - apply task-shaping filters to individual tasks, such as budget shaping
 * links(ctx, filter) - link-shaping filters, link capacity heuristics / graph connectivity constraints
 * beliefs(ctx, filter) - belief capacity heuristics
 * questions(ctx, filter) - question capacity heuristics
 * <p>
 * each task uses the $pri (intensity) to decide the effort applied in the load/save process.
 * by default full intensity would apply complete effort to a load and store operation which isnt necessarily instantaneous.  it could start and end in short or long ranges of time, or for example, a synchronization process
 * can be a ongoing background task that doesnt really end and yet it represents some kind of goal for
 * data transmission.
 * <p>
 * commands are resilient to repeated invocation although the effort could vary
 * as much as the user/NAR wants.
 * <p>
 * AtomicExec is designed for careful atomic invocation depending on belief/goal states as they
 * change from cycle to cycle.  when a goal concept with an associated AtomicExec operation
 * exceeds a threshold of desire and non-belief it invokes the operation.  when this desire/non-goal
 * threshold decreases or is replaced by either non-desire or sufficient belief (satisfied) then
 * the atomic operation is stopped.  https://github.com/automenta/narchy/blob/skynet5/nal/src/main/java/nars/util/AtomicExec.java#L31
 * ```
 */
public class Memory {

    final Logger logger = LoggerFactory.getLogger(Memory.class);

    final Multimap<String, BytesToTasks> readFormats = Multimaps.newListMultimap(new ConcurrentHashMap<>(), FasterList::new);
    final Multimap<String, TasksToBytes> writeFormats = Multimaps.newListMultimap(new ConcurrentHashMap<>(), FasterList::new);
    final List<MemoryResolver> resolvers = new CopyOnWriteArrayList();

    public Memory() {
        resolvers.add(URIResolver);
    }
    public Memory(NAR nar) {
        this();
        add(nar);
    }

    public Memory add(NAR n) {
        resolvers.add(new NARResolver(n));
        return this;
    }

    final MemoryResolver URIResolver = new MemoryResolver() {


        @Override
        public Stream<Supplier<Stream<Task>>> readers(Term x, Memory m) {
            Stream<URI> uri = termToURIs(x);
            if (uri != null) {
                return uri.map((URI u) -> {

                    String extension = extension(u);
                    if (extension != null) {
                        Collection<BytesToTasks> formats = m.readFormats.get(extension);
                        if (!formats.isEmpty()) {
                            return (Supplier<Stream<Task>>)()-> {
                                try {
                                    URL url = u.toURL();
                                    return read(url.openStream(), formats);
                                } catch (Exception e) {
                                    logger.warn("{} {}", u, e);
                                    return null; //Stream.empty();
                                }
                            };
                        }
                    }
                    return null;
                }).filter(Objects::nonNull);
            }
            return null;
        }

        @Override
        public Stream<Consumer<Stream<Task>>> writers(Term x) {
            return null;
        }
    };

    @Nullable static String extension(URI u) {
        //TODO real MIME Content-Type resolver
        ////Files.probeContentType()

        String path = u.getPath();
        int afterPeriod = path.lastIndexOf('.');
        if (afterPeriod==-1)
            return null;

        return path.substring(afterPeriod+1);
    }

    @Nullable
    static Stream<URI> termToURIs(Term x) {
        if (x.op() == ATOM) {
            String s = $.unquote(x);
            try {
                URI u = URI.create(s);
                return Stream.of(u);
            } catch (IllegalArgumentException e) {
                //..??
            }
        }

        //TODO compound URI patterns, allowing variables etc
        return null;
    }

    static Stream<Task> read(InputStream in, Collection<BytesToTasks> readFormats) throws IOException {
        if (readFormats.size() == 1) {
            return readFormats.iterator().next().apply(in);
        } else {
            //load once into byte buffer then create temporary ByteArrayInputStream over it
            byte[] b = in.readAllBytes();
            return readFormats.stream().flatMap(r -> r.apply(new ByteArrayInputStream(b)));
        }
    }

    public void on(BytesToTasks f) {
        for (String e : f.extensions)
            readFormats.put(e, f);
    }

    public void on(TasksToBytes f) {
        for (String e : f.extensions)
            writeFormats.put(e, f);
    }

    public Stream<Supplier<Stream<Task>>> readers(Term x) {
        return resolvers.stream().flatMap(r -> r.readers(x, Memory.this)).filter(Objects::nonNull);
    }

    public interface MemoryResolver {
        @Nullable
        Stream<Supplier<Stream<Task>>> readers(Term x, Memory m);

        @Nullable
        Stream<Consumer<Stream<Task>>> writers(Term x);
    }


    /** resolves memory by NAR's current Self term */
    private static class NARResolver implements MemoryResolver {
        private final NAR nar;

        public NARResolver(NAR n) {
            this.nar = n;
        }

        @Override
        public Stream<Supplier<Stream<Task>>> readers(Term x, Memory m) {
            if (nar.self().equals(x)) {
                return Stream.of(nar::tasks);
            }
            return null;
        }

        @Override
        public Stream<Consumer<Stream<Task>>> writers(Term x) {
            if (nar.self().equals(x)) {
                return Stream.of(nar::input);
            }
            return null;
        }
    }


    /**
     * registers the ability to translate a file format into NAL
     */
    abstract public static class BytesToTasks implements Function<InputStream, Stream<Task>> {
        private final String[] extensions;

        public BytesToTasks(String... extension) {
            this.extensions = extension;
        }
    }

    abstract public static class TasksToBytes implements Function<Stream<Task>, ByteArrayOutputStream> {
        private final String[] extensions;

        public TasksToBytes(String... extension) {
            this.extensions = extension;
        }
    }

}
