**Note:** *Although you are able to remove p2 from Eclipse SDK 3.4.x
builds, it cannot be removed from 3.5.x builds due to increased
integration with other components. In the 3.5 build cycle many
components in the SDK have changed their dependencies to be on p2 and to
call p2 API, rather than rely on the old Update Manager code, in
preparation for removal of the Update Manager completely in a future
release.*

Eclipse platform Ganymede/3.4 contains two provisioning systems: the
original Update Manager, and the new [Equinox p2](Equinox_p2 "wikilink")
provisioning platform. By default, p2 is in control of the system and
the Update Manager is hidden. This page describes how to revert to the
classic Update Manager and remove p2.

1.  **Scripts available in  automate this process** (tested on Linux and
    Cygwin).
2.  For manual removal, the following should be deleted:
      - eclipse/plugins/org.eclipse.\*.p2\*
      - eclipse/features/\*.p2\*
      - eclipse/plugins/org.eclipse.ecf\*
      - eclipse/plugins/\*frameworkadmin\*
      - eclipse/plugins/\*sat4j\*
      - eclipse/plugins/\*simpleconfigurator.manipulator\*
      - eclipse/dropins\*
      - eclipse/p2\*
      - eclipse/configuration/\*
3.  Copy **eclipse/configuration/config.ini** from a 3.3.2 version of
    the platform into this release
      - In the config.ini file, replace the "eclipse.buildId" setting
        with the correct build id (optional)
4.  Replace the **eclipse/eclipse.ini** file with the contents from
    Eclipse platform version 3.3.2.

Note an issue existed with removing the source plugins and features
pre-3.4M7, see .

Note that an issue was observed with I20080422-0800 and P2 removed, see
.

[Removal](Category:Equinox_p2 "wikilink")