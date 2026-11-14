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
package org.eclipse.equinox.p2.metadata.osgi.namespace;

import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Namespace;

/**
 * Feature Capability and Requirement Namespace.
 * <p>
 * This class defines the names for the attributes and directives for this
 * namespace, modeled after {@link BundleNamespace osgi.wiring.bundle}: the
 * namespace name doubles as the capability attribute holding the feature id,
 * plus a {@value #CAPABILITY_VERSION_ATTRIBUTE} attribute holding the feature's
 * {@code Version}.
 * <p>
 * A resource that represents a feature (e.g. an Eclipse {@code feature.xml})
 * provides exactly one {@value #FEATURE_NAMESPACE} capability describing
 * itself, in addition to an {@code osgi.identity} capability whose
 * {@link IdentityNamespace#CAPABILITY_TYPE_ATTRIBUTE type} attribute is
 * {@value #TYPE_FEATURE}. Other features can then depend on it by declaring a
 * {@value #FEATURE_NAMESPACE} requirement with a filter matching the feature's
 * id and version range, the same way a {@code osgi.wiring.bundle} requirement
 * is used to depend on a bundle.
 * <p>
 * This namespace intentionally only depends on {@code org.osgi.resource} and
 * {@code org.osgi.framework.namespace} APIs so that it can be proposed as a
 * standalone addition to the OSGi specification (e.g. as a companion to the
 * Feature Launcher specification) without requiring any further change on the
 * side of its current users.
 *
 * @Immutable
 * @since 2.10
 */
public final class FeatureNamespace extends Namespace {

	/**
	 * Namespace name for feature capabilities and requirements.
	 * <p>
	 * Also, the capability attribute used to specify the id of the feature.
	 */
	public static final String FEATURE_NAMESPACE = "osgi.wiring.feature"; //$NON-NLS-1$

	/**
	 * The capability attribute identifying the {@code Version} of the feature if
	 * one is specified or {@code 0.0.0} if not specified. The value of this
	 * attribute must be of type {@code Version}.
	 */
	public static final String CAPABILITY_VERSION_ATTRIBUTE = "version"; //$NON-NLS-1$

	/**
	 * The attribute value identifying the resource
	 * {@link IdentityNamespace#CAPABILITY_TYPE_ATTRIBUTE type} as a feature.
	 *
	 * @see IdentityNamespace#CAPABILITY_TYPE_ATTRIBUTE
	 */
	public static final String TYPE_FEATURE = "osgi.feature"; //$NON-NLS-1$

	private FeatureNamespace() {
		// empty
	}

}
