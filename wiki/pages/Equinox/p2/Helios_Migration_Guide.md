In the [Helios](Helios "wikilink") release p2 graduated much of its
provisional API into stable, real API. This page provides some guidance
to users of the old provisional p2 API on how to migrate to the mature
API in the Helios release.

# Pervasive changes

## Switch from global OSGi services to agent-specific services

In the [Ganymede](Ganymede "wikilink") and [Galileo](Galileo "wikilink")
releases, p2 services were registered as global OSGi services. This
style made it difficult to support multiple instances of p2 services
operating in the same framework instance at the same time. The p2 API
now uses an approach where services are obtained from a provisioning
agent instance rather than directly from the OSGi registry. This means
code like this:

``` java
  ServiceReference ref = bundleContext.getServiceReference(IEngine.class.getName());
  IEngine engine = (IEngine)bundleContext.getService(ref);
```

Is now replaced by code like this:

``` java
  IProvisioningAgent agent = ...;//get the agent you want to work with
  IEngine engine = (IEngine) agent.getService(IEngine.SERVICE_NAME);
```

For details on how to obtain an agent, see the [Multiple
Agents](Equinox/p2/Multiple_Agents#Using_the_agent_API "wikilink")
documentation.

### Default Agent and Declarative Services

IProvisioningAgent and related services for the currently running system
(see [Multiple
Agents](Equinox/p2/Multiple_Agents#Using_the_agent_API "wikilink")) are
registered with declarative services (DS). If you want to take advantage
of these (and not register them yourself), you must include the
declarative services bundle (**org.eclipse.equinox.ds**) in your target
platform and make sure it activates.

# Metadata changes

## IRequiredCapability

IRequiredCapability was limiting in what it could express. For example
it could not express negation and or'ing, and it could only described
dependencies on something that had a namespace, a name and a version and
we are striving to express requirements and capabilities on other things
(for example BundleExecutionEnvironment). As such, to ensure for API
evolution we have turned the too specific IRequiredCapability into an
IRequirement.

## IInstallableUnit

IInstallableUnit use to have constants allowing for the identification
of groups, categories and the like. These constants were either used to
query for a given kind of element in a repository or identify the kind
of a given element. The constants have been removed from the API to the
benefit of pre-canned queries and helper methods (e.g GroupQuery and
CategoryQuery)

# Query changes

## CapabilityQuery

CapabilityQuery from the provisional API has been deleted in favor of
expressions. Thus the former:

``` java
IRequiredCapability cap = ...;
new CapabilityQuery(cap);
```

Can be written as:

``` java
IRequirement cap = ...;
new ExpressionQuery(IInstallableUnit.class, cap.getMatches());
```

Or, using Java 5 generics:

``` java
IRequirement cap = ...;
new ExpressionQuery<IInstallableUnit>(IInstallableUnit.class,
cap.getMatches());
```

## InstallableUnitQuery

This class has been replaced by factory methods on the QueryUtil class.
See QueryUtil\#createIUQuery(...).

## IUPropertyQuery

There is currently no direct API replacement for IUPropertyQuery, but
the equivalent query can be performed using an expression query:

``` java
new ExpressionQuery<IInstallableUnit>(IInstallableUnit.class,
 ExpressionUtil.parse("properties[$0] == $1"), "foo", "true");
```

The above will match all IInstallableUnits with a "foo" property whose
value is "true".

# Engine changes

## Operands

The previous provisional engine API required the client to construct
Operand instances to be passed into the engine. Rather than constructing
operands, the client now constructs a plan object that is passed into
the engine. For example, a client that previously created an
InstallableUnitOperand and passed it to IEngine\#perform would now do
the following:

``` java
IEngine engine = ...;
IProvisioningPlan plan = engine.createPlan(profile, context);
plan.addInstallableUnit(someIU);
engine.perform(plan, phases, monitor);
```

[Helios Migration Guide](Category:Equinox_p2 "wikilink")