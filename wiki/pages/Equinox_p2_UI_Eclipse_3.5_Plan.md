## 3.5 Milestone Plans

### RC1 - May 15, 2009

  - Accessibility issues
  - Bug fixing

### M7 - May 1, 2009 - Development Freeze

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") High value
    polish issues
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Repo
        selection polish
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            Autocomplete in repo combo
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            Provide discovery for disabled sites
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            Better client side validation
          - ![Image:Ok_green.gif](images/Ok_green.gif
            "Image:Ok_green.gif")Misc bugs involving missing schemes,
            leading/trailing slash issues
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Better
        explanation when UI filters out content
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Repo
        error reporting improvements, allow user to correct bad
        locations
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Layout
        work - remembering user sizes and column widths, etc.
  - UI support for late-breaking core features
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        consider additional enhancements in resolve error/explanation
        space
        [Bug 261928](https://bugs.eclipse.org/bugs/show_bug.cgi?id=261928)
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            refactor wizard page flow to report errors on page that
            allows selection modification
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        consider UI implications of install handlers
        [Bug 266061](https://bugs.eclipse.org/bugs/show_bug.cgi?id=266061)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        consider UI implications of patches appearing as updates
        [Bug 245299](https://bugs.eclipse.org/bugs/show_bug.cgi?id=245299)
  - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif")
    Accessibility issues - deferred to RC1
  - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Bug fixing
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Product
    configuration examples
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") RCP
        product with p2 SDK UI
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") RCP
        product with p2 SDK UI, but no auto updates
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") RCP
        product updating from "cloud" (user cannot change/modify the
        repos accessed)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Alternate IU visibility (plug-ins instead of features)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Silent/automatic updating, user never sees the update UI, it
        just happens (but may need confirmation, restart dialog, etc.)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") App
        that reuses unmodified p2.user.ui feature can still contribute a
        policy

## Past 3.4.x Maintenance Streams

### 3.4.1

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Improved
    progress reporting and honoring cancellation requests
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Improved
    presentation of errors (resizability, formatting)
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Fix
    confusing restart language
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Annoying
    /low risk bugs
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Refresh artifact repos when metadata repos are refreshed

### 3.4.2

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Enable a
    site when a user adds a disabled one (workaround until core fixes in
    3.5)
  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") NLS
    formatting fixes (awaiting NL team verification)

## Past 3.5 Milestones

### M1 - Aug 8, 2008

  - UI/Usability
      - Usability review of general strategy (modality, overall
        organization, etc.)
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            Walkthrough with Eclipse UIWG
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            Solicit usability and user persona input from product teams
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            Write concrete user personas to guide use cases
      - Bugs
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            Bring 3.4.1 fixes into 3.5 stream

### M2 - Sep 19, 2008

  - UI/Usability
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Develop use cases based on user persona input
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Investigate integration of installed view with about dialog
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Develop mockups for new workflows and solicit feedback
  - Performance/Stability
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") revisit
        checkbox/filter/deferred fetch strategy
        [Bug 233269](https://bugs.eclipse.org/bugs/show_bug.cgi?id=233269)
        (continuing in M3)
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif")
        investigate resolution in the background
        [Bug 236495](https://bugs.eclipse.org/bugs/show_bug.cgi?id=236495)
        (continuing in the scope of new workflows in M3)

### M3 - Oct 31, 2008

  - UI/Usability
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Complete new workflows (installed view may not yet be integrated
        with about dialog)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Begin
        improved implementation of installed view
        [Bug 224472](https://bugs.eclipse.org/bugs/show_bug.cgi?id=224472)
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            Allow drill-down in installed view of requirements that are
            visible as groups
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            Identify any necessary metadata changes to make this
            simpler/better (see
            [Bug 227675](https://bugs.eclipse.org/bugs/show_bug.cgi?id=227675))
  - Performance/Stability
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif")
        finalize checkbox/filter/deferred fetch strategy
        [Bug 233269](https://bugs.eclipse.org/bugs/show_bug.cgi?id=233269)
          - Better/faster filtering without graying out filter box
            (postponed to M4)
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            Eliminate "duplicate nodes" problem on first repo read
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        resolution in the background to be provided as part of new
        workflows
        [Bug 236495](https://bugs.eclipse.org/bugs/show_bug.cgi?id=236495)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Investigate repo adding/loading performance issues and identify
        necessary changes to core
        [Bug 236485](https://bugs.eclipse.org/bugs/show_bug.cgi?id=236485)

### M4 - Dec 12, 2008

  - UI/Usability
      - Finish up installed view changes
          - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif")
            Complete integration of installed view with about dialog
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            Install drill-down info also shown in install/update wizards
            [Bug 250862](https://bugs.eclipse.org/bugs/show_bug.cgi?id=250862)
          - ![Image:Glass.gif](images/Glass.gif "Image:Glass.gif") Investigate
            the ability to provide optional/related content
            [Bug 247342](https://bugs.eclipse.org/bugs/show_bug.cgi?id=247342).
            (Decision is not to do anything at this time)
      - ![Image:Glass.gif](images/Glass.gif "Image:Glass.gif") Investigate
        better affordances in available view to show already installed,
        available updates, etc.
        [Bug 216032](https://bugs.eclipse.org/bugs/show_bug.cgi?id=216032)
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            Complete initial model/collector work needed to distinguish
            status and improve "already installed" filtering
            [Bug 210583](https://bugs.eclipse.org/bugs/show_bug.cgi?id=210583)
            and
            [Bug 232632](https://bugs.eclipse.org/bugs/show_bug.cgi?id=232632)
  - Performance/Stability
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        finalize checkbox/filter/deferred fetch strategy
        [Bug 233269](https://bugs.eclipse.org/bugs/show_bug.cgi?id=233269)
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            Better/faster filtering without graying out filter box
  - Usability+Performance
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") More
        selective loading of repos when sites are added
        [Bug 236485](https://bugs.eclipse.org/bugs/show_bug.cgi?id=236485)
  - API
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Separation of contributions from the rest of the code
        [Bug 221760](https://bugs.eclipse.org/bugs/show_bug.cgi?id=221760)
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") Ability
        to reassemble groups (available, installed, history, repo
        management) into new locations (pref page vs. wizard, etc.)

### M5 - Jan 30, 2009 - Major Feature Freeze

  - UI/Usability
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        improved error reporting and explanation of problems between the
        planner and the UI
        [Bug 218055](https://bugs.eclipse.org/bugs/show_bug.cgi?id=218055)
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif")
        complete integration of p2 installation pages with about dialog
        (requires refactoring on Platform UI side)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") misc
        polish bugs
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") better
        presentation of repositories
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            Access to sites from all workflows
            [Bug 250316](https://bugs.eclipse.org/bugs/show_bug.cgi?id=250316)
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            improve presentation of enabled/disabled repositories
            [Bug 218534](https://bugs.eclipse.org/bugs/show_bug.cgi?id=218534)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Implementation for available view affordances
        [Bug 216032](https://bugs.eclipse.org/bugs/show_bug.cgi?id=216032)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Revert
        UI improvements
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            history view integrated into about dialog
            [Bug 250316](https://bugs.eclipse.org/bugs/show_bug.cgi?id=250316),
            see
            [Mockups](http://wiki.eclipse.org/Equinox_p2_UI_3.5_workflows#About)
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            background job/progress reporting/UI freeze issues
  - API
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Ability to reassemble groups (available, installed, history,
        repo management) into new locations (pref page vs. wizard, etc.)

### M6 - Mar 12, 2009 - API Freeze

  - UI/Usability
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") Better
        support of disconnected user during install
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            Wizard should open immediately
          - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
            Resolve against only the scoped repos
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") UI
        changes to support site-qualified category ids and versions
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        complete integration of p2 installation pages with about dialog
        (The work to be done is in Platform UI, not p2)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        copy/paste support in various views
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") user
        naming of repos
      - ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif") misc
        bugs

<!-- end list -->

  - API
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") misc.
        API/code cleanup bugs

## Deferred Items

### UI/Usability

  - Ability to install/uninstall user-named groups of IU's
  - Separation of product base vs. "add-ons"
    [Bug 215398](https://bugs.eclipse.org/bugs/show_bug.cgi?id=215398)
  - Fast-path install scenarios
    [Bug 223264](https://bugs.eclipse.org/bugs/show_bug.cgi?id=223264)
  - consider proposed license UI
    [Bug 217944](https://bugs.eclipse.org/bugs/show_bug.cgi?id=217944)

### Major Features

  - User's environment is defined explicitly by someone external
    (administrator)
      - Upgrade or install to a new specification
      - Compare current installation to required one

### API

  - Core/UI responsibilities
      - should UI be the one coordinating provisioning operations vs.
        having scheduling rules
        [Bug 218216](https://bugs.eclipse.org/bugs/show_bug.cgi?id=218216)
      - batched repo events
  - Supported API for all UI building blocks
      - Individual wizards, dialogs, commands
      - consider handlers vs. actions for UI pluggability/are some
        command ID's contracts (so clients can invoke UI by id)
      - Content and label providers

[UI Plan](Category:Equinox_p2 "wikilink")