/*******************************************************************************
 * Copyright (c) 2022 Eclipse contributors and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.bouncycastle.openpgp.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.processors.pgp.PGPPublicKeyStore;
import org.eclipse.equinox.internal.p2.artifact.processors.pgp.PGPSignatureVerifier;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.spi.PGPPublicKeyService;
import org.eclipse.ui.internal.about.AboutBundleData;
import org.eclipse.ui.internal.about.AboutPluginsPage;
import org.osgi.framework.Bundle;

/**
 * A factory used by the {@link AboutPluginsPage} to provide extended signing
 * information, in particular information about PGP signing.
 *
 * @since 2.7.400
 */
public class KeySigningInfoFactory implements IAdapterFactory {

	private static final Class<?>[] CLASSES = new Class[] { AboutBundleData.ExtendedSigningInfo.class };

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adapterType == AboutBundleData.ExtendedSigningInfo.class) {
			return adapterType.cast(new AboutBundleData.ExtendedSigningInfo() {
				private final Map<File, Map<PGPSignature, PGPPublicKey>> bundlePoolArtficactSigningDetails = getBundlePoolArtficactPGPSigningDetails();

				@Override
				public boolean isSigned(Bundle bundle) {
					return getDetails(bundle) != null;
				}

				@Override
				public String getSigningType(Bundle bundle) {
					return ProvUIMessages.KeySigningInfoFactory_PGPSigningType;
				}

				@Override
				public String getSigningDetails(Bundle bundle) {
					Map<PGPSignature, PGPPublicKey> details = getDetails(bundle);
					if (details != null) {
						PGPPublicKeyService keyService = getKeyService();
						List<String> lines = new ArrayList<>();
						for (PGPPublicKey key : details.values()) {
							if (keyService != null) {
								// Be sure to normalize/enhance the key so we properly don't show
								// self-signatures.
								key = keyService.addKey(key);
							}
							if (!lines.isEmpty()) {
								lines.add(""); //$NON-NLS-1$
							}
							addDetails(key, lines, ""); //$NON-NLS-1$
							if (keyService != null) {
								Set<PGPPublicKey> verifiedCertifications = keyService.getVerifiedCertifications(key);
								boolean first = true;
								for (PGPPublicKey verifyingKey : verifiedCertifications) {
									/// Don't show self-signatures.
									if (!verifyingKey.equals(key)) {
										if (first) {
											lines.add("  " + ProvUIMessages.KeySigningInfoFactory_KeySignersSection); //$NON-NLS-1$
											first = false;
										}
										addDetails(verifyingKey, lines, "    "); //$NON-NLS-1$
									}
								}
							}
						}
						return String.join("\n", lines); //$NON-NLS-1$
					}
					return null;
				}

				@Override
				public Date getSigningTime(Bundle bundle) {
					Map<PGPSignature, PGPPublicKey> details = getDetails(bundle);
					return details == null ? null : details.keySet().iterator().next().getCreationTime();
				}

				private void addDetails(PGPPublicKey key, List<String> lines, String indentation) {
					lines.add(indentation + ProvUIMessages.KeySigningInfoFactory_FingerprintItem
							+ PGPPublicKeyService.toHexFingerprint(key));
					for (Iterator<String> userIDs = key.getUserIDs(); userIDs.hasNext();) {
						lines.add(indentation + ProvUIMessages.KeySigningInfoFactory_UserIDItem + userIDs.next());
					}
				}

				private PGPPublicKeyService getKeyService() {
					IProvisioningAgent agent = org.eclipse.equinox.internal.p2.extensionlocation.Activator
							.getCurrentAgent();
					return agent == null ? null : agent.getService(PGPPublicKeyService.class);
				}

				private Map<PGPSignature, PGPPublicKey> getDetails(Bundle bundle) {
					try {
						File bundleFile = FileLocator.getBundleFileLocation(bundle).orElseThrow().getCanonicalFile();
						return bundlePoolArtficactSigningDetails.get(bundleFile);
					} catch (IOException | RuntimeException e) {
						ProvUIActivator.getDefault().getLog().log(Status.error(e.getMessage(), e));
						return null;
					}
				}
			});
		}

		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return CLASSES;
	}

	/**
	 * Returns a map from artifact files to the PGP signature/key pairs that were
	 * used to {@link PGPSignatureVerifier#close() verify} the artifact while it was
	 * being downloaded.
	 *
	 * @return a map of all the PGP signed artifact files to their signature/key
	 *         pairs.
	 */
	private static Map<File, Map<PGPSignature, PGPPublicKey>> getBundlePoolArtficactPGPSigningDetails() {
		Map<File, Map<PGPSignature, PGPPublicKey>> result = new LinkedHashMap<>();
		try {
			// Look up artifact metadata for all the bundle pool repository of the
			// installation.
			IFileArtifactRepository bundlePoolRepository = org.eclipse.equinox.internal.p2.extensionlocation.Activator
					.getBundlePoolRepository();
			if (bundlePoolRepository == null) {
				return Collections.emptyMap();
			}
			IQueryResult<IArtifactKey> allArtifactKeys = bundlePoolRepository.query(ArtifactKeyQuery.ALL_KEYS, null);
			for (IArtifactKey key : allArtifactKeys) {
				for (IArtifactDescriptor descriptor : bundlePoolRepository.getArtifactDescriptors(key)) {
					File file = bundlePoolRepository.getArtifactFile(descriptor);
					if (file != null) {
						try {
							Collection<PGPSignature> signatures = PGPSignatureVerifier.getSignatures(descriptor);
							if (!signatures.isEmpty()) {
								Map<PGPSignature, PGPPublicKey> details = new LinkedHashMap<>();
								PGPPublicKeyStore keys = PGPSignatureVerifier.getKeys(descriptor);
								for (PGPSignature signature : signatures) {
									Collection<PGPPublicKey> signingKeys = keys.getKeys(signature.getKeyID());
									if (!signingKeys.isEmpty()) {
										// There is vanishingly small chance that two keys with colliding key IDs were
										// used on two different signatures on the same artifact.
										details.put(signature, signingKeys.iterator().next());
									}
								}
								if (!details.isEmpty()) {
									result.put(file.getCanonicalFile(), details);
								}
							}
						} catch (IOException | PGPException | RuntimeException e) {
							ProvUIActivator.getDefault().getLog().log(Status.error(e.getMessage(), e));
						}
					}
				}
			}
		} catch (RuntimeException e) {
			ProvUIActivator.getDefault().getLog().log(Status.error(e.getMessage(), e));
		}
		return result;
	}
}
