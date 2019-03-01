package nars.io;

import jcog.io.BytesInput;
import nars.Op;
import nars.Task;
import nars.task.CommandTask;
import nars.task.NALTask;
import nars.term.Term;
import nars.truth.Truth;

import java.io.DataInput;
import java.io.IOException;

import static nars.Op.COMMAND;

/** TODO */
public class TaskIO {
    static boolean hasTruth(byte punc) {
        return punc == Op.BELIEF || punc == Op.GOAL;
    }

    public static Task readTask(DataInput in) throws IOException {

        byte punc = in.readByte();

        Term preterm = TermIO.the.read(in);

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
     * WARNING
     */
    public static Task bytesToTask(byte[] b) throws IOException {
        return readTask(input(b));
    }

    static DataInput input(byte[] b) {
        return new BytesInput(b);
    }
}
