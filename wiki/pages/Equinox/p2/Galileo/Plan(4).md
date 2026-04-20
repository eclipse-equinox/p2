This page lays out milestone plans for the development of [Equinox
p2](Equinox_p2 "wikilink") in the Eclipse [Galileo](Galileo "wikilink")
release (aka the Eclipse Platform version 3.5).

  - For a high level feature plan, see
    [Equinox/p2/Galileo/Features](Equinox/p2/Galileo/Features "wikilink").
  - For a more detailed view of the UI plan, see [Equinox p2 UI
    Plan](Equinox_p2_UI_Plan "wikilink").

## Current Plan: RC4 - June 5, 2009 - Documentation pass

The following documents need creating and/or updating during the Galileo
documentation pass:

  - Workbench User Guide
      - Document new p2 UI
      - Fix references to old p2 UI
      - Look for obsolete references to Help \> Software Updates
      - Review user guide for references to About dialog, due to About
        changes in 3.5
      - Review p2 items in What's new for accuracy of content and
        screenshots
  - Platform Plug-in Developers Guide
      - Document new p2 director application and Ant task
      - Document Repo2Runnable application and Ant task
      - Document Composite repository Ant tasks
      - Document new publisher applications/tasks
      - Document p2.inf format and usage
      - Consider adding interesting ISV information in What's new (new
        apps/tasks, etc)
  - PDE User guide
      - Document PDE/p2 integration

## Future Plans

### 3.5 items left

  - Transport
      - Better explanation of error [1](http://bugs.eclipse.org/248604)
      - Improved cancellability [2](http://bugs.eclipse.org/263613)

<!-- end list -->

  - Artifact repository
      - Robustness in reading and writing.
      - Mirroring application... ppl still have problems
      - Concurrency issues when reading and writing (one process)

<!-- end list -->

  - Eclipse touchpoint
      - Improve detection for conflicting arguments

<!-- end list -->

  - Native touchpoint
      - Improve detection for conflicting arguments

<!-- end list -->

  - Engine
      - Reacting to change of property
      - GC of profile registry

<!-- end list -->

  - Update manager
      - Provide replacement constructs for UM
      - Review problems connecting to legacy update sites

<!-- end list -->

  - Publisher / generator
      - Get rid of the generator bundle to the benefit of the publisher

<!-- end list -->

  - Director application
      - Make it easier to do a set of operations (do we need a "response
        file")
      - Provide the ability to set properties at install time

<!-- end list -->

  - Mirror app
      - Need to make the artifact repository more robust

<!-- end list -->

  - UI
      - Ability to define/install/uninstall user-named groups of IU's
      - Fast-path install scenarios (gestures for auto-install)
      - Show licenses by license instead of by IU
      - Showing non-greedy optional dependencies for selection

<!-- end list -->

  - Misc
      - Review the support for UNC paths
        [3](http://bugs.eclipse.org/207103)

## Previous Plans

### M1 - August 8th, 2008

  - Decompression
  - Bug triage, community assistance
  - Fixing critical defects and performance problems
  - Test framework for dropin reconciler \[DJ\]
  - Creation of Publisher - replacement for metadata generator \[Jeff\]
  - UI walkthrough and usability input \[Susan\]

### M2 - September 19th, 2008

  - Planning
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")Focus on
    3.4.1 defects
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")Port 3.4.1
    fixes to 3.5 stream
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")Integration
    of publisher \[Jeff\]
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")Mock up new
    UI workflows for community feedback \[Susan\]
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")Add a bunch
    of automated tests.

### M3 - October 31, 2008

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Convert
    from using URL to URI where possible \[John\]
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Refactor
    repository managers to remove code duplication \[John\]
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Revise
    Touchpoint action contribution model \[Simon\]
  - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Integration
    of publisher with PDE build \[Andrew N\]
      - Rewrite generator app and Ant tasks to invoke publisher.
      - To be continued in M4
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
    Investigate use of Apache HTTP client (**Note: cross-project impact
    on ECF**) \[Scott\]
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") MD5
    processing step to verify download integrity \[Pascal\]
  - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Identify
    all Eclipse SDK dependencies on Update Manager and determine work
    required to remove Update Manager dependency \[DJ\] (**Note: impact
    on other SDK components**)
      - To be continued in M4
  - Map out metadata authoring and development work flow \[Andrew N\]
    (**Note: impact on PDE tooling work**)
      - Deferred to M4
  - Investigate install handler replacement solution \[helpwanted\]
      - Deferred to M4
  - ![Image:Glass.gif](images/Glass.gif "Image:Glass.gif") Investigate metadata
    construct to separate line-up information from grouping information
    \[Pascal\]
      - Dropped until further notice
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Reorganize
    UI workflows to separate update from install \[Susan\] (**Note:
    potential impact on Platform UI**)
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
    Investigate drill-down of install info and impact on metadata
    \[Susan\]
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Release
    reconciler test framework \[DJ\]
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Release
    publisher tests \[John\]

### M4 - December 12th, 2008

  - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Identify
    all Eclipse SDK dependencies on Update Manager and determine work
    required to remove Update Manager dependency \[DJ\] (**Note: impact
    on other SDK components**)
  - Map out metadata authoring and development work flow \[Andrew N\]
    (**Note: impact on PDE tooling work**)
  - Investigate install handler replacement solution \[helpwanted\]
      - Moved to M5. Help needed.
  - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Complete
    integration of publisher with PDE build \[Andrew N\]
      - To be continued in M5. Help needed.
  - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Repository
    association feature work \[John\]
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Support
    for composite repository \[Andrew C.\]
  - ![Image:Error.gif](images/Error.gif "Image:Error.gif") Closer integration
    with VM \[Simon/Pascal\]
      - Removed from Galileo unless someone picks it up
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Polish the
    action contribution model \[Simon\]
  - ![Image:Error.gif](images/Error.gif "Image:Error.gif") Metadata constructs
    allowing the expression of negation and choice \[Pascal\]
      - Removed from Galileo, unless someone picks it up.
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
    Integration of install view into proposed about dialog framework
    \[Susan\]
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Complete
    detailed drill-down of IU's in install view and update/install
    wizards \[Susan\]
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Separation
    of UI contributions from the supporting class library \[Susan\]
  - ![Image:Error.gif](images/Error.gif "Image:Error.gif") Investigate
    fast-path install scenarios \[Susan\]
      - Moved to M5
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Add
    regression/performance tests

### M5 - January 30th, 2009 - Major Feature Freeze

  - ![Image:Error.gif](images/Error.gif "Image:Error.gif") Complete
    implementation of install handler replacement \[Simon / Henrik\]
      - Some initial design in place. Work to be completed early in M6
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Improve
    available view filtering & affordances
    [Bug 216032](https://bugs.eclipse.org/bugs/show_bug.cgi?id=216032)
    \[Susan\]
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Better
    presentation of repositories
    [Bug 250316](https://bugs.eclipse.org/bugs/show_bug.cgi?id=250316)
    \[Susan\]
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Improve
    the revert experience and workflow
    [Bug 216031](https://bugs.eclipse.org/bugs/show_bug.cgi?id=216031)
    \[Simon / Susan\]
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Better
    analysis of proposed provisioning plan/inform user when request is
    altered \[Susan/Pascal\]
  - ![Image:Error.gif](images/Error.gif "Image:Error.gif") Integrate p2 install
    pages with platform about dialog
    [Bug 246875](https://bugs.eclipse.org/bugs/show_bug.cgi?id=246875)
    \[Susan\]
      - **Note: requires refactoring of Platform UI About
        contributions**. Deferred to M6.
  - ![Image:Error.gif](images/Error.gif "Image:Error.gif") Investigate
    streamlined license UI
    [Bug 217944](https://bugs.eclipse.org/bugs/show_bug.cgi?id=217944)
    \[Susan\]
      - Not addressed in 3.5
  - ![Image:Error.gif](images/Error.gif "Image:Error.gif") Complete integration
    of publisher with PDE build \[Andrew N / Jeff\]
      - Most of the pieces are in place. To be completed in M6
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Multiple
    processes modifying the same profile registry / profile \[Pradeep\]
  - ![Image:Error.gif](images/Error.gif "Image:Error.gif") Need a way to ensure
    that a bundle pool is only used by one agent to avoid problems with
    GC \[Pradeep\]
      - Deferred to M6
  - ![Image:Error.gif](images/Error.gif "Image:Error.gif") Support for
    explanation \[Jed / Daniel / Pascal\]
      - Code to integrate with SAT4J released in a branch. Integration
        completion scheduled for M6.
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Reacting
    to change of CU
  - ![Image:Error.gif](images/Error.gif "Image:Error.gif") Improve detection of
    conflicting arguments \[Matthew\]
      - Code ready, deferred to M6 for lack of time from Pascal to
        review.
  - ![Image:Error.gif](images/Error.gif "Image:Error.gif") Integration of
    composite repositories into build \[DJ\]
      - Most of the pieces are in place. Need to integrate to the build.
        Rest of the work to happen in M6
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Metadata
    work-flow from production to consumption \[DJ\]
  - Add regression/performance tests

### M6 - March 13th, 2009

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Complete
    implementation of install handler replacement \[Simon / Henrik\]
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Support to
    expose a profile as a p2 repository \[Simon\]
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Refactor
    platform UI about contributions and integrate with p2 install pages
    [Bug 246875](https://bugs.eclipse.org/bugs/show_bug.cgi?id=246875)
    \[Susan\]
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Better
    support for disconnected user during install \[Susan\]
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Complete
    integration of publisher with PDE build \[Andrew N / Jeff\]
  - Need a way to ensure that a bundle pool is only used by one agent to
    avoid problems with GC \[Pradeep\]
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Support
    for explanation \[Jed / Daniel / Pascal\]
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Improve
    detection of conflicting arguments \[Matthew\]
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
    Integration of composite repositories into build \[Kim / DJ\]
  - Support for Framework extensions \[Community / Pascal\]
  - Make sure the publisher reuses metadata available in existing repo
    \[Andrew / Pascal\]
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Add
    regression/performance tests

### M7 - May 1st, 2009 - Development Complete

  - Polish items
  - Performance work
  - UI Accessibility
  - Testing and test framework improvements
  - Release train build/repository assistance

## Past Releases

[p2 Ganymede Milestone Plan](Equinox/p2/Ganymede/Plan "wikilink")

## Legend

![Image:Glass.gif](images/Glass.gif "Image:Glass.gif") Needs some investigation

![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Work in
progress

![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Bug fixed /
Feature added

[Plan](Category:Equinox_p2 "wikilink")