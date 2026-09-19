package se.bjurr.violations.comments.github.lib.client.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(
    value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
    justification = "Read by Jackson via reflection when serializing the request body")
public class CreateIssueCommentRequest {
  public String body;

  public CreateIssueCommentRequest(final String body) {
    this.body = body;
  }
}
