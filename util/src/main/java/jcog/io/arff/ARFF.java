/*
 * Copyright (c) 2008, Mikio L. Braun, Cheng Soon Ong, Soeren Sonnenburg
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 *   * Neither the names of the Technical University of Berlin, ETH
 *   Zürich, or Fraunhofer FIRST nor the names of its contributors may
 *   be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jcog.io.arff;

import com.google.common.primitives.Primitives;
import jcog.Texts;
import jcog.data.ArrayHashSet;
import jcog.list.FasterList;
import jcog.util.Reflect;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.intelligentjava.machinelearning.decisiontree.FloatTable;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static jcog.io.arff.ARFF.AttributeType.*;

/**
 * https:
 *
 * <p>A class for reading and writing Arff-Files.</p>
 *
 * <p>You can either load a file, parse a string or a BufferedReader. Afterwards, you
 * can extract the information with the methods getComment(), getNumberOfAttributes(),
 * getAttributeName(), getAttributeType(), getAttributeData().</p>
 *
 * <p>Alternative, you can construct an empty ArffFile object, and then use setComment(),
 * defineAttribute(), add() to fill in the data and then save to a file with save(), or
 * to a string with write().</p>
 *
 * <p>The first comment in an ArffFile is extracted and made available through the *Comment()
 * accessors. Usually, this comment contains some interesting information about the data set.</p>
 *
 * <p>Currently, the class only supports numerical, string and nominal attributes. It also does
 * not support sparse storage (yet).</p>
 *
 * @author Mikio L. Braun, mikio@cs.tu-berlin.de
 * <p>
 * https:
 * <p>
 * https:
 * https:
 */
public class ARFF extends jcog.io.Schema implements Iterable<ImmutableList> {


    static final String NEW_LINE = System.getProperty("line.separator");
    private static final int COMMENT = 0;
    private static final int HEADER = 1;
    private static final int DATA = 2;
    /**
     * data 'rows'
     * TODO abstract this to different underlying data model
     */


    private String relation;
    private String comment;

    public final Collection<ImmutableList> data;

    protected ARFF(ARFF copyMetadataFrom, Collection<ImmutableList> data) {
        super(copyMetadataFrom);
        this.data = data;
        this.relation = copyMetadataFrom.relation;
        this.comment = copyMetadataFrom.comment;
    }

    /**
     * Parse an ArffFile from a string.
     */
    public ARFF(String l) throws IOException, ARFFParseError {
        this(l, newDefaultDataCollection());
    }

    public ARFF(String l, Collection<ImmutableList> data) throws IOException, ARFFParseError {
        this(new BufferedReader(new StringReader(l)), data);
    }

    /**
     * default data model: concurrent hash set, eliminates duplicate
     * data points but limited in performance/data access methods.
     */
    public ARFF() {
        this(newDefaultDataCollection());
    }


    /**
     * Construct an empty ArffFile.
     */
    public ARFF(Collection<ImmutableList> data) {
        super();
        relation = "data";
        comment = null;
        this.data = data;
    }

    /**
     * Parse an ArffFile from a BufferedReader.
     */
    public ARFF(BufferedReader r, Collection<ImmutableList> data) throws IOException, ARFFParseError {
        this(data);
        int[] state = new int[]{COMMENT};

        StringBuilder collectedComment = new StringBuilder();

        String line;
        int lineno = 1;
        while ((line = r.readLine()) != null) {
            readLine(lineno++, state, line.trim(), collectedComment);
        }
        this.comment = collectedComment.toString();
    }

    static Collection<ImmutableList> newDefaultDataCollection() {
        //return Sets.newConcurrentHashSet();
        return new ArrayHashSet();
    }

    /**
     * Formats an array of Objects in the passed StringBuilder using toString()
     * and using del as the delimiter.
     * <p>
     * For example, on <tt>objects = { 1, 2, 3 }</tt>, and <tt>del = " + "</tt>, you get
     * <tt>"1 + 2 + 3"</tt>.
     */
    private static void joinWith(Object[] objects, Appendable s, CharSequence del) throws IOException {
        boolean first = true;
        for (Object o : objects) {
            if (!first) {
                s.append(del);
            }
            s.append(o.toString());
            first = false;
        }
    }

    private static void joinWith(ImmutableList objects, Appendable s, CharSequence del) throws IOException {
        boolean first = true;
        for (Object o : objects) {
            if (!first)
                s.append(del);

            String oo = o!=null ? o.toString() : "null";

            s.append(o instanceof Number ? oo : quoteIfNecessary(oo));

            first = false;
        }
    }

    static boolean isQuoteNecessary(CharSequence t) {
        int len = t.length();

        if (len > 1 && (t.charAt(0) == '\"') && (t.charAt(len - 1) == '\"'))
            return false; 

        for (int i = 0; i < len; i++) {
            char x = t.charAt(i);
            switch (x) {
                case ' ':
                    return true;
                case ',':
                    return true;
                case '-':
                    return true;
                case '+':
                    return true;
                case '.':
                    return true;
                
            }
        }

        return false;
    }

    public ARFF clone(Collection<ImmutableList> with) {
        return new ARFF(this, with);
    }

    private void readLine(int lineNum, int[] state, String line, StringBuilder collectedComment) throws ARFFParseError {
        int ll = line.length();
        switch (state[0]) {
            case COMMENT:
                if (ll > 1 && line.charAt(0) == '%') {
                    if (ll >= 2)
                        collectedComment.append(line.substring(2));
                    collectedComment.append(NEW_LINE);
                } else {
                    state[0] = HEADER;
                    readLine(lineNum, state, line, collectedComment);
                }
                break;
            case HEADER:
                String lowerline = line.toLowerCase();
                if (lowerline.startsWith("@relation")) {
                    readRelationDefinition(line);
                } else if (lowerline.startsWith("@attribute")) {
                    try {
                        readAttributeDefinition(lineNum, line);
                    } catch (ARFFParseError e) {
                        System.err.println("Warning: " + e.getMessage());
                    }
                } else if (lowerline.startsWith("@data")) {
                    state[0] = DATA;
                }
                break;

            case DATA:
                if (ll > 0 && line.charAt(0) != '%')
                    parseData(lineNum, line);
                break;
        }
    }

    private void readRelationDefinition(String line) {
        int i = line.indexOf(' ');
        relation = line.substring(i + 1);
    }


    private void readAttributeDefinition(int lineno, String line) throws ARFFParseError {
        Scanner s = new Scanner(line);
        Pattern p = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*|\\{[^\\}]+\\}|\\'[^\\']+\\'|\\\"[^\\\"]+\\\"");
//        String keyword = s.findInLine(p);
        String name = s.findInLine(p);
        String type = s.findInLine(p);

        if (name == null || type == null) {
            throw new ARFFParseError(lineno, "Attribute definition cannot be parsed");
        }

        String lowertype = type.toLowerCase();

        if (lowertype.equals("real") || lowertype.equals("numeric") || lowertype.equals("integer")) {
            define(name, Numeric);
        } else if (lowertype.equals("string")) {
            define(name, Text);
        } else if (type.startsWith("{") && type.endsWith("}")) {
            type = type.substring(1, type.length() - 1);
            type = type.trim();
            defineNominal(name, type.split("\\s*,\\s*"));
        } else {
            throw new ARFFParseError(lineno, "Attribute of type \"" + type + "\" not supported (yet)");
        }
    }

    private void parseData(int lineno, String line) throws ARFFParseError {
        int num_attributes = attribute_names.size();
        if (line.charAt(0) == '{' && line.charAt(line.length() - 1) == '}') {
            throw new ARFFParseError(lineno, "Sparse data not supported (yet).");
        } else {
            String[] tokens = line.split("\\s*,\\s*");
            if (tokens.length != num_attributes) {
                throw new ARFFParseError(lineno, "Warning: line " + lineno + " does not contain the right " +
                        "number of elements (should be " + num_attributes + ", got " + tokens.length + '.');
            }

            Object[] datum = new Object[num_attributes];
            for (int i = 0; i < num_attributes; i++) {
                
                String name = attribute_names.get(i);
                switch (attrTypes.get(name)) {
                    case Numeric:
                        datum[i] = Double.parseDouble(tokens[i]);
                        break;
                    case Text:
                        datum[i] = tokens[i];
                        break;
                    case Nominal:
                        if (!isNominalValueValid(name, tokens[i])) {
                            throw new ARFFParseError(lineno, "Undefined nominal value \"" +
                                    tokens[i] + "\" for field " + name + ".");
                        }
                        datum[i] = tokens[i];
                        break;
                }
            }

            add(datum);
        }
    }

    private boolean isNominalValueValid(String name, String token) {
        switch (token) {
            case "?":
                return true; 
            case "_":
                return true; 
        }

        String[] values = nominalCats.get(name);
        boolean found = false;
        for (String value : values) {
            if (value.equals(token)) {
                found = true;
            }
        }
        return found;
    }

    /**
     * Generate a string which describes the data set.
     */
    public StringBuilder describe() {
        StringBuilder s = new StringBuilder();

        try {
            s.append("Relation " + relation).append(NEW_LINE).append("with attributes").append(NEW_LINE);
            for (String n : attribute_names) {
                s.append("   " + n + " of type " + attrTypes.get(n));
                if (attrTypes.get(n) == Nominal) {
                    s.append(" with values ");
                    joinWith(nominalCats.get(n), s, ", ");
                }
                s.append(NEW_LINE);
            }

            s.append(NEW_LINE).append("Data (first 10 lines of " + data.size() + "):").append(NEW_LINE);

            int i = 0;
            for (ImmutableList row : data) {
                
                joinWith(row, s, ", ");
                s.append(NEW_LINE);
                if (i++ > 10) break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return s;
    }

    /**
     * Write the ArffFile to a string.
     */
    public void write(Appendable s) throws IOException {

        if (comment != null) {
            s.append("% ").append(comment.replaceAll(NEW_LINE, NEW_LINE + "% ")).append(NEW_LINE);
        }

        s.append("@relation ").append(relation).append(NEW_LINE);

        for (String name : attribute_names) {
            s.append("@attribute ").append(quoteIfNecessary(name)).append(" ");

            switch (attrTypes.get(name)) {
                case Numeric:
                    s.append("numeric");
                    break;
                case Text:
                    s.append("string");
                    break;
                case Nominal:
                    s.append("{");
                    joinWith(nominalCats.get(name), s, ",");
                    s.append("}");
                    break;
            }
            s.append(NEW_LINE);
        }

        s.append("@data").append(NEW_LINE);

        for (ImmutableList datum : data) {
            joinWith(datum, s, ",");
            s.append(NEW_LINE);
        }


    }

    public static String quoteIfNecessary(String name) {
        return isQuoteNecessary(name) ? Texts.quote(name) : name;
    }

    /**
     * Save the data into a file.
     */
    public void writeToFile(String filename) throws IOException {
        try (FileWriter w = new FileWriter(filename)) {
            write(w);
            w.flush();
        }
    }

    /**
     * Get the name of the relation.
     */
    public String getRelation() {
        return relation;
    }

    /**
     * Set the name of the relation.
     */
    public void setRelation(String relation) {
        this.relation = relation;
    }

    /**
     * Get the initial comment of the relation.
     */
    public String getComment() {
        return comment;
    }

    /**
     * Set the initial comment of the relation.
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /** TODO use StringBuilder or something */
    public void addComment(String s) {
        this.comment += s;
    }


    /**
     * Get additional information on the attribute. This data is used for
     * nominal attributes to define the possible values.
     */
    public String[] categories(String nominalAttributeName) {
        return nominalCats.get(nominalAttributeName);
    }

    /**
     * Add a data point
     * TODO check data type of each point component
     */
    public boolean add(Object... point) {
        return add(Lists.immutable.of(point)); 
    }

    public boolean add(ImmutableList point) {
        if (point.size() != attribute_names.size())
            throw new UnsupportedOperationException("row structure mismatch: provided " + point.size() + " != expected " + attribute_names.size());
        return this.data.add(point);
    }

    public Iterator<ImmutableList> iterator() {
        return data.iterator();
    }

    public void print() {
        try {
            write(System.out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Stream<ImmutableList> stream() {
        return data.stream();
    }

    public boolean addAll(ARFF incoming) {
        if (this == incoming)
            return false;
        if (!equalSchema(incoming)) {
            print();
            incoming.print();
            throw new RuntimeException("schemas differ");
        }
        final boolean[] changed = {false};
        incoming.forEach(p -> {
            changed[0] |= add(p);
        });
        return changed[0];
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Deprecated public FloatTable<String> toFloatTable() {

        FloatTable<String> data = new FloatTable<>(this.attrNames() );

        int cols = data.cols.length;

        for (ImmutableList exp : this.data) {
            float[] r = new float[cols];
            for (int i = 0; i < cols; i++)
                r[i] = ((Number) exp.get(i)).floatValue();
            data.add(r);
        }

        return data;
    }

//    public FloatTable<String> toFloatTable(int... columns) {
//
//        Arrays.sort(columns);
//
//        int n = columns.length;
//        String[] names = new String[n];
//        int i = 0;
//        for (int c : columns) {
//            names[i++] = attrName(c);
//        }
//        FloatTable<String> data = new FloatTable<>(names);
//
//
//        for (ImmutableList exp : this.data) {
//            float[] r = new float[n];
//            int k = 0;
//            for (int c : columns)
//                r[k++] = ((Number) exp.get(c)).floatValue();
//            data.add(r);
//        }
//
//        return data;
//
//    }



    public enum AttributeType {
        /*
        boolean?
        numeric
        integer is treated as numeric
        real is treated as numeric
         */
        Numeric,

        Text,

        Nominal

        /*
        TODO
        date [<date-format>]
        relational for multi-instance data (for future use)
        */
    }

    public static class ARFFParseError extends Exception {

        /**
         * Construct a new ArffFileParseErrro object.
         */
        ARFFParseError(int lineno, String string) {
            super("Parse error line " + lineno + ": " + string);
        }

    }

    /**
     * ARFF that is defined by and can be bound to/from a simple POJO
     */
    public static class ARFFObject<X> extends ARFF {

        final static Logger logger = LoggerFactory.getLogger(ARFFObject.class);
        private final Function[] extractor;

        /**
         * TODO hints for extracting Nominals
         */
        public ARFFObject(Class<X> c) {
            Reflect C = Reflect.on(c);

            FasterList<Function<X, ?>> extractor = new FasterList();

            for (Map.Entry<String, Reflect> e : C.fields(true, false).entrySet()) {
                String n = e.getKey();

                Field field = e.getValue().get();
                field.trySetAccessible();

                Class<?> t = Primitives.wrap(field.getType());
                if (Byte.class.isAssignableFrom(t) || Short.class.isAssignableFrom(t) || Integer.class.isAssignableFrom(t) || Long.class.isAssignableFrom(t)) {
                    defineNumeric(n);
                    extractor.add((x) -> {
                        try {
                            return ((Number) field.get(x)).longValue();
                        } catch (IllegalAccessException e1) {
                            logger.error("field {} : {}", e1);
                            return Double.NaN;
                        }
                    });

                } else if (Boolean.class.isAssignableFrom(t)) {
                    defineNominal(n, "true", "false");
                    extractor.add((x) -> {
                        try {
                            return Boolean.toString( ((Boolean) field.get(x)).booleanValue() );
                        } catch (IllegalAccessException e1) {
                            logger.error("field {} : {}", e1);
                            return null;
                        }
                    });
                } else if (Number.class.isAssignableFrom(t)) {

                    defineNumeric(n);
                    extractor.add((x) -> {
                        try {
                            return ((Number) field.get(x)).doubleValue();
                        } catch (IllegalAccessException e1) {
                            logger.error("field {} : {}", e1);
                            return Double.NaN;
                        }
                    });
                } else {
                    
                    defineText(n);
                    extractor.add((x) -> {
                        try {
                            return field.get(x).toString();
                        } catch (IllegalAccessException e1) {
                            logger.error("field {} : {}", e1);
                            return "?";
                        }
                    });
                }

            }

            if (extractor.size() == 0)
                throw new RuntimeException("no fields accessed");

            this.extractor = extractor.toArrayRecycled(Function[]::new);
        }

        public boolean put(X x) {
            int n = attrCount();
            Object[] o = new Object[n];
            for (int i = 0; i < n; i++) {
                o[i] = extractor[i].apply(x);
            }
            return add(o);
        }
    }


    public void saveOnShutdown(String file) {
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            try {
                writeToFile(file);
                System.out.println("saved " + data.size() + " experiment results to: " + file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }

    @Nullable
    public ImmutableList maxBy(int column) {
        double bestScore = Double.NEGATIVE_INFINITY;
        ImmutableList best = null;
        for (ImmutableList e: data) {
            double s = ((Number) e.get(column)).doubleValue();
            if (s > bestScore) {
                best = e;
                bestScore = s;
            }
        }
        return best;
    }

}