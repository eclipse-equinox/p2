# Possible legacy cleanups

This file records code in this bundle that looks like a candidate for removal or for moving into a separate bundle, for example `org.eclipse.equinox.p2.transport.ecf.legacy`.
Nothing listed here is done yet.
Each entry should be checked for its actual usage before it is changed.

## Fallback to the JRE HTTP client

ECF signals with the HTTP status code 477 that a request has to be retried with the JRE HTTP client.
The following code only exists to handle that case:

* `RepositoryStatusHelper.checkJREHttpClientRequired` throws `JREHttpClientRequiredException` for error code 477.
* `RepositoryTransport` catches the exception in three places, retries once and calls `Activator.useJREHttpClient()`.
* `Activator.useJREHttpClient()` uses `IFileTransferProtocolToFactoryMapper` (with the service tracker `protocolToFactoryMapperTracker`) to remove the `http` and `https` browse, retrieve and send factories, so that ECF falls back to the JRE based provider.
* `FileReader` and `FileInfoReader` declare `JREHttpClientRequiredException` in many `throws` clauses.
* `JREHttpClientRequiredException` itself lives in `org.eclipse.equinox.p2.repository`.

Possible directions:

* Remove the fallback if the ECF providers in use no longer report 477.
* Move it into a legacy bundle that is only needed together with the original ECF providers.

Effect on the ECF dependencies: `IFileTransferProtocolToFactoryMapper` (package `org.eclipse.ecf.provider.filetransfer`) would no longer be referenced by this bundle.
The package could then be dropped from the imports and exports instead of being provided as a substitute.

## Starting ECF bundles by name

`Activator` starts the bundles `org.eclipse.ecf` and `org.eclipse.ecf.provider.filetransfer` by symbolic name.
This is a hidden bundle dependency.
If these bundles are not installed, nothing is started.
It could move to the legacy bundle as well.
