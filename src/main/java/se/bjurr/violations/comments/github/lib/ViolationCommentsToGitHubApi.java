package se.bjurr.violations.comments.github.lib;

import static java.lang.Integer.MAX_VALUE;
import static se.bjurr.violations.comments.lib.CommentsCreator.createComments;
import static se.bjurr.violations.lib.util.Optional.fromNullable;
import static se.bjurr.violations.lib.util.Utils.checkNotNull;
import static se.bjurr.violations.lib.util.Utils.emptyToNull;
import static se.bjurr.violations.lib.util.Utils.firstNonNull;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.bjurr.violations.comments.lib.CommentsProvider;
import se.bjurr.violations.comments.lib.ViolationsLogger;
import se.bjurr.violations.lib.model.Violation;
import se.bjurr.violations.lib.util.Optional;

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
  private List<Violation> violations;
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

  private ViolationCommentsToGitHubApi() {}

  private void checkState() {
    if (oAuth2Token == null //
        && (username == null || password == null)) {
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
    checkNotNull(pullRequestId, "PullRequestId");
    checkNotNull(repositoryName, "RepositoryName");
    checkNotNull(repositoryOwner, "RepositoryOwner");
  }

  public ViolationCommentsToGitHubApi withViolationsLogger(
      final ViolationsLogger violationsLogger) {
    this.violationsLogger = violationsLogger;
    return this;
  }

  public boolean getCommentOnlyChangedContent() {
    return commentOnlyChangedContent;
  }

  public boolean getCreateCommentWithAllSingleFileComments() {
    return createCommentWithAllSingleFileComments;
  }

  public boolean getCreateSingleFileComments() {
    return createSingleFileComments;
  }

  public String getGitHubUrl() {
    if (gitHubUrl == null || gitHubUrl.trim().isEmpty()) {
      return "https://api.github.com/";
    }
    return gitHubUrl;
  }

  public String getOAuth2Token() {
    return oAuth2Token;
  }

  public String getPassword() {
    return password;
  }

  public String getPropOAuth2Token() {
    return propOAuth2Token;
  }

  public String getPropPassword() {
    return propPassword;
  }

  public String getPropUsername() {
    return propUsername;
  }

  public int getPullRequestId() {
    return pullRequestId;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public String getRepositoryOwner() {
    return repositoryOwner;
  }

  public String getUsername() {
    return username;
  }

  private void populateFromEnvironmentVariables() {
    if (System.getProperty(propUsername) != null) {
      username = firstNonNull(username, System.getProperty(propUsername));
    }
    if (System.getProperty(propPassword) != null) {
      password = firstNonNull(password, System.getProperty(propPassword));
    }
    if (System.getProperty(propOAuth2Token) != null) {
      oAuth2Token = firstNonNull(oAuth2Token, System.getProperty(propOAuth2Token));
    }
  }

  public void toPullRequest() throws Exception {
    populateFromEnvironmentVariables();
    checkState();
    final CommentsProvider commentsProvider = new GitHubCommentsProvider(violationsLogger, this);
    createComments(violationsLogger, violations, MAX_VALUE, commentsProvider);
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
    propOAuth2Token = envOAuth2Token;
  }

  public void withPropPassword(final String envPassword) {
    propPassword = envPassword;
  }

  public void withPropUsername(final String envUsername) {
    propUsername = envUsername;
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

  public ViolationCommentsToGitHubApi withViolations(final List<Violation> violations) {
    this.violations = violations;
    return this;
  }

  public ViolationCommentsToGitHubApi withKeepOldComments(final boolean keepOldComments) {
    this.keepOldComments = keepOldComments;
    return this;
  }

  public boolean getKeepOldComments() {
    return keepOldComments;
  }

  public ViolationCommentsToGitHubApi withCommentTemplate(final String commentTemplate) {
    this.commentTemplate = commentTemplate;
    return this;
  }

  public Optional<String> findCommentTemplate() {
    return fromNullable(commentTemplate);
  }
}
