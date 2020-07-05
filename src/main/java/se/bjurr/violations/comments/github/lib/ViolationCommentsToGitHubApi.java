package se.bjurr.violations.comments.github.lib;

import static java.util.Optional.ofNullable;
import static se.bjurr.violations.comments.lib.CommentsCreator.createComments;
import static se.bjurr.violations.lib.util.Utils.checkNotNull;
import static se.bjurr.violations.lib.util.Utils.emptyToNull;
import static se.bjurr.violations.lib.util.Utils.firstNonNull;

import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.bjurr.violations.comments.lib.CommentsProvider;
import se.bjurr.violations.lib.ViolationsLogger;
import se.bjurr.violations.lib.model.Violation;

public class ViolationCommentsToGitHubApi {
  public static final String DEFAULT_PROP_VIOLATIONS_OAUTH2TOKEN = "VIOLATIONS_OAUTH2TOKEN";
  public static final String DEFAULT_PROP_VIOLATIONS_USERNAME = "VIOLATIONS_USERNAME";
  public static final String DEFAULT_PROP_VIOLATIONS_PASSWORD = "VIOLATIONS_PASSWORD";

  public static ViolationCommentsToGitHubApi violationCommentsToGitHubApi() {
    return new ViolationCommentsToGitHubApi();
  }

  private String propUsername = DEFAULT_PROP_VIOLATIONS_USERNAME;
  private String propPassword = DEFAULT_PROP_VIOLATIONS_PASSWORD;
  private String propOAuth2Token = DEFAULT_PROP_VIOLATIONS_OAUTH2TOKEN;
  private String gitHubUrl;
  private String oAuth2Token;
  private String username;
  private String password;
  private String repositoryOwner;
  private String repositoryName;
  private int pullRequestId;
  private Set<Violation> violations;
  private boolean createSingleFileComments = true;
  private boolean createCommentWithAllSingleFileComments = false;

  private boolean commentOnlyChangedContent = false;
  private boolean keepOldComments = false;
  private String commentTemplate;
  private ViolationsLogger violationsLogger =
      new ViolationsLogger() {
        @Override
        public void log(final Level level, final String string) {
          Logger.getLogger(ViolationCommentsToGitHubApi.class.getSimpleName()).log(level, string);
        }

        @Override
        public void log(final Level level, final String string, final Throwable t) {
          Logger.getLogger(ViolationCommentsToGitHubApi.class.getSimpleName())
              .log(level, string, t);
        }
      };
  private Integer maxCommentSize;
  private Integer maxNumberOfViolations;
  private boolean commentOnlyChangedFiles = true;

  private ViolationCommentsToGitHubApi() {}

  private void checkState() {
    if (this.oAuth2Token == null //
        && (this.username == null || this.password == null)) {
      throw new IllegalStateException(
          "User and Password, or OAuth2 token, must be set! They can be set with the API or by setting properties.\n"
              + //
              "Username/password:\n"
              + //
              "-D"
              + DEFAULT_PROP_VIOLATIONS_USERNAME
              + "=theuser -D"
              + DEFAULT_PROP_VIOLATIONS_PASSWORD
              + "=thepassword\n"
              + //
              "Or OAuth2Token:\n"
              + //
              "-D"
              + DEFAULT_PROP_VIOLATIONS_OAUTH2TOKEN
              + "=123ASDAA...");
    }
    checkNotNull(this.pullRequestId, "PullRequestId");
    checkNotNull(this.repositoryName, "RepositoryName");
    checkNotNull(this.repositoryOwner, "RepositoryOwner");
  }

  public ViolationCommentsToGitHubApi withViolationsLogger(
      final ViolationsLogger violationsLogger) {
    this.violationsLogger = violationsLogger;
    return this;
  }

  public boolean getCommentOnlyChangedContent() {
    return this.commentOnlyChangedContent;
  }

  public boolean getCreateCommentWithAllSingleFileComments() {
    return this.createCommentWithAllSingleFileComments;
  }

  public boolean getCreateSingleFileComments() {
    return this.createSingleFileComments;
  }

  public String getGitHubUrl() {
    if (this.gitHubUrl == null || this.gitHubUrl.trim().isEmpty()) {
      return "https://api.github.com/";
    }
    return this.gitHubUrl;
  }

  public String getOAuth2Token() {
    return this.oAuth2Token;
  }

  public String getPassword() {
    return this.password;
  }

  public String getPropOAuth2Token() {
    return this.propOAuth2Token;
  }

  public String getPropPassword() {
    return this.propPassword;
  }

  public String getPropUsername() {
    return this.propUsername;
  }

  public int getPullRequestId() {
    return this.pullRequestId;
  }

  public String getRepositoryName() {
    return this.repositoryName;
  }

  public String getRepositoryOwner() {
    return this.repositoryOwner;
  }

  public String getUsername() {
    return this.username;
  }

  private void populateFromEnvironmentVariables() {
    if (System.getProperty(this.propUsername) != null) {
      this.username = firstNonNull(this.username, System.getProperty(this.propUsername));
    }
    if (System.getProperty(this.propPassword) != null) {
      this.password = firstNonNull(this.password, System.getProperty(this.propPassword));
    }
    if (System.getProperty(this.propOAuth2Token) != null) {
      this.oAuth2Token = firstNonNull(this.oAuth2Token, System.getProperty(this.propOAuth2Token));
    }
  }

  public void toPullRequest() throws Exception {
    this.populateFromEnvironmentVariables();
    this.checkState();
    final CommentsProvider commentsProvider =
        new GitHubCommentsProvider(this.violationsLogger, this);
    createComments(this.violationsLogger, this.violations, commentsProvider);
  }

  public ViolationCommentsToGitHubApi withCommentOnlyChangedContent(
      final boolean commentOnlyChangedContent) {
    this.commentOnlyChangedContent = commentOnlyChangedContent;
    return this;
  }

  public ViolationCommentsToGitHubApi withCreateCommentWithAllSingleFileComments(
      final boolean createCommentWithAllSingleFileComments) {
    this.createCommentWithAllSingleFileComments = createCommentWithAllSingleFileComments;
    return this;
  }

  public ViolationCommentsToGitHubApi withCreateSingleFileComments(
      final boolean createSingleFileComments) {
    this.createSingleFileComments = createSingleFileComments;
    return this;
  }

  public ViolationCommentsToGitHubApi withCommentOnlyChangedFiles(
      final boolean commentOnlyChangedFiles) {
    this.commentOnlyChangedFiles = commentOnlyChangedFiles;
    return this;
  }

  public boolean getCommentOnlyChangedFiles() {
    return this.commentOnlyChangedFiles;
  }

  public ViolationCommentsToGitHubApi withGitHubUrl(final String gitHubUrl) {
    this.gitHubUrl = gitHubUrl;
    return this;
  }

  public ViolationCommentsToGitHubApi withoAuth2Token(final String oAuth2Token) {
    this.oAuth2Token = emptyToNull(oAuth2Token);
    return this;
  }

  public ViolationCommentsToGitHubApi withPassword(final String password) {
    this.password = password;
    return this;
  }

  public void withPropOAuth2Token(final String envOAuth2Token) {
    this.propOAuth2Token = envOAuth2Token;
  }

  public void withPropPassword(final String envPassword) {
    this.propPassword = envPassword;
  }

  public void withPropUsername(final String envUsername) {
    this.propUsername = envUsername;
  }

  public ViolationCommentsToGitHubApi withPullRequestId(final int pullRequestId) {
    this.pullRequestId = pullRequestId;
    return this;
  }

  public ViolationCommentsToGitHubApi withRepositoryName(final String repositoryName) {
    this.repositoryName = emptyToNull(repositoryName);
    return this;
  }

  public ViolationCommentsToGitHubApi withRepositoryOwner(final String repositoryOwner) {
    this.repositoryOwner = emptyToNull(repositoryOwner);
    return this;
  }

  public ViolationCommentsToGitHubApi withUsername(final String username) {
    this.username = username;
    return this;
  }

  public ViolationCommentsToGitHubApi withViolations(final Set<Violation> violations) {
    this.violations = violations;
    return this;
  }

  public ViolationCommentsToGitHubApi withKeepOldComments(final boolean keepOldComments) {
    this.keepOldComments = keepOldComments;
    return this;
  }

  public boolean getKeepOldComments() {
    return this.keepOldComments;
  }

  public ViolationCommentsToGitHubApi withCommentTemplate(final String commentTemplate) {
    this.commentTemplate = commentTemplate;
    return this;
  }

  public Optional<String> findCommentTemplate() {
    return ofNullable(this.commentTemplate);
  }

  public ViolationCommentsToGitHubApi withMaxCommentSize(final Integer maxCommentSize) {
    this.maxCommentSize = maxCommentSize;
    return this;
  }

  public ViolationCommentsToGitHubApi withMaxNumberOfViolations(
      final Integer maxNumberOfViolations) {
    this.maxNumberOfViolations = maxNumberOfViolations;
    return this;
  }

  public Integer getMaxNumberOfViolations() {
    return this.maxNumberOfViolations;
  }

  public Integer getMaxCommentSize() {
    return this.maxCommentSize;
  }
}
