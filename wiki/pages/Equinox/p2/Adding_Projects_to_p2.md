This page describes the list of steps that you need to take in order to
add a new project to Equinox p2 Provisioning.

### Load RelEng Projects

You will need to check out the Platform's Release Engineering project
from the repository:

    repo: dev.eclipse.org
    path: /cvsroot/eclipse
    project: org.eclipse.releng

Inside there is a *maps* folder which contains entries for all our
projects. The build takes a project name and looks it up in this file in
order to determine which version to check out from the repository. You
should add your bundles to the Provisioning section and try to keep them
in alphabetical order in order to find them easier.

### Update Features

As far as the build itself goes, the build is driven from features and
not bundles. So we have created some features to build and included our
bundles in them. The features can be found in the p2 Release Engineering
project:

    repo: dev.eclipse.org
    path: /cvsroot/eclipse/equinox-incubator/provisioning
    project: org.eclipse.equinox.p2.releng

Inside this project we have a *buildtime-features* folder which contains
the 5 features that we currently build: the agent, the metadata
generator, the director, the user ui, and a self-hosting feature. Where
will your bundles be used? They may need to be added to more than one
feature.

### Update the config.ini

The build actually runs the director to provision an agent so if your
bundle is required as part of the director, then the config.ini
(osgi.bundles list) needs to be updated to include your new bundle. You
should also check the other config.ini files, just in case.

    repo: dev.eclipse.org
    path: /cvsroot/eclipse
    project: org.eclipse.releng.eclipsebuilder
    files: equinox/buildConfigs/equinox.prov/files

### Releasing Your Changes

Once your bundles have been added to the map file and appropriate
feature files, you need to:

  - release the changes to the feature files to the repository
  - tag the `org.eclipse.equinox.p2.releng` project with a version
  - update the entries for the 5 features in the map file to be the
    version that you just tagged
  - tag your projects
  - update your entries in the map file to be the version that you
    tagged

Once this initial setup of your projects is done, you can use the
Release Engineering Tools to release your projects on a weekly (or
whenever) basis. The RelEng Tools bundle is available from the Eclipse
downloads page and it adds a Team -\> Release menu option which will
take care of tagging your project, updating the map file, and releasing
the updated map file to the repository.

[Category:Equinox p2](Category:Equinox_p2 "wikilink")