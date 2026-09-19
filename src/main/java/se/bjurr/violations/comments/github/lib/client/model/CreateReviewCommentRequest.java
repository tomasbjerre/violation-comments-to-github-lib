package se.bjurr.violations.comments.github.lib.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(
    value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
    justification = "Read by Jackson via reflection when serializing the request body")
public class CreateReviewCommentRequest {
  public String body;

  @JsonProperty("commit_id")
  public String commitId;

  public String path;
  public int position;

  public CreateReviewCommentRequest(
      final String body, final String commitId, final String path, final int position) {
    this.body = body;
    this.commitId = commitId;
    this.path = path;
    this.position = position;
  }
}
