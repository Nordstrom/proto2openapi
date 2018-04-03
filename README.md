# Protobuf 3 to Open API 3 Converter
[![Build Status][ci-image]][ci-link] [ ![Download][artifact-image]][artifact-download]

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
      .convert(output);
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
      .convertModel();
  }
}
```

[ci-image]:https://travis-ci.org/Nordstrom/proto2openapi.svg?branch=master
[ci-link]:https://travis-ci.org/Nordstrom/proto2openapi
[artifact-image]:https://api.bintray.com/packages/nordstromoss/oss_maven/proto2openapi/images/download.svg
[artifact-download]:https://bintray.com/nordstromoss/oss_maven/proto2openapi/_latestVersion