__TOC__ p2 UI Walkthrough Eclipse UI Best Practices Working Group
6/25/2008

## Prep/Homework

The discussion will be the most productive if the participants are
already familiar with both the Eclipse Update Manager UI and the p2 UI.

  - [Equinox p2 UM workflows](Equinox_p2_UM_workflows "wikilink") shows
    the scenarios in the Eclipse Update Manager that were used to set
    goals for improving the user experience.
  - [Equinox p2 UI 3.4
    workflows](Equinox_p2_UI_3.4_workflows "wikilink") shows the same
    scenarios in the new p2 UI and compares aspects of the design to the
    UM design.
  - Detailed references
      - [Equinox p2 UI Plan](Equinox_p2_UI_Plan "wikilink")
      - [Update Manager and p2
        Compatibility](Update_Manager_and_p2_Compatibility "wikilink")

## Discussion Topics

  - Are we ready to tweak/fix problems in the current UI or do we need
    to step back and reexamine the overall organization and metaphor?
      - Is "browsing" vs. task-oriented "search" wizard a better
        metaphor for finding content (esp. given performance issues,
        user wait time)?
      - Does eliminating modality improve the metaphor?
      - Is the juxtaposition of "installed" and "available" working?

<!-- end list -->

  - Known problem areas and possible solutions
      - How to integrate repo management into workflows
          - Presentation of large numbers of repositories for browsing
          - Selection of repositories for updating (solution might be to
            do smart picking for the user)
      - Unfolding of detail for installation contents
          - Balancing the simple view with more detail for advanced
            users
          - Is a tree view the best presentation for selectively
            exposing detail about the installed software?

<!-- end list -->

  - Specific UI improvements that could be made (style-guide type
    issues: wording, button organization, etc.)
  - Other update UIs that are done well

## Discussion Notes from Walkthrough

I didn't capture the names of who said what, and I'm sure I missed some
things. These are the points that I took away from the walkthrough, and
I've annotated existing bug numbers for issues brought up where
applicable.

  - The black and white icons for available software imply that you need
    to do something to enable them.
    [Bug 210583](https://bugs.eclipse.org/bugs/show_bug.cgi?id=210583)
      - If affordance is going to show installed vs. not installed,
        don't rely solely on color
        [Bug 216032](https://bugs.eclipse.org/bugs/show_bug.cgi?id=216032)
      - We'll probably have affordances for more states so the icon
        should probably be the color one
  - Update manager hid a lot of the site management when searching for
    updates. It knew where to look. Putting repo management on user's
    shoulder through checkboxes is a step backward.
    [Bug 234213](https://bugs.eclipse.org/bugs/show_bug.cgi?id=234213)
      - Install view should show the user (in properties, tooltip, etc.)
        what site something originally came from so they'd know where to
        look for updates
      - If we retain that relationship (today we don't), we could be
        smarter searching for updates so that user doesn't need to know
        at all
  - Things that can take quite some time are modal and prevent user from
    working
      - Software Updates... dialog should not be modal
        [Bug 221755](https://bugs.eclipse.org/bugs/show_bug.cgi?id=221755)
      - Progress dialog after clicking Install... can take a long time,
        it should not be blocking.
        [Bug 236495](https://bugs.eclipse.org/bugs/show_bug.cgi?id=236495)
  - User is used to 4 different kinds of dialogs in Eclipse (status,
    selection, preferences, wizard) with well-known button locations.
    The update dialog doesn't fall into any category, so user doesn't
    know what to do.
      - User expected that checking boxes and selecting "Close..." would
        have retained selections so they could open dialog again and
        perform the install
        [Bug 235288](https://bugs.eclipse.org/bugs/show_bug.cgi?id=235288)
      - Would moving the buttons around help with this?
      - Update Manager had wizard presentation, it was straightforward
      - Update Manager also had manage configuration dialog (different
        looking)
      - Would moving "Install..." to the bottom of available features
        page make it more familiar, or having an "Apply" Button
      - Where would "Update..." and "Uninstall..." go
      - Consider integrating a task list so user can perform actions
        (install, uninstall, update) that get added to a task list and
        then one push of "Apply" button will do what they wanted
  - General discussion of organization of "Installed and Available".
    Does user know what to do?
      - There are inconsistencies in these views (standard selection vs.
        check selection, etc.)
      - Should one view lead to another? User opens installed view and
        can push button to open detail about what's available?
      - Does user really care to see these together? Aren't the kinds of
        things you do with your installation different than when you are
        looking for stuff?
      - Why isn't the information in Help\>About integrated into the
        install view?
        [Bug 224472](https://bugs.eclipse.org/bugs/show_bug.cgi?id=224472)
      - 90% case is user just wants to check for updates of what they
        have. Need fast way to do that which doesn't require seeing
        installed or available (ie...same behavior they get when
        automatic updating is on and they click on update affordance )
      - Consider an advanced, full-fledged view that shows everything
        integrated (installed, what's available, etc.) and lets user
        filter many different ways with view buttons across the top
          - Show only what's installed, or only updates, or everything
          - Lots of ways to filter, for example just see service
            level/maintenance updates vs. major releases
  - The differences in margins, spacing, button sizes make the UI look
    unpolished

## Conclusions/To-do's

  - A one-hour walkthrough is often only enough time to surface the
    important questions
  - Consider coming back to discuss specific issues now that
    participants have context
  - Work on mock-ups/proposals that address the issue of overall
    organization (what tasks is user doing and what do they need to see
    to do them?) Post to wiki when available and contact UIWG to revisit
    issue. Ideas include:
      - Separate installed and available view and layer them. For
        example, installed view that expands into view of what's
        available
      - Fast path for simple case (check for updates...)
      - Merge the installed/available view into one view that allows
        filtering

The examination of these workflow issues is being tracked by
[Bug 236740](https://bugs.eclipse.org/bugs/show_bug.cgi?id=236740), but
design discussions and ideas will be captured in [Equinox p2 UI Use
Cases](Equinox_p2_UI_Use_Cases "wikilink").

## Other thoughts

Notes from others attending the meeting that weren't captured elsewhere.

\[David Williams\] It was mentioned there are "different types" of
installs ... such as installed explicitly at user's request versus
installed implicitly as part of some other installation. Would be nice
if that difference could be captured in the UI, such as some users may
want to hide everything that is installed, not matter how installed, but
at times, may only want to hide things they explicitly installed. \[Ok,
I confess, that didn't come up in the meeting, but there just wasn't
time with so many opinionated people on the call ... and, I mean that in
the best possible way :) \]

See [Bug 224472](https://bugs.eclipse.org/bugs/show_bug.cgi?id=224472)

It was mentioned (for real this time) that there are (or could be)
several types of updates ... update qualifiers only, service fields
only, minor fields, major fields\! This will add a new dimension
(complication) to many of the UIs and user choices.

\[Ingo Muschenetz\] Apologies--was unable to attend the meeting. Is
there some parallel system to policy files in p2? Somewhat related to
the above comment, there is a definite use case for the relatively
non-technical developer who wishes just to get more bleeding edge
releases of currently installed software, and to treat it as an update
check, not a new installation from a different location. Previously we
(Aptana) have done so using policy files to redirect users to different
update sites, but this workflow is non-obvious now, if even possible.
Textmate has a very simple dialog for this. "Automatically check for
updates", then choose Cutting Edge/Minor/Major. That's about all the
complexity an average developer would want.

[UIWG Walthrough](Category:Equinox_p2 "wikilink")