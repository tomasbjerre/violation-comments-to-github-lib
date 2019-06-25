package se.bjurr.violations.comments.github.lib;

import static org.assertj.core.api.Assertions.assertThat;
import static se.bjurr.violations.comments.github.lib.GitHubCommentsProvider.getGitHubClient;

import org.junit.Test;

public class GitHubCommentsProviderTest {

  @Test
  public void testGetGitHubClient() {
    assertThat(getGitHubClient("http://api.github.com/").getBaseUri()) //
        .isEqualTo("http://api.github.com");
    assertThat(getGitHubClient("https://api.github.com/").getBaseUri()) //
        .isEqualTo("https://api.github.com");
    assertThat(getGitHubClient("https://api.othergithub.com/").getBaseUri()) //
        .isEqualTo("https://api.othergithub.com");
    assertThat(getGitHubClient("http://othergithub.com:99/").getBaseUri()) //
        .isEqualTo("http://othergithub.com:99");
  }
}
