package se.bjurr.violations.comments.github.lib;

import static org.eclipse.egit.github.core.client.GitHubClient.createClient;
import static se.bjurr.violations.comments.lib.PatchParser.findLineToComment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.bjurr.violations.comments.lib.model.ChangedFile;
import se.bjurr.violations.comments.lib.model.Comment;
import se.bjurr.violations.comments.lib.model.CommentsProvider;
import se.bjurr.violations.lib.model.Violation;
import se.bjurr.violations.lib.util.Optional;

public class GitHubCommentsProvider implements CommentsProvider {
  private static final Logger LOG = LoggerFactory.getLogger(GitHubCommentsProvider.class);

  private static final String TYPE_DIFF = "TYPE_DIFF";
  private static final String TYPE_PR = "TYPE_PR";

  private final IssueService issueSerivce;
  private final String pullRequestCommit;
  private final PullRequestService pullRequestService;

  private final RepositoryId repository;

  private final ViolationCommentsToGitHubApi violationCommentsToGitHubApi;

  public GitHubCommentsProvider(ViolationCommentsToGitHubApi violationCommentsToGitHubApi) {
    final GitHubClient gitHubClient = createClient(violationCommentsToGitHubApi.getGitHubUrl());
    if (violationCommentsToGitHubApi.getOAuth2Token() != null) {
      gitHubClient.setOAuth2Token(violationCommentsToGitHubApi.getOAuth2Token());
    } else if (violationCommentsToGitHubApi.getUsername() != null
        && violationCommentsToGitHubApi.getPassword() != null) {
      gitHubClient.setCredentials(
          violationCommentsToGitHubApi.getUsername(), violationCommentsToGitHubApi.getPassword());
    }
    repository =
        new RepositoryId(
            violationCommentsToGitHubApi.getRepositoryOwner(),
            violationCommentsToGitHubApi.getRepositoryName());
    pullRequestService = new PullRequestService(gitHubClient);
    issueSerivce = new IssueService(gitHubClient);
    List<RepositoryCommit> commits = null;
    try {
      commits =
          pullRequestService.getCommits(
              repository, violationCommentsToGitHubApi.getPullRequestId());
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    pullRequestCommit = commits.get(commits.size() - 1).getSha();
    this.violationCommentsToGitHubApi = violationCommentsToGitHubApi;
  }

  @Override
  public void createCommentWithAllSingleFileComments(String comment) {
    try {
      issueSerivce.createComment(
          repository, violationCommentsToGitHubApi.getPullRequestId(), comment);
    } catch (final IOException e) {
      LOG.error("", e);
    }
  }

  @Override
  public void createSingleFileComment(ChangedFile file, Integer line, String comment) {
    Optional<Integer> lineToComment = findLineToComment(file.getSpecifics().get(0), line);
    if (!lineToComment.isPresent()) {
      // Put comments, that are not int the diff, on line 1
      lineToComment = Optional.fromNullable(1);
    }
    try {
      final CommitComment commitComment = new CommitComment();
      commitComment.setBody(comment);
      commitComment.setPath(file.getFilename());
      commitComment.setCommitId(pullRequestCommit);
      commitComment.setPosition(lineToComment.get());
      pullRequestService.createComment(
          repository, violationCommentsToGitHubApi.getPullRequestId(), commitComment);
    } catch (final IOException e) {
      LOG.error( //
          "File: \""
              + file
              + "\" \n"
              + //
              "Line: \""
              + line
              + "\" \n"
              + //
              "Position: \""
              + lineToComment.orNull()
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
          pullRequestService.getComments(
              repository, violationCommentsToGitHubApi.getPullRequestId())) {
        comments.add(
            new Comment(
                Long.toString(commitComment.getId()),
                commitComment.getBody(),
                TYPE_DIFF,
                specifics));
      }
      for (final org.eclipse.egit.github.core.Comment comment :
          issueSerivce.getComments(repository, violationCommentsToGitHubApi.getPullRequestId())) {
        comments.add(
            new Comment(Long.toString(comment.getId()), comment.getBody(), TYPE_PR, specifics));
      }
    } catch (final Exception e) {
      LOG.error("", e);
    }
    return comments;
  }

  @Override
  public List<ChangedFile> getFiles() {
    final List<ChangedFile> changedFiles = new ArrayList<>();
    try {
      final List<CommitFile> files =
          pullRequestService.getFiles(repository, violationCommentsToGitHubApi.getPullRequestId());
      for (final CommitFile commitFile : files) {
        final List<String> list = new ArrayList<>();
        list.add(commitFile.getPatch());
        changedFiles.add(new ChangedFile(commitFile.getFilename(), list));
      }
    } catch (final IOException e) {
      LOG.error("", e);
    }
    return changedFiles;
  }

  @Override
  public void removeComments(List<Comment> comments) {
    for (final Comment comment : comments) {
      try {
        final Long commentId = Long.valueOf(comment.getIdentifier());
        if (comment.getType().equals(TYPE_DIFF)) {
          pullRequestService.deleteComment(repository, commentId);
        } else {
          issueSerivce.deleteComment(repository, commentId);
        }
      } catch (final Exception e) {
        LOG.error("", e);
      }
    }
  }

  @Override
  public boolean shouldComment(ChangedFile changedFile, Integer line) {
    final Optional<Integer> lineToComment =
        findLineToComment(changedFile.getSpecifics().get(0), line);
    final boolean lineNotChanged = !lineToComment.isPresent();
    final boolean commentOnlyChangedContent =
        violationCommentsToGitHubApi.getCommentOnlyChangedContent();
    if (commentOnlyChangedContent && lineNotChanged) {
      return false;
    }
    return true;
  }

  @Override
  public boolean shouldCreateCommentWithAllSingleFileComments() {
    return violationCommentsToGitHubApi.getCreateCommentWithAllSingleFileComments();
  }

  @Override
  public boolean shouldCreateSingleFileComment() {
    return violationCommentsToGitHubApi.getCreateSingleFileComments();
  }

  @Override
  public Optional<String> findCommentFormat(ChangedFile changedFile, Violation violation) {
    return Optional.absent();
  }

  @Override
  public boolean shouldKeepOldComments() {
    return violationCommentsToGitHubApi.getKeepOldComments();
  }
}
