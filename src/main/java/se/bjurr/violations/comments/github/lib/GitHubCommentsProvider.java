package se.bjurr.violations.comments.github.lib;

import static java.util.logging.Level.SEVERE;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.spotify.github.v3.clients.GitHubClient;
import com.spotify.github.v3.clients.IssueClient;
import com.spotify.github.v3.clients.PullRequestClient;
import com.spotify.github.v3.clients.RepositoryClient;
import com.spotify.github.v3.prs.ImmutableReviewComment;
import com.spotify.github.v3.prs.ImmutableReviewParameters;
import com.spotify.github.v3.prs.PullRequest;
import com.spotify.github.v3.prs.Review;
import com.spotify.github.v3.prs.ReviewParameters;

import se.bjurr.violations.comments.lib.CommentsProvider;
import se.bjurr.violations.comments.lib.model.ChangedFile;
import se.bjurr.violations.comments.lib.model.Comment;
import se.bjurr.violations.lib.ViolationsLogger;
import se.bjurr.violations.lib.util.PatchParserUtil;

public class GitHubCommentsProvider implements CommentsProvider {
  private static final String TYPE_DIFF = "TYPE_DIFF";
  private static final String TYPE_PR = "TYPE_PR";

  private final IssueClient issueSerivce;
  private final String pullRequestCommit;
  private final PullRequestClient pullRequestService;

  private final RepositoryClient repository;

  private final ViolationCommentsToGitHubApi violationCommentsToGitHubApi;
  private final ViolationsLogger violationsLogger;

  public GitHubCommentsProvider(
      final ViolationsLogger violationsLogger,
      final ViolationCommentsToGitHubApi violationCommentsToGitHubApi) {
    this.violationsLogger = violationsLogger;
    final GitHubClient gitHubClient = getGitHubClient(violationCommentsToGitHubApi.getGitHubUrl(),violationCommentsToGitHubApi.getOAuth2Token());
    this.repository = gitHubClient.createRepositoryClient(violationCommentsToGitHubApi.getRepositoryOwner(), violationCommentsToGitHubApi.getRepositoryName());
    this.pullRequestService = this.repository.createPullRequestClient();
    this.issueSerivce = this.repository.createIssueClient();
    try {
    	final PullRequest pullRequest = this.pullRequestService.get(violationCommentsToGitHubApi.getPullRequestId()).get();
		this.pullRequestCommit =  pullRequest.mergeCommitSha().get();
    } catch (final Exception e) {
      throw new RuntimeException(e);
	}
    this.violationCommentsToGitHubApi = violationCommentsToGitHubApi;
  }

  static GitHubClient getGitHubClient(final String gitHubUrl, final String acessToken) {
    try {
      return GitHubClient.create(URI.create(gitHubUrl), acessToken);
    } catch (final Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public void createComment(final String comment) {
    try {
      this.issueSerivce.createComment(
this.violationCommentsToGitHubApi.getPullRequestId(), comment);
    } catch (final Exception e) {
      this.violationsLogger.log(SEVERE, e.getMessage(), e);
    }
  }

  @Override
  public void createSingleFileComment(
      final ChangedFile file, final Integer position, final String comment) {
    final String patchString = file.getSpecifics().get(0);
    final Optional<Integer> lineToCommentOpt =
        new PatchParserUtil(patchString).findLineInDiff(position);
    final Integer lineToComment = lineToCommentOpt.orElse(1);
    try {
      final ReviewParameters commitComment = ImmutableReviewParameters.builder()
    		  .body(comment)
    		  .commitId(this.pullRequestCommit)
    		  .addComments(ImmutableReviewComment.builder()
    				  .path(file.getFilename())
    				  .position(position) // or is it lineToComment?
    				  .build())
    		  .build();
      this.pullRequestService.createReview(this.violationCommentsToGitHubApi.getPullRequestId(), commitComment);
    } catch (final Exception e) {
      this.violationsLogger.log(
          SEVERE,
          "File: \""
              + file
              + "\" \n"
              + //
              "position: \""
              + position
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
      for (final Review commitComment :
          this.pullRequestService.listReviews(this.violationCommentsToGitHubApi.getPullRequestId()).get()) {
        comments.add(
            new Comment(Long.toString(commitComment.id()), commitComment.body().orElse(""), TYPE_PR, specifics));
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
    final boolean lineChanged = new PatchParserUtil(patchString).isLineInDiff(line);
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
