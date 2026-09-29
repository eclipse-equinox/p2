The following is a set of terms and concepts that are prevalent in the
Equinox Provisioning work. Some of the terms here are duplicates of
those on the [general provisioning terminology
page](Provisioning_Terminology "wikilink").

  - Agent
    The provisioning infrastructure on client machines is generally
    referred to as the agent. Agents can manage themselves as well as
    other profiles. An agent may run separate from any other Eclipse
    system being managed or may be embedded inside of another Eclipse
    system. Agents can manage many profiles (see below) and indeed, a
    given system may have many agents running on it. There is no such
    thing as "the p2 agent" or a bundle that goes by that name. This is
    because p2 is modular and what you need to run on an embedded device
    is not the same than what you need on a desktop or an autonomous
    server.

<!-- end list -->

  - Artifact
    Artifacts are the actual content being installed or managed. Bundle
    JARs and executable files are examples of artifacts.

<!-- end list -->

  - Artifact Repository
    Artifact repositories hold artifacts

<!-- end list -->

  - Director
    The director is a high level API that combines the work of the
    planner and the engine. That is, the director invokes the planner to
    compute the provisioning operations to perform, and then invokes the
    engine with the planner's output to achieve the desired profile
    changes.

<!-- end list -->

  - [Engine](Equinox/p2/Engine "wikilink")
    The engine is responsible for carrying out the desired provisioning
    operations as determined by a director. Whereas the subject of the
    director's work is metadata, the subject of the engine's work is the
    artifacts and configuration information contained in the IUs
    selected by the director. Engines cooperate with repositories and
    transport mechanisms to ensure that the required artifacts are
    available in the desired locations. The engine runs by invoking a
    set of engine Phases and working with the various Touchpoints to
    effect the desired result.

<!-- end list -->

  - Garbage Collection
    Element of repositories (metadata and artifact) can be garbage
    collected by tracing reachability from a set of known *roots*. For
    example, the set of all profiles managed by an agent transitively
    identifies all IUs that are currently of direct interest to the
    provisioning agent. Similarly, the IUs identify the artifacts
    required to run the profiles. Any IUs or artifacts that are not in
    the transitive list are *garbage* and can be collected.

<!-- end list -->

  - [Installable Units](Installable_Units "wikilink")(IU)
    Installable Units are **metadata** that describe things that can be
    installed. They are **not** the things themselves. So an IU for a
    bundle is not the bundle but a description of the bundle: its name,
    version, capabilities, requirements, etc.. The bundle JAR is an
    *artifact*.

<!-- end list -->

  - Metadata Repository
    A metadata repository holds installable units.

<!-- end list -->

  - Mirroring
    The basic operation of distribution is mirroring. The key here is
    that metadata and artifacts are not *downloaded*, they are mirrored.
    The subtle distinction is that local mirrors are a) simple caches of
    something that is remote and b) potential sources of further
    mirroring. This means that locally held information can be deleted
    and replaced as needed by re-mirroring. Similarly, having local
    copies act as mirrors opens the path to natural peer-to-peer
    distribution. Note that metadata and artifacts are quite separate
    and having an IU mirrored in one repo does not imply that the
    associated artifacts are in/near/beside/... that repo.

<!-- end list -->

  - Phase
    Provisioning operations generally happen by walking through a set of
    steps or phases. At each phase a particular kind of activity takes
    place. For example, during the Fetch phase, the various artifacts
    required for the operation are Mirrored while during the Configure
    phase IUs are woven into the underlying runtime system by their
    associated Touchpoints.

<!-- end list -->

  - Planner
    The planner is responsible for determining what should be done to a
    given profile to reshape it as requested. That is, given the current
    state of a profile, a description of the desired end state of that
    profile and metadata describing the available IUs, a planner
    produces a list of provisioning operations (e.g., install, update or
    uninstall) to perform on the related IUs.

<!-- end list -->

  - Profile
    Profiles are the target of install/management operations. They are a
    list of IUs that go together to make up a system. They are roughly
    equivalent to the traditional Eclipse *configurations*. When an IU
    is *installed* it is added to a profile. That profile can then be
    run and the artifacts associated with the installed IUs executed (or
    whatever). Later the IU can be uninstalled or updated in that
    profile. The exact same IU can be installed simultaneously in many
    profiles.

<!-- end list -->

  - Touchpoint
    A part of the engine that is responsible for integrating the
    provisioning system to a particular runtime or management system.
    For example, the Eclipse Touchpoint understands how Equinox stores
    and manages bundles. Different platforms have different *Native*
    Touchpoints that integrate with the Windows desktop, RPMs, various
    registries etc. See also [Touchpoint Use
    Cases](Touchpoint_Use_Cases "wikilink").

[Concepts](Category:Equinox_p2 "wikilink")