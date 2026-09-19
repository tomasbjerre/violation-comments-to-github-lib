package se.bjurr.violations.comments.github.lib;

import static java.util.logging.Level.SEVERE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import se.bjurr.violations.comments.github.lib.client.GitHubApiClient;
import se.bjurr.violations.comments.github.lib.client.model.GitHubCommentDto;
import se.bjurr.violations.comments.github.lib.client.model.GitHubCommitDto;
import se.bjurr.violations.comments.github.lib.client.model.GitHubFileDto;
import se.bjurr.violations.comments.lib.CommentsProvider;
import se.bjurr.violations.comments.lib.model.ChangedFile;
import se.bjurr.violations.comments.lib.model.Comment;
import se.bjurr.violations.lib.ViolationsLogger;
import se.bjurr.violations.lib.util.PatchParserUtil;

public class GitHubCommentsProvider implements CommentsProvider {
  private static final String TYPE_DIFF = "TYPE_DIFF";
  private static final String TYPE_PR = "TYPE_PR";

  private final GitHubApiClient gitHubApiClient;
  private final String pullRequestCommit;
  private final ViolationCommentsToGitHubApi violationCommentsToGitHubApi;
  private final ViolationsLogger violationsLogger;

  public GitHubCommentsProvider(
      final ViolationsLogger violationsLogger,
      final ViolationCommentsToGitHubApi violationCommentsToGitHubApi) {
    this.violationsLogger = violationsLogger;
    this.violationCommentsToGitHubApi = violationCommentsToGitHubApi;
    this.gitHubApiClient =
        new GitHubApiClient(
            violationsLogger,
            violationCommentsToGitHubApi.getGitHubUrl(),
            violationCommentsToGitHubApi.getOAuth2Token(),
            violationCommentsToGitHubApi.getUsername(),
            violationCommentsToGitHubApi.getPassword(),
            violationCommentsToGitHubApi.getRepositoryOwner(),
            violationCommentsToGitHubApi.getRepositoryName());
    final List<GitHubCommitDto> commits =
        this.gitHubApiClient.getCommits(violationCommentsToGitHubApi.getPullRequestId());
    this.pullRequestCommit = commits.get(commits.size() - 1).sha;
  }

  @Override
  public void createComment(final String comment) {
    try {
      this.gitHubApiClient.createIssueComment(
          this.violationCommentsToGitHubApi.getPullRequestId(), comment);
    } catch (final RuntimeException e) {
      this.violationsLogger.log(SEVERE, e.getMessage(), e);
    }
  }

  @Override
  public void createSingleFileComment(
      final ChangedFile file, final Integer line, final String comment) {
    final String patchString = file.getSpecifics().get(0);
    final Optional<Integer> lineToCommentOpt =
        new PatchParserUtil(patchString).findLineInDiff(line);
    final Integer lineToComment = lineToCommentOpt.orElse(1);
    try {
      this.gitHubApiClient.createReviewComment(
          this.violationCommentsToGitHubApi.getPullRequestId(),
          comment,
          this.pullRequestCommit,
          file.getFilename(),
          lineToComment);
    } catch (final RuntimeException e) {
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
      for (final GitHubCommentDto reviewComment :
          this.gitHubApiClient.getReviewComments(
              this.violationCommentsToGitHubApi.getPullRequestId())) {
        comments.add(
            new Comment(Long.toString(reviewComment.id), reviewComment.body, TYPE_DIFF, specifics));
      }
      for (final GitHubCommentDto issueComment :
          this.gitHubApiClient.getIssueComments(
              this.violationCommentsToGitHubApi.getPullRequestId())) {
        comments.add(
            new Comment(Long.toString(issueComment.id), issueComment.body, TYPE_PR, specifics));
      }
    } catch (final RuntimeException e) {
      this.violationsLogger.log(SEVERE, e.getMessage(), e);
    }
    return comments;
  }

  @Override
  public List<ChangedFile> getFiles() {
    final List<ChangedFile> changedFiles = new ArrayList<>();
    try {
      final List<GitHubFileDto> files =
          this.gitHubApiClient.getFiles(this.violationCommentsToGitHubApi.getPullRequestId());
      for (final GitHubFileDto file : files) {
        final List<String> list = new ArrayList<>();
        list.add(file.patch);
        changedFiles.add(new ChangedFile(file.filename, list));
      }
    } catch (final RuntimeException e) {
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
          this.gitHubApiClient.deleteReviewComment(commentId);
        } else {
          this.gitHubApiClient.deleteIssueComment(commentId);
        }
      } catch (final RuntimeException e) {
        this.violationsLogger.log(SEVERE, e.getMessage(), e);
      }
    }
  }

  @Override
  public boolean shouldComment(final ChangedFile changedFile, final Integer line) {
    final String patchString = changedFile.getSpecifics().get(0);
    final boolean lineChanged = new PatchParserUtil(patchString).isLineInDiff(line);
    final boolean commentOnlyChangedContent =
        this.violationCommentsToGitHubApi.getCommentOnlyChangedContent();
    return !commentOnlyChangedContent || lineChanged;
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
