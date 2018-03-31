package com.nordstrom.proto2openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.squareup.wire.schema.SchemaLoader;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import lombok.val;

public class SchemaConverter {
  private final SchemaLoader loader = new SchemaLoader();

  private final ObjectMapper mapper = createMapper();
  private FileSystem fs;

  public SchemaConverter() {
    this(FileSystems.getDefault());
  }

  public SchemaConverter(FileSystem fs) {
    this.fs = fs;
  }

  public SchemaConverter addSource(File file) {
    loader.addSource(file);
    return this;
  }

  public SchemaConverter addSource(Path path) {
    loader.addSource(path);
    return this;
  }

  public SchemaConverter addSource(String path) {
    return addSource(fs.getPath(path));
  }

  public SchemaConverter addProto(String proto) {
    loader.addProto(proto);
    return this;
  }

  public SchemaConverter generate(OutputStream output) throws IOException {
    return generate(new OutputStreamWriter(output));
  }

  public SchemaConverter generate(Writer writer) throws IOException {
    mapper.writeValue(writer, generateModel());

    return this;
  }

  public OpenAPI generateModel() throws IOException {
    val protoSchema = loader.load();
    val converter = new ModelConverter(protoSchema);
    return converter.convert();
  }

  private static ObjectMapper createMapper() {
    val mapper = new ObjectMapper(new YAMLFactory());
    mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    return mapper;
  }
}
