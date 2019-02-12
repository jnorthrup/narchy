package nars;


import com.google.common.io.ByteArrayDataOutput;
import jcog.TODO;
import jcog.data.byt.DynBytes;
import jcog.data.byt.RecycledDynBytes;
import jcog.data.byt.util.IntCoding;
import jcog.io.BytesInput;
import jcog.pri.Prioritized;
import nars.subterm.Subterms;
import nars.task.CommandTask;
import nars.task.NALTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.Terms;
import nars.term.anon.Anom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.util.TermException;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

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
public class IO {


    /** upper control flags for the op byte */
    public static final byte SPECIAL_BIT = (byte) (1 << 7);
    public static final byte TEMPORAL_BIT = (byte) (1 << 6);

    /** lower 5 bits (bits 0..4) */
    public static final byte OP_MASK = (0b00011111);

    @Deprecated public static final byte SPECIAL_BYTE = (byte) 0xff;


    public static final int STREAM_BUFFER_SIZE = 64 * 1024;
    static final int GZIP_BUFFER_SIZE =
            //512; //default
            1024;
    //4 * 1024;

    public static int readTasks(byte[] t, Consumer<Task> each) throws IOException {
        return readTasks(new ByteArrayInputStream(t), each);
    }


    public static int readTasks(InputStream i, Consumer<Task> each) throws IOException {

        DataInputStream ii = new DataInputStream(i);
        int count = 0;
        while (i.available() > 0 /*|| (i.available() > 0) || (ii.available() > 0)*/) {
            Task t = readTask(ii);
            //try {
            each.accept(t);
            count++;
//            } catch (Throwable t) {
//
//            }
        }
        ii.close();
        return count;
    }

    private static boolean hasTruth(byte punc) {
        return punc == Op.BELIEF || punc == Op.GOAL;
    }

    public static Task readTask(DataInput in) throws IOException {

        byte punc = in.readByte();

        Term preterm = readTerm(in);

        final Term term = preterm.normalize();
        if (term == null)
            throw new IOException("un-normalizable task target");

        if (punc != COMMAND) {
            Truth truth = hasTruth(punc) ? readTruth(in) : null;

            long start = in.readLong();
            long end = in.readLong();

            long[] evi = readEvidence(in);

            float pri = in.readFloat();

            long cre = in.readLong();

            return new NALTask(term, punc, truth, cre, start, end, evi).priSet(pri);
        } else {
            return new CommandTask(term);
        }
    }

    private static long[] readEvidence(DataInput in) throws IOException {
        int eviLength = in.readByte();
        long[] evi = new long[eviLength];
        for (int i = 0; i < eviLength; i++) {
            evi[i] = in.readLong();
        }
        return evi;
    }

    private static Truth readTruth(DataInput in) throws IOException {

        return Truth.read(in);
    }


    /**
     * with Term first
     */
    private static void bytes(ByteArrayDataOutput out, Task t) throws IOException {


        byte p = t.punc();
        out.writeByte(p);


        t.term().appendTo(out);


        if (p != COMMAND) {
            if (hasTruth(p))
                Truth.write(t.truth(), out);

            //TODO use delta zig zag encoding (with creation time too)
            out.writeLong(t.start());
            out.writeLong(t.end());

            writeEvidence(out, t.stamp());

            writeBudget(out, t);

            out.writeLong(t.creation());
        }

    }


    private static void writePriority(DataOutput out, Prioritized t) throws IOException {
        out.writeFloat(t.priElseZero());
    }

    private static void writeBudget(DataOutput out, Prioritized t) throws IOException {
        writePriority(out, t);
    }

    public static void writeEvidence(DataOutput out, long[] evi) throws IOException {
        int evil = evi.length;
        out.writeByte(evil);
        //TODO use zigzag delta encoding
        for (long anEvi : evi)
            out.writeLong(anEvi);
    }


    private static Atomic readVariable(DataInput in, /*@NotNull*/ Op o) throws IOException {
        return $.v(o, in.readByte());
    }


    /**
     * called by readTerm after determining the op type
     */
    private static Term readTerm(DataInput in) throws IOException {

        byte opByte = in.readByte();
        if (opByte != SPECIAL_BYTE) {
            Op o = Op.ops[opByte & OP_MASK];
            switch (o) {
                case VAR_DEP:
                case VAR_INDEP:
                case VAR_PATTERN:
                case VAR_QUERY:
                    return readVariable(in, o);
                case IMG:
                    return in.readByte() == ((byte)'/') ? Op.ImgExt : Op.ImgInt;
                case BOOL:
                    byte code = in.readByte();
                    switch (code) {
                        case -1:
                            return Bool.Null;
                        case 0:
                            return Bool.False;
                        case 1:
                            return Bool.True;
                        default:
                            throw new UnsupportedEncodingException();
                    }
                case ATOM:
                    switch (subType(opByte)) {
                        case 0:
                            return Atomic.the(in.readUTF());
                        case 1:
                            return Anom.the(in.readByte());
                        default:
                            throw new TODO();
                    }
                case INT:
                    return Int.the(IntCoding.readZigZagInt(in));
                case NEG:
                    return readNegated(in);
                default:
                    return readCompound(in, o, (opByte & TEMPORAL_BIT) != 0);

            }
        } else {
            return readSpecialTerm(in);
        }
    }

    public static byte opAndSubType(Op op, byte subtype) {
        return opAndSubType(op.id, subtype);
    }

    private static byte opAndSubType(byte op, byte subtype) {
        return (byte) (op | (subtype << 5));
    }

    private static byte subType(byte opByte) {
        return (byte) ((opByte & 0b11100000) >> 5);
    }

    private static Term readSpecialTerm(DataInput in) throws IOException {
        try {
            return Narsese.term(in.readUTF(), false);
        } catch (Narsese.NarseseException e) {
            throw new IOException(e);
        }
    }




    private static Term[] readTermContainer(DataInput in) throws IOException {
        int siz = in.readByte();

        assert (siz < Param.SUBTERMS_MAX);

        Term[] s = new Term[siz];
        for (int i = 0; i < siz; i++) {
            Term read = (s[i] = readTerm(in));
            if (read == null)
                throw new TermException(Op.PROD /* consider the termvector as a product */, s, "invalid");
        }

        return s;
    }

    private static Term readNegated(DataInput in) throws IOException {
        return readTerm(in).neg();
    }

    /**
     * called by readTerm after determining the op type
     * TODO make a version which reads directlyinto TermIndex
     */
    private static Term readCompound(DataInput in, /*@NotNull*/ Op o, boolean temporal) throws IOException {

        int dt = temporal ? IntCoding.readZigZagInt(in) : DTERNAL;

        Term[] v = readTermContainer(in);

        Term y = o.the(dt, v);
        if (!(y instanceof Compound))
            throw new TermException(o, dt, v, "read invalid compound");

        return y;
    }

    public static byte[] termToBytes(Term t) {
        if (t instanceof Atomic) {
            return ((Atomic) t).bytes();
        } else {
            try (RecycledDynBytes d = RecycledDynBytes.get()) { //termBytesEstimate(t) /* estimate */);
                t.appendTo((ByteArrayDataOutput) d);
                return d.arrayCopy();
            }
        }
    }

    /**
     * warning: result may be RecycledDynBytes
     */
    public static DynBytes termToDynBytes(Term t) {
        if (t instanceof Atomic) {
            return new DynBytes(((Atomic) t).bytes()); //dont recycle
        } else {
            DynBytes d = new DynBytes(termBytesEstimate(t));
            t.appendTo((ByteArrayDataOutput) d);
            return d;
        }
    }

    public static int termBytesEstimate(Termlike t) {
        return t.volume() * 8;
    }


    @Nullable
    public static byte[] taskToBytes(Task x) {
        DynBytes dos = new DynBytes(termBytesEstimate(x));

        byte[] b = taskToBytes(x, dos);

        dos.close();

        return b;
    }

    public @Nullable
    static byte[] taskToBytes(Task x, DynBytes dos) {
        return bytes(x, dos).arrayCopy();
    }

    @Nullable
    public static DynBytes bytes(Task x, DynBytes dos) {
        try {


            dos.clear();
            IO.bytes(dos, x);


        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return dos;
    }

    /**
     * WARNING
     */
    public static Task bytesToTask(byte[] b) throws IOException {
        return IO.readTask(input(b));
    }

    /**
     * WARNING
     */
    @Nullable
    public static Term bytesToTerm(byte[] b) {
        try {
            return IO.readTerm(input(b));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } /*catch (Term.InvalidTermException ignored) {
            return null;
        }*/
    }

    private static DataInput input(byte[] b) {
        return new BytesInput(b);
    }

    public static void mapSubTerms(byte[] term, EachTerm t) throws IOException {

        int l = term.length;
        int i = 0;

        int level = 0;
        final int MAX_LEVELS = 16;
        byte[][] levels = new byte[MAX_LEVELS][2];

        do {

            int termStart = i;
            byte ob = term[i];
            i++;
            Op o = Op.values()[ob];
            t.nextTerm(o, level, termStart);


            if (o.var) {
                i += 1;
            } else if (o.atomic) {

                int hi = term[i++] & 0xff;
                int lo = term[i++] & 0xff;
                int utfLen = (hi << 8) | lo;
                i += utfLen;

            } else {

                int subterms = term[i++];
                levels[level][0] = ob;
                levels[level][1] = (byte) (subterms  /* include this? */);
                level++;

            }

            //pop:
            while (level > 0) {
                byte[] ll = levels[level - 1];
                byte subtermsRemain = ll[1];
                if (subtermsRemain == 0) {

                    Op ol = Op.values()[ll[0]];
                    if (ol.temporal) {
                        throw new TODO("i += IntCoding.variableByteLengthOfZigZagInt()");
                        // 4;
                    }
                    level--;
                } else {
                    ll[1] = (byte) (subtermsRemain - 1);
                    break;
                }
            }

        } while (i < l);

        if (i != l) {
            throw new IOException("decoding error");
        }
    }


//    public static void writeUTF8WithPreLen(String s, DataOutput o) throws IOException {
//        DynBytes d = new DynBytes(s.length());
//
//        new Utf8Writer(d).write(s);
//
//        o.writeShort(d.length());
//        d.appendTo(o);
//    }

    public enum TaskSerialization {
        TermFirst,
        TermLast
    }


    public interface Printer {

        static void compoundAppend(Compound c, Appendable p, Op op) throws IOException {

            p.append(Op.COMPOUND_TERM_OPENER);

            op.append(c, p);

            Subterms cs = c.subterms();
            if (cs.subs() == 1)
                p.append(Op.ARGUMENT_SEPARATOR);

            appendArgs(cs, p);

            p.append(Op.COMPOUND_TERM_CLOSER);

        }

        static void compoundAppend(String o, Subterms c, Function<Term, Term> filter, Appendable p) throws IOException {

            p.append(Op.COMPOUND_TERM_OPENER);

            p.append(o);

            if (c.subs() == 1)
                p.append(Op.ARGUMENT_SEPARATOR);

            appendArgs(c, filter, p);

            p.append(Op.COMPOUND_TERM_CLOSER);

        }


        static void appendArgs(Subterms c, Appendable p) throws IOException {
            int nterms = c.subs();

            boolean bb = nterms > 1;
            for (int i = 0; i < nterms; i++) {
                if ((i != 0) || bb) {
                    p.append(Op.ARGUMENT_SEPARATOR);
                }
                c.sub(i).appendTo(p);
            }
        }

        static void appendArgs(Subterms c, Function<Term, Term> filter, Appendable p) throws IOException {
            int nterms = c.subs();

            boolean bb = nterms > 1;
            for (int i = 0; i < nterms; i++) {
                if ((i != 0) || bb) {
                    p.append(Op.ARGUMENT_SEPARATOR);
                }
                filter.apply(c.sub(i)).appendTo(p);
            }
        }

        static void append(Compound c, Appendable p) throws IOException {
            final Op op = c.op();

            switch (op) {

                case SECTi:
                case SECTe:
                    sectAppend(c, p);
                    return;

                case SETi:
                case SETe:
                    setAppend(c, p);
                    return;
                case PROD:
                    productAppend(c.subterms(), p);
                    return;
                case NEG:
                    negAppend(c, p);
                    return;
            }

            if (op.statement || c.subs() == 2) {


                if (c.hasAll(Op.FuncBits)) {
                    Term subj = c.sub(0);
                    if (op == INH && subj.op() == Op.PROD) {
                        Term pred = c.sub(1);
                        Op pOp = pred.op();
                        if (pOp == ATOM) {
                            operationAppend((Compound) subj, (Atomic) pred, p);
                            return;
                        }
                    }
                }

                statementAppend(c, p, op);

            } else {
                compoundAppend(c, p, op);
            }
        }

        static void sectAppend(Compound c, Appendable p) throws IOException {
            Op o = c.op();
            Subterms cs = c.subterms();
            if (cs.subs() == 2) {
                Term subracted = cs.sub(0), from;
                //negated subterm will be in the 0th position, if anywhere due to target sorting
                if (subracted.op() == NEG && (from=cs.sub(1)).op()!=NEG) {
                    p.append('(');
                    from.appendTo(p);
                    p.append(o == SECTe ? DIFFi : DIFFe);
                    subracted.unneg().appendTo(p);
                    p.append(')');
                    return;
                }

                statementAppend(c, p, o);
            } else {
                compoundAppend(c, p, o);
            }
        }

        static void negAppend(final Compound neg, Appendable p) throws IOException {
            /**
             * detect a negated conjunction of negated subterms:
             * (--, (&&, --A, --B, .., --Z) )
             */

            final Term sub = neg.unneg();

            if ((sub.op() == CONJ) && sub.hasAny(NEG.bit)) {
                int dt;
                if ((((dt = sub.dt()) == DTERNAL) || (dt == XTERNAL))) {
                    Subterms cxx = sub.subterms();
                    if (Terms.negatedNonConjCount(cxx) >= cxx.subs() / 2) {
                        String s;
                        switch (dt) {
                            case XTERNAL:
                                s = Op.DISJstr + "+- ";
                                break;
                            case DTERNAL:
                                s = Op.DISJstr;
                                break;
                            default:
                                throw new UnsupportedOperationException();
                        }

//                                    if (cxx.subs() == 2) {
//                                        statementAppend(cx, op, p); //infix
//                                    } else {
                        compoundAppend(s, cxx, Term::neg, p);
//                                    }
                        return;

                    }
                }
            }

            p.append("(--,");
            sub.appendTo(p);
            p.append(')');
        }


        static void statementAppend(Term c, Appendable p, Op op  /*@NotNull*/) throws IOException {


            p.append(Op.COMPOUND_TERM_OPENER);

            int dt = c.dt();

            boolean reversedDT = dt != DTERNAL && /*dt != XTERNAL && */ dt < 0 && op.commutative;

            Subterms cs = c.subterms();
            cs.sub(reversedDT ? 1 : 0).appendTo(p);

            op.append(dt, p, reversedDT);

            cs.sub(reversedDT ? 0 : 1).appendTo(p);

            p.append(Op.COMPOUND_TERM_CLOSER);
        }


        static void productAppend(Subterms product, Appendable p) throws IOException {

            int s = product.subs();
            p.append(Op.COMPOUND_TERM_OPENER);
            for (int i = 0; i < s; i++) {
                product.sub(i).appendTo(p);
                if (i < s - 1) {
                    p.append(',');
                }
            }
            p.append(Op.COMPOUND_TERM_CLOSER);
        }


        static void setAppend(Compound set, Appendable p) throws IOException {

            int len = set.subs();


            char opener, closer;
            if (set.op() == Op.SETe) {
                opener = Op.SETe.ch;
                closer = Op.SET_EXT_CLOSER;
            } else {
                opener = Op.SETi.ch;
                closer = Op.SET_INT_CLOSER;
            }

            p.append(opener);

            Subterms setsubs = set.subterms();
            for (int i = 0; i < len; i++) {
                if (i != 0) p.append(Op.ARGUMENT_SEPARATOR);
                setsubs.sub(i).appendTo(p);
            }
            p.append(closer);
        }

        static void operationAppend(Compound argsProduct, Atomic operator, Appendable w) throws IOException {

            operator.appendTo(w);

            w.append(Op.COMPOUND_TERM_OPENER);


            argsProduct.forEachWith((t, n) -> {
                try {
                    if (n != 0)
                        w.append(Op.ARGUMENT_SEPARATOR);

                    t.appendTo(w);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            w.append(Op.COMPOUND_TERM_CLOSER);

        }


    }


    /**
     * visits each subterm of a compound and stores a tuple of integers for it
     */


    @FunctionalInterface
    public interface EachTerm {
        void nextTerm(Op o, int depth, int byteStart);
    }

}





































































































