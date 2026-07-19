This page describes the overall status of the work in progress toward
making p2 run on Apache Felix.

## Goal of the effort

Make the headless subset of p2 run on felix. At this point the effort is
focused on getting everything necessary to get p2.operations up and
running.

## Overview of the work

There are mainly two parts to this work:

  - Cleanup p2 dependencies. Some of the p2 bundles rely on Equinox
    specific packages. These needs to be cleaned up. See the list of
    bundles below for more details
  - Teach p2 to deal with Felix. The goal of frameworkadmin is to
    isolate p2 from the file layouts of various frameworks (e.g.
    config.ini for equinox). Framework specifics are handled in a
    separate bundle like the frameworkadmin.equinox that knows how to
    deal with equinox. In order to teach p2 how to deal with Felix a
    frameworkadmin.felix bundle should be written.

## Contributing to the code

1.  Get the most recent 3.7 build available
    (http://download.eclipse.org/eclipse/downloads/)
2.  Create a CVS repository location for
    ":pserver:anonymous@dev.eclipse.org:/cvsroot/rt". Hint: Select the
    quoted text, open the **CVS Repositories** view, and select **Paste
    Connection** or hit Ctrl+V to add the connection
3.  Expand **HEAD \> org.eclipse.equinox \> p2**.
4.  Checkout "org.eclipse.equinox.p2.releng".
5.  Import the "felix-subset.psf" project set by right clicking on the
    file in the Package Explorer and clicking **Import Project Set...**
    in the releng project you just checked out.
6.  Set the target platform to be the felix.target one. Open the
    felix.target file contained in org.eclipse.equinox.p2.releng bundle
    and hit the "set as target platform" link. This target platform only
    contains the minimal set of prereq and does not include felix nor
    equinox.
7.  Loading Felix. Because p2 relies on the 1.6 version of some of the
    packages from the framework, you will need to get a dev version of
    felix. The easiest is to download the felix jar from
    <https://repository.apache.org/content/repositories/snapshots/org/apache/felix/org.apache.felix.framework/3.3.0-SNAPSHOT/>
    and then add it to the target platform.

After that you should be "good to go".

## Status of the bundles

#### org.eclipse.equinox.frameworkadmin

  - Status: compile errors
  - Work to be done: Make the dependency on pluginconversion optional
    and ensure that at runtime the code is not loaded. The compile
    errors are fine since at build time the environment provides the
    necessary packages.

#### frameworkadmin.equinox

  - Status: expected compile errors - unclear if this bundle is required
  - Work to be done: Make sure that at runtime the bundle will not touch
    all the packages that are declared as being optional.

#### frameworkadmin.felix

  - Status: unknown. There is no compile errors, but this code has not
    been exercised in years.
  - Work to be done: Make it work :)

#### artifact.repository

  - Status: compile errors because the SignatureVerifier facility
    requires org.eclipse.osgi.signedcontent from equinox.
  - Work to be done: three possibilities - The choice here will depend
    if signature verification is desired in a Felix environment.
      - Make the dependency on the signature verifier optional
      - Split the verifier code into a new bundle (would be odd since it
        is
      - See if the signedcontent package from equinox can be separated
        in its own bundle.

#### p2.core

  - Status: ok

#### p2.director

  - Status: ok

#### p2.engine

  - Status: compile errors because of the CheckTrust phase that
    validates certificates
  - Work to be done: two possibilities:
      - Make the dependency on the signature verifier optional and make
        sure that even when the CheckTrust phase is invoked nothing
      - See if the signedcontent package from equinox can be separated
        in its own bundle.

#### p2.garbagecollector

  - Status: ok

#### p2.jarprocessor

  - Status: expected errors. The errors are caused by the reference to
    the org.eclipse.ant.core extension point used to contribute ant
    tasks.

#### p2.metadata

  - Status: ok

#### p2.metadata.repository

  - Status: expected errors. The errors are caused by the reference to
    the org.eclipse.ant.core extension point used to contribute ant
    tasks.

#### p2.operations

  - Status: ok

#### p2.ql

  - Status: ok

#### p2.repository

  - Status: ok

#### p2.touchpoint.eclipse

  - Status: expected compile errors. The bundle has optional
    dependencies on the publisher.

#### p2.touchpoint.native

  - Status: ok

#### p2.transport.ecf

  - Status: ok

#### equinox.simpleconfiguator

  - Status: expected compile errors because the bundle register console
    commands and depends on the resolver API that are both equinox
    specific.

#### equinox.simpleconfigurator.manipulator

  - Status: ok

#### ECF

  - Status: unknown
  - Work to be done: verify that the subset of ECF we are interested
    works on felix. A cursory looks at the manifest seems to indicate
    that it is possible.

#### Sat4j

  - Status: ok

[Category:Equinox_p2](Category:Equinox_p2 "wikilink")