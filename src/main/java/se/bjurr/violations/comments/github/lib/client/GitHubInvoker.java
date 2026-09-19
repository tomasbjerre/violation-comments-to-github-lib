package se.bjurr.violations.comments.github.lib.client;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.logging.Level.INFO;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Base64;
import se.bjurr.violations.lib.ViolationsLogger;

public class GitHubInvoker {

  public enum Method {
    DELETE,
    GET,
    POST
  }

  private static final HttpClient HTTP_CLIENT =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

  public GitHubResponse invoke(
      final ViolationsLogger violationsLogger,
      final String url,
      final Method method,
      final String postContent,
      final String oAuth2Token,
      final String username,
      final String password) {
    final String authorizationValue;
    if (oAuth2Token != null) {
      authorizationValue = "Bearer " + oAuth2Token;
    } else {
      final String userAndPass = username + ":" + password;
      authorizationValue =
          "Basic " + Base64.getEncoder().encodeToString(userAndPass.getBytes(UTF_8));
    }
    return this.doInvoke(violationsLogger, url, method, postContent, authorizationValue);
  }

  private GitHubResponse doInvoke(
      final ViolationsLogger violationsLogger,
      final String url,
      final Method method,
      final String postContent,
      final String authorizationValue) {
    try {
      final HttpRequest.Builder requestBuilder =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .timeout(Duration.ofSeconds(30))
              .header("Authorization", authorizationValue)
              .header("Accept", "application/vnd.github+json")
              .header("X-GitHub-Api-Version", "2022-11-28")
              .header("Content-Type", "application/json; charset=utf-8");

      switch (method) {
        case DELETE:
          requestBuilder.DELETE();
          break;
        case GET:
          requestBuilder.GET();
          break;
        case POST:
          requestBuilder.POST(
              postContent == null
                  ? BodyPublishers.noBody()
                  : BodyPublishers.ofString(postContent, UTF_8));
          break;
        default:
          throw new IllegalArgumentException("Unsupported http method: " + method);
      }

      final HttpResponse<String> response =
          HTTP_CLIENT.send(requestBuilder.build(), BodyHandlers.ofString(UTF_8));
      final int statusCode = response.statusCode();
      final String body = response.body();

      final boolean wasNotOk = statusCode < 200 || statusCode >= 300;
      if (wasNotOk) {
        violationsLogger.log(
            INFO,
            method
                + " "
                + url
                + " "
                + statusCode
                + "\nSent:\n"
                + postContent
                + "\nResponse:\n"
                + body);
      } else {
        violationsLogger.log(INFO, method + " " + url + " " + statusCode);
      }

      return new GitHubResponse(statusCode, body);
    } catch (final Exception e) {
      throw new RuntimeException("Error calling:\n" + url + "\n" + method + "\n" + postContent, e);
    }
  }
}
