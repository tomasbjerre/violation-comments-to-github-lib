package se.bjurr.violations.comments.github.lib;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import se.bjurr.violations.comments.lib.model.ChangedFile;

public class GitHubCommentsProviderTest {

 private static final String NEW_DIFF = "@@ -1,6 +1,6 @@\n <html>\n  <head></head>\n <body>\n-<font>\n+<font> \n </body> \n </html>";
 private static final String CHANGED_DIFF = " @@ -1,4 +1,5 @@\n .klass {\n  font-size: 14px;\n+ \n  font-size: 14px;\n }";
 private static final String CHANGED_DIFF_2 = "@@ -6,6 +6,16 @@\n  void npe(String a, String b) {\n   if (a == null) {\n    System.out.println();\n+   System.out.println();\n+  } else {\n+\n+  }\n+  a.length();\n+ }\n+\n+ void npe2(String a, String b) {\n+  if (a == null) {\n+   System.out.println();\n   } else {\n \n   }\n@@ -14,6 +24,6 @@ void npe(String a, String b) {\n \n  @Override\n  public boolean equals(Object obj) {\n-  return true;\n+  return false;\n  }\n }";

 @Test
 public void testThatChangedContentCanBeCommented() {
  assertThat(findLineToComment("filename", "patch", 1))//
    .isNull();
 }

 @Test
 public void testThatChangedContentCanBeCommentedNewFile() {
  assertThat(findLineToComment("filename", NEW_DIFF, 1))//
    .isEqualTo(1);

  assertThat(findLineToComment("filename", NEW_DIFF, 1))//
    .isEqualTo(1);

  assertThat(findLineToComment("filename", NEW_DIFF, 5))//
    .isEqualTo(6);

  assertThat(findLineToComment("filename", NEW_DIFF, 5))//
    .isEqualTo(6);
 }

 @Test
 public void testThatChangedContentCanBeCommentedChangedFile() {
  assertThat(findLineToComment("filename", CHANGED_DIFF, 1))//
    .isEqualTo(2);

  assertThat(findLineToComment("filename", CHANGED_DIFF, 1))//
    .isEqualTo(2);

  assertThat(findLineToComment("filename", CHANGED_DIFF, 4))//
    .isEqualTo(5);

  assertThat(findLineToComment("filename", CHANGED_DIFF, 4))//
    .isEqualTo(5);
 }

 @Test
 public void testThatChangedContentCanBeCommentedChangedPartsOfFile() {
  assertThat(findLineToComment("filename", CHANGED_DIFF_2, 6))//
    .isEqualTo(1);

  assertThat(findLineToComment("filename", CHANGED_DIFF_2, 8))//
    .isEqualTo(3);

  assertThat(findLineToComment("filename", CHANGED_DIFF_2, 14))//
    .isEqualTo(9);

  assertThat(findLineToComment("filename", CHANGED_DIFF_2, 21))//
    .isEqualTo(16);
 }

 private Integer findLineToComment(String filename, String patch, int commentLint) {
  return GitHubCommentsProvider// .
    .findLineToComment(new ChangedFile(filename, newArrayList(patch)), commentLint)//
    .orNull();
 }

}
