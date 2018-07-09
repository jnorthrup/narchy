package nars.util.term;

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
public class TermBytes extends HashCachedBytes {

    
    

    private static final boolean COMPRESS = false;

    /**
     * term with volume byte prepended for sorting by volume
     */
    public static TermBytes termByVolume(Term x) {
        TermBytes y = new TermBytes(x.volume() * 4 + 64 /* ESTIMATE */);
        try {

            
            
            int c = x.volume();
            y.writeByte(c);

            writeTermSeq(y, x, false);

            





            
            y.compact();
            return y;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private TermBytes(int len) {
        super(len);
    }

    public TermBytes(Task task) {
        super(task.volume() * 4 + 64 /* ESTIMATE */);
        try {

            task.term().appendTo((ByteArrayDataOutput)this);

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

    private static void writeTermSeq(DataOutput out, Term term, boolean includeTemporal) throws IOException {


        if (term instanceof Atomic) {
            if (IO.isSpecial(term)) {
                out.writeByte(SPECIAL_OP);
            }
            
            

            writeStringBytes(out, term);

        } else {
            writeCompoundSeq(out, (Compound) term, includeTemporal);
        }
    }

    private static final Charset utf8 = Charsets.UTF_8;

    private static byte[] bytes(String s) {
        return s.getBytes(utf8); 
    }

    private static void writeStringBytes(DataOutput out, Object o) throws IOException {
        out.write(bytes(o.toString()));
    }






    private static void writeCompoundSeq(DataOutput out, Compound c, boolean includeTemporal) throws IOException {

        out.writeByte('(');
        writeTermContainerSeq(out, c.subterms(), includeTemporal);
        out.writeByte(')');

        /*@NotNull*/ Op o = c.op();
        out.writeByte(o.id); 
        if (includeTemporal && o.temporal) {
            out.writeInt(c.dt());
        }

    }


    private static void writeTermContainerSeq(DataOutput out, Subterms c, boolean includeTemporal) throws IOException {

        int siz = c.subs();
        for (int i = 0; i < siz; i++) {
            writeTermSeq(out, c.sub(i), includeTemporal);
            if (i < siz - 1)
                out.writeByte(',');
        }

    }


}
