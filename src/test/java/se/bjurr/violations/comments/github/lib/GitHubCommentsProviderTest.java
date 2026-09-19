package se.bjurr.violations.comments.github.lib;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static se.bjurr.violations.comments.github.lib.ViolationCommentsToGitHubApi.violationCommentsToGitHubApi;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import se.bjurr.violations.comments.lib.model.ChangedFile;
import se.bjurr.violations.comments.lib.model.Comment;
import se.bjurr.violations.lib.ViolationsLogger;
import se.bjurr.violations.lib.util.PatchParserUtil;
import tools.jackson.databind.json.JsonMapper;

/**
 * Integration tests that replay, via WireMock, request/response pairs recorded from the real GitHub
 * REST API. Every fixture under {@code src/test/resources/github-api} except {@code
 * issue-comment-422-known-bug.json} is an unmodified capture: a throwaway branch and pull request
 * (#12) were pushed to {@code tomasbjerre/violation-comments-to-github-lib}, exercised with {@code
 * gh api} for every scenario below (including a real 422 from an out-of-range diff position), and
 * then deleted. {@code issue-comment-422-known-bug.json} reconstructs the exact error shape from
 * the bug report this rewrite fixes ("Validation Failed (422): Error with 'data' field in
 * IssueComment resource") — that specific failure is a serialization defect in the old {@code
 * org.eclipse.egit.github.core} client, not something reproducible by sending well-formed JSON, so
 * it could not be captured live.
 *
 * <p>WireMock never runs on a host named {@code api.github.com}/{@code github.com}/{@code
 * gist.github.com}, so every request below also exercises the GitHub Enterprise {@code /api/v3} URL
 * prefix; the public-host (no-prefix) branch is covered by {@link GitHubApiClientBaseUriTest}.
 */
class GitHubCommentsProviderTest {

  private static final String OWNER = "tomasbjerre";
  private static final String REPO = "violation-comments-to-github-lib";
  private static final int PR_ID = 12;
  private static final String REPO_PATH = "/api/v3/repos/" + OWNER + "/" + REPO;
  private static final String LAST_COMMIT_SHA = "24043046096d8dd3e3362e7311cc1ba9b203bfef";
  private static final String EXAMPLE_PATCH =
      "@@ -0,0 +1,5 @@\n+line one\n+line two\n+line three\n+line four\n+line five";

  private static final JsonMapper JSON_MAPPER =
      JsonMapper.builder()
          .changeDefaultVisibility(vc -> vc.withFieldVisibility(Visibility.ANY))
          .build();

  @RegisterExtension static WireMockExtension wireMock = WireMockExtension.newInstance().build();

  private ViolationsLogger violationsLogger;
  private List<String> severeLogs;

  @BeforeEach
  void setUp() {
    this.wireMock.resetAll();
    this.severeLogs = new ArrayList<>();
    this.violationsLogger =
        new ViolationsLogger() {
          @Override
          public void log(final Level level, final String string) {
            if (level == Level.SEVERE) {
              GitHubCommentsProviderTest.this.severeLogs.add(string);
            }
          }

          @Override
          public void log(final Level level, final String string, final Throwable t) {
            if (level == Level.SEVERE) {
              GitHubCommentsProviderTest.this.severeLogs.add(string + " " + t.getMessage());
            }
          }
        };
    this.wireMock.stubFor(
        get(urlPathEqualTo(REPO_PATH + "/pulls/" + PR_ID + "/commits"))
            .willReturn(okJson(fixture("commits.json"))));
  }

  private static String fixture(final String name) {
    try {
      return Files.readString(Path.of("src/test/resources/github-api", name));
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private ViolationCommentsToGitHubApi newApi() {
    return violationCommentsToGitHubApi()
        .withGitHubUrl(this.wireMock.baseUrl() + "/")
        .withoAuth2Token("test-token")
        .withRepositoryOwner(OWNER)
        .withRepositoryName(REPO)
        .withPullRequestId(PR_ID);
  }

  private GitHubCommentsProvider newProvider(final ViolationCommentsToGitHubApi api) {
    return new GitHubCommentsProvider(this.violationsLogger, api);
  }

  private String lastRequestBody(final RequestPatternBuilder pattern) {
    final List<LoggedRequest> requests = this.wireMock.findAll(pattern);
    assertThat(requests).isNotEmpty();
    return new String(requests.get(requests.size() - 1).getBody(), StandardCharsets.UTF_8);
  }

  @Test
  void constructorResolvesTheLastCommitFromTheRealPullRequest() {
    this.wireMock.stubFor(
        post(urlPathEqualTo(REPO_PATH + "/pulls/" + PR_ID + "/comments"))
            .willReturn(okJson(fixture("review-comment-create.json"))));

    final GitHubCommentsProvider provider = this.newProvider(this.newApi());
    final ChangedFile file = new ChangedFile("example.txt", List.of(EXAMPLE_PATCH));
    provider.createSingleFileComment(file, 3, "comment");

    final Map<?, ?> body =
        JSON_MAPPER.readValue(
            this.lastRequestBody(
                postRequestedFor(urlPathEqualTo(REPO_PATH + "/pulls/" + PR_ID + "/comments"))),
            Map.class);
    assertThat(body.get("commit_id")).isEqualTo(LAST_COMMIT_SHA);
  }

  @Test
  void createCommentPostsAnIssueComment() {
    this.wireMock.stubFor(
        post(urlPathEqualTo(REPO_PATH + "/issues/" + PR_ID + "/comments"))
            .willReturn(okJson(fixture("issue-comment-create.json"))));

    final GitHubCommentsProvider provider = this.newProvider(this.newApi());
    provider.createComment("Recorded test comment from violation-comments-to-github-lib rewrite");

    final String body =
        this.lastRequestBody(
            postRequestedFor(urlPathEqualTo(REPO_PATH + "/issues/" + PR_ID + "/comments")));
    final Map<?, ?> parsed = JSON_MAPPER.readValue(body, Map.class);
    assertThat(parsed.get("body"))
        .isEqualTo("Recorded test comment from violation-comments-to-github-lib rewrite");
    assertThat(this.severeLogs).isEmpty();
  }

  @Test
  void createCommentEscapesTrickyContentCorrectly() {
    this.wireMock.stubFor(
        post(urlPathEqualTo(REPO_PATH + "/issues/" + PR_ID + "/comments"))
            .willReturn(okJson(fixture("issue-comment-create.json"))));
    final String trickyComment =
        "Bad practice: `if (x == null)` \"quoted\" 🚀\\backslash\nSecond line\twith tab";

    final GitHubCommentsProvider provider = this.newProvider(this.newApi());
    provider.createComment(trickyComment);

    final String rawBody =
        this.lastRequestBody(
            postRequestedFor(urlPathEqualTo(REPO_PATH + "/issues/" + PR_ID + "/comments")));
    final Map<?, ?> parsed = JSON_MAPPER.readValue(rawBody, Map.class);
    assertThat(parsed.get("body")).isEqualTo(trickyComment);
  }

  @Test
  void createCommentLogsSevereOnValidationFailureInsteadOfThrowing() {
    this.wireMock.stubFor(
        post(urlPathEqualTo(REPO_PATH + "/issues/" + PR_ID + "/comments"))
            .willReturn(
                aResponse()
                    .withStatus(422)
                    .withHeader("Content-Type", "application/json; charset=utf-8")
                    .withBody(fixture("issue-comment-422-known-bug.json"))));

    final GitHubCommentsProvider provider = this.newProvider(this.newApi());

    provider.createComment("a comment that GitHub rejects");

    assertThat(this.severeLogs).isNotEmpty();
    assertThat(this.severeLogs.get(0)).contains("422").contains("IssueComment");
  }

  @Test
  void createSingleFileCommentPostsAReviewCommentAtTheParsedDiffPosition() {
    this.wireMock.stubFor(
        post(urlPathEqualTo(REPO_PATH + "/pulls/" + PR_ID + "/comments"))
            .willReturn(okJson(fixture("review-comment-create.json"))));

    final GitHubCommentsProvider provider = this.newProvider(this.newApi());
    final ChangedFile file =
        new ChangedFile("src/test/resources/recording-scratch/Example.txt", List.of(EXAMPLE_PATCH));

    provider.createSingleFileComment(file, 3, "Recorded review comment on line 3");

    final int expectedPosition = new PatchParserUtil(EXAMPLE_PATCH).findLineInDiff(3).orElse(1);
    final Map<?, ?> body =
        JSON_MAPPER.readValue(
            this.lastRequestBody(
                postRequestedFor(urlPathEqualTo(REPO_PATH + "/pulls/" + PR_ID + "/comments"))),
            Map.class);
    assertThat(body.get("body")).isEqualTo("Recorded review comment on line 3");
    assertThat(body.get("path")).isEqualTo("src/test/resources/recording-scratch/Example.txt");
    assertThat(body.get("commit_id")).isEqualTo(LAST_COMMIT_SHA);
    assertThat(((Number) body.get("position")).intValue()).isEqualTo(expectedPosition);
    assertThat(this.severeLogs).isEmpty();
  }

  @Test
  void createSingleFileCommentLogsSevereOnTheRealOutOfRangePositionFailure() {
    this.wireMock.stubFor(
        post(urlPathEqualTo(REPO_PATH + "/pulls/" + PR_ID + "/comments"))
            .willReturn(
                aResponse()
                    .withStatus(422)
                    .withHeader("Content-Type", "application/json; charset=utf-8")
                    .withBody(fixture("review-comment-422.json"))));

    final GitHubCommentsProvider provider = this.newProvider(this.newApi());
    final ChangedFile file = new ChangedFile("example.txt", List.of(EXAMPLE_PATCH));

    provider.createSingleFileComment(file, 3, "comment");

    assertThat(this.severeLogs).isNotEmpty();
    assertThat(this.severeLogs.get(0)).contains("could not be resolved");
  }

  @Test
  void getCommentsReturnsBothReviewAndIssueCommentsFromTheRealPullRequest() {
    this.wireMock.stubFor(
        get(urlPathEqualTo(REPO_PATH + "/pulls/" + PR_ID + "/comments"))
            .willReturn(okJson(fixture("review-comments-list.json"))));
    this.wireMock.stubFor(
        get(urlPathEqualTo(REPO_PATH + "/issues/" + PR_ID + "/comments"))
            .willReturn(okJson(fixture("issue-comments-list.json"))));

    final GitHubCommentsProvider provider = this.newProvider(this.newApi());
    final List<Comment> comments = provider.getComments();

    assertThat(comments).hasSize(2);
    assertThat(comments.stream().map(Comment::getIdentifier))
        .containsExactlyInAnyOrder("4053349034", "5742380693");
    assertThat(comments.stream().map(Comment::getType))
        .containsExactlyInAnyOrder("TYPE_DIFF", "TYPE_PR");
  }

  @Test
  void getFilesReturnsTheChangedFilesWithTheirRealPatches() {
    this.wireMock.stubFor(
        get(urlPathEqualTo(REPO_PATH + "/pulls/" + PR_ID + "/files"))
            .willReturn(okJson(fixture("files.json"))));

    final GitHubCommentsProvider provider = this.newProvider(this.newApi());
    final List<ChangedFile> files = provider.getFiles();

    assertThat(files).hasSize(2);
    assertThat(files.stream().map(ChangedFile::getFilename))
        .containsExactlyInAnyOrder(
            "src/test/resources/recording-scratch/Example.txt",
            "src/test/resources/recording-scratch/NOTE.md");
    final ChangedFile exampleFile =
        files.stream()
            .filter(f -> f.getFilename().endsWith("Example.txt"))
            .findFirst()
            .orElseThrow();
    assertThat(exampleFile.getSpecifics().get(0)).isEqualTo(EXAMPLE_PATCH);
  }

  @Test
  void removeCommentsDeletesAReviewCommentForADiffTypeComment() {
    this.wireMock.stubFor(
        delete(urlPathEqualTo(REPO_PATH + "/pulls/comments/4053349034"))
            .willReturn(aResponse().withStatus(204)));

    final GitHubCommentsProvider provider = this.newProvider(this.newApi());
    final Comment comment = new Comment("4053349034", "body", "TYPE_DIFF", new ArrayList<>());

    provider.removeComments(List.of(comment));

    this.wireMock.verify(
        deleteRequestedFor(urlPathEqualTo(REPO_PATH + "/pulls/comments/4053349034")));
    assertThat(this.severeLogs).isEmpty();
  }

  @Test
  void removeCommentsDeletesAnIssueCommentForAPrTypeComment() {
    this.wireMock.stubFor(
        delete(urlPathEqualTo(REPO_PATH + "/issues/comments/5742380693"))
            .willReturn(aResponse().withStatus(204)));

    final GitHubCommentsProvider provider = this.newProvider(this.newApi());
    final Comment comment = new Comment("5742380693", "body", "TYPE_PR", new ArrayList<>());

    provider.removeComments(List.of(comment));

    this.wireMock.verify(
        deleteRequestedFor(urlPathEqualTo(REPO_PATH + "/issues/comments/5742380693")));
    assertThat(this.severeLogs).isEmpty();
  }

  @Test
  void removeCommentsLogsSevereInsteadOfThrowingWhenTheCommentIsAlreadyGone() {
    this.wireMock.stubFor(
        delete(urlPathEqualTo(REPO_PATH + "/issues/comments/1"))
            .willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json; charset=utf-8")
                    .withBody(fixture("comment-not-found-404.json"))));

    final GitHubCommentsProvider provider = this.newProvider(this.newApi());
    final Comment comment = new Comment("1", "body", "TYPE_PR", new ArrayList<>());

    provider.removeComments(List.of(comment));

    assertThat(this.severeLogs).isNotEmpty();
    assertThat(this.severeLogs.get(0)).contains("Not Found");
  }

  @Test
  void commitsAndFilesArePagedUntilAShortPageIsReturned() {
    this.wireMock.resetAll();
    final StringBuilder fullPage = new StringBuilder("[");
    for (int i = 0; i < 100; i++) {
      if (i > 0) {
        fullPage.append(',');
      }
      fullPage.append("{\"sha\":\"commit-").append(i).append("\"}");
    }
    fullPage.append(']');

    this.wireMock.stubFor(
        get(urlPathEqualTo(REPO_PATH + "/pulls/" + PR_ID + "/commits"))
            .withQueryParam("page", equalTo("1"))
            .willReturn(okJson(fullPage.toString())));
    this.wireMock.stubFor(
        get(urlPathEqualTo(REPO_PATH + "/pulls/" + PR_ID + "/commits"))
            .withQueryParam("page", equalTo("2"))
            .willReturn(okJson("[{\"sha\":\"" + LAST_COMMIT_SHA + "\"}]")));
    this.wireMock.stubFor(
        post(urlPathEqualTo(REPO_PATH + "/pulls/" + PR_ID + "/comments"))
            .willReturn(okJson(fixture("review-comment-create.json"))));

    final GitHubCommentsProvider provider = this.newProvider(this.newApi());
    final ChangedFile file = new ChangedFile("example.txt", List.of(EXAMPLE_PATCH));
    provider.createSingleFileComment(file, 3, "comment");

    this.wireMock.verify(
        getRequestedFor(urlPathEqualTo(REPO_PATH + "/pulls/" + PR_ID + "/commits"))
            .withQueryParam("page", equalTo("1")));
    this.wireMock.verify(
        getRequestedFor(urlPathEqualTo(REPO_PATH + "/pulls/" + PR_ID + "/commits"))
            .withQueryParam("page", equalTo("2")));
    final Map<?, ?> body =
        JSON_MAPPER.readValue(
            this.lastRequestBody(
                postRequestedFor(urlPathEqualTo(REPO_PATH + "/pulls/" + PR_ID + "/comments"))),
            Map.class);
    assertThat(body.get("commit_id")).isEqualTo(LAST_COMMIT_SHA);
  }

  @Test
  void usesBasicAuthenticationWhenUsernameAndPasswordAreConfiguredInstead() {
    this.wireMock.stubFor(
        post(urlPathEqualTo(REPO_PATH + "/issues/" + PR_ID + "/comments"))
            .willReturn(okJson(fixture("issue-comment-create.json"))));

    final ViolationCommentsToGitHubApi api =
        violationCommentsToGitHubApi()
            .withGitHubUrl(this.wireMock.baseUrl() + "/")
            .withUsername("someuser")
            .withPassword("somepassword")
            .withRepositoryOwner(OWNER)
            .withRepositoryName(REPO)
            .withPullRequestId(PR_ID);

    final GitHubCommentsProvider provider = this.newProvider(api);
    provider.createComment("a comment");

    final String expectedAuth =
        "Basic "
            + java.util.Base64.getEncoder()
                .encodeToString("someuser:somepassword".getBytes(StandardCharsets.UTF_8));
    this.wireMock.verify(
        postRequestedFor(urlPathEqualTo(REPO_PATH + "/issues/" + PR_ID + "/comments"))
            .withHeader("Authorization", equalTo(expectedAuth)));
  }

  @Test
  void usesBearerAuthenticationWhenAnOAuth2TokenIsConfigured() {
    this.wireMock.stubFor(
        post(urlPathEqualTo(REPO_PATH + "/issues/" + PR_ID + "/comments"))
            .willReturn(okJson(fixture("issue-comment-create.json"))));

    final GitHubCommentsProvider provider = this.newProvider(this.newApi());
    provider.createComment("a comment");

    this.wireMock.verify(
        postRequestedFor(urlPathEqualTo(REPO_PATH + "/issues/" + PR_ID + "/comments"))
            .withHeader("Authorization", equalTo("Bearer test-token")));
  }

  @Test
  void shouldCommentIsTrueWhenCommentOnlyChangedContentIsDisabled() {
    final GitHubCommentsProvider provider = this.newProvider(this.newApi());
    final ChangedFile file = new ChangedFile("example.txt", List.of(EXAMPLE_PATCH));

    assertThat(provider.shouldComment(file, 999)).isTrue();
  }

  @Test
  void shouldCommentReflectsWhetherTheLineIsInTheDiffWhenRestricted() {
    final GitHubCommentsProvider provider =
        this.newProvider(this.newApi().withCommentOnlyChangedContent(true));
    final ChangedFile file = new ChangedFile("example.txt", List.of(EXAMPLE_PATCH));

    assertThat(provider.shouldComment(file, 3)).isTrue();
    assertThat(provider.shouldComment(file, 999)).isFalse();
  }
}
