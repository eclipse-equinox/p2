This page lays out milestone plans for the development of [Equinox
p2](Equinox_p2 "wikilink") in the Eclipse [Helios](Helios "wikilink")
release (aka the Eclipse Platform version 3.6).

  - For a high level feature plan, see
    [Equinox/p2/Galileo/Features](Equinox/p2/Galileo/Features "wikilink").
  - For a more detailed view of the UI plan, see [Equinox p2 UI
    Plan](Equinox_p2_UI_Plan "wikilink").

## Current Plan: M6 - March 12th, 2010 - API Freeze

  - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") API, this
    must be the \#1 priority
      - Metadata package review and changes (Pascal / Thomas)
          - Filter on IRequirement
            <https://bugs.eclipse.org/bugs/show_bug.cgi?id=299507>
          - Provided capabilities
          - API for IUPatches
          - Metadata factory
            <https://bugs.eclipse.org/bugs/show_bug.cgi?id=301083>
      - Define the API for the director (Pascal / John)
          - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif")
            ProvisioningContext / handling of repo (Susan)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Query
        vs p2QL overlap (Ian / Thomas)
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif")
        Translation Support (John / Thomas)
        <https://bugs.eclipse.org/bugs/show_bug.cgi?id=298333>

<!-- end list -->

  - ![Image:Error.gif](images/Error.gif "Image:Error.gif") License
    identification work (DJ / John) --\> This has been deferred.
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Foundation to define license URI and update the core to support
        for license URI
      - ![Image:Error.gif](images/Error.gif "Image:Error.gif") UI work to
        verify that everything is happening properly
      - ![Image:Error.gif](images/Error.gif "Image:Error.gif") Publisher / PDE

<!-- end list -->

  - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Discovery
    UI
      - Mylyn metadata work (Steffen / Pascal)
      - Mylyn UI work (Steffen)

<!-- end list -->

  - Planner
      - Work on the encoding of conditional installation (Daniel /
        Pascal)

<!-- end list -->

  - Misc
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Remove
        references to the services - 299987 (John / Thomas)
      - Serialization to support for new expression (Thomas)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Improve download strategies for mirror (Thomas)

## Future Plans

### M7 - April 30, 2010 - Feature Freeze

  - Polish items
  - Performance work
  - UI Accessibility
  - Testing and test framework improvements
  - Release train build/repository assistance

### Helios items left

  - Eclipse touchpoint
      - Improve detection for conflicting arguments

<!-- end list -->

  - Native touchpoint
      - Improve detection for conflicting arguments

<!-- end list -->

  - Engine
      - Reacting to change of property
      - Support for unicodes in actions
      - Consistent handling of @artifacts
      - Pluggable phases

<!-- end list -->

  - Publisher / generator
      - Get rid of the generator bundle to the benefit of the publisher

<!-- end list -->

  - Director application
      - Make it easier to do a set of operations (do we need a "response
        file"), see relationship with installer
      - Provide the ability to set properties at install time

<!-- end list -->

  - UI
      - Ability to define/install/uninstall user-named groups of IU's
      - Fast-path install scenarios (gestures for auto-install)
      - Showing non-greedy optional dependencies for selection

<!-- end list -->

  - Planner
      - Explore addition of negation and or
      - Explore a way to remove the need of optional installation for
        patches
      - Explore the addition of new types of requirements and
        capabilities
      - Explore a way to provide more stability when uninstalling or
        installing
      - Automatic discovery of best update
      - Improve speed of explanation

<!-- end list -->

  - Repository
      - Make the query mechanism more lazy
      - Persistence scalability
      - Validation facility
      - Improve repository tools (Repo2Runnable, Mirroring, etc.)

<!-- end list -->

  - General
      - Define API
      - Improve general flexibility of p2
          - Make the resolver standalone
          - Have several instances of p2 run at the same time in the
            same VM
      - Make p2 run on other frameworks

<!-- end list -->

  - Improve the installer
  - Explore another to categorize content

## Previous Plans

### M5 - January 29th, 2010 - Major Feature Freeze

  - Define API (All)
  - Have several instances of p2 run at the same time in the same VM
    (John / Pascal / Henrik)
  - Planner improvements

### M4 - December 11, 2009

  - Define API (All)
  - Have several instances of p2 run at the same time in the same VM
    (John)
  - Planner improvements
  - Support for install filters on features

### M3 - October 30, 2009

  - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Define API
    (All)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") First
        step is to gather API feedback (Pascal)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Identify what we want to make API (All)
          - We will first focus on the API for Actions and Touchpoints,
            investigate the creation of a higher level API like the
            actions provided in UI, and review the rest.
  - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Make p2 run
    on other frameworks (DJ)
  - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Have
    several instances of p2 run at the same time in the same VM - see
    [Equinox/p2/Multiple Agents](Equinox/p2/Multiple_Agents "wikilink")
    (John / Pascal)
  - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Get rid of
    the generator bundle to the benefit of the publisher (Andrew / Ian)
  - Create a repository validation facility (Ian)
  - Engine work (Simon)
  - Improve repository tools (Repo2Runnable, Mirroring, etc.) (Andrew)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Remove
        IU
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Slicing option to always pick the highest version
      - Slicing option on the repo2Runnable
  - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Polishing
    server side provisioning

### M2 - September 18th, 2009

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Planning
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Galileo
    SR1 (3.5.1) fixes

### M1- August 7, 2009

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
    Decompression
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Bug
    triage, community assistance
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Critical
    bug fixes
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Galileo
    SR1 (3.5.1) fixes
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Show
    licenses by license instead of by IU

## Past Releases

  - [p2 Galileo Milestone Plan](Equinox/p2/Galileo/Plan "wikilink")
  - [p2 Ganymede Milestone Plan](Equinox/p2/Ganymede/Plan "wikilink")

## Legend

![Image:Glass.gif](images/Glass.gif "Image:Glass.gif") Needs some investigation

![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Work in
progress

![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Bug fixed /
Feature added

[Plan](Category:Equinox_p2 "wikilink")