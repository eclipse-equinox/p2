p2's ability to provision complete products from repositories has made
the concept of repositories a first-class citizen of the eclipse
ecosystem. People can now use repositories during the build, at
development time to setup their target and find missing bundles or
simply to setup their IDE. Consequently, repository users expect the
content of these repositories to be stable, and it is important to make
the retention policy of each repository clearly available.

The following document describes the retention policy used by the
[Eclipse](Eclipse "wikilink") Project:

  - Release repositories: the repository contains all the metadata and
    artifact of a given release (e.g. Galileo) and no content is ever
    removed from it. When a new SR is made available the metadata and
    the artifacts are appended to the existing repository.
  - Milestone build repositories: content is expected to stay until the
    release is complete. As of 4/9/2016  only about two milestones are
    left in the composite milestone repository. Earlier ones not in the
    composite will be available either on downloads server or in
    archives until the stream is released. In this context "Release
    Candidates" are considered "Milestones".
  - Integration build repositories: content is expected to stay until
    the milestone is complete, but can be disposed at any time. As of
    4/9/2016  only about four I-builds are left in the composite I-build
    repository, but earlier ones not in the composite are typically
    available on downloads server until the next milestone is available.
    They may be deleted but usually only if it is known to be a "bad"
    I-build (that is, will hurt something if installed).
  - Nightly build repositories: content can be disposed at any time.

Though specific to the Eclipse platform team, these rules are general
enough that we encourage any other team producing repositories to follow
them.

See also:
[Eclipse_Project_Update_Sites](Eclipse_Project_Update_Sites "wikilink")

[Category:Equinox_p2](Category:Equinox_p2 "wikilink")
[Category:Eclipse](Category:Eclipse "wikilink")