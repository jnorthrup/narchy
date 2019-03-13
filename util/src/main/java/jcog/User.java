package jcog;

import com.fasterxml.jackson.core.JsonProcessingException;
import jcog.event.ListTopic;
import jcog.event.Off;
import jcog.event.Topic;
import jcog.exe.Exe;
import jcog.math.Longerval;
import jcog.tree.rtree.rect.HyperRectDouble;
import jcog.tree.rtree.rect.HyperRectFloat;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NRTCachingDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;

/**
 * general user-scope global utilities and facilities
 */
public class User {

    /** unique identifier */
    static final String ID = "i";

    /** how is stored */
    static final String CODEC = "c";

    /** spatiotemporal bounds */
    static final String BOUNDS = "b";

    /** a tag field (multiple allowed) */
    static final String TAG = "t";


    private static User user = null;

    /**
     * general purpose user notification broadcast
     */
    public final Topic<Object> notice = new ListTopic();

    private static final Logger logger = LoggerFactory.getLogger(User.class);

    private final Directory d;

    private IndexWriter iw;
    

    public synchronized static User the() {
        if (user == null)
            user = new User(Paths.get(System.getProperty("user.home")).resolve(".me"));
        return user;
    }

    /**
     * temporary in-memory user
     */
    public User() {

        RAMDirectory base = new RAMDirectory();
        d = nrt(base);

        init();
    }

    protected User(Path dir) {
        

        try {
            if (!dir.toFile().exists()) {
                logger.warn("create {}", dir);
                Files.createDirectory(dir);
            } else {
                logger.warn("load {}", dir);
            }

            d = nrt(FSDirectory.open(Paths.get(dir.toAbsolutePath().toString())));


            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    logger.warn("save {}", dir);
                    d.close();
                } catch (IOException e) {
                    throw new Error(e);
                }
            }));
        } catch (Exception e) {
            throw new Error(e);
        }

        init();
    }

    /**
     * http:
     * @param base
     * @return
     */
    private static NRTCachingDirectory nrt(Directory base) {
        
        
        return new NRTCachingDirectory(base, 5.0, 60.0);
    }



    private void init() {


        IndexWriterConfig iwc = new IndexWriterConfig();
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        iwc.setCommitOnClose(true);
        

        try {
            iw = new IndexWriter(d, iwc);
            
        } catch (IOException e) {
            throw new Error(e);
        }



















    }

    public void notice(Object x) {
        logger.info("-> {}", x);
        notice.emitAsync(x);
    }

    public Off onNotice(Consumer x) {
        logger.info("noticing {}", x);
        return notice.onWeak(x);
    }

    public void whileEach(Predicate<Document> d) {
        read((r) -> {
            int n = r.numDocs();
            for (int i = 0; i < n; i++) {
                try {
                    if (!d.test(r.document(i)))
                        break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void put(String id, Object x) {
        Document d = document(id, x);
        if (d != null) {
            logger.debug("put {} {}", id, x);
            write((iw) -> {
                try {
                    
                    
                    iw.updateDocument(id(id), d);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            });
        } else {
            throw new RuntimeException("null document for " + x);
        }
    }


    public <X> void get(String id, Consumer<X> yy) {
        logger.debug("get {}", id);
        final Document[] D = new Document[1];
        search((iis) -> {
            try {
                TopDocs y = iis.search(
                        new TermQuery(id(id)), 1);
                if (y.totalHits.value > 0)
                    D[0] = iis.doc(y.scoreDocs[0].doc); 

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        if (D[0] != null) { 
            yy.accept(undocument(D[0]));
        }
    }

    public void run(Runnable r) {
        Exe.executor().execute(r);
    }

    /** view for a search result document, w/ score and method to decode to lazily object.
     * it will be changesd on each iterated result so don't keep it.
     * caches the generated document to a field while it's still visiting it.
     *
     * maybe use: https:
     * */
    public final class DocObj {

        private final IndexReader reader;
        private int doc;
        private float score;
        private Document _doc;

        private DocObj(IndexReader reader) {
            this.reader = reader;
        }

        private DocObj update(int doc, float score) {
            this._doc = null;
            this.doc = doc; this.score = score;
            return this;
        }

        public float score() {
            return score;
        }

        @Override
        public String toString() {
            return score() + " " + doc();
        }

        public Document doc() {

            if (this._doc!=null)
                return _doc;

            try {
                return this._doc = reader.document(doc);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        public Object obj() {
            return undocument(doc());
        }

        private void clear() {
            doc = -1;
            score = Float.NaN;
            _doc = null;
        }
    }


    public void get(String query, int n, Predicate<DocObj> yy) {
        
        
        get(new FuzzyQuery(new Term("i", query)), n, yy);
    }

    public final void get(Query q, int n, Predicate<DocObj> yy) {
        get(q, n, null, yy);
    }

    public void get(Query q, int n, @Nullable FieldDoc after, Predicate<DocObj> yy) {
        logger.debug("query {}", q);
        search((iis) -> {
            try {
                TopDocs y = iis.searchAfter(after, q, n);
                if (y.totalHits.value > 0) {
                    DocObj d = new DocObj(iis.getIndexReader());
                    for (ScoreDoc sd : y.scoreDocs) {
                        if (!yy.test(d.update(sd.doc, sd.score)))
                            break;
                    }
                    d.clear();
                }
            } catch (IOException e) {
                logger.error("search {}", q, e);
            }
        });
    }

    public <X> void get(String id, Supplier<X> ifAbsent, Consumer<X> with) {
        logger.debug("get {}", id);
        final Document[] D = new Document[1];
        search((is) -> {

            try {
                TopDocs y = is.search(new TermQuery(id(id)), 1);
                if (y.totalHits.value > 0)
                    D[0] = is.doc(y.scoreDocs[0].doc);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        if (D[0] != null) { 
            with.accept(undocument(D[0]));
        } else {
            X a = ifAbsent.get();
            if (a != null) {
                put(id, a);
                with.accept(a);
            }
        }
    }

    private static Term id(String id) {
        return new Term("i", id);
    }


    private void write(Consumer<IndexWriter> with) {



        try {
            with.accept(iw);
            iw.commit();

            

        } catch (IOException e) {
            e.printStackTrace();
        }

        



    }

    private void read(Consumer<IndexReader> with) {
        try {

            DirectoryReader r = DirectoryReader.open(iw);

            with.accept(r);

            r.close();

        } catch (IOException e) {
            logger.error("read", e);
        }
    }

    private void search(Consumer<IndexSearcher> with) {
        read(ir -> {


            try {
                DirectoryReader r = DirectoryReader.open(iw);
                IndexSearcher s = new IndexSearcher(r);
                with.accept(s);
                r.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
    }

    public <X> X undocument(Document doc) {
        String codec = doc.get("c");
        return (X) codecs.get(codec).unapply(doc);
    }


    private Document document(String id, Object x) {

        Document d = new Document();
        d.add(new StringField(ID, id, Field.Store.YES));

        String codec = codec(x);
        d.add(new StringField(CODEC, codec, Field.Store.YES));
        codecs.get(codec).apply(d, x);
        return d;
    }

    private String codec(Object input) {

        String c;
        switch (input.getClass().getSimpleName()) {
            case "byte[]":
                c = "blob";
                break;
            case "String":
                c = "string";
                break;
            default:
                c = "msgpack";
                break;
        }
        return c;
    }


    public interface DocCodec<X> {
        void apply(Document d, X x);

        X unapply(Document doc);
    }

    private final Map<String, DocCodec> codecs = Map.of(
            "string", new DocCodec<String>() {

                @Override
                public void apply(Document d, String x) {
                    d.add(new StringField("string", x, Field.Store.YES));
                }

                @Override
                public String unapply(Document doc) {
                    return doc.get("string");
                }
            },
            "blob", new DocCodec<byte[]>() {
                @Override
                public void apply(Document d, byte[] bytes) {
                    d.add(new StoredField("blob", new BytesRef(bytes)));
                }

                @Override
                public byte[] unapply(Document doc) {
                    BytesRef bytes = doc.getBinaryValue("blob");
                    return bytes.bytes;
                }
                /* byte[] */
            },
            "msgpack", new DocCodec<Object>() {
                @Override
                public void apply(Document d, Object o) {
                    try {
                        d.add(new StoredField("msgpack", new BytesRef(Util.toBytes(o))));
                        d.add(new StringField("javatype",
                                o.getClass().getName(),
                                Field.Store.YES));

                        if (o instanceof HyperRectFloat) {
                            HyperRectFloat r = (HyperRectFloat)o;
                            if (r.dim() == 4) {
                                double[] min = Util.toDouble(r.min.data);
                                double[] max = Util.toDouble(r.max.data);
                                d.add(new DoubleRange(BOUNDS, min, max));
                            }
                        } else if (o instanceof HyperRectDouble) {
                            HyperRectDouble r = (HyperRectDouble)o;
                            if (r.dim() == 4) {
                                double[] min = (r.min.coord);
                                double[] max = (r.max.coord);
                                d.add(new DoubleRange(BOUNDS, min, max));
                            }
                        } else if (o instanceof Longerval) {
                            
                            Longerval l = (Longerval)o;
                            double[] min = { l.a, NEGATIVE_INFINITY, NEGATIVE_INFINITY, NEGATIVE_INFINITY };
                            double[] max = { l.b, POSITIVE_INFINITY, POSITIVE_INFINITY, POSITIVE_INFINITY };
                            d.add(new DoubleRange(BOUNDS, min, max));
                        }


                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public Object unapply(Document doc) {
                    BytesRef bytes = doc.getBinaryValue("msgpack");
                    String javatype = doc.get("javatype");
                    try {
                        return Util.fromBytes(bytes.bytes, Class.forName(javatype));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                /** msgpack */

            }

    );


}
