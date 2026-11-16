## Sanity Tests

These tests should be run quickly every time you take a new build.

### Sanity test of installed software

  - Help-\>About
  - Press the Installation Details... button
  - You should start out on the "Installed Software Page"
  - You should see one entry for the SDK
  - You can expand this to see the SDK's requirements
  - Selecting a top-level item in this list should enable the update and
    uninstall buttons
  - If any lower level items are selected, the buttons should be gray

### Sanity checking of the repositories

  - Help-\>Install New Software...
  - Add one p2 and one update manager site (see [\#Test
    Sites](#Test_Sites "wikilink"))
      - Drag URL from browsers to different targets
          - Available software list in first page of wizard
          - Add site dialog entry field
  - Use Preferences\>Install/Update\>Available Software Sites to disable
    one of the sites
      - Verify that the content of disabled sites is no longer showing
      - Verify that the list of sites showing in the Install wizard
        "Work With" combo contains the enabled sites
  - Select different sites in the "Work With" combo and verify that the
    content only from that site is showing.

### Sanity Test of Available Software wizard page

  - Help-\>Install New Software...
  - Toggle the \[ \] Show only latest versions
      - Verify that this controls whether all versions or only the
        latest are shown for an IU.
  - Toggle the \[ \] Hide items that are already installed
      - Installed items should disappear when you toggle this. If "Show
        only latest..." is not checked, then you should only see IU
        versions of installed things if they are newer than what is
        installed.
  - Toggle the \[ \] Group items by category
  - Switch back to your assigned view
  - Type "releng" in the filter box, it should appear in bold in
    category view, plain in name view
  - Select releng, and verify that details are shown in the detail pain.
    Select the "More..." link and verify that the property pages look
    right.

## Test Sites

  - remote composite sites
      - <http://download.eclipse.org/eclipse/updates/3.6-I-builds>
      - <http://download.eclipse.org/releases/helios>
      - <http://download.eclipse.org/releases/ganymede>
      - <http://download.eclipse.org/releases/galileo>
  - other remote sites
      - <http://www.polarion.org/projects/subversive/download/eclipse/2.0/update-site/>
      - <http://update.eclemma.org>
      - <http://findbugs.cs.umd.edu/eclipse>
      - <http://www.eclipse.org/equinox/p2/testing/updateSite>
  - update manager sites with install handlers
      - <http://www.developer.com/img/2007/03/upd-install-site.xml>
  - Authenticated and ill-behaved sites
      - Use the [Equinox/p2/Testing\#Testserver p2 Test
        Server](Equinox/p2/Testing#Testserver_p2_Test_Server "wikilink")
      - See the launch config in **org.eclipse.equinox.p2.testserver**.
        Once launched, point your browser to
        <http://localhost:8080/public/index.html> for instructions
  - local archived p2 site
  - local folders with an eclipse/features, eclipse/plugins dir
    structure

## Test Pass 3.6 M7

### Areas of Emphasis

  - We should sanity check the authentication and "repo not found"
    prompts against 3.5. Are we prompting more than we used to?
  - Switch to different repositories in the combo. If you have to wait
    for a repo to load (you see "Pending..."), try filtering,
    cancelling, changing checkboxes while this is happening. The
    filtering should be more responsive in M7
  - Please annotate
    [Bug 277265](https://bugs.eclipse.org/bugs/show_bug.cgi?id=277265)
    if you have a case where you can't **Finish** when installing.
  - Please annotate
    [Bug 300441](https://bugs.eclipse.org/bugs/show_bug.cgi?id=300441)
    if you have a case where you see odd Next/Finish button enablement
    when installing (Next enabled when it shouldn't be, Finish enabled
    when it shouldn't be, buttons that are enabled but don't do
    anything, etc.)

### Test Pass Groups

User groups are assigned to run with different preference settings. Some
of the tests will have you switch some of the toggles or prefs, but the
mode described below is the mode you should run otherwise. Some of the
tests are sequential, to keep setup to a minimum. If one test leaves you
with a wizard open, leave it open and read the next test before closing
it. The next test might involve that same wizard.

  - User Group A -
      - Automatic Updates - Auto updates on, Look for Updates each time
        platform is started, download new updates automatically, Notify
        me once
      - Available Software
          - \[ \] show only latest versions
          - \[X\] Hide items that are already installed
          - \[X\] Group items by category
          - \[X\] Contact all update sites during install to find
            required software

<!-- end list -->

  - User Group B -
      - Automatic Updates - Auto updates off
      - Available Software
          - \[X\] show only latest versions,
          - \[X\] Hide items that are already installed
          - \[ \] Group items by category
          - \[ \] Contact all update sites during install to find
            required software

<!-- end list -->

  - User Group C -
      - Automatic Updates - Auto updates on, Look for Updates each time
        platform is started, Search for updates and notify when
        available, Notify every 30 minutes
      - Available Software
          - \[ \] show only latest versions
          - \[ \] Hide items that are already installed
          - \[ \] Group items by category
          - \[ \] Contact all update sites during install to find
            required software

### Test Matrix

Each tester has an assigned platform and UI group.

  - Pascal, MacOs, XP, UI group A
  - Simon, Windows XP, UI group B
  - DJ, Linux, UI group C
  - John, Vista, Windows XP, UI group A
  - Susan, Windows 7, UI group A, B
  - Ian, Linux, UI Group C
  - Daniel, Linux 64, UI Group A
  - Andrew, Linux 64, UI Group B

## Manual Tests

### Setup

  - Start with a clean SDK
  - The update tests require an SDK later than what you've already
    provisioned. If this is not practical at the time you are running
    the tests, then first install a feature for which there is a later
    update available, and then test updating that feature (such as
    Eclemma, FindBugs, Field_Assist_Example Feature, etc.)
  - Use a fresh install so that dialog settings, prefs, repo caches, and
    remembered licenses are cleared and start with defaults
  - Window\>Preferences\>Install/Update\>Automatic Updates
      - Set the preferences according to your user group
  - Help \>Install New Software
      - Set the check boxes for your user group

### Sanity Test of Installed Features

  - Help-\>About
  - Press the Installation Details... button
  - You should start out on the "Installed Software Page"
  - You should see one entry for the SDK
  - You can expand this to see the SDK's requirements
  - Selecting a top-level item in this list should enable the update and
    uninstall buttons
  - If any lower level items are selected, the buttons should be gray

### Add the test updates site using drag

  - Help-\>Install New Software...
  - Drag this link to the available software page:
    <http://download.eclipse.org/eclipse/updates/3.6-I-builds>
  - It may take a little time to refresh the view, now check
      - Group A - The categories for (only) the new site should appear,
        the site is shown in the combo box.
      - Group B - The IU's in the site should appear, the site is shown
        in the combo box
      - Group C - The IU's in the site should appear, the site is shown
        in the combo box

### Sanity Test of Available Software wizard page

  - Switch between different repos in the Work With: combo box as the
    top and verify that only content from those repos are shown when the
    repo is active in the combo. Choosing "All Available Sites" should
    show everything
  - Toggle the \[ \] Show only latest versions
      - Verify that this controls whether all versions or only the
        latest are shown for an IU.
  - Toggle the \[ \] Hide items that are already installed
      - Installed items should disappear when this box is checked. If
        all versions are showing, the installed version and versions
        earlier than the installed version should not show. Newer IU
        versions will still show, and should show an update icon
  - Toggle the \[ \] Group items by category view
  - Switch back to your assigned checkbox states
  - Type "releng" in the filter box, it should appear in bold in
    category view, plain in name view
  - Select releng and look at the properties, verify that property pages
    look right

### Install

  - Check mark the releng tools in the Available Software list
  - The next button should enable
  - Review choices in the wizard
  - You should have a gray finish button because releng tools has a
    license
  - Click "Next" and accept the license
  - Click "Finish"
  - Accept the restart.
  - Notice the presence of the releng tools (should be listed in the
    installed software page)

### Uninstall the releng tools

  - Help\>About\>Installation Details...
  - Select the releng tools
  - Uninstall
  - Accept the restart
  - Come back up and notice the absence of the releng tools (no longer
    listed in installed software page)

### General preferences/install/upgrade scenario

  - Help\>Install New Software
  - Drag or paste this site in the combo:
      - <http://www.eclipse.org/equinox/p2/testing/updateSite>
  - Check "Field_Assist_Example Feature" 1.0.0 and push "Next"
  - After the restart dialog, choose Apply Changes
  - Go to the Install/Update preferences
  - Click the "Show all versions of available software" button and close
    the prefs
  - Help\>Check for Updates...
      - You'll get a wizard that lists version 1.1.0 and 2.0.0 of the
        same feature. Version 2.0.0 should be selected by default.
      - Click "Next" and you'll move to the details page without any
        delay (install is already resolved)
      - Press "Back" and check the other version instead
      - Press "Next" and this time it will have to recompute the resolve
      - Press "Back" and try selecting both, then "Next"
      - You should get a resolve error
      - Select version 1.1.0 and finish the update.

### Update the SDK

  - Ensure you are connected to a site with a newer version of the SDK
  - First verify that the Available Features page has an updated SDK
      - Help-\>Install New Software...
      - Choose "Add site..." and add the test site if you need to
      - Uncheck the \[ \] Hide items that are already installed and
        verify that you see an SDK and that it shows an update icon
      - Cancel the wizard
  - Help\>Check for Updates...
  - You should see the new SDK offered as an update
  - You should also see the "Field_Assist_Example Feature 2.0.0"
    offered as an update
      - Click "Next..." and in the Install Details you should be able to
        expand the SDK and see what items underneath

it will be also be installed

  -   - You shouldn't have to accept a license for the SDK (assuming the
        EPL is the same for the updated SDK as it was for releng tools)
      - You will have a new license to accept for the field assist
        feature
      - Finish the wizard, accept the restart when done
      - Verify that the Installed Software page now shows the new
        version of your upgrades

### Reverting the configuration

  - Help\>About\>Installation Details...
  - Select the "Installation History" page
  - Select a timestamped configuration that does not have the upgrade
    that you just did
  - Push **Revert** and restart
  - In the installed software list, verify that the original SDK and
    Field Assist version is there

### Automatic updates (part 1)

  - After restarting from the previous test...
      - User Group A - you should get a popup telling you the software
        updates have been downloaded. However this will take some time
        since the updates will be downloaded first. Close the popup.
      - User Group B - you should not get any automatic update
        notifications
      - User Group C - you should get the popup telling you that
        software updates were found (for the upgrade you just reverted
        from). This should happen within a minute or two (after the
        repos have loaded). Click to close the popup so that you get
        reminded again later.

### Automatic updates (part 2) User Group A & C

  - User group A & C should see an affordance in the status bar for the
    updates that are available. (It appears

once you've been reminded about updates).

  - Click on the status bar
  - You should see the update wizard containing the SDK update

### Automatic updates (part 2) User Group B

  - Help-\>Check for Updates...
  - You should get an update wizard showing the SDK update

### Update (part 3), all users

  - You should be looking at an update wizard with the SDK and Field
    Assist update
  - You should be able to check/uncheck the proposed updates. If no
    items are checked the "Next..." button should be gray
  - Select the IU and you should see the update description in the
    details area of the wizard
  - Select "More..." to see the properties page for the selected IU
  - Click "Next..." to see the details about the updates. You should be
    able to expand the SDK item and see more detail
  - You should not have to visit the license page since you have already
    done this upgrade
  - Go ahead and update
  - The affordance should be gone and there should be no more reminding
    of updates.
  - User Group A - the update should happen quite fast because the
    software has already been downloaded

### Updating to accept a patch

<Now sufficiently covered in automatic tests>

### Sanity checking of the repositories

  - Open the "Install New Software..." wizard and try adding different
    sites [\#Test Sites](#Test_Sites "wikilink") in different ways
    inside this wizard. Each time a site is added, it should appear as
    the selected site in the "Work With" combo and the software should
    be filtered by that site. Try each of these techniques:
      - Drag URL's from browsers to different targets
          - The Work With: combo box in the available software page
              - Select items in the combo
              - Type new repo names in the combo
              - Use the autocomplete feature to select a repo
          - Available software list
          - Repository preferences page
          - Add site dialog entry field
      - Drag folders from platform browsers to same targets (Please
        annotate
        [Bug 223991](https://bugs.eclipse.org/bugs/show_bug.cgi?id=223991)
        with any platform differences)
      - Use the archive button in the add site dialog to add a jar or
        zip
      - Use the local button in the add site dialog to add a folder
      - Paste a site name into the "Work With" combo and press Enter
  - Open the site preferences from the install wizard using the "Go to
    the Available Software Sites" preference link. Work done inside this
    preference page should not cause an update in the underlying wizard
    until you press OK.
      -   - Drag sites into the page as described above
          - Add sites using the dialog as described above (try adding a
            name)
          - Edit the name and/or location using the Properties dialog
          - Disable and enable different sites
          - Rename sites by clicking in the name field and editing the
            name
          - Press OK

      - Verify that the changes made in the preference page are
        reflected in the wizard

### Import/export from p2

  - Window\>Preferences\>Install/Update\>Available Software Sites
  - Export... to a file
  - Select all of the repos and remove them with the remove
    sites...button
  - Import... from the file you just exported
  - Everything should be restored as you expect

### Installing software that requires Install Handlers (p2 install actions)

(Now covered by an automated test)

### Switch to Update Manager for old-style install handlers

  - Add the site
    <http://www.developer.com/img/2007/03/upd-install-site.xml>
  - Check "Feature Feature 1.0.0" and press the Next button
  - A message box should appear that informs you that the feature cannot
    be installed and offers to launch the update manager.
  - Press "Launch" and verify that the update manager is launched.

### Connection to authenticated sites

  - Ensure the test server is started
  - Help-\>Install New Software...
  - Paste or drag this site into the Work With combo.
    <http://localhost:8080/proxy/never>
  - You should be prompted for login
  - You will get three attempts to provide a valid username and password
  - If you enter three bad passwords, you need to remove and re-add the
    repository to be able to try again, or restart Eclipse
  - Check the box to save the password across sessions
  - After entering a valid password, shutdown and restart to verify that
    the password is correctly persisted across sessions
      - The site password can be removed through the Secure Storage
        section of the preferences
  - If you do not save the password across sessions, you should only
    have to authenticate once per session

### Signature trust check during install

  - Add this repository:
    <http://www.eclipse.org/equinox/p2/testing/selfsignedsite>
  - Attempt to install software that contains software of unknown trust
    from the above site
  - Dialog opens showing certificate chain
  - Should be able to look at details of each certificate in a separate
    dialog
  - Close the dialog without indicating that you trust any certificates
  - Install should abort, but content will remain downloaded in your
    eclipse/plugins directory
  - Attempt to install the same software again
  - Nothing will be downloaded, but you will be asked again to establish
    trust
  - Indicate that you trust the certificate provider
  - Install should succeed
  - Uninstall the software
  - Shutdown and restart
  - Attempt to install the software again, and you should not be
    prompted again to establish trust

### Publisher Tests

  - Create a Feature Project
  - Create a Category Definition (New -\> Other -\> Category Definition)
      - Add a new category
      - Add the feature to that category
  - Export the feature (Export -\> Deployable features) and on the
    options tab select categorize repository and choose the category
    file
  - Help -\> Install New Software
      - Add a local site (point to your exported repository)
      - Ensure that the feature is properly categorized

[UI Manual Tests](Category:Equinox_p2 "wikilink")