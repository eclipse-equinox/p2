This page will be used to keep track of initial thoughts on the
compatibility between p2 and update manager.

### Problems with the platform.xml

There are some problems with the current form of the platform.xml file.
Here is a list of some of the things which come to mind.

  - inference of bundle name from the file path
  - there isn't a way to set the start level for a bundle which isn't on
    the osgi.bundles list
  - ditto for the initial state (started or not)
  - p2 explicitly lists all bundles, you don't get automatic site
    discovery, have to install the directory watcher bundle for this
  - directory watcher can be run at anytime...no only at startup
  - look at the platform.xml and create artifact/metadata repositories
    for it

### Questions

  - Are we shipping the Update Manager UI with Eclipse 3.4?
  - Are we shipping org.eclipse.update.core.\* with Eclipse 3.4?
  - What is considered API in the update bundles? Yes, almost everything
    is marked as provisional but its been like that since 2.0 and if we
    don't offer some level of support then a lot of people may be
    broken.
  - Once you install something with p2, can you go back to the UM UI?
    Can features reference IUs?
  - Reverse scenario.
  - Will we have to modify the simple configurator to handle links/
    folders?
  - What changes will we have to make to support the Update Manager's
    Manage Configurations dialog?

### Scenarios

  - install a feature from an old-style update site that doesn't have p2
    metadata
      - don't do it
      - (have 2 UIs) use old code path, spoof up metadata for what we
        downloaded or hack update to generate md for what we are
        downloading
      - generate p2 metadata on the fly for what is on the update site
  - People who are using the update.core APIs in headless applications.
  - Running SDK with UM and p2.
      - Install IU which represents (for instance) RCP via p2.
      - Want to install a feature which depends on RCP via UM.
      - Do we know that the RCP feature is installed and our dependency
        requirements are met?

### Ideas

  - Hook the update.ui bundle so it calls new p2 APIs.
  - Have the update manger bundles operate as per Eclipse 3.3 and use
    the directory watcher bundle to notice changes and update the
    bundles.txt file accordingly.
  - Might have to modify the directory watcher so it notices changes in
    the platform.xml file.

### Misc

  - update core APIs : install this feature
      - can we say we don't support this?

<!-- end list -->

  - have a server with p2 data and 3.3 style and someone installs a
    feature with the old update UI

<!-- end list -->

  - I have the SDK installed by p2 and I want to download a feature
    which requires the RCP but I don't know about the RCP feature
      - do we keep feature information around?
      - how do features appear in PDE?
      - how much tooling do we have for groups/features?

<!-- end list -->

  - do we delete the update.ui bundle from the SDK for 3.4?
      - if so, then we need to be able to handle old update sites, etc
        from the new ui

<!-- end list -->

  - can features and IUs have dependancies on each other?
  - what level of integration do we seek?
  - do we ever want to reconcilate 2 worlds?
  - can update manager just install things and we have a hook/listener
    in the update core?
  - maybe the directory watcher can update the bundles.txt for things
    which have been installed by the update manager apis
  - low bar: we should still be able to reconsume things from an update
    site
  - what if the platform configuration updated the bundles.txt?
  - old ui + features -\> install, update platform.xml -\> should we
    update the bundles.txt?
  - new ui + ius -\> install, update bundles.txt -\> should we update
    the platform.xml?
  - In Eclipse 3.4, we will always start based on the bundles.txt.

<!-- end list -->

  - I have an old UM site with features, how do I install them?
      - 1). old UM ui/core APIs
          - bundles.txt
          - platform.xml
      - 2). p2
          - generate metadata on the fly -\> look at Maya, effeciency?
          - install handlers - what do they have access to? life cycle?
            API?
          - platform.xml
          - still download the feature jars
          - PDE is feature-aware

<!-- end list -->

  - change update configurator to lay down a bundles.txt as well as the
    platform.xml
  - like we have config scripts to update the bundles.txt file, we
    should have scripts which update the platform.xml file
  - we need to ship features to handle the case where features on sites
    depend on it
  - also to handle people who are developing against previous code bases
  - features are listed only in the platform.xml, and the bundles are
    listed in both.
  - platform.xml is our backwards compat trick
  - need to maintain consistency between the 2 files
  - do we want metadata for things installed by update manager

<!-- end list -->

  - UM sites can be considered as a backwards compatibility thing. If
    you connect and try to install from one then you can expect extra
    work to happen. e.g. downloading the features and create the
    metadata on the fly
  - if we are only using p2 then everything is ok
  - UM areas
      - update site
          - install from p2
          - install from UM UI
      - UM APIs
          - install/uninstall from UM UI or UM core APIs from headless
            app
      - UM UI
          - Manage Configurations dialog

### Levels of Integration

#### Baseline 0

  - run a p2 provisioned SDK
  - install a feature from an update site
  - restart eclipse
  - new functionality should be in Eclipse
  - note: eclipse starts from a bundles.txt

### Notes

1). Synchronize the bundles.txt and platform.xml

  - can modify the directory watcher
  - does it need to be in both directions? or can we get away with only
    updating the bundles.txt?

2). Do we need p2 metadata for features from update sites?

  - can generate on the fly
  - Maya has something like that

3). Dependencies between p2 and features

  - if you have a p2 provisioned SDK and go to an UM site and want to
    download a feature which depends on the Platform feature, how do we
    get validation to pass?

4). Feature Install Handlers

  - how do these work?
  - what can they see?
  - can we map these to configuration data?

5). PDE deals with features

### The Configuration Watcher

The ConfigurationWatcher is essentially a class which leverages the
DirectoryWatcher mechanism and watches a specific file, the
platform.xml. It controls the creation of DirectoryWatchers for the
sites contained within a configuration. When the platform.xml file
changes, the ConfigurationWatcher reacts in the following ways:

  - ADDED - If a new site is added to the file, then a new watcher is
    created on that site
  - REMOVED - If a site is removed from the file, then the watcher on
    that site is disposed. Note that the director will have to be called
    here to un-install all the bundles that were in the site.
  - CHANGED - If a site has changed, then the watcher will have to have
    its filters changed to match the expectations in the platform.xml
    definition.

### Scenario - Install Feature via Update Manager (no dependencies)

  - Have a p2 provisioned Eclipse which contains old update manager code
  - Start Eclipse
  - The ConfigurationWatcher creates a DirectoryWatcher on the plugins
    and features directories in the Eclipse install, as well as a
    watcher on the platform.xml file.
  - Start up old Update Manager UI
  - Select a new feature to install, and install it
  - New feature and bundles are installed
      - DirectoryWatcher sees new feature and bundles
      - Director is called with the install command
      - The bundles.txt file is updated
  - The platform.xml file is updated
      - The ConfigurationWatcher sees the change in the platform.xml
      - If there is a new site added, then a new DirectoryWatcher is
        created to watch it
      - If there is a change in policy, bundle list, etc then the
        DirectoryWatcher and its filters are modified to match

### Scenario - Install Feature via Update Manager (Dependencies)

  - Have a p2 provisioned Eclipse which contains the old update manager
    code
  - Start Eclipse and open the Update Manager UI
  - We want to install a feature from the Update site which has a
    dependancy on a feature which we are supposed to have locally. (e.g.
    the RCP feature)
  - Since the build was provisioned by p2, we don't have features. This
    means that the update manager will fail when checking pre-conditions
    to ensure that all the dependancies are met before downloading the
    new feature
  - Solution: we will have to ship features. We will need to generate
    metadata for features and create IUs which have the feature
    artifacts so they are installed.

### Uninstall via Update Manager

I've been looking at how the old update manager code uninstalls features
and here is what happens:

  - in the Manage Configurations window you select your feature and hit
    "Uninstall"
  - it removes the feature from the list of features in the site in the
    platform.xml
  - (for a USER-EXCLUDE policy) it adds all the file paths for all of
    the feature's bundles to the site policy's plug-in list
  - it drops a file (configuration/org.eclipse.update/toBeUninstalled)
    which contains a list of all the feature names that will be
    uninstalled along with their corresponding site url from the
    platform.xml
  - on startup, the plug-ins are ignored because of their include in the
    plug-in list in the site policy
  - the update.scheduler bundle adds an extension to the UI's
    earlyStartup extension point which goes and looks for the
    toBeUninstalled file
  - the file is parsed and it is determined what needs to be removed
    from the file-system and deletion is done
  - platform.xml is updated (the files are no longer needed in the site
    policy's plug-in list)

[Category:Equinox p2](Category:Equinox_p2 "wikilink")