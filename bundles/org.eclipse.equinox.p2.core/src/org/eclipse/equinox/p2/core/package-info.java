@org.osgi.annotation.bundle.Export
@org.osgi.annotation.versioning.Version("2.8.0")
/**
 * Provides core support for interacting with a p2-based provisioning system
 * Package Specification
 * 
 * This package specifies API for creating, using, and extending a provisioning
 * system. A provisioning agent ties together a set of related services that
 * work together to implement a provisioning system. For end users of the
 * provisioning system, they simply instantiate or obtain an agent and get the
 * services they require from the agent. Extenders can register a factory for
 * adding new services to the system, or add services directly to an agent.
 * 
 * This package also provides some basic utility classes that are common across
 * large parts of the system.
 * 
 * @since 2.0
 */
package org.eclipse.equinox.p2.core;

