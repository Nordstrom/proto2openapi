package com.nordstrom.proto2openapi;

import static com.squareup.wire.schema.ProtoType.BOOL;
import static com.squareup.wire.schema.ProtoType.BYTES;
import static com.squareup.wire.schema.ProtoType.DOUBLE;
import static com.squareup.wire.schema.ProtoType.FIXED32;
import static com.squareup.wire.schema.ProtoType.FIXED64;
import static com.squareup.wire.schema.ProtoType.FLOAT;
import static com.squareup.wire.schema.ProtoType.INT32;
import static com.squareup.wire.schema.ProtoType.INT64;
import static com.squareup.wire.schema.ProtoType.SFIXED32;
import static com.squareup.wire.schema.ProtoType.SFIXED64;
import static com.squareup.wire.schema.ProtoType.SINT32;
import static com.squareup.wire.schema.ProtoType.SINT64;
import static com.squareup.wire.schema.ProtoType.STRING;
import static com.squareup.wire.schema.ProtoType.UINT32;
import static com.squareup.wire.schema.ProtoType.UINT64;

import com.squareup.wire.schema.EnumType;
import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.ProtoType;
import com.squareup.wire.schema.Rpc;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.Service;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.val;

// "null", "boolean", "object", "array", "number", or "string"
public class ModelConverter {
  private static final String JSON_BOOLEAN = "boolean";
  private static final String JSON_STRING = "string";
  private static final String JSON_NUMBER = "number";
  private static final String JSON_OBJECT = "object";
  private static final String JSON_ARRAY = "array";

  private static final Map<ProtoType, String> SCALAR_TYPES;

  static {
    Map<ProtoType, String> scalarTypes = new LinkedHashMap<>();
    scalarTypes.put(BOOL, JSON_BOOLEAN);
    scalarTypes.put(BYTES, JSON_STRING);
    scalarTypes.put(DOUBLE, JSON_NUMBER);
    scalarTypes.put(FLOAT, JSON_NUMBER);
    scalarTypes.put(FIXED32, JSON_NUMBER);
    scalarTypes.put(FIXED64, JSON_STRING);
    scalarTypes.put(INT32, JSON_NUMBER);
    scalarTypes.put(INT64, JSON_STRING);
    scalarTypes.put(SFIXED32, JSON_NUMBER);
    scalarTypes.put(SFIXED64, JSON_STRING);
    scalarTypes.put(SINT32, JSON_NUMBER);
    scalarTypes.put(SINT64, JSON_STRING);
    scalarTypes.put(STRING, JSON_STRING);
    scalarTypes.put(UINT32, JSON_NUMBER);
    scalarTypes.put(UINT64, JSON_STRING);
    SCALAR_TYPES = Collections.unmodifiableMap(scalarTypes);
  }

  private Schema protoSchema;
  private Map<String, io.swagger.v3.oas.models.media.Schema> oaSchemas = new LinkedHashMap<>();

  public ModelConverter(Schema protoSchema) {
    this.protoSchema = protoSchema;
  }

  public OpenAPI convert() {
    val oa = new OpenAPI();

    val paths =
        protoSchema
            .protoFiles()
            .stream()
            .flatMap(this::toPathItemMapEntries)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    val components = new Components().schemas(oaSchemas);

    val pathsObj = new Paths();
    pathsObj.putAll(paths);
    oa.paths(pathsObj);
    oa.components(components);

    return oa;
  }

  Stream<Map.Entry<String, PathItem>> toPathItemMapEntries(ProtoFile file) {
    return file.services().stream().flatMap(service -> toPathItemMapEntries(file, service));
  }

  Stream<Map.Entry<String, PathItem>> toPathItemMapEntries(ProtoFile file, Service service) {
    return service.rpcs().stream().map(rpc -> toPathItemMapEntry(file, service, rpc));
  }

  Map.Entry<String, PathItem> toPathItemMapEntry(ProtoFile file, Service service, Rpc rpc) {
    val path = String.format("/%s.%s/%s", file.packageName(), service.name(), rpc.name());
    return new AbstractMap.SimpleImmutableEntry<>(path, toPathItem(file, service, rpc));
  }

  PathItem toPathItem(ProtoFile file, Service service, Rpc rpc) {
    return new PathItem().post(toOperation(file, service, rpc));
  }

  Operation toOperation(ProtoFile file, Service service, Rpc rpc) {
    val operationId = String.format("%s.%s.%s", file.packageName(), service.name(), rpc.name());
    val summary = spacify(rpc.name());
    return new Operation()
        .description(rpc.documentation())
        .operationId(operationId)
        .summary(summary)
        .requestBody(toRequestBody(rpc))
        .responses(toResponses(rpc));
  }

  RequestBody toRequestBody(Rpc rpc) {
    return new RequestBody().content(toContent(rpc.requestType()));
  }

  ApiResponses toResponses(Rpc rpc) {
    return new ApiResponses().addApiResponse("200", toResponse(rpc));
  }

  ApiResponse toResponse(Rpc rpc) {
    return new ApiResponse().content(toContent(rpc.responseType()));
  }

  Content toContent(ProtoType type) {
    return new Content()
        .addMediaType("application/json", toJsonMediaType(type))
        .addMediaType("application/protobuf", toProtoMediaType(type));
  }

  MediaType toJsonMediaType(ProtoType type) {
    return new MediaType().schema(toSchema(type));
  }

  MediaType toProtoMediaType(ProtoType type) {
    // TODO: this is a stopgap.  need a better way to represent binary.
    return new MediaType()
        .schema(
            new io.swagger.v3.oas.models.media.Schema<>()
                .name(type.simpleName())
                .type(JSON_OBJECT));
  }

  io.swagger.v3.oas.models.media.Schema toSchema(ProtoType type) {
    val schema = new io.swagger.v3.oas.models.media.Schema<>().name(type.simpleName());

    if (type.isScalar()) {
      return schema.type(scalarType(type)).format(scalarFormat(type));
    }
    if (type.isMap()) {
      return schema.type(JSON_OBJECT);
    }
    return schema.$ref(ref(type));
  }

  io.swagger.v3.oas.models.media.Schema toSchema(Field field) {
    val schema = toSchema(field.type());
    return schema.description(field.documentation());
  }

  io.swagger.v3.oas.models.media.Schema toSchema(MessageType type) {
    return new io.swagger.v3.oas.models.media.Schema<>()
        .description(type.documentation())
        .type("object")
        .required(type.getRequiredFields())
        .properties(type.fields().stream().collect(Collectors.toMap(Field::name, this::toSchema)));
  }

  String ref(ProtoType type) {
    val name = type.simpleName();
    if (!oaSchemas.containsKey(name)) {
      val objType = protoSchema.getType(type);
      if (objType instanceof MessageType) {
        oaSchemas.put(name, toSchema((MessageType) objType));
      } else if (objType instanceof EnumType) {
        // TODO: implement enum support
      }
    }
    return String.format("#/components/schemas/%s", name);
  }

  String scalarType(ProtoType type) {
    return SCALAR_TYPES.get(type);
  }

  String scalarFormat(ProtoType type) {
    return type.simpleName();
  }

  String spacify(String s) {
    val buffer = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      val ch = s.charAt(i);
      if (Character.isUpperCase(ch) && i > 0) {
        buffer.append(" ");
      }
      buffer.append(ch);
    }
    return buffer.toString();
  }
}
