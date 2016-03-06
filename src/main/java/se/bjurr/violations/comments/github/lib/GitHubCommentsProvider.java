package se.bjurr.violations.comments.github.lib;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.egit.github.core.client.GitHubClient.createClient;

import java.io.IOException;
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

import se.bjurr.violations.comments.lib.model.Comment;
import se.bjurr.violations.comments.lib.model.CommentsProvider;

public class GitHubCommentsProvider implements CommentsProvider {
 private static final Logger LOG = LoggerFactory.getLogger(GitHubCommentsProvider.class);

 private static final String TYPE_PR = "TYPE_PR";
 private static final String TYPE_DIFF = "TYPE_DIFF";
 private final RepositoryId repository;
 private final PullRequestService pullRequestService;
 private final IssueService issueSerivce;
 private final int pullRequestId;
 private final String pullRequestCommit;
 private final boolean createSingleFileComment;
 private final boolean createCommentWithAllSingleFileComments;

 public GitHubCommentsProvider(ViolationCommentsToGitHubApi violationCommentsToGitHubApi) {
  GitHubClient gitHubClient = createClient(violationCommentsToGitHubApi.getGitHubUrl());
  if (violationCommentsToGitHubApi.getOAuth2Token() != null) {
   gitHubClient.setOAuth2Token(violationCommentsToGitHubApi.getOAuth2Token());
  } else if (violationCommentsToGitHubApi.getUsername() != null && violationCommentsToGitHubApi.getPassword() != null) {
   gitHubClient.setCredentials(violationCommentsToGitHubApi.getUsername(), violationCommentsToGitHubApi.getPassword());
  }
  pullRequestId = violationCommentsToGitHubApi.getPullRequestId();
  repository = new RepositoryId(violationCommentsToGitHubApi.getRepositoryOwner(),
    violationCommentsToGitHubApi.getRepositoryName());
  pullRequestService = new PullRequestService(gitHubClient);
  issueSerivce = new IssueService(gitHubClient);
  List<RepositoryCommit> commits = null;
  try {
   commits = pullRequestService.getCommits(repository, pullRequestId);
  } catch (IOException e) {
   throw propagate(e);
  }
  this.pullRequestCommit = checkNotNull(commits.get(0).getSha(), "PR commit sha");
  this.createCommentWithAllSingleFileComments = violationCommentsToGitHubApi
    .getCreateCommentWithAllSingleFileComments();
  this.createSingleFileComment = violationCommentsToGitHubApi.getCreateSingleFileComments();
 }

 @Override
 public void removeComments(List<Comment> comments) {
  for (Comment comment : comments) {
   try {
    Long commentId = Long.valueOf(comment.getIdentifier());
    if (comment.getType().equals(TYPE_DIFF)) {
     pullRequestService.deleteComment(repository, commentId);
    } else {
     issueSerivce.deleteComment(repository, commentId);
    }
   } catch (Exception e) {
    LOG.error("", e);
   }
  }
 }

 @Override
 public List<String> getFiles() {
  List<String> fileStrings = newArrayList();
  try {
   List<CommitFile> files = pullRequestService.getFiles(repository, pullRequestId);
   for (CommitFile commitFile : files) {
    fileStrings.add(commitFile.getFilename());
   }
  } catch (IOException e) {
   LOG.error("", e);
  }
  return fileStrings;
 }

 @Override
 public List<Comment> getComments() {
  List<Comment> comments = newArrayList();
  try {
   for (CommitComment commitComment : pullRequestService.getComments(repository, pullRequestId)) {
    comments.add(new Comment(Long.toString(commitComment.getId()), commitComment.getBody(), TYPE_DIFF));
   }
   for (org.eclipse.egit.github.core.Comment comment : issueSerivce.getComments(repository, pullRequestId)) {
    comments.add(new Comment(Long.toString(comment.getId()), comment.getBody(), TYPE_PR));
   }
  } catch (Exception e) {
   LOG.error("", e);
  }
  return comments;
 }

 @Override
 public void createSingleFileComment(String file, Integer line, String comment) {
  if (!createSingleFileComment) {
   return;
  }
  try {
   CommitComment commitComment = new CommitComment();
   commitComment.setBody(comment);
   commitComment.setPath(file);
   commitComment.setCommitId(pullRequestCommit);
   /**
    * Hard coding 1 here. This is the line of the diff, not the line of the
    * file. Cannot figure out how to translate from line in file to line in
    * diff...
    */
   commitComment.setPosition(1);
   pullRequestService.createComment(repository, pullRequestId, commitComment);
  } catch (IOException e) {
   LOG.error(//
     "File: \"" + file + "\"" + //
       "Line: \"" + line + "\"" + //
       "Comment: \"" + comment + "\"" //
     , e);
  }
 }

 @Override
 public void createCommentWithAllSingleFileComments(String comment) {
  if (!createCommentWithAllSingleFileComments) {
   return;
  }
  try {
   issueSerivce.createComment(repository, pullRequestId, comment);
  } catch (IOException e) {
   LOG.error("", e);
  }
 }
}
