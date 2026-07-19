This page was used as part of the planning process for creating the p2
1.0 release (part of the [Galileo](Galileo "wikilink") simultaneous
release). The specific features listed here may or may not be present in
any particular version of p2. Readers interested in additional *under
the covers* details should see the [technical
specs](Equinox_p2_1.0_Technical_Specs "wikilink") page.

## User Interaction

  - **Managing other profiles**
    p2 inherently supports the management of profiles other than the one
    currently running the p2 infrastructure. Priority: 1
  - **Drag and drop installation**
    Users can install new function by dragging and dropping the related
    files (JARs, directories, zips, ...) on a running Eclipse. Priority:
    2
  - **Browser-based installation**
    Installation of new function can be triggered by users clicking on a
    link in a browser. Priority: 3
  - **Directory monitoring**
    Users can designate any number of directories to be watched. When
    installable elements (e.g., bundle JARs and directories) are copied
    into or remove from watched directories, their contents are
    installed or uninstalled (respectively). Priority: 1
  - **Update scheduling and policies**
    p2 supports a number of update policies allowing users to, for
    example, set update polling periods, schedule updates and have
    detected updates automatically downloaded. Priority: 1
  - **Headless operation**
    All p2 function is accessible through command-line or programmatic
    interfaces. Complete installation operations can be performed
    without a graphical user interface. Some operations support the use
    of response files to silently provide input. Priority: 2
  - **Remembered signers**
    Users are able to accept signer certificates and not be asked each
    time such *remembered* certificates are encountered. The set of
    remembered certificates is managed through a preference page that
    allows for certificates to be added and removed. Priority: 2
  - **Integrated user prompting**
    Prompts for security information (e.g., login, certificate trust,
    ...) are consistent and well-integrated into the user workflow.
    Priority: 2
  - **Internationalization**
    p2 will provide an internationalized installation experience.
    Priority 1.

## Download technology

  - **Automatic detection of proxy/socks settings from the
    OS/Browser**
    p2 uses the standard Eclipse proxy and socks settings management
    system and integrates such settings from the OS and browsers.
    Priority: 2
  - **Adaptive downloads and mirror selection**
    p2 dynamically adapts its artifact download strategy based the
    characteristics of the servers available, the connection speeds and
    the system being provisioned. Retries are automatically attempted
    and mirrors re-selected depending on failures. Priority: 1
  - **Download integrity through MD5/SHA1 and signature verification**
    The integrity of downloaded artifacts can be verified using MD5/SHA1
    hashing algorithms and/or signature verification. Priority: 1
  - **Integrated compression technologies**
    p2 allows artifact repositories to maintain artifacts in a variety
    of formats (e.g., compressed using pack200, JAR and binary deltas
    relative to previous versions, etc.). This can dramatically reduce
    the bandwidth requirements for new software installations. Priority:
    1
  - **Peer-to-peer downloads**
    Since all downloads in p2 are based on a mirroring metaphor,
    artifacts and metadata can come from repositories on central servers
    or peer machines on a local network. Priority 2
  - **Transparent restart**
    Aborted installations and downloads can be restarted without
    refetching. Priority: 1
  - **Download time estimation**
    Estimation of the download time as the download progresses.
    Priority: 2
  - **Repository seeding**
    Repositories are able to reference other repositories and thus
    inform p2 of additional sources of artifacts and metadata. Priority:
    1
  - **Media support**
    p2 supports and properly manages the interaction with repositories
    stored on removable and *volume-oriented* media such as CDs, DVDs.
    Priority: 1

## Security

  - **Metadata signing**
    Metadata is signed to ensure content integrity. Priority: 3
  - **Secure transports (https, ...)**
    Secure transports such as https are supported. Priority: 1
  - **Repository trust**
    p2 has the ability to identify repositories as trusted or untrusted
    as well as white and black lists of domains housing repositories.
    Priority: 2
  - **Repository authentication**
    p2 supports a variety of mechanisms for authenticating to servers.
    Priority: 2
  - **JAR signature verification**
    Signed JARs downloaded from untrusted repositories are verified to
    establish trust. Priority: 1

## Core facilities

  - **Generic Metadata**
    Underlying p2 is a generic metadata model of *Installable Units*. p2
    metadata captures dependencies on non-Eclipse/OSGi based elements
    (e.g., JREs, native code, other applications, ...) as well as on
    physical elements of the machine (e.g., number of CPUs, amount of
    memory or drive space). Priority: 1
  - **Shared (multi-user) installs**
    Scenarios where Eclipse installs are shared across multiple users is
    streamlined. Priority: 1
  - **Bundle pooling**
    p2 *pools* the set of bundles installed across the profiles it
    manages such that any given bundle appears only once on disk. This
    saves disk space as well as dramatically speeding subsequent
    installation operations. Priority: 1
  - **Garbage collection of unused bundles**
    Bundles no longer used in any managed profiles are garbage collected
    according to a flexible policy. Priority: 1
  - **Resilience to install problems**
    p2 provides a best effort approach to ensure that failed
    installations do not leave the system in an inconsistent state. This
    includes a *safe mode* for the provisioning infrastructure itself.
    Priority: 1
  - **Fix delivery**
    Fixes to existing installed function can be installed and
    uninstalled without *updating* the base function. Priority: 1
  - **Sequenced provisioning**
    Users and developers can mandate that various update and install
    operations must be executed prior to attempting subsequent
    operations. Priority: 2
  - **Staged provisioning**
    Provisioning operations can be staged such that all required
    artifacts are downloaded and then, at some later time, the actual
    installation and configuration executed. Priority: 2
  - **Fine grain installation**
    p2 supports the installation of individual Installable Units as well
    as groups of Installable Units. Since typically one IU represents
    one bundle, p2 allows for the installation of individual bundles.
    Priority: 1
  - **Dynamic dependency discovery**
    When p2 is asked to install an IU it can optionally attempt to
    satisfy all prerequisites by discovering and installing other IUs
    that supply the required capabilities. Priority: 2
  - **Managing non-running systems**
    p2 is able to manage Eclipse profiles even when the profile is not
    active/running. Priority: 1
  - **Managing running systems**
    p2 is able to manage and properly interact with running Eclipse
    profiles. For example, triggering restarts of the remove system as
    needed. Priority: 2
  - **Rollback**
    Users can restore previous states of a profile. Priority: 1
  - **Profile interchange**
    Profiles can be manipulated and exchanged between users. This allows
    previous setups to be stored and recreated and for users to exchange
    profiles. Priority: 3
  - **Revert to the previous install**
    When an installation succeeds but is not satisfactory, users can
    revert the system to the *exact* same state as it was before.
    Priority: 3
  - **OS integration**
    Applications installed using p2 can be tightly integrated with the
    underlying operating system. For example, desktop shortcuts,
    registry entries, etc. can be deployed as part of the installation.
    Priority: 2
  - **Governor**
    The governor represents an authority allowing or vetoing
    provisioning operations. Priority 2.

## UM Compatibility

  - **Update site integration**
    p2 is able to read existing update sites created for use with Update
    Manager. Indexing and conversion tools are provided for optimizing
    the use of such sites. Priority: 1
  - **Feature compatibility**
    The feature data structure is not part of the p2 infrastructure but
    p2 allows Update Manager Features to be published to metadata and
    artifact repositories. Priority: 1
  - **Feature Install Handlers**
    Feature install handlers continue to work in p2 with some
    restrictions. A migration/porting guide helps developers in moving
    to the new infrastructure. Priority: 1
  - **Links directory**
    p2 includes tooling to publish existing Eclipse installs into
    metadata and artifact repositories. This includes the correct
    traversal of *links* directories. Priority: 1

## Tooling

  - **Generation of p2 repositories at build time**
    The PDE build mechanism produces metadata and artifact repositories
    as part of the normal build. Priority: 2
  - **Generation of p2 enabled products at build time**
    The PDE build mechanism produces all of the p2 related artifacts and
    metadata (e.g., install registry, profile, ...) when RCP apps are
    being built. This allows applications deployed as zips to be p2
    enabled out of the box.

<!-- end list -->

  - **Streamlined p2 self-hosting**
    PDE incrementally and continuously produces p2 metadata and artifact
    information based on the contents of the workspace and target
    platform. This simplifies the development of p2-enabled applications
    by eliminating the need for time-consuming *deployment* and export
    cycles while testing and allowing developers to install bundles
    directly from their workspace without exporting or deploying.
    Priority: 2
  - **Provisioning the target**
    PDE's Target Provisioner mechanism has been extended to allow the
    use of p2 when adding bundles to the target platform. This allows
    bundle developers to benefit from all facilities in p2 when managing
    their targets. Priority: 2
  - **Repository browsers and editors**
    p2 tooling includes browsers and editors for the artifact and
    metadata repositories. Users can view, add and remove elements from
    local and remote p2 repositories. Priority: 1
  - **Migration tools**
    Developers can deploy existing features and Eclipse product
    configurations into p2 repositories using p2 Publisher tools that
    automatically transform runtime and Update Manager markup into p2
    data structures and add this data and artifacts to the relevant
    repositories. Priority: 1
  - **Repository Optimizers**
    p2 includes tools that analyze, transform and optimize the artifacts
    in an artifact repository to improve download time, enhance
    security, etc. Priority: 1
  - **Mirroring tools**
    Artifact and metadata repositories can be duplicated in whole or in
    part using a set of tools included in p2. Priority: 1
  - **Metadata Authoring**
    p2 tooling will offer the ability to author installable units.
    Priority 3.

[Feature list](Category:Equinox_p2 "wikilink")