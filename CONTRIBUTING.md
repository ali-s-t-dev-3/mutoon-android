# Contributing to Mutoon

Thank you for contributing.

## Ticket, branch, and pull-request workflow

1. Start from an assigned Jira ticket.
2. Create one branch named `mut-<ticket-number>-<short-slug>`, for example `mut-2-bootstrap`.
3. Keep the branch and pull request limited to that ticket.
4. Link the Jira ticket in the pull-request title or description.
5. Obtain review and merge the pull request before starting the next implementation ticket.

Do not merge your own pull request.

## Required checks

Run the following before requesting review:

```text
./gradlew clean assembleDebug lintDebug testDebugUnitTest
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Privacy, security, and rights

Mutoon is fully offline. Contributions must not introduce the Android `INTERNET` permission, advertising, analytics, telemetry, crash reporting, accounts, or cloud services.

Never commit signing keys or passwords, credentials, user data, supplied PDFs, or supplied screenshots. Book content is not automatically covered by the repository's GPL license; redistribution rights must be confirmed separately before content is added publicly.
