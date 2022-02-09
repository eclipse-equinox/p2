/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine.phases;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.equinox.internal.p2.artifact.processors.pgp.PGPPublicKeyStore;
import org.eclipse.equinox.internal.p2.artifact.processors.pgp.PGPSignatureVerifier;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.core.UIServices.TrustInfo;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.ProfileScope;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.IArtifactUIServices;
import org.eclipse.equinox.p2.repository.spi.PGPPublicKeyService;
import org.eclipse.osgi.service.security.TrustEngine;
import org.eclipse.osgi.signedcontent.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Checks the certificates or PGP signatures on a set of files or artifacts and
 * reports back any problems with unsigned artifacts, untrusted certificates, or
 * tampered content.
 */
public class CertificateChecker {
	private static final String DEBUG_PREFIX = "certificate checker"; //$NON-NLS-1$

	public static final String TRUST_ALWAYS_PROPERTY = "trustAlways"; //$NON-NLS-1$

	public static final String TRUSTED_KEY_STORE_PROPERTY = "pgp.trustedPublicKeys"; //$NON-NLS-1$

	/***
	 * Store the optional profile for PGP key handling
	 */
	private IProfile profile;

	/**
	 * Stores artifacts to check
	 */
	private Map<IArtifactDescriptor, File> artifacts = new HashMap<>();
	private final IProvisioningAgent agent;
	private final PGPPublicKeyService keyService;

	// Lazily loading
	private Supplier<PGPPublicKeyStore> trustedKeys = new Supplier<>() {
		private PGPPublicKeyStore cache = null;

		public PGPPublicKeyStore get() {
			if (cache == null) {
				cache = getPreferenceTrustedKeys();
				getContributedTrustedKeys().keySet().forEach(cache::addKey);
			}
			return cache;
		}
	};

	public CertificateChecker() {
		this(null);
	}

	public CertificateChecker(IProvisioningAgent agent) {
		this.agent = agent;
		artifacts = new HashMap<>();
		keyService = agent.getService(PGPPublicKeyService.class);

	}

	public IStatus start() {
		final BundleContext context = EngineActivator.getContext();
		ServiceReference<SignedContentFactory> contentFactoryRef = context
				.getServiceReference(SignedContentFactory.class);
		SignedContentFactory verifierFactory = context.getService(contentFactoryRef);
		try {
			return checkCertificates(verifierFactory);
		} finally {
			context.ungetService(contentFactoryRef);
		}
	}

	private IStatus checkCertificates(SignedContentFactory verifierFactory) {
		if (artifacts.isEmpty()) {
			return Status.OK_STATUS;
		}

		UIServices serviceUI = agent.getService(UIServices.class);
		if (serviceUI == null) {
			return Status.OK_STATUS;
		}

		if (isTrustAlways()) {
			return Status.OK_STATUS;
		}

		IArtifactUIServices artifactServiceUI = serviceUI instanceof IArtifactUIServices
				? (IArtifactUIServices) serviceUI
				: (untrustedCertificateChains, untrustedPGPKeys, unsignedArtifacts,
						artifactFiles) -> IArtifactUIServices.getTrustInfo(serviceUI, untrustedCertificateChains,
								untrustedPGPKeys, unsignedArtifacts, artifactFiles);

		Map<List<Certificate>, Set<IArtifactKey>> untrustedCertificates = new LinkedHashMap<>();
		Map<PGPPublicKey, Set<IArtifactKey>> untrustedPGPKeys = new LinkedHashMap<>();
		Set<IArtifactKey> unsignedArtifacts = new LinkedHashSet<>();
		Set<PGPPublicKey> trustedKeySet = new HashSet<>();
		boolean isTrustedKeySetInitialized = false;
		Map<IArtifactKey, File> artifactFiles = new LinkedHashMap<>();
		for (Entry<IArtifactDescriptor, File> artifact : artifacts.entrySet()) {
			IArtifactDescriptor artifactDescriptor = artifact.getKey();
			IArtifactKey artifactKey = artifactDescriptor.getArtifactKey();
			File artifactFile = artifact.getValue();
			artifactFiles.put(artifactKey, artifactFile);
			try {
				SignedContent content = verifierFactory.getSignedContent(artifactFile);
				if (content.isSigned()) {
					SignerInfo[] signerInfo = content.getSignerInfos();
					// Only record the untrusted elements if there are no trusted elements.
					if (Arrays.stream(signerInfo).noneMatch(SignerInfo::isTrusted)) {
						for (SignerInfo element : signerInfo) {
							if (!element.isTrusted()) {
								List<Certificate> certificateChain = Arrays.asList(element.getCertificateChain());
								untrustedCertificates.computeIfAbsent(certificateChain, key -> new LinkedHashSet<>())
										.add(artifactKey);
							}
						}
					}
				} else {
					// The keys are in this destination artifact's properties if and only if the
					// PGPSignatureVerifier verified the signatures against these keys.
					List<PGPPublicKey> verifiedKeys = PGPPublicKeyStore
							.readPublicKeys(
									artifactDescriptor.getProperty(PGPSignatureVerifier.PGP_SIGNER_KEYS_PROPERTY_NAME))
							.stream().map(keyService::addKey).collect(Collectors.toList());
					if (!verifiedKeys.isEmpty()) {
						if (!isTrustedKeySetInitialized) {
							isTrustedKeySetInitialized = true;
							trustedKeySet.addAll(trustedKeys.get().all());
						}
						// Only record the untrusted keys if none of the keys are trusted.
						if (verifiedKeys.stream().noneMatch(trustedKeySet::contains)) {
							verifiedKeys.forEach(key -> untrustedPGPKeys
									.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(artifactKey));
						}
					} else {
						unsignedArtifacts.add(artifactKey);
					}
				}
			} catch (GeneralSecurityException e) {
				return new Status(IStatus.ERROR, EngineActivator.ID, Messages.CertificateChecker_SignedContentError, e);
			} catch (IOException e) {
				return new Status(IStatus.ERROR, EngineActivator.ID, Messages.CertificateChecker_SignedContentIOError,
						e);
			}
		}

		// log the unsigned artifacts if requested
		if (DebugHelper.DEBUG_CERTIFICATE_CHECKER_UNSIGNED && !unsignedArtifacts.isEmpty()) {
			StringBuilder message = new StringBuilder("The following artifacts are unsigned:\n"); //$NON-NLS-1$
			for (IArtifactKey key : unsignedArtifacts) {
				message.append(NLS.bind("  {0}\n", artifactFiles.get(key).getPath())); //$NON-NLS-1$
			}
			DebugHelper.debug(DEBUG_PREFIX, message.toString());
		}

		// log the untrusted certificates if requested
		if (DebugHelper.DEBUG_CERTIFICATE_CHECKER_UNTRUSTED && !untrustedCertificates.isEmpty()) {
			StringBuilder message = new StringBuilder("The following certificates are untrusted:\n"); //$NON-NLS-1$
			for (Map.Entry<List<Certificate>, Set<IArtifactKey>> entry : untrustedCertificates.entrySet()) {
				message.append(entry.getKey().get(0).toString() + "\n"); //$NON-NLS-1$
				message.append("  used by the following artifacts:\n"); //$NON-NLS-1$
				for (IArtifactKey key : entry.getValue()) {
					message.append(NLS.bind("    {0}\n", artifactFiles.get(key).getPath())); //$NON-NLS-1$
				}

			}
			DebugHelper.debug(DEBUG_PREFIX, message.toString());
		}

		if (DebugHelper.DEBUG_CERTIFICATE_CHECKER_UNTRUSTED && !untrustedPGPKeys.isEmpty()) {
			StringBuilder message = new StringBuilder("The following PGP Keys are untrusted:\n"); //$NON-NLS-1$
			for (Map.Entry<PGPPublicKey, Set<IArtifactKey>> entry : untrustedPGPKeys.entrySet()) {
				message.append(Long.toHexString(entry.getKey().getKeyID()) + "\n"); //$NON-NLS-1$
				message.append("  used by the following artifacts:\n"); //$NON-NLS-1$
				for (IArtifactKey key : entry.getValue()) {
					message.append(NLS.bind("    {0}\n", key)); //$NON-NLS-1$
				}
			}
			DebugHelper.debug(DEBUG_PREFIX, message.toString());
		}

		String policy = getUnsignedContentPolicy();
		// if there is unsigned content and we should never allow it, then fail without
		// further checking certificates
		if (!unsignedArtifacts.isEmpty() && EngineActivator.UNSIGNED_FAIL.equals(policy)) {
			return new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(
					Messages.CertificateChecker_UnsignedNotAllowed,
					unsignedArtifacts.stream().map(key -> artifactFiles.get(key)).collect(Collectors.toList())));
		}

		// If there was no unsigned content, and nothing untrusted, no need to prompt.
		if ((EngineActivator.UNSIGNED_ALLOW.equals(policy) || unsignedArtifacts.isEmpty())
				&& untrustedCertificates.isEmpty() && untrustedPGPKeys.isEmpty()) {
			return Status.OK_STATUS;
		}

		TrustInfo trustInfo = artifactServiceUI.getTrustInfo(untrustedCertificates, untrustedPGPKeys, unsignedArtifacts,
				artifactFiles);

		setTrustAlways(trustInfo.trustAlways());

		if (!trustInfo.trustAlways()) {
			// If there is unsigned content and user doesn't trust unsigned content, cancel
			// the operation.
			if (!unsignedArtifacts.isEmpty() && !trustInfo.trustUnsignedContent()) {
				return Status.CANCEL_STATUS;
			}

			Certificate[] trustedCertificates = trustInfo.getTrustedCertificates();
			// If we had untrusted chains and nothing was trusted, cancel the operation
			if (!untrustedCertificates.isEmpty() && trustedCertificates == null) {
				return new Status(IStatus.CANCEL, EngineActivator.ID, Messages.CertificateChecker_CertificateRejected);
			}

			// Anything that was trusted should be removed from the untrusted list
			if (trustedCertificates != null) {
				List<Certificate> trustedCertificateList = Arrays.asList(trustedCertificates);
				untrustedCertificates.keySet().removeIf(it -> trustedCertificateList.contains(it.get(0)));
			}

			trustedKeySet.addAll(trustInfo.getTrustedPGPKeys());

			Set<IArtifactKey> trustedArtifactKeys = trustedKeySet.stream().map(untrustedPGPKeys::get)
					.filter(Objects::nonNull).flatMap(Set::stream).collect(Collectors.toSet());
			untrustedPGPKeys.values().forEach(it -> it.removeAll(trustedArtifactKeys));
			untrustedPGPKeys.values().removeIf(Collection::isEmpty);

			// If there is still untrusted content, cancel the operation
			if (!untrustedCertificates.isEmpty() || !untrustedPGPKeys.isEmpty()) {
				return new Status(IStatus.CANCEL, EngineActivator.ID, Messages.CertificateChecker_CertificateRejected);
			}
		}

		// If we should persist the trusted certificates, add them to the trust engine
		if (trustInfo.persistTrust()) {
			IStatus certificatesStatus = trustInfo.getTrustedCertificates().length == 0 ? null
					: persistTrustedCertificates(trustInfo.getTrustedCertificates());
			PGPPublicKeyStore keyStore = new PGPPublicKeyStore();
			trustInfo.getTrustedPGPKeys().forEach(keyStore::addKey);

			IStatus pgpStatus = trustInfo.getTrustedPGPKeys().isEmpty() ? null : persistTrustedKeys(keyStore);
			if (pgpStatus == null) {
				return certificatesStatus;
			}
			if (certificatesStatus == null) {
				return pgpStatus;
			}

			return new MultiStatus(getClass(), IStatus.OK, new IStatus[] { pgpStatus, certificatesStatus },
					pgpStatus.getMessage() + '\n' + certificatesStatus.getMessage(), null);
		}

		return Status.OK_STATUS;
	}

	private IStatus persistTrustedCertificates(Certificate[] trustedCertificates) {
		if (trustedCertificates == null)
			// I'm pretty sure this would be a bug; trustedCertificates should never be null
			// here.
			return new Status(IStatus.INFO, EngineActivator.ID, Messages.CertificateChecker_CertificateRejected);
		ServiceTracker<TrustEngine, TrustEngine> trustEngineTracker = new ServiceTracker<>(EngineActivator.getContext(),
				TrustEngine.class, null);
		trustEngineTracker.open();
		Object[] trustEngines = trustEngineTracker.getServices();
		try {
			if (trustEngines == null)
				return null;
			for (Certificate trustedCertificate : trustedCertificates) {
				for (Object engine : trustEngines) {
					TrustEngine trustEngine = (TrustEngine) engine;
					if (trustEngine.isReadOnly())
						continue;
					try {
						trustEngine.addTrustAnchor(trustedCertificate, trustedCertificate.toString());
						// this should mean we added an anchor successfully; continue to next
						// certificate
						break;
					} catch (IOException e) {
						// just return an INFO so the user can proceed with the install
						return new Status(IStatus.INFO, EngineActivator.ID,
								Messages.CertificateChecker_KeystoreConnectionError, e);
					} catch (GeneralSecurityException e) {
						return new Status(IStatus.INFO, EngineActivator.ID,
								Messages.CertificateChecker_CertificateError, e);
					}
				}
			}
		} finally {
			trustEngineTracker.close();
		}
		return Status.OK_STATUS;
	}

	/**
	 * Return the policy on unsigned content.
	 */
	private String getUnsignedContentPolicy() {
		String policy = EngineActivator.getContext().getProperty(EngineActivator.PROP_UNSIGNED_POLICY);
		if (policy == null)
			policy = EngineActivator.UNSIGNED_PROMPT;
		return policy;

	}

	public void setProfile(IProfile profile) {
		this.profile = profile;
	}

	public void add(Map<IArtifactDescriptor, File> toAdd) {
		artifacts.putAll(toAdd);
	}

	public Map<PGPPublicKey, Set<Bundle>> getContributedTrustedKeys() {
		// Build the map based on fingerprints to properly avoid duplicates as we
		// accumulate the full set of keys.
		Map<ByteBuffer, Set<Bundle>> keys = new LinkedHashMap<>();

		// Load from the extension registry.
		for (IConfigurationElement extension : RegistryFactory.getRegistry()
				.getConfigurationElementsFor(EngineActivator.ID, "pgp")) { //$NON-NLS-1$
			if ("trustedKeys".equals(extension.getName())) { //$NON-NLS-1$
				String pathInBundle = extension.getAttribute("path"); //$NON-NLS-1$
				if (pathInBundle != null) {
					String name = extension.getContributor().getName();
					Stream.of(EngineActivator.getContext().getBundles())
							.filter(bundle -> bundle.getSymbolicName().equals(name)).findAny().ifPresent(bundle -> {
								URL keyURL = bundle.getEntry(pathInBundle);
								try (InputStream stream = keyURL.openStream()) {
									PGPPublicKeyStore.readPublicKeys(stream).stream().map(keyService::addKey)
											.forEach(key -> keys.computeIfAbsent(ByteBuffer.wrap(key.getFingerprint()),
													k -> new LinkedHashSet<>()).add(bundle));
								} catch (IOException e) {
									DebugHelper.debug(DEBUG_PREFIX, e.getMessage());
								}
							});
				}
			}
		}

		Map<PGPPublicKey, Set<Bundle>> result = keys.entrySet().stream()
				.collect(Collectors.toMap(entry -> keyService.getKey(entry.getKey().array()), Map.Entry::getValue));
		return result;
	}

	public boolean isTrustAlways() {
		if (profile != null) {
			ProfileScope profileScope = new ProfileScope(agent.getService(IAgentLocation.class),
					profile.getProfileId());
			return profileScope.getNode(EngineActivator.ID).getBoolean(TRUST_ALWAYS_PROPERTY, false);
		}
		return false;
	}

	public void setTrustAlways(boolean trustAlways) {
		if (profile != null) {
			ProfileScope profileScope = new ProfileScope(agent.getService(IAgentLocation.class),
					profile.getProfileId());
			profileScope.getNode(EngineActivator.ID).putBoolean(TRUST_ALWAYS_PROPERTY, trustAlways);
		}
	}

	public PGPPublicKeyStore getPreferenceTrustedKeys() {
		PGPPublicKeyStore trustStore = new PGPPublicKeyStore();
		if (profile != null) {
			ProfileScope profileScope = new ProfileScope(agent.getService(IAgentLocation.class),
					profile.getProfileId());
			PGPPublicKeyStore
					.readPublicKeys(profileScope.getNode(EngineActivator.ID).get(TRUSTED_KEY_STORE_PROPERTY, null))
					.stream().map(keyService::addKey).forEach(trustStore::addKey);
		}
		return trustStore;
	}

	public IStatus persistTrustedKeys(PGPPublicKeyStore trustStore) {
		if (profile != null) {
			ProfileScope profileScope = new ProfileScope(agent.getService(IAgentLocation.class),
					profile.getProfileId());
			IEclipsePreferences node = profileScope.getNode(EngineActivator.ID);
			try {
				node.put(TRUSTED_KEY_STORE_PROPERTY, trustStore.toArmoredString());
				node.flush();
			} catch (IOException | BackingStoreException ex) {
				return new Status(IStatus.ERROR, EngineActivator.ID, ex.getMessage(), ex);
			}
		}
		return Status.OK_STATUS;
	}
}
