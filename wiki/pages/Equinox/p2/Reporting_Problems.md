## Reporting network connection issues

Connectivity issues are some of the trickiest to report and to track
down for us as they often involve some particular network settings that
only you have access to. To help us figure out what is going on the
following information is necessary:

  - Version of Eclipse (build ID available in Help\>About)
  - OS
  - The URL/URI of the repository you are connecting to and the type of
    operation you are trying to do (install / uninstall / rollback /
    update). If you are using the p2 UI, you can copy the repository
    location from most views.
  - JVM version
  - Proxy setup from Eclipse
  - Proxy setup from the OS
  - Information from the Eclipse Error Log

If this information isn't enough for us to diagnose the problem, you
will likely be requested to run eclipse in console mode and with special
options to get some debug info from the underlying http client. Here are
a few vm arguments to add to your eclipse.ini. Make sure to add those
\*after\* the -vmargs argument in the file

From eclipse Kepler on (apache httpclient 4.x):

` -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog`
` -Dorg.apache.commons.logging.simplelog.showdatetime=true`
` -Dorg.apache.commons.logging.simplelog.log.org.apache.http=DEBUG`
` -Dorg.apache.commons.logging.simplelog.log.org.apache.http.wire=ERROR`

For eclipse Juno and older (apache httpclient 3.x):

` -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog`
` -Dorg.apache.commons.logging.simplelog.showdatetime=true`
` -Dorg.apache.commons.logging.simplelog.log.httpclient.wire.header=debug`
` -Dorg.apache.commons.logging.simplelog.log.org.apache.commons.httpclient=debug`
` -Dorg.apache.commons.logging.simplelog.log.httpclient.wire=debug`

## Plug-in installed through the dropins does not install

If a plug-in or a feature that you are installing through the dropins
does not install, first here are a few things to check.

  - Try to install the software using the p2 UI (Help \> Install New
    Software ...). Failing to install here will provide you with an
    explanation and you should be able to figure it out
  - If the previous install succeeded, verify that the location you are
    putting the plug-ins in is supported.

When you open a bug, please provide the following information

  -   - Attach the content of the most recent version of the profile
        file contained in
        <eclipseInstall>/p2/org.elipse.equinox.p2.engine/profileRegistry/<profileID>.profile
      - Provide a link where we can get the plug-ins you are trying to
        install. If they are commercial plug-ins and features, the
        Manifest.MF and feature.xml would be good enough.

## Computing the explanation for the inability to install software is too slow

Computing the explanation as to why the software is not installable is
an NP hard problem and we are always on the look out for test data. If
you have a large setup and the software is failing to install, please
open a bug with the following details:

  - OS
  - JVM version
  - Attach the content of the most recent version of the profile file
    contained in
    <eclipseInstall>/p2/org.elipse.equinox.p2.engine/profileRegistry/<profileID>.profile
  - Indicate the operation you were trying to perform (install /
    uninstall / update / rollback), which sites you were connecting to,
    and what software you were installing. The easiest way to capture
    the sites in your setup is to go to the
    **Preferences\>Install/Update\>Available Software Sites**, select
    all of your sites, and **Export...** the sites to an XML file.

[Repository Association](Category:Equinox_p2 "wikilink")