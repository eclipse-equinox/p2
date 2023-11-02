/*******************************************************************************
 * Copyright (c) 2023 Eclipse contributors and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine.phases;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.cert.Certificate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.net.ssl.SSLPeerUnverifiedException;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.internal.p2.engine.Messages;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.metadata.spi.IInstallableUnitUIServices;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Checks the originating sources of all installable units, checking if they
 * originate from trusted authorities, and reports that information to the user
 * for review and approval.
 */
public class AuthorityChecker {

	public static final String TRUST_ALL_AUTHORITIES = "trustAllAuthorities"; //$NON-NLS-1$

	public static final String TRUSTED_AUTHORITIES_PROPERTY = "trustedAuthorities"; //$NON-NLS-1$

	/** Supports extracting the underlying URI of an archive URI. */
	private static final Pattern ARCHIVE_URI_PATTERN = Pattern.compile("(?i)(jar|zip|archive):(.*)?/(.*)"); //$NON-NLS-1$

	/** Supports extracting the authority chain of a URII. */
	private static final Pattern HIERARCHICAL_URI_PATTERN = Pattern
			.compile("((?:[^/:]+):(?://[^/]+|///|/)?)([^?#]*)([#?].*)?"); //$NON-NLS-1$

	private final IProvisioningAgent agent;
	private final ProvisioningContext context;
	private final IProfile profile;
	private final Collection<? extends IInstallableUnit> ius;
	private final Collection<? extends IArtifactKey> artifacts;

	/**
	 * Creates an instance that can only be used for managing profiles preferences.
	 */
	public AuthorityChecker(IProvisioningAgent agent, IProfile profile) {
		this(agent, null, List.of(), List.of(), profile);
	}

	/**
	 * Creates an instance that can actually do the checking.
	 */
	AuthorityChecker(IProvisioningAgent agent, ProvisioningContext context, Collection<? extends IInstallableUnit> ius,
			Collection<? extends IArtifactKey> artifacts, IProfile profile) {
		this.agent = agent;
		this.context = context;
		this.ius = ius;
		this.artifacts = artifacts;
		this.profile = profile;
	}

	IStatus start(IProgressMonitor monitor) {
		if (isTrustAlways()) {
			return Status.OK_STATUS;
		}

		var uiServices = agent.getService(UIServices.class);
		if (uiServices instanceof IInstallableUnitUIServices installableUnitUIServices) {
			var iuSources = context.getInstallableUnitSources(ius, monitor);
			var artifactSources = context.getArtifactSources(artifacts, monitor);

			// Build a map from each source IU to the keys it provides.
			var artifactKeyIUs = new LinkedHashMap<IArtifactKey, Set<IInstallableUnit>>();
			iuSources.values().stream().flatMap(Set::stream).forEach(iu -> {
				for (var key : iu.getArtifacts()) {
					artifactKeyIUs.computeIfAbsent(key, it -> new HashSet<>()).add(iu);
				}
			});

			// For each artifact key, include the IUs that provide it in the IU sources map.
			for (var entry : artifactSources.entrySet()) {
				var location = entry.getKey();
				for (var artifact : entry.getValue()) {
					var artifactIUs = artifactKeyIUs.get(artifact);
					if (artifactIUs != null) {
						iuSources.computeIfAbsent(location, it -> new TreeSet<>()).addAll(artifactIUs);
					}
				}
			}

			var preferenceTrustedAuthorities = getPreferenceTrustedAuthorities();
			var allTrustedAuthorities = new TreeSet<>(preferenceTrustedAuthorities);

			// Remove all trusted sources from consideration.
			iuSources.keySet().removeIf(source -> {
				for (var uri : getAuthorityChain(source)) {
					if ("file".equalsIgnoreCase(uri.getScheme())) { //$NON-NLS-1$
						return true;
					}
					if (allTrustedAuthorities.contains(uri)) {
						return true;
					}
				}
				return false;
			});

			var certificates = getCertificates(iuSources.keySet(), monitor);
			var trustInfo = installableUnitUIServices.getTrustAuthorityInfo(iuSources, certificates);

			setTrustAlways(trustInfo.isTrustAlways());

			if (!isTrustAlways()) {
				if (trustInfo.isSave()) {
					preferenceTrustedAuthorities.addAll(trustInfo.getTrustedAuthorities());
					persistTrustedAuthorities(preferenceTrustedAuthorities);
				}

				var trustedAuthorities = trustInfo.getTrustedAuthorities();
				iuSources.keySet().removeIf(source -> {
					for (var uri : getAuthorityChain(source)) {
						if (trustedAuthorities.contains(uri)) {
							return true;
						}
					}
					return false;
				});

				if (!iuSources.isEmpty()) {
					return new Status(IStatus.CANCEL, EngineActivator.ID,
							Messages.AuthorityChecker_UntrustedAuthorities);
				}
			}
		}

		return Status.OK_STATUS;
	}

	public boolean isTrustAlways() {
		var preferences = getEnngineProfilePreferences();
		if (preferences != null) {
			return preferences.getBoolean(TRUST_ALL_AUTHORITIES, false);
		}
		return false;
	}

	public IStatus setTrustAlways(boolean trustAlways) {
		var preferences = getEnngineProfilePreferences();
		if (preferences != null) {
			try {
				preferences.putBoolean(TRUST_ALL_AUTHORITIES, trustAlways);
				preferences.flush();
			} catch (BackingStoreException ex) {
				return new Status(IStatus.ERROR, EngineActivator.ID, ex.getMessage(), ex);
			}
		}
		return Status.OK_STATUS;
	}

	@SuppressWarnings("nls")
	public Set<URI> getPreferenceTrustedAuthorities() {
		var result = new LinkedHashSet<URI>();
		var preferences = getEnngineProfilePreferences();
		if (preferences != null) {
			String defaultValue = EngineActivator.getContext().getProperty("p2.trustedAuthorities");
			if (defaultValue == null) {
				defaultValue = "https://download.eclipse.org https://archive.eclipse.org";
			} else {
				defaultValue = defaultValue.replace(',', ' ');
			}
			for (var value : preferences.get(TRUSTED_AUTHORITIES_PROPERTY, defaultValue).split("\\s+")) {
				try {
					if (!value.isBlank()) {
						result.add(new URI(value));
					}
				} catch (URISyntaxException e) {
					//$FALL-THROUGH$
				}
			}
		}
		return result;
	}

	@SuppressWarnings("nls")
	public IStatus persistTrustedAuthorities(Collection<? extends URI> trustedAuthorities) {
		var preferences = getEnngineProfilePreferences();
		if (preferences != null) {
			try {
				preferences.put(TRUSTED_AUTHORITIES_PROPERTY,
						String.join(" ", getFilteredAuthorities(trustedAuthorities).stream().map(URI::toString)
								.collect(Collectors.toList())));
				preferences.flush();
			} catch (BackingStoreException ex) {
				return new Status(IStatus.ERROR, EngineActivator.ID, ex.getMessage(), ex);
			}
		}
		return Status.OK_STATUS;
	}

	public IEclipsePreferences getEnngineProfilePreferences() {
		if (profile != null) {
			var profileScope = new ProfileScope(agent.getService(IAgentLocation.class), profile.getProfileId());
			return profileScope.getNode(EngineActivator.ID);
		}
		return null;
	}

	public static List<URI> getFilteredAuthorities(Collection<? extends URI> authorities) {
		var filteredAuthorities = new ArrayList<URI>(authorities);
		filteredAuthorities.removeIf(uri -> {
			for (var authority : getAuthorityChain(uri)) {
				if (!authority.equals(uri) && authorities.contains(authority)) {
					return true;
				}
			}
			return false;
		});
		return filteredAuthorities;
	}

	public static Map<URI, List<Certificate>> getCertificates(Collection<? extends URI> uris,
			IProgressMonitor monitor) {
		var certificates = new TreeMap<URI, List<Certificate>>();
		var authorities = new TreeMap<URI, List<Certificate>>();
		for (var uri : uris) {
			var authorityURI = getAuthorityChain(uri).get(0);
			certificates.put(uri, authorities.computeIfAbsent(authorityURI, it -> new ArrayList<>()));
		}
		gatherCertificates(authorities, monitor);
		return certificates;
	}

	public static void gatherCertificates(Map<URI, List<Certificate>> authorities, IProgressMonitor montior) {
		var client = HttpClient.newBuilder().build();
		var requests = authorities.keySet().stream().collect(Collectors.toMap(Function.identity(), uri -> {
			try {
				return Optional.of(client.sendAsync(
						HttpRequest.newBuilder().uri(uri).method("HEAD", BodyPublishers.noBody()).build(), //$NON-NLS-1$
						BodyHandlers.ofString()));
			} catch (RuntimeException ex) {
				return Optional.<CompletableFuture<HttpResponse<String>>>ofNullable(null);
			}
		}));

		for (var entry : authorities.entrySet()) {
			if (montior.isCanceled()) {
				return;
			}
			requests.get(entry.getKey()).ifPresent(httpResponse -> {
				try {
					httpResponse.get().sslSession().ifPresent(sslSession -> {
						try {
							var peerCertificates = sslSession.getPeerCertificates();
							entry.getValue().addAll(Arrays.asList(peerCertificates));
						} catch (SSLPeerUnverifiedException e) {
							//$FALL-THROUGH$
						}
					});
				} catch (RuntimeException | InterruptedException | ExecutionException e) {
					//$FALL-THROUGH$
				}
			});
		}
	}

	/**
	 * Returns a list of URIs representing the hierarchical chain, starting from the
	 * root authority, to the given location. The location will always be present as
	 * the last element in the list. The first element represents the root
	 * authority.
	 *
	 * <ul>
	 * <li>file:/C:/folder/file</li>
	 * <ul>
	 * <li>file:/</li>
	 * <li>file:/C:/</li>
	 * <li>file:/C:/folder/</li>
	 * </ul>
	 * <li>https://host/folder/file</li>
	 * <ul>
	 * <li>https://host</li>
	 * <li>https://host/folder/</li>
	 * <li>https://host/folder/file</li>
	 * </ul>
	 * <li>jar:https://host/folder/file.jar!/path</li>
	 * <ul>
	 * <li>https://host</li>
	 * <li>https://host/folder/</li>
	 * <li>https://host/folder/file.jar</li>
	 * <li>jar:https://host/folder/file.jar!/path</li>
	 * </ul>
	 * </ul>
	 *
	 * @param location an arbitrary location.
	 * @return a URI representing the underlying authority of the given location.
	 */
	public static List<URI> getAuthorityChain(URI location) {
		var result = new ArrayList<URI>();
		if (location.isOpaque()) {
			var matcher = ARCHIVE_URI_PATTERN.matcher(location.toString());
			if (matcher.matches()) {
				result.addAll(getAuthorityChain(URI.create(matcher.group(2))));
			}
		} else {
			var scheme = location.getScheme();
			if (scheme != null) {
				try {
					var matcher = HIERARCHICAL_URI_PATTERN.matcher(location.toString());
					if (matcher.matches()) {
						String authority = matcher.group(1);
						String path = matcher.group(2);
						String tail = matcher.group(3);
						if (path != null && !path.isEmpty() || tail != null) {
							var uri = new URI(authority);
							result.add(uri);
							var length = path.length();
							for (var index = path.indexOf('/', 1); index != -1
									&& (index == 1 || ++index != length); index = path.indexOf('/', index + 1)) {
								String segment = path.substring(0, index);
								var uri2 = new URI(authority + segment);
								result.add(uri2);
							}
							if (tail != null) {
								var uri2 = new URI(authority + path);
								result.add(uri2);
							}
						}
					}
				} catch (URISyntaxException e) {
					//$FALL-THROUGH$
				}
			}
		}
		result.add(location);
		return result;
	}

//	@SuppressWarnings("nls")
//	public static void main(String[] args) {
//		System.err.println(getAuthorityChain(URI.create("file:/C:/folder/file")));
//		System.err.println(getAuthorityChain(URI.create("https://host/folder/file")));
//		System.err.println(getAuthorityChain(URI.create("jar:https://host/folder/file.jar!/path")));
//		System.err.println(getAuthorityChain(URI.create("https://host/folder/file?query")));
//		System.err.println(getAuthorityChain(URI.create("https://%41")));
//		System.err.println(getAuthorityChain(URI.create("https://%41/")));
//		System.err.println(getAuthorityChain(URI.create("file:///")));
//		System.err.println(getAuthorityChain(URI.create("file:////")));
//		System.err.println(getAuthorityChain(URI.create("file://///")));
//		System.err.println(getAuthorityChain(URI.create("file:///a/b/")));
//		System.err.println(getAuthorityChain(URI.create("file:/a/b")));
//		System.err.println(getAuthorityChain(URI.create("file:/C:/")));
//		System.err.println(getAuthorityChain(URI.create("file:/C:/a/b")));
//		System.err.println(getAuthorityChain(URI.create("file://C:/a/b")));
//		System.err.println(getAuthorityChain(URI.create("file:///C:/a/b")));
//		System.err.println(getAuthorityChain(URI.create("https://%41/")));
//		System.err.println(getAuthorityChain(URI.create("https://%41/%20/?x")));
//		System.err.println(getAuthorityChain(URI.create("https://%41/%20/")));
//		System.err.println(getAuthorityChain(URI.create("https://%41/group/%20/")));
//		System.err.println(getAuthorityChain(URI.create("https://%41/%20")));
//		System.err.println(getAuthorityChain(URI.create("https://%41/%20#")));
//		System.err.println(getAuthorityChain(URI.create("jar:https://%41/%20!/sadfsda")));
//		System.err.println(getAuthorityChain(URI.create("jar:https://%41/%20/x.jar!/sadfsda")));
//		System.err.println(getAuthorityChain(URI.create("a/b?x")));
//	}

}
