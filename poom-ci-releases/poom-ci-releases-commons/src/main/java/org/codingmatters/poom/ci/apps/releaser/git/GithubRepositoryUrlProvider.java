package org.codingmatters.poom.ci.apps.releaser.git;

@FunctionalInterface
public interface GithubRepositoryUrlProvider {
    String url(String repository);

    static GithubRepositoryUrlProvider ssh() {
        return repository -> String.format("git@github.com:%s.git", repository);
    }

    static GithubRepositoryUrlProvider httpsWithToken(String token) {
        return repository -> String.format("https://%s@github.com/%s.git", token, repository);
    }
}
