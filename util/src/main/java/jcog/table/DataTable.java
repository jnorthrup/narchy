package jcog.table;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import jcog.TODO;
import jcog.data.list.FasterList;
import jcog.learn.decision.FloatTable;
import jcog.util.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.AbstractColumnType;
import tech.tablesaw.columns.AbstractParser;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.io.csv.CsvWriteOptions;
import tech.tablesaw.io.csv.CsvWriter;
import tech.tablesaw.table.Rows;

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * specified semantics of a data record / structure
 * TODO move most of this to 'MutableDataTable' implementation of interface DataTable
 **/
public class DataTable extends Table {


    protected final Map<String, String[]> nominalCats;

    /** see: Table.copy() */
    public DataTable(Table copy) {
        super(copy.name());

        int rc = copy.rowCount();
        for (Column<?> column : copy.columns()) {
            addColumns(column.emptyCopy(rc));
        }

        int[] rows = new int[rc];
        for (int i = 0; i < rc; i++) {
            rows[i] = i;
        }
        Rows.copyRowsToTable(rows, copy, this);
        nominalCats = new HashMap<>();
    }

    public DataTable(DataTable copyMetadataFrom) {
        super("");
        addColumns(columnArray());
//        this.attribute_names = copyMetadataFrom.attribute_names;
//        this.attrTypes = copyMetadataFrom.attrTypes;
        this.nominalCats = copyMetadataFrom.nominalCats;
    }

    public DataTable() {
        super("");
//        this.attribute_names = new FasterList<>();
//        this.attrTypes = new HashMap<>();
        this.nominalCats = new HashMap<>();
    }

    public boolean equalSchema(DataTable other) {

        return (this == other) ||

                (columnTypes().equals(other.columnTypes()) &&
//                (attribute_names.equals(other.attribute_names) &&
//                        attrTypes.equals(other.attrTypes) &&
                        nominalCats.equals(other.nominalCats)
                );
    }

    /**
     * Get the name of an attribute.
     */
    public String attrName(int idx) {
        return column(idx).name();
    }



    /**
     * Get the type of an attribute. Currently, the attribute types are
     * "numeric", "string", and "nominal". For nominal attributes, use getAttributeData()
     * to retrieve the possible values for the attribute.
     */
    public ColumnType attrType(String name) {
        return column(name).type();
    }

    /**
     * Define a new attribute. Type must be one of "numeric", "string", and
     * "nominal". For nominal attributes, the allowed values
     * must also be given. This variant of defineAttribute allows to set this data.
     */
    public DataTable define(String name, ColumnType type) {

        //assert (type != Nominal);
//        if (attrTypes.put(name, type) != null)
//            throw new RuntimeException("column name collision");

        if (type == ColumnType.STRING) {
            addColumns(StringColumn.create(name));
        } else if (type == ColumnType.DOUBLE) {
            addColumns(DoubleColumn.create(name));
        } else {
            throw new TODO();
        }


        return this;
    }

    public DataTable defineText(String attr) {
        return define(attr, ColumnType.STRING);
    }

    public DataTable defineNumeric(String attr) {
        return define(attr, ColumnType.DOUBLE);
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
//        return addAt(Lists.immutable.of(point));
//    }
//
//    public boolean addAt(ImmutableList point) {
        if (point.length != columnCount())
            throw new UnsupportedOperationException("row structure mismatch: provided " + point.length + " != expected " + columnCount());

        List<Column<?>> l = columns();
        for (int i = 0; i < point.length; i++) {
            Object c = point[i];
            if (c instanceof Number) {
                c = ((Number)c).doubleValue(); //come on tablesaw
            }
            l.get(i).appendObj(c);
        }

        return true;
    }

    public boolean isEmpty() {
        return size()==0;
    }

    @Deprecated public FloatTable<String> toFloatTable() {

        FloatTable<String> data = new FloatTable<>(columnNames().toArray(ArrayUtils.EMPTY_STRING_ARRAY) );

        doWithRows(rr -> data.add(toFloatArray(rr)));

        return data;
    }

    public float[] toFloatArray(Row rr) {
        int cols = rr.columnCount();
        float[] r = new float[cols];
        for (int i = 0; i < cols; i++) {
            double v = rr.getDouble(i);
            //r[i] = v!=null ? ((float)v) : Float.NaN;
            r[i] = (float)v;
        }
        return r;
    }

    public final int size() {
        return rowCount();
    }

    public Stream<Instance> stream() {
        return Streams.stream(iterator()).map(this::instance);
    }


    public void printCSV() {
        printCSV(new FilterOutputStream(System.out) {
            @Override
            public void close()  {
                //HACKK dont close it - can cause VM shutdown
            }
        });
    }

    public void printCSV(OutputStream o) {
        new CsvWriter(this, new CsvWriteOptions.Builder(o).header(true).build()).write();
//        String s = data.columnNames().toString();
//        System.out.println(s.substring(/*'['*/ 1, s.length()-1 /*']'*/));
    }

    @Nullable
    public Row maxBy(int column) {
        final double[] bestScore = {Double.NEGATIVE_INFINITY};
        final Row[] best = {null};
        doWithRows( e->{
            double s = e.getDouble(column);
            if (s > bestScore[0]) {
                best[0] = e;
                bestScore[0] = s;
            }
        });
        return best[0];
    }

    public static class NominalColumnType extends AbstractColumnType {
        public final String[] values;

        public NominalColumnType(String name, String[] values) {
            super(4,
                    name,
                    name);
            this.values = values;
        }

        @Override
        public Column<?> create(String name) {
            return null;
        }

        @Override
        public AbstractParser<?> customParser(CsvReadOptions options) {
            throw new TODO();
        }


//        @Override
//        public StringColumn create(String name) {
//            return StringColumn.create(name);
//        }
//
//        @Override
//        public StringStringParser defaultParser() {
//            return new StringStringParser(this);
//        }
//
//        @Override
//        public StringStringParser customParser(CsvReadOptions options) {
//            return new StringStringParser(this, options);
//        }

    }

    public DataTable defineNominal(String nominalAttribute, String... categories) {
        if (categories.length < 2)
            throw new RuntimeException("nominal types require > 1 categories");

//        attribute_names.addAt(nominalAttribute);
//        attrTypes.put(nominalAttribute, Nominal);
        String[] prev = nominalCats.put(nominalAttribute, categories);

        addColumns(StringColumn.create(nominalAttribute));

        assert (prev == null);
        return this;
    }

    @Deprecated private Instance instance(Row x) {

        ColumnType[] ct = columnTypes();
        List<Double> d = new FasterList(ct.length);
        for (int i1 = 0, ctLength = ct.length; i1 < ctLength; i1++) {
//            ColumnType t = ct[i1];
//            if (t instanceof FloatColumnType)
//                d.add((double)x.getFloat(i1));
//            else if (t == DoubleColumnType)
                d.add(x.getDouble(i1));
        }
        return new Instance(ImmutableList.copyOf(d));
    }

    @Deprecated public class Instance {
        public final ImmutableList data;

        public Instance(ImmutableList data) {

            this.data = data;
        }



        @Override
        public String toString() {
            return DataTable.this + " " + data;
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        public double[] toDoubleArray() {
            return toDoubleArray(0, data.size());
        }
        public double[] toDoubleArray(int from, int to) {
            double[] x = new double[to-from];
            int j = 0;
            for (int i = from; i < to; i++) {
                Object o = data.get(i);
                double v;
                if (o instanceof Number) {
                    v = ((Number) o).doubleValue();
                } else {
                    throw new UnsupportedOperationException();
                }
                x[j++] = v;
            }
            return x;
        }

        protected DataTable table() { return DataTable.this; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Instance)) return false;

            Instance i = (Instance) obj;
            return i.data.equals(data) && table().equals(i.table());
        }


    }
}
