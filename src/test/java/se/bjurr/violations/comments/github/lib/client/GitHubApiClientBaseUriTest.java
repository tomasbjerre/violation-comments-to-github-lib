package se.bjurr.violations.comments.github.lib.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GitHubApiClientBaseUriTest {

  @Test
  void resolvesTheDefaultGitHubComHostToTheApiHost() {
    assertThat(GitHubApiClient.resolveBaseUri("https://github.com/"))
        .isEqualTo("https://api.github.com");
  }

  @Test
  void resolvesTheGistHostToTheApiHost() {
    assertThat(GitHubApiClient.resolveBaseUri("https://gist.github.com/"))
        .isEqualTo("https://api.github.com");
  }

  @Test
  void keepsTheApiHostAsIsOverHttp() {
    assertThat(GitHubApiClient.resolveBaseUri("http://api.github.com/"))
        .isEqualTo("http://api.github.com");
  }

  @Test
  void keepsTheApiHostAsIsOverHttps() {
    assertThat(GitHubApiClient.resolveBaseUri("https://api.github.com/"))
        .isEqualTo("https://api.github.com");
  }

  @Test
  void keepsACustomEnterpriseHostAsIsWithoutAPort() {
    assertThat(GitHubApiClient.resolveBaseUri("https://api.othergithub.com/"))
        .isEqualTo("https://api.othergithub.com");
  }

  @Test
  void keepsACustomEnterpriseHostAndPortAsIs() {
    assertThat(GitHubApiClient.resolveBaseUri("http://othergithub.com:99/"))
        .isEqualTo("http://othergithub.com:99");
  }
}
