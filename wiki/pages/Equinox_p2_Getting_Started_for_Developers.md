So you are interested in the provisioning code. Great\! There are
several levels of involvement ranging from calling the API to
contributing provisioning code. This page should help you understand how
to do all of those. If you are actually looking to get started as an
end-user of the provisioning facilities then check out the [getting
started guide for users](Equinox_p2_Getting_Started "wikilink").

## Getting the code

**The p2 sources are being moved to Git (see
[1](https://bugs.eclipse.org/bugs/show_bug.cgi?id=345479)) – This
information is partially outdated\!**

1.  Get the most recent 3.7 build available
    (http://download.eclipse.org/eclipse/downloads/)
2.  Create a CVS repository location for
    ":pserver:anonymous@dev.eclipse.org:/cvsroot/rt". Hint: Select the
    quoted text, open the **CVS Repositories** view, and select **Paste
    Connection** or hit Ctrl+V to add the connection
3.  Expand **HEAD \> org.eclipse.equinox \> p2**.
4.  Checkout "org.eclipse.equinox.p2.releng". Should you need to work on
    a maintenance stream (e.g. 3.6.x), you can find a branch of this
    project.
5.  Import the "projectSet.psf" project set by right clicking on the
    file in the Package Explorer and clicking **Import Project Set...**
    in the releng project you just checked out. If you are a committer,
    and able to make an extssh connection, you can use
    projectSet-extssh.psf

You will get a mess of projects added to you workspace and you are "good
to go".

## Understanding the code

While it would be extremely hard to capture essence of all the different
code areas here, we can give you a few starting points and places to
look.

  - The [Equinox p2 Concepts](Equinox_p2_Concepts "wikilink") document
    sets out much of the terminology for and relationships between the
    different elements of the Equinox provisioning system.
  - ProvisioningHelper in the console bundle. This is a helper class
    that has lots of useful methods and is useful as an example of how
    to do various operations. Note that this class is currently not API
    and is sadly misplaced to be widely useful (in the console
    bundle?\!), but overall, it is very educational.
  - Also it worth nothing that the code you check out contains two kinds
    of bundles. First are the runtime bundles (you will deploy a subset
    of those when you want to use p2 as a provisioning system, e.g.
    engine, director, etc.), second are the bundles related to tooling
    (e.g. publisher, metadata.generator, repo tools, etc.).

## Self hosting

  - Since 3.6 (Helios) PDE has introduced limited support for
    self-hosting p2. This means that on startup PDE will generate a
    profile from the set of bundles that you have selected. To enable
    this functionality, select the "Support software installation"
    option in the configuration tab of the launch configuration. Note
    though that this has some limitations and may not represent the
    reality of your deployed application to a 100%.

<!-- end list -->

  - Another approach, useful when you want to work against an existing
    p2 profile, is to set the p2 data area of the launched workbench to
    point to the p2 directory of the profile that you want to debug.
    These arguments can be added to the launch configuration of your
    workbench. For example, if the eclipse install you wish to work with
    is in "c:/testBuild" then your launch config arguments look like
    this:

<!-- end list -->

    -Declipse.p2.data.area=C:/testBuild/eclipse/p2/
    -Declipse.p2.profile=SDKProfile

It's not such a good idea to point the launch config at the host
eclipse, since you could potentially trash your host profile while
debugging new code.

## My first run

Note that there are also a bunch of launch configurations that come in
the various projects. We can't explain them all here but looking at the
launch configs (and the code they run) is one interesting starting
point. To get a feel for how things work the section below walks you
through using a few of the launch configs to setup a working
provisioning system.

1.  **director app** This allows you to run the director application,
    which is a headless way to install and uninstall using p2.
    <http://help.eclipse.org/ganymede/index.jsp?topic=/org.eclipse.platform.doc.isv/guide/p2_director.html>

<!-- end list -->

1.  **Publisher \*** This allows you to generate p2 metadata from
    various input

[Getting Started for Developers](Category:Equinox_p2 "wikilink")