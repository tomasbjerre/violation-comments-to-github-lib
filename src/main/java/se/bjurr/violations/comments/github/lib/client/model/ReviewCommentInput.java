package se.bjurr.violations.comments.github.lib.client.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(
    value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
    justification = "Read by Jackson via reflection when serializing the request body")
public class ReviewCommentInput {
  public String path;
  public int position;
  public String body;

  public ReviewCommentInput(final String path, final int position, final String body) {
    this.path = path;
    this.position = position;
    this.body = body;
  }
}
