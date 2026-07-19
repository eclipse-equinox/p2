## Introduction

p2 manages all of its data in repositories. There are two types of
repos, artifact and metadata.

p2 artifact repositories can hold many different forms of the same
artifact. For example, a JAR artifact may exist as

  - a canonical (unmolested) JAR
  - a compressed JAR (assuming the original was not compressed)
  - a pack200 JAR
  - a delta relative to another version of the artifact
  - ...

p2 metadata repositories describe inter-component dependencies and
identify artifacts to install.

The Repository Mirroring applications can be used to mirror artifact and
metadata repositories. In addition, users can do selective mirroring of
artifacts or metadata either to create a more specific mirror (e.g. only
mirror latest code) or merge content into an existing mirror.

This example demonstrates how to mirror a repository using the
repository mirror applications available in the equinox p2 tools.

## Running the Mirroring Tools

Currently, there are two different mirror applications: one for
artifacts and one for metadata.

### Mirroring Metadata

To make an exact mirror of a metadata repository, use the following
arguments. Note that if the target repository does not exist, a new
repository is created with the same properties as the source.

` eclipse -nosplash -verbose`
` -application org.eclipse.equinox.p2.metadata.repository.mirrorApplication`
` -source Insert Source URL (e.g. http://download.eclipse.org/releases/2020-06)`
` -destination Insert Destination URL (e.g. `<file:/tmp/2020-06-Mirror/>`)`

The application will be "eclipsec.exe" (instead of "eclipse") for
Windows installations.

By adding the argument **-writeMode clean**, all installable units in
the target destination will be removed before the mirroring is
performed.

Adding the argument **-destinationName <destination name>** will set the
destination repository's name to the specified destination name. If this
argument is not included the destination will use the source
repository's name if no repository exists at the destination.

### Mirroring Artifacts

To make an exact mirror of an artifact repository, use the following
arguments. Note that if the target repository does not exist, a new
repository is created with the same properties as the source.

` eclipse -nosplash -verbose`
` -application org.eclipse.equinox.p2.artifact.repository.mirrorApplication`
` -source Insert Source URL (e.g. http://download.eclipse.org/releases/2020-06)`
` -destination Insert Destination URL (e.g. `<file:/tmp/2020-06-Mirror/>`)`

Again, the application will be "eclipsec.exe" (instead of "eclipse") for
Windows installations.

By adding the argument **-writeMode clean**, all artifacts in the target
destination will be removed before the mirroring is performed.

Adding the argument **-destinationName <destination name>** will set the
destination repository's name to the specified destination name. If this
argument is not included the destination will use the source
repository's name if no repository exists at the destination.

Adding the argument **-verbose** will enable verbose error reporting and
logging. This will write errors to the
<eclipse workspace>/.metadata/.log file.

Adding the argument **-ignoreErrors** will ensure the mirror application
does not fail in the event of an error. Note: while using this argument
the mirror application may complete without errors but the destination
repository may not include all artifacts from the source repository.

Adding the argument **-raw** instructs the mirroring application to copy
the exact artifact descriptors from source into the destination instead
of initializing new artifact descriptors with properties from the source
descriptors.

Adding the argument **-compare** instructs the mirroring application to
perform a comparison when a duplicate artifact descriptor is found.

Adding the argument **-comparator <comparator ID>** specifies the
mirroring application should use an Artifact Comparator with an ID of
"comparator ID" to compare artifact descriptors. The mirroring
application uses the "MD5 Comparator" to compare the MD5 hash property
of the artifact descriptors if no comparator is defined.

Adding the argument **-baseline <baseline location>** will compare all
artifacts in the source against a known good baseline repository
specified by the baseline location. In the event of conflicting
artifacts, precedence is given to the baseline repository. Can be
combined with **-comparator <comparator ID>**.

[Repository Mirroring](Category:Equinox_p2 "wikilink")