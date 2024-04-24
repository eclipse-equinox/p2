Installable Units
=================

Contents
--------

*   [1 Installable Unit](#Installable-Unit)
    *   [1.1 IU Identity](#IU-Identity)
    *   [1.2 Enablement filter](#Enablement-filter)
    *   [1.3 IU dependencies and capabilities](#IU-dependencies-and-capabilities)
        *   [1.3.1 Capability](#Capability)
        *   [1.3.2 Requirement expression](#Requirement-expression)
        *   [1.3.3 Requirement](#Requirement)
        *   [1.3.4 Example](#Example)
    *   [1.4 Content aspect](#Content-aspect)
    *   [1.5 Touchpoint, touchpoint data](#Touchpoint.2C-touchpoint-data)
    *   [1.6 Update information](#Update-information)
    *   [1.7 Fixes](#Fixes)
    *   [1.8 Properties](#Properties)
*   [2 Grouping](#Grouping)
*   [3 Installable Unit fragments](#Installable-Unit-fragments)
*   [4 Installable Unit best practices](#Installable-Unit-best-practices)

Installable Unit
----------------

As the name implies, Installable Units (IUs for short) describe things that can be installed, updated or uninstalled. They do not contain the actual artifacts but rather essential information about such artifacts (e.g., names, ids, version numbers, dependencies, etc) and are not aware about what they deliver. They describe things. They are NOT the things. So for example an IU for a bundle is NOT the bundle. The bundle is an "artifact". The metadata allows dependencies to be structured as graphs without forcing containment relationships between nodes. Here is detailed presentation of what an installable unit is made of.

### IU Identity

An IU is uniquely identified by an ID and a version.

### Enablement filter

The enablement filter is of the form of an LDAP filter \[1\]. It indicates in which contexts an installable unit can be installed. The evaluation of this filter is done against a set of valued variables called an “evaluation context”.

### IU dependencies and capabilities

In the same way bundles have import and export packages, IUs have dependencies to talk about their prerequisites and provide capabilities to tell others what they offer.

#### Capability

A capability has the three following attributes:

*   A namespace
*   A name
*   A version

We often say that an IU provides capabilities.

  
Dependencies are expressed against those capabilities to express all the requirements of an IU. This approach offers great flexibility to express dependencies.

#### Requirement expression

A requirement expression is composed of two parts:

*   An enablement filter of the form of an LDAP filter \[1\]. The absence of a filter is equivalent to a filter evaluating to true. When a filter evaluates to false, the requirement is ignored.
*   A Conjunctive Normal Form of Requirements.

#### Requirement

A requirement has the following attributes:

*   A namespace
*   A name
*   A version range
*   A greediness flag, indicates whether or not a new IU should be added to the solution to define satisfy this requirement
*   A multiplicity flag, indicates whether or not multiple IUs should be added to the solution to satisfy this requirement
*   An optionality flag

Requirements are satisfied by capabilities.

  
Note that the id of the IU is also exposed as a capability in the org.eclipse.equinox.p2.iu.

These dependencies information are used by the agent to decide what needs to be installed. For example if you are installing the org.eclipse.jdt.ui IU, the dependencies expressed will cause the transitive closure of IUs reachable to be installed.

#### Example

The syntax used here is not normative. In fact p2 will not define a serialization format for IUs to allow for greater flexibility in storage and manipulation.

 IU org.eclipse.swt v 3.2.0
   Capabilities:
     {namespace=package, name=a, version=1.0.0}
     {namespace=foo, name=b, version=1.3.0}
     {namespace=package, name=c, version=4.1.0}
 Requirement expressions
   (true) ->
     {namespace=package, name=r1, range=\[1.0.0, 2.0.0)} and
     {namespace=foo, name=r1, range=\[3.2.0, 4.0.0)}
   (& (os=linux) (ws=gtk)) ->
     {namespace=package, name=r2, range=\[1.0.0, 2.0.0)} or
     {namespace=foo, name=bar, range=\[3.2.0, 4.0.0)}
 IU org.eclipse.jface v 3.3.0
   Capabilities:
     {namespace=package, name=a, version=1.0.0}
     {namespace=package, name=jface, version=3.1.0}
   Requirement expressions:
     (true) ->
        {namespace=package, name=a, range=\[1.0.0, 2.0.0)} and
        {namespace=foo, name=b, range=\[1.0.0, 4.0.0)}
  (& (os=linux) (ws=gtk)) ->
        {namespace=package, name=a, range=\[1.0.0, 1.1.0)} or
        {namespace=foo, name=bar, range=\[3.2.0, 4.0.0)}

### Content aspect

The IU does not deliver any content. Instead it refers to artifacts. The artifacts are mirrored from an artifact server into a local artifact server on the request of touchpoints.

### Touchpoint, touchpoint data

IUs can be stamped with a type. Using this type, the engine identifies the touchpoint responsible for marrying the IU with the related system. The touchpoint data contains information that will be used to apply the software lifecycle (install, uninstall, update, configure, etc).

### Update information

The lineage information of an IU is explicit. Each IU can express the IU(s) it is an update of. This information is stored in the form of one requirement, thus allowing for an IU to be an update of multiple of its predecessors.

We are contemplating supporting multiple requirements to allow for cases where an IU has been split into multiple IUs or where multiple IUs have merged into one. Another thing that we are contemplating the addition of "staged update" concept. This would allow for cases where an update must be applied even though an higher version exists.

### Fixes

Support for fixes will be added, however the format has not been decided yet.

### Properties

An IU can carry arbitrary properties. These properties are usually only considered by the user interface and the director. The properties targeted at the user are the one containing user readable name information (UI name, license, description, etc.) and the one allowing for better filtering of what is being shown to the user (see grouping section). The properties targeted at the director are usually used as hints/advices to the resolution process.

For properties influencing the director, they should be such that even if the director to which these properties are targeted at is not used, the Installable Unit should still be successfully resolvable.

Grouping
--------

There are various circumstances where grouping is necessary. To address this, p2 does not call out for a specific construct. Instead in p2 groups are just IUs expressing requirements on other IUs. For example, here is an excerpt of the group representing the RCP functionality of eclipse

 IU org.eclipse.rcp v 3.2.0
   Requirement expressions
     (true) ->
       {namespace=iu, name=org.eclipse.osgi, range=\[3.2.0, 3.3.0)} and
       {namespace=iu, name=org.eclipse.jface, range=\[3.2.0, 3.3.0)}
     (& (os=linux) (ws=gtk)) ->
       {namespace=iu, name=org.eclipse.swt.linux.gtk, range=\[3.3.0, 3.4.0)}
     (& (os=win32) (ws=win32) (arch=x86)) ->
       {namespace=iu, name=org.eclipse.swt.win32.win32.x86, range=\[3.3.0, 3.4.0)}

For filtering in the user interface a property flagging group as such is set on the IU.

Installable Unit fragments
--------------------------

Installable unit fragments are installable units that complement an existing installable unit. When a fragment applies to an installable unit, it is being attached to this installable unit. A fragment can apply to multiple installable unit. Once the fragment has been attached its content is seamlessly accessible from the installable unit.

An installable unit fragment can not modify the dependencies of the installable unit to which it is attached.

Installable unit fragments are used to deliver touchpoint data common to multiple installable units. It could also be used to deliver metadata translation.

Installable Unit best practices
-------------------------------

The information contained in an installable unit must be kept generic to allow for reuse. For example, the org.eclipse.equinox.common bundle needs to be started. However the start level at which it needs to be started differs based on the scenario where the bundle is being used (e.g. in a RCP context, it needs to be started at level 2, whereas it needs to be started at level 3 in the server side context).

Therefore for the IU to stay generic the touchpoint data for org.eclipse.equinox.common can not specify a start level. The IU should only contain information related to the dependencies and capabilities that the IU has.

In order to deliver the start level information, installable unit fragments will be created. In our example we would have one installable unit fragment to be used in an RCP Context and another one for the server side context. Each of these IU fragments will have its own unique ID and will not collide with each others.

