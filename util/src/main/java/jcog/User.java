package jcog;

import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.compressing.Compressor;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * general user-scope global utilities and facilities
 */
public class User {

    private static User user = null;

    private Directory d;


    public synchronized static User the() {
        if (user == null)
            user = new User();
        return user;
    }

    private User() {
        //System.getProperties().forEach((x, y) -> System.out.println(x + " " + y));

        Path dir = Paths.get(System.getProperty("user.home")).resolve(".me");
        try {
            if (!dir.toFile().exists())
                Files.createDirectory(dir);

            d = FSDirectory.open(Paths.get(dir.toAbsolutePath() + "/me"));


            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    d.close();
                } catch (IOException e) {
                    throw new Error(e);
                }
            }));
        } catch (Exception e) {
            throw new Error(e);
        }


    }


    void writer(Consumer<IndexWriter> with) {
        try {
            IndexWriterConfig iwc = new IndexWriterConfig();
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            IndexWriter iw = new IndexWriter(d, iwc);
            with.accept(iw);
            iw.close(); //commits and closes
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void reader(Consumer<IndexReader> with) {
        try {
            DirectoryReader ir = DirectoryReader.open(d);
            with.accept(ir);
            ir.close(); //commits and closes
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void forEach(Consumer<Document> d) {
        reader((r)->{
            int n = r.numDocs();
            for (int i = 0; i < n; i++) {
                try {
                    d.accept(r.document(i));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    void search(Consumer<IndexSearcher> with) {
        reader(ir -> {
           IndexSearcher s = new IndexSearcher(ir);
           with.accept(s);
        });
    }

    static final Logger logger = LoggerFactory.getLogger(User.class);

    public void put(String id, Object x) {
        writer((iw) -> {
            logger.info(id);
            Document d = document(id, x);
            try {
                iw.addDocument(d);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public <X> void get(String id, Consumer<X> yy) {
        search((is) -> {

            try {
                //Analyzer analyzer = new StandardAnalyzer();


                TopDocs y = is.search(new TermQuery(new Term("i", id)), 1);
                if (y.totalHits>0) {
                    yy.accept(undocument(is.doc(y.scoreDocs[0].doc)));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private <X> X undocument(Document doc) {
        BytesRef bytes = doc.getBinaryValue("byte[]");
        if (bytes!=null) {
            return (X) bytes.bytes;
        }
        String s = doc.get("string");
        if (s!=null) {
            return (X)s;
        }

        return null;
    }

    private Document document(String id, Object x) {
        Document d = new Document();

        d.add(new TextField("i", id, Field.Store.YES));

        if (x instanceof String) {
            d.add(new TextField("string", ((String) x), Field.Store.YES));
        } else if (x instanceof byte[]) {
            d.add(new StoredField("byte[]", new BytesRef((byte[]) x)));
        } else {
            throw new UnsupportedOperationException();
        }

        return d;
    }

}
