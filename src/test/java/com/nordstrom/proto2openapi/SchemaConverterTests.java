package com.nordstrom.proto2openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SchemaConverterTests {
  private ByteArrayOutputStream output = new ByteArrayOutputStream();
  private SchemaConverter converter = new SchemaConverter();
  private FileSystem fs = FileSystems.getDefault();

  @AfterEach
  void afterEach() throws IOException {
    output.close();
  }

  @Test
  void testSimple() throws IOException, URISyntaxException {
    generateAndCompare("simple");
  }

  @Test
  void testDescriptions() throws IOException, URISyntaxException {
    generateAndCompare("descriptions");
  }

  @Test
  void testTypes() throws IOException, URISyntaxException {
    generateAndCompare("types");
  }

  void generateAndCompare(String filePrefix) throws IOException, URISyntaxException {
    converter.addSource("test-data").addProto(filePrefix + ".proto").generate(output);
    assertEquals(loadFileString("test-data/" + filePrefix + ".yml"), output.toString());
  }

  File file(String fileName) {
    return new File(fileName);
  }

  String loadFileString(String fileName) throws IOException {
    try (val input = new FileInputStream(file(fileName))) {
      return toString(input);
    }
  }

  String toString(InputStream input) throws IOException {
    val result = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int length;
    while ((length = input.read(buffer)) != -1) {
      result.write(buffer, 0, length);
    }
    return result.toString(StandardCharsets.UTF_8.name());
  }
}
