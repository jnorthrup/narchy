package org.boon.bugs;

import org.boon.json.JsonFactory;
import org.boon.json.JsonParserFactory;
import org.boon.json.JsonSerializerFactory;
import org.boon.json.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
//import static junit.framework.Assertions.assertEquals;
import static org.boon.Boon.puts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Created by rhightower on 4/18/15.
 */
public class Bug312 {

    public class MyClass {

        private String string = "foo";

        private int integer = 1;

        private Map<String, String> map;
    }

    @Test
    public void test() {
        MyClass myClass = new MyClass();
        final String json = JsonFactory.toJson(myClass);
        final MyClass myClass1 = JsonFactory.fromJson(json, MyClass.class);
        assertEquals("foo", myClass1.string);
        assertEquals(1, myClass1.integer);
        assertNull(myClass1.map);
    }

    @Test
    public void testWithMap() {
        MyClass myClass = new MyClass();
        myClass.map = new HashMap<>();
        myClass.map.put("foo", "bar");
        final String json = JsonFactory.toJson(myClass);
        final MyClass myClass1 = JsonFactory.fromJson(json, MyClass.class);
        assertEquals("foo", myClass1.string);
        assertEquals(1, myClass1.integer);
        assertEquals("bar", myClass1.map.get("foo"));
    }

    @Test
    public void testWithMapUsingFactory() {
        JsonParserFactory jsonParserFactory = new JsonParserFactory().useFieldsFirst().lax().setCharset(//Set the standard charset, defaults to UTF_8
        StandardCharsets.UTF_8).setLazyChop(//similar to chop but only does it after map.get
        true);
        JsonSerializerFactory jsonSerializerFactory = new JsonSerializerFactory().useFieldsFirst().useJsonFormatForDates().includeEmpty().includeNulls().includeDefaultValues().handleComplexBackReference().setHandleSimpleBackReference(//looks for simple back reference for parent
        true).setCacheInstances(//turns on caching for immutable objects
        true);
        final ObjectMapper objectMapper = JsonFactory.create(jsonParserFactory, jsonSerializerFactory);
        MyClass myClass = new MyClass();
        final String json = objectMapper.toJson(myClass);
        puts(json);
        final MyClass myClass1 = objectMapper.readValue(json, MyClass.class);
        assertEquals("foo", myClass1.string);
        assertEquals(1, myClass1.integer);
        assertNull(myClass1.map);
    }
}
