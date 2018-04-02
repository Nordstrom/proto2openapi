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

import com.google.common.collect.ImmutableMap;
import com.squareup.wire.schema.EnumConstant;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.val;

/**
 * This converts the wire protobuf Schema model to the swagger v3 OpenAPI model. Essentially it
 * converts from protobuf 3 to OpenAPI (Swagger 3) in object form.
 *
 * <p>This converts the proto model by looping through all rpc calls. For each call it generates the
 * following path/operation. <code>POST /package.Service/RpcCall</code>
 *
 * <p>It converts the proto messages used in the rpc call to referenced schemas added to the
 * components section of the OpenAPI specification. As it encounters a proto type, if the type is a
 * complex type it walks down each field until it reaches a scalar type, converting to OpenAPI
 * schemas along the way.
 */
public class ModelConverter {
  private static final String JSON_BOOLEAN = "boolean";
  private static final String JSON_STRING = "string";
  private static final String JSON_NUMBER = "number";
  private static final String JSON_OBJECT = "object";
  private static final String JSON_ARRAY = "array";

  private static final ImmutableMap<ProtoType, String> SCALAR_TYPES =
      ImmutableMap.<ProtoType, String>builder()
          .put(BOOL, JSON_BOOLEAN)
          .put(BYTES, JSON_STRING)
          .put(DOUBLE, JSON_NUMBER)
          .put(FLOAT, JSON_NUMBER)
          .put(FIXED32, JSON_NUMBER)
          .put(FIXED64, JSON_STRING)
          .put(INT32, JSON_NUMBER)
          .put(INT64, JSON_STRING)
          .put(SFIXED32, JSON_NUMBER)
          .put(SFIXED64, JSON_STRING)
          .put(SINT32, JSON_NUMBER)
          .put(SINT64, JSON_STRING)
          .put(STRING, JSON_STRING)
          .put(UINT32, JSON_NUMBER)
          .put(UINT64, JSON_STRING)
          .build();

  private final Schema protoSchema;
  private final Map<String, io.swagger.v3.oas.models.media.Schema> oaSchemas =
      new LinkedHashMap<>();

  private ModelConverter(Schema protoSchema) {
    this.protoSchema = protoSchema;
  }

  /**
   * Convert protobuf 3 schema object to OpenAPI schema object.
   *
   * @param protoSchema protobuf schema object
   * @return a converted OpenAPI schema object
   */
  public static OpenAPI convert(Schema protoSchema) {
    return new ModelConverter(protoSchema).convert();
  }

  private OpenAPI convert() {
    val oa = new OpenAPI();

    val paths =
        protoSchema
            .protoFiles()
            .stream()
            .flatMap(this::rpcToPaths)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // oaSchemas should have been populated while resolving rpc calls above.
    // TODO: validate that we have at least one schema.
    val components = new Components().schemas(oaSchemas);

    val pathsObj = new Paths();
    pathsObj.putAll(paths);
    oa.paths(pathsObj);
    oa.components(components);

    return oa;
  }

  private Stream<Map.Entry<String, PathItem>> rpcToPaths(final ProtoFile file) {
    return file.services().stream().flatMap(service -> rpcToPaths(file, service));
  }

  private Stream<Map.Entry<String, PathItem>> rpcToPaths(
      final ProtoFile file, final Service service) {
    return service.rpcs().stream().map(rpc -> rpcToPath(file, service, rpc));
  }

  private Map.Entry<String, PathItem> rpcToPath(
      final ProtoFile file, final Service service, final Rpc rpc) {
    val path = String.format("/%s.%s/%s", file.packageName(), service.name(), rpc.name());
    val pathItem = new PathItem().post(rpcToOperation(file, service, rpc));
    return new AbstractMap.SimpleImmutableEntry<>(path, pathItem);
  }

  private Operation rpcToOperation(final ProtoFile file, final Service service, final Rpc rpc) {
    val operationId = String.format("%s.%s.%s", file.packageName(), service.name(), rpc.name());
    val summary = spacify(rpc.name());
    val requestBody = new RequestBody().content(typeToContent(rpc.requestType()));
    val response = new ApiResponse().content(typeToContent(rpc.responseType()));
    val responses = new ApiResponses().addApiResponse("200", response);
    return new Operation()
        .description(rpc.documentation())
        .operationId(operationId)
        .summary(summary)
        .requestBody(requestBody)
        .responses(responses);
  }

  private Content typeToContent(final ProtoType type) {
    return new Content()
        .addMediaType("application/json", typeToJsonMediaType(type))
        .addMediaType("application/protobuf", typeToProtoMediaType(type));
  }

  private MediaType typeToJsonMediaType(final ProtoType type) {
    return new MediaType().schema(typeToSchema(type));
  }

  private MediaType typeToProtoMediaType(final ProtoType type) {
    // TODO: this is a stopgap.  need a better way to represent binary.
    return new MediaType()
        .schema(
            new io.swagger.v3.oas.models.media.Schema<>()
                .name(type.simpleName())
                .type(JSON_OBJECT));
  }

  private io.swagger.v3.oas.models.media.Schema typeToSchema(final ProtoType type) {
    val schema = new io.swagger.v3.oas.models.media.Schema<>().name(type.simpleName());

    if (type.isScalar()) {
      return schema.type(scalarType(type)).format(scalarFormat(type));
    }
    if (type.isMap()) {
      return schema.type(JSON_OBJECT);
    }
    return schema.$ref(typeToRef(type));
  }

  private io.swagger.v3.oas.models.media.Schema fieldToSchema(final Field field) {
    val schema = typeToSchema(field.type());
    return schema.description(field.documentation());
  }

  private String typeToRef(final ProtoType type) {
    val name = String.format("%s.%s", type.enclosingTypeOrPackage(), type.simpleName());
    if (!oaSchemas.containsKey(name)) {
      val objType = protoSchema.getType(type);
      if (objType instanceof MessageType) {
        oaSchemas.put(name, messageTypeToSchema((MessageType) objType));
      } else if (objType instanceof EnumType) {
        oaSchemas.put(name, enumTypeToSchema((EnumType) objType));
      } else {
        throw new IllegalArgumentException("Invalid proto type " + type);
      }
    }
    return String.format("#/components/schemas/%s", name);
  }

  private io.swagger.v3.oas.models.media.Schema messageTypeToSchema(final MessageType type) {
    return new io.swagger.v3.oas.models.media.Schema<>()
        .description(type.documentation())
        .type(JSON_OBJECT)
        .required(type.getRequiredFields())
        .properties(
            type.fields().stream().collect(Collectors.toMap(Field::name, this::fieldToSchema)));
  }

  private io.swagger.v3.oas.models.media.Schema enumTypeToSchema(final EnumType type) {
    val schema =
        new io.swagger.v3.oas.models.media.Schema<>()
            .description(type.documentation())
            .type(JSON_STRING);
    schema.setEnum(type.constants().stream().map(EnumConstant::name).collect(Collectors.toList()));
    return schema;
  }

  private String scalarType(final ProtoType type) {
    return SCALAR_TYPES.get(type);
  }

  private String scalarFormat(final ProtoType type) {
    return type.simpleName();
  }

  private String spacify(String s) {
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
