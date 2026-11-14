This document presents an abstract perspective on the data and the
problem solved by p2, the new provisioning system being developed in the
Equinox project.

Overall, the goal of p2 is to provide users the ability to manage the
software installed on their system. Target systems are componentized
such that each component expresses various dependencies, and there are
multiple versions of each component available to the user. The
installation of a given component is equivalent to satisfying all the
constraints such that the component and all its prerequisites are
installed while guaranteeing, for example, that the most recent version
of each component is available.

## Installable units: the software components

#### Installable Unit

Installable units (also referred to as IU) are unit of componentization
used for software delivery in p2. An installable unit has the following
attributes:

  - An identifier
  - A version
  - An enablement filter
  - A disjunction of requirement expressions
  - A set of capabilities
  - A singleton flag

#### Enablement filter

The enablement filter is of the form of an LDAP filter \[1\]. It
indicates in which contexts an installable unit can be installed. The
evaluation of this filter is done against a set of valued variables
called an “evaluation context”.

#### Evaluation context

The evaluation context is a set of sets of valued variables. The
following example represents an evaluation context containing two sets
of valued variables:  Such a context would evaluate to true for the
following filters

  - (os=win32)
  - (& (os=linux) (nl=en_CA))

but would evaluate to false for the following filters:

  - (os=macos)
  - (& (os=win32) (ws=win32) (nl=fr_FR) (color=blue))

#### Capability

A capability has the three following attributes:

  - A namespace
  - A name
  - A version

We often say that an IU provides capabilities.

#### Requirement expression

A requirement expression is composed of two parts:

  - An enablement filter as defined previously. The absence of a filter
    is equivalent to a filter evaluating to true.
  - A Conjunctive Normal Form of Requirements. Note that we do not allow
    for negation in these CNFs, however we would be interested in
    understanding if this would be possible. In the examples “-\>” will
    be used to separate the filter from the CNF of requirements.

#### Requirement

A requirement has the following attributes:

  - A namespace
  - A name
  - A version range
  - A greediness flag
  - A multiplicity flag
  - An optionality flag

Requirements are satisfied by capabilities.

#### Facts

  - In a given IU, there exists no relationship between the
    namespace/name/version of its capabilities and its
    identifier/version.
  - In a given IU, there exists no relationship between the
    namespace/name/version range of its requirements and its
    identifier/version.
  - In a given IU, there exists no relationship between the
    namespace/name/version of its capabilities and the
    namespace/name/version range of its requirements.
  - The same capability can be provided by multiple IUs.
  - The same requirement can be expressed by multiple IUs.
  - IUs can be involved in cyclic dependencies:
  - Example:

` IU A provides capability X and requires capability Y`
` IU B provides capability Y and requires capability X`

  - An IU can provide a capability it requires.
  - A capability can satisfy requirements from several IUs at the same
    time.

#### Example

` IU org.eclipse.swt v 3.2.0`
`   Capabilities:`
`     {namespace=package, name=a, version=1.0.0}`
`     {namespace=foo, name=b, version=1.3.0}`
`     {namespace=package, name=c, version=4.1.0}`
` Requirement expressions`
`   (true) ->`
`     {namespace=package, name=r1, range=[1.0.0, 2.0.0)} and`
`     {namespace=foo, name=r1, range=[3.2.0, 4.0.0)}`
`   (& (os=linux) (ws=gtk)) ->`
`     {namespace=package, name=r2, range=[1.0.0, 2.0.0)} or`
`     {namespace=foo, name=bar, range=[3.2.0, 4.0.0)}`
` IU org.eclipse.jface v 3.3.0`
`   Capabilities:`
`     {namespace=package, name=a, version=1.0.0}`
`     {namespace=package, name=jface, version=3.1.0}`
`   Requirement expressions:`
`     (true) ->`
`        {namespace=package, name=a, range=[1.0.0, 2.0.0)} and`
`        {namespace=foo, name=b, range=[1.0.0, 4.0.0)}`
`  (& (os=linux) (ws=gtk)) ->`
`        {namespace=package, name=a, range=[1.0.0, 1.1.0)} or`
`        {namespace=foo, name=bar, range=[3.2.0, 4.0.0)}`

## Satisfaction of an installable unit

In order to be successfully installed on a user system, an installable
unit’s requirement expression must be satisfied. This section defines
what this means.

#### Satisfaction of an installable unit

An installable unit can be installed (satisfied) when one of its
conjunction of requirement expression is satisfied and when each IU
satisfying each requirement in this expression can also be installed.

#### Solution and pools of installable units

The transitive closure of the IUs satisfying the IU to install is called
the Solution. The installable units added to the solution are coming
from a Pool of Installable Units (IUPool).

#### Satisfaction of a requirement

A requirement is said to be satisfied by a capability iff the namespace
and the name of the requirement and the capability are the same and the
capability’s version is included into the requirement’s version range.

#### Satisfaction of requirement expressions

A requirement expression is satisfied iff the enablement filter
evaluates to true and the CNF of requirements evaluates to true. For
example, the following requirement expression

` (& (os=linux) (gtk=linux)) ->`
`   {namespace=package, name=a, range=[1.0.0, 1.1.0)} or`
`   {namespace=osfacility, name=gtk, range=[3.2.0, 4.0.0)}`

will evaluate to true if the evaluation context contains at least a set
such that `{os=linux, gtk=linux}` and one of the following requirement
`{namespace=package, name=a, version=1.0.0}` or `{namespace=osfacility,
name=gtk, version=3.3.0}` is satisfied.

#### Requirements greed

When a requirement is marked greedy (greediness flag on a requirement),
installable units satisfying the requirements are added to the solution,
whereas IU satisfying non greedy requirements should be added to the
solution by another requirement. By default all requirements are greedy.

#### Requirements multiplicity

When the multiplicity flag is set to true, it indicates that all IUs
carrying a capability satisfying the requirement should be considered
for addition into the solution. By default this flag is false. Depending
on the complexity we would like to explore whether this multiplicity
flag could be changed to be a cardinality (for example: 0 or more, 1 or
more, only 3, etc.).

#### Requirements optionality

The optionality flag on a requirement indicates that failure to satisfy
the requirement does not prevent the IU from being satisfied.

#### IUs and singletons

IUs with the same identifier and with the singleton flag set to false
can be installed simultaneously. IUs with the same identifier and where
one or many have the flag singleton set to true can not be installed
simultaneously.

## Limiting the space: advice

In this section we present the notion of advice. Advice is a construct
used as a source of information to further control which IUs will
constitute the solution. We have currently 4 kinds of advice described
below. They should be taken into account in the following order (the
first one being the most important to respect): version control,
causality, uses-clause, affinity

#### Version control

Version control advice is used to better control which IU could satisfy
a requirement. For example, when an IU expresses a requirement on X
\[3.0.0, 4.0.0), the following advice: X \[3.3.0, 3.4.0) would cause a
more controlled version of X to be picked. Another way to look at advice
is to consider it as directly overriding the requirements expressed in
the IUs. The ability to narrow down the range is a must have, whereas
the ability to widen/change the range is not.

#### Causality

Causality allows indicating additional IUs to be considered for
installation when a particular IU is installed. For example one could
say: When IU Word 97 is installed the IU X must be installed.

The strength of the relationship between the ‘cause’ IU and the
‘consequence’ IU could vary. Causality differs from requirements
because they are not located in an IU and thus can not cause any IU to
be non installable.

We would like to be able to have causes be expressions (e.g. A and B).

We would like to be able to use the availability of a capability as a
cause.

#### Uses-clauses

Uses-clause expresses solution level consistency rules based on IUs. For
example in a situation where:

` IU A`
`   requires B [1.0, 2.0) and C [1.0, 3.0)`
` IU B`
`   requires C [1.1, 3.0)`
` IU C (version 1.0, 2.0 and 3.0 available)`

we need to able to express that the C used to satisfy A must be the same
C that satisfied B. This construct would be highly desirable.

#### Affinities

An affinity allows expressing favored relationships between IUs thus
guiding what should be picked when trying to satisfy a requirement.
Affinities can not cause the installation of the other IUs involved in
the relationship, they are non-greedy. For example one could say, When
the IU “Word 97 is installed and an IU “Excel” is necessary, favor IU
“Excel 97” if it matches.

### Dynamic Advice

As a cross-cutting concern, we are interested in understanding if advice
can be discovered or changed while the solution is being built and if so
under which limitations.

## The question to be answered

The main question we want to ask the solver is:

**Given, a set of sets of IU to install, an IUPool, a Solution and
Advice, is there a solution where one IU from each set can be
installed?**

For example, given the following install sets , is there a solution
where one of each of the subsets can be installed (e.g. IU A, IU B, IU
C) can be installed.

Depending on the circumstances, we would like one or several of the
following characteristics to also be met:

  - Ensure that it is always the highest/lowest version possible of each
    IU
  - Have the least number of IU in the solution
  - The least change from the existing solution

The solution returned should contain information on which IU satisfied
which requirement. In the absence of a solution, an explanation must to
be provided. We would also be interested in having the solver indicate
if a solution existed by relaxing a few constraints.

### Chained / nested solutions

Given multiple, inter-dependent systems and software to be deployed on
some of them, could the solver be used to reason about the validity of
all the systems at the same time. For example, if we had a client and a
server in which software needed to be installed, how would we reason
about the overall validity of the client and the server given that they
interact and need to have compatible versions of the software on both?

### Size of the problem and other relevant facts

  - Taken individually each IU from the IUPool is usually installable.
    It means that the

metadata of an IU has been written such that the IU is installable and
it means that the IUPool usually contains all the IUs necessary to the
satisfaction of an IU to be installed.

  - A typical system will likely be composed of thousands of IUs. An IU
    usually has around a 25 to 50 requirements and around 50
    capabilities.
  - The IUPool may be composed of tens of thousands of IUs.
  - The current system at hand is composed of 1100 IUs.

## References

A String Representation of LDAP Search Filters, RFC 1960, UMich, 1996,
<http://www.ietf.org/rfc/rfc1960.txt>

## Current solution

Following the ideas of \[EDOS\] and \[OPIUM\], the p2 resolver available
in 1.0 (Eclipse 3.4 and greater) is based on a SAT solver approach using
SAT4J (http://sat4j.org) pseudo boolean capabilities. Each IU is mapped
to a set of constraints and an optimization function (more can be found
in the class Projector
[1](http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.director/src/org/eclipse/equinox/internal/p2/director/Projector.java?root=RT_Project&view=log%7Corg.eclipse.equinox.internal.p2.director.Projector)).

EDOS (W2P2) <http://www.edos-project.org> which explores ways to
validate the content of a repository.

OPIUM <http://www.cse.ucsd.edu/~rjhala/papers/opium.html> which
addresses the installation problem using a pseudo boolean solver.

[Resolution](Category:Equinox_p2 "wikilink")