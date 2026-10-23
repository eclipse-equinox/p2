__TOC__

## Overview

The Equinox administrator UI is a developer tool that helps you explore
and manage applications provisioned with [Equinox
p2](Equinox_p2 "wikilink"). This article guides you through a happy-path
using the Equinox p2 Administrator UI to install an Eclipse SDK.

There are four basic concepts that will help you understand the install:

  - **Agent** - the program that will perform the install. In general,
    the provisioning agent could appear in various forms - a standalone
    application, a silent install demon, a perspective in the ide. We
    will use the [Admin UI
    application](Equinox_p2_Admin_UI_Users_Guide "wikilink") to do our
    install. Note that this Admin UI will not be what regular users will
    experience.
  - **Metadata** - the information about what can be installed. The
    metadata is used by the agent to analyze dependencies and perform
    the installation steps. Metadata lives in one or more repositories.
  - **Artifacts** - the actual bits that will be installed. There are
    various kinds of artifacts that are processed differently during the
    install. Associated metadata determines how a given artifact is
    processed. Artifacts live in one or more repositories. The metadata
    and artifacts generally come from different repositories and may be
    widely distributed.
  - **Profile** - in the most simple form, a profile is the location
    where the bits will be installed. The term 'profile' is not a very
    good term and probably will disappear from end user concepts
    (there's a bug about this), but it is the term we are using for now.

## Steps to using the agent

In this example, we will be installing a 3.4M5 SDK. Versions prior to M5
no longer work with the Eclipse test site. The example uses the
windows-based agent, but other platforms are available on the [Equinox
download site](http://download.eclipse.org/equinox/).

### Step 1 - Downloading the agent

The Admin agent application is available from the [Equinox download
site](http://www.eclipse.org/downloads/download.php?file=/eclipse/equinox/drops/S-3.4M5-200802071530/equinox-p2-agent-3.4M5-win32.zip)
- click the link and you should see (Firefox)

`   `![`Image:AgentDownload.jpg`](images/AgentDownload.jpg
"Image:AgentDownload.jpg")

The zip file is \~11 MB. Download it and unzip it anywhere on a writable
local drive. The result of this step is that you will have a directory
(equinox.p2) containing the agent:

`   `![`Image:ProvDirectory.jpg`](images/ProvDirectory.jpg
"Image:ProvDirectory.jpg")

### Step 2 - Exploring the agent

Double-click on the eclipse.exe and the Admin UI will come up. It looks
something like *(resized to fit here)*:

`   `![`Image:InitialRCPAgent.jpg`](images/InitialRCPAgent.jpg
"Image:InitialRCPAgent.jpg")

Note that you see here all four concepts:

1.  Agent - the main window.
2.  Metadata - the exposed **Metadata Repositories** view.
3.  Artifacts - the hidden **Artifact Repositories** view.
4.  Profiles - the **Profiles** view.

The Metadata Repositories view is empty, so first let's add a repository
on eclipse.org which contains metadata for the Eclipse SDK, and other
goodies. You can do this from the popup menu in the Metadata
Repositories view.

`   `![`Image:SelectAddRepository.jpg`](images/SelectAddRepository.jpg
"Image:SelectAddRepository.jpg")

You'll see a dialog that lets you add a URL (or a local directory or jar
file). We're going to use the Eclipse project milestone update site
(http://download.eclipse.org/eclipse/updates/3.4milestones/). (the
screen shot shows a different repository that is more up to date but
less stable)...

`   `![`Image:AddMetadataRepo.jpg`](images/AddMetadataRepo.jpg
"Image:AddMetadataRepo.jpg")

Once the repository has been added, you should see something like this:

`   `![`Image:RCPAgent.jpg`](images/RCPAgent.jpg "Image:RCPAgent.jpg")

The Profiles view shows the Equinox Provisioning UI profile. This is the
profile defining the install location for the Admin UI app you are
running. If you right click on the list item and select properties, you
will see some information about this profile:

`   `![`Image:AgentProfileProperties.jpg`](images/AgentProfileProperties.jpg
"Image:AgentProfileProperties.jpg")

If a Metadata repository or a Profile is not empty, then you can expand
it to see the installable units (which are really installed units) in
the profile. A preference is available to show only the installable
units which are a Group; the default for this preference is true.
Expanding to show the groups will give you something like this:

`   `![`Image:ExpandedInstallableUnits.jpg`](images/ExpandedInstallableUnits.jpg
"Image:ExpandedInstallableUnits.jpg")

Now, we'll switch to the Artifact Repositories view. It is empty because
you have not yet added an artifact repository.

The metadata repository you added describes what software is available,
while the artifact repository contains the actual software that will be
downloaded. These repositories don't have to be located at the same URL,
but they can be.

The server on eclipse.org has the metadata and artifacts in the same
location, so you can add an artifact repository using the same URL we
used for the metadata repository (by using the popup menu in the
Artifact Repositories view). Once the repository has been added, you
should see the actual artifacts in the Artifact Repository view:

`   `![`Image:Artifacts.jpg`](images/Artifacts.jpg "Image:Artifacts.jpg")

### Step 3 - Creating a new profile

You will need to decide where you want to install the SDK and create a
profile for that location. In the profile view, right click and select
'Add profile...':

`   `![`Image:SelectNewProfile.jpg`](images/SelectNewProfile.jpg
"Image:SelectNewProfile.jpg")

then fill in the appropriate data in the Profile properties dialog:

`   `![`Image:NewProfile.jpg`](images/NewProfile.jpg "Image:NewProfile.jpg")

The meanings of many of these properties are advanced, so don't play
with the default settings unless you want to explore error conditions\!

### Step 4 - Doing the install

The **sdk** group is near the bottom of the list of group installable
units in the repository, so scroll down until **sdk** is in view. There
may be multiple versions of the sdk available. If so, find the one you
are interested in.

**NOTE: Due to
[bug 208143](https://bugs.eclipse.org/bugs/show_bug.cgi?id=208143) you
should install the 1101-0010 version of the SDK IU.**

The Admin UI supports two ways of initiating an install: 1). you can
right click on the **sdk** group, select Install..., then choose the
profile in the resulting popup dialog; or 2). you can drag\&drop the
**sdk** group onto the profile. Let's do d\&d since it is easier (the
little smudge over SDK is the drop cursor):

`   `![`Image:DropSDK.jpg`](images/DropSDK.jpg "Image:DropSDK.jpg")

You will see a progress indicator while the agent figures out what needs
to be downloaded. Next, you will be shown more detail about what is to
be installed, such as the size of the artifacts needed. (We'll be
showing more information here as the project evolves, such as license
information or detail).

`   `![`Image:InstallDialog.jpg`](images/InstallDialog.jpg
"Image:InstallDialog.jpg")

Click the install button, and the install will start (assuming you have
network connectivity org.eclipse.download, no firewall issues, etc):

`   `![`Image:InstallProgress.jpg`](images/InstallProgress.jpg
"Image:InstallProgress.jpg")

Anecdotally, the install takes slightly less time than the download of a
corresponding Eclipse SDK zip (one datapoint: 21 minutes for equinox
provisioning install vs. 23 minutes for download of the zip).

### Step 5- After the install

The install will finish with a whimper, not a bang, as the progress
dialog disappears and the new profile you created is populated with the
groups and installable units that are now installed into the profile:

`   `![`Image:AgentPostInstall.jpg`](images/AgentPostInstall.jpg
"Image:AgentPostInstall.jpg")

The directory you chose for your profile location will look like this,
before running the eclipse you just installed:

`   `![`Image:InstalledSDK.jpg`](images/InstalledSDK.jpg
"Image:InstalledSDK.jpg")

### Step 6- Installing the end user UI

The Agent UI is really intended for system administrators and power
users who want to browse and manage the applications they have
installed. A typical end user of an Eclipse-based application doesn't
want to be exposed to all these concepts of metadata, artifacts,
profiles, and IUs. They just want to install, run, and update their
software.

For this purpose, there is a p2-based end user UI intended for dropping
into an Eclipse application. Once you have installed the SDK, you can
install this end-user UI by dragging the "userui" group from the
metadata repository into the profile that you just installed the SDK
into.

`   `![`Image:InstallUserUI.jpg`](images/InstallUserUI.jpg
"Image:InstallUserUI.jpg")

Now, when you start up the SDK, there will be a new menu entry (Help \>
Software Updates (Incubation), that you can use to manage and update the
running application.

You can find out more about this UI in [Equinox p2 Update UI Users
Guide](Equinox_p2_Update_UI_Users_Guide "wikilink")

## See also

If you encounter bugs, or would like to enter enhancement requests for
this work, please use the Equinox Incubator category in
[Bugzilla](https://bugs.eclipse.org/bugs/enter_bug.cgi?product=Equinox).
You can add the prefix "\[prov\]" to the subject line of the bug report
to help us with bug triage.

For more detailed information, visit one or more of the following pages:

  - [Equinox p2](Equinox_p2 "wikilink") Top level site for the
    provisioning work going on in Equinox.
  - [Equinox p2 Admin UI Users
    Guide](Equinox_p2_Admin_UI_Users_Guide "wikilink") For additional
    information about the agent UI you just used.
  - [Equinox p2 Update UI Users
    Guide](Equinox_p2_Update_UI_Users_Guide "wikilink") Information
    about the "end user" UI that can be dropped into an SDK (or any
    other Eclipse RCP application).
  - [Equinox p2 Console Users
    Guide](Equinox_p2_Console_Users_Guide "wikilink") For OSGI geeks who
    enjoy starting and stopping bundles.
  - [Equinox p2 Getting Started for
    Developers](Equinox_p2_Getting_Started_for_Developers "wikilink")
    For developers who want to create applications that extend or
    exploit equinox provisioning.

[Getting Started Admin UI](Category:Equinox_p2 "wikilink")