### 3.4 manual tests

This is the testing page used for manual tests during the 3.4 release.
Core tests have been moved into automated test cases. The current UI
manual test page is [p2 UI Manual
Tests](Equinox/p2/UI_Manual_Tests "wikilink")

### Who, what platform

Each tester has an assigned platform and is also assigned a UI group.
What it means to be in a UI group is defined in the UI tests.

  - Pascal, MacOs, XP, UI group A
  - Andrew, Linux (gtk 2.12.1), UI group B
  - DJ, Linux, UI group C
  - John, Vista, Windows XP, UI group A
  - Simon, Windows XP, UI group B
  - Susan, Windows XP, UI group C
  - Tim Mok, ??, UI group A

### UI Tests

There are several different preferences/modes that drive the UI, so we
have assigned user groups to run with different preference settings.
Some of the tests will have you switch some of the toggles or prefs, but
this is the mode you should run otherwise. Some of the tests are
sequential, to keep setup to a minimum. If one test leaves you with a
wizard open, leave it open and read the next test before closing it. The
next test might involve that same wizard.

  - User Group A -
      - Preferences
          - Automatic Updates - Auto updates on, Look for Updates each
            time platform is started, Search for updates and notify when
            available, Notify every 30 minutes
          - Updating from the file system - use defaults
      - Available Software - view by site, \[X\] show only latest
        versions, \[ \] Include items that have already been installed

<!-- end list -->

  - User Group B -
      - Preferences
          - Automatic Updates - Auto updates on, Look for Updates each
            Tuesday at... (use a time that will trigger during the test
            pass), Download new updates automatically, Notify me once
          - Updating from the file system - use defaults
      - Available Software - view by category, \[X\] show only latest
        versions, \[ \] Include items that have already been installed

<!-- end list -->

  - User Group C -
      - Preferences
          - Automatic Updates - Auto updates off
          - Updating from the file system - use defaults
      - Available Software - view by name, \[ \] show only latest
        versions, \[X\] Include items that have already been installed

#### Setup

  - Start with a clean SDK
  - Some of the tests require an SDK later than what you've already
    provisioned. If you don't have one available, you can generate a
    local metadata repository for the SDK that you are using, and hack
    the version number of the SDK root IU to make it look later.
  - Use a fresh workspace so that dialog settings, prefs, and remembered
    licenses start with the defaults
  - Window\>Preferences\>Install/Update\>Automatic Updates
  - Set the preferences according to your user group
  - Help \>Software Updates
  - Switch to Available Software Page
  - Set the view mode and check boxes for your user group

#### Sanity Test of Installed Features

  - Switch to Installed Software Page
  - You should see one entry for the SDK
  - No other entries unless you have enabled some drop-ins

#### Add the test updates site using drag

  - Switch to Available Software Page
  - Drag this link to the available software page:
    <http://download.eclipse.org/eclipse/testUpdates/>
  - It may take a little time to refresh the view, now check
      - Group A - The site should be selected and expanded once the view
        is refreshed
      - Group B - The categories for the site should appear
      - Group C - The IU's in the site should appear

#### Sanity Test of Available Software Page

  - Toggle the \[ \] Show only latest versions
      - Verify that this controls whether all versions or only the
        latest are shown for an IU.
  - Toggle the \[ \] Include items that have already been installed
      - Installed items should disappear when you toggle this. If all
        versions are showing, the installed version disappears but the
        other IU versions show
  - Switch between site, category, and name view
  - Switch back to your assigned view
  - Type "releng" in the filter box, it should appear in bold in site or
    category view, plain in name view
  - Select releng and look at the properties, verify that property pages
    look right

#### Install from End User UI

  - Check mark the releng tools in the Available Software list
  - The install button should enable
  - Click the button and review choices in the wizard
  - Unchecking the box in the wizard should show an error
  - Check box again. You should have a gray finish button because releng
    tools has a license
  - Click "Next" and accep the license
  - Click "Finish"
  - Accept the restart.
  - Notice the presence of the releng tools (should be listed in the
    installed software page)

Verification with admin UI - the provisioned SDK profile should show the
IU

#### Uninstall the releng tools from End User UI

  - Help\>Software Updates
      - Go to Installed features page
      - Check mark the releng tools
      - Uninstall
  - Accept the restart
  - Come back up and notice the absence of the releng tools (no longer
    listed in installed software page)

Verification with admin UI - releng tools are absent

#### Update the SDK from End User UI

  - Ensure you are connected to a site with a newer version of the SDK
  - Help\>Software Updates
  - First verify that the Available Features page has an updated SDK
      - Choose "Manage sites" and add the test site if you need to
  - Now go to Installed features page
      - Click "Check for Updates"
      - You should see the new SDK offered as an update.
      - You shouldn't have to accept a license (assuming the EPL is the
        same for the updated SDK as it was for releng tools)
      - Finish the wizard, accept the restart when done
      - Verify that the Installed Software page now shows the new
        version

#### Reverting the configuration

  - Go to the installed software page
  - Choose "Revert Configuration..." and select a timestamped
    configuration that does not have the upgrade that you just did
  - Push Finish and restart
  - In the installed software list, verify that the original SDK version
    is there

#### Automatic updates (part 1)

  - After restarting from the previous test...
      - User Group A - you should get the popup telling you that
        software updates were found (for the upgrade you just reverted
        from). Click to close the popup so that you get reminded again
        later.
      - User Group B - you should a popup at the specified time telling
        you the update was downloaded. Close the popup.
      - User Group C = you should not get any automatic update
        notifications

#### Automatic updates (part 2) User Group A & B

  - User group A & B should see an affordance in the status bar for the
    updates that are available. (It appears

once you've been reminded about updates).

  - Click on the status bar
  - You should see the update wizard containing the SDK update

#### Automatic updates (part 2) User Group C

  - Go to Installed features page
  - Click "Check for Updates"
  - You should get an update wizard showing the SDK update

#### Update (part 3), all users

  - You should be looking at an update wizard with the SDK update
  - Select the IU and you should see the update description in the
    details area of the wizard
  - If you uncheck the IU, you should get an error message
  - Check it again, and you'll see a progress bar while the validity is
    being computed
  - You should not have to visit the license page since you have already
    done this upgrade
  - Go ahead and update
  - The affordance should be gone and there should be no more reminding
    of updates.

#### Sanity checking of the repositories

  - Try adding different kinds of sites in different ways.
      - Kinds of sites to add:
          - Old update sites
          - p2-generated sites in a folder
          - p2-generated sites in a jar file
          - p2-generated sites in a zip file
          - extension location sites (local folders with an
            eclipse/features, eclipse/plugins dir structure
      - Ways to add sites:
          - Drag URL's from browsers to different targets
              - Available software page
              - Repository dialog (Manage sites...)
              - Add site dialog entry field
          - Drag folders from platform browsers to same targets (may not
            work on Mac and Unix, please annotate
            <https://bugs.eclipse.org/bugs/show_bug.cgi?id=223991> with
            your findings)
          - Use the archive button in the add site dialog to add a jar
            or zip
          - Use the local button in the add site dialog to add a folder
      - Post links to sites you have tested here, so we can see what
        kind of coverage we are getting
          - <http://update.eclemma.org>
          - <http://download.eclipse.org/tools/mylyn/update/e3.4>
  - Use the manage sites dialog to disable some sites
      - Verify that the content of disabled sites is no longer showing

#### Import/export of repositories from UM

  - Start up an Eclipse 3.3 install and export the repositories from
    update manager
      - Help\>Software Updates\>Find and Install...
      - Search for new features to Install\>Next...
      - In the "Update sites to visit" list, note what sites are there
        and whether they are checked or not
      - Export...
  - In your M7 test SDK...
      - Help\>Software Updates
      - Available Software\>Manage Sites...
      - Import...
      - Select the file you just exported
      - You should see the same sites appear with the same check marks.
        The old site names will not be remembered, this is a known
        limitation.
      - Browse the content of the new repositories to sanity check that
        you are seeing what you expect

#### Import/export from p2

  -   - Help\>Software Updates
      - Available Software\>Manage Sites...
      - Export... to a file
      - Select all of the repos and remove them with the remove
        sites...button
      - Import... from the file you just exported
      - Everything should be restored as you expect

#### General preferences

  - Go to the Install/Update preferences
  - Click the "Show all versions of available software" button and close
    the prefs
  - Help\>Software Updates
      - Go to the installed features page and check for updates
      - You should see every version of the SDK that is newer than what
        you installed, not just the latest
      - The latest one should be checked in the wizard
      - Try checking other ones or combinations and see what the
        resolver does

#### Connection to authenticated sites

  - This test requires access to a repository that requires
    authenticated login
  - Click "Add Site", and add the new repository
  - Attempt to expand the site to see the contents, and you should be
    prompted for login
  - You will get three attempts to provide a valid username and password
  - If you enter three bad passwords, you need to remove and re-add the
    repository to be able to try again, or restart Eclipse
  - After entering a valid password, shutdown and restart to verify that
    the password is correctly persisted across sessions

#### Signature trust check during install

  - Connect to a repository that contains self-signed features, or
    features signed with a certificate that is not already linked to a
    certificate in your trust store.
  - Attempt to install software that contains software of unknown trust
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
    prompted again to establish trust (broken in 3.4 M7)

### Core Tests (using Admin UI)

#### Basic install, run and update from Admin UI

  - Using the Admin UI:
      - create a new profile
      - install the SDK-v1
  - Run the provisioned install
  - Using the Admin UI:
      - look for updates, install eclipse SDK-v2
  - Run the provisioned install and verify that the SDK-v2 plugins are
    here
  - Also check that the profile is properly updated in the Admin UI

Variation, try with/without exiting the Admin UI.

#### Basic install, run and update using remote repository

  - Using the Admin UI:
      - create a new profile
      - connect to the test repository
      - install the SDK-v1
  - Run the provisioned install
  - Using the Admin UI:
      - look for updates, install eclipse SDK-v2
  - Run the provisioned install and verify that the SDK-v2 plugins are
    here
  - Also check that the profile is properly updated in the Admin UI

#### Basic install/update from Admin UI

  - Using the Admin UI:
      - Create a new profile
      - Install the SDK-v1
  - Do \*not\* start the provisioned install
  - Using the Admin UI:
      - look for updates, install eclipse SDK-v2
  - Run and verify that the plugins have been changed
  - Also check that the profile is properly updated in the Admin UI

Variation, try with/without exiting the Admin UI.

#### Bundle pool

  - Using the Admin UI:
      - Create a new profile
      - Install the SDK. During the installation notice the dialog
        saying that around 150M (the size of the SDK) should be
        "installed" (caveat, this may already be giving you the same
        result than in the second step if you are installing an already
        installed SDK.)
  - Create a second profile
  - Install the SDK in it
      - During the installation notice the dialog saying that only
        around 50K should be "installed".
      - Notice that the download phase goes much faster.
  - Uninstall the two SDK previously installed
  - Navigate to the folder of the bundle pool and notice that all the
    jars and folders are now gone. the GC has run.

#### Bundle pool in the eclipse install

  - Using the Admin UI:
      - Create a new profile: in the dialog set the install folder and
        the bundle pool to the same location. It will be the root of
        your eclipse install.
  - Install the SDK
  - Verify that the plugins folder is located at a subfolder of the
    install location
  - Verify that the path in the bundles.txt are relative.
  - Verify that the path in the osgi.bundles property of the config.ini
    are relative

#### Download Resilience

  - From the Admin UI
      - Create a profile
      - Install an SDK and while the download proceeds find an inventive
        way (a.k.a not using cancel) to abort the download (e.g. unplug
        the network cable, kill the process, etc.).
      - Restart the installation and see if it completes normally

#### Dropins Reconciler

  - From the Admin UI
      - Create a profile and install the SDK and User UI
  - For M4 the following manual steps are required.
      - Go to your install folder and edit your eclipse.ini
          - add "-console" at the top.
          - add
            "-Dorg.eclipse.equinox.p2.reconciler.dropins.directory=C:\\ontheside\\dropins"
            to the VM arguments at the bottom
          - Note: The dropin folder must not be inside the profile
            because of how frameworkadmin adjusts paths to make them
            relative and a
            [bug](https://bugs.eclipse.org/bugs/show_bug.cgi?id=212067)
            with install dir.
          - If you want to collocate your dropins folder you will have
            to set "-Dosgi.install.area=<file:/c:/yourprofile>"
      - save and start eclipse
      - at the console "start" the
        org.eclipse.equiniox.p2.reconciler.dropins bundle
      - exit eclipse
  - Testing
      - put a bundle in your dropins folder
      - start eclipse and verify that the bundle was installed
      - exit eclipse
      - remove the bundle from your dropins folder
      - start eclipse and verify that the bundle was uninstalled

#### Update Manager compatibility

  - Run the Agent
  - Create a new profile
  - Add metadata and artifact repositories
  - Install an SDK into the profile.
      - Set the bundle pool location to be the same as the install
        directory to get the same setup as the current Eclipse SDK
  - Install the End User UI into the same profile
  - Modify the eclipse.ini file to turn on reconciliation

<!-- end list -->

    -Dorg.eclipse.p2.update.compatibility=true

  - You can now add extension locations via the Update Manager "Manage
    Configurations" dialog
  - Also links/ folders should be discovered on startup

[Tests](Category:Equinox_p2 "wikilink")