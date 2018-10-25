package jcog.io;

import com.google.common.collect.ImmutableList;
import jcog.TODO;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.AbstractColumnType;
import tech.tablesaw.columns.Column;
import tech.tablesaw.columns.StringParser;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * specified semantics of a data record / structure
 * TODO move most of this to 'MutableSchema' implementation of interface Schema
 * 
 * TODO use TableSaw Table
 */
@Deprecated public class Schema extends Table {


//    protected final List<String> attribute_names;
//    protected final Map<String, ARFF.AttributeType> attrTypes;
    protected final Map<String, String[]> nominalCats;

    public Schema(Schema copyMetadataFrom) {
        super("");
        addColumns(columnArray());
//        this.attribute_names = copyMetadataFrom.attribute_names;
//        this.attrTypes = copyMetadataFrom.attrTypes;
        this.nominalCats = copyMetadataFrom.nominalCats;
    }

    public Schema() {
        super("");
//        this.attribute_names = new FasterList<>();
//        this.attrTypes = new HashMap<>();
        this.nominalCats = new HashMap<>();
    }

    public boolean equalSchema(Schema other) {

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
    public Schema define(String name, ColumnType type) {

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
//        addColumns(
//        addColumn(new Column())
//        attrTypes.put(name, type);
//        attribute_names.add(name);

        return this;
    }

    public Schema defineText(String attr) {
        return define(attr, ColumnType.STRING);
    }

    public Schema defineNumeric(String attr) {
        return define(attr, ColumnType.DOUBLE);
    }

    public static class NominalColumnType extends AbstractColumnType {
        public final String[] values;

        public NominalColumnType(String name, String[] values) {
            super("",
                    4,
                    name,
                    name);
            this.values = values;
        }

        @Override
        public Column<?> create(String name) {
            return null;
        }

        @Override
        public StringParser<?> defaultParser() {
            return null;
        }

        @Override
        public StringParser<?> customParser(CsvReadOptions options) {
            return null;
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

    public Schema defineNominal(String nominalAttribute, String... categories) {
        if (categories.length < 2)
            throw new RuntimeException("nominal types require > 1 categories");

//        attribute_names.add(nominalAttribute);
//        attrTypes.put(nominalAttribute, Nominal);
        String[] prev = nominalCats.put(nominalAttribute, categories);
        assert (prev == null);
        return this;
    }


    public static class Instance {
        public final Schema schema;
        public final ImmutableList data;

        public Instance(Schema schema, ImmutableList data) {
            this.schema = schema;
            this.data = data;
        }

        @Override
        public String toString() {
            return schema + " " + data;
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

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Instance)) return false;

            Instance i = (Instance) obj;
            return i.data.equals(data) && i.schema.equals(schema);
        }


    }
}
