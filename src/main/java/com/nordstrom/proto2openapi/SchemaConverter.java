/*
 * Copyright 2018 Nordstrom, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

/**
 * This converts a set of proto files in source directories representing a protobuf 3 schema to
 * either a OpenAPI object model or to OpenAPI yaml writen to an OutputStream or Writer.
 */
public class SchemaConverter {
  private final SchemaLoader loader = new SchemaLoader();

  private final ObjectMapper mapper = createMapper();
  private final FileSystem fs;

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

  public SchemaConverter convert(OutputStream output) throws IOException {
    return convert(new OutputStreamWriter(output));
  }

  public SchemaConverter convert(Writer writer) throws IOException {
    mapper.writeValue(writer, convert());

    return this;
  }

  public OpenAPI convert() throws IOException {
    val protoSchema = loader.load();
    return ModelConverter.convert(protoSchema);
  }

  private static ObjectMapper createMapper() {
    val mapper = new ObjectMapper(new YAMLFactory());
    mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    return mapper;
  }
}
