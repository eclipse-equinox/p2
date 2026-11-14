Starting in Ganymede M6 (Eclipse 3.4.0), p2 Equinox has been introduced
for feature and plug-in installation. The Eclipse platform has been
changed to utilize this new component [Cross Project
Topic](http://dev.eclipse.org/mhonarc/lists/cross-project-issues-dev/msg02165.html).
This page will detail how to install TPTP based on changes made for p2.

To install using the dropins folder, similar to expanding a zip to the
eclipse directory in pre-3.4.0 M6 (when using the TPTP all-in-one, no
action is required unless you want to add additional dependencies):

1.  Download and extract the Eclipse SDK to an installation directory.
2.  Download TPTP and any dependencies and extract them to
    <installation directory>/dropins. The eclipse/plugins directory
    structure can be maintained in the dropins folder.
3.  Launch the Eclipse SDK.
4.  Wait for installation to complete and begin using the workbench.

Installation using the Software Updates menu entry:

1.  Download and extract the Eclipse SDK to an installation directory.
2.  Download TPTP and any dependencies and extract them to a directory
    (when using an update site, skip this step).
3.  Launch the Eclipse SDK.
4.  From the Help menu, select Software Updates...
5.  Switch to the Available Software Tab.
6.  Click the Manage sites... button
7.  Click the Add... button
8.  Enter the update site and use the Local... button to point to an
    expanded driver's directory. Complete this once for each driver
    required if they are stored separately. Click OK when done to close
    the Update Sites window.
9.  Expand the Added site in the Available Software tab and select the
    features to install (all of them excluding the category for the
    all-in-one using ctrl-a).
10. Click the Install... button and it will verify the dependencies.
11. Click next on the install page, review the license agreement and
    select Finish.
12. Wait for installation to complete and restart the workbench when
    prompted.

Current active bugs for p2 being tracked:

[`Unable``   ``to``   ``drop-in``   ``more``   ``recent``   ``version``
 ``of``   ``bundle``   ``already``   ``in``   ``my``
 ``install.`](https://bugs.eclipse.org/bugs/show_bug.cgi?id=222945)

For more information on P2 Equinox:

  - [Equinox P2 Getting
    Started](http://wiki.eclipse.org/Equinox_p2_Getting_Started)
  - [P2 Equinox WIKI](http://wiki.eclipse.org/Category:Equinox_p2)