# What is this

This is the implementation of an error handling extension that is suited for HTTP based integrations in Syndesis (API provider, webhook). When added to integration all steps that follow it will be handeled so that the HTTP status code is set for all errors to the provided HTTP status code and HTTP body will set to empty. This extension can be added multiple times to an integration flow and the last step before the step that reports an error will handle the error.

# Building

Run:
```shell
$ ./mvnw
```

There should be `target/syndesis-http-error-handling-extension-1.0.0.jar` that you can upload to Syndesis.
