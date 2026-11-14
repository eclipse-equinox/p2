__TOC__ This page captures user scenarios in the p2 User Interface
and discusses problems and issues in the workflow.

## Original Goals

Most of the goals were derived from problems observed/reported in the
previous UI (Update Manager). See [Equinox p2 UM
workflows](Equinox_p2_UM_workflows "wikilink") for the equivalent
scenarios.

  - Reduce click/decision counts
  - Move advanced concepts out of main workflows
      - required software details (automatic resolution)
      - repo management (alternate views of content)
      - mirror selection (automatic)
      - install locations (not supported)
  - Automatic updating should not interrupt workflow
  - Make it easier for users to get content they find on the web
  - Integrate "what do I have" with "what can I get"
  - Integrate "what do I have" with "what updates are available"
  - User-oriented error messages, higher level explanations
  - Code structured for multiple levels of integration (pluggable
    policies, different host dialogs, building blocks)

## Scenarios

### Scenario 1: Check for updates

  - Help \>
  - Software Updates...

![Image:P2InstalledList.jpg](images/P2InstalledList.jpg
"Image:P2InstalledList.jpg")

  - select Installed Software page
  - Update...

![Image:P2UpdateDialog.jpg](images/P2UpdateDialog.jpg
"Image:P2UpdateDialog.jpg")

  - Next

![Image:P2LicenseDialog.jpg](images/P2LicenseDialog.jpg
"Image:P2LicenseDialog.jpg")

  - I accept...
  - Finish

#### Click/decision count

  - 7 clicks/decisions (vs. 14 for UM)
  - Can reduce to 5 once a license has been accepted/remembered (vs. 12
    with UM automatic mirror selection)

### Scenario 2: What add-ons can I get?

  - Help \>
  - Software Updates...

![Image:P2AvailableList.jpg](images/P2AvailableList.jpg
"Image:P2AvailableList.jpg")

  - select Available Software page
  - expand each site, then categories, and checkmark desired items

<!-- end list -->

  -   - ![Image:P2BySite.jpg](images/P2BySite.jpg "Image:P2BySite.jpg")

<!-- end list -->

  - OR view by categories, expand categories, and checkmark desired
    items

<!-- end list -->

  -   - ![Image:P2ByCategory.jpg](images/P2ByCategory.jpg
        "Image:P2ByCategory.jpg")

<!-- end list -->

  - OR view by name, checkmark desired items

<!-- end list -->

  -   - ![Image:P2ByName.jpg](images/P2ByName.jpg "Image:P2ByName.jpg")

<!-- end list -->

  - OR filter by name from any view

<!-- end list -->

  -   - ![Image:P2Filtered.jpg](images/P2Filtered.jpg "Image:P2Filtered.jpg")

<!-- end list -->

  - Install...
  - Progress dialog followed by Install dialog
  - Scenario continues similar to update completion

#### Click/decision count

  - 3 clicks/decisions to get to browsing mode (vs. 7 for UM)
  - alternate ways to find content (UM by site only)
  - ability to find by name
  - once items are selected for install, 3 clicks to finish (Next, I
    accept..., Finish) as in update scenario. One click if license was
    previously accepted. (UM is 4 clicks)

### Scenario 3: Found something cool on the web

Setup: User finds software with an update site reference on the web

` `![`Image:P2WebContent.jpg`](images/P2WebContent.jpg
"Image:P2WebContent.jpg")

  - Help \>
  - Software Updates...
  - select Available Software page
  - drag URL from browser to list (this adds the site and expands it)

![Image:P2DragResult.jpg](images/P2DragResult.jpg "Image:P2DragResult.jpg")

#### Click/decision count

  - 4 clicks/decisions to get new content shown in UI (vs. 14 for UM)
  - once items are selected for install, 3 clicks to finish (Next, I
    accept..., Finish) as in update scenario. One click if license was
    previously accepted. (UM is 4 clicks)

### Scenario 4: What do I have?

  - Help \>
  - Software Updates...
  - select Installed Software page

![Image:P2InstalledList.jpg](images/P2InstalledList.jpg
"Image:P2InstalledList.jpg")

#### Click/decision count

  - 3 clicks/decisions to see high level of what user installed (vs. 4
    for UM)
  - No way to get more detail (UM provides drill-down detail)
  - No link from installed view to more detailed Help \> About
    information

#### Detailed feature view of what's installed

  - Help \>
  - About Eclipse SDK
  - Feature Details

![Image:P2AboutFeatureDetail.jpg](images/P2AboutFeatureDetail.jpg
"Image:P2AboutFeatureDetail.jpg")

#### Detailed plug-in view of what's installed

  - Help \>
  - About Eclipse SDK
  - Plug-in Details

### Scenario 5: Automatic Updating

Like Update Manager, p2 defines preferences for the user to set up
automatic update scheduling on every startup of Eclipse, or at daily or
weekly scheduled times. Users can also set a preference so that updates
are automatically downloaded before the user is ever notified that
updates have been found. In addition, users can set reminder options so
that they can ignore update notifications and be reminded later.

Setup: User is working and updates have been discovered and/or
downloaded

  - Popup notifies user of available updates. User can keep working.

![Image:P2UpdateNotify.jpg](images/P2UpdateNotify.jpg
"Image:P2UpdateNotify.jpg")

  - User can click on the popup to review the updates when ready. This
    will result in the update dialog shown in Scenario \#1
  - User can ignore popup until ready to deal with it
  - User can dismiss popup and retrieve the update list again by
    clicking on the update affordance in the status bar

#### Click/decision count

  - Work is not interrupted, user can ignore updates until ready or
    dismiss popup until ready
      - After clicking on popup, 3 clicks (or 1 if license has
        previously been accepted) to install the updates (vs. 7 for UM)
      - If popup is dismissed, 4 clicks (or 2 if license has previously
        been accepted) to reactivate and complete update dialog (vs. 14
        clicks/decisions to manually recheck for updates with UM)
      - Reminder options allow the 3-click (or 1 if license has
        previously been accepted) option at specified intervals

### Scenario 6: What's wrong with my configuration?

p2 currently does not provide any diagnostic tools to help users when
plug-ins they expect to be installed are not there, or if there are
compatibility conflicts with plug-ins. Users must use the **Help \>
About** mechanism to see what features or plug-ins are installed.

To resolve a problem, users can choose to revert to a previously known
configuration.

  - Help \>
  - Software Updates...

![Image:P2InstalledList.jpg](images/P2InstalledList.jpg
"Image:P2InstalledList.jpg")

  - select Installed Software page
  - Revert Configuration...

![Image:P2RevertDialog.jpg](images/P2RevertDialog.jpg
"Image:P2RevertDialog.jpg")

  - browse dated snapshots and choose desired configuration
  - Finish
  - User prompted to restart

#### Click/decision count

  - 4 clicks to browse revert history (same as UM)
  - User is shown the content of each configuration (unlike UM which
    does not give this info)
  - No description of the delta is provided, user must figure it out by
    looking at content differences
  - UM provides contextual information about why configuration was saved
    (though it is difficult to understand)

### Scenario 7: What sites am I using?

The UM scenarios lead the user through site selection every time the
user searches for content. The metaphor is that the user is always
performing a "search" and the wizard leads them through the site
selection each time (including the ability to add, remove, import, or
export sites) each time the search task is performed.

In p2, the metaphor is that the user "browses" all of the software
available. The system automatically searches all sites when looking for
updates or finding software required by other software. The default view
when browsing available software is grouping the software by site (this
is a familiar presentation for UM users). However, users can also view
by category or by name. Sites can be added using drag and drop, but to
remove sites, disable sites, import/export sites, users must use the
Available Software Site dialog.

  - Help \>
  - Software Updates...
  - select Available Software page
  - Manage Sites...

![Image:P2ManageSites.jpg](images/P2ManageSites.jpg "Image:P2ManageSites.jpg")

#### Click/decision count

  - 4 clicks to get to a page where sites can be configured
  - Task is defined as "Manage Sites..." (In UM, user must know to use
    the search wizard to get to this task).

#### Houston, we have a problem

Egad\! Where did all of those sites come from?

There is an inherent difference between Update Manager's model of
repositories and p2's model. The intention in p2 is to make life easier
for the software producer by separating the definitions of sites that
should be checked for updates from the content itself. This lets
software producers include other components without having to accept
that component's definition of where updates come from.

However, this can put some burden back on the end user, especially those
using older update sites. In UM, features can define discovery sites and
update sites. Sites can define associated sites. UM traverses this site
structure under the covers when looking for updates and thus the user
never has to think about where their updates are coming from. UM can be
very specific in its determination of which sites are needed for an
operation.

Since p2 does not define relationships between content and sites, there
is one pool of sites used when browsing software or searching for
updates. This works nicely when the pool of sites is small and access is
fast. However, in order to achieve backward compatibility with UM, p2
must honor all previously referenced sites when it encounters old update
sites and site references inside features. This may cause performance
problems because the site pool can get very large, and often the
referred sites are old sites which must be parsed and converted to p2
sites when first encountered. For example, the Ganymede site refers to
over 60 other sites.

This discrepancy in the underlying model caused a serious performance
hit when working with older sites. Since p2 was near the end of the
release cycle, we had to solve the problem without seriously changing
the existing workflows. We had to remember the sites encountered when
parsing old update sites, but did not want to add all of them to the
pool of referenced sites. We had to define the notion of **enabled** and
**disabled** sites in p2. The checkmarked sites are those that are
enabled, and therefore their content is shown in the Available Features
page and used when checking for updates.

This means that the user must know to visit the Available Software Sites
page and enable certain sites when looking for certain updates, whereas
UM would have found them automatically. And they are presented with a
large list of sites they have never seen before.

## Improvements

  - Update notification happens without interrupting user work
  - Ability to remind of updates
  - Ability to reduce click count by remembering accepted licenses
  - Click count cut in half or better for most common tasks
  - Simplified presentation and integrated access to "what's installed"
  - Simplified presentation of revert history

## New Problems/Regressions

  - User must visit '''Help \> About ''' to get detailed information
    about the configuration
  - No ability to disable/enable plug-ins (user must uninstall/install)
  - No control over install locations/extension locations
  - User must be aware of enabled/disabled repositories and manage them
    vs. having the system "know what to do" when checking for updates.

## Implementation Problems impacting the Design

  - First-time access of repositories has same (and in some cases worse)
    performance problems as UM did. Original assumptions were that repo
    information was more quickly/easily accessed and cached. User may
    have to wait a long time for content to fill in on available
    software page.
  - Original modeless ("dashboard") design forced to become modal due to
    implementation problems with progress reporting, keyboard handling,
    inconsistency with rest of Eclipse
  - User may have to get involved in advanced repo management to find
    updates (enabling and disabling sites, must figure out where updates
    are located)
  - We haven't improved error reporting (some find it worse) - need to
    decipher resolution errors and report in user-friendly way
  - Inconsistencies in presentation of component composition due to
    late-breaking issues (need to show licenses for all items required
    even though user doesn't see those things anywhere else).

[User Interface](Category:Equinox_p2 "wikilink")