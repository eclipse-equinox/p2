# Background

## Existing p2 service model

In the Ganymede and Galileo releases, p2 used a service model that
assumed all services were singletons. In particular, the API and
implementation assumed there would only ever be one planner, director,
engine, metadata repository manager, etc, available at any given time.
This was reinforced through use of helper classes such as ServiceHelper
that performed simple OSGi service lookups without providing any context
that would allow for a particular instance among multiple available
instances to be selected.

This "singleton" service model is very limiting because it requires a
separate VM/framework to be forked if another p2-managed system is to be
manipulated. For example if PDE wants to manipulate a target platform,
or a build system wants to export/provision into a separate system, it
cannot be done in the same process.

## Agent locations

Metadata representing an Eclipse configuration managed by p2 is stored
in a p2 *agent location*. While multiple profiles can theoretically be
managed from a single location, the most common case is that each
application has its own private *agent location*. This is the
*eclipse/p2* directory in an Eclipse SDK install. All p2 services at
some point need to know which *agent location* they are operating
against, so they know where to access/store metadata representing the
system being provisioned or manipulated.

In a system where multiple instances of p2 services are running, it
becomes important to determine which of the available implementations to
use at any given time. In particular a director or engine operating on a
given location must use the IProfileRegistry, IProvisioningEventBus,
etc, matching that location. This is the crux of the p2 *singleton
service* problem we are seeking to solve in the next release of p2.

# p2 Agent API

## Using the agent API

p2 [Helios](Helios "wikilink") contains a new IProvisioningAgent API. An
agent instance represents a single agent location on disk, and all
service instances linked to that location. When running the Eclipse
platform, an instance of IProvisioningAgent is registered as an OSGi
service, representing the agent of the currently running system. Other
p2 services can be obtained from this agent interface:

``` java
IProvisioningAgent agent = bundleContext.getService(...);
IDirector director = (IDirector)agent.getService(IDirector.SERVICE_NAME);
director.provision(...);
```

To obtain an agent instance for another location, the
IProvisioningAgentProvider service is used:

``` java
IProvisioningAgentProvider provider = bundleContext.getService(...);
IProvisioningAgent agent = provider.createAgent(new URI("file:/some/location"));
IDirector director = (IDirector)agent.getService(IDirector.SERVICE_NAME);
director.provision(...);
agent.stop();
```

The agent for the currently running system can also be obtained by
invoking createAgent(null) on the IProvisioningAgentProvider. Note that
a client who instantiates an agent by invocating createAgent must stop
the agent when they are finished using it by invoking
IProvisioningAgent\#stop().

## Contributing a service to an agent

The agent itself doesn't know anything about specific services such as
the director, engine, etc. The system is completely pluggable because
some p2 applications may only require a small subset of the available p2
services. The different p2 bundles contribute services to the agent by
providing an IAgentServiceFactory, typically via a DS component. A
property on the factory describes what service it is providing. For
example, here is the DS component definition for the factory that
provides the engine:

``` xml
<?xml version="1.0" encoding="UTF-8"?>
<scr:component xmlns:scr="http://www.osgi.org/xmlns/scr/v1.1.0"
  name="org.eclipse.equinox.p2.engine">
   <implementation class="org.eclipse.equinox.internal.p2.engine.EngineComponent"/>
   <service>
      <provide interface="org.eclipse.equinox.p2.core.spi.IAgentServiceFactory"/>
   </service>
   <property
     name="p2.agent.servicename"
     type="String"
     value="org.eclipse.equinox.internal.provisional.p2.engine.IEngine"/>
</scr:component>
```

In essence this declaration says, "This is a component that provides an
agent service factory that knows how to create engines". Typically a
given p2 service relies on services provided by other p2 bundles. To
ensure that each instance gets wired up to the correct instances of
other services, it must obtain those services from the agent from within
the factory method. For example the profile registry requires the agent
location (so it knows where the registry lives on disk), and the event
bus (so it can broadcast profile change events). These dependencies are
satisfied in the factory method:

``` java
public Object createService(IProvisioningAgent agent) {
  AgentLocation location = (AgentLocation) agent.getService(AgentLocation.SERVICE_NAME);
  SimpleProfileRegistry registry = new SimpleProfileRegistry(
    SimpleProfileRegistry.getDefaultRegistryDirectory(location));
  registry.setEventBus((IProvisioningEventBus) agent.getService(IProvisioningEventBus.SERVICE_NAME));
  return registry;
}
```

This will cause the AgentLocation and IProvisioningEventBus services to
be instantiated if necessary. If those services in turn require other
services, those dependencies will be satisfied when their factory method
runs. In this way the entire service dependency tree is loaded as each
service is needed.

# p2 implementation cleanup

The p2 implementation is gradually being moved over to support multiple
agents. In the meantime, exemplarysetup continues to register each of
the services for the currently running system (IEngine, IDirector, etc),
so all existing code continues to work but only against the current
system. Some guidelines on moving to a multi-agent world:

  - The general approach is to pass services into an object that needs
    them, rather than have the object reach out. The services can be
    passed in the constructor, or via a setter method if they are things
    that may change throughout the lifetime of the object using the
    service.
  - Avoid creating and using static convenience methods that require p2
    services. Since static methods don't have any way to determine
    *which* agent to use, they can't select the right agent. This
    includes generic conveniences like ServiceHelper, but also classes
    like ProvisioningUtil,
    org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util, etc.
    Instead, whoever is *reaching out* to these static helpers should
    instead generally have the required services passed into them.
  - We might not need all of p2 to be multi-agent aware. For example the
    console or even the UI might be ok with their current assumption
    that they only operate on the agent of the currently running system.
    However, it wouldn't hurt for all p2 bundles to isolate out service
    lookup from the rest of their code to increase flexibility.

# References

  -
  -
[Multiple Agents](Category:Equinox_p2 "wikilink")