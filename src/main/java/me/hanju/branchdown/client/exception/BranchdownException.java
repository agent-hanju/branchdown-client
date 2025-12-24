package me.hanju.branchdown.client.exception;

/** Branchdown API에서 에러 응답을 반환한 경우 */
public class BranchdownException extends RuntimeException {

  public BranchdownException(String message) {
    super(message);
  }
}
