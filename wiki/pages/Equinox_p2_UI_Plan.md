__TOC__ This page describes the work planned for the next release
(3.6) of the p2 UI. This includes improvements for the p2 UI in the
Eclipse SDK, as well as issues that have to be addressed for alternate
UIs (such as RCP app requirements). Important fixes will be examined for
inclusion/backport to 3.5.x maintenance stream.

The overall p2 plan for 3.6 is located at
[Equinox_p2_Plan](Equinox_p2_Plan "wikilink")

The milestone plans for 3.5 have moved to [Equinox p2 UI Eclipse 3.5
Plan](Equinox_p2_UI_Eclipse_3.5_Plan "wikilink")

The milestone plans for 3.4 have moved to [Equinox p2 UI Eclipse 3.4
Plan](Equinox_p2_UI_Eclipse_3.4_Plan "wikilink")

## 3.6 Ideas list

### UI/Usability

  - Ability to define/share/install/uninstall user-named groups of IU's
    (Susan, Ian?)
      - "My tool set"
      - Extension location use cases that don't necessarily depend on
        physical location
  - Transport-related issues
      - showing detailed repository status when known
      - authentication scoping
      - progress reporting improvements
  - Network discovery of repos
    [bug 218534](https://bugs.eclipse.org/bugs/show_bug.cgi?id=218534) -
    Scott
      - discover LAN (and WAN) based metadata and artifact repos
      - depends on server-side work in bug
        [bug 258340](https://bugs.eclipse.org/bugs/show_bug.cgi?id=258340)
  - Separation of product base vs. "add-ons"
    [Bug 215398](https://bugs.eclipse.org/bugs/show_bug.cgi?id=215398)
      - Differentiating the base product stuff from added content
      - User shouldn't be able to (unintentionally) redefine what is
        "the product" by installing something
  - Fast-path install scenarios
    [Bug 223264](https://bugs.eclipse.org/bugs/show_bug.cgi?id=223264)
  - Search based metaphor
      - Currently the UI is repository centric. It would probably be
        beneficial to have a more search oriented UI.

### API

  - Cleanup and document existing provisional API
      - javadoc
      - wizard page creation should be early and explicit (problems with
        dynamic page creation)
      - ability to insert pages into a wizard
      - actions should not be API (make internal or migrate to commands
        and handlers)
  - Supported API for all UI building blocks
      - Individual wizards, dialogs, commands
      - consider handlers vs. actions for UI pluggability/are some
        command ID's contracts (so clients can invoke UI by id)
      - Content and label providers

## 3.6 Milestone Plans

### M1 - Aug 7, 2009

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") alternate
    license UI
    [Bug 217944](https://bugs.eclipse.org/bugs/show_bug.cgi?id=217944) -
    (Susan)

### M2 - Sep 18, 2009

  - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
    Improvements to the installation history (Susan)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif") User
        management of profile snapshots
        [Bug 284798](https://bugs.eclipse.org/bugs/show_bug.cgi?id=284798)
      - ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
        Comparing configurations in the install history

### M3 - Oct 30, 2009

  - Install wizard improvements (Susan)
      - Selection count indicator and popup
      - non-filtered selections
        [Bug 235654](https://bugs.eclipse.org/bugs/show_bug.cgi?id=235654),
        [Bug 257597](https://bugs.eclipse.org/bugs/show_bug.cgi?id=257597),
        [Bug 278189](https://bugs.eclipse.org/bugs/show_bug.cgi?id=278189)
      - Consider modeless operation
  - Query/load assumptions, better async support (Susan)
      - Get rid of deferred tree content manager and the "load is slow,
        query is fast" assumptions
      - Alternate progress indication during fetch job (redirect to the
        wizard)
      - incremental fill-in

### M4 - Dec 11, 2009

  - Cleanup dead code and formalize wizard page progression in install
    wizards (Susan)
  - First class support of explanations in UI (Susan)
      - error indicators and quick fix/alternate actions
        [Bug 248959](https://bugs.eclipse.org/bugs/show_bug.cgi?id=248959),
        [Bug 267464](https://bugs.eclipse.org/bugs/show_bug.cgi?id=267464),
        [Bug 218048](https://bugs.eclipse.org/bugs/show_bug.cgi?id=218048),
        [Bug 261928](https://bugs.eclipse.org/bugs/show_bug.cgi?id=261928)
      - consolidate "empty explanations" with error indicators
      - better consolidation of repo errors (repo empty explanation vs.
        accumulating repo error indicator)

### M5 - Jan 29, 2010

### M6 - Mar 11, 2010

### M7 - Apr 30, 2010

[UI Plan](Category:Equinox_p2 "wikilink")