package org.boon.json;

import org.boon.json.implementation.ObjectMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.boon.Boon.puts;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by mstipanov on 28.05.2014..
 */
public class JsonCaseInsensitiveReaderFieldsPropertiesTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() throws Exception {
        objectMapper = new ObjectMapperImpl(new JsonParserFactory().useFieldsFirst().caseInsensitiveFields(), new JsonSerializerFactory().useFieldsFirst());
    }

    @Test
    public void test_caseInsensitiveProperty_lowercase() {
        String json = "{\"typename\":\"Processes\",\"fields\":[{\"name\":\"process\",\"type\":\"ConversionRateProcess[]\",\"properties\":[\"REQUIRED\"]}]} ";
        ApiDynamicType map = objectMapper.fromJson(json, ApiDynamicType.class);
        puts(json);
        puts(objectMapper.toJson(map));
        assertThat(objectMapper.fromJson(objectMapper.toJson(map)), is(objectMapper.fromJson("{\"typeName\":\"Processes\",\"fields\":[{\"name\":\"process\",\"type\":\"ConversionRateProcess[]\",\"properties\":[\"REQUIRED\"]}]}")));
    }

    @Test
    public void test_caseInsensitiveProperty_normal() {
        String json = "{\"typeName\":\"Processes\",\"fields\":[{\"name\":\"process\",\"type\":\"ConversionRateProcess[]\",\"properties\":[\"REQUIRED\"]}]} ";
        ApiDynamicType map = objectMapper.fromJson(json, ApiDynamicType.class);
        puts(json);
        puts(objectMapper.toJson(map));
        assertThat(objectMapper.fromJson(objectMapper.toJson(map)), is(objectMapper.fromJson("{\"typeName\":\"Processes\",\"fields\":[{\"name\":\"process\",\"type\":\"ConversionRateProcess[]\",\"properties\":[\"REQUIRED\"]}]}")));
    }

    @Test
    public void test_caseInsensitiveProperty_uppercase() {
        String json = "{\"TYPENAME\":\"Processes\",\"fields\":[{\"name\":\"process\",\"type\":\"ConversionRateProcess[]\",\"properties\":[\"REQUIRED\"]}]} ";
        ApiDynamicType map = objectMapper.fromJson(json, ApiDynamicType.class);
        puts(json);
        puts(objectMapper.toJson(map));
        assertThat(objectMapper.fromJson(objectMapper.toJson(map)), is(objectMapper.fromJson("{\"typeName\":\"Processes\",\"fields\":[{\"name\":\"process\",\"type\":\"ConversionRateProcess[]\",\"properties\":[\"REQUIRED\"]}]}")));
    }

    public enum ApiMethodParameterProperty {

        REQUIRED
    }

    public class ApiDynamicType {

        private String typeName;

        private ApiDynamicTypeField[] fields;

        public ApiDynamicType() {
        }

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }
    }

    public class ApiDynamicTypeField {

        private String name;

        private String type;

        private ApiMethodParameterProperty[] properties;

        private String[] allowedValues;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
