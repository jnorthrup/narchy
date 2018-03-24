package nars.index.term;

import com.google.common.base.Charsets;
import com.google.common.io.ByteArrayDataOutput;
import jcog.data.byt.HashCachedBytes;
import nars.IO;
import nars.Op;
import nars.Task;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.Charset;

import static nars.IO.SPECIAL_OP;
import static nars.IO.writeEvidence;

/**
 * TODO lazily compute
 */
public class TermKey extends HashCachedBytes {

    //public final static ThreadLocal<Lz4Compressor> compressor = ThreadLocal.withInitial(Lz4Compressor::new);
    //final static Lz4Decompressor decompressor = new Lz4Decompressor();

    private static final boolean COMPRESS = false;

    /**
     * term with volume byte prepended for sorting by volume
     */
    public static TermKey termByVolume(Term x) {
        TermKey y = new TermKey(x.volume() * 4 + 64 /* ESTIMATE */);
        try {

            //volume byte: pre-sorts everything by complexity or volume from the root, so that items of certain sizes can
            //be selected
            int c = x.volume();
            y.writeByte(c);

            writeTermSeq(y, x, false);

            //int before = length();
//            if (COMPRESS && c > 1 && y.compress(1)!=-1) {
//                //int after = length();
//                //System.out.println(conceptualizable + "\t" + before + " -> " + after + "\t" + new String(array()));
//            }

            //this.writeByte(0); //null terminator, signifying end-of-term
            y.compact();
            return y;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    protected TermKey(int len) {
        super(len);
    }

    public TermKey(Task task) {
        super(task.volume() * 4 + 64 /* ESTIMATE */);
        try {

            task.term().append((ByteArrayDataOutput)this);

            byte punc = task.punc();
            this.writeByte(punc);

            writeLong(task.start());

            if ((punc == Op.BELIEF) || (punc == Op.GOAL)) {
                writeInt(task.truth().hashCode());
            }

            writeEvidence(this, task.stamp());

            if (COMPRESS)
                compress();

            compact();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    @Override
    public boolean equals(Object obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    static void writeTermSeq(DataOutput out, Term term, boolean includeTemporal) throws IOException {


        if (term instanceof Atomic) {
            if (IO.isSpecial(term)) {
                out.writeByte(SPECIAL_OP);
            }
            //out.writeUTF(term.toString());
            //IO.writeUTFWithoutLength(out, term.toString());

            writeStringBytes(out, term);

        } else {
            writeCompoundSeq(out, (Compound) term, includeTemporal);
        }
    }

    static final Charset utf8 = Charsets.UTF_8;

    public static byte[] bytes(String s) {
        return s.getBytes(utf8); //SAFE access:
    }

    static void writeStringBytes(DataOutput out, Object o) throws IOException {
        out.write(bytes(o.toString()));
    }

//    static void writeStringBytes(DataOutput out, String s) throws IOException {
//        out.write(bytes(s));
//    }


    static void writeCompoundSeq(DataOutput out, Compound c, boolean includeTemporal) throws IOException {

        out.writeByte('(');
        writeTermContainerSeq(out, c.subterms(), includeTemporal);
        out.writeByte(')');

        /*@NotNull*/ Op o = c.op();
        out.writeByte(o.id); //put operator last
        if (includeTemporal && o.temporal) {
            out.writeInt(c.dt());
        }

    }


    static void writeTermContainerSeq(DataOutput out, Subterms c, boolean includeTemporal) throws IOException {

        int siz = c.subs();
        for (int i = 0; i < siz; i++) {
            writeTermSeq(out, c.sub(i), includeTemporal);
            if (i < siz - 1)
                out.writeByte(',');
        }

    }


}
