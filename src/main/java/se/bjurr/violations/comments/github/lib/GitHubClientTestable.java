package se.bjurr.violations.comments.github.lib;

import org.eclipse.egit.github.core.client.GitHubClient;

public class GitHubClientTestable extends GitHubClient {
  public GitHubClientTestable(final String hostname, final int port, final String scheme) {
    super(hostname, port, scheme);
  }

  public String getBaseUri() {
    return this.baseUri;
  }
}
