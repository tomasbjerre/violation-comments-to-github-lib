package se.bjurr.violations.comments.github.lib.client;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import se.bjurr.violations.comments.github.lib.client.GitHubInvoker.Method;
import se.bjurr.violations.comments.github.lib.client.model.CreateIssueCommentRequest;
import se.bjurr.violations.comments.github.lib.client.model.CreateReviewCommentRequest;
import se.bjurr.violations.comments.github.lib.client.model.GitHubCommentDto;
import se.bjurr.violations.comments.github.lib.client.model.GitHubCommitDto;
import se.bjurr.violations.comments.github.lib.client.model.GitHubFileDto;
import se.bjurr.violations.lib.ViolationsLogger;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

public class GitHubApiClient {
  private static final String HOST_DEFAULT = "github.com";
  private static final String HOST_GISTS = "gist.github.com";
  private static final String HOST_API = "api.github.com";
  private static final String SEGMENT_V3_API = "/api/v3";
  private static final int PAGE_SIZE = 100;

  private static final JsonMapper JSON_MAPPER =
      JsonMapper.builder()
          .changeDefaultVisibility(vc -> vc.withFieldVisibility(Visibility.ANY))
          .build();

  private static GitHubInvoker gitHubInvoker = new GitHubInvoker();

  public static void setGitHubInvoker(final GitHubInvoker gitHubInvoker) {
    GitHubApiClient.gitHubInvoker = gitHubInvoker;
  }

  private final ViolationsLogger violationsLogger;
  private final String oAuth2Token;
  private final String username;
  private final String password;
  private final String repositoryOwner;
  private final String repositoryName;
  private final String baseUri;
  private final String pathPrefix;

  public GitHubApiClient(
      final ViolationsLogger violationsLogger,
      final String gitHubUrl,
      final String oAuth2Token,
      final String username,
      final String password,
      final String repositoryOwner,
      final String repositoryName) {
    this.violationsLogger = violationsLogger;
    this.oAuth2Token = oAuth2Token;
    this.username = username;
    this.password = password;
    this.repositoryOwner = repositoryOwner;
    this.repositoryName = repositoryName;
    this.baseUri = resolveBaseUri(gitHubUrl);
    this.pathPrefix = isPublicApiHost(gitHubUrl) ? "" : SEGMENT_V3_API;
  }

  private static boolean isPublicApiHost(final String gitHubUrl) {
    try {
      final String hostname = new URL(gitHubUrl).getHost();
      return HOST_DEFAULT.equals(hostname)
          || HOST_GISTS.equals(hostname)
          || HOST_API.equals(hostname);
    } catch (final Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Resolves the scheme+host(+port) to use for the GitHub API, given a GitHub URL. {@code
   * github.com} and {@code gist.github.com} are rewritten to {@code api.github.com}. Any other
   * host, such as a GitHub Enterprise instance, is kept as-is; requests to it are prefixed with
   * {@code /api/v3}.
   */
  public static String resolveBaseUri(final String gitHubUrl) {
    try {
      final URL url = new URL(gitHubUrl);
      String hostname = url.getHost();
      if (HOST_DEFAULT.equals(hostname) || HOST_GISTS.equals(hostname)) {
        hostname = HOST_API;
      }
      final StringBuilder uri = new StringBuilder(url.getProtocol());
      uri.append("://").append(hostname);
      if (url.getPort() > 0) {
        uri.append(':').append(url.getPort());
      }
      return uri.toString();
    } catch (final Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  private String repoPath() {
    return this.baseUri
        + this.pathPrefix
        + "/repos/"
        + this.repositoryOwner
        + "/"
        + this.repositoryName;
  }

  private GitHubResponse invoke(final String url, final Method method, final String postContent) {
    return gitHubInvoker.invoke(
        this.violationsLogger,
        url,
        method,
        postContent,
        this.oAuth2Token,
        this.username,
        this.password);
  }

  private GitHubResponse invokeChecked(
      final String url, final Method method, final String postContent) {
    final GitHubResponse response = this.invoke(url, method, postContent);
    if (!response.isSuccessful()) {
      throw new GitHubApiException(
          method.name(), url, response.getStatusCode(), response.getBody());
    }
    return response;
  }

  private <T> List<T> getAllPages(final String path, final TypeReference<List<T>> type) {
    final List<T> all = new ArrayList<>();
    int page = 1;
    while (true) {
      final String url =
          path + (path.contains("?") ? "&" : "?") + "per_page=" + PAGE_SIZE + "&page=" + page;
      final GitHubResponse response = this.invokeChecked(url, Method.GET, null);
      final List<T> pageResult = JSON_MAPPER.readValue(response.getBody(), type);
      all.addAll(pageResult);
      if (pageResult.size() < PAGE_SIZE) {
        break;
      }
      page++;
    }
    return all;
  }

  public List<GitHubCommitDto> getCommits(final int pullRequestId) {
    return this.getAllPages(
        this.repoPath() + "/pulls/" + pullRequestId + "/commits",
        new TypeReference<List<GitHubCommitDto>>() {});
  }

  public List<GitHubFileDto> getFiles(final int pullRequestId) {
    return this.getAllPages(
        this.repoPath() + "/pulls/" + pullRequestId + "/files",
        new TypeReference<List<GitHubFileDto>>() {});
  }

  public List<GitHubCommentDto> getReviewComments(final int pullRequestId) {
    return this.getAllPages(
        this.repoPath() + "/pulls/" + pullRequestId + "/comments",
        new TypeReference<List<GitHubCommentDto>>() {});
  }

  public List<GitHubCommentDto> getIssueComments(final int pullRequestId) {
    return this.getAllPages(
        this.repoPath() + "/issues/" + pullRequestId + "/comments",
        new TypeReference<List<GitHubCommentDto>>() {});
  }

  public GitHubCommentDto createIssueComment(final int pullRequestId, final String body) {
    final String postContent = JSON_MAPPER.writeValueAsString(new CreateIssueCommentRequest(body));
    final GitHubResponse response =
        this.invokeChecked(
            this.repoPath() + "/issues/" + pullRequestId + "/comments", Method.POST, postContent);
    return JSON_MAPPER.readValue(response.getBody(), GitHubCommentDto.class);
  }

  public GitHubCommentDto createReviewComment(
      final int pullRequestId,
      final String body,
      final String commitId,
      final String path,
      final int position) {
    final String postContent =
        JSON_MAPPER.writeValueAsString(
            new CreateReviewCommentRequest(body, commitId, path, position));
    final GitHubResponse response =
        this.invokeChecked(
            this.repoPath() + "/pulls/" + pullRequestId + "/comments", Method.POST, postContent);
    return JSON_MAPPER.readValue(response.getBody(), GitHubCommentDto.class);
  }

  public void deleteReviewComment(final long commentId) {
    this.invokeChecked(this.repoPath() + "/pulls/comments/" + commentId, Method.DELETE, null);
  }

  public void deleteIssueComment(final long commentId) {
    this.invokeChecked(this.repoPath() + "/issues/comments/" + commentId, Method.DELETE, null);
  }
}
