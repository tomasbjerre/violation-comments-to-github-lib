# Violation Comments to GitHub Lib

[![Maven Central](https://img.shields.io/maven-central/v/se.bjurr.violations/violation-comments-to-github-lib.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/se.bjurr.violations/violation-comments-to-github-lib)

This is a library that adds violation comments from static code analysis to GitHub.

It uses [Violation Comments Lib](https://github.com/tomasbjerre/violation-comments-lib) and supports the same formats as [Violations Lib](https://github.com/tomasbjerre/violations-lib).
 
Very easy to use with a nice builder pattern
```
  violationsToGitHubApi() //
    .withViolations(".*/findbugs/.*\\.xml$", FINDBUGS, rootFolder) //
    .withViolations(".*/checkstyle/.*\\.xml$", CHECKSTYLE, rootFolder) //
    .withUsername("username") // This is Optional!
    .withPassword("password") // This is Optional!
    .usingOAuth2Token("token") // This is Optional!
    .withRepositoryOwner("repositoryOwner")
    .withRepositoryName("repositoryName")
    .withPullRequestId("pullRequestId")
    .toPullRequest();
```

Authentication can be done by supplying username/password or OAuth2Token in the builder. 

## Usage

This software can be used:
 * From [Command Line](https://github.com/tomasbjerre/violation-comments-to-github-command-line).

# Github supports SARIF

This is an alernative to this tool, but something you may want to explore.

You can transform the violation reports to SARIF:

```sh
npx violations-command-line -sarif sarif-report.json \
  -v "FINDBUGS" "." ".*spotbugs/main\.xml$" "Spotbugs" \
  -v "CHECKSTYLE" "." ".*checkstyle/main\.xml$" "Checkstyle" \
  -v "PMD" "." ".*pmd/main\.xml$" "PMD" \
  -v "JUNIT" "." ".*test/TEST-.*\.xml$" "JUNIT"
```

And upload Sarif to Github. I do this with [Github action](https://github.com/github/codeql-action):

```yaml
steps:
  - name: Do analysis
    shell: bash
    run: |
      echo do your analysis here
  - name: Transorm static code analysis to SARIF
    if: ${{ (success() || failure()) }}
    run: |
      npx violations-command-line -sarif sarif-report.json \
      -v "FINDBUGS" "." ".*spotbugs/main\.xml$" "Spotbugs" \
      -v "CHECKSTYLE" "." ".*checkstyle/main\.xml$" "Checkstyle" \
      -v "PMD" "." ".*pmd/main\.xml$" "PMD" \
      -v "JUNIT" "." ".*test/TEST-.*\.xml$" "JUNIT"
  - uses: github/codeql-action/upload-sarif@v2
    if: ${{ (success() || failure()) }}
    with:
      sarif_file: sarif-report.json
```

My setup is here:
https://github.com/tomasbjerre/.github/tree/master
