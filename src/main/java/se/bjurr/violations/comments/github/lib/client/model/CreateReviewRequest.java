package se.bjurr.violations.comments.github.lib.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

@SuppressFBWarnings(
    value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
    justification = "Read by Jackson via reflection when serializing the request body")
public class CreateReviewRequest {
  @JsonProperty("commit_id")
  public String commitId;

  public String event;
  public List<ReviewCommentInput> comments;

  public CreateReviewRequest(
      final String commitId, final String event, final List<ReviewCommentInput> comments) {
    this.commitId = commitId;
    this.event = event;
    this.comments = comments;
  }
}
