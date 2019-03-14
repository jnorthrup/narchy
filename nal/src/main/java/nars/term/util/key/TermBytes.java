package nars.term.util.key;

import jcog.data.byt.HashCachedBytes;
import nars.Op;
import nars.Task;
import nars.io.TermIO;
import nars.term.Term;

import java.io.IOException;

import static nars.io.IO.writeEvidence;

/**
 * TODO lazily compute
 */
public class TermBytes extends HashCachedBytes {




    /**
     * target with volume byte prepended for sorting by volume
     */
    public static TermBytes termByVolume(Term x) {
        int vol = x.volume();

        TermBytes y = new TermBytes(vol * 4 + 64 /* ESTIMATE */);

        y.writeByte(vol);

        TermIO.the.write(x, y);

        y.compact();

        return y;
    }


    private TermBytes(int len) {
        super(len);
    }

    public TermBytes(Task task) {
        super(task.volume() * 4 + 64 /* ESTIMATE */);
        try {

            TermIO.the.write(task.term(), this);

            byte punc = task.punc();
            this.writeByte(punc);

            writeLong(task.start());

            if ((punc == Op.BELIEF) || (punc == Op.GOAL)) {
                writeInt(task.truth().hashCode());
            }


            writeEvidence(this, task.stamp());

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

//    private static void writeTermSeq(DataOutput out, Term target, boolean includeTemporal) throws IOException {
//
//
//        if (target instanceof Atomic) {
//            if (IO.isSpecial(target)) {
//                out.writeByte(SPECIAL_OP);
//            }
//
//
//
//            writeStringBytes(out, target);
//
//        } else {
//            writeCompoundSeq(out, (Compound) target, includeTemporal);
//        }
//    }

//    private static final Charset utf8 = Charsets.UTF_8;

//    private static byte[] bytes(String s) {
//        return s.getBytes(utf8);
//    }

//    private static void writeStringBytes(DataOutput out, Object o) throws IOException {
//        out.write(bytes(o.toString()));
//    }


//    private static void writeCompoundSeq(DataOutput out, Compound c, boolean includeTemporal) throws IOException {
//
//        out.writeByte('(');
//        writeTermContainerSeq(out, c.subterms(), includeTemporal);
//        out.writeByte(')');
//
//        /*@NotNull*/
//        Op o = c.op();
//        out.writeByte(o.id);
//        if (includeTemporal && o.temporal) {
//            out.writeInt(c.dt());
//        }
//
//    }


//    private static void writeTermContainerSeq(DataOutput out, Subterms c, boolean includeTemporal) throws IOException {
//
//        int siz = c.subs();
//        for (int i = 0; i < siz; i++) {
//            writeTermSeq(out, c.sub(i), includeTemporal);
//            if (i < siz - 1)
//                out.writeByte(',');
//        }
//
//    }


}
