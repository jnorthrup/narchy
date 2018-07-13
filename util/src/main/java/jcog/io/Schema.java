package jcog.io;

import jcog.io.arff.ARFF;
import jcog.data.list.FasterList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static jcog.io.arff.ARFF.AttributeType.*;

/**
 * specified semantics of a data record / structure
 * TODO move most of this to 'MutableSchema' implementation of interface Schema
 */
public class Schema {

    protected final List<String> attribute_names;
    protected final Map<String, ARFF.AttributeType> attrTypes;
    protected final Map<String, String[]> nominalCats;

    public Schema(Schema copyMetadataFrom) {
        this.attribute_names = copyMetadataFrom.attribute_names;
        this.attrTypes = copyMetadataFrom.attrTypes;
        this.nominalCats = copyMetadataFrom.nominalCats;
    }

    public Schema() {
        this.attribute_names = new FasterList<>();
        this.attrTypes = new HashMap<>();
        this.nominalCats = new HashMap<>();
    }

    public boolean equalSchema(Schema other) {
        return (this == other) ||

               ( attribute_names.equals(other.attribute_names) &&
                 attrTypes.equals(other.attrTypes) &&
                 nominalCats.equals(other.nominalCats)
        );
    }

    /**
     * Get the number of attributes.
     */
    public int attrCount() {
        return attribute_names.size();
    }

    /**
     * Get the name of an attribute.
     */
    public String attrName(int idx) {
        return attribute_names.get(idx);
    }

    public boolean hasAttr(String name) { return attribute_names.contains(name); }

    /**
     * Get the type of an attribute. Currently, the attribute types are
     * "numeric", "string", and "nominal". For nominal attributes, use getAttributeData()
     * to retrieve the possible values for the attribute.
     */
    public ARFF.AttributeType attrType(String name) {
        return attrTypes.get(name);
    }

    public ARFF.AttributeType attrType(int n) {
        return attrTypes.get(attrName(n));
    }
    public String[] attrNames() {
        return attribute_names.toArray(new String[0]);
    }

    /**
     * Define a new attribute. Type must be one of "numeric", "string", and
     * "nominal". For nominal attributes, the allowed values
     * must also be given. This variant of defineAttribute allows to set this data.
     */
    public Schema define(String name, ARFF.AttributeType type) {

        assert (type != Nominal);
        if (attrTypes.put(name, type)!=null)
            throw new RuntimeException("column name collision");

        attrTypes.put(name, type);
        attribute_names.add(name);

        return this;
    }

    public Schema defineText(String attr) {
        return define(attr, Text);
    }

    public Schema defineNumeric(String attr) {
        return define(attr, Numeric);
    }

    public Schema defineNominal(String nominalAttribute, String... categories) {
        if (categories.length < 2)
            throw new RuntimeException("nominal types require > 1 categories");

        attribute_names.add(nominalAttribute);
        attrTypes.put(nominalAttribute, Nominal);
        String[] prev = nominalCats.put(nominalAttribute, categories);
        assert (prev == null);
        return this;
    }


}
