package jcog;

import jcog.event.ListTopic;
import jcog.event.On;
import jcog.event.Topic;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * general user-scope global utilities and facilities
 */
public class User {

    private static User user = null;
    private final DocCodec docCodec = new DefaultDocCodec();
    private final Executor exe = ForkJoinPool.commonPool();

    /**
     * general purpose user notification broadcast
     */
    public final Topic<Object> notice = new ListTopic();

    static final Logger logger = LoggerFactory.getLogger(User.class);

    private final Directory d;

    public synchronized static User the() {
        if (user == null)
            user = new User();
        return user;
    }

    private User() {
        //System.getProperties().forEach((x, y) -> System.out.println(x + " " + y));

        Path dir = Paths.get(System.getProperty("user.home")).resolve(".me");
        try {
            if (!dir.toFile().exists()) {
                logger.warn("create {}", dir);
                Files.createDirectory(dir);
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
                    iw.updateDocument(id(id), d);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }


    public <X> void get(String id, Consumer<X> yy) {
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
            yy.accept(undocument(D[0]));
        }
    }


    private static Term id(String id) {
        return new Term("i", id);
    }


    private void write(Consumer<IndexWriter> with) {
        try {
            IndexWriterConfig iwc = new IndexWriterConfig();
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

            IndexWriter iw = new IndexWriter(d, iwc);
            with.accept(iw);
            iw.close(); //commits and closes
        } catch (IOException e) {
            logger.error("write", e);
        }
    }

    private void read(Consumer<IndexReader> with) {
        try {
            DirectoryReader ir = DirectoryReader.open(d);
            with.accept(ir);
            ir.close(); //commits and closes
        } catch (IOException e) {
            logger.error("read", e);
        }
    }

    private void search(Consumer<IndexSearcher> with) {
        read(ir -> {
            IndexSearcher s = new IndexSearcher(ir);
            with.accept(s);
        });
    }

    private <X> X undocument(Document doc) {
        return docCodec.unapply(doc);
    }

    private Document document(String id, Object x) {
        return docCodec.apply(id, x);
    }

    public interface DocCodec {
        Document apply(String id, Object x);

        <X> X unapply(Document doc);
    }

    public static class DefaultDocCodec implements DocCodec {

        static final Logger logger = LoggerFactory.getLogger(DefaultDocCodec.class);

        @Override
        public <X> X unapply(Document doc) {
            BytesRef bytes = doc.getBinaryValue("byte[]");
            if (bytes != null) {
                return (X) bytes.bytes;
            }
            String s = doc.get("string");
            if (s != null) {
                return (X) s;
            }

            logger.error("null {}", doc);
            return null;
        }

        @Override
        public Document apply(String id, Object x) {
            Document d = new Document();

            d.add(new TextField("i", id, Field.Store.YES));

            if (x instanceof String) {
                d.add(new TextField("string", ((String) x), Field.Store.YES));
            } else if (x instanceof byte[]) {
                d.add(new StoredField("byte[]", new BytesRef((byte[]) x)));
            } else {
                logger.error("unsupported type {}", x.getClass());
                return null;
                //throw new UnsupportedOperationException();
            }

            return d;
        }
    }


}
