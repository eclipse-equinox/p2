p2 artifact repositories can hold many different forms of the same
artifact. For example, a JAR artifact may exist as

  - a canonical (unmolested) JAR
  - a compressed JAR (assuming the original was not compressed)
  - a pack200 JAR
  - a delta relative to another version of the artifact
  - ...

This document outlines the behavior of p2 in the various scenarios and
details how to populate a repository with a optimized forms of
artifacts.

__TOC__

## Pack200 Optimization

Pack200 is a standard part of Java 5. It is a rather radical compression
algorithm that is class file aware. It typically results in
approximately **70% smaller JARs** assuming the JARs are mostly code.
Pack200 has no effect on non-class files. Packed JARs can be supplied by
the client populating a repository or added after the fact by running
the Pack 200 repository optimizer. See the
**org.eclipse.equinox.p2.artifact.optimizers** bundle and the
application
**org.eclipse.equinox.p2.artifact.optimizers.pack200optimizer**.

Once the repository has been optimized, any agent pointing at the
repository will automatically choose to download the packed form of the
artifact if the repository is not local to the agent. In the case of
local repositories it is more efficient to simply copy the canonical
form if available.

Note that the agent running must have the
**org.eclipse.equinox.p2.jarprocessor** bundle installed and resolved.

Arguments:

  - \-artifactRepository the URL of the repository to optimize

## Class Delta Optimization

When JARs change often only a few classes in them change. The difference
between two JARs can then be captured as a set of deletions,
replacements and additions. These can themselves be contained in a JAR.
That JAR is called a "delta JAR". The delta JAR can itself be packed,
signed, ... since it is just a normal JAR.

See the **org.eclipse.equinox.p2.artifact.optimizers** bundle and the
application
**org.eclipse.equinox.p2.artifact.optimizers.jardeltaoptimizer**.

## Full Delta Optimization

While class delta optimization only works at the granularity of files,
full delta optimization (also called binary delta optimization) involves
computing differences between different version of an artifact at the
byte level. Old and new versions of the artifact are compared, and a
binary delta is created that describes the differences between them. The
small binary delta is transferred to the client, and then the new
artifact is reassembled by combining the delta with the old version of
the artifact. The p2 implementation currently uses the
[JBDiff](http://freshmeat.net/projects/jbdiff) utility for computing
binary deltas.

  - The processing steps to create the delta and reconstitute the final
    files are in place. So is the repo optimization application.
  - Because of limitations in the download manager, the delta
    representation of the artifact will only get picked if the repo
    being contacted has only one "non-canonical" artifact in it (e.g. if
    you had a pack200 version and a delta version of an artifact, then
    there is no guarantee over which one would be used).
  - Due to some timing constraints, this will not be officially
    supported in the 1.0 release of p2

See the **org.eclipse.equinox.p2.artifact.optimizers** bundle and the
application
**org.eclipse.equinox.p2.artifact.optimizers.jbdiffoptimizer**.

[Repository Optimization](Category:Equinox_p2 "wikilink")