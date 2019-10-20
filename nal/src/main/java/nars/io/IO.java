package nars.io;


import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import jcog.data.byt.DynBytes;
import jcog.data.byt.RecycledDynBytes;
import jcog.io.BytesInput;
import jcog.pri.Prioritized;
import nars.Op;
import nars.Task;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

/**
 * Created by me on 5/29/16.
 * <p>
 * a target's op is only worth encoding in more than a byte since there arent more than 10..20 base operator types. that leaves plenty else for other kinds of control codes that btw probably can fit within the (invisible) ascii control codes. i use basically 1 byte for the op, then depending on this if the target is atomic or compound, the atom decoding reads UTF-8 byte[] directly as the atom's key. compound ops are followed by a byte for # of subterms that will follow, forming its subterm vector. except negation, where we can assume it is only arity=1, so its following arity byte is omitted for efficiency.
 * <p>
 * this limits max # of direct subterms to 127 in signed decimals which i currently dont have any problem with. i count volume with short so the total recursive size of a compound limited to ~16000. these limits are more or less arbitrary and can be re-decided.
 * <p>
 * the set of "anom" atoms, and normalized variables are special cases of atoms which get a more compact encoding due to their frequent repeated appearance during internal target activity: one 16-bit containing the 8-bit op select followed by an 8-bit ordinal id up to 127/255 also mostly wasted. this also supporting fast reads/decodes/deserialization that only needs to lookup a particular array index of for the associated globally-shared immutable instance.
 * https:
 * <p>
 * certain subterm implementations have accelerated read/write procedures, such as https:
 * <p>
 * other atoms serialize their entirity as UTF8 byte[], allowing unicode, etc. append() methods write directly to printable output streams or string builders. unnormalized variables, and other special cases get a default UTF-8 encoding for output that is reversible through the general narsese parser as a final option before failing due to suspected garbage input.
 * <p>
 * image / \ markers are represented internally as special un-unifiable vardep's taking the upper 2 indices of the vardep allocations.
 * <p>
 * Int (32-bit integer) subterm type is processed similar to the Anon's and serializes as its default 4-byte low endian ? encoding. special int packing (zig zag etc) could be used to reduce these on a per-instance level for common values like 0, -1, +1, and numbers less than 127. the Int exists for fast arithmetic ops that otherwise would involve an Atom encoding and decoding to an array, and the need to parse the content to determine if 'isInt()' etc.
 * <p>
 * 3 special Bool constants holds results of tautological truths which occur mostly in intermediate target construction/reduction steps, signaling either target construction failure (Null), True (inert when appearing in Conjunction, or simplify Implications to a subj or predicate, etc..), and False which is effectively (--,True). ie. (X && --X) => False, but --(X && --X) => True.
 * <p>
 * the compressibility of individual terms and task byte[] keys is not as as good as a batch block encoding of several terms and tasks sharing repeated common subterms. but an individual byte[] target or task 'key' is still somewhat compressible and snappy and lz4 can produce canonically compressed versions of terms but use of this can be decided depending if the uncompressed string exceeds some global threshold length due to seek acceptable balance between cpu and memory cost.
 *
 * @see: RLP classes: https:
 * TODO use http:
 */
public enum IO {
    ;


    @Deprecated public static final byte SPECIAL_BYTE = (byte) 0xff;


    public static final int streamBufferBytesDefault = 64 * 1024;
    public static final int gzipWindowBytesDefault =
            //512; //default
            1024;
    public static final int outputBufferBytesDefault = 128 * 1024;

    public static int readTasks(byte[] t, Consumer<Task> each) throws IOException {
        return readTasks(new ByteArrayInputStream(t), each);
    }


    public static int readTasks(InputStream i, Consumer<Task> each) throws IOException {

        DataInputStream ii = new DataInputStream(i);
        int count = 0;
        while (i.available() > 0 /*|| (i.available() > 0) || (ii.available() > 0)*/) {
            each.accept(TaskIO.readTask(ii));
            count++;
        }
        ii.close();
        return count;
    }


    private static void writePriority(ByteArrayDataOutput out, Prioritized t)  {
        out.writeFloat(t.priElseZero());
    }

    static void writeBudget(ByteArrayDataOutput out, Prioritized t)  {
        writePriority(out, t);
    }

    public static void writeEvidence(ByteArrayDataOutput out, long[] evi)  {
        int evil = evi.length;
        out.writeByte(evil);
        //TODO use zigzag delta encoding
        for (long anEvi : evi)
            out.writeLong(anEvi);
    }


    public static byte opAndEncoding(byte op, byte encoding) {
        return (byte) ((int) op | ((int) encoding << 5));
    }

    public static byte opAndEncoding(Op op, byte encoding) {
        return opAndEncoding(op.id, encoding);
    }

    static byte encoding(byte opByte) {
        return (byte) (((int) opByte & 0b11100000) >> 5);
    }


    public static byte[] termToBytes(Term t) {
        if (t instanceof Atomic) {
            return ((Atomic) t).bytes();
        } else {
            try (RecycledDynBytes d = RecycledDynBytes.get()) { //termBytesEstimate(t) /* estimate */);
                TermIO.the.write(t, d);
                return d.arrayCopy();
            }
        }
    }


    public static int termBytesEstimate(Termlike t) {
        return t.volume() * 8;
    }


    public static @Nullable byte[] taskToBytes(Task x) {
        DynBytes dos = new DynBytes(termBytesEstimate(x));

        @Nullable byte[] b = taskToBytes(x, dos);

        dos.close();

        return b;
    }

    public static @Nullable byte[] taskToBytes(Task x, DynBytes dos) {
        TaskIO.write(x, dos,true, true);
        return dos.arrayCopy();
    }

    public static DynBytes bytes(Task x, boolean budget, boolean creation, DynBytes dos) {
        dos.clear();
        TaskIO.write(x, dos, budget, creation);
        return dos;
    }
    public static DynBytes bytes(Term x, DynBytes dos) {
        dos.clear();
        TermIO.the.write(x, dos);
        return dos;
    }
    /**
     * WARNING
     */
    public static Term bytesToTerm(byte[] b) {
        try {
            return TermIO.the.read(input(b));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } /*catch (Term.InvalidTermException ignored) {
            return null;
        }*/
    }
    public static Term bytesToTerm(byte[] b, int offset) {
        try {
            return TermIO.the.read(input(b, offset));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } /*catch (Term.InvalidTermException ignored) {
            return null;
        }*/
    }

    public static BytesInput input(byte[] b) {
        return new BytesInput(b);
    }

    public static ByteArrayDataInput input(byte[] b, int offset) {
        return ByteStreams.newDataInput(b, offset);
    }


//    @FunctionalInterface
//    public interface EachTerm {
//        void nextTerm(Op o, int depth, int byteStart);
//    }
//    public static void mapSubTerms(byte[] term, EachTerm t) throws IOException {
//
//        int l = term.length;
//        int i = 0;
//
//        int level = 0;
//        final int MAX_LEVELS = 16;
//        byte[][] levels = new byte[MAX_LEVELS][2];
//
//        do {
//
//            int termStart = i;
//            byte ob = term[i];
//            i++;
//            Op o = Op.values()[ob];
//            t.nextTerm(o, level, termStart);
//
//
//            if (o.var) {
//                i += 1;
//            } else if (o.atomic) {
//
//                int hi = term[i++] & 0xff;
//                int lo = term[i++] & 0xff;
//                int utfLen = (hi << 8) | lo;
//                i += utfLen;
//
//            } else {
//
//                int subterms = term[i++];
//                levels[level][0] = ob;
//                levels[level][1] = (byte) (subterms  /* include this? */);
//                level++;
//
//            }
//
//            //pop:
//            while (level > 0) {
//                byte[] ll = levels[level - 1];
//                byte subtermsRemain = ll[1];
//                if (subtermsRemain == 0) {
//
//                    Op ol = Op.values()[ll[0]];
//                    if (ol.temporal) {
//                        throw new TODO("i += IntCoding.variableByteLengthOfZigZagInt()");
//                        // 4;
//                    }
//                    level--;
//                } else {
//                    ll[1] = (byte) (subtermsRemain - 1);
//                    break;
//                }
//            }
//
//        } while (i < l);
//
//        if (i != l) {
//            throw new IOException("decoding error");
//        }
//    }


//    public static void writeUTF8WithPreLen(String s, DataOutput o) throws IOException {
//        DynBytes d = new DynBytes(s.length());
//
//        new Utf8Writer(d).write(s);
//
//        o.writeShort(d.length());
//        d.appendTo(o);
//    }

//    public enum TaskSerialization {
//        TermFirst,
//        TermLast
//    }


    /**
     * visits each subterm of a compound and stores a tuple of integers for it
     */



}





































































































