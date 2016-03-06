# Violation Comments to GitHub Lib [![Build Status](https://travis-ci.org/tomasbjerre/violation-comments-to-github-lib.svg?branch=master)](https://travis-ci.org/tomasbjerre/violation-comments-to-github-lib) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/se.bjurr.violations/violation-comments-to-github-lib/badge.svg)](https://maven-badges.herokuapp.com/maven-central/se.bjurr.violations/violation-comments-to-github-lib)

This is a library that adds violation comments from static code analysis to GitHub.

It uses [Violation Comments Lib](https://github.com/tomasbjerre/violation-comments-lib) and supports the same formats as [Violations Lib](https://github.com/tomasbjerre/violations-lib):
 * [_Checkstyle_](http://checkstyle.sourceforge.net/)
 * [_CPPLint_](https://github.com/theandrewdavis/cpplint)
 * [_CPPCheck_](http://cppcheck.sourceforge.net/)
 * [_CSSLint_](https://github.com/CSSLint/csslint)
 * [_Findbugs_](http://findbugs.sourceforge.net/)
 * [_Flake8_](http://flake8.readthedocs.org/en/latest/) ([_PyLint_](https://www.pylint.org/), [_Pep8_](https://github.com/PyCQA/pycodestyle), [_Mccabe_](https://pypi.python.org/pypi/mccabe), [_PyFlakes_](https://pypi.python.org/pypi/pyflakes))
 * [_JSHint_](http://jshint.com/)
 * _Lint_ A common XML format, used by different linters.
 * [_PerlCritic_](https://github.com/Perl-Critic)
 * [_PMD_](https://pmd.github.io/)
 * [_ReSharper_](https://www.jetbrains.com/resharper/)
 * [_XMLLint_](http://xmlsoft.org/xmllint.html)
 
Very easy to use with a nice builder pattern
```
  violationsToGitHubApi() //
    .withViolations(".*/findbugs/.*\\.xml$", FINDBUGS, rootFolder) //
    .withViolations(".*/checkstyle/.*\\.xml$", CHECKSTYLE, rootFolder) //
    .limitNumberOfCommentsTo(20) // Optional
    .usingCredentials("username","password") // This is Optional!
    .usingOAuth2Token("token") // This is Optional!
    .toPullRequest("organization","repository",repositoryId);
```

Authentication can be done by supplying username/password or OAuth2Token in the builder. 

## Usage
This software can be used:
 * With a [Gradle plugin](https://github.com/tomasbjerre/violation-comments-to-github-gradle-plugin).
 * With a [Maven plugin](https://github.com/tomasbjerre/violation-comments-to-github-maven-plugin).
 * With a [Jenkins plugin](https://github.com/tomasbjerre/violation-comments-to-github-jenkins-plugin).

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
