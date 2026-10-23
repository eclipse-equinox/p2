### What are recommendation descriptors?

Recommendation descriptors provide advice to the provisioning agent.
This advice helps the agent make choices on the versions of IUs that
need to be picked.

### Why are those necessary?

One may ask, why do we need recommendations when InstallableUnits
already define ranges of tolerated version? Because the ranges expressed
in installable units are usually the expression of the producer's point
of view who is trying to maximize the use cases in which its component
can be used. This view point is different from the one of an integrator
/ product producer who wants to ship a very specific / tested alignment
of versions and plug-ins.

Therefore the recommendation descriptor is here to represent the
integrator/product producer view point at provisioning time, such that a
provisioned system does not end up providing an uncontrolled set of
installable units.

### What are they made of?

Recommendation descriptors can be seen as rewriting rules on the
requirements expressed by an IU (this is how they are currently
implemented). Currently the only thing that is allowed to be rewritten
is the version range expressed on a requirement. For example they allow
the rewriting of the JFace IU dependency on swt \[3.0, 4.0) to swt
\[3.3.0, 3.5)

### What can they do?

  - Allow precise control over the set of IUs being installed by
    specifying in a recommendation descriptor the set of versions that
    must be used for each IU.
  - Prevent the installation of IUs that are known to be incompatible or
    undesirable. For example, one product producer could decide that
    he/she never wants to have PDE installed in a system where his/her
    product is installed.
  - Detection of incompatibilities between products. The recommendation
    descriptors can be analyzed (at install time, or at dev time) to
    check for incompatibilities. For example product X is incompatible
    with PDE.
  - Qualification: verify that a system is in a particular state
    matching what is described in recommendations.

### Delivery mechanism?

Recommendations will not be a special construct. They will be delivered
by an installable unit containing the necessary information.

### Usage

The recommended way to use this construct is still being refined but for
the moment here's what we have in mind. Recommendation descriptors will
be used to replace the list of included plug-ins and features found in a
feature.xml Group level recommendation descriptors shipped with eclipse
should only provide recommendations for the plug-in that they actually
own. For example the PDE recommendation descriptor should only have
information about org.eclipse.pde.\* IUs

### Problems

  - What are the rules to detect incompatible recommendations?
    <https://bugs.eclipse.org/bugs/show_bug.cgi?id=196043>
  - Given that multiple versions of an installable unit are installable
    in the same profile, how do we express recommendations for different
    versions? <https://bugs.eclipse.org/bugs/show_bug.cgi?id=196063>
  - Which rules should be used to see if a recommendation is eligible to
    rewrite a particular requirement?
    <https://bugs.eclipse.org/bugs/show_bug.cgi?id=196061>

[Recommendation descriptors](Category:Equinox_p2 "wikilink")