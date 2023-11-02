/*******************************************************************************
 *  Copyright (c) 2007, 2022 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - transport split
 *     Christoph Läubrich - Issue #6 - Deprecate Transport.download(URI, OutputStream, long, IProgressMonitor)
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.repository;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.repository.IStateful;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.prefs.BackingStoreException;

public abstract class Transport {

	public static final String SERVICE_NAME = Transport.class.getName();

	/**
	 * Rules for how to handle a URI scheme's.
	 */
	public enum ProtocolRule {
		/**
		 * Allow the scheme to be used as is.
		 */
		ALLOW,
		/**
		 * Redirect to a secure variant of the scheme.
		 */
		REDIRECT,
		/**
		 * Disallow the scheme.
		 */
		BLOCK;

		public static ProtocolRule of(String literal) {
			if (literal == null) {
				return REDIRECT;
			}
			switch (literal) {
			case "allow": //$NON-NLS-1$
				return ALLOW;
			case "redirect": //$NON-NLS-1$
				return REDIRECT;
			default: {
				return BLOCK;
			}
			}
		}
	}

	private static final Map<String, ProtocolRule> RULES = Map.of( //
			"http", ProtocolRule.of(System.getProperty("p2.httpRule")), //$NON-NLS-1$ //$NON-NLS-2$
			"ftp", ProtocolRule.of(System.getProperty("p2.ftpRule"))); //$NON-NLS-1$ //$NON-NLS-2$

	/** Allows to mute "Using unsafe http transport" warnings */
	private static final boolean SKIP_REPOSITORY_PROTOCOL_CHECK = Boolean.getBoolean("p2.skipRepositoryProtocolCheck"); //$NON-NLS-1$

	/** Supports extracting the underlying URI of an archive URI. */
	private static final Pattern ARCHIVE_URI_PATTERN = Pattern.compile("(?i)(jar|zip|archive):(.*)!/(.*)"); //$NON-NLS-1$

	private static IEclipsePreferences getTransportPreferences() {
		return (IEclipsePreferences) ConfigurationScope.INSTANCE.getNode(Activator.ID).node("protocolRules"); //$NON-NLS-1$
	}

	/** Avoids repeated logging the same URI. */
	private final Set<URI> loggedURIs = ConcurrentHashMap.newKeySet();

	/**
	 * Perform a download, writing into the target output stream. Progress is
	 * reported on the monitor. If the <code>target</code> is an instance of
	 * {@link IStateful} the resulting status is also set on the target. An
	 * IStateful target is updated with status even if this methods throws
	 * {@link OperationCanceledException}.
	 *
	 * @return IStatus, that is a {@link DownloadStatus} on success.
	 * @param toDownload URI of file to download
	 * @param target     OutputStream where result is written
	 * @param startPos   the starting position of the download, or -1 for from start
	 * @param monitor    where progress should be reported
	 * @throws OperationCanceledException if the operation was canceled.
	 * @deprecated this method is actually never called from P2 code and thus
	 *             deprecated, callers should migrate to
	 *             {@link #download(URI, OutputStream, IProgressMonitor)},
	 *             implementors should simply remove this and rely on the default
	 *             implementation
	 */
	@Deprecated(forRemoval = true)
	public IStatus download(URI toDownload, OutputStream target, long startPos, IProgressMonitor monitor) {
		if (startPos <= 0) {
			return download(toDownload, target, monitor);
		}
		throw new UnsupportedOperationException(
				"positional downloads are actually never called from P2 code and thus disabled by default, please use the method without a position instead"); //$NON-NLS-1$
	}

	/**
	 * Perform a download, writing into the target output stream. Progress is
	 * reported on the monitor. If the <code>target</code> is an instance of
	 * {@link IStateful} the resulting status is also set on the target.
	 *
	 * @return IStatus, that is a {@link DownloadStatus} on success.
	 * @param toDownload URI of file to download
	 * @param target     OutputStream where result is written
	 * @param monitor    where progress should be reported
	 * @throws OperationCanceledException if the operation was canceled.
	 */
	public abstract IStatus download(URI toDownload, OutputStream target, IProgressMonitor monitor);

	/**
	 * Perform a stream download, writing into an InputStream that is returned.
	 * Performs authentication if needed.
	 *
	 * @return InputStream a stream with the content from the toDownload URI, or
	 *         null
	 * @param toDownload URI of file to download
	 * @param monitor    monitor checked for cancellation
	 * @throws OperationCanceledException    if the operation was canceled.
	 * @throws AuthenticationFailedException if authentication failed, or too many
	 *                                       attempt were made
	 * @throws FileNotFoundException         if the toDownload was reported as non
	 *                                       existing
	 * @throws CoreException                 on errors
	 */
	public abstract InputStream stream(URI toDownload, IProgressMonitor monitor)
			throws FileNotFoundException, CoreException, AuthenticationFailedException;

	/**
	 * Returns the last modified date for a URI. A last modified of 0 typically
	 * indicates that the server response is wrong, but should not be interpreted as
	 * a file not found.
	 *
	 * @param toDownload
	 * @param monitor
	 * @throws OperationCanceledException if the operation was canceled by the user.
	 * @return last modified date (possibly 0)
	 */
	public abstract long getLastModified(URI toDownload, IProgressMonitor monitor)
			throws CoreException, FileNotFoundException, AuthenticationFailedException;

	/**
	 * Returns the corresponding secure location given an arbitrary location.
	 * Subclasses are encouraged to use this method, to ensure that only secure
	 * locations are accessed by the transport implementation.
	 * <p>
	 * System properties affect the {@link #getDefaultProtocolRules() default}
	 * behavior:
	 * </p>
	 * <ul>
	 * <li>p2.httpRule</li>
	 * <ul>
	 * <li>redirect http:// -&gt; https://</li>
	 * <li>allow http:// -&gt; http://</li>
	 * <li>block http:// -&gt; CoreException</li>
	 * </ul>
	 * <li>p2.ftpRule</li>
	 * <ul>
	 * <li>redirect ftp:// -&gt; ftps://</li>
	 * <li>allow ftp:// -&gt; ftp://</li>
	 * <li>block ftp:// -&gt; CoreException</li>
	 * </ul>
	 * </ul>
	 *
	 * @param location an arbitrary location.
	 * @return the corresponding secure location or the location itself.
	 * @throws CoreException if the location URI is considered unacceptably
	 *                       insecure.
	 *
	 * @see #getProtocolRules()
	 * @see #getDefaultProtocolRules()
	 */
	public URI getSecureLocation(URI location) throws CoreException {
		String scheme = location.getScheme();
		String canonicalScheme = scheme == null ? "null" : scheme.toLowerCase(); //$NON-NLS-1$
		ProtocolRule protocolRule = getProtocolRule(canonicalScheme); // $NON-NLS-1$
		if (protocolRule != null) { // $NON-NLS-1$
			switch (protocolRule) {
			case REDIRECT: {
				try {
					return new URI(canonicalScheme + "s", location.getUserInfo(), location.getHost(), //$NON-NLS-1$
							location.getPort(), location.getPath(), location.getQuery(), location.getFragment());
				} catch (URISyntaxException e) {
					// Cannot happen.
					throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, e.getLocalizedMessage(), e));
				}
			}
			case BLOCK: {
				throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID,
						NLS.bind(Messages.RepositoryTransport_unsafeProtocolBlocked, canonicalScheme, location)));
			}
			default: {
				if (!SKIP_REPOSITORY_PROTOCOL_CHECK) {
					if (loggedURIs.add(location)) {
						LogHelper.log(new Status(IStatus.WARNING, Activator.ID,
								NLS.bind(Messages.RepositoryTransport_unsafeProtocol, canonicalScheme, location)));
					}
				}
			}
			}
		} else {
			Matcher matcher = ARCHIVE_URI_PATTERN.matcher(location.toString());
			if (matcher.matches()) {
				return URI.create(matcher.group(1) + ':'
						+ getSecureLocation(URI.create(matcher.group(2) + "!/" + matcher.group(3)))); //$NON-NLS-1$
			}
		}
		return location;
	}

	/**
	 * Returns the protocol rule, if any, associated with the given scheme. The
	 * default implementation first considers the {@link #getProtocolRules()
	 * preferred protocol rules}, and then the {@link #getDefaultProtocolRules()
	 * default protocol rules}.
	 *
	 * @param scheme the scheme in question.
	 * @return the protocol rule, if any, associated with the given scheme.
	 *
	 * @see #getSecureLocation(URI)
	 */
	protected ProtocolRule getProtocolRule(String scheme) {
		ProtocolRule result = getProtocolRules().get(scheme);
		if (result == null) {
			result = getDefaultProtocolRules().get(scheme);
		}
		return result;
	}

	/**
	 * Returns the known default protocol rules.
	 *
	 * @return the known default protocol rules.
	 */
	public Map<String, ProtocolRule> getDefaultProtocolRules() {
		return RULES;
	}

	/**
	 * Returns the preferred protocol rules.
	 *
	 * @return the preferred protocol rules.
	 */
	public Map<String, ProtocolRule> getProtocolRules() {
		var rules = new TreeMap<String, ProtocolRule>();
		IEclipsePreferences transportPreferences = getTransportPreferences();
		for (String key : RULES.keySet()) {
			String value = transportPreferences.get(key, null);
			rules.put(key, value == null ? null : ProtocolRule.of(value));
		}
		return rules;
	}

	/**
	 * Updates the preferred protocol rules.
	 *
	 * @param rules the new preferred protocol rules.
	 */
	public void setProtocolRules(Map<String, ProtocolRule> rules) {
		IEclipsePreferences transportPreferences = getTransportPreferences();
		for (var entry : rules.entrySet()) {
			String key = entry.getKey();
			ProtocolRule value = entry.getValue();
			if (value == null) {
				transportPreferences.remove(key);
			} else {
				transportPreferences.put(key, value.toString().toLowerCase());
			}
		}

		try {
			transportPreferences.flush();
		} catch (BackingStoreException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, e.getLocalizedMessage(), e));
		}
	}
}
