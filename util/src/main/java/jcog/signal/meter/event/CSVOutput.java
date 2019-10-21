package jcog.signal.meter.event;

import com.google.common.base.Joiner;
import jcog.Util;
import jcog.data.iterator.ArrayIterator;

import java.io.*;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * dead simple CSV logging
 */
public class CSVOutput extends PrintStream {

    

    public CSVOutput(File out, String... headers) throws FileNotFoundException {
        this(new FileOutputStream(out), headers);
    }

    public CSVOutput(OutputStream out, String... headers) {
        super(out);
        println(Joiner.on(',').join(StreamSupport.stream(new ArrayIterator<>(headers).spliterator(), false).map(new Function<String, String>() {
                    @Override
                    public String apply(String col) {
                        return '"' + col + '"';
                    }
                }).collect(Collectors.toList())
        ));
    }

    public void out(double... row) {
        StringJoiner joiner = new StringJoiner(",");
        for (double v : row) {
            String s = String.valueOf(v);
            joiner.add(s);
        }
        println(joiner.toString());
    }

    public void out(float... row) {
        out(Util.toDouble(row));
    }

}
