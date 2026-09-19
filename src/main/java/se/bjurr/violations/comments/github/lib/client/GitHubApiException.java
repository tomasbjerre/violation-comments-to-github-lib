package se.bjurr.violations.comments.github.lib.client;

public class GitHubApiException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final int statusCode;
  private final String responseBody;

  public GitHubApiException(
      final String method, final String url, final int statusCode, final String responseBody) {
    super(
        method
            + " "
            + url
            + " failed with status "
            + statusCode
            + (responseBody == null || responseBody.isEmpty() ? "" : ":\n" + responseBody));
    this.statusCode = statusCode;
    this.responseBody = responseBody;
  }

  public int getStatusCode() {
    return this.statusCode;
  }

  public String getResponseBody() {
    return this.responseBody;
  }
}
