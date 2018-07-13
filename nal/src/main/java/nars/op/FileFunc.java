package nars.op;

import nars.$;
import nars.term.Term;
import nars.term.atom.Atomic;

import java.nio.file.Path;

public class FileFunc {


    /** file system path */
    public final static Atomic File = Atomic.the("file");

    static Term theComponent(Path file) {

        int n = file.getNameCount();

        Term[] t = new Term[n];
        for (int i = 0; i < n; i++)
            t[i] = $.the(file.getName(i).toString());

        return $.pRecurse(true, t);
    }

    public static Term the(Path file) {
        return $.inh(theComponent(file), File);
    }
}
