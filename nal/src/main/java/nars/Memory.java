package nars;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Streams;
import jcog.TODO;
import jcog.data.byt.DynBytes;
import jcog.data.list.FasterList;
import jcog.io.bzip2.BZip2InputStream;
import jcog.io.bzip2.BZip2OutputStream;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
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
 * the atomic operation is stopped.  https:
 * ```
 */
public class Memory {


    final static Atomic stdin = Atomic.the("stdin");
    final static Atomic stdout = Atomic.the("stdout");


    /**
     * text
     */
    static final TasksToBytes Tasks_To_Text = new TasksToBytes("nal") {
        @Override
        public void accept(Stream<Task> tt, OutputStream out) {
            tt.forEach(t -> {
                try {
                    out.write(t.toString(false).toString().getBytes());
                    out.write('\n');
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    };

    /**
     * binary, uncompressed
     */
    static final TasksToBytes Tasks_To_Binary = new TasksToBytes("nalb") {
        @Override
        public void accept(Stream<Task> tt, OutputStream out) {
            tt.forEach(t -> {
                try {
                    out.write(IO.taskToBytes(t));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    };

    /**
     * bzip2 compressed
     * note: doesnt write the BZ 2 byte header which is standard identifying prefix for bzip2 files
     */
    static final TasksToBytes Tasks_To_BinaryZipped = new TasksToBytes("nalz") {
        @Override
        public void accept(Stream<Task> tt, OutputStream out) {
            try (OutputStream o = new BZip2OutputStream(out)) {
                DynBytes d = new DynBytes(256);
                tt.forEach(t -> {
                    try {
                        IO.bytes(t, d).appendTo(o);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (ArithmeticException | IOException f) {
                f.printStackTrace();
            }
        }
    };
    final Logger logger = LoggerFactory.getLogger(Memory.class);
    final Multimap<String, BytesToTasks> readFormats = Multimaps.newListMultimap(new ConcurrentHashMap<>(), FasterList::new);
    final Multimap<String, TasksToBytes> writeFormats = Multimaps.newListMultimap(new ConcurrentHashMap<>(), FasterList::new);
    final List<MemoryResolver> resolvers = new CopyOnWriteArrayList();
    final MemoryResolver StdIOResolver = new MemoryResolver() {

        @Override
        public Stream<Supplier<Stream<Task>>> readers(Term x, Memory m) {
            if (x.equals(stdin)) {
                throw new TODO();
            }
            return null;
        }

        @Override
        public Stream<Consumer<Stream<Task>>> writers(Term x, Memory m) {
            if (x.equals(stdout)) {
                return Stream.of((t) -> t.forEach(System.out::println
                ));
            }
            return null;
        }
    };
    final MemoryResolver URIResolver = new MemoryResolver() {

        final List<Term> ROOTS = List.of(
                Atomic.the("file:///"),
                Atomic.the("http://github.com/automenta/narchy"));

        @Override
        public Stream<Term> roots(Memory m) {
            return ROOTS.stream();
        }

        @Override
        public Stream<Supplier<Stream<Task>>> readers(Term x, Memory m) {
            Stream<URI> uri = termToURIs(x);
            if (uri != null) {
                return uri.map((URI u) -> {

                    String extension = extension(u);
                    if (extension != null) {
                        Collection<BytesToTasks> formats = m.readFormats.get(extension);
                        if (!formats.isEmpty()) {
                            return (Supplier<Stream<Task>>) () -> {
                                try {
                                    URL url = u.toURL();
                                    return read(url.openStream(), formats);
                                } catch (Exception e) {
                                    logger.warn("{} {}", u, e);
                                    return null;
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
        public Stream<Consumer<Stream<Task>>> writers(Term x, Memory m) {
            Stream<URI> uri = termToURIs(x);
            if (uri != null) {
                return uri.map((u -> {

                    String extension = extension(u);
                    if (extension != null) {
                        Collection<TasksToBytes> formats = m.writeFormats.get(extension);
                        if (!formats.isEmpty()) {
                            if (formats.size() > 1)
                                logger.warn("multiple output formats, choosing first: {}", formats);

                            return (Stream<Task> tt) -> {
                                try (OutputStream out = Files.newOutputStream(Paths.get(u))) {
                                    formats.iterator().next().accept(tt, out);
                                } catch (IOException e) {
                                    logger.warn("{} {}", u, e);
                                }

                            };
                        }
                    }
                    return null;
                }));
            } else
                return null;
        }

        @Override
        public Stream<Term> contents(Term x, Memory m) {
            Stream<URI> uri = termToURIs(x);
            if (uri != null) {
                //noinspection RedundantCast
                return uri.map((URI u)->Paths.get((URI)u)).filter(p -> Files.isDirectory(p)).flatMap(p -> {
                    try {
                        return Files.list(p).map(pp -> uriToTerm(pp.toUri()));
                    } catch (IOException e) {
                        logger.warn("{} {}", p, e);
                        return Stream.empty();
                    }
                });
            }
            return null;
        }
    };
    /**
     * bzip2 compressed
     * note: doesnt write the 'BZ' 2 byte header which is standard identifying prefix for bzip2 files
     */
    final BytesToTasks BinaryZipped_To_Tasks = new BytesToTasks("nalz") {


        @Override
        public Stream<Task> apply(InputStream ii) {
            return Streams.stream(() -> new Iterator<Task>() {

                final DataInputStream i = new DataInputStream(new BZip2InputStream(ii));
                Task next = null;


                @Override
                public boolean hasNext() {
                    try {
                        next = IO.readTask(i);
                        return true;
                    } catch (EOFException f) {
                        next = null;
                        return false;
                    } catch (IOException e) {
                        next = null;
                        logger.warn("{} {}", ii, e);
                        return false;
                    }
                }

                @Override
                public Task next() {
                    return next;
                }
            });
        }
    };

    public Memory() {
        resolvers.add(URIResolver);
        resolvers.add(StdIOResolver);
        on(Tasks_To_Text);
        on(Tasks_To_Binary);
        on(Tasks_To_BinaryZipped);
        on(BinaryZipped_To_Tasks);
    }

    public Memory(NAR nar) {
        this();
        add(nar);
    }

    @Nullable
    static String extension(URI u) {


        String path = u.getPath();
        int afterPeriod = path.lastIndexOf('.');
        if (afterPeriod == -1)
            return null;

        return path.substring(afterPeriod + 1);
    }

    static Term uriToTerm(URI x) {
        return $.quote(x.toString());
    }

    @Nullable
    static Stream<URI> termToURIs(Term x) {
        if (x.op() == ATOM) {
            String s = $.unquote(x);
            try {
                URI u = URI.create(s);
                return Stream.of(u);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }


        return null;
    }

    static Stream<Task> read(InputStream in, Collection<BytesToTasks> readFormats) throws IOException {
        if (readFormats.size() == 1) {
            return readFormats.iterator().next().apply(in);
        } else {

            byte[] b = in.readAllBytes();
            return readFormats.stream().flatMap(r -> r.apply(new ByteArrayInputStream(b)));
        }
    }

    public Stream<Term> contents(String address) {
        return contents(Atomic.the(address));
    }

    public Stream<Term> contents(Term address) {
        return resolvers.stream().flatMap(r -> r.contents(address, Memory.this)).filter(Objects::nonNull).distinct();
    }

    public Stream<Term> roots() {
        return resolvers.stream().flatMap(r -> r.roots(Memory.this)).filter(Objects::nonNull).distinct();
    }

    public Memory add(NAR n) {
        resolvers.add(new NARResolver(n));
        return this;
    }

    public void on(BytesToTasks f) {
        for (String e: f.extensions)
            readFormats.put(e, f);
    }

    public void on(TasksToBytes f) {
        for (String e: f.extensions)
            writeFormats.put(e, f);
    }

    public Stream<Supplier<Stream<Task>>> readers(Term x) {
        return resolvers.stream().flatMap(r -> r.readers(x, Memory.this)).filter(Objects::nonNull);
    }

    public Stream<Consumer<Stream<Task>>> writers(Term x) {
        return resolvers.stream().flatMap(r -> r.writers(x, Memory.this)).filter(Objects::nonNull);
    }

    @Nullable
    public Runnable copy(Term i, Term o) {
        return copy(i, o, null);
    }

    /**
     * returns a runnable copy procedure for the resolved readers and writers
     */
    @Nullable
    public Runnable copy(Term i, Term o, @Nullable Function<Stream<Task>, Stream<Task>> filter) {
        Set<Supplier<Stream<Task>>> readers = readers(i).collect(toSet());
        if (!readers.isEmpty()) {
            Set<Consumer<Stream<Task>>> writers = writers(o).collect(toSet());
            if (!writers.isEmpty()) {
                return () -> {

                    /** read input */
                    Stream<Task> fromIn = readers.stream().flatMap(Supplier::get);

                    /** filter (intermediate) */
                    Stream<Task> toOut = (filter != null ? filter.apply(fromIn) : fromIn).distinct();

                    /** write output */
                    if (writers.size() == 1) {
                        writers.iterator().next().accept(toOut);
                    } else {

                        List<Task> outs = toOut.collect(toList());

                        writers.forEach(w -> w.accept(outs.stream()));
                    }
                };
            }
        }

        return null;
    }

    public interface MemoryResolver {
        @Nullable
        Stream<Supplier<Stream<Task>>> readers(Term x, Memory m);

        @Nullable
        Stream<Consumer<Stream<Task>>> writers(Term x, Memory m);

        /**
         * lists the contents, ie. if it is a directory / container.  items
         * returned may be resolvable by this or another resolver.
         */
        default Stream<Term> contents(Term x, Memory m) {
            return Stream.empty();
        }


        /**
         * entry points into memory.  none by default
         */
        default Stream<Term> roots(Memory m) {
            return Stream.empty();
        }
    }

    /**
     * resolves memory by NAR's current Self term
     */
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
        public Stream<Consumer<Stream<Task>>> writers(Term x, Memory m) {
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

    abstract public static class TasksToBytes implements BiConsumer<Stream<Task>, OutputStream> {
        private final String[] extensions;

        public TasksToBytes(String... extension) {
            this.extensions = extension;
        }
    }

    /*
    TODO lucene user resolver:

                nar.runLater(() -> {
                User.the().get(id.toString(), (byte[] x) -> {
                    try {
                        nn.inputBinary(new ByteArrayInputStream(x));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    nn.logger.info("loaded {}", id);
                });
            });

     */
}
