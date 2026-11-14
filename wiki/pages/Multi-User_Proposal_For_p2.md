### This document is based on

  - Our current design implementation for multi-user using Eclipse 3.2.

  - \- \[prov\] separate the install folder

  - Wiki topic “Multi-User Install Concerns”

  - Weekly discussion topic for 0813 on shared install.

### Assumptions

`   I have to make a few assumptions since there are many areas that are not either developed or I have not had time to explore.`

  - Touchpoints will support mechanisms that currently are managed using
    install handlers.

### Proposal

  - This proposal describes a wiring of components based on known use
    cases and discussions.
  - It is not expected to be optimal or complete. It has not been
    reviewed as this is a first pass.
  - This proposal will start with the most complex case, multi-user. It
    can then be decomposed into the simpler use cases.

### Definitions

`   Because many technical terms have been overloaded, I have provided definitions for this document:`

  - Configuration is the set of bundles OSGi starts for a user. It would
    also include native artifacts and environment related to these
    bundles.
  - Configuration directory is a private user writable area that
    contains the files necessary to manage a user’s configuration.
  - Workspace is a private writable area that contains a user’s data
    related to using a configuration.
  - Proxy will be used to represent a shifted responsibility for
    management of a user or admin function.
  - There are at least 4 mutually exclusive phase/operations – install,
    configure, unconfigure, uninstall. Because of the current eclipse
    most think of install as also performing a configure. In this
    document that will not be the case. Since the P2 engine does not yet
    have hard definitions for what is an operation and what is a phase I
    am forced to be a little ambiguous.
  - Shared areas are any file folders and shared native artifacts that
    multiple users are expected to have access. Generally users will
    have read and execute access and only the administrator will have
    full access.

### Problems residing in current eclipse (3.x)

  - The shared install mentioned in many places and in bug 185826 refer
    to an admin providing a configuration that is shared. What the admin
    should provide is functionality that the user (or proxy) can chose
    to use. In the current eclipse a shared configuration may mean it
    will be loaded. It also means a version has been preselected and may
    prevent running an unrelated bundle because of conflicts. If it is
    started and not used it bloats the running image. If a user wants to
    use a JVM with reduced functionality but the shared image requires
    more functionality we will have resolution problems. By separating
    out the management of configuration from the installation of the
    shared bundles we can avoid problems.
  - The configurator when using the user-exclude policy was eager to
    change the configuration based on timestamps and version numbers.
    While this might be great for dropping plugins into a configuration
    it is bad for a generic solution to multi-user. The configurator
    should load the configuration and leave the modification of the
    configuration to the Agent.
  - The validator validated the configuration for each feature
    installed. This makes it impossible to get a successful validation
    for some dependency graphs. The validator must validate the
    configuration projected from pending changes.

### Operations on the shared multi-user areas

  - The install operation will put new plugins and IUs in the designated
    place. Since these artifacts are by definition new versions or new
    plugins, user are free to keep working with their existing
    configurations. This operation has to be performed by the
    administrator. The new artifacts can be fetched in-place, there is
    no need to cache. Touchpoint actions can also be performed in the
    shared areas. Flag: There may be special cases for touchpoint
    actions that need special handling.
  - The configure operation must be performed by users. Since every
    thing has been previously installed its sole purposes is to modify
    the users configuration directory and execute touchpoint operations
    related to this user. These touchpoint actions can not modify the
    shared areas during a configure operation.
  - The unconfigure operation should undo what was performed by the
    configure operation. This has to be performed by the user.
  - The uninstall operation can be a cleanup operation. It should undo
    what was performed in the install action. If it is performed after
    all users have unconfigured then the admin is free to remove
    artifacts as a convenient time. We will delay explaining options for
    removal until the end of the document.
  - Definitions of touchpoint actions will not be addressed in this
    section.
  - Install and uninstall operations will be performed by the user agent
    for user areas.

### Profiles, Installable Units (IUs), and bundles.txt

  - bundles.txt resides in the user’s configuration directory and
    represents the list of bundles to start, the start level and if it
    needs to be started.
  - Profiles and IUs define a graph of what should be installed. For
    this discussion I will use Profile to describe a high-level
    requirement and IUs to describe artifacts needed to resolve the
    profile(s). This is not strictly true in P2 as IUs can just be a
    container describing other IUs to be installed.

### Core agent component

  - This core component consist of all bundles and artifacts required to
    launch a provisioning application. It be preconfigured with a
    configuration directory that contains a bundles.txt that represents
    this core. This means that it can be downloaded as a zip file,
    unzipped and executed without any other work or minimal work.
  - Any dependencies on this core provisioning component should be
    described as requirements to an OSGi service interface and import of
    packages. IU version matching requirements should be no tighter than
    the compatible level. This will allow the wholesale replacement of
    the core agent without creating dependency conflicts. This should be
    possible for the provisioning part. What about the rest of the core?
  - It would be necessary to define an IU that implements each the os,
    ws, and arch filters supported for the core component.

### Administrator role when installing for multi-user

  - Administrator downloads core agent component and the required
    profiles.
  - Because it is an administrative role, the agent will only run for
    the install operation. The install operation can also include some
    touchpoint actions (including modification of files in the shared
    area, modification of global registry values). It would not include
    the modification of user configuration directories since there is no
    way to know all the locations or to have access rights to these
    private areas.
  - If desired the admin may run the agent as a user which will create a
    user configuration for the admin.

### User role in multi-user install

  - User will launch the agent application which is in the shared areas.
  - We detect that this user is not an admin.
  - We create the user a new workspace and configuration. It will
    initially be populated with the core component bundles.txt or it
    will be referenced. Launcher support may be needed to do this.
  - Using the install registry for the shared area, the user’s agent can
    determine what is installed and available to be configured for the
    user.
  - Using profiles pointed to or provided, the agent builds the
    remainder of the user’s configuration.
  - Agent exits or restarts the user’s configuration.

### RPM install

  - The core RPM should contain the core components described above and
    any necessary profiles. The installation of the RPM would be free to
    remove the previous core and install the new one. It can then launch
    the agent and it can provision additional components described in
    the profile(s).
  - A core UI RPM could add a profile that would add UI components if
    desired. It would have a requires on “gtk2” or something, thereby
    not allowing it to be installed on a Linux box without gtk2. It
    would also prevent gtk2 from being uninstalled without warnings and
    errors.
  - Since RPM does not manage multiple version easily, the provisioning
    of items outside of the core should be left to the agent and
    profiles. RPM could possibly manage layers of profiles. Each of
    these RPMs should define required system components dependencies.

### Native artifacts

  - If at all possible native components should be packed as plugins and
    remain in the eclipse installed area. This leads to simpler
    provisioning operations. This greatly simplifies running on various
    operating systems.

### Property Files

`   I will describe how we have been able to manage launch parameters. They fall into 3 categories:`

  - Global. There are not many of these but they are critical for
    launching from a relocatable file system (mounted remotely for
    example). They are also necessary for the launching of different
    JVMs. This would be the eclipse.ini file in the current P2
    implementation.
  - User. Most properties reside here. If each user has a different
    configuration and then it is natural that the properties should be
    user based. These also need to support a relocatable file system.
    These are usually managed by a feature’s install handler (now a
    touchpoint action) and set on launch by the launcher.
  - Plugin/IU. The problem here is that each type of JVM requires
    different launch parameters.

At some point we should look at how we manage the various properties and
see if it can be managed better with a different scheme. But, it works
for a fair level of complexity.

### Profiles are controlled by policy decisions

  - Which profiles apply to a user is a policy decision. Decisions could
    be made remotely, by the administrator, by the user, or a
    combination of these.
  - A profile might also contain entirely optional IUs. For instance a
    user might use the provisioning UI to connect to a repository
    profile and then select which IUs to install.

### Single user install

  - Profiles are obtained for a user as is done for multi-user case.
  - Agent detects that this is a single user install and performs an
    install operation followed by a configure operation.

### Uninstall operation

  - Use-counts on artifacts can be used to know when to uninstall. This
    can be accomplished with a global tracking mechanisms of some kind.
    Even then, we can have a user that does not migrate when asked and
    this mechanism will fail by itself.
  - Users can be forced to upgrade on their next start. This can be done
    by monitoring the profile version numbers. If the version has
    changed, a migration to the next profile level is initiated.
  - Force an upgrade by only keeping 1 version of shared artifacts. When
    the user detects that his configuration is invalid (either though
    profile version change or error on startup) the agent reconciles and
    updates the user configuration. Because we are using a unified core
    agent component we can guarantee that a user will be able to run the
    agent.

[Multi User Install](Category:Equinox_p2 "wikilink")