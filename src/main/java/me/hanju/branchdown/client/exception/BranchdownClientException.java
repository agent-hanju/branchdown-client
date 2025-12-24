package me.hanju.branchdown.client.exception;

/** 클라이언트 측에서 발생한 Exception일 경우 */
public class BranchdownClientException extends RuntimeException {

  public BranchdownClientException(String message, Throwable cause) {
    super(message, cause);
  }
}
