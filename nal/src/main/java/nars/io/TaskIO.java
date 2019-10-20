package nars.io;

import com.google.common.io.ByteArrayDataOutput;
import nars.Op;
import nars.Task;
import nars.task.AbstractCommandTask;
import nars.task.NALTask;
import nars.term.Term;
import nars.truth.Truth;

import java.io.DataInput;
import java.io.IOException;

import static nars.Op.COMMAND;

/** TODO */
public enum TaskIO {
	;

	static boolean hasTruth(byte punc) {
        return (int) punc == (int) Op.BELIEF || (int) punc == (int) Op.GOAL;
    }

    public static Task readTask(DataInput in) throws IOException {

        byte punc = in.readByte();

        Term preterm = TermIO.the.read(in);

        Term term = preterm.normalize();
        if (term == null)
            throw new IOException("un-normalizable task target");

        if ((int) punc != (int) COMMAND) {
            Truth truth = hasTruth(punc) ? readTruth(in) : null;

            long start = in.readLong();
            long end = in.readLong();

            long[] evi = readEvidence(in);

            float pri = in.readFloat();

            long cre = in.readLong();

			return ((Task) NALTask.the(term, punc, truth, cre, start, end, evi)).pri(pri);
		} else {
            return new AbstractCommandTask(term);
        }
    }

    private static long[] readEvidence(DataInput in) throws IOException {
        int eviLength = (int) in.readByte();
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
        return readTask(IO.input(b));
    }

	/**
	 * with Term first
	 */
	static void write(Task t, ByteArrayDataOutput out, boolean budget, boolean creation)  {


        byte p = t.punc();
		out.writeByte((int) p);


		TermIO.the.write(t.term(), out);


		if ((int) p != (int) COMMAND) {
			if (hasTruth(p))
				Truth.write(t.truth(), out);

			//TODO use delta zig zag encoding (with creation time too)
			out.writeLong(t.start());
			out.writeLong(t.end());

			IO.writeEvidence(out, t.stamp());

			if (budget)
				IO.writeBudget(out, t);

			if (creation)
				out.writeLong(t.creation());
		}

	}
}
