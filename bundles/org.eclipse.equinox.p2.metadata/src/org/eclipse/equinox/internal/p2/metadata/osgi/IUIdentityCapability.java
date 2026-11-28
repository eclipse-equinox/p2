/*******************************************************************************
 * Copyright (c) 2026 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.osgi;

import java.util.Map;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

/**
 * Derives an <code>osgi.identity</code> {@link Capability} from the
 * {@link IInstallableUnit#getId()}/{@link IInstallableUnit#getVersion()} of an
 * {@link IInstallableUnit}, see
 * <a href="https://docs.osgi.org/specification/osgi.core/8.0.0/framework.resource.html">Resource
 * API Specification</a>
 */
public class IUIdentityCapability implements Capability {

	private final IUResource resource;
	private final Map<String, Object> attributes;

	public IUIdentityCapability(IUResource resource, String type) {
		this.resource = resource;
		IInstallableUnit installableUnit = resource.installableUnit;
//      <capability namespace='osgi.identity'>
//        <attribute name='osgi.identity' value='org.acme.pool'/>
//        <attribute name='version' type='Version' value='1.5.6'/>
//        <attribute name='type' value='osgi.bundle'/>
//      </capability>
		this.attributes = Map.of(//
				IdentityNamespace.IDENTITY_NAMESPACE, resource.getId(), //
				IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE,
				new Version(installableUnit.getVersion().toString()), //
				IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, type);
	}

	@Override
	public String getNamespace() {
		return IdentityNamespace.IDENTITY_NAMESPACE;
	}

	@Override
	public Map<String, String> getDirectives() {
		return Map.of();
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public Resource getResource() {
		return resource;
	}

	@Override
	public String toString() {
		return getNamespace() + "; " + attributes; //$NON-NLS-1$
	}

}
