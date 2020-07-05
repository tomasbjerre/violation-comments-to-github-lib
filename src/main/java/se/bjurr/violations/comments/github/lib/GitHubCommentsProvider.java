package se.bjurr.violations.comments.github.lib;

import static java.util.logging.Level.SEVERE;
import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_API;
import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_DEFAULT;
import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_GISTS;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;
import se.bjurr.violations.comments.lib.CommentsProvider;
import se.bjurr.violations.comments.lib.PatchParser;
import se.bjurr.violations.comments.lib.model.ChangedFile;
import se.bjurr.violations.comments.lib.model.Comment;
import se.bjurr.violations.lib.ViolationsLogger;

public class GitHubCommentsProvider implements CommentsProvider {
  private static final String TYPE_DIFF = "TYPE_DIFF";
  private static final String TYPE_PR = "TYPE_PR";

  private final IssueService issueSerivce;
  private final String pullRequestCommit;
  private final PullRequestService pullRequestService;

  private final RepositoryId repository;

  private final ViolationCommentsToGitHubApi violationCommentsToGitHubApi;
  private final ViolationsLogger violationsLogger;

  public GitHubCommentsProvider(
      final ViolationsLogger violationsLogger,
      final ViolationCommentsToGitHubApi violationCommentsToGitHubApi) {
    this.violationsLogger = violationsLogger;
    final GitHubClient gitHubClient = getGitHubClient(violationCommentsToGitHubApi.getGitHubUrl());
    if (violationCommentsToGitHubApi.getOAuth2Token() != null) {
      gitHubClient.setOAuth2Token(violationCommentsToGitHubApi.getOAuth2Token());
    } else if (violationCommentsToGitHubApi.getUsername() != null
        && violationCommentsToGitHubApi.getPassword() != null) {
      gitHubClient.setCredentials(
          violationCommentsToGitHubApi.getUsername(), violationCommentsToGitHubApi.getPassword());
    }
    this.repository =
        new RepositoryId(
            violationCommentsToGitHubApi.getRepositoryOwner(),
            violationCommentsToGitHubApi.getRepositoryName());
    this.pullRequestService = new PullRequestService(gitHubClient);
    this.issueSerivce = new IssueService(gitHubClient);
    List<RepositoryCommit> commits = null;
    try {
      commits =
          this.pullRequestService.getCommits(
              this.repository, violationCommentsToGitHubApi.getPullRequestId());
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    this.pullRequestCommit = commits.get(commits.size() - 1).getSha();
    this.violationCommentsToGitHubApi = violationCommentsToGitHubApi;
  }

  static GitHubClientTestable getGitHubClient(final String gitHubUrl) {
    try {
      final URL url = new URL(gitHubUrl);
      String hostname = url.getHost();
      if (HOST_DEFAULT.equals(hostname) || HOST_GISTS.equals(hostname)) {
        hostname = HOST_API;
      }
      final int port = url.getPort();
      final String scheme = url.getProtocol();
      return new GitHubClientTestable(hostname, port, scheme);
    } catch (final IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public void createComment(final String comment) {
    try {
      this.issueSerivce.createComment(
          this.repository, this.violationCommentsToGitHubApi.getPullRequestId(), comment);
    } catch (final IOException e) {
      this.violationsLogger.log(SEVERE, e.getMessage(), e);
    }
  }

  @Override
  public void createSingleFileComment(
      final ChangedFile file, final Integer line, final String comment) {
    final String patchString = file.getSpecifics().get(0);
    final Optional<Integer> lineToCommentOpt = new PatchParser(patchString).findLineInDiff(line);
    final Integer lineToComment = lineToCommentOpt.orElse(1);
    try {
      final CommitComment commitComment = new CommitComment();
      commitComment.setBody(comment);
      commitComment.setPath(file.getFilename());
      commitComment.setCommitId(this.pullRequestCommit);
      commitComment.setLine(line);
      commitComment.setPosition(lineToComment);
      this.pullRequestService.createComment(
          this.repository, this.violationCommentsToGitHubApi.getPullRequestId(), commitComment);
    } catch (final IOException e) {
      this.violationsLogger.log(
          SEVERE,
          "File: \""
              + file
              + "\" \n"
              + //
              "Line: \""
              + line
              + "\" \n"
              + //
              "Position: \""
              + lineToComment
              + "\" \n"
              + //
              "Comment: \""
              + comment
              + "\"" //
          ,
          e);
    }
  }

  @Override
  public List<Comment> getComments() {
    final List<Comment> comments = new ArrayList<>();
    try {
      final List<String> specifics = new ArrayList<>();
      for (final CommitComment commitComment :
          this.pullRequestService.getComments(
              this.repository, this.violationCommentsToGitHubApi.getPullRequestId())) {
        comments.add(
            new Comment(
                Long.toString(commitComment.getId()),
                commitComment.getBody(),
                TYPE_DIFF,
                specifics));
      }
      for (final org.eclipse.egit.github.core.Comment comment :
          this.issueSerivce.getComments(
              this.repository, this.violationCommentsToGitHubApi.getPullRequestId())) {
        comments.add(
            new Comment(Long.toString(comment.getId()), comment.getBody(), TYPE_PR, specifics));
      }
    } catch (final Exception e) {
      this.violationsLogger.log(SEVERE, e.getMessage(), e);
    }
    return comments;
  }

  @Override
  public List<ChangedFile> getFiles() {
    final List<ChangedFile> changedFiles = new ArrayList<>();
    try {
      final List<CommitFile> files =
          this.pullRequestService.getFiles(
              this.repository, this.violationCommentsToGitHubApi.getPullRequestId());
      for (final CommitFile commitFile : files) {
        final List<String> list = new ArrayList<>();
        list.add(commitFile.getPatch());
        changedFiles.add(new ChangedFile(commitFile.getFilename(), list));
      }
    } catch (final IOException e) {
      this.violationsLogger.log(SEVERE, e.getMessage(), e);
    }
    return changedFiles;
  }

  @Override
  public void removeComments(final List<Comment> comments) {
    for (final Comment comment : comments) {
      try {
        final Long commentId = Long.valueOf(comment.getIdentifier());
        if (comment.getType().equals(TYPE_DIFF)) {
          this.pullRequestService.deleteComment(this.repository, commentId);
        } else {
          this.issueSerivce.deleteComment(this.repository, commentId);
        }
      } catch (final Throwable e) {
        this.violationsLogger.log(SEVERE, e.getMessage(), e);
      }
    }
  }

  @Override
  public boolean shouldComment(final ChangedFile changedFile, final Integer line) {
    final String patchString = changedFile.getSpecifics().get(0);
    final boolean lineChanged = new PatchParser(patchString).isLineInDiff(line);
    final boolean commentOnlyChangedContent =
        this.violationCommentsToGitHubApi.getCommentOnlyChangedContent();
    if (commentOnlyChangedContent && !lineChanged) {
      return false;
    }
    return true;
  }

  @Override
  public boolean shouldCreateCommentWithAllSingleFileComments() {
    return this.violationCommentsToGitHubApi.getCreateCommentWithAllSingleFileComments();
  }

  @Override
  public boolean shouldCreateSingleFileComment() {
    return this.violationCommentsToGitHubApi.getCreateSingleFileComments();
  }

  @Override
  public boolean shouldKeepOldComments() {
    return this.violationCommentsToGitHubApi.getKeepOldComments();
  }

  @Override
  public boolean shouldCommentOnlyChangedFiles() {
    return this.violationCommentsToGitHubApi.getCommentOnlyChangedFiles();
  }

  @Override
  public Optional<String> findCommentTemplate() {
    return this.violationCommentsToGitHubApi.findCommentTemplate();
  }

  @Override
  public Integer getMaxNumberOfViolations() {
    return this.violationCommentsToGitHubApi.getMaxNumberOfViolations();
  }

  @Override
  public Integer getMaxCommentSize() {
    return this.violationCommentsToGitHubApi.getMaxCommentSize();
  }
}
