package me.hanju.branchdown.client;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import me.hanju.branchdown.api.dto.CommonResponseDto;
import me.hanju.branchdown.api.dto.PointDto;
import me.hanju.branchdown.api.dto.StreamDto;
import me.hanju.branchdown.client.exception.BranchdownClientException;
import me.hanju.branchdown.client.exception.BranchdownException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * OkHttp 기반 Branchdown API 클라이언트 구현체.
 * Spring 비의존 환경에서 사용 가능.
 */
public class OkHttpBranchdownClient implements BranchdownClient {

  private static final MediaType JSON = MediaType.get("application/json");
  private static final TypeReference<CommonResponseDto<StreamDto.Response>> STREAM_RESPONSE = new TypeReference<>() {
  };
  private static final TypeReference<CommonResponseDto<PointDto.Response>> POINT_RESPONSE = new TypeReference<>() {
  };
  private static final TypeReference<CommonResponseDto<List<PointDto.Response>>> POINT_LIST_RESPONSE = new TypeReference<>() {
  };
  private static final TypeReference<CommonResponseDto<Void>> VOID_RESPONSE = new TypeReference<>() {
  };

  private final OkHttpClient client;
  private final ObjectMapper objectMapper;
  private final String baseUrl;

  public OkHttpBranchdownClient(final OkHttpClient client, final String baseUrl) {
    if (client == null) {
      throw new IllegalArgumentException("client must not be null");
    }
    if (baseUrl == null) {
      throw new IllegalArgumentException("baseUrl must not be null");
    }
    this.client = client;
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    this.objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  // ========== Stream API ==========

  @Override
  public Long createStream() {
    try {
      final Request request = new Request.Builder()
          .url(baseUrl + "/api/streams")
          .post(RequestBody.create("", JSON))
          .build();
      return this.execute(request, STREAM_RESPONSE).id();
    } catch (JsonProcessingException e) {
      throw new BranchdownClientException("Failed to serialize/deserialize", e);
    } catch (IOException e) {
      throw new BranchdownClientException("Request failed", e);
    }
  }

  @Override
  public StreamDto.Response getStream(final long streamId) {
    try {
      final Request request = new Request.Builder()
          .url(baseUrl + "/api/streams/" + streamId)
          .get()
          .build();
      return this.execute(request, STREAM_RESPONSE);
    } catch (JsonProcessingException e) {
      throw new BranchdownClientException("Failed to serialize/deserialize", e);
    } catch (IOException e) {
      throw new BranchdownClientException("Request failed", e);
    }
  }

  @Override
  public void deleteStream(final long streamId) {
    try {
      final Request request = new Request.Builder()
          .url(baseUrl + "/api/streams/" + streamId)
          .delete()
          .build();
      this.execute(request, VOID_RESPONSE);
    } catch (JsonProcessingException e) {
      throw new BranchdownClientException("Failed to serialize/deserialize", e);
    } catch (IOException e) {
      throw new BranchdownClientException("Request failed", e);
    }
  }

  @Override
  public List<PointDto.Response> getStreamPoints(final long streamId) {
    try {
      final Request request = new Request.Builder()
          .url(baseUrl + "/api/streams/" + streamId + "/points")
          .get()
          .build();
      return this.execute(request, POINT_LIST_RESPONSE);
    } catch (JsonProcessingException e) {
      throw new BranchdownClientException("Failed to serialize/deserialize", e);
    } catch (IOException e) {
      throw new BranchdownClientException("Request failed", e);
    }
  }

  @Override
  public List<PointDto.Response> getBranchPoints(final long streamId, final int branchNum) {
    try {
      final Request request = new Request.Builder()
          .url(baseUrl + "/api/streams/" + streamId + "/branches/" + branchNum + "/points")
          .get()
          .build();
      return this.execute(request, POINT_LIST_RESPONSE);
    } catch (JsonProcessingException e) {
      throw new BranchdownClientException("Failed to serialize/deserialize", e);
    } catch (IOException e) {
      throw new BranchdownClientException("Request failed", e);
    }
  }

  // ========== Point API ==========

  @Override
  public PointDto.Response addPoint(final long parentPointId, final String itemId) {
    if (itemId == null) {
      throw new IllegalArgumentException("itemId must not be null");
    }
    try {
      final String json = objectMapper.writeValueAsString(new PointDto.DownRequest(itemId));
      final Request request = new Request.Builder()
          .url(baseUrl + "/api/points/" + parentPointId + "/down")
          .post(RequestBody.create(json, JSON))
          .build();
      return this.execute(request, POINT_RESPONSE);
    } catch (JsonProcessingException e) {
      throw new BranchdownClientException("Failed to serialize/deserialize", e);
    } catch (IOException e) {
      throw new BranchdownClientException("Request failed", e);
    }
  }

  @Override
  public List<PointDto.Response> getAncestors(final long pointId) {
    try {
      final Request request = new Request.Builder()
          .url(baseUrl + "/api/points/" + pointId + "/ancestors")
          .get()
          .build();
      return this.execute(request, POINT_LIST_RESPONSE);
    } catch (JsonProcessingException e) {
      throw new BranchdownClientException("Failed to serialize/deserialize", e);
    } catch (IOException e) {
      throw new BranchdownClientException("Request failed", e);
    }
  }

  // ========== Helper Methods ==========

  private <T> T execute(final Request request, final TypeReference<CommonResponseDto<T>> typeRef) throws IOException {
    try (Response response = client.newCall(request).execute()) {
      final String body = response.body() != null ? response.body().string() : "";
      final CommonResponseDto<T> commonResponse = this.objectMapper.readValue(body, typeRef);
      return this.unwrap(commonResponse);
    }
  }

  private <T> T unwrap(final CommonResponseDto<T> response) {
    if (response == null) {
      throw new BranchdownException("Empty response");
    }
    if (!response.success()) {
      throw new BranchdownException(response.message());
    }
    return response.data();
  }
}
