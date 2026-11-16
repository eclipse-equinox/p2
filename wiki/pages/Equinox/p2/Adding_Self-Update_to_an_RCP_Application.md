This page consolidates information about how to add p2 self-updating
support to your RCP application. The p2 API has been released in Eclipse
3.6. This page is under construction as the examples are adapted to the
new API.

See [Equinox/p2/Adding Self Update to an RCP
Application-3.5](Equinox/p2/Adding_Self_Update_to_an_RCP_Application-3.5 "wikilink")
for the 3.5 version of this page.

We start with the RCP Mail example application and modify it in
different ways.

## Adding p2, building, and provisioning your application with p2

The process for setting up your application to update with p2 involves
several steps.

  - The p2 bundles must be added to your application. This can be
    expressed by including the p2 bundles in one of your product's
    features (using the feature **org.eclipse.equinox.p2.user.ui** as a
    guide). It can also be done by expressing requirements in your
    features and letting the p2 install of your application pull in the
    right code.
  - Set up a build for your product. This includes
      - installing the delta pack for headless builds
      - specifying the normal build steps in your build.properties
      - adding some p2-specific steps to the build
          - export the metadata for your build into a p2 repository
          - run the p2 director to provision the application

This process is described in more detail in [Andrew Niefer's blog
post](http://aniefer.blogspot.com/2009/03/building-p2-rcp-products-in-eclipse.html)
on the topic.

An example build based on a modified RCP Mail application can be found
[here](http://git.eclipse.org/c/equinox/rt.equinox.p2.git/tree/examples/org.eclipse.equinox.p2.examples.rcp.cloud.releng).

Ensure that you have the correct start levels and auto-start flags set
for the bundles required to run the p2 processes (see [this
discussion](http://www.eclipse.org/forums/index.php?t=msg&th=171233&S=de07466533bdf05f45fbbdcce18d2bec#msg_544484)).
In the product definition, include

    <configurations>
    <plugin id="<my product>" autoStart="false" startLevel="5" />
    <plugin id="org.eclipse.core.runtime" autoStart="true" startLevel="4" />
    <plugin id="org.eclipse.equinox.common" autoStart="true" startLevel="2" />
    <plugin id="org.eclipse.equinox.ds" autoStart="true" startLevel="2" />
    <plugin id="org.eclipse.equinox.simpleconfigurator" autoStart="true" startLevel="1" />
    </configurations>

## Configuring the user's default repositories

The repositories that should initially be present in the application can
be controlled using touchpoint instructions. The *addRepository*
instruction should be used for each repository. In the p2 UI, we assume
"colocated" metadata and artifact repositories. When the user adds a
repository in the UI, both a metadata and an artifact repository are
added for that location. When using touchpoint instructions, both the
metadata and artifact repository must be added.

These instructions can be specified using a touchpoint advice file
(p2.inf) that is included with the application. See [the online
help](http://help.eclipse.org/helios/index.jsp?topic=/org.eclipse.platform.doc.isv/guide/p2_actions_touchpoints.html)
for more details.

Here is a [sample
p2.inf](https://git.eclipse.org/c/equinox/rt.equinox.p2.git/tree/examples/org.eclipse.equinox.p2.examples.rcp.sdkui/p2.inf)
file used in one of the p2 RCP Mail examples.

### Alternate approach

If the RCP application will be installed into a user-write protected
directory, p2.inf will fail to be able to add the repositories (as it
tries to modify the configuration on the first RCP run). One workaround
is to programmatically set the list of allowed repositories using
org.eclipse.equinox.internal.p2.ui.model.ElementUtils.updateRepositoryUsingElements
[(source)](http://coopology.com/2012/08/eclipse-rcp-setting-p2-repositories-update-sites-programmatically-for-when-p2-inf-fails).

## Configuring the p2 UI

There are several different levels of integration with the p2 UI,
depending on what kind of support you want to surface to your users.

### Reusing the Eclipse SDK UI in its entirety

If your goal is to simply use the same update UI used in the SDK inside
your RCP app, very few modifications are required. A sample RCP Mail
application which shows how to do this can be found
[here](http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.equinox/p2/examples/org.eclipse.equinox.p2.examples.rcp.sdkui/?root=RT_Project).
You can view the changes that were required to the standard RCP Mail
application by looking for 'XXX' task tags in the code.

![Image:RCPp2sdkui.jpg](images/RCPp2sdkui.jpg "Image:RCPp2sdkui.jpg")

You'll need to include the org.eclipse.equinox.p2.user.ui feature in
your application. This will add the following UI bundles to your
application (in addition to all of the p2 core and other required
bundles):

  - org.eclipse.equinox.p2.ui
  - org.eclipse.equinox.p2.ui.sdk
  - org.eclipse.equinox.p2.ui.sdk.scheduler

To use the p2 SDK UI bundles unmodified, you will need to ensure you
provide the same UI services expected by the p2 UI. This includes:

  - **ApplicationWorkbenchWindowAdvisor** should define a status line
    and progress area so that the p2 automatic update status and
    progress reporting will be shown.

<!-- end list -->

```
    public void preWindowOpen() {
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        configurer.setInitialSize(new Point(600, 400));
        configurer.setShowCoolBar(true);

        // XXX We set the status line and progress indicator so that update
        // information can be shown there
        configurer.setShowStatusLine(true);
        configurer.setShowProgressIndicator(true);
    }
```

  - **ApplicationActionBarAdvisor** should ensure that there is a
    **Help** menu on the menu bar and that it defines an **additions**
    group where the p2 contributions can me made. In order to make the
    update preferences available to the user, there must also be access
    to the preferences from some menu.

<!-- end list -->

```
    protected void fillMenuBar(IMenuManager menuBar) {
        MenuManager fileMenu = new MenuManager("&File", IWorkbenchActionConstants.M_FILE);
        // XXX Window menu
        MenuManager windowMenu = new MenuManager("&Window", IWorkbenchActionConstants.M_WINDOW);
        MenuManager helpMenu = new MenuManager("&Help", IWorkbenchActionConstants.M_HELP);

        menuBar.add(fileMenu);

        // XXX Window menu
        menuBar.add(windowMenu);

        ...

        // XXX Window menu
        windowMenu.add(preferencesAction);

        // Help
        // XXX add an additions group because this is what SDK UI expects
        helpMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        helpMenu.add(new Separator());
        helpMenu.add(aboutAction);
    }
```

Since the RCP Mail application does not define preferences of its own,
the resulting preference dialog will only show the preferences
contributed by p2 and its required bundles.

![Image:RCPp2sdkprefs.jpg](images/RCPp2sdkprefs.jpg "Image:RCPp2sdkprefs.jpg")

### Reusing the Eclipse SDK UI without automatic updating

If you want to use the SDK UI, but do not wish to add automatic update
support, the application is modified as above. However, the following
bundles can be eliminated from the product:

  - org.eclipse.equinox.p2.ui.sdk.scheduler
  - org.eclipse.equinox.p2.updatechecker

A sample RCP Mail application which shows how to do this can be found
[here](https://git.eclipse.org/c/equinox/rt.equinox.p2.git/tree/examples/org.eclipse.equinox.p2.examples.rcp.sdknoautoupdates).

In this case, the application looks mostly the same, except that there
are no preferences shown for automatic updating.

![Image:RCPp2noautoupdateprefs.jpg](images/RCPp2noautoupdateprefs.jpg
"Image:RCPp2noautoupdateprefs.jpg")

### Modifying the UI contributions

If you want to include p2 update functionality in the UI, but you don't
want these items to appear in exactly the same way as they do in the
SDK, you can provide your own bundle that makes p2 UI contributions in
lieu of the SDK bundle. This allows you to simply rearrange the way
users encounter the install/update functionality, or provide more
precise control of what can be done by the user. For example, you may
wish to expose a subset of functionality, such as permitting updating,
but not allowing the user to uninstall, install, or revert
configurations. Or you may wish to change what is visible in the
installed software page, or what software is visible when browsing a
site. Or you may wish to limit the sites that the user can contact.

We'll look at several specific examples, but the general structure of
the application is as follows:

  - Decide which p2 UI bundles to include in your product
      - Always include **org.eclipse.equinox.p2.ui**
      - If you wish to use automatic update checking, include
        **org.eclipse.equinox.p2.updatechecker**
      - If you wish to reuse the SDK UI for automatic update checking
        (the same pref page, popup, etc.), then you can also include
        **org.eclipse.equinox.p2.ui.sdk.scheduler**

Your application must replace the functionality provided by
**org.eclipse.equinox.p2.ui.sdk** according to what functionality is
needed. Use the sdk bundle as a model. Things you need to think about:

  - Consider whether you will be showing update status and progress as
    shown in the SDK UI. If so, then you'll want to make sure the status
    and progress indicators are shown in your workbench window as done
    in the previous examples.
  - Your bundle will probably need to configure a default instance of
    **org.eclipse.equinox.p2.ui.policy.Policy** which controls different
    aspects of the the UI. This can be done by simply configuring a
    policy instance as appropriate for your application, and registering
    it as an OSGi service. Depending on the application, the policy
    instance may be simply configured in code, or some of the decisions
    may be exposed to the user in a preference page. The policy allows
    you to control things such as
      - whether repositories (sites) are visible to the user, and
        whether the user is permitted to manipulate (add, enable,
        disable, remove) the sites that are used for install and update
      - what software (installable units) is visible to the user when
        browsing software sites
      - what software (installable units) is shown as the roots of the
        'Installed Software' page
      - whether restart is required after updating an application
  - If you are exposing user preferences, and you are including the
    SDK's automatic update support (and pref page), then you probably
    want the automatic update preferences to appear underneath your
    application's update preferences. If so, then you'll want to use the
    same preference page ID as used by **org.eclipse.equinox.p2.ui.sdk**
    so that automatic update pref page contribution falls underneath it.
  - The IHandler classes in **org.eclipse.equinox.p2.ui.sdk** invoke the
    update and install function. If you are simply rearranging the menus
    in your application, you can copy these handler classes and command
    contributions to your bundle and simply revise the menu
    contributions as you wish. Or you can invoke the UI in a completely
    different way.
  - The **org.eclipse.ui.about.installationPages** contributions made by
    the SDK UI provide access to update functionality. Consider
    replacing or modifying the installation page contributions if some
    of the actions are not relevant.
      - The **Installed Software** page shows the installed content and
        provides buttons for updating and uninstalling software.
      - The **Installation History** page shows the previous
        installation configurations and provides buttons for reverting
        to previous configurations.

The following examples show specific examples of this kind of
modification.

#### Updating from the Cloud

A common scenario in RCP applications is allowing the user to perform
update or install operations, but not permitting the user to control
which sites the updates or new software come from. This is common in
managed installations, where a systems administrator is maintaining an
internal update site. From the user point of view, updates come from one
"cloud" rather than individual software sites, and there is no
visibility of software sites.

A sample RCP Mail application which shows how to do this can be found
[here](https://git.eclipse.org/c/equinox/rt.equinox.p2.git/tree/examples/org.eclipse.equinox.p2.examples.rcp.cloud).
As before, you can view the changes that were required to the standard
RCP Mail application by looking for 'XXX' task tags in the code.

![Image:RCPp2cloud.jpg](images/RCPp2cloud.jpg "Image:RCPp2cloud.jpg")

In this example, we want the UI to behave in the following ways:

  - we group the application preferences and update menu items in a
    **Tools** menu
  - we want to contribute the standard installation pages (allowing
    access to uninstall, update, and revert).
  - we want to allow automatic updating using the standard preference
    page
  - we do not want to expose any site management function or site
    preferences
  - we do not want to expose the standard SDK update preferences, and
    instead use our own values

First, we'll look at the changes required in the standard RCP mail code.

  - As before, **ApplicationWorkbenchWindowAdvisor** should define a
    status line and progress area so that the p2 automatic update status
    and progress reporting will be shown.

<!-- end list -->

```
    public void preWindowOpen() {
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        configurer.setInitialSize(new Point(600, 400));
        configurer.setShowCoolBar(true);

        // XXX We set the status line and progress indicator so that update
        // information can be shown there
        configurer.setShowStatusLine(true);
        configurer.setShowProgressIndicator(true);
    }
```

  - **ApplicationActionBarAdvisor** defines the tools menu, as well as a
    group marker for making additions to it. We also contribute the
    **About** action so that the installation pages can be shown.

<!-- end list -->

```
    protected void fillMenuBar(IMenuManager menuBar) {
        MenuManager fileMenu = new MenuManager("&File", IWorkbenchActionConstants.M_FILE);
        // XXX add a tools menu
        MenuManager toolsMenu = new MenuManager("&Tools", M_TOOLS);
        MenuManager helpMenu = new MenuManager("&Help", IWorkbenchActionConstants.M_HELP);

        menuBar.add(fileMenu);
        menuBar.add(toolsMenu);

        ...

        // XXX add preferences to tools
        toolsMenu.add(preferencesAction);
        // XXX add a group for new other tools contributions
        toolsMenu.add(new Separator());
        toolsMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

        // Help
        helpMenu.add(aboutAction);
    }
```

We need to configure the p2 UI **Policy** according to our requirements
and register it. We are not going to define a new bundle for our p2 UI
contributions, so we'll register our policy in the startup code of the
existing bundle class.

  - **Activator** registers the UI policy in its startup code.

<!-- end list -->

```
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        // XXX register the p2 UI policy
        registerP2Policy(context);
        getPreferenceStore().addPropertyChangeListener(getPreferenceListener());
    }
```

  - The registration method creates a default policy instance, updates
    it according to some preference values, and registers it.

<!-- end list -->

```
    private void registerP2Policy(BundleContext context) {
        policy = new CloudPolicy();
        policy.updateForPreferences();
        policyRegistration = context.registerService(Policy.class.getName(), policy, null);
    }
```

  - The **CloudPolicy** is defined in a new package,
    **org.eclipse.equinox.p2.examples.cloud.p2**. In this example, we
    wish to initialize a policy instance that prevents the user from
    manipulating the repositories. There are several ways to go about
    this. The most direct way is that the policy initializes its desired
    values on construction.

<!-- end list -->

    public class CloudPolicy extends Policy {
        public CloudPolicy() {
            // XXX User has no visibility for repos
            setRepositoriesVisible(false);
        }
    }

A more general approach is for the policy to derive its values from
preference settings. These preferences are not exposed to the end user,
but are used to control how the policy initializes itself. This is the
approach taken in the cloud example. The advantage of such an approach
is that the policy can be configured by altering the preference values
in the **plugin_customization.ini** file for the application.

The policy code simply reads preference settings and adjusts the policy
accordingly.

```
    public void updateForPreferences() {
        IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
        setRepositoriesVisible(prefs
                .getBoolean(PreferenceConstants.REPOSITORIES_VISIBLE));
        setRestartPolicy(prefs.getInt(PreferenceConstants.RESTART_POLICY));
        setShowLatestVersionsOnly(prefs
                .getBoolean(PreferenceConstants.SHOW_LATEST_VERSION_ONLY));
        setGroupByCategory(prefs
                .getBoolean(PreferenceConstants.AVAILABLE_GROUP_BY_CATEGORY));
        setShowDrilldownRequirements(prefs
                .getBoolean(PreferenceConstants.SHOW_DRILLDOWN_REQUIREMENTS));
        if (prefs.getBoolean(PreferenceConstants.AVAILABLE_SHOW_ALL_BUNDLES))
            setVisibleAvailableIUQuery(QueryUtil.ALL_UNITS);
        else
            setVisibleAvailableIUQuery(QueryUtil.createIUGroupQuery());
        if (prefs.getBoolean(PreferenceConstants.INSTALLED_SHOW_ALL_BUNDLES))
            setVisibleInstalledIUQuery(QueryUtil.ALL_UNITS);
        else
            setVisibleInstalledIUQuery(new UserVisibleRootQuery());

    }
```

Now, the **plugin_customization.ini** file can be edited according to
the desired policy. A sample file that explains all of the configurable
aspects of the policy is included in the example.

    # we can configure the update UI by using application preferences to initialize the default UI policy

    # should user be able to see and manipulate repositories in the install wizard
    org.eclipse.equinox.p2.examples.rcp.cloud/repositoriesVisible=false

    # force restart after a provisioning operation (see possible values in org.eclipse.equinox.p2.ui.Policy.restartPolicy())
    org.eclipse.equinox.p2.examples.rcp.cloud/restartPolicy=1

    # show only latest versions when browsing for updates
    org.eclipse.equinox.p2.examples.rcp.cloud/showLatestVersionOnly=true

    # software should be grouped by category by default
    org.eclipse.equinox.p2.examples.rcp.cloud/groupByCategory=true

    # show only groups (features) in the available list, not every bundle
    org.eclipse.equinox.p2.examples.rcp.cloud/showAllBundlesAvailable=false

    # show only the install roots in the installed software list
    org.eclipse.equinox.p2.examples.rcp.cloud/showAllBundlesInstalled=false

    # do not drilldown into requirements in the wizards, just show the high level things
    org.eclipse.equinox.p2.examples.rcp.cloud/showDrilldownRequirements=false

    # automatic update options are defined in org.eclipse.equinox.p2.sdk.scheduler.PreferenceConstants

    # check for updates on startup
    org.eclipse.equinox.p2.ui.sdk.scheduler/enabled=true
    org.eclipse.equinox.p2.ui.sdk.scheduler/schedule=on-startup

    # remind the user every 4 hours
    org.eclipse.equinox.p2.ui.sdk.scheduler/remindOnSchedule=true
    # see AutomaticUpdatesPopup, values can be "30 minutes", "Hour", "4 Hours"
    org.eclipse.equinox.p2.ui.sdk.scheduler/remindElapsedTime=4 Hours

    # download updates before notifying the user
    org.eclipse.equinox.p2.ui.sdk.scheduler/download=true

The rest of the **org.eclipse.equinox.p2.examples.cloud.p2** package is
based on code from **org.eclipse.equinox.p2.ui.sdk**.

  - We copy the command handlers for install and update
      - InstallNewSoftwareHandler
      - PreloadingRepositoryHandler
      - UpdateHandler
  - We choose not to copy any preference pages
  - We copy and modify the externalized strings to include only those
    that we need. We also rename the message class.
      - Messages
      - messages.properties

Finally, we modify the **plugin.xml** generated for RCP Mail in the
following ways.

  - We add contributions for the standard installation pages. The page
    implementations are contained in the p2 class library, so we only
    need to define the names of the pages and point to the existing
    implementations.

<!-- end list -->

```
      <extension
         point="org.eclipse.ui.installationPages">
          <page
            name="%installedSoftwarePage"
            class="org.eclipse.equinox.p2.ui.InstalledSoftwarePage"
            id="10.org.eclipse.equinox.p2.examples.rcp.cloud.InstalledSoftwarePage">
          </page>
          <page
            name="%installHistoryPage"
            class="org.eclipse.equinox.p2.ui.RevertProfilePage"
            id="11.org.eclipse.equinox.p2.examples.rcp.cloud.RevertProfilePage">
          </page>
    </extension>
```

  - We define the commands and handlers for install and update,
    referring to our copied handler classes.

<!-- end list -->

```
   <extension
         point="org.eclipse.ui.commands">
      <command
            name="%Update.command"
            id="org.eclipse.equinox.p2.examples.rcp.cloud.command.update">
      </command>
      <command
            name="%Install.command"
            id="org.eclipse.equinox.p2.examples.rcp.cloud.command.install">
      </command>
   </extension>
   <extension
         point="org.eclipse.ui.handlers">
      <handler
            commandId="org.eclipse.equinox.p2.examples.rcp.cloud.command.update"
            class="org.eclipse.equinox.p2.examples.rcp.cloud.p2.UpdateHandler">
      </handler>
      <handler
            commandId="org.eclipse.equinox.p2.examples.rcp.cloud.command.install"
            class="org.eclipse.equinox.p2.examples.rcp.cloud.p2.InstallNewSoftwareHandler">
      </handler>
   </extension>
```

  - We contribute our commands to the tools menu we defined earlier.

<!-- end list -->

```
   <extension
         point="org.eclipse.ui.menus">
      <menuContribution
            locationURI="menu:tools?after=additions">
            <command
                  commandId="org.eclipse.equinox.p2.examples.rcp.cloud.command.update"
                  mnemonic="%Update.command.mnemonic"
                  id="org.eclipse.equinox.p2.examples.rcp.cloud.menu.update">
            </command>

      </menuContribution>
      <menuContribution
            locationURI="menu:tools?after=org.eclipse.equinox.p2.examples.rcp.cloud.menu.update">
            <command
                  commandId="org.eclipse.equinox.p2.examples.rcp.cloud.command.install"
                  mnemonic="%Install.command.mnemonic"
                  id="org.eclipse.equinox.p2.examples.rcp.cloud.menu.install">
            </command>
      </menuContribution>
   </extension>
```

The most significant change is that the **Install New Software...**
wizard no longer provides any control over which sites are shown. Only
the content from sites preconfigured in the product (using the
**p2.inf** file in our example) are shown.

![Image:RCPp2cloudinstall.jpg](images/RCPp2cloudinstall.jpg
"Image:RCPp2cloudinstall.jpg")

Since we didn't contribute any preferences, only those provided by the
included bundles are shown. Most notably, the preferences for the
software sites do not appear.

![Image:RCPp2cloudprefs.jpg](images/RCPp2cloudprefs.jpg
"Image:RCPp2cloudprefs.jpg")

The resulting application provides access to the standard installation
pages:

![Image:RCPp2cloudinstallpages.jpg](images/RCPp2cloudinstallpages.jpg
"Image:RCPp2cloudinstallpages.jpg")

Any further restriction of the user actions could be based on this
example.

  - We could contribute only an update command, (or only an install
    command,) so that only one menu item was available.
  - We could remove the **Installation History** installation page
    contribution if the user should not be able to revert.
  - We could remove the **Installed Software** page if the user should
    not be permitted to uninstall or update individual items. (If we
    wanted to retain the installation page but not permit uninstall or
    update on the page, we would have to subclass
    **InstalledSoftwarePage** and override **createPageButtons**).

#### Changing the Visibility of Available and Installed Content

The Eclipse SDK and the examples shown so far use the default p2 UI
**Policy** values that control the visibility of software items. Special
properties are used to determine which software items are shown in the
UI.

In practice, this means that only Eclipse features are shown when
browsing the various update sites. However, p2 does not have any
specific knowledge of Eclipse features, nor does it require that
installation and update operations be based on features. Similarly, only
items actually installed by the end user, (or defined as installed items
at product-build time), are shown in the **Installed Software** page.
This is done to simplify the view of the installation.

The visibility of the available and installed items can be modified.
Applications can define which queries should be used to obtain the list
of available content and the list of installed content. In this example,
we'll replace the filtered queries normally used by the Eclipse SDK with
queries that show everything available. (We'll also show how this can be
achieved by altering the preference settings in the Cloud example. The
approach taken in this example is shown in order to demonstrate how the
queries can be replaced with application-defined queries. For example,
the query could be modified to show only IU's with a certain property,
or only those whose ids are associated with the application).

A sample RCP Mail application which sets up custom queries in the UI
policy can be found
[here](https://git.eclipse.org/c/equinox/rt.equinox.p2.git/tree/examples/org.eclipse.equinox.p2.examples.rcp.sdkbundlevisibility/).
As usual, you can view the changes that were required to the standard
RCP Mail application by looking for 'XXX' task tags in the code.

When installing new software, every available bundle is shown in the
install wizard. (Note that you must uncheck the **Group items by
category** checkbox to see this list.)

![Image:RCPp2bundlevisibility.jpg](images/RCPp2bundlevisibility.jpg
"Image:RCPp2bundlevisibility.jpg")

The **Installed Software** page shows every bundle in the installation,
not just the "root" of the product and the user-installed items.

![Image:RCPp2bundlevisibilityinstall.jpg](images/RCPp2bundlevisibilityinstall.jpg
"Image:RCPp2bundlevisibilityinstall.jpg")

The steps for building the contributions are similar to those for the
Cloud example above. Rather than cover each change in detail, we'll only
look at the policy class, this time called **AllIUsAreVisiblePolicy**.

    public class AllIUsAreVisiblePolicy extends Policy {
        public AllIUsAreVisiblePolicy() {
            // XXX Use the pref-based repository manipulator
            setRepositoryPreferencePageId(PreferenceConstants.PREF_PAGE_SITES);

            // XXX All available IU's should be shown, not just groups/features
            setVisibleAvailableIUQuery(InstallableUnitQuery.ANY);
            // XXX All installed IU's should be shown, not just the user-installed.
            setVisibleInstalledIUQuery(InstallableUnitQuery.ANY);
        }
    }

This example uses OSGi declarative services to register the policy.
Rather than manually register the service when our example Activator
starts, we instead declare the service in a **policy_component.xml**
file. Using declarative services is not necessary in this particular
example, but could become necessary if we were to separate our p2 UI
contributions into another bundle. In that case, it becomes possible for
p2 UI components that use the policy (the preference page or
installation pages) to be invoked before the bundle that configures the
policy starts. Declarative services ensures that the policy is found and
the bundle starts when the service is needed.

As mentioned previously, the Cloud example preferences may also be used
to make everything visible in the UI. This can be done by editing the
**plugin_customization.ini** file in the example.

    # show only groups (features) in the available list, not every bundle
    org.eclipse.equinox.p2.examples.rcp.cloud/showAllBundlesAvailable=false

    # show only the install roots in the installed software list
    org.eclipse.equinox.p2.examples.rcp.cloud/showAllBundlesInstalled=false

#### Reassembling the UI

Some of the pages used in the p2 UI are defined as API so that
applications can reassemble the UI by contributing these pages wherever
desired.

  - **AcceptLicensesWizardPage** can be included in a custom wizard to
    handle the license checking.
  - **InstalledSoftwarePage** shows the installed content. It is
    currently contributed as an installation page in the about dialog,
    but it could be moved to its own dialog.
  - **RepositoryManipulationPage** shows the various repositories and
    provides buttons for adding, removing, importing, exporting, etc.
    The SDK contributes this page as a pref page, but the page could
    also be hosted in a different container, such as a TitleAreaDialog.
    See the javadoc for RepositoryManipulationPage for an example of how
    to do this.
  - **RevertProfilePage** shows the installation history and allows the
    user to revert to a previous configuration. It is currently
    contributed as an installation page in the about dialog, but it
    could be moved to its own dialog.

When this kind of reassembly is undertaken, it is important to remember
that some of the contributions (such as the repository preference page)
may be invoked before any of your custom UI code is invoked. It is
important to ensure that your application's p2 UI Policy is registered
before this happens. This can be done using declarative services, as in
the previous example, or by ensuring that the bundle that registers the
policy is auto-started in the application's config.ini.

### Modifying the p2 UI Policy while reusing the p2 UI feature

Because the p2 UI Policy is defined as an OSGi service, products that
ship with the **org.eclipse.equinox.p2.user.ui** feature unmodified can
still provide an alternate implementation of the UI Policy. The
**org.eclipse.equinox.p2.ui.sdk** bundle declares the service with a
default ranking value (0). This means that the product must supply a
policy implementation with a higher service ranking. When the policy
service is found, the highest ranking policy will win.

A sample file that declares a policy with a ranking of 1000 can be found
[here](https://git.eclipse.org/c/equinox/rt.equinox.p2.git/tree/examples/org.eclipse.equinox.p2.examples.rcp.sdkbundlevisibility/OSGI-INF/policy_component.xml).

## Headless Updating on Startup

Sometimes the simplest UI is no UI. In a highly-managed product
installation, it may be desirable to automatically update the
application each time it is started, with no intervention from the user.

A sample RCP Mail application which shows how to do this can be found
[here](https://git.eclipse.org/c/equinox/rt.equinox.p2.git/tree/examples/org.eclipse.equinox.p2.examples.rcp.prestartupdate).

The update is not truly "headless," as a progress indicator is shown
while searching for updates. The user may cancel the update, but
otherwise cannot intervene with the update. If no updates are found, the
user is notified.

![Image:RCPp2prestartupdate.jpg](images/RCPp2prestartupdate.jpg
"Image:RCPp2prestartupdate.jpg")

In this configuration, the p2 UI class libraries bundle
(**org.eclipse.equinox.p2.ui**) is not needed at all. Only the p2 core
code is used to achieve the update.

  - To ensure that all the p2 services are available at startup time,
    the example bundle and the **org.eclipse.equinox.ds** bundle must be
    started when the application is launched. This can be specified in a
    number of ways, depending on how you are running the example:
      - set the bundle start level in the launch configuration
      - set the bundle start level in the Configuration tab of the
        product editor (the .product file for your build) (example
        releng project needed...)
      - force a start of the bundle in the config.ini of the already
        built app
  - **ApplicationWorkbenchWindowAdvisor** must define a
    **postWindowOpen()** method, which sets up the progress monitoring,
    invokes the update search, and handles any errors or notifications.
    It uses a preference to remember if it is restarting after an
    update, so that the update search is not performed immediately after
    updating.
  - The update check method itself is rather simple, because it does not
    attempt to involve the user in making any choices about the updates.
    It uses the p2 operations API (new in Eclipse 3.6) to retrieve the
    updates and construct the provisioning plan.

<!-- end list -->

    public class P2Util {
        // XXX Check for updates to this application and return a status.
        static IStatus checkForUpdates(IProvisioningAgent agent, IProgressMonitor monitor) throws OperationCanceledException {
            ProvisioningSession session = new ProvisioningSession(agent);
            // the default update operation looks for updates to the currently
            // running profile, using the default profile root marker. To change
            // which installable units are being updated, use the more detailed
            // constructors.
            UpdateOperation operation = new UpdateOperation(session);
            SubMonitor sub = SubMonitor.convert(monitor,
                    "Checking for application updates...", 200);
            IStatus status = operation.resolveModal(sub.newChild(100));
            if (status.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE) {
                return status;
            }
            if (status.getSeverity() == IStatus.CANCEL)
                throw new OperationCanceledException();

            if (status.getSeverity() != IStatus.ERROR) {
                // More complex status handling might include showing the user what updates
                // are available if there are multiples, differentiating patches vs. updates, etc.
                // In this example, we simply update as suggested by the operation.
                ProvisioningJob job = operation.getProvisioningJob(null);
                status = job.runModal(sub.newChild(100));
                if (status.getSeverity() == IStatus.CANCEL)
                    throw new OperationCanceledException();
            }
            return status;
        }
    }

## Helpful Links

  - [Bug 237537](https://bugs.eclipse.org/bugs/show_bug.cgi?id=237537) -
    Bug containing an example RCP Product Build
  - [Andrew Niefer's blog post about RCP p2
    builds](http://aniefer.blogspot.com/2009/03/building-p2-rcp-products-in-eclipse.html)
  - [Eclipse RCP PDE p2 builds - Tutorial from Lars
    Vogel](http://www.vogella.de/articles/EclipsePDEBuild/article.html)
  - [Tutorial: p2 updates for Eclipse RCP
    applications](http://www.ralfebert.de/blog/eclipsercp/p2_updates_tutorial/)
  - [Tutorial: Eclipse RCP Update with p2 - Tutorial from Lars
    Vogel](http://www.vogella.de/articles/EclipseP2Update/article.html)

[Adding Self-Update to an RCP
Application](Category:Equinox_p2 "wikilink")