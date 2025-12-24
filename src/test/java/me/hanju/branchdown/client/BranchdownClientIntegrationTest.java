package me.hanju.branchdown.client;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import me.hanju.branchdown.api.dto.PointDto;
import me.hanju.branchdown.api.dto.StreamDto;
import okhttp3.OkHttpClient;

/**
 * Branchdown Client 통합 테스트.
 * agenthanju/branchdown:0.2.2 도커 이미지와 MariaDB를 사용.
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BranchdownClientIntegrationTest {

  private static final Network NETWORK = Network.newNetwork();

  @Container
  private static final GenericContainer<?> MARIADB = new GenericContainer<>(DockerImageName.parse("mariadb:11"))
      .withNetwork(NETWORK)
      .withNetworkAliases("mariadb")
      .withExposedPorts(3306)
      .withEnv("MARIADB_DATABASE", "branchdown")
      .withEnv("MARIADB_USER", "branchdown")
      .withEnv("MARIADB_PASSWORD", "branchdown")
      .withEnv("MARIADB_ROOT_PASSWORD", "root")
      .waitingFor(Wait.forLogMessage(".*ready for connections.*", 2)
          .withStartupTimeout(Duration.ofMinutes(2)));

  @Container
  private static final GenericContainer<?> BRANCHDOWN = new GenericContainer<>(
      DockerImageName.parse("agenthanju/branchdown:0.2.2"))
      .withNetwork(NETWORK)
      .withExposedPorts(8080, 8081)
      .withEnv("SPRING_PROFILES_ACTIVE", "prod")
      .withEnv("MARIADB_URL", "jdbc:mariadb://mariadb:3306/branchdown")
      .withEnv("MARIADB_USER", "branchdown")
      .withEnv("MARIADB_PASSWORD", "branchdown")
      .withEnv("DDL_AUTO", "create")
      .dependsOn(MARIADB)
      .waitingFor(Wait.forHttp("/actuator/health")
          .forPort(8081)
          .forStatusCode(200)
          .withStartupTimeout(Duration.ofMinutes(2)));

  private static BranchdownClient client;
  private static Long streamId;
  private static Long rootPointId;
  private static Long childPointId;

  @BeforeAll
  static void setUp() {
    String baseUrl = "http://" + BRANCHDOWN.getHost() + ":" + BRANCHDOWN.getMappedPort(8080);
    OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(10))
        .readTimeout(Duration.ofSeconds(30))
        .build();
    client = new OkHttpBranchdownClient(okHttpClient, baseUrl);
  }

  @Test
  @Order(1)
  void createStream_shouldReturnStreamId() {
    // when
    streamId = client.createStream();

    // then
    assertNotNull(streamId);
    assertTrue(streamId > 0);
  }

  @Test
  @Order(2)
  void getStream_shouldReturnStreamInfo() {
    // when
    StreamDto.Response stream = client.getStream(streamId);

    // then
    assertNotNull(stream);
    assertEquals(streamId, stream.id());
    assertNotNull(stream.createdAt());
  }

  @Test
  @Order(3)
  void getStreamPoints_shouldReturnRootPoint() {
    // when
    List<PointDto.Response> points = client.getStreamPoints(streamId);

    // then
    assertNotNull(points);
    assertEquals(1, points.size());
    rootPointId = points.get(0).id();
    assertNotNull(rootPointId);
  }

  @Test
  @Order(4)
  void addPoint_shouldCreateChildPoint() {
    // when
    PointDto.Response point = client.addPoint(rootPointId, "item-001");

    // then
    assertNotNull(point);
    assertNotNull(point.id());
    assertEquals("item-001", point.itemId());
    childPointId = point.id();
  }

  @Test
  @Order(5)
  void getAncestors_shouldReturnSelfExcludingRoot() {
    // getAncestors는 자신을 포함하고 루트는 제외
    // when
    List<PointDto.Response> ancestors = client.getAncestors(childPointId);

    // then
    assertNotNull(ancestors);
    // 자신(childPoint)만 포함, 루트는 제외되므로 1개
    assertEquals(1, ancestors.size());
    assertEquals(childPointId, ancestors.get(0).id());
    // 루트 포인트는 포함되지 않음
    assertTrue(ancestors.stream().noneMatch(p -> p.id().equals(rootPointId)));
  }

  @Test
  @Order(6)
  void getBranchPoints_withDepth_shouldReturnPointsAfterDepth() {
    // depth 파라미터: 해당 depth보다 큰 포인트만 반환 (p.depth > :depth)
    // depth=-1이면 모든 포인트 반환 (root 포함)
    // depth=0이면 depth > 0인 포인트만 반환 (root 제외)

    // when: depth=-1로 모든 포인트 조회 (root 포함)
    List<PointDto.Response> allPoints = client.getBranchPoints(streamId, 0, -1);

    // then: root(depth=0) + childPoint(depth=1) = 2개
    assertNotNull(allPoints);
    assertEquals(2, allPoints.size());

    // when: depth=0으로 root 제외 조회
    List<PointDto.Response> afterDepth0 = client.getBranchPoints(streamId, 0, 0);

    // then: childPoint(depth=1)만 = 1개
    assertNotNull(afterDepth0);
    assertEquals(1, afterDepth0.size());
    assertEquals(childPointId, afterDepth0.get(0).id());
  }

  @Test
  @Order(7)
  void addPoint_toSameParent_shouldCreateNewBranch() {
    // 같은 부모(root)에 두 번째 포인트 추가 시 새 브랜치 생성
    // given: 이미 Order(4)에서 rootPointId 아래에 item-001 추가됨 (branch 0)

    // when: 같은 root에 또 추가
    PointDto.Response branch1Point = client.addPoint(rootPointId, "item-002");

    // then: 새 브랜치(1)에 생성됨
    assertNotNull(branch1Point);
    assertEquals(1, branch1Point.branchNum()); // 새 브랜치 번호

    // when: 새 브랜치의 포인트 조회
    List<PointDto.Response> branch1Points = client.getBranchPoints(streamId, 1);

    // then: 새 브랜치에는 item-002가 있음
    assertNotNull(branch1Points);
    assertTrue(branch1Points.stream().anyMatch(p -> "item-002".equals(p.itemId())));
  }

  @Test
  @Order(8)
  void deleteStream_shouldSucceed() {
    // when & then: should not throw
    assertDoesNotThrow(() -> client.deleteStream(streamId));
  }

  @Test
  @Order(9)
  void addPoint_withNullItemId_shouldThrowException() {
    // given
    Long newStreamId = client.createStream();
    List<PointDto.Response> points = client.getStreamPoints(newStreamId);
    Long newRootPointId = points.get(0).id();

    // when & then
    assertThrows(IllegalArgumentException.class,
        () -> client.addPoint(newRootPointId, null));

    // cleanup
    client.deleteStream(newStreamId);
  }
}
