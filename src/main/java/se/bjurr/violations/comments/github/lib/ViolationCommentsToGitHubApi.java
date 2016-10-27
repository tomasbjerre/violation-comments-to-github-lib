package se.bjurr.violations.comments.github.lib;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;
import static java.lang.Integer.MAX_VALUE;
import static se.bjurr.violations.comments.lib.CommentsCreator.createComments;

import java.util.List;

import se.bjurr.violations.comments.lib.model.CommentsProvider;
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
 private String gitHubUrl = "https://api.github.com/";
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

 private ViolationCommentsToGitHubApi() {

 }

 private void checkState() {
  if (oAuth2Token == null //
    && (username == null || password == null)) {
   throw new IllegalStateException(
     "User and Password, or OAuth2 token, must be set! They can be set with the API or by setting properties.\n" + //
       "Username/password:\n" + //
       "-D" + DEFAULT_PROP_VIOLATIONS_USERNAME + "=theuser -D" + DEFAULT_PROP_VIOLATIONS_PASSWORD + "=thepassword\n" + //
       "Or OAuth2Token:\n" + //
       "-D" + DEFAULT_PROP_VIOLATIONS_OAUTH2TOKEN + "=123ASDAA...");
  }
  checkNotNull(gitHubUrl, "GitHubUrl");
  checkNotNull(pullRequestId, "PullRequestId");
  checkNotNull(repositoryName, "RepositoryName");
  checkNotNull(repositoryOwner, "RepositoryOwner");
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
  CommentsProvider commentsProvider = new GitHubCommentsProvider(this);
  createComments(commentsProvider, violations, MAX_VALUE);
 }

 public ViolationCommentsToGitHubApi withCommentOnlyChangedContent(boolean commentOnlyChangedContent) {
  this.commentOnlyChangedContent = commentOnlyChangedContent;
  return this;
 }

 public ViolationCommentsToGitHubApi withCreateCommentWithAllSingleFileComments(
   boolean createCommentWithAllSingleFileComments) {
  this.createCommentWithAllSingleFileComments = createCommentWithAllSingleFileComments;
  return this;
 }

 public ViolationCommentsToGitHubApi withCreateSingleFileComments(boolean createSingleFileComments) {
  this.createSingleFileComments = createSingleFileComments;
  return this;
 }

 public ViolationCommentsToGitHubApi withGitHubUrl(String gitHubUrl) {
  this.gitHubUrl = gitHubUrl;
  return this;
 }

 public ViolationCommentsToGitHubApi withoAuth2Token(String oAuth2Token) {
  this.oAuth2Token = emptyToNull(oAuth2Token);
  return this;
 }

 public ViolationCommentsToGitHubApi withPassword(String password) {
  this.password = password;
  return this;
 }

 public void withPropOAuth2Token(String envOAuth2Token) {
  propOAuth2Token = envOAuth2Token;
 }

 public void withPropPassword(String envPassword) {
  propPassword = envPassword;
 }

 public void withPropUsername(String envUsername) {
  propUsername = envUsername;
 }

 public ViolationCommentsToGitHubApi withPullRequestId(int pullRequestId) {
  this.pullRequestId = pullRequestId;
  return this;
 }

 public ViolationCommentsToGitHubApi withRepositoryName(String repositoryName) {
  this.repositoryName = emptyToNull(repositoryName);
  return this;
 }

 public ViolationCommentsToGitHubApi withRepositoryOwner(String repositoryOwner) {
  this.repositoryOwner = emptyToNull(repositoryOwner);
  return this;
 }

 public ViolationCommentsToGitHubApi withUsername(String username) {
  this.username = username;
  return this;
 }

 public ViolationCommentsToGitHubApi withViolations(List<Violation> violations) {
  this.violations = violations;
  return this;
 }
}
