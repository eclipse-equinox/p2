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
import java.security.cert.*;
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
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.core.UIServices.TrustInfo;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.ProfileScope;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
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

	private static boolean VERIFY_CERTIFICATE_SIGNATURE_VALIDITY = Boolean.TRUE.toString()
			.equalsIgnoreCase(System.getProperty("p2.verifyCertificateSignatureValidity", Boolean.TRUE.toString())); //$NON-NLS-1$

	public static final String TRUST_ALWAYS_PROPERTY = "trustAlways"; //$NON-NLS-1$

	public static final String TRUSTED_KEY_STORE_PROPERTY = "pgp.trustedPublicKeys"; //$NON-NLS-1$

	public static final String TRUSTED_CERTIFICATES_PROPERTY = "trustedCertificates"; //$NON-NLS-1$

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

	// Lazily loading in case we ever add an extension point for registering
	// certificates.
	private Supplier<Collection<? extends Certificate>> additionalTrustedCertificates = new Supplier<>() {
		private Collection<? extends Certificate> cache = null;

		public Collection<? extends Certificate> get() {
			if (cache == null) {
				cache = getPreferenceTrustedCertificates();
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

		// If unsigned content is allowed then the flood gates are open so there is no
		// point in checking for unrooted certificates nor for not-yet-trusted keys.
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=578583
		String policy = getUnsignedContentPolicy();
		if (EngineActivator.UNSIGNED_ALLOW.equals(policy)) {
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
		Set<SimpleArtifactRepository> repositories = new HashSet<>();
		boolean isTrustedKeySetInitialized = false;
		Map<IArtifactKey, File> artifactFiles = new LinkedHashMap<>();
		for (Entry<IArtifactDescriptor, File> artifact : artifacts.entrySet()) {
			IArtifactDescriptor artifactDescriptor = artifact.getKey();
			IArtifactRepository repository = artifactDescriptor.getRepository();
			if (repository instanceof SimpleArtifactRepository simpleArtifactRepository) {
				repositories.add(simpleArtifactRepository);
			}
			IArtifactKey artifactKey = artifactDescriptor.getArtifactKey();
			File artifactFile = artifact.getValue();
			artifactFiles.put(artifactKey, artifactFile);
			boolean artifactTrustedByCertificate = false;
			try {
				SignedContent content = verifierFactory.getSignedContent(artifactFile);
				boolean signed = content.isSigned();
				if (signed) {
					SignerInfo[] signerInfo = content.getSignerInfos();

					// Only record the untrusted elements if there are no trusted elements.
					// Also check previously trusted certificates from the preferences.
					if (Arrays.stream(signerInfo).noneMatch(SignerInfo::isTrusted)
							&& Arrays.stream(signerInfo).map(SignerInfo::getCertificateChain).flatMap(Arrays::stream)
									.noneMatch(cert -> additionalTrustedCertificates.get().contains(cert))) {
						for (SignerInfo element : signerInfo) {
							if (!element.isTrusted()) {
								List<Certificate> certificateChain = Arrays.asList(element.getCertificateChain());
								untrustedCertificates.computeIfAbsent(certificateChain, key -> new LinkedHashSet<>())
										.add(artifactKey);
							}
						}
					} else {
						artifactTrustedByCertificate = true;
					}

					// Treat the artifact as untrusted if the signature is outside of the
					// certificate's validity range.
					if (VERIFY_CERTIFICATE_SIGNATURE_VALIDITY) {
						List<SignerInfo> invalidSignatures = Arrays.stream(signerInfo).filter(info -> {
							try {
								content.checkValidity(info);
								return false;
							} catch (CertificateExpiredException | CertificateNotYetValidException e) {
								return true;
							}
						}).collect(Collectors.toList());

						// Only complain if all signatures are invalid and do so even if the certificate
						// itself is trusted.
						if (signerInfo.length == invalidSignatures.size()) {
							artifactTrustedByCertificate = false;
							for (SignerInfo info : invalidSignatures) {
								List<Certificate> certificateChain = Arrays.asList(info.getCertificateChain());
								untrustedCertificates.computeIfAbsent(certificateChain, key -> new LinkedHashSet<>())
										.add(artifactKey);
							}
						}
					}

				}

				// Also check for PGP signatures if the artifact is not trusted by a certificate
				// because there might be trusted PGP keys too.
				if (!signed || !artifactTrustedByCertificate) {
					// The keys are in this destination artifact's properties if and only if the
					// PGPSignatureVerifier verified the signatures against these keys.
					List<PGPPublicKey> verifiedKeys = PGPPublicKeyStore
							.readPublicKeys(
									artifactDescriptor.getProperty(PGPSignatureVerifier.PGP_SIGNER_KEYS_PROPERTY_NAME))
							.stream().map(keyService::addKey).collect(Collectors.toList());
					if (!verifiedKeys.isEmpty()) {
						if (!isTrustedKeySetInitialized) {
							isTrustedKeySetInitialized = true;
							trustedKeySet.addAll(trustedKeys.get().all().stream()
									.filter(it -> keyService.getVerifiedRevocationDate(it) == null).toList());
						}
						// Only record the untrusted keys if none of the keys are trusted.
						if (verifiedKeys.stream().noneMatch(trustedKeySet::contains)) {
							verifiedKeys.forEach(key -> untrustedPGPKeys
									.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(artifactKey));
						} else {
							// There are PGP keys and at least one of them is trusted so even if there are
							// untrusted certificates we will not prompt for those because we only prompt if
							// none of the certificates *and* none of the PGP keys are trusted.
							// So clean them out of the map.
							untrustedCertificates.values().forEach(it -> it.remove(artifactKey));
							untrustedCertificates.values().removeIf(Collection::isEmpty);
						}
					} else if (!signed) {
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

		// if there is unsigned content and we should never allow it, then fail without
		// further checking certificates
		if (!unsignedArtifacts.isEmpty() && EngineActivator.UNSIGNED_FAIL.equals(policy)) {
			return new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(
					Messages.CertificateChecker_UnsignedNotAllowed,
					unsignedArtifacts.stream().map(key -> artifactFiles.get(key)).collect(Collectors.toList())));
		}

		// If there was no unsigned content, and nothing untrusted, no need to prompt.
		if (unsignedArtifacts.isEmpty() && untrustedCertificates.isEmpty() && untrustedPGPKeys.isEmpty()) {
			return Status.OK_STATUS;
		}

		TrustInfo trustInfo = artifactServiceUI.getTrustInfo(untrustedCertificates, untrustedPGPKeys, unsignedArtifacts,
				artifactFiles);

		setTrustAlways(trustInfo.trustAlways());

		if (!trustInfo.trustAlways()) {
			// For any certificate that was newly trusted, its associated artifacts are
			// trusted.
			Set<IArtifactKey> trustedArtifactKeys = new HashSet<>();
			Certificate[] trustedCertificates = trustInfo.getTrustedCertificates();
			if (trustedCertificates != null) {
				for (Entry<List<Certificate>, Set<IArtifactKey>> entry : untrustedCertificates.entrySet()) {
					for (Certificate trustedCertificate : trustedCertificates) {
						if (entry.getKey().contains(trustedCertificate)) {
							trustedArtifactKeys.addAll(entry.getValue());
						}
					}
				}
			}

			// Add any newly trusted keys.
			// The artifacts associated with those keys are now trusted.
			trustedKeySet.addAll(trustInfo.getTrustedPGPKeys());
			Set<IArtifactKey> pgpTrustedArtifactKeys = trustedKeySet.stream().map(untrustedPGPKeys::get)
					.filter(Objects::nonNull).flatMap(Set::stream).collect(Collectors.toSet());
			trustedArtifactKeys.addAll(pgpTrustedArtifactKeys);

			// Remove all trusted artifacts the values of both maps.
			untrustedCertificates.values().forEach(it -> it.removeAll(trustedArtifactKeys));
			untrustedPGPKeys.values().forEach(it -> it.removeAll(trustedArtifactKeys));

			// Remove any values that are now empty sets.
			untrustedCertificates.values().removeIf(Collection::isEmpty);
			untrustedPGPKeys.values().removeIf(Collection::isEmpty);

			String errorMessage = !unsignedArtifacts.isEmpty() && !trustInfo.trustUnsignedContent()
					? Messages.CertificateChecker_UnsignedRejected
					: !untrustedCertificates.isEmpty() && !untrustedPGPKeys.isEmpty()
							? Messages.CertificateChecker_CertificateOrPGPKeyRejected
							: !untrustedCertificates.isEmpty() ? Messages.CertificateChecker_CertificateRejected
									: !untrustedPGPKeys.isEmpty() ? Messages.CertificateChecker_PGPKeyRejected : null;
			if (errorMessage != null) {
				Set<IArtifactKey> keys = new HashSet<>();
				keys.addAll(unsignedArtifacts);
				untrustedCertificates.values().stream().forEach(keys::addAll);
				untrustedPGPKeys.values().stream().forEach(keys::addAll);
				for (var repository : repositories) {
					repository.removeDescriptors(keys.toArray(IArtifactKey[]::new), true, null);
				}

				return new Status(IStatus.CANCEL, EngineActivator.ID, errorMessage);
			}
		}

		// If we should persist the trusted certificates and keys.
		if (trustInfo.persistTrust()) {
			List<IStatus> statuses = new ArrayList<>();

			Set<Certificate> unsavedCertificates = new LinkedHashSet<>();
			if (trustInfo.getTrustedCertificates() != null) {
				unsavedCertificates.addAll(Arrays.asList(trustInfo.getTrustedCertificates()));
			}

			if (!unsavedCertificates.isEmpty()) {
				// Try to save to the trust engine first.
				IStatus status = persistTrustedCertificatesInTrustEngine(unsavedCertificates);
				if (status != null && !status.isOK()) {
					statuses.add(status);
				}

				// If we couldn't save them in the trust engine, save them in the preferences.
				if (!unsavedCertificates.isEmpty()) {
					// Be sure we add the new certificates to the previously saved certificates.
					unsavedCertificates.addAll(getPreferenceTrustedCertificates());
					IStatus preferenceStatus = persistTrustedCertificates(unsavedCertificates);
					if (preferenceStatus != null && !preferenceStatus.isOK()) {
						statuses.add(preferenceStatus);
					}
				}
			}

			if (!trustInfo.getTrustedPGPKeys().isEmpty()) {
				// Be sure we add the new keys to the previously saved keys.
				PGPPublicKeyStore keyStore = getPreferenceTrustedKeys();
				trustInfo.getTrustedPGPKeys().forEach(keyStore::addKey);
				IStatus status = persistTrustedKeys(keyStore);
				if (status != null && !status.isOK()) {
					statuses.add(status);
				}
			}

			if (!statuses.isEmpty()) {
				if (statuses.size() == 1) {
					return statuses.get(1);
				}
				String message = statuses.stream().map(IStatus::getMessage).collect(Collectors.joining("\n")); //$NON-NLS-1$
				return new MultiStatus(getClass(), IStatus.OK, statuses.toArray(IStatus[]::new), message, null);
			}
		}

		return Status.OK_STATUS;
	}

	/**
	 * This modifies the argument collection to remove the certificates that were
	 * successfully saved. Often no certificates are saved because this tries to
	 * store in the Java runtime cacerts and those are typically read-only.
	 */
	private IStatus persistTrustedCertificatesInTrustEngine(
			Collection<? extends Certificate> unsavedTrustedCertificates) {
		ServiceTracker<TrustEngine, TrustEngine> trustEngineTracker = new ServiceTracker<>(EngineActivator.getContext(),
				TrustEngine.class, null);
		trustEngineTracker.open();
		Object[] trustEngines = trustEngineTracker.getServices();
		try {
			if (trustEngines == null)
				return null;
			for (Iterator<? extends Certificate> it = unsavedTrustedCertificates.iterator(); it.hasNext();) {
				Certificate trustedCertificate = it.next();
				for (Object engine : trustEngines) {
					TrustEngine trustEngine = (TrustEngine) engine;
					if (trustEngine.isReadOnly())
						continue;
					try {
						trustEngine.addTrustAnchor(trustedCertificate, trustedCertificate.toString());
						// this should mean we added an anchor successfully; continue to next
						// certificate
						it.remove();
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

	public IStatus persistTrustedCertificates(Collection<? extends Certificate> trustedCertificates) {
		if (profile != null) {
			ProfileScope profileScope = new ProfileScope(agent.getService(IAgentLocation.class),
					profile.getProfileId());
			IEclipsePreferences node = profileScope.getNode(EngineActivator.ID);
			try {
				CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509"); //$NON-NLS-1$
				CertPath certPath = certificateFactory.generateCertPath(new ArrayList<>(trustedCertificates));
				byte[] encoded = certPath.getEncoded("PKCS7"); //$NON-NLS-1$
				node.putByteArray(TRUSTED_CERTIFICATES_PROPERTY, encoded);
				node.flush();
			} catch (BackingStoreException | CertificateException ex) {
				return new Status(IStatus.ERROR, EngineActivator.ID, ex.getMessage(), ex);
			}
		}
		return Status.OK_STATUS;
	}

	public Set<? extends Certificate> getPreferenceTrustedCertificates() {
		if (profile != null) {
			ProfileScope profileScope = new ProfileScope(agent.getService(IAgentLocation.class),
					profile.getProfileId());
			IEclipsePreferences node = profileScope.getNode(EngineActivator.ID);
			try {
				byte[] encoded = node.getByteArray(TRUSTED_CERTIFICATES_PROPERTY, null);
				if (encoded != null) {
					CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509"); //$NON-NLS-1$
					CertPath certPath = certificateFactory.generateCertPath(new ByteArrayInputStream(encoded), "PKCS7"); //$NON-NLS-1$
					return new LinkedHashSet<>(certPath.getCertificates());
				}
			} catch (CertificateException ex) {
				DebugHelper.debug(DEBUG_PREFIX, ex.getMessage());
			}
		}
		return Set.of();
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

	public IStatus setTrustAlways(boolean trustAlways) {
		if (profile != null) {
			ProfileScope profileScope = new ProfileScope(agent.getService(IAgentLocation.class),
					profile.getProfileId());
			IEclipsePreferences node = profileScope.getNode(EngineActivator.ID);
			try {
				node.putBoolean(TRUST_ALWAYS_PROPERTY, trustAlways);
				node.flush();
			} catch (BackingStoreException ex) {
				return new Status(IStatus.ERROR, EngineActivator.ID, ex.getMessage(), ex);
			}
		}
		return Status.OK_STATUS;
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
