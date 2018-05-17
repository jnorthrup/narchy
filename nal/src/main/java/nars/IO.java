package nars;


import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import jcog.TODO;
import jcog.data.byt.DynBytes;
import jcog.data.string.Utf8Writer;
import jcog.pri.Prioritized;
import nars.subterm.Subterms;
import nars.task.CommandTask;
import nars.task.NALTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.anon.Anom;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.var.UnnormalizedVariable;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static nars.Op.*;
import static nars.term.compound.FastCompound.ov;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * Created by me on 5/29/16.
 * <p>
 * a term's op is only worth encoding in more than a byte since there arent more than 10..20 base operator types. that leaves plenty else for other kinds of control codes that btw probably can fit within the (invisible) ascii control codes. i use basically 1 byte for the op, then depending on this if the term is atomic or compound, the atom decoding reads UTF-8 byte[] directly as the atom's key. compound ops are followed by a byte for # of subterms that will follow, forming its subterm vector. except negation, where we can assume it is only arity=1, so its following arity byte is omitted for efficiency.
 * <p>
 * this limits max # of direct subterms to 127 in signed decimals which i currently dont have any problem with. i count volume with short so the total recursive size of a compound limited to ~16000. these limits are more or less arbitrary and can be re-decided.
 * <p>
 * the set of "anom" atoms, and normalized variables are special cases of atoms which get a more compact encoding due to their frequent repeated appearance during internal term activity: one 16-bit containing the 8-bit op select followed by an 8-bit ordinal id up to 127/255 also mostly wasted. this also supporting fast reads/decodes/deserialization that only needs to lookup a particular array index of for the associated globally-shared immutable instance.
 * https://github.com/automenta/narchy/blob/skynet5/nal/src/main/java/nars/term/anon/AnonID.java#L11
 * <p>
 * certain subterm implementations have accelerated read/write procedures, such as https://github.com/automenta/narchy/blob/skynet5/nal/src/main/java/nars/term/anon/AnonVector.java#L51 which is used when every subterm is an Anon instance ("anom" atoms or normalized variables, which can be used to account for 99% of the unification activity). the write procedure for example essentially decompresses its short[] that fully describes its contents, including any negations (element's highest bit is set)
 * <p>
 * other atoms serialize their entirity as UTF8 byte[], allowing unicode, etc. append() methods write directly to printable output streams or string builders. unnormalized variables, and other special cases get a default UTF-8 encoding for output that is reversible through the general narsese parser as a final option before failing due to suspected garbage input.
 * <p>
 * image / \ markers are represented internally as special un-unifiable vardep's taking the upper 2 indices of the vardep allocations.
 * <p>
 * Int (32-bit integer) subterm type is processed similar to the Anon's and serializes as its default 4-byte low endian ? encoding. special int packing (zig zag etc) could be used to reduce these on a per-instance level for common values like 0, -1, +1, and numbers less than 127. the Int exists for fast arithmetic ops that otherwise would involve an Atom encoding and decoding to an array, and the need to parse the content to determine if 'isInt()' etc.
 * <p>
 * 3 special Bool constants holds results of tautological truths which occur mostly in intermediate term construction/reduction steps, signaling either term construction failure (Null), True (inert when appearing in Conjunction, or simplify Implications to a subj or predicate, etc..), and False which is effectively (--,True). ie. (X && --X) => False, but --(X && --X) => True.
 * <p>
 * the compressibility of individual terms and task byte[] keys is not as as good as a batch block encoding of several terms and tasks sharing repeated common subterms. but an individual byte[] term or task 'key' is still somewhat compressible and snappy and lz4 can produce canonically compressed versions of terms but use of this can be decided depending if the uncompressed string exceeds some global threshold length due to seek acceptable balance between cpu and memory cost.
 *
 * @see: RLP classes: https://github.com/ethereum/ethereumj/blob/develop/ethereumj-core/src/main/java/org/ethereum/util/RLP.java
 * TODO use http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/io/ByteStreams.html
 */
public class IO {

    public static final byte SPECIAL_OP = (byte) 0xff;

    public static int readTasks(byte[] t, Consumer<Task> each) throws IOException {
        return readTasks(new ByteArrayInputStream(t), each);
    }

//    public interface TermEncoder {
//        default void write(Term x) {
//            write(x, new DynBytes(x.volume() * 4 /* ESTIMATE */));
//        }
//
//        void write(Term x, DynBytes to);
//    }

//    public static class DefaultTermEncoder implements TermEncoder {
//
//        @Override
//        public void write(Term x, DynBytes to) {
//
//            x.append((ByteArrayDataOutput) to);
//
//        }
//    }

//TODO
//    public interface TermDecoder {
//        void Term
//    }

    public static int readTasks(InputStream i, Consumer<Task> each) throws IOException {
        //SnappyFramedInputStream i = new SnappyFramedInputStream(tasks, true);
        DataInputStream ii = new DataInputStream(i);
        int count = 0;
        while (i.available() > 0 /*|| (i.available() > 0) || (ii.available() > 0)*/) {
            Task t = readTask(ii);
            each.accept(t);
            count++;
        }
        ii.close();
        return count;
    }

    static boolean hasTruth(byte punc) {
        return punc == Op.BELIEF || punc == Op.GOAL;
    }

    public static Task readTask(DataInput in) throws IOException {

        byte punc = in.readByte();

        Term preterm = readTerm(in);

        final Term term = preterm.normalize();
        if (term == null)
            throw new IOException("un-normalizable task term");

        if (punc!=COMMAND) {
            Truth truth = hasTruth(punc) ? readTruth(in) : null;

            long start = in.readLong();
            long end = in.readLong();

            long[] evi = readEvidence(in);

            float pri = in.readFloat();

            long cre = in.readLong();

            return new NALTask(term, punc, truth, cre, start, end, evi).pri(pri);
        } else {
            return new CommandTask(term);
        }
    }

    public static long[] readEvidence(DataInput in) throws IOException {
        int eviLength = in.readByte();
        long[] evi = new long[eviLength];
        for (int i = 0; i < eviLength; i++) {
            evi[i] = in.readLong();
        }
        return evi;
    }

    public static Truth readTruth(DataInput in) throws IOException {
        //return DiscreteTruth.intToTruth(in.readInt());
        return Truth.read(in);
    }


    /**
     * with Term first
     */
    public static void writeTask(DataOutput out, Task t) throws IOException {


        byte p = t.punc();
        out.writeByte(p);

        //        if (out instanceof ByteArrayDataOutput) {
        t.term().append((ByteArrayDataOutput) out);
//        } else {
//            out.write(IO.termToBytes(tt)); //buffer to bytes
//        }


        if (p!=COMMAND) {
            if (hasTruth(p))
                Truth.write(t.truth(), out);

            out.writeLong(t.start());
            out.writeLong(t.end());

            writeEvidence(out, t.stamp());

            writeBudget(out, t);

            out.writeLong(t.creation()); //put this last because it is the least useful really
        }

    }

//    /**
//     * with Term last
//     */
//    public static void writeTask2(DataOutput out, Task t) throws IOException {
//
//        byte p = t.punc();
//        out.writeByte(p);
//
//        writeBudget(out, t);
//
//        out.writeLong(t.start());
//        out.writeLong(t.end());
//
//        if (hasTruth(p)) {
//            out.writeFloat(t.freq());
//            out.writeFloat(t.conf());
//        }
//
//        //writeEvidence(out, t.evidence());
//
//        //out.writeLong(t.creation()); //put this last because it is the least useful really
//
//        IO.writeUTF8WithPreLen(t.term().toString(), out);
//    }

    //    public static void writeStringUTF(DataOutput out, String s) throws IOException {
//
//        //byte[] bb = s.getBytes(Charset.defaultCharset());
//        byte[] bb = s.getBytes(Charset.defaultCharset()); //Hack.bytes(s);
//        out.writeShort(bb.length);
//        out.write(bb);
//    }

    public static void writePriority(DataOutput out, Prioritized t) throws IOException {
        out.writeFloat(t.priElseZero());
    }

    public static void writeBudget(DataOutput out, Prioritized t) throws IOException {
        writePriority(out, t);
    }

    public static void writeEvidence(DataOutput out, long[] evi) throws IOException {
        int evil = evi.length;
        out.writeByte(evil);
        for (int i = 0; i < evil; i++)
            out.writeLong(evi[i]);
    }


    public static Atomic readVariable(DataInput in, /*@NotNull*/ Op o) throws IOException {
        return $.v(o, in.readByte());
    }


    /**
     * direct method of reading Atomic from a byte[]
     */
    public static Atomic readAtomic(byte[] b) {
        byte oo = b[0];
        if (oo == SPECIAL_OP)
            return (Atomic) termFromBytes(b);

        Op o = ov[oo];
        switch (o) {

            case ATOM:
                return new Atom(b);

            case VAR_PATTERN:
            case VAR_DEP:
            case VAR_QUERY:
            case VAR_INDEP:
                return $.v(o, b[1]);

            case INT:
//                byte subType = in.readByte();
//                switch (subType) {
//                    case 0: return Int.the( in.readInt());
//                    case 1: return Int.range( in.readInt(), in.readInt() );
//                    default: throw new TODO();
//                }
                return (Atomic) termFromBytes(b);
            //throw new TODO();


            //TODO normalized Variable cases

            default:


//                try {
//                    return $.$(new String(b));
//                } catch (Narsese.NarseseException e) {
//                }
//                    throw new TODO();
                return (Atomic) termFromBytes(b);

        }
    }

    static Atomic readAtomic(DataInput in, /*@NotNull*/ Op o, byte subType) throws IOException {

        switch (o) {

            case INT:
                switch (subType) {
                    case 0:
                        return Int.the(in.readInt());
                    case 1:
                        return Int.range(in.readInt(), in.readInt());
                    default:
                        throw new TODO();
                }
            case ATOM: {
                switch (subType) {
                    case 0:
                        return Atomic.the(in.readUTF());
                    case 1:
                        return Anom.the(in.readByte());
                    default:
                        throw new TODO();

                }
            }
            default:
                String s = in.readUTF();
                try {
                    return $.$(s);
                } catch (Narsese.NarseseException e) {
                    throw new UnsupportedEncodingException(e.getMessage());
                }
        }

    }


    /**
     * called by readTerm after determining the op type
     */
    static Term readTerm(DataInput in) throws IOException {

        byte opByte = in.readByte();
        if (opByte == SPECIAL_OP)
            return readSpecialTerm(in);
        else {
            //base op contained in lower 5-bits (0..31)
            byte op = (byte) (opByte & 0b00011111);
            Op o = Op.ops[op];
            if (o.var)
                return readVariable(in, o);
            else if (o.atomic)
                return readAtomic(in, o, subType(opByte));
            else if (o == NEG)
                return readNegated(in);
            else
                return readCompound(in, o);
        }
    }

    public static byte opAndSubType(Op op, byte subtype) {
        return opAndSubType(op.id, subtype);
    }

    public static byte opAndSubType(byte op, byte subtype) {
        return (byte) (op | (subtype<<5));
    }

    static byte subType(byte opByte) {
        return (byte) ((opByte & 0b11100000) >> 5);
    }

    static Term readSpecialTerm(DataInput in) throws IOException {
        try {
            return Narsese.term(in.readUTF(), false);
        } catch (Narsese.NarseseException e) {
            throw new IOException(e);
        }
    }


    public static void writeCompoundSuffix(DataOutput out, int dt, Op o) throws IOException {
        if (o.temporal)
            out.writeInt(dt);
    }

    public static boolean isSpecial(Term term) {
        return term instanceof UnnormalizedVariable;
    }


    public static Term[] readTermContainer(DataInput in) throws IOException {
        int siz = in.readByte();

        assert (siz < Param.COMPOUND_SUBTERMS_MAX);

        Term[] s = new Term[siz];
        for (int i = 0; i < siz; i++) {
            Term read = (s[i] = readTerm(in));
            if (read == null || read instanceof Bool)
                throw new Term.InvalidTermException(Op.PROD /* consider the termvector as a product */, s, "invalid");
        }

        return s;
    }

    static Term readNegated(DataInput in) throws IOException {
        return readTerm(in).neg();
    }

    /**
     * called by readTerm after determining the op type
     * TODO make a version which reads directlyinto TermIndex
     */
    static Term readCompound(DataInput in, /*@NotNull*/ Op o) throws IOException {

        Term[] v = readTermContainer(in);

        int dt;

        if (o.temporal) {
            dt = in.readInt();
        } else {
            dt = DTERNAL;
        }

        Term y = o.compound(dt, v);
        if (y instanceof Bool)
            throw new Term.InvalidTermException(o, dt, v, "invalid term");

//        if (key == null)
//            throw new UnsupportedOperationException();
//        return (Compound) t.normalize(key, true);
        return y;
    }

    public static byte[] termToBytes(Term t) {
        if (t instanceof Atomic) {
            return ((Atomic) t).bytes();
        } else {
            DynBytes d = new DynBytes(t.volume() * 6 /* estimate */);
            t.append((ByteArrayDataOutput) d);
            return d.array();
        }
    }

//    public static void saveTasksToTemporaryTSVFile(NAR nar) throws IOException {
//        Path f = Files.createTempFile(Paths.get("/tmp"), "nar", ".tsv");
//        System.out.println("saving tasks: " + f);
//        FileOutputStream os = new FileOutputStream(f.toFile());
//        PrintStream ps = new PrintStream(os);
//        nar.tasks().forEach(t -> {
//            Task tt = t;
//            try {
//                tt.appendTSV(ps);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
//    }

//    public static void saveTasksToTemporaryTextFile(NAR nar) throws IOException {
//        Path f = Files.createTempFile(Paths.get("/tmp"), "nar", ".nal");
//        System.out.println("saving tasks: file://" + f);
//        FileOutputStream os = new FileOutputStream(f.toFile());
//        PrintStream ps = new PrintStream(os);
//        nar.tasks().forEach(t -> {
//            Task tt = t;
//            try {
//                tt.appendTo(ps);
//                ps.append('\n');
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
//    }

    @Nullable
    public static byte[] taskToBytes(Task x) {
//        return taskToBytes(x, TermFirst);
//    }
//
//    public static byte[] taskToBytes(Task x, TaskSerialization mode) {
        try {
            DynBytes dos = new DynBytes(x.volume() * 16);
//            switch (mode) {
//                case TermFirst:
                    IO.writeTask(dos, x);
//                    break;
//                case TermLast:
//                    IO.writeTask2(dos, x);
//                    break;
//            }
            return dos.array();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * WARNING
     */
    @Nullable
    public static Task taskFromBytes(byte[] b) {
        try {
            return IO.readTask(input(b));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * WARNING
     */
    @Nullable
    public static Term termFromBytes(byte[] b) {
        try {
            return IO.readTerm(input(b));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } /*catch (Term.InvalidTermException ignored) {
            return null;
        }*/
    }

    public static ByteArrayDataInput input(byte[] b) {
        return input(b, 0);
        //return ByteStreams.newDataInput(b);
        //return new DataInputStream(new ByteArrayInputStream(b));
    }

    public static ByteArrayDataInput input(byte[] b, int offset) {
        return ByteStreams.newDataInput(b, offset);
        //return new DataInputStream(new ByteArrayInputStream(b, offset, b.length - offset));
    }

    public static void writeUTF8WithPreLen(String s, DataOutput o) throws IOException {
        DynBytes d = new DynBytes(s.length());

        new Utf8Writer(d).write(s);

        o.writeShort(d.length());
        d.appendTo(o);
    }

    public static void mapSubTerms(byte[] term, EachTerm t) throws IOException {

        int l = term.length;
        int i = 0;

        int level = 0;
        final int MAX_LEVELS = 16;
        byte[][] levels = new byte[MAX_LEVELS][2]; //level stack x (op, subterms remaining) tuple

        do {

            int termStart = i;
            byte ob = term[i];
            i++;
            Op o = Op.values()[ob];
            t.nextTerm(o, level, termStart);


            if (o.var) {
                i += 1; //int id = input(term, i).readByte();
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

            pop:
            while (level > 0) {
                byte[] ll = levels[level - 1];
                byte subtermsRemain = ll[1];
                if (subtermsRemain == 0) {
                    //end of compound:
                    Op ol = Op.values()[ll[0]];
                    if (ol.temporal)
                        i += 4; //skip temporal dt (32 bits)
                    level--;
                    continue pop; //see if the next level up is finished
                } else {
                    ll[1] = (byte) (subtermsRemain - 1);
                    break; //continue to next subterm
                }
            }

        } while (i < l);

        if (i != l) {
            throw new IOException("decoding error");
        }
    }



//    public static void writeUTF8(String s, DataOutput o) throws IOException {
//        new Utf8Writer(o).write(s);
//    }

    public enum TaskSerialization {
        TermFirst,
        TermLast
    }

//    public static Term fromJSON(String json) {
//        JsonValue v = Json.parse(json);
//        return fromJSON(v);
//    }
//
//    public static Term toJSON(Term term) {
//        return $.func("json", $.quote(toJSONValue(term)));
//    }
//
//    public static JsonValue toJSONValue(Term term) {
//        switch (term.op()) {
//
//            //TODO other types
//
//            /*case SETe: {
//                JsonObject o = Json.object();
//                for (Term x : term)
//                    o.add
//            }*/
//            case PROD:
//                JsonArray a = (JsonArray) Json.array();
//                for (Term x : ((Compound) term))
//                    a.add(toJSONValue(x));
//                return a;
//            default:
//                return Json.value(term.toString());
//        }
//    }
//
//    public static Term fromJSON(JsonValue v) {
//        if (v instanceof JsonObject) {
//            JsonObject o = (JsonObject) v;
//            int s = o.size();
//            List<Term> members = $.newArrayList(s);
//            o.forEach(m -> members.add($.inh(fromJSON(m.getValue()), $.the(m.getName()))));
//            return $.
//                    //parallel
//                            sete
//                    //secte
//                            (members/*.toArray(new Term[s])*/);
//
//        } else if (v instanceof JsonArray) {
//            JsonArray o = (JsonArray) v;
//            List<Term> vv = $.newArrayList(o.size());
//            o.forEach(x -> vv.add(fromJSON(x)));
//            return $.p(vv);
//        }
//        String vv = v.toString();
//        return $.the(vv);
//        //return $.quote(vv);
//    }

//    /**
//     * Writes a string to the specified DataOutput using
//     * <a href="DataInput.html#modified-utf-8">modified UTF-8</a>
//     * encoding in a machine-independent manner.
//     * <p>
//     * First, two bytes are written to out as if by the <code>writeShort</code>
//     * method giving the number of bytes to follow. This value is the number of
//     * bytes actually written out, not the length of the string. Following the
//     * length, each character of the string is output, in sequence, using the
//     * modified UTF-8 encoding for the character. If no exception is thrown, the
//     * counter <code>written</code> is incremented by the total number of
//     * bytes written to the output stream. This will be at least two
//     * plus the length of <code>str</code>, and at most two plus
//     * thrice the length of <code>str</code>.
//     *
//     * @param out destination to write to
//     * @param str a string to be written.
//     * @return The number of bytes written out.
//     * @throws IOException if an I/O error occurs.
//     */
//    public static void writeUTFWithoutLength(DataOutput out, String str) throws IOException {
//
//
//        //int c, count = 0;
//
////        /* use charAt instead of copying String to char array */
////        for (int i = 0; i < strlen; i++) {
////            c = str.charAt(i);
////            if ((c >= 0x0001) && (c <= 0x007F)) {
////                utflen++;
////            } else if (c > 0x07FF) {
////                utflen += 3;
////            } else {
////                utflen += 2;
////            }
////        }
////
////        if (utflen > 65535)
////            throw new UTFDataFormatException(
////                    "encoded string too long: " + utflen + " bytes");
//
//        //byte[] bytearr = null;
////        if (out instanceof DataOutputStream) {
////            DataOutputStream dos = (DataOutputStream)out;
////            if(dos.bytearr == null || (dos.bytearr.length < (utflen+2)))
////                dos.bytearr = new byte[(utflen*2) + 2];
////            bytearr = dos.bytearr;
////        } else {
//        //bytearr = new byte[utflen];
////        }
//
//        //Length information, not written
//        //bytearr[count++] = (byte) ((utflen >>> 8) & 0xFF);
//        //bytearr[count++] = (byte) ((utflen >>> 0) & 0xFF);
//
//        int strlen = str.length();
//        int i, c;
//        for (i = 0; i < strlen; i++) {
//            c = str.charAt(i);
//            if (!((c >= 0x0001) && (c <= 0x007F))) break;
//            out.writeByte((byte) c);
//        }
//
//        for (; i < strlen; i++) {
//            c = str.charAt(i);
//            if ((c >= 0x0001) && (c <= 0x007F)) {
//                out.writeByte((byte) c);
//
//            } else if (c > 0x07FF) {
//                out.writeByte((byte) (0xE0 | ((c >> 12) & 0x0F)));
//                out.writeByte((byte) (0x80 | ((c >> 6) & 0x3F)));
//                out.writeByte((byte) (0x80 | ((c >> 0) & 0x3F)));
//            } else {
//                out.writeByte((byte) (0xC0 | ((c >> 6) & 0x1F)));
//                out.writeByte((byte) (0x80 | ((c >> 0) & 0x3F)));
//            }
//        }
//    }

    public interface Printer {

        static void compoundAppend(Compound c, Appendable p) throws IOException {

            p.append(Op.COMPOUND_TERM_OPENER);

            c.op().append(c, p);

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
                c.sub(i).append(p);
            }
        }

        static void appendArgs(Subterms c, Function<Term, Term> filter, Appendable p) throws IOException {
            int nterms = c.subs();

            boolean bb = nterms > 1;
            for (int i = 0; i < nterms; i++) {
                if ((i != 0) || bb) {
                    p.append(Op.ARGUMENT_SEPARATOR);
                }
                filter.apply(c.sub(i)).append(p);
            }
        }

        static void append(Compound c, Appendable p) throws IOException {
            final Op op = c.op();

            switch (op) {

                case SETi:
                case SETe:
                    setAppend(c, p);
                    return;
                case PROD:
                    productAppend(c.subterms(), p);
                    return;

                //case INHERIT: inheritAppend(c, p, pretty); return;
                //case SIMILAR: similarAppend(c, p, pretty); return;

                case NEG:
                    /**
                     * detects a negated conjunction of negated subterms:
                     * (--, (&&, --A, --B, .., --Z) )
                     */

                    if (c.op() == NEG) {
                        Term cx = c.unneg();
                        if (cx.op() == CONJ && cx.dt() == DTERNAL) { //for DTERNAL only
                            if (Terms.allNegated(cx.subterms())) {
                                compoundAppend(Op.DISJstr, cx.subterms(), Term::neg, p);
                                return;
                            }
                        }
                    }
            }

            if (op.statement || c.subs() == 2) {

                //special case: functional form
                if (c.hasAll(Op.funcBits)) {
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
                compoundAppend(c, p);
            }
        }

//        static void inheritAppend(Compound c, Appendable p) throws IOException {
//            Term a = Statement.subj(c);
//            Term b = Statement.pred(c);
//
//            p.append(Symbols.COMPOUND_TERM_OPENER);
//            b.append(p);
//            p.append(Symbols.INHERIT_SEPARATOR);
//            a.append(p);
//            p.append(Symbols.COMPOUND_TERM_CLOSER);
//        }
//        static void similarAppend(Compound c, Appendable p) throws IOException {
//            Term a = Statement.subj(c);
//            Term b = Statement.pred(c);
//
//            p.append(Symbols.COMPOUND_TERM_OPENER);
//            a.append(p);
//            p.append(Symbols.SIMILAR_SEPARATOR);
//            b.append(p);
//            p.append(Symbols.COMPOUND_TERM_CLOSER);
//        }

        static void statementAppend(Compound c, Appendable p, /*@NotNull*/ Op op) throws IOException {

            Subterms cs = c.subterms();
            Term a = cs.sub(0);
            Term b = cs.sub(1);


            p.append(Op.COMPOUND_TERM_OPENER);
            boolean reversedDT;

            int dt = c.dt();
            if (c.op().commutative && dt != XTERNAL && dt != DTERNAL && dt < 0) {
                reversedDT = true;
                Term x = a;
                a = b;
                b = x;
            } else {
                reversedDT = false;
            }

            a.append(p);

            op.append(dt, p, reversedDT);

            b.append(p);

            p.append(Op.COMPOUND_TERM_CLOSER);
        }


        static void productAppend(Subterms product, Appendable p) throws IOException {

            int s = product.subs();
            p.append(Op.COMPOUND_TERM_OPENER);
            for (int i = 0; i < s; i++) {
                product.sub(i).append(p);
                if (i < s - 1) {
                    p.append(",");
                }
            }
            p.append(Op.COMPOUND_TERM_CLOSER);
        }


        static void setAppend(Compound set, Appendable p) throws IOException {

            int len = set.subs();

            //duplicated from above, dont want to store this as a field in the class
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
                setsubs.sub(i).append(p);
            }
            p.append(closer);
        }

        static void operationAppend(Compound argsProduct, Atomic operator, Appendable p) throws IOException {

            //Term predTerm = operator.identifier(); //getOperatorTerm();
            //        if ((predTerm.volume() != 1) || (predTerm.hasVar())) {
            //            //if the predicate (operator) of this operation (inheritance) is not an atom, use Inheritance's append format
            //            appendSeparator(p, pretty);
            //            return;
            //        }


            Term[] xt = argsProduct.arrayShared();

            p.append(operator.toString());

            p.append(Op.COMPOUND_TERM_OPENER);

            int n = 0;
            for (Term t : xt) {
                if (n != 0) {
                    p.append(Op.ARGUMENT_SEPARATOR);
                    /*if (pretty)
                        p.append(' ');*/
                }

                t.append(p);


                n++;
            }

            p.append(Op.COMPOUND_TERM_CLOSER);

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
//    /**
//     * serialization and deserialization of terms, tasks, etc.
//     */
//    public static class DefaultCodec extends FSTConfiguration {
//
//        final TermIndex index;
//
//        public DefaultCodec(TermIndex t) {
//            super(null);
//
//            this.index = t;
//
//            createDefaultConfiguration();
//            //setStreamCoderFactory(new FBinaryStreamCoderFactory(this));
//            setForceSerializable(true);
//
//
//            //setCrossPlatform(false);
//            setShareReferences(false);
//            setPreferSpeed(true);
//            setCrossPlatform(false);
//
//
//            registerClass(Atom.class, GenericCompound.class,
//                    AbstractTask.class,
//                    Term[].class,
//                    TermContainer.class,
//                    //long[].class, char.class,
//                    Op.class);
//
//
//            registerSerializer(AbstractTask.class, new FSTBasicObjectSerializer() {
//
//                @Override
//                public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws Exception {
//                }
//
//                @NotNull
//                @Override
//                public Object instantiate(Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws IOException {
//                    return readTask(in, index);
//                }
//
//                @Override
//                public void writeObject(FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
//                    writeTask(out, (Task) toWrite);
//                }
//            }, true);
//
//
//            registerSerializer(Atom.class, terms, true);
//            registerSerializer(Atomic.class, terms, true);
//
//            registerSerializer(GenericCompound.class, terms, true);
//
//            registerSerializer(AtomConcept.class, terms, true);
//            registerSerializer(CompoundConcept.class, terms, true);
//
//        }
//
////        @Nullable
////        final FSTBasicObjectSerializer termContainers = new FSTBasicObjectSerializer() {
////
////            @Override
////            public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws Exception {
////            }
////
////            @Nullable
////            @Override
////            public Object instantiate(Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws IOException {
////                return readTermContainer(in, index);
////            }
////
////            @Override
////            public void writeObject(FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
////                writeTermContainer(out, (TermContainer) toWrite);
////            }
////        };
//
//        @Nullable
//        final FSTBasicObjectSerializer terms = new FSTBasicObjectSerializer() {
//
//            @Override
//            public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws Exception {
//            }
//
//            @Nullable
//            @Override
//            public Object instantiate(Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws IOException {
//                return readTerm(in, index);
//            }
//
//            @Override
//            public void writeObject(FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
//                writeTerm(out, (Term) toWrite);
//            }
//        };
//
//
//    }
//
