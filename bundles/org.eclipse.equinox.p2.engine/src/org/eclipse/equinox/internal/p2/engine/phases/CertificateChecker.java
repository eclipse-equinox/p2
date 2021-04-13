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

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.processors.pgp.PGPSignatureVerifier;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.UIServices;
import org.eclipse.equinox.p2.core.UIServices.TrustInfo;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.osgi.service.security.TrustEngine;
import org.eclipse.osgi.signedcontent.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Checks the certificates or PGP signatures on a set of files or artifacts and
 * reports back any problems with unsigned artifacts, untrusted certificates, or
 * tampered content.
 */
public class CertificateChecker {
	private static final String DEBUG_PREFIX = "certificate checker"; //$NON-NLS-1$

	/**
	 * Stores artifacts to check
	 */
	private Map<IArtifactDescriptor, File> artifacts = new HashMap<>();
	private final IProvisioningAgent agent;

	public CertificateChecker() {
		this(null);
	}

	public CertificateChecker(IProvisioningAgent agent) {
		this.agent = agent;
		artifacts = new HashMap<>();
	}

	private static class PGPPublicKeyEntry {
		public final PGPPublicKey key;

		public PGPPublicKeyEntry(PGPPublicKey key) {
			this.key = key;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof PGPPublicKeyEntry)) {
				return false;
			}
			return key.getKeyID() == ((PGPPublicKeyEntry) obj).key.getKeyID();
		}

		@Override
		public int hashCode() {
			return Long.hashCode(key.getKeyID());
		}
	}

	public IStatus start() {
		final BundleContext context = EngineActivator.getContext();
		ServiceReference<SignedContentFactory> contentFactoryRef = context.getServiceReference(SignedContentFactory.class);
		SignedContentFactory verifierFactory = context.getService(contentFactoryRef);
		try {
			return checkCertificates(verifierFactory);
		} finally {
			context.ungetService(contentFactoryRef);
		}
	}

	private IStatus checkCertificates(SignedContentFactory verifierFactory) {
		UIServices serviceUI = agent.getService(UIServices.class);
		ArrayList<Certificate> untrustedCertificates = new ArrayList<>();
		Map<IArtifactDescriptor, Collection<PGPPublicKey>> untrustedPGPArtifacts = new HashMap<>();
		Map<PGPPublicKeyEntry, Collection<IArtifactDescriptor>> untrustedPGPKeys = new HashMap<>();
		Map<IArtifactDescriptor, File> unsigned = new HashMap<>();
		ArrayList<Certificate[]> untrustedChain = new ArrayList<>();
		Map<Certificate, Collection<File>> untrustedArtifacts = new HashMap<>();
		IStatus status = Status.OK_STATUS;
		if (artifacts.isEmpty() || serviceUI == null) {
			return status;
		}
		for (Entry<IArtifactDescriptor, File> artifact : artifacts.entrySet()) {
			File artifactFile = artifact.getValue();
			try {
				SignedContent content = verifierFactory.getSignedContent(artifactFile);
				if (content.isSigned()) {
					SignerInfo[] signerInfo = content.getSignerInfos();
					if (Arrays.stream(signerInfo).noneMatch(SignerInfo::isTrusted)) {
						// Only record the untrusted elements if there are no trusted elements.
						for (SignerInfo element : signerInfo) {
							if (!element.isTrusted()) {
								Certificate[] certificateChain = element.getCertificateChain();
								if (!untrustedCertificates.contains(certificateChain[0])) {
									untrustedCertificates.add(certificateChain[0]);
									untrustedChain.add(certificateChain);
								}
								if (DebugHelper.DEBUG_CERTIFICATE_CHECKER_UNTRUSTED) {
									untrustedArtifacts.computeIfAbsent(certificateChain[0], key -> new ArrayList<>())
											.add(artifactFile);
								}
							}
						}
					}
				} else {
					Collection<PGPPublicKey> signers = PGPSignatureVerifier.getSigners(artifact.getKey());
					if (!signers.isEmpty()) {
						if (signers.stream().noneMatch(this::isTrusted)) {
							untrustedPGPArtifacts.putIfAbsent(artifact.getKey(), signers);
							signers.forEach(signer -> untrustedPGPKeys
									.computeIfAbsent(new PGPPublicKeyEntry(signer), key -> new HashSet<>())
									.add(artifact.getKey()));
						}
					} else {
						unsigned.put(artifact.getKey(), artifactFile);
					}
				}

			} catch (GeneralSecurityException e) {
				return new Status(IStatus.ERROR, EngineActivator.ID, Messages.CertificateChecker_SignedContentError, e);
			} catch (IOException e) {
				return new Status(IStatus.ERROR, EngineActivator.ID, Messages.CertificateChecker_SignedContentIOError, e);
			}
		}

		// log the unsigned artifacts if requested
		if (DebugHelper.DEBUG_CERTIFICATE_CHECKER_UNSIGNED && !unsigned.isEmpty()) {
			StringBuilder message = new StringBuilder("The following artifacts are unsigned:\n"); //$NON-NLS-1$
			for (File file : unsigned.values()) {
				message.append(NLS.bind("  {0}\n", file.getPath())); //$NON-NLS-1$
			}
			DebugHelper.debug(DEBUG_PREFIX, message.toString());
		}

		// log the untrusted certificates if requested
		if (DebugHelper.DEBUG_CERTIFICATE_CHECKER_UNTRUSTED && !untrustedCertificates.isEmpty()) {
			StringBuilder message = new StringBuilder("The following certificates are untrusted:\n"); //$NON-NLS-1$
			for (Certificate cert : untrustedArtifacts.keySet()) {
				message.append(cert.toString() + "\n"); //$NON-NLS-1$
				message.append("  used by the following artifacts:\n"); //$NON-NLS-1$
				for (File file : untrustedArtifacts.get(cert)) {
					message.append(NLS.bind("    {0}\n", file.getPath())); //$NON-NLS-1$
				}
			}
			DebugHelper.debug(DEBUG_PREFIX, message.toString());
		}
		if (DebugHelper.DEBUG_CERTIFICATE_CHECKER_UNTRUSTED && !untrustedPGPKeys.isEmpty()) {
			StringBuilder message = new StringBuilder("The following PGP Keys are untrusted:\n"); //$NON-NLS-1$
			for (Entry<PGPPublicKeyEntry, Collection<IArtifactDescriptor>> entry : untrustedPGPKeys.entrySet()) {
				message.append(entry.getKey().key.getKeyID() + "\n"); //$NON-NLS-1$
				message.append("  used by the following artifacts:\n"); //$NON-NLS-1$
				for (IArtifactDescriptor artifact : entry.getValue()) {
					message.append(NLS.bind("    {0}\n", artifact.getArtifactKey())); //$NON-NLS-1$
				}
			}
			DebugHelper.debug(DEBUG_PREFIX, message.toString());
		}

		String policy = getUnsignedContentPolicy();
		//if there is unsigned content and we should never allow it, then fail without further checking certificates
		if (!unsigned.isEmpty() && EngineActivator.UNSIGNED_FAIL.equals(policy)) {
			return new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.CertificateChecker_UnsignedNotAllowed, unsigned));
		}

		String[] details = EngineActivator.UNSIGNED_ALLOW.equals(policy) || unsigned.isEmpty() ? null
				: unsigned.values().stream().map(Object::toString).toArray(String[]::new);
		Certificate[][] unTrustedCertificateChains = untrustedCertificates.isEmpty() ? null
				: untrustedChain.toArray(Certificate[][]::new);
		// If there was no unsigned content, and nothing untrusted, no need to prompt.
		if (details == null && unTrustedCertificateChains == null && untrustedPGPArtifacts.isEmpty()) {
			return status;
		}

		TrustInfo trustInfo = serviceUI.getTrustInfo(unTrustedCertificateChains,
				untrustedPGPKeys.keySet().stream().map(entry -> entry.key).collect(Collectors.toUnmodifiableList()),
				details);

		// If user doesn't trust unsigned content, cancel the operation
		if (!unsigned.isEmpty() && !trustInfo.trustUnsignedContent()) {
			return Status.CANCEL_STATUS;
		}

		Certificate[] trustedCertificates = trustInfo.getTrustedCertificates();
		// If we had untrusted chains and nothing was trusted, cancel the operation
		if (unTrustedCertificateChains != null && trustedCertificates == null) {
			return new Status(IStatus.CANCEL, EngineActivator.ID, Messages.CertificateChecker_CertificateRejected);
		}
		// Anything that was trusted should be removed from the untrusted list
		if (trustedCertificates != null) {
			for (Certificate trustedCertificate : trustedCertificates) {
				untrustedCertificates.remove(trustedCertificate);
			}
		}
		Collection<PGPPublicKey> trustedPGPKeys = trustInfo.getTrustedPGPKeys();
		untrustedPGPArtifacts.values().removeIf(pgpKeys -> !Collections.disjoint(pgpKeys, trustedPGPKeys));
		trustedPGPKeys.stream().map(PGPPublicKeyEntry::new).forEach(untrustedPGPKeys::remove);

		// If there is still untrusted content, cancel the operation
		if (!untrustedCertificates.isEmpty() || !untrustedPGPKeys.isEmpty()) {
			return new Status(IStatus.CANCEL, EngineActivator.ID, Messages.CertificateChecker_CertificateRejected);
		}
		// If we should persist the trusted certificates, add them to the trust engine
		if (trustInfo.persistTrust()) {
			return persistTrustedCertificates(trustedCertificates);
			// do not persist PGP key at the moment
		}

		return status;
	}

	private IStatus persistTrustedCertificates(Certificate[] trustedCertificates) {
		if (trustedCertificates == null)
			// I'm pretty sure this would be a bug; trustedCertificates should never be null here.
			return new Status(IStatus.INFO, EngineActivator.ID, Messages.CertificateChecker_CertificateRejected);
		ServiceTracker<TrustEngine, TrustEngine> trustEngineTracker = new ServiceTracker<>(EngineActivator.getContext(), TrustEngine.class, null);
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
						// this should mean we added an anchor successfully; continue to next certificate
						break;
					} catch (IOException e) {
						//just return an INFO so the user can proceed with the install
						return new Status(IStatus.INFO, EngineActivator.ID, Messages.CertificateChecker_KeystoreConnectionError, e);
					} catch (GeneralSecurityException e) {
						return new Status(IStatus.INFO, EngineActivator.ID, Messages.CertificateChecker_CertificateError, e);
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

	public void add(Map<IArtifactDescriptor, File> toAdd) {
		artifacts.putAll(toAdd);
	}

	private boolean isTrusted(PGPPublicKey pgppublickey) {
		return false;
	}
}

