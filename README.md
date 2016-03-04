# Violation Comments to GitHub Lib [![Build Status](https://travis-ci.org/tomasbjerre/violation-comments-to-github-lib.svg?branch=master)](https://travis-ci.org/tomasbjerre/violation-comments-to-github-lib) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/se.bjurr.violations/violation-comments-to-github/badge.svg)](https://maven-badges.herokuapp.com/maven-central/se.bjurr.violations/violation-comments-to-github)

This is a library that adds violation comments from static code analysis to GitHub.

It uses [Violation Comments Lib](https://github.com/tomasbjerre/violation-comments-lib) and supports the same formats as [Violations Lib](https://github.com/tomasbjerre/violations-lib).

Very easy to use with a nice builder patternn
```
  violationsToGitHubApi() //
    .withViolations(".*/findbugs/.*\\.xml$", FINDBUGS, rootFolder) //
    .withViolations(".*/checkstyle/.*\\.xml$", CHECKSTYLE, rootFolder) //
    .limitNumberOfCommentsTo(20) // Optional
    .usingCredentials("username","password") // This is Optional!
    .usingOAuth2Token("token") // This is Optional!
    .toPullRequest("organization","repository",repositoryId);
```

Authentication can be done by supplying username/password or OAuth2Token in the builder. If these are not supplied, the lib will look for these environment variables:
 * *github_oauth2token*
 * *github_username* and *github_password*

## Usage
This software can be used:
 * With a [Gradle plugin](https://github.com/tomasbjerre/violation-comments-to-github-gradle-plugin).
 * With a [Maven plugin](https://github.com/tomasbjerre/violation-comments-to-github-maven-plugin).

## Developer instructions

To build the code, have a look at `.travis.yml`.

To do a release you need to do `./gradlew release` and release the artifact from [staging](https://oss.sonatype.org/#stagingRepositories). More information [here](http://central.sonatype.org/pages/releasing-the-deployment.html).
