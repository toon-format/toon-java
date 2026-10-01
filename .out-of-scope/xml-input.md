# XML Input

JToon doesn't read XML. There is no `encodeXml` and no XML normalizer in the library, and there won't be.

## Why this is out of scope

TOON carries the JSON data model ([§2](https://github.com/toon-format/spec/blob/main/SPEC.md#2-data-model)), and [§3](https://github.com/toon-format/spec/blob/main/SPEC.md#3-encoding-normalization-reference-encoder) requires a documented mapping from host values into it. XML has no single mapping: attributes versus child elements, one element versus several with the same name, text next to child elements, and all-text leaves are each a choice that depends on the document. The tests in [#27](https://github.com/toon-format/toon-java/pull/27) show it – `employee` became a tabular array in a department with two employees and a plain object in one with a single employee.

A converter in JToon would fix one mapping for every caller and pull `jackson-dataformat-xml` into every install, while Jackson's `XmlMapper` already reads a document into a `Map` with options that fit it, ready for `JToon.encode`.

On [#25](https://github.com/toon-format/toon-java/pull/25#issuecomment-3516912524), @felipestanzani asked for a concrete case: "Toon is intended to pass data to LLMs. I don't see a real scenario where would be needed to convert XML to toon. What is the real case where it would be used?" The scenarios later listed in [#28](https://github.com/toon-format/toon-java/discussions/28) – legacy systems, SOAP responses, XML logs – are all served by `XmlMapper`.

## Prior requests

- [#25](https://github.com/toon-format/toon-java/pull/25) – "XML to TOON Conversion in JToon" (`encodeXml` plus `XmlNormalizer`)
- [#27](https://github.com/toon-format/toon-java/pull/27) – "XML to TOON" (the same change, resubmitted)
- [#28](https://github.com/toon-format/toon-java/discussions/28) – "We can use XML to TOON conversion ?"
