We need to stabilize the file system layout of p2 metadata so that we
can self-host on p2-provisioned Eclipse installs, and use p2 to upgrade
across builds.

## Current State

  - There is a p2 root directory, either supplied using the
    "eclipse.p2.data.area" system property, or by default it is
    eclipse/configuration/org.eclipse.equinox.p2.core/agentdata/. When
    the agent location is co-located with the config location, we have
    this structure:
  - eclipse/
      - configuration/
          - org.eclipse.equinox.p2.core/
              - agentdata/
                  - installRegistry.xml
                  - profileRegistry.xml
                  - metadata/ (the agent's metadata repository -
                    AgentLocation.getMetadataRepositoryURL)
                  - artifacts/ (containing the agent's artifact
                    repository - AgentLocation.getArtifactRepositoryURL;
                    also known as the download cache)
                  - touchpoints/
                      - director/
                          - content.xml (director's rollback repository)
                      - org.eclipse.equinox.p2.touchpoint.eclipse/
                          - bundlepool/
                              - plugins/ (the bundle pool)

## Suggested approach

  - In theory, everything that reads/writes data is pluggable. We want
    to be able to swap in another implementation of IInstallRegistry,
    IProfileRegistry, IDirector, IMetadataRepositoryManager,
    IArtifactRepositoryManager, Engine, etc, and have the other pieces
    continue to work.
  - Nothing in p2 should be exempt from having to qualify their data
    location with their bundle id (namespace). Currently in p2,
    touchpoints are required to do this, but other bits of p2 do not
    (simple director, simple profile registry, etc).
  - We should continue to have a single root directory defined by a
    system property, or by default it will be the under the install
    location (not a sub-directory of configuration location).
  - Each p2 plug-in must write into a sub-directory of that location,
    defined by their plugin namespace.
  - There is no distinction between touchpoint data areas, and other
    data areas (for example the SimpleDirector's data area)
  - We don't provide API to access the root, but we provide an API
    method in p2.core that dishes out the data location for a particular
    plugin namespace (AgentLocation.getDataLocation(String namespace)).
    This would replace the current AgentLocation.getTouchpointDataArea,
    since it's not only touchpoints that have a need to store data.
  - For example, the eclipse touchpoint would write its data under
    <root>/org.eclipse.equinox.p2.touchpoint.eclipse. Since the profile
    registry is currently implemented by the engine plugin, its file
    would be at
    <root>/org.eclipse.equinox.p2.engine/profileRegistry.xml.
  - Remove the "bundlepool" segment from the Eclipse touchpoint's bundle
    pool path.

A typical Eclipse install with p2 agent data co-located with the install
directory would look like this (following from example above):

  - eclipse/
      - configuration/
      - p2/
          - org.eclipse.equinox.p2.engine/
              - installRegistry.xml
              - profileRegistry.xml
          - org.eclipse.equinox.p2.core/
              - cache/ (the download cache, a colocated
                metadata/artifact repository)
          - org.eclipse.equinox.p2.touchpoints.eclipse
              - plugins/ (bundle pool)
          - org.eclipse.equinox.p2.director
              - rollback/ (director rollback repository)

[Data paths](Category:Equinox_p2 "wikilink")