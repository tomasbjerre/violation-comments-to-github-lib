package se.bjurr.violations.comments.github.lib;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.egit.github.core.client.GitHubClient.createClient;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

public class GitHubCommentsProvider implements CommentsProvider {
 private static final Logger LOG = LoggerFactory.getLogger(GitHubCommentsProvider.class);

 private static final String TYPE_PR = "TYPE_PR";
 private static final String TYPE_DIFF = "TYPE_DIFF";
 private final RepositoryId repository;
 private final PullRequestService pullRequestService;
 private final IssueService issueSerivce;
 private final String pullRequestCommit;

 private final ViolationCommentsToGitHubApi violationCommentsToGitHubApi;

 public GitHubCommentsProvider(ViolationCommentsToGitHubApi violationCommentsToGitHubApi) {
  GitHubClient gitHubClient = createClient(violationCommentsToGitHubApi.getGitHubUrl());
  if (violationCommentsToGitHubApi.getOAuth2Token() != null) {
   gitHubClient.setOAuth2Token(violationCommentsToGitHubApi.getOAuth2Token());
  } else if (violationCommentsToGitHubApi.getUsername() != null && violationCommentsToGitHubApi.getPassword() != null) {
   gitHubClient.setCredentials(violationCommentsToGitHubApi.getUsername(), violationCommentsToGitHubApi.getPassword());
  }
  repository = new RepositoryId(violationCommentsToGitHubApi.getRepositoryOwner(),
    violationCommentsToGitHubApi.getRepositoryName());
  pullRequestService = new PullRequestService(gitHubClient);
  issueSerivce = new IssueService(gitHubClient);
  List<RepositoryCommit> commits = null;
  try {
   commits = pullRequestService.getCommits(repository, violationCommentsToGitHubApi.getPullRequestId());
  } catch (IOException e) {
   throw propagate(e);
  }
  this.pullRequestCommit = checkNotNull(commits.get(0).getSha(), "PR commit sha");
  this.violationCommentsToGitHubApi = violationCommentsToGitHubApi;
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
 public List<ChangedFile> getFiles() {
  List<ChangedFile> changedFiles = newArrayList();
  try {
   List<CommitFile> files = pullRequestService.getFiles(repository, violationCommentsToGitHubApi.getPullRequestId());
   for (CommitFile commitFile : files) {
    changedFiles.add(new ChangedFile(commitFile.getFilename(), newArrayList(commitFile.getPatch())));
   }
  } catch (IOException e) {
   LOG.error("", e);
  }
  return changedFiles;
 }

 @Override
 public List<Comment> getComments() {
  List<Comment> comments = newArrayList();
  try {
   for (CommitComment commitComment : pullRequestService.getComments(repository,
     violationCommentsToGitHubApi.getPullRequestId())) {
    comments.add(new Comment(Long.toString(commitComment.getId()), commitComment.getBody(), TYPE_DIFF));
   }
   for (org.eclipse.egit.github.core.Comment comment : issueSerivce.getComments(repository,
     violationCommentsToGitHubApi.getPullRequestId())) {
    comments.add(new Comment(Long.toString(comment.getId()), comment.getBody(), TYPE_PR));
   }
  } catch (Exception e) {
   LOG.error("", e);
  }
  return comments;
 }

 @Override
 public void createSingleFileComment(ChangedFile file, Integer line, String comment) {
  if (!violationCommentsToGitHubApi.getCreateSingleFileComments()) {
   return;
  }
  Optional<Integer> lineToComment = findLineToComment(file, line);
  if (violationCommentsToGitHubApi.getCommentOnlyChangedContent() && !lineToComment.isPresent()) {
   return;
  } else if (!lineToComment.isPresent()) {
   // Put comments, that are not int the diff, on line 1
   lineToComment = Optional.of(1);
  }
  try {
   CommitComment commitComment = new CommitComment();
   commitComment.setBody(comment);
   commitComment.setPath(file.getFilename());
   commitComment.setCommitId(pullRequestCommit);
   commitComment.setPosition(lineToComment.get());
   pullRequestService.createComment(repository, violationCommentsToGitHubApi.getPullRequestId(), commitComment);
  } catch (IOException e) {
   LOG.error(//
     "File: \"" + file + "\" " + //
       "Line: \"" + line + "\" " + //
       "Comment: \"" + comment + "\"" //
     , e);
  }
 }

 /**
  * http://en.wikipedia.org/wiki/Diff_utility#Unified_format
  */
 @VisibleForTesting
 static Optional<Integer> findLineToComment(ChangedFile file, Integer lineToComment) {
  int currentLine = -1;
  int patchLocation = 0;
  String patchString = file.getSpecifics().get(0);
  for (String line : patchString.split("\n")) {
   if (line.startsWith("@")) {
    Matcher matcher = Pattern.compile(
      "@@\\p{IsWhite_Space}-[0-9]+(?:,[0-9]+)?\\p{IsWhite_Space}\\+([0-9]+)(?:,[0-9]+)?\\p{IsWhite_Space}@@.*")
      .matcher(line);
    if (!matcher.matches()) {
     throw new IllegalStateException("Unable to parse patch line " + line + "\nFull patch: \n" + patchString);
    }
    currentLine = Integer.parseInt(matcher.group(1));
   } else if (line.startsWith("+") || line.startsWith(" ")) {
    // Added or unmodified
    if (currentLine == lineToComment) {
     return Optional.of(patchLocation);
    }
    currentLine++;
   }
   patchLocation++;
  }
  return absent();
 }

 @Override
 public void createCommentWithAllSingleFileComments(String comment) {
  if (!violationCommentsToGitHubApi.getCreateCommentWithAllSingleFileComments()) {
   return;
  }
  try {
   issueSerivce.createComment(repository, violationCommentsToGitHubApi.getPullRequestId(), comment);
  } catch (IOException e) {
   LOG.error("", e);
  }
 }
}
