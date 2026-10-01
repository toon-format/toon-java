# XML Input

JToon doesn't read XML. There is no `encodeXml` and no XML normalizer in the library, and there won't be.

## Why this is out of scope

TOON carries the JSON data model ([§2](https://github.com/toon-format/spec/blob/main/SPEC.md#2-data-model)), and [§3](https://github.com/toon-format/spec/blob/main/SPEC.md#3-encoding-normalization-reference-encoder) requires encoders to map every host value into that model with a documented mapping. XML has no single mapping into it. Attributes versus child elements, one element versus several with the same name, text next to child elements, every leaf being text – each of these is a choice, and the right answer depends on the document.

The tests added in [#27](https://github.com/toon-format/toon-java/pull/27) show the problem. In one document, `employees.employee` came out as a tabular array for a department with two employees and as a plain object for a department with one, and the attribute `id="123"` became the string `"123"`:

```
departments:
  department[2]:
    - name: Engineering
      employees:
        employee[2]{name,role}:
          Alice,Developer
          Bob,Manager
    - name: Marketing
      employees:
        employee:
          name: Carol
          role: Director
```

A converter in JToon would have to pick one of these answers for every caller, keep it stable in the public API, and pull `jackson-dataformat-xml` into every install. The caller is in a better position: Jackson's `XmlMapper` reads the document into a `Map` with whatever mapping options fit the data, and `JToon.encode` takes the result as is:

```java
Map<?, ?> data = new XmlMapper().readValue(xml, Map.class);
String toon = JToon.encode(data);
```

When [#25](https://github.com/toon-format/toon-java/pull/25) came in, @felipestanzani asked for a concrete case: "Toon is intended to pass data to LLMs. I don't see a real scenario where would be needed to convert XML to toon. What is the real case where it would be used?" The follow-up in [#28](https://github.com/toon-format/toon-java/discussions/28) listed generic scenarios – legacy systems, SOAP responses, XML logs – and the two lines above cover every one of them.

## Prior requests

- [#25](https://github.com/toon-format/toon-java/pull/25) – "XML to TOON Conversion in JToon" (`encodeXml` plus `XmlNormalizer`)
- [#27](https://github.com/toon-format/toon-java/pull/27) – "XML to TOON" (the same change, resubmitted)
- [#28](https://github.com/toon-format/toon-java/discussions/28) – "We can use XML to TOON conversion ?"
