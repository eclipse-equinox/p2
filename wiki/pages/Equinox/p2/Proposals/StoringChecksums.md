p2 is able to check integrity of downloaded artifacts using [MD5
algorithm](https://en.wikipedia.org/wiki/MD5) only.  is going to add
support for SHA-256 algorithm but the way artifact's MD5 checksum stored
in artifact metadata is not ready for such minor extension ([Gerrit
change \#59612](https://git.eclipse.org/r/59612) shows how adding
SHA-256 support looks like using the same approach as MD5).

In a long run, however, this solution is not future-proof.

## Separate property for every checksum type (artifact + download) and supported algorithm

Proposed implementation: [Change
\#59612](https://git.eclipse.org/r/59612)

Follows current scheme and stores SHA-256 checksums in the two new
properties, `artifact.sha256` and `download.sha256`:

<code>

` `<artifact classifier='osgi.bundle' id='org.eclipse.osgi' version='3.4.3.R34x_v20081215-1030'>
`   `<properties>
`     `<property name='artifact.md5' value='58057045158895009b845b9a93f3eb6e'/>
`     `<property name='artifact.sha256' value='58057045158895009b845b9a93f3eb6e58057045158895009b845b9a93f3eb6e'/>
`     `<property name='download.md5' value='58057045158895009b845b9a93f3eb6e'/>
`     `<property name='download.sha256' value='58057045158895009b845b9a93f3eb6e58057045158895009b845b9a93f3eb6e'/>
`   `</properties>
` `</artifact>

</code>

Problems:

  - New fields should be added to
    [`org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor`](https://git.eclipse.org/r/#/c/59612/15/bundles/org.eclipse.equinox.p2.repository/src/org/eclipse/equinox/p2/repository/artifact/IArtifactDescriptor.java).
    Deprecating such algorithm in the future and removing these
    properties is an API breaking change.
  -
  -
## Separate property for artifact and download checksums

Proposed implementation: [Change
\#69560](https://git.eclipse.org/r/#/c/69560/)

Two new properties, `artifact.checksums` and `download.checksums`, store
a semi-colon separated list of checksums. Each checksum is a key-value
pair `algotrithm,checksum` separated with `=`:

<code>

` `<artifact classifier='osgi.bundle' id='org.eclipse.osgi' version='3.4.3.R34x_v20081215-1030'>
`   `<properties>
`     `<property name='artifact.checksums' value='md5=58057045158895009b845b9a93f3eb6e;sha256=58057045158895009b845b9a93f3eb6e58057045158895009b845b9a93f3eb6e'/>
`     `<property name='download.checksums' value='md5=58057045158895009b845b9a93f3eb6e;sha256=58057045158895009b845b9a93f3eb6e58057045158895009b845b9a93f3eb6e'/>
`   `</properties>
` `</artifact>

</code>

  - Adding/removing algorithms requires no changes to the API.
  - Adding implementation for the new algorithm still require code
    changes:
      - extend
        [`org.eclipse.equinox.internal.p2.artifact.processors.checksum.ChecksumVerifier`](https://git.eclipse.org/r/#/c/69560/2/bundles/org.eclipse.equinox.p2.artifact.repository/src/org/eclipse/equinox/internal/p2/artifact/processors/checksum/ChecksumVerifier.java)
        (see
        [`MD5Verifier`](https://git.eclipse.org/r/#/c/69560/2/bundles/org.eclipse.equinox.p2.artifact.repository/src/org/eclipse/equinox/internal/p2/artifact/processors/checksum/MD5Verifier.java)
        and
        [`SHA256Verifier`](https://git.eclipse.org/r/#/c/69560/2/bundles/org.eclipse.equinox.p2.artifact.repository/src/org/eclipse/equinox/internal/p2/artifact/processors/checksum/SHA256Verifier.java))
      - register new implementation in
        [`org.eclipse.equinox.internal.p2.artifact.processors.checksum.ChecksumUtilities`](https://git.eclipse.org/r/#/c/69560/2/bundles/org.eclipse.equinox.p2.artifact.repository/src/org/eclipse/equinox/internal/p2/artifact/processors/checksum/ChecksumUtilities.java)
      -
  - `org.eclipse.equinox.internal.p2.artifact.processors.checksum.ChecksumVerifier`
    uses `java.security.MessageDigest` thus limiting number of supported
    algorithms to MD5 and SHA-256 only.

## XML

### Option \#1

<code>

` `<artifact classifier='osgi.bundle' id='org.eclipse.osgi' version='3.4.3.R34x_v20081215-1030'>
`   `<checksums>
`     `<checksum>
`       `<property name='algorithm' value='md5'/>
`       `<property name='artifact' value='58057045158895009b845b9a93f3eb6e'/>
`       `<property name='download' value='58057045158895009b845b9a93f3eb6e'/>
`     `</checksum>
`     `<checksum>
`       `<property name='algorithm' value='sha256'/>
`       `<property name='artifact' value='58057045158895009b845b9a93f3eb6e58057045158895009b845b9a93f3eb6e'/>
`       `<property name='download' value='58057045158895009b845b9a93f3eb6e58057045158895009b845b9a93f3eb6e'/>
`     `</checksum>
`   `</checksums>
` `</artifact>

</code>

### Option \#2

<code>

` `<artifact classifier='osgi.bundle' id='org.eclipse.osgi' version='3.4.3.R34x_v20081215-1030'>
`   `<checksums>
`     `<checksum
        algorithm="md5"
        download="58057045158895009b845b9a93f3eb6e58057045158895009b845b9a93f3eb6e"
        artifact="58057045158895009b845b9a93f3eb6e58057045158895009b845b9a93f3eb6e"/>
`     `<checksum
        algorithm="sha256"
        download="58057045158895009b845b9a93f3eb6e58057045158895009b845b9a93f3eb6e58057045158895009b845b9a93f3eb6e58057045158895009b845b9a93f3eb6e"
        artifact="58057045158895009b845b9a93f3eb6e58057045158895009b845b9a93f3eb6e58057045158895009b845b9a93f3eb6e58057045158895009b845b9a93f3eb6e"/>
`   `</checksums>
` `</artifact>

</code>

### Option \#3

<code>

` `<artifact classifier='osgi.bundle' id='org.eclipse.osgi' version='3.4.3.R34x_v20081215-1030'>
`   `<checksums>
`     `<checksum>
`       `<algorithm>`md5`</algorithm>
`       `<download>`58057045158895009b845b9a93f3eb6e`</download>
`       `<artifact>`58057045158895009b845b9a93f3eb6e`</artifact>
`     `</checksum>
`     `<checksum>
`       `<algorithm>`sha256`</algorithm>
`       `<download>`58057045158895009b845b9a93f3eb6e58057045158895009b845b9a93f3eb6e`</download>
`       `<artifact>`58057045158895009b845b9a93f3eb6e58057045158895009b845b9a93f3eb6e`</artifact>
`     `</checksum>
`   `</checksums>
` `</artifact>

</code>

[Category:Equinox p2](Category:Equinox_p2 "wikilink")