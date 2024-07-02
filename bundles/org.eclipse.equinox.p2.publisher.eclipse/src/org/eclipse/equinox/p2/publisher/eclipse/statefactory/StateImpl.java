/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Danail Nachev -  ProSyst - bug 218625
 *     Rob Harrop - SpringSource Inc. (bug 247522)
 *     Karsten Thoms (itemis) - bug 535341
 *******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse.statefactory;

import org.osgi.framework.Constants;

public abstract class StateImpl {

	public static final String ECLIPSE_PLATFORMFILTER = "Eclipse-PlatformFilter"; //$NON-NLS-1$
	public static final String Eclipse_JREBUNDLE = "Eclipse-JREBundle"; //$NON-NLS-1$
	/**
	 * Manifest Export-Package directive indicating that the exported package should only
	 * be made available when the resolver is not in strict mode.
	 */
	public static final String INTERNAL_DIRECTIVE = "x-internal"; //$NON-NLS-1$

	/**
	 * Manifest Export-Package directive indicating that the exported package should only
	 * be made available to friends of the exporting bundle.
	 */
	public static final String FRIENDS_DIRECTIVE = "x-friends"; //$NON-NLS-1$

	/**
	 * Manifest header (named &quot;Provide-Package&quot;)
	 * identifying the packages name
	 * provided to other bundles which require the bundle.
	 *
	 * <p>
	 * NOTE: this is only used for backwards compatibility, bundles manifest using
	 * syntax version 2 will not recognize this header.
	 *
	 * <p>The attribute value may be retrieved from the
	 * <code>Dictionary</code> object returned by the <code>Bundle.getHeaders</code> method.
	 * @deprecated
	 */
	@Deprecated
	public final static String PROVIDE_PACKAGE = "Provide-Package"; //$NON-NLS-1$

	/**
	 * Manifest header attribute (named &quot;reprovide&quot;)
	 * for Require-Bundle
	 * identifying that any packages that are provided
	 * by the required bundle must be reprovided by the requiring bundle.
	 * The default value is <code>false</code>.
	 * <p>
	 * The attribute value is encoded in the Require-Bundle manifest
	 * header like:
	 * <pre>
	 * Require-Bundle: com.acme.module.test; reprovide="true"
	 * </pre>
	 * <p>
	 * NOTE: this is only used for backwards compatibility, bundles manifest using
	 * syntax version 2 will not recognize this attribute.
	 * @deprecated
	 */
	@Deprecated
	public final static String REPROVIDE_ATTRIBUTE = "reprovide"; //$NON-NLS-1$

	/**
	 * Manifest header attribute (named &quot;optional&quot;)
	 * for Require-Bundle
	 * identifying that a required bundle is optional and that
	 * the requiring bundle can be resolved if there is no
	 * suitable required bundle.
	 * The default value is <code>false</code>.
	 *
	 * <p>The attribute value is encoded in the Require-Bundle manifest
	 * header like:
	 * <pre>
	 * Require-Bundle: com.acme.module.test; optional="true"
	 * </pre>
	 * <p>
	 * NOTE: this is only used for backwards compatibility, bundles manifest using
	 * syntax version 2 will not recognize this attribute.
	 * @since 1.3 <b>EXPERIMENTAL</b>
	 * @deprecated
	 */
	@Deprecated
	public final static String OPTIONAL_ATTRIBUTE = "optional"; //$NON-NLS-1$

	public static final String OSGI_RESOLVER_MODE = "osgi.resolverMode"; //$NON-NLS-1$
	public static final String STRICT_MODE = "strict"; //$NON-NLS-1$
	public static final String DEVELOPMENT_MODE = "development"; //$NON-NLS-1$

	public static final String STATE_SYSTEM_BUNDLE = "osgi.system.bundle"; //$NON-NLS-1$

	private static final String OSGI_OS = "osgi.os"; //$NON-NLS-1$
	private static final String OSGI_WS = "osgi.ws"; //$NON-NLS-1$
	private static final String OSGI_NL = "osgi.nl"; //$NON-NLS-1$
	private static final String OSGI_ARCH = "osgi.arch"; //$NON-NLS-1$
	public static final String[] PROPS = {OSGI_OS, OSGI_WS, OSGI_NL, OSGI_ARCH, Constants.FRAMEWORK_SYSTEMPACKAGES, Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, OSGI_RESOLVER_MODE, Constants.FRAMEWORK_EXECUTIONENVIRONMENT, "osgi.resolveOptional", "osgi.genericAliases", Constants.FRAMEWORK_OS_NAME, Constants.FRAMEWORK_OS_VERSION, Constants.FRAMEWORK_PROCESSOR, Constants.FRAMEWORK_LANGUAGE, STATE_SYSTEM_BUNDLE, Constants.FRAMEWORK_SYSTEMCAPABILITIES, Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA}; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String OSGI_EE_NAMESPACE = "osgi.ee"; //$NON-NLS-1$

	
}
