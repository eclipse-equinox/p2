## User Interaction

  - **Managing other profiles**
    p2 inherently supports the management of profiles other than the one
    currently running the p2 infrastructure. Priority: 1
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Mechanism to identify the profile managing a product. Priority:
        1
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Mechanism to identify the agent (agent data area) managing a
        product. Priority: 1.
      - UI to browse profiles. This may be used in the target management
        as part of the tooling. Priority: 3

<!-- end list -->

  - **Drag and drop installation**
    ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Users can
    install new function by dragging and dropping the related files
    (JARs, directories, zips, ...) on a running Eclipse. Priority: 2
      - Relationship between this and the directory watcher
      - UI to support this kind of metaphor

<!-- end list -->

  - **Browser-based installation**
    Installation of new function can be triggered by users clicking on a
    link in a browser. Priority: 3
      - Mechanism to find running instances of eclipse
      - Mechanism to invoke a p2

<!-- end list -->

  - **Directory monitoring**
    Users can designate any number of directories to be watched. When
    installable elements (e.g., bundle JARs and directories) are copied
    into or remove from watched directories, their contents are
    installed or uninstalled (respectively). Priority: 1
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Support for installing metadata-less bundles (Need to invoke the
        metadata generator). Priority: 1
      - Support for installing metadata-aware bundles. Need to define a
        serialized format of IUs for embedding with the bundle. Do we
        have a concept of artifact repository. Priority: 2
      - Mechanism to control the frequency at which directories are
        being polled. Priority: 2
      - UI to add/remove watched folders and set the polling frequency.
        Priority: 2
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Define
        backward compatibility with UM links and extensions folders.
        Priority: 1
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Integration into the startup sequence. Priority: 1
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Relationship with the shared install and the reconciler.

<!-- end list -->

  - **Update scheduling and policies**
    ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") p2
    supports a number of update policies allowing users to, for example,
    set update polling periods, schedule updates and have detected
    updates automatically downloaded. Priority: 1
      - See also Staged provisioning.
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Need a
        way to notify the user about available installations. Priority:
        1.
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Need a
        way to set the frequency of checks. Priority: 1.

<!-- end list -->

  - **Headless operation**
    All p2 function is accessible through command-line or programmatic
    interfaces. Complete installation operations can be performed
    without a graphical user interface. Some operations support the use
    of response files to silently provide input. Priority: 2
      - Support to get the input from a response file. Define this in
        relationship with the callable User Input service. Priority: 3.
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Define
        applications to invoke p2 from the command line to perform
        installation / uninstallation of IUs. See also
        <http://help.eclipse.org/help33/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/misc/update_standalone.html>
        . Priority: 1.
      - Separation of the installation phase from the configuration
        phase. Priority: 2.

<!-- end list -->

  - **Remembered signers**
    Users are able to accept signer certificates and not be asked each
    time such *remembered* certificates are encountered. The set of
    remembered certificates is managed through a preference page that
    allows for certificates to be added and removed. Priority: 2
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Storage for remembered signers
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Mechanism to present signatures, see also callable User Input.

<!-- end list -->

  - **Integrated user prompting**
    Prompts for security information (e.g., login, certificate trust,
    ...) are consistent and well-integrated into the user workflow.
    Priority: 2
      - Callable User Input service to gather information from the User
        (https://bugs.eclipse.org/bugs/show_bug.cgi?id=206903).
        Priority: 2
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Presentation of licenses. Priority: 1.

<!-- end list -->

  - **Internationalization**
    p2 will provide an internationalized installation experience.
    Priority: 1.
      - Support for DBCS in the metadata, the artifact names, the
        install paths. Priority: 1.
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Ability to install a translated version of a software. Priority:
        1.
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Ability to translate the content of the metadata while browsing
        the repository. Priority: 2.

## Download technology

  - **Automatic detection of proxy/socks settings from the
    OS/Browser**
    ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") p2 uses
    the standard Eclipse proxy and socks settings management system and
    integrates such settings from the OS and browsers. Priority: 2
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") (ECF)
        Ensure platform proxy settings are being used. Priority: 1
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        (Platform) Detection of OS / Browser proxy settings. Priority:3

<!-- end list -->

  - **Adaptive downloads and mirror selection**
    p2 dynamically adapts its artifact download strategy based the
    characteristics of the servers available, the connection speeds and
    the system being provisioned. Retries are automatically attempted
    and mirrors re-selected depending on failures. Priority: 1
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Simple
        mirror selection. Priority: 1.
      - Support to get stats as we go. Priority: 2.
      - Support to query repo for partially obtained artifacts.
        Priority: 2.
      - Interaction of restartable download with processing steps.
        Priority: 2.
      - Query property from artifact repositories. Priority: 2.
      - Query transports available. Priority: 2.

<!-- end list -->

  - **Download integrity through MD5/SHA1 and signature verification**
    The integrity of downloaded artifacts can be verified using MD5/SHA1
    hashing algorithms and/or signature verification. Priority: 1
      - SHA1 processing step and publication steps. Priority: 2.
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") MD5
        processing step and publication steps. Priority: 1.
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Signature verification processing step. Priority: 1.

<!-- end list -->

  - **Integrated compression technologies**
    p2 allows artifact repositories to maintain artifacts in a variety
    of formats (e.g., compressed using pack200, JAR and binary deltas
    relative to previous versions, etc.). This can dramatically reduce
    the bandwidth requirements for new software installations. Priority:
    1
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Pack
        200 processing step and publication step. Priority: 1.
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        General delta processing step and publication step. Priority: 2.
      - Jar delta processing step and publication step. Priority: 2.
      - See also repository optimizers tool.

<!-- end list -->

  - **Peer-to-peer downloads**
    Since all downloads in p2 are based on a mirroring metaphor,
    artifacts and metadata can come from repositories on central servers
    or peer machines on a local network. Priority: 2
      - Publication of local repositories as http. Priority: 2.
      - Announcement of available repositories on the network (e.g.
        SLP). Priority: 2.

<!-- end list -->

  - **Transparent restart**
    Aborted installations and downloads can be restarted without
    refetching. Priority: 1
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Artifact repositories as pools. Priority: 1
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Garbage collector of artifacts. Priority: 1
      - Repositories to support restart for failed downloads. Priority:
        2.

<!-- end list -->

  - **Download time estimation**
    Estimation of the download time as the download progresses.
    Priority: 2

<!-- end list -->

  - **Repository seeding**
    Repositories are able to reference other repositories and thus
    inform p2 of additional sources of artifacts and metadata. Priority:
    1
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Define
        a way to discover other repositories and add them. Priority: 1.

<!-- end list -->

  - **Media support**
    p2 supports and properly manages the interaction with repositories
    stored on removable and *volume-oriented* media such as CDs, DVDs.
    Priority: 2
      - Define repository format for *volume-oriented* repositories.
        Priority: 2
      - Ensure compatibility of the smart download manager with such
        medias. Priority: 2.

## Security

  - **Metadata signing**
    Metadata is signed to ensure content integrity. Priority: 3

<!-- end list -->

  - **Secure transports (https, ...)**
    Secure transports such as https are supported. Priority: 1
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        (Security) Certificate storage
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") (ECF)
        Support for https connection
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        (ECF/Security) ECF / Security integration for prompting and
        storing certificates.

<!-- end list -->

  - **Repository trust**
    p2 has the ability to identify repositories as trusted or untrusted
    as well as white and black lists of domains housing repositories.
    Priority: 2
      - Define white list / black list and integrate into the repository
        managers.
      - Define a notion of trusted repository. How is the trust
        established. What are the implications of being trusted. Where
        is this information stored.

<!-- end list -->

  - **Repository authentication**
    p2 supports a variety of mechanisms for authenticating to servers.
    Priority: 2
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Support to query the user for login / pwd. Priority: 1.

<!-- end list -->

  - **JAR signature verification**
    ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Signed
    JARs downloaded from untrusted repositories are verified to
    establish trust. Priority: 1

## Core facilities

  - **Generic Metadata**
    Underlying p2 is a generic metadata model of *Installable Units*. p2
    metadata captures dependencies on non-Eclipse/OSGi based elements
    (e.g., JREs, native code, other applications, ...) as well as on
    physical elements of the machine (e.g., number of CPUs, amount of
    memory or drive space). Priority: 1
      - Define a generic model of capabilities / requirements. Priority:
        2.
      - Define the way by which some capabilities get populated. For
        example how do we know the OS. See also JRE reconcialiation pb
        which is similar. Priority: 2.
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Separate meta-level capabilities and requirements from the base
        level ones. This factors in the discussion Properties vs.
        Capabilities. Priority: 1.
      - Separate the dependency information from the configuration one.
        \[This needs to be clarified\].
      - Support for canonicalization of the version numbers. Priority:3.
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") See
        also Fix delivery
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") See
        also Update delivery

<!-- end list -->

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") **Shared
    (multi-user) installs**
    Scenarios where Eclipse installs are shared across multiple users is
    streamlined. Priority: 1
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Support Vista. Priority: 1.
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Support Linux. Priority: 1.

<!-- end list -->

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") **Bundle
    pooling**
    ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") p2 *pools*
    the set of bundles installed across the profiles it manages such
    that any given bundle appears only once on disk. This saves disk
    space as well as dramatically speeding subsequent installation
    operations. Priority: 1
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Support for multiple bundle pools per profile. Priority: 1.

<!-- end list -->

  - **Garbage collection of unused bundles**
    Bundles no longer used in any managed profiles are garbage collected
    according to a flexible policy. Priority: 1
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Bundle
        pool garbage collector. Priority: 1.
      - Metadata garbage collector. Priority: 2.
      - Need a way to ensure that a bundle pool is only used by one
        agent to avoid problems with GC. Priority: 1.

<!-- end list -->

  - **Resilience to install problems**
    p2 provides a best effort approach to ensure that failed
    installations do not leave the system in an inconsistent state. This
    includes a *safe mode* for the provisioning infrastructure itself.
    Priority: 1
      - Define a way to recover from p2 information loss.
          - Self contained installs (eclipse 3.3 shape). Priority: 1.
          - Other shapes. Priority: 2.
      - Define a way to recover from a crash during the installation.
        Priority: 2.
          - The engine could persist the operation currently being
            performed. This allows for reboot of the provisioned system
            during an installation, as well as for checkpointing on
            restart in case the system crashed during an installation.
            Priority: 2.
          - Support for Touchpoint actions to store previous state. This
            allows for recovering from a crash. Priority: 2.
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Leave
        the provisioned thing in a functional state when the
        installation is canceled. Priority: 1.
          - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif")
            Touchpoint actions need to capture previous state before
            doing their work. Priority: 1.
      - Resilience to corrupted files. Priority: 1.

<!-- end list -->

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") **Fix
    delivery**
    Fixes to existing installed function can be installed and
    uninstalled without *updating* the base function. Priority: 1
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Define
        metadata for fixes. Priority: 1.
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Define
        the lifecycle of the fixes, when do they get installed, how do
        they get discovered, when are they uninstalled. Priority: 1.

<!-- end list -->

  - **Sequenced provisioning**
    Users and developers can mandate that various update and install
    operations must be executed prior to attempting subsequent
    operations. Priority: 2
      - Define markup identifying such updates.
      - Adapt the director to detect such updates and process them
        accordingly.

<!-- end list -->

  - **Staged provisioning**
    ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
    Provisioning operations can be staged such that all required
    artifacts are downloaded and then, at some later time, the actual
    installation and configuration executed. Priority: 2
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Ability to invoke the engine by specifying different phases.

<!-- end list -->

  - **Fine grain installation**
    ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") p2
    supports the installation of individual Installable Units as well as
    groups of Installable Units. Since typically one IU represents one
    bundle, p2 allows for the installation of individual bundles.
    Priority: 1

<!-- end list -->

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") **Dynamic
    dependency discovery**
    When p2 is asked to install an IU it can optionally attempt to
    satisfy all prerequisites by discovering and installing other IUs
    that supply the required capabilities. Priority: 2
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Explore a resolution algorithm supporting backtracking. Priorty
        1.

<!-- end list -->

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") **Managing
    non-running systems**
    p2 is able to manage Eclipse profiles even when the profile is not
    active/running. Priority: 1
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Headless functionality. Priority: 1.
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") User
        interface. Priority:3 (same than item in the user interaction).

<!-- end list -->

  - **Managing running systems**
    p2 is able to manage and properly interact with running Eclipse
    profiles. For example, triggering restarts of the remove system as
    needed. Priority: 2
      - Ability for touchpoint to cause a runtime to be started /
        stopped / restarted. Priority: 2.
      - Ability to update eclipse.exe. Priority: 2.

<!-- end list -->

  - **Rollback**
    Users can restore previous states of a profile. Priority: 1
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Remember previous state of the system at the profile level and
        allow for returning to a previous state. Priority: 1.
          - Need to name previous state and identify the cause of the
            changes. Priority: 1.
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            Need to group/batch the changes done on a profile and have
            an API allowing for such a thing. Priority: 1.
          - Decide whether or not this is core to the agent. Priority:
            2.
          - Define a mechanism allowing to set the purge policy.
            Priority: 2.

<!-- end list -->

  - **Profile interchange**
    Profiles can be manipulated and exchanged between users. This allows
    previous setups to be stored and recreated and for users to exchange
    profiles. Priority: 3

<!-- end list -->

  - **Revert to the previous install**
    When an installation succeeds but is not satisfactory, users can
    revert the system to the *exact* same state as it was before.
    Priority: 3
      - Remember previous state of the machine at the system level (e.g.
        snapshot the file system, the registry, etc.). Priority:3.
          - Define a phase and an API allowing for such operation to be
            done. Priority:3.
          - Provide a preference to disable such a mechanism and to also
            purge.

<!-- end list -->

  - **OS integration**
    Applications installed using p2 can be tightly integrated with the
    underlying operating system. For example, desktop shortcuts,
    registry entries, etc. can be deployed as part of the installation.
    Priority: 2

<!-- end list -->

  - **Governor**
    The governor represents an authority allowing or vetoing
    provisioning operations. Priority: 2.
      - Review if the AccessController like model is suitable.
      - Define which operations must be protected.
      - Define how it gets the context in which the operation is
        executed.

<!-- end list -->

  - **Framework Admin**
    Framework admin is an API allowing for the transparent manipulation
    of OSGi frameworks. Priority: 1.
      - Review performance on large systems.
      - Rewrite it to address the shortcomings and limit it
        functionalities
        <https://bugs.eclipse.org/bugs/show_bug.cgi?id=209422>.
        Priority: 2.

<!-- end list -->

  - **Simple configurator**
    Simple configurator is the bundle responsible for taking the
    description of a system and applying it.
      - Validate performances on large configurations. Priority: 1.
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Provide an option to have simple configurator play nice with
        others (don't delete bundles it does not know about). Priority:
        1.
      - Decide if simple configurator checks for the start levels
        programmatically. Priority: 1.
      - Review the way start levels are being changed. Priority: 2.

<!-- end list -->

  - **Misc.**
    \* Nested profiles. Ability to have profiles refer to each others.
    Priority: 2.
    \* Variables. Ability to parameterize the installation and
    configuration of an IU. Priority: 2.
    \* File locator. Ability to locate a file within an IU (Priority: 1)
    and from another IU (Priority: 2). This has an implication on the
    actions who refer to files and who lay down files.
    \* Agent data sharing.
    \*\* Need a way to lock the agent data area to prevent multiple
    processes collision. Priority: 1.
    \*\* Need a way to share the agent data area across multiple running
    processes. Priority: 2.
    \* How do we interact with multiple agent data area from within one
    process. Priority: 1.
    \*\* Is it needed?
    \*\* What is needs to be accessed (e.g. profile registry, install
    registry, ...)?
    \* Flavors. Review the concept of flavor and the default flavor we
    ship. Priority: 1.
    \* ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
    Markers. Review and refine the concept of installable unit markers
    and their lifecycle. Priority: 2.
    \* Make the agent dynamic. Priority: 2.
    \* ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
    Scalability. Priority: 1.
    \* ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
    Reconciliation with some "changeable" part of a profile (e.g. JRE).

## UM Compatibility

  - **Update site integration**
    See also [Update Manager and p2
    Compatibility](Update_Manager_and_p2_Compatibility "wikilink")
    p2 is able to read existing update sites created for use with Update
    Manager. Indexing and conversion tools are provided for optimizing
    the use of such sites. Priority: 1
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Metadata generator tool to run over update sites. Priority: 1.
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Support for mapping update site categories in p2 style
        categorization. Priority: 2.
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Define
        representation of categories in p2. Priority: 1.
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Support to install from pure old-style update site. Several
        alternatives:
          - Use the update manager code and have it generates the
            bundles.txt. We may still use the new UI but in degraded
            mode.
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") On
            the fly generation of Metadata / artifact repos, reusing
            MayInstall code.

<!-- end list -->

  - **Feature compatibility**
    The feature data structure is not part of the p2 infrastructure but
    p2 allows Update Manager Features to be published to metadata and
    artifact repositories. Priority: 1
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Does
        p2 continue still deliver the features out of the box?
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") When a
        user uses 3.4 and it has features depending on the features from
        the SDK, what happens?

<!-- end list -->

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") **Feature
    Install Handlers**
    ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Feature
    install handlers continue to work in p2 with some restrictions. A
    migration/porting guide helps developers in moving to the new
    infrastructure. Priority: 1

<!-- end list -->

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")**Links
    directory**
    ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") p2
    includes tooling to publish existing Eclipse installs into metadata
    and artifact repositories. This :includes the correct traversal of
    *links* directories. Priority: 1
      - See Directory monitoring

<!-- end list -->

  - **Misc**

This covers other subtle cases that were made possible by Update
Manager.

:\* ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
Installations are movable when they are self-contained. Priority: 1.

:\* ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Review the
concept of policy files. Priority: 1.

## Tooling

  - **Generation of p2 repositories at build time**
    The PDE build mechanism produces metadata and artifact repositories
    as part of the normal build. Priority: 2
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Application and Ant task to publish content into repositories
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Application and Ant task to generate metadata / artifacts from
        existing bundles / features
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Modification of the build infrastructure to produce artifact and
        metadata repos

<!-- end list -->

  - **Generation of p2 enabled products at build time**
    ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") The PDE
    build mechanism produces all of the p2 related artifacts and
    metadata (e.g., install registry, profile, ...) when RCP apps are
    being built. This allows applications deployed as zips to be p2
    enabled out of the box. Priority: 1.
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Modification of the product build to produce p2 enabled
        products. This may not be necessary if p2 is able of
        bootstrapping itself.

<!-- end list -->

  - **Streamlined p2 self-hosting**
    PDE incrementally and continuously produces p2 metadata and artifact
    information based on the contents of the workspace and target
    platform. This simplifies the development of p2-enabled applications
    by eliminating the need for time-consuming *deployment* and export
    cycles while testing and allowing developers to install bundles
    directly from their workspace without exporting or deploying.
    Priority: 2
      - PDE Model watcher invoking the metadata generation facility and
        generate artifacts
      - Provision on launch
      - Definition of a metadata generator advise format, see if this
        somewhat relates to an externalized format of the IU. Priority:
        1.

<!-- end list -->

  - **Provisioning the target**
    PDE's Target Provisioner mechanism has been extended to allow the
    use of p2 when adding bundles to the target platform. This allows
    bundle developers to benefit from all facilities in p2 when managing
    their targets. Priority: 3
      - Profile browser
      - Discovery of the profile and agent by which an eclipse is
        managed
      - Workflow / User UI to add IUs to the target

<!-- end list -->

  - **Repository browsers and editors**
    p2 tooling includes browsers and editors for the artifact and
    metadata repositories. Users can view, add and remove elements from
    local and remote p2 repositories. Priority: 2
      - Provide viewers to browse repo content
      - Provide addition / removal capabilities of item into repository
      - Provide IU / artifact correlation tool to check if all the
        artifacts of an IU are available in an artifact repository, to
        remove all the artifacts related to an IU, etc.

<!-- end list -->

  - **Metadata Authoring**
    p2 tooling will offer the ability to author installable units.
    Priority:3.
      - Define a serialized format of IUs for edition purpose.
      - Define an editor and a model allowing for ease of edition.
      - ...

<!-- end list -->

  - **Migration tools**
    Developers can deploy existing features and Eclipse product
    configurations into p2 repositories using p2 Publisher tools that
    automatically transform runtime and Update Manager markup into p2
    data structures and add this data and artifacts to the relevant
    repositories. Priority: 1 if p2 can not bootstrap itself.

<!-- end list -->

  - **Repository Optimizers**
    p2 includes tools that analyze, transform and optimize the artifacts
    in an artifact repository to improve download time, enhance
    security, etc. Priority: 1
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Ability to publish artifacts with known processing steps
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Application and Ant task to optimize a repository

<!-- end list -->

  - **Mirroring tools**

:\* ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Artifact
and metadata repositories can be duplicated in whole or in part using a
set of tools included in p2. Priority: 2

:\* ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
Application and Ant task to invoke the mirroring application. Do we get
much over rsync?

[Technical Spec](Category:Equinox_p2 "wikilink")