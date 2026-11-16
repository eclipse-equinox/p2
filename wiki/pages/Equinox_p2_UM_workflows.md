This page captures user scenarios in the Eclipse Update Manager and
discusses issues or problems that were to be solved by the next
provisioning UI ([Equinox p2 User
Interface](Equinox_p2_User_Interface "wikilink")).

## Scenarios

### Scenario 1: Check for updates

  - Help \>
  - Software Updates \>
  - Find and Install...

![Image:UMFindAndInstall.jpg](images/UMFindAndInstall.jpg
"Image:UMFindAndInstall.jpg")

  - Search for updates of the currently installed features
  - Finish

![Image:UMMirrorSelection.jpg](images/UMMirrorSelection.jpg
"Image:UMMirrorSelection.jpg")

  - select mirror
  - OK

![Image:UMUpdateResults.jpg](images/UMUpdateResults.jpg
"Image:UMUpdateResults.jpg")

  - expand site
  - expand category
  - check mark item(s)
  - Next

![Image:UMLicense.jpg](images/UMLicense.jpg "Image:UMLicense.jpg")

  - I accept...
  - Next

![Image:UMInstallReview.jpg](images/UMInstallReview.jpg
"Image:UMInstallReview.jpg")

  - Finish

#### Click/Decision Count

  - 14 clicks/decisions
  - Can reduce to 12 by checking "Automatically Select Mirrors"
  - Reasonable partitioning of advanced concepts
      - Change Location

### Scenario 2: What add-ons can I get?

  - Help \>
  - Software Updates \>
  - Find and Install...

![Image:UMFindAndInstall.jpg](images/UMFindAndInstall.jpg
"Image:UMFindAndInstall.jpg")

  - Search for new features to install
  - Next

![Image:UMSitesToVisit.jpg](images/UMSitesToVisit.jpg
"Image:UMSitesToVisit.jpg")

  - select sites to visit
  - Finish
  - Progress dialog followed by results dialog
  - Scenario continues similar to update results

#### Click/decision count

  - 7 clicks/decisions to get to browsing mode
  - Can only access content by site
  - Once items are selected for install, 4 clicks to finish (Next, I
    accept..., Next, Finish) as in update scenario

### Scenario 3: Found something cool on the web

Setup: User finds software with an update site reference on the web

`    `![`Image:P2WebContent.jpg`](images/P2WebContent.jpg
"Image:P2WebContent.jpg")

  - Copy URL from update site to clipboard
  - Help \>
  - Software Updates \>
  - Find and Install...
  - Search for new features to install
  - Next

![Image:UMSitesToVisit.jpg](images/UMSitesToVisit.jpg
"Image:UMSitesToVisit.jpg")

  - New Remote Site...

![Image:UMRemoteSite.jpg](images/UMRemoteSite.jpg "Image:UMRemoteSite.jpg")

  - Type a name for the site
  - Paste URL into location field
  - OK
  - Uncheck any sites that should not be included in the search
  - Finish
  - Expand site
  - Expand category

#### Click/decision count

  - 14 clicks/decisions to get new content shown in UI
  - once items are selected for install, 4 clicks to finish (Next, I
    accept..., Next Finish) as in install scenario.

### Scenario 4: What do I have?

  - Help \>
  - Software Updates \>
  - Manage Configuration...

![Image:UMManageConfiguration.jpg](images/UMManageConfiguration.jpg
"Image:UMManageConfiguration.jpg")

  - Expand install location

![Image:UMInstallList.jpg](images/UMInstallList.jpg "Image:UMInstallList.jpg")

  - Expand top level feature to see feature detail

![Image:UMFeatureDetail.jpg](images/UMFeatureDetail.jpg
"Image:UMFeatureDetail.jpg")

#### Click/decision count

  - 4 clicks/decisions to see high level of what user installed
  - Expanding top level features shows same detail user could see in
    Feature Details view of about dialog (Saves these 3 steps to see
    detail)

<!-- end list -->

  - Help \>
  - About Eclipse SDK
  - Feature Details

![Image:UMAboutFeatureDetail.jpg](images/UMAboutFeatureDetail.jpg
"Image:UMAboutFeatureDetail.jpg")

  - No link from UM installed view to Help \> About information

#### Detailed plug-in view of what's installed

  - Help \>
  - About Eclipse SDK
  - Plug-in Details

### Scenario 5: Automatic Updating

Preferences are provided for the user to set up automatic update
scheduling on every startup of Eclipse, or at daily or weekly scheduled
times. Users can also set a preference so that updates are automatically
downloaded before the user is ever notified that updates have been
found.

Setup: User is working and updates have been discovered and/or
downloaded

  - User is interrupted with modal dialog notifying of updates

![Image:UMUpdateNotify.jpg](images/UMUpdateNotify.jpg
"Image:UMUpdateNotify.jpg")

  - User must choose yes or no
      - Choosing yes will present user with search results wizard from
        other scenarios
      - Choosing no will ignore the updates. User must check manually in
        order to see them again

#### Click/decision count

  - Work interrupted, user must click yes or no
      - Choosing yes, 7 clicks/decisions to finish the update
      - Choosing no, 14 clicks/decisions to find updates manually later
        (Same as Scenario 1 above)

### Scenario 6: What's wrong with my configuration?

If users encounter a problem that leads them to believe that plug-ins
they expect to be installed are not there, or that there are
compatibility conflicts with plug-ins, they can use the **Manage
Configuration...** dialog to work with their configuration. This dialog
will show them if there are conflicts with certain features or plug-ins.
It also allows them to disable features without uninstalling them.

To resolve a problem, users can choose to revert to a previously known
configuration.

  - Help \>
  - Software Updates \>
  - Manage Configuration...

![Image:UMManageConfiguration.jpg](images/UMManageConfiguration.jpg
"Image:UMManageConfiguration.jpg")

  - Revert to Previous

![Image:UMRevertDialog.jpg](images/UMRevertDialog.jpg
"Image:UMRevertDialog.jpg")

#### Click/decision count

  - 4 clicks to browse revert history
  - You can't tell what is actually in each configuration, only what
    happened to cause it to be saved.
  - It can be very difficult to understand the descriptive detail in the
    revert history. For example, the most recent configuration is
    described as being created due to the action 'Disabled' for the
    eclemma feature. However this does not mean that the feature is
    disabled in that configuration. In fact, the feature is enabled in
    that configuration. The list is trying to convey that it is the
    disabling of the feature that caused the configuration to be saved.
    This is confusing until you can wrap your head around it. It would
    be useful to instead show some kind of delta between configurations.

### Scenario 7: What sites am I using

There is no notion of configuring the sites used by update manager. When
searching for updates, UM uses site references located in the features
themselves or in the sites themselves. When searching for new software,
the user selects the sites to be used each time a search operation is
performed. If a user wishes to work with sites (add, remove, export,
import), they must know to go through the search wizard.

  - Help \>
  - Software Updates \>
  - Find and Install...
  - Search for new features to install
  - Next

![Image:UMSitesToVisit.jpg](images/UMSitesToVisit.jpg
"Image:UMSitesToVisit.jpg")

#### Click/decision count

  - 5 clicks to get to a page where sites can be configured
  - User had to know to use the search wizard to get here

## Overall Problems

  - User must always decide first if they are "searching" or "managing"
    and it's not always obvious. This separation of searching ("what can
    I get") vs. managing ("what do I have") is caused by performance
    characteristics of update sites (long download times). User defines
    the search and can keep working until results appear.
  - High click count to update content, find new content or add new
    content found on web
  - Repo management always visited as part of basic workflows
  - No way to find something by name
  - Finding updates interrupts workflow
  - Presentation of detail that user didn't understand (error messages
    referencing software user didn't know about)
  - No ability to retarget UI (dialog vs. preference page) - "All or
    Nothing" implementation

[User Interface](Category:Equinox_p2 "wikilink")