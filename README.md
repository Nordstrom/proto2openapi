# Protobuf 3 to Open API 3 Converter

This converts proto3 files to Open API 3 schemas.

## Install

TBD

## Usage

Convert `proto-directory/my-proto.proto` to a FileOutputStream as follows.

```java
import com.nordstrom.proto2openapi.SchemaConverter;

class Example {
  void main(String[] args) {
    FileOutputStream output = new FileOutputStream("openapi-output.yml");
    SchemaConverter converter = new SchemaConverter();
    converter
      .addSource("proto-directory")
      .addProto("my-proto.proto")
      .generate(output);
  }
}
```

Convert `proto-directory/my-proto.proto` to a `OpenAPI` object model as follows.

```java
import com.nordstrom.proto2openapi.SchemaConverter;
import io.swagger.v3.oas.models.OpenAPI;

class Example {
  void main(String[] args) {
    SchemaConverter converter = new SchemaConverter();
    OpenAPI model = converter
      .addSource("proto-directory")
      .addProto("my-proto.proto")
      .generateModel();
  }
}
```
