### Fetch artifacts for your build using p2

A new extension has been added to PDE/Build that enables users to fetch
artifacts from p2 repositories. You can now add an entry to your map
file for build input and PDE/Build will retrieve the artifact and
include it in the build.

The map file entry needs to include the id and version of the
installable unit (IU), and the URI of the repository where the IU can be
found. The metadata and artifact repositories are assumed to be
co-located. An example is:

    plugin@my.bundle.id,1.0.0=p2IU,id=my.bundle.id,version=1.0.0,repository=http:/example.com/repo

    feature@my.feature.id,1.0.0=p2IU=my.feature.id.feature.jar,version=1.0.0,repository=http://example.com/repo

When fetching features from a p2 repository, you must refer to the
feature jar, and not the group. See [Feature Metadata
Layout](http://help.eclipse.org/galileo/index.jsp?topic=/org.eclipse.pde.doc.user/tasks/pde_p2_featuremetadata.htm)
in the eclipse help.

### p2 repositories as a target for PDE Build

You can now specify a location that contains local p2 repositories in
nested folders or zip files. The build will automatically transform the
repositories into a form that PDE/Build can compile against.

There is a new processRepos phase in the main build script with
corresponding pre and post targets in the customTargets.xml file.

You must set these properties in the build.properties for your builder.
The repoBaseLocation is the location of the folder containing the local
p2 repositories. The transformedRepoLocation is the location where the
transformed artifacts will be placed after being processed.

    repoBaseLocation=${base}/repos
    transformedRepoLocation=${base}/transformedRepos

[Build Input](Category:Equinox_p2 "wikilink")
[p2](Category:PDE/Build "wikilink")