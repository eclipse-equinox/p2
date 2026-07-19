This page lays out milestone plans for the development of [Equinox
p2](Equinox_p2 "wikilink") in the Eclipse
[Ganymede](Ganymede "wikilink") release (aka Eclipse platform 3.4).

### 3.4 M7 - May 2, 2008 (Feature freeze)

  - Performance / scalability
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif")
        metadata repo lookup (Simon)
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") memory
        leaks and consumptions
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") update
        site (DJ)

<!-- end list -->

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Patches
    (Pascal)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Define
        metadata and change the planner
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Generate p2 metadata from feature patches
      - (Deferred) Forcing a plug-in in the system

<!-- end list -->

  - Backward compatibility
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        platform.xml synchronizer (Dave / DJ)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Keeping update manager happy (e.g. old features) (DJ / Dave)
      - Verification of the headless APIs (DJ)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Connecting to old style update sites (Simon/DJ/Tim/Jed)

<!-- end list -->

  - Misc
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Re-enable revert (Simon)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        support for associated sites and discovery sites (John)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        optionality (Pascal)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Signature trust checks (John/Tim)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") http
        authentication (John/Tim)
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") https
        (John/Tim)

<!-- end list -->

  - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Shared
    installs (Simon / Andrew O)
      - Ensure that we behave properly on Vista (John)
      - Introduce the concept of a base
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif")
        Aggregating repositories (Andrew O)
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif")
        Aggregating bundles.info (Andrew O)
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Profile
        generation (Andrew O)

<!-- end list -->

  - Builds
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif")
        Ganymede p2-izer (Andrew O / Pascal)
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") EPP
        packages (Andrew O / Pascal)
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") PDE
        Build integration, production of product metadata (Andrew N)
      - (Deferred) Modifying the tests to provision
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Educate community to use the director in their build

<!-- end list -->

  - Polish
      - (Deferred) More detailed and user friendly explanations from
        solver
      - (Deferred) Recovery application (headless)
      - Localization of features (Dave)

<!-- end list -->

  - UI (Susan)- see [Equinox p2 UI
    Plan\#M7](Equinox_p2_UI_Plan#M7 "wikilink")
      - d\&d support (the unzipping of the things is weird)

## Future Plans

## Previous Plans

## 3.4 M6 - March 28th, 2007

Theme: ***Really*** **Integrating in the SDK**

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Changing
    the SDK build / SDK features (DJ, Andrew N, Kim)
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Improving
    the Install Registry format (Simon)
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") User work
    flows (build, product creation, ...) (Pascal)
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Running
    the SDK tests
    (https://bugs.eclipse.org/bugs/show_bug.cgi?id=208021) (John / DJ)
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Mac build
    (Pascal)
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Reviewing
    / renaming file names
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Reviewing
    / renaming metadata namespaces
  - Forcing a plugin in the system
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") SAT4J
    integration (Pascal)
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Support
    for mirrors (John / Tim W)
  - Ganymede p2-izer (Andrew N)
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Production
    of product IU (Andrew N)
  - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Shared
    install
  - Metadata for patches
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Metadata
    translation (https://bugs.eclipse.org/bugs/show_bug.cgi?id=222309)
    (Dave)
  - Vista
  - Reconciling with the running JRE
  - Base
  - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Support for
    signature verification / presentation
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Fancier
    download technology

<!-- end list -->

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")Testing /
    improving the compatibility
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Improving
    the story for compatibility in presence of install handler (Simon /
    Susan)
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") UI - see
    [Equinox p2 UI Plan\#M6](Equinox_p2_UI_Plan#M6 "wikilink")

## 3.4 M5 - February 8, 2007

Theme: **Integrating in the SDK**

  - Change the SDK build
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Decide on
    the shape of SDK
      - SDK.zip shape
      - Metadata for the SDK (product vs extension)
      - How many bundles does p2 ship in the SDK
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Generator
    for ganymede

<!-- end list -->

  - Metadata for fixes
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Metadata
    for updates
  - Internationalization: translation of metadata, translation fragments

<!-- end list -->

  - Profile preferences: repos, gc, etc.
  - Mirrors, repository seeding

<!-- end list -->

  - Finish up UM compatibility
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Connecting
    to an old style update site through p2

<!-- end list -->

  - Resolver
  - Signature verification

<!-- end list -->

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Shared
    install

<!-- end list -->

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Pack200
    support

<!-- end list -->

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Installer

<!-- end list -->

  - Need a way to lock the agent data area to prevent multiple processes
    collision.

<!-- end list -->

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Control
    the context in which an operation occurs (limit repos)

<!-- end list -->

  - UI
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Restructure available IU viewer for PDE consumption
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Flesh
        out property pages for end user UI
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Summary info for install/update wizards
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Automatic updates affordance and popup cosmetics
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Performance issues
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            Background resolving/sizing for user-triggered updates
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            Rework model to reference ids (profile ids, repo URLs) in
            lieu of objects

## 3.4 M4 - December 14, 2007

Theme: **Ready for integration in the SDK**

Must-do: **The whole team consumes I-builds using p2**

  - Tooling:
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") PDE to
        detect whether it is running on a provisioned system and
        properly set the default in the target platform (Darin)
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Ant
        tasks and wiki page to have people produce repositories (DJ)
        [bug 209086](https://bugs.eclipse.org/bugs/show_bug.cgi?id=209086)
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Allow
        for PDE Build to produce p2 artifacts and repositories (Andrew
        N.)

<!-- end list -->

  - Test
      - Improve the coverage of the tests (all)
      - Multiple form of an artifact in the repository (all)
      - Regularly run the Eclipse tests on a provisioned SDK
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Manual
        Tests to be run every week by someone from the team. (all)

<!-- end list -->

  - UI (Susan)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Improved support for browsing repos
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            Duplicates, sorting, filtering, show latest version only
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") IU
            categories
            [Bug 203115](https://bugs.eclipse.org/bugs/show_bug.cgi?id=203115)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Polish
        and cleanup (see more details at [Equinox p2 User
        Interface\#Milestone
        Plan](Equinox_p2_User_Interface#Milestone_Plan "wikilink"))
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Performance investigations for add repo, resolving and sizing
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Preferences for reminding user of available updates
        [Bug 207493](https://bugs.eclipse.org/bugs/show_bug.cgi?id=207493)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") UI for
        Revert
        [Bug 205223](https://bugs.eclipse.org/bugs/show_bug.cgi?id=205223)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Present license information and remember accepted licenses
        [Bug 205232](https://bugs.eclipse.org/bugs/show_bug.cgi?id=205232)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Misc
        Admin UI features as requested by team

<!-- end list -->

  - Shared Install
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Define
        how shared installs work from a layout point of view (Andrew)

<!-- end list -->

  - Download
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Robustness of transports. Support for cancellation. (Stefan)
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif")
        Pluggable download manager strategy (Tim, Stefan, Scott)
      - ECF support for introspecting transports (throughput, latency)
        (Scott)

<!-- end list -->

  - Update manager compatibility
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Directory watcher (Simon, Jeff)
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Initial
        support to allow for installation from an update site (DJ,
        Pascal)

<!-- end list -->

  - Core things
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Scalability / OOM (John, Dave)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Garbage collection of the artifacts (Allan, DJ, Pascal)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") API
        Cleanup (John)

<!-- end list -->

  - Misc.
      - JRE Reconciliation on provisioning
      - Decide of the delivery format of the SDK for M5 (Pascal, Jeff)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Ensure
        simple configurator has a promiscuous mode.
      - Make the metadata generator more flexible and take advices
        [Bug 209544](https://bugs.eclipse.org/bugs/show_bug.cgi?id=209544)

### 3.4 M3 - November 2, 2007

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Establish
    concrete set of functionality to be available in 3.4 final
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Perform
    rename of bundles and packages to the new name: p2. Updating wiki
    pages and other documents accordingly (dj/John)
  - True self hosting:
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Produce regular I-builds and nightly builds of p2 bundles
        (Kim/DJ)
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") p2 team
        will update across I-builds using p2 (all)
      - PDE target provisioning from bundles.txt
  - Cross-platform:
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Build
        the agent for all platforms (dj)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Metadata generation for all platforms (dj)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Be
        able to install to any platform/os from a single update site
  - Engine:
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Transaction support across phases (Simon)
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Multiple
    forms of an artifact in a repository (Stefan/Jeff)
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Integrate
    metadata generation with current build update site generation (dj)
  - UI (see more details at [Equinox p2 User Interface\#Milestone
    Plan](Equinox_p2_User_Interface#Milestone_Plan "wikilink")): (Susan)
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Improve
        workflow and info provided pre-install/update/uninstall
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Polling for automatic updates and user prefs driving how updates
        are handled
      - (slip to M4) Improved support for browsing repos (categories,
        filtering)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Admin
        UI shows more info in artifact repo view
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Polish
        and cleanup
  - (should have basics in for M3) Shared install (AndrewO, Tim)
  - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Pluggable
    download manager strategy (Tim)
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") ECF
    support for pause/resume (Scott)
  - ECF support for introspecting transports (throughput, latency)
    (Scott)
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Review
    Framework admin API, and usage of Framework admin in p2 (John)
  - (slip to M4) Define 3.3/3.4 compatibility story (Pascal)
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
    Replacement for Xstream (Dave)
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Review
    Director/Engine relationship and API (Pascal+Susan)

### Questions for M3

#### Variables

Parameters required at install time, such as port numbers

  - Where - in the engine? IU actions
  - Why? - for configuration? installation?
  - What for? - IU Actions
  - Scope?
  - When are they actually used?
  - are the read/write or read only? who can write?
  - Mechanism to query the user for data

#### Nested profiles

Multiple "products" installed together but run independently.
Essentially suites of software. Otherwise discussed as Compound
Applications

  - What are the API changes needed if someone was to do this work?
  - Do we really need this in 1.0

#### Prerequisites

The ability to express dependencies on things other than IUs. For
example, hardware, execution environment, ...

  - Seems like this is good to do. some sub questions
  - how/who to initailize the values? When?
  - need new kind of requirement? LDAP filter based?

#### Governor

What does the governor look like?

  - see
    [Bug 205068](https://bugs.eclipse.org/bugs/show_bug.cgi?id=205068)

#### Installer

What does the installer look like?

  - How to get the initial install of the agent? JNLP? self-extracting
    zip, OSGi initial provisioning?
  - Should the agent be an installer
      - branding the user experience
      - installing function/resources into the agent itself
  - silent install? (response file)

#### Installing into the agent (customizing installer)

Adding function into the agent itself

  - adding new transports, repositories, touchpoints based on what the
    user is doing/asking.

#### Resolver ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif")

  - Current resolver cannot backtrack. We cannot do selection reliably.
    Do we need a new resolver algorithm? (Pascal)
  - Do we need a full resolver? Is there a more constrained problem
    (e.g., resolving just features)
  - Update manager equivalent resolver?
  - Can we find a SAT solver that has the right licencse, language, can
    be used, ...
  - need a low bar solution for 3.4. Possibility to extend in the
    future.

#### Uses ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")

  - Do we need to support "uses" clauses in the metadata depenedency
  - need support in the resolver/picker and this has proven to be hard
  - not doing uses for 3.4

#### Advice

The producer/consumer problem needs mechanisms for other people to
supply advice on how the agent should behave. See [Equinox P2
Resolution\#Limiting the space:
advice](Equinox_P2_Resolution#Limiting_the_space:_advice "wikilink").
Advice may not come in the same form as IUs etc.

  - when can advice be supplied?
      - Must be defined at the beginning of resolution. Otherwise we
        would be in a dynamic advice situation (see below).
  - where does advice come from
      - top level IU?
      - side files etc?
  - Version control - should be able to do
  - Adding requirements (Causality) - essentially adding new
    requirements on other IUs
      - how does this relate to fragments
      - related to eager optional requirements
      - should be able to do this one
  - Uses
      - see above, not likely to do
  - Affinities
      - seems related to uses. Similarly then it is unclear how we can
        implement this now.
  - Dynamic Advice
      - no

#### Security

Various levels of security

  - JAR signuture verification - Yes
  - MD5 hash verification - yes
  - signatures on IUs themselves - not now
  - secure transports (HTTPS, ...) - yes
  - Repository trust - rudimentary facilities
      - signed repositories/certificates on server - yes
      - white/black lists - yes
  - login - yes

#### Simple configurator policy

Whether or not the simple configurator should play nicely with others
who call installBundle()

  - not sure what the implications would be
  - seems harsh to simply uninstall things that we don't know about
  - have an option to play nice - yes but you what you asked for.
  - do something simple not involving state resolution and optimization.
    Just whether or not to uninstlal things that SC did not install.

#### Locating files

The ability for to find where a file was put during the install. Example
you might need to know where the java.exe is so it can update a script.
This is somewhat related to variables but has a different
flavour/approach. An extension of this is the ability of one IU to look
for files from another IU.

  - Actions have to participate by letting the system know where it put
    files or by offering to help find files when someone is looking for
    them
  - Ask Andrew how this works in RPM etc
  - Locating files within one IU - yes
  - Locating files across IUs - maybe Try to do something at least basic
    here.

#### Sequenced update

Expressing the need to update along a certain path. For example,
updating from Foo 1.0 to Foo 1.2 may require that you update to Foo 1.1
first and then update to Foo 1.2. For example to get some data
transformations run that are only available in Foo 1.1.

  - Need for a new kind of dependency to express the path. for example
    Foo 1.1 needs to say that it can update Foo 1.0 whereas Foo 1.2
    would say it can update Foo 1.1.
  - should try to do something here if it is reasonably easy

#### Reasoning about versions

How do you reason about version numbers that are in different formats.
For example, the current Version class does not allow to properly reason
about the version of JRE.

  - Two approaches
      - Make versions representation/logic pluggable.
      - canonicalize the version number schemes in terms of the base
        Version scheme
  - Do the second approach (canonicalization) for now

#### Shared Agent data access

If there are multple Agents running all using the same data area we need
to protect the actual data files. This can happen for exapmle when you
have several p2 enabled applications running where each has the agent
code in it.

  - Seems like a real usecase
  - need to do something at least basic here.

#### Advising the metadata generator

Allow people to advise the metadata generator to add various
dependencies, configuration information, ... as the metadata and
artifacts are generated.

  - This is a must have

#### Revise capabilities model

Right now the capabilities model mixes meta-level capabilities (e.g.,
declaring the IU type) with base-level capabilities (e.g., exporting a
package). This is convenient but makes it hard to reason about the IU.

  - Must do something here.

tooling

self hosting

### 3.4 M2 - September 21, 2007

Goals:

  - Updating the running profile (self update with a reasonable UI)
  - Support for update / rollback in the director
  - Support for transaction in the engine

Details:

  - Director / metadata:
      - Implement groups and selectors
      - Refine and implement constraints descriptors

<!-- end list -->

  - Engine:
      - Support for transaction

<!-- end list -->

  - UI:
      - End user UI to install and update
      - Presentation of metadata repo content to the user

<!-- end list -->

  - Misc
      - Move to ECF 1.0.2
      - Support for relative paths
      - Review the usage of framework admin:
          - How are we using it?
          - Why do we use it, what does it bring?
      - Discover the JRE being used to run

<!-- end list -->

  - Shared install:
      - Initial implementation

<!-- end list -->

  - Repository:
      - Support for filtering content presented to the user
      - Make the artifact repository writable and have support for
        post-processing
      - Have an artifact repository implementation to read update sites

### 3.4 M1 - August 2, 2007

Goals:

  - Operations supported: install, uninstall, update, rollback.
  - Self provisioning from a small download of the agent
  - The agent runs in process

Details:

  - Director / metadata:
      - Implement groups and selectors
      - Define constraints descriptors
      - Refine how fragments are being attached

<!-- end list -->

  - Engine:
      - Define new phases and operations.

<!-- end list -->

  - UI:
      - Browse what's installed in a profile
      - Invoke operations
      - Browse a repository

<!-- end list -->

  - First run integration: ability to ship metadata / artifact repo /
    profile with eclipse
  - Investigate shared install problems

## Legend

![Image:Glass.gif](images/Glass.gif "Image:Glass.gif") Needs some investigation

![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Patch in
progress

![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Bug fixed /
Feature added

[Plan](Category:Equinox_p2 "wikilink")