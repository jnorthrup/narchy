package jcog;

import com.fasterxml.jackson.core.JsonProcessingException;
import jcog.event.ListTopic;
import jcog.event.On;
import jcog.event.Topic;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * general user-scope global utilities and facilities
 */
public class User {

    private static User user = null;
    private final ExecutorService exe = ForkJoinPool.commonPool();

    /**
     * general purpose user notification broadcast
     */
    public final Topic<Object> notice = new ListTopic();

    static final Logger logger = LoggerFactory.getLogger(User.class);

    private final Directory d;

    private IndexWriter iw;
    final StandardAnalyzer analyzer = new StandardAnalyzer();

    public synchronized static User the() {
        if (user == null)
            user = new User(Paths.get(System.getProperty("user.home")).resolve(".me"));
        return user;
    }

    /**
     * temporary in-memory user
     */
    public User() {

        d = new RAMDirectory();

        init();
    }

    protected User(Path dir) {
        //System.getProperties().forEach((x, y) -> System.out.println(x + " " + y));

        try {
            if (!dir.toFile().exists()) {
                logger.warn("create {}", dir);
                Files.createDirectory(dir);
            } else {
                logger.warn("load {}", dir);
            }

            d = FSDirectory.open(Paths.get(dir.toAbsolutePath().toString()));


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

    private void init() {


        IndexWriterConfig iwc = new IndexWriterConfig();
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        iwc.setCommitOnClose(true);
        //iwc.setReaderPooling(true);

        try {
            iw = new IndexWriter(d, iwc);
            //ir = DirectoryReader.open(iw);
        } catch (IOException e) {
            throw new Error(e);
        }


//        read((ir) -> {
//            int m = ir.maxDoc();
//            for (int i = 0; i < m; i++) {
//                try {
//                    List<IndexableField> ff = ir.document(i).getFields();
//                    ff.forEach(g -> {
//                        if (g.name().equals("i")) {
//                            System.out.println(g);
//                        } else {
//                            System.out.println(g.name() + " " + g.fieldType());
//                        }
//                    });
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
    }

    public void notice(Object x) {
        logger.info("-> {}", x);
        notice.emitAsync(x, exe);
    }

    public On onNotice(Consumer x) {
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
                    //iw.deleteDocuments(id(id));
                    //iw.addDocument(d);
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
                if (y.totalHits > 0)
                    D[0] = iis.doc(y.scoreDocs[0].doc);

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        if (D[0] != null) { //outside of any critical section
            yy.accept(undocument(D[0]));
        }
    }

    public void run(Runnable r) {
        exe.submit(r);
    }

    /** view for a search result document, w/ score and method to decode to lazily object.
     * it will be changesd on each iterated result so don't keep it.
     * caches the generated document to a field while it's still visiting it.
     * */
    public final class DocObj {

        private final IndexReader reader;
        private int doc;
        private float score;
        private Document _doc;

        public DocObj(IndexReader reader) {
            this.reader = reader;
        }

        DocObj update(int doc, float score) {
            this.doc = doc; this.score = score;
            this._doc = null;
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

        public void clear() {
            doc = -1;
            score = Float.NaN;
            _doc = null;
        }
    }


    public <X> void getAll(String query, Predicate<DocObj> yy) {
        logger.debug("getAll {}", query);
        Query q = new QueryBuilder(analyzer).createPhraseQuery("i", query);
        search((iis) -> {
            try {
                TopDocs y = iis.search(q, 16);
                if (y.totalHits > 0) {
                    DocObj d = new DocObj(iis.getIndexReader());
                    for (ScoreDoc sd : y.scoreDocs) {
                        if (!yy.test(d.update(sd.doc, sd.score)))
                            break;
                    }
                    d.clear();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public <X> void get(String id, Supplier<X> ifAbsent, Consumer<X> with) {
        logger.debug("get {}", id);
        final Document[] D = new Document[1];
        search((is) -> {

            try {
                TopDocs y = is.search(new TermQuery(id(id)), 1);
                if (y.totalHits > 0)
                    D[0] = is.doc(y.scoreDocs[0].doc);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        if (D[0] != null) { //outside of any critical section
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
//        try {


        try {
            with.accept(iw);
            iw.commit();

            //iw.forceMergeDeletes(true);

        } catch (IOException e) {
            e.printStackTrace();
        }

        //iw.close(); //commits and closes
//        } catch (IOException e) {
//            logger.error("write", e);
//        }
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

    <X> X undocument(Document doc) {
        String codec = doc.get("c");
        return (X) codecs.get(codec).unapply(doc);
    }

    Document document(String id, Object x) {
        String codec = codec(x);

        Document d = new Document();
        d.add(new StringField("i", id, Field.Store.YES));
        d.add(new StringField("c", codec, Field.Store.YES));
        codecs.get(codec).apply(d, x);
        return d;
    }

    String codec(Object input) {

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

    public final Map<String, DocCodec> codecs = Map.of(
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
