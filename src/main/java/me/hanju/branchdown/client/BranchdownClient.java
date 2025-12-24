package me.hanju.branchdown.client;

import java.util.List;

import me.hanju.branchdown.api.dto.PointDto;
import me.hanju.branchdown.api.dto.StreamDto;

/**
 * Branchdown API 클라이언트 인터페이스.
 *
 * @see me.hanju.branchdown.client.exception.BranchdownException 서버에서 에러 응답 반환 시
 * @see me.hanju.branchdown.client.exception.BranchdownClientException 클라이언트 측 오류 발생 시
 */
public interface BranchdownClient {

  // ========== Stream API ==========

  /**
   * 새 스트림을 생성한다.
   *
   * @return 생성된 스트림의 ID
   */
  Long createStream();

  /**
   * 스트림 정보를 조회한다.
   *
   * @param streamId 스트림 ID
   * @return 스트림 정보
   */
  StreamDto.Response getStream(long streamId);

  /**
   * 스트림을 삭제한다.
   *
   * @param streamId 스트림 ID
   */
  void deleteStream(long streamId);

  /**
   * 스트림에 속한 모든 포인트를 조회한다.
   *
   * @param streamId 스트림 ID
   * @return 포인트 목록
   */
  List<PointDto.Response> getStreamPoints(long streamId);

  /**
   * 특정 브랜치에 속한 포인트를 조회한다.
   *
   * @param streamId 스트림 ID
   * @param branchNum 브랜치 번호
   * @return 포인트 목록
   */
  List<PointDto.Response> getBranchPoints(long streamId, int branchNum);

  // ========== Point API ==========

  /**
   * 부모 포인트 아래에 새 포인트를 추가한다.
   *
   * @param parentPointId 부모 포인트 ID
   * @param itemId 아이템 ID (null 불가)
   * @return 생성된 포인트 정보
   * @throws IllegalArgumentException itemId가 null인 경우
   */
  PointDto.Response addPoint(long parentPointId, String itemId);

  /**
   * 포인트의 조상 포인트들을 조회한다.
   *
   * @param pointId 포인트 ID
   * @return 조상 포인트 목록
   */
  List<PointDto.Response> getAncestors(long pointId);
}
