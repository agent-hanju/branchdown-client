package me.hanju.branchdown.client;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.CodecException;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import me.hanju.branchdown.api.dto.CommonResponseDto;
import me.hanju.branchdown.api.dto.PointDto;
import me.hanju.branchdown.api.dto.StreamDto;
import me.hanju.branchdown.client.exception.BranchdownClientException;
import me.hanju.branchdown.client.exception.BranchdownException;

/**
 * WebClient 기반 Branchdown API 클라이언트 구현체.
 * Spring WebFlux 환경 및 @LoadBalanced 지원.
 */
public class WebClientBranchdownClient implements BranchdownClient {

  private static final ParameterizedTypeReference<CommonResponseDto<StreamDto.Response>> STREAM_RESPONSE = new ParameterizedTypeReference<>() {
  };
  private static final ParameterizedTypeReference<CommonResponseDto<PointDto.Response>> POINT_RESPONSE = new ParameterizedTypeReference<>() {
  };
  private static final ParameterizedTypeReference<CommonResponseDto<List<PointDto.Response>>> POINT_LIST_RESPONSE = new ParameterizedTypeReference<>() {
  };
  private static final ParameterizedTypeReference<CommonResponseDto<Void>> VOID_RESPONSE = new ParameterizedTypeReference<>() {
  };

  private final WebClient webClient;

  public WebClientBranchdownClient(final WebClient.Builder webClientBuilder, final String baseUrl) {
    if (webClientBuilder == null) {
      throw new IllegalArgumentException("webClientBuilder must not be null");
    }
    if (baseUrl == null) {
      throw new IllegalArgumentException("baseUrl must not be null");
    }
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  // ========== Stream API ==========

  @Override
  public Long createStream() {
    try {
      final CommonResponseDto<StreamDto.Response> response = this.webClient.post()
          .uri("/api/streams")
          .contentType(MediaType.APPLICATION_JSON)
          .retrieve()
          .bodyToMono(STREAM_RESPONSE)
          .block();
      return this.unwrap(response).id();
    } catch (BranchdownException e) {
      throw e;
    } catch (CodecException e) {
      throw new BranchdownClientException("Failed to serialize/deserialize", e);
    } catch (WebClientException e) {
      throw new BranchdownClientException("Request failed", e);
    }
  }

  @Override
  public StreamDto.Response getStream(final long streamId) {
    try {
      final CommonResponseDto<StreamDto.Response> response = this.webClient.get()
          .uri("/api/streams/{id}", streamId)
          .retrieve()
          .bodyToMono(STREAM_RESPONSE)
          .block();
      return this.unwrap(response);
    } catch (BranchdownException e) {
      throw e;
    } catch (CodecException e) {
      throw new BranchdownClientException("Failed to serialize/deserialize", e);
    } catch (WebClientException e) {
      throw new BranchdownClientException("Request failed", e);
    }
  }

  @Override
  public void deleteStream(final long streamId) {
    try {
      final CommonResponseDto<Void> response = this.webClient.delete()
          .uri("/api/streams/{id}", streamId)
          .retrieve()
          .bodyToMono(VOID_RESPONSE)
          .block();
      this.unwrap(response);
    } catch (BranchdownException e) {
      throw e;
    } catch (CodecException e) {
      throw new BranchdownClientException("Failed to serialize/deserialize", e);
    } catch (WebClientException e) {
      throw new BranchdownClientException("Request failed", e);
    }
  }

  @Override
  public List<PointDto.Response> getStreamPoints(final long streamId) {
    try {
      final CommonResponseDto<List<PointDto.Response>> response = this.webClient.get()
          .uri("/api/streams/{id}/points", streamId)
          .retrieve()
          .bodyToMono(POINT_LIST_RESPONSE)
          .block();
      return this.unwrap(response);
    } catch (BranchdownException e) {
      throw e;
    } catch (CodecException e) {
      throw new BranchdownClientException("Failed to serialize/deserialize", e);
    } catch (WebClientException e) {
      throw new BranchdownClientException("Request failed", e);
    }
  }

  @Override
  public List<PointDto.Response> getBranchPoints(final long streamId, final int branchNum, final int depth) {
    try {
      final CommonResponseDto<List<PointDto.Response>> response = this.webClient.get()
          .uri("/api/streams/{id}/branches/{branchNum}/points?depth={depth}", streamId, branchNum, depth)
          .retrieve()
          .bodyToMono(POINT_LIST_RESPONSE)
          .block();
      return this.unwrap(response);
    } catch (BranchdownException e) {
      throw e;
    } catch (CodecException e) {
      throw new BranchdownClientException("Failed to serialize/deserialize", e);
    } catch (WebClientException e) {
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
      final CommonResponseDto<PointDto.Response> response = this.webClient.post()
          .uri("/api/points/{id}/down", parentPointId)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(new PointDto.DownRequest(itemId))
          .retrieve()
          .bodyToMono(POINT_RESPONSE)
          .block();
      return this.unwrap(response);
    } catch (BranchdownException e) {
      throw e;
    } catch (CodecException e) {
      throw new BranchdownClientException("Failed to serialize/deserialize", e);
    } catch (WebClientException e) {
      throw new BranchdownClientException("Request failed", e);
    }
  }

  @Override
  public List<PointDto.Response> getAncestors(final long pointId) {
    try {
      final CommonResponseDto<List<PointDto.Response>> response = this.webClient.get()
          .uri("/api/points/{id}/ancestors", pointId)
          .retrieve()
          .bodyToMono(POINT_LIST_RESPONSE)
          .block();
      return this.unwrap(response);
    } catch (BranchdownException e) {
      throw e;
    } catch (CodecException e) {
      throw new BranchdownClientException("Failed to serialize/deserialize", e);
    } catch (WebClientException e) {
      throw new BranchdownClientException("Request failed", e);
    }
  }

  // ========== Helper Methods ==========

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
