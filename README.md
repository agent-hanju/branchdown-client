# Branchdown Java SDK

[Branchdown](https://github.com/agent-hanju/branchdown) API를 위한 Java 클라이언트 라이브러리.

## 요구사항

- Java 21+

## 설치

### Gradle (JitPack)

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.agent-hanju:branchdown-client:0.2.2'
}
```

### 구현체별 의존성

**Spring WebFlux 환경:**

```gradle
implementation 'org.springframework:spring-webflux:6.2.3'
```

**OkHttp 환경 (Spring 비의존):**

```gradle
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
implementation 'com.fasterxml.jackson.core:jackson-databind:2.18.2'
implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2'
```

## 사용법

### WebClient 구현체

```java
WebClient.Builder webClientBuilder = WebClient.builder();
BranchdownClient client = new WebClientBranchdownClient(webClientBuilder, "http://localhost:8080");
```

### OkHttp 구현체

```java
OkHttpClient okHttpClient = new OkHttpClient();
BranchdownClient client = new OkHttpBranchdownClient(okHttpClient, "http://localhost:8080");
```

### API 사용 예시

```java
// 스트림 생성
Long streamId = client.createStream();

// 스트림 조회
StreamDto.Response stream = client.getStream(streamId);

// 포인트 추가
PointDto.Response point = client.addPoint(stream.rootPointId(), "item-123");

// 조상 포인트 조회
List<PointDto.Response> ancestors = client.getAncestors(point.id());

// 스트림 삭제
client.deleteStream(streamId);
```

## 예외 처리

| 예외                        | 설명                                     |
| --------------------------- | ---------------------------------------- |
| `BranchdownException`       | 서버에서 에러 응답 반환                  |
| `BranchdownClientException` | 클라이언트 측 오류 (네트워크, 직렬화 등) |
| `IllegalArgumentException`  | 잘못된 파라미터                          |

```java
try {
    client.getStream(999L);
} catch (BranchdownException e) {
    // 서버 에러 (예: 스트림 없음)
} catch (BranchdownClientException e) {
    // 클라이언트 에러 (예: 네트워크 오류)
}
```

## 관련 프로젝트

- [branchdown](https://github.com/agent-hanju/branchdown) - Branchdown 서버
- [branchdown-api](https://github.com/agent-hanju/branchdown-api) - 공용 DTO 라이브러리

## 라이선스

[MIT License](LICENSE)
