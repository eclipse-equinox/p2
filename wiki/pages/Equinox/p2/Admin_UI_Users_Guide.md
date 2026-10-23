__TOC__

## About the Eclipse Provisioning RCP Agent

The Eclipse Provisioning RCP agent, also known as the "Admin UI" is used
by the p2 team to try out function and configure profiles. This
application will unlikely be delivered as is and is definitely not what
regular users will experience to install eclipse software.

`  `![`Image:RCPAgent.jpg`](images/RCPAgent.jpg "Image:RCPAgent.jpg")

It is also the tool we distribute for doing an initial install of an
Eclipse SDK using p2. To follow step by step instructions for
downloading the agent and using it to install an SDK, see [Equinox p2
Getting Started](Equinox_p2_Getting_Started "wikilink").

## Things to know

  - The Admin UI in the RCP agent is simply a browsing interface into
    the provisioning API. It does not represent what we think actual
    user workflows for installing or updating an Eclipse-based
    application would be. See [Equinox p2 Update UI Users
    Guide](Equinox_p2_Update_UI_Users_Guide "wikilink") for information
    on the end-user update UI.
  - The Admin UI is very flexible in manipulating the environment,
    sometimes moreso than the underlying infrastructure supports.
    Failures can be confusing (error reporting that requires you to
    check your .log file, read stack traces, or understand the innards
    of the provisioning engine).
  - The UI described below can also be run from a full Eclipse SDK
    workbench. You can open the **Provisioning** perspective to see the
    same functionality that you see in the downloaded agent.

## Overview of the Eclipse Provisioning RCP Agent

  - The RCP app is comprised of three views:
      - The **Metadata Repositories** view shows those
        [metadata](Equinox_p2_Concepts "wikilink") repositories known by
        the provisioning infrastructure.
          -
            ![Image:metadatarepo.jpg](images/metadatarepo.jpg
            "Image:metadatarepo.jpg")
        <!-- end list -->
          - The provisioning code uses local metadata repositories to
            store information about your environment. In general, these
            repositories are not shown, but you can control this using a
            preference on the provisioning preference page.
          - The repositories may be remote (such as those provided on
            eclipse.org), local (those generated using the p2 metadata
            generator), or archived (zips or jars made available on
            various sites).
          - You may add or remove repositories using this view.
          - Expanding a metadata repository will show you the
            [installable units](Equinox_p2_Concepts "wikilink")(IU's)
            available for installing.
          - You may examine the properties of repositories or IU's
          - You may install an IU to a specified profile using drag and
            drop or the popup menu.
      - The **Artifact Repositories** view shows those
        [artifact](Equinox_p2_Concepts "wikilink") repositories known by
        the provisioning infrastructure.
          -
            ![Image:ArtifactRepo.jpg](images/ArtifactRepo.jpg
            "Image:ArtifactRepo.jpg")
        <!-- end list -->
          - Like metadata repositories, artifact repositories may be
            remote, local, or zipped.
          - You may add or remove repositories using this view.
          - Expanding an artifact repository will show you what
            artifacts are located there
          - You may examine the properties of the repository
          - Expanding an artifact will show you its descriptor
      - The **Profiles** view shows profiles defined for installing
        software. [Profiles](Equinox_p2_Concepts "wikilink") are the
        target of an install operation.
          -
            ![Image:Profiles.jpg](images/Profiles.jpg "Image:Profiles.jpg")
        <!-- end list -->
          - The **EquinoxProvisioningUI** profile represents the agent
            application itself. It contains the IU's (bundles) that
            comprise the RCP app.
          - You may add or remove profiles using this view.
          - Expanding a profile shows you what IU's have been installed
            in the profile.
          - You may examine and change the properties of profiles and
            IU's.
          - You may uninstall an IU from a profile using the popup menu.
          - You may check for updates that may be available for an IU
            using the popup menu.
  - The **Provisioning** preference page lets you control some
    application preferences:
    \*:![Image:P2Preferences.jpg](images/P2Preferences.jpg
    "Image:P2Preferences.jpg")
      -   - You can control whether you view all IU's in the
            repositories and profiles, or just those that have been
            marked as a "group" IU. Viewing only the groups helps to
            reduce the clutter when you are trying to find something to
            install like the SDK.
          - You can control whether internal/implementation repositories
            should be shown in your repositories views.

## Cool stuff you can try

  - ![P2-admin-ui-in-Ganymede.png](P2-admin-ui-in-Ganymede.png
    "P2-admin-ui-in-Ganymede.png") If you install the SDK into the agent
    profile (**EquinoxProvisioningUI**), then the next time you launch
    the rcp app, you'll actually be running the Eclipse SDK with the
    Provisioning Perspective. See also [Admin UI Eclipse
    Perspective](Starting_Eclipse_Commandline_With_Equinox_Launcher/p2_Admin_UI#Admin_UI_Eclipse_Perspective "wikilink").

## For more information

If you encounter bugs, or would like to enter enhancement requests for
this work, please use the Equinox Incubator category in
[Bugzilla](https://bugs.eclipse.org/bugs/enter_bug.cgi?product=Equinox).
You can add the prefix "\[prov\]" to the subject line of the bug report
to help us with bug triage.

  - See [Equinox p2 User
    Interface](Equinox_p2_User_Interface "wikilink")

[Admin UI Users Guide](Category:Equinox_p2 "wikilink")