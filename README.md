# Violation Comments to GitHub Lib [![Build Status](https://travis-ci.org/tomasbjerre/violation-comments-to-github-lib.svg?branch=master)](https://travis-ci.org/tomasbjerre/violation-comments-to-github-lib) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/se.bjurr.violations/violation-comments-to-github-lib/badge.svg)](https://maven-badges.herokuapp.com/maven-central/se.bjurr.violations/violation-comments-to-github-lib) [ ![Bintray](https://api.bintray.com/packages/tomasbjerre/tomasbjerre/se.bjurr.violations%3Aviolation-comments-to-github-lib/images/download.svg) ](https://bintray.com/tomasbjerre/tomasbjerre/se.bjurr.violations%3Aviolation-comments-to-github-lib/_latestVersion)

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
 * With a [Gradle plugin](https://github.com/tomasbjerre/violation-comments-to-github-gradle-plugin).
 * With a [Maven plugin](https://github.com/tomasbjerre/violation-comments-to-github-maven-plugin).
 * With a [Jenkins plugin](https://github.com/tomasbjerre/violation-comments-to-github-jenkins-plugin).
 * From [Command Line](https://github.com/tomasbjerre/violation-comments-to-github-command-line).

You may also checkout [this blog post](http://bjurr.se/static-code-analysis-with-github/) that explains how to set it up with Travis.

## Travis
To set this up in Travis, you will need to create a GitHub OAuth2 token.
```
curl -u 'yourgithubuser' -d '{"note":"Violation comments"}' https://api.github.com/authorizations
```

The token needs to be encrypted before added to your `.travis.yml`.
```
sudo apt-get install ruby-dev
gem install travis
travis encrypt export GITHUB_OAUTH2TOKEN=YOUR TOKEN HERE
```

Now add it to `.travis.yml` like this.
```
sudo: false  
language: java  
env:  
  - secure: "YOUR ENCRYPTED TOKEN HERE"
jdk:  
  - oraclejdk7
script:  
  - ./gradlew build violationCommentsToGitHub -DGITHUB_PULLREQUESTID=$TRAVIS_PULL_REQUEST -DGITHUB_OAUTH2TOKEN=$GITHUB_OAUTH2TOKEN -i --stacktrace
notifications:  
  email: false
```

Here I used [Gradle plugin](https://github.com/tomasbjerre/violation-comments-to-github-gradle-plugin) but you can do the same thing with [Maven plugin](https://github.com/tomasbjerre/violation-comments-to-github-maven-plugin).


## Developer instructions

To build the code, have a look at `.travis.yml`.

To do a release you need to do `./gradlew release` and release the artifact from [staging](https://oss.sonatype.org/#stagingRepositories). More information [here](http://central.sonatype.org/pages/releasing-the-deployment.html).
