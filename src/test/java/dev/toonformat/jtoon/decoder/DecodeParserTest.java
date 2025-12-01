package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.DecodeOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
public class DecodeParserTest {

    @Test
    @DisplayName("Should parse TOON format primitive array to JSON")
    void parsePrimitiveArray() {
        DecodeParser parser = setUpParser("items[3]: a,\"b,c\",\"d:e\"");
        assertEquals("{items=[a, b,c, d:e]}", parser.parseValue().toString());
    }

    @Test
    @DisplayName("Should parse TOON format tabular array to JSON")
    void parseTabularArray() {
        DecodeParser parser = setUpParser("items[2]{id,name}:\n  1,Alice\n  2,Bob\ncount: 2");
        assertEquals("{items=[{id=1, name=Alice}, {id=2, name=Bob}], count=2}", parser.parseValue().toString());
    }

    @Test
    @DisplayName("Should parse TOON format nested array to JSON")
    void parseNestedArray() {
        DecodeParser parser = setUpParser(
            "items[1]:\n  - users[2]{id,name}:\n      1,Ada\n      2,Bob\n    status: active"
        );
        assertEquals("{items=[{users=[{id=1, name=Ada}, {id=2, name=Bob}], status=active}]}", parser.parseValue().toString());
    }

    @Test
    @DisplayName("Should parse TOON format object to JSON")
    void parseObject() {
        DecodeParser parser = setUpParser("id: 123\nname: Ada\nactive: true");
        assertEquals("{id=123, name=Ada, active=true}", parser.parseValue().toString());
    }

    @Test
    @DisplayName("Should parse TOON format number to JSON")
    void parseNumber() {
        DecodeParser parser = setUpParser("value: 1.5000");
        assertEquals("{value=1.5}", parser.parseValue().toString());
    }

    @Test
    @DisplayName("Should parse TOON format to JSON tolerating whitespaces")
    void parseToleratingSpacesInCommas() {
        DecodeParser parser = setUpParser("tags[3]: a , b , c");
        assertEquals("{tags=[a, b, c]}", parser.parseValue().toString());
    }

    private DecodeParser setUpParser(String toon) {
        return new DecodeParser(toon, DecodeOptions.DEFAULT);
    }
}
