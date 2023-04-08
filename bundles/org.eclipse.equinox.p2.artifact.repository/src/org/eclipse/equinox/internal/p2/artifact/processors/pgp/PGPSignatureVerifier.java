/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.processors.pgp;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.bc.BcPGPObjectFactory;
import org.bouncycastle.openpgp.operator.PGPContentVerifier;
import org.bouncycastle.openpgp.operator.PGPContentVerifierBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.spi.PGPPublicKeyService;
import org.eclipse.osgi.util.NLS;

/**
 * This processing step verifies PGP signatures are correct (i.e., the artifact
 * was not tampered during fetch). Note that is does <b>not</b> deal with trust.
 * Dealing with trusted signers is done as part of CheckTrust and touchpoint
 * phase.
 */
public final class PGPSignatureVerifier extends ProcessingStep {

	/**
	 * ID of the registering
	 * <code>org.eclipse.equinox.p2.artifact.repository.processingSteps</tt>
	 * extension.
	 */
	public static final String ID = "org.eclipse.equinox.p2.processing.PGPSignatureCheck"; //$NON-NLS-1$

	public static final String PGP_SIGNER_KEYS_PROPERTY_NAME = "pgp.publicKeys"; //$NON-NLS-1$

	public static final String PGP_SIGNATURES_PROPERTY_NAME = "pgp.signatures"; //$NON-NLS-1$

	private PGPPublicKeyService keyService;

	private IArtifactDescriptor sourceDescriptor;

	private Map<PGPSignature, List<PGPContentVerifier>> signaturesToVerify = new LinkedHashMap<>();

	private Map<PGPContentVerifier, PGPPublicKey> verifierKeys = new LinkedHashMap<>();

	private List<OutputStream> signatureVerifiers = new ArrayList<>();

	public PGPSignatureVerifier() {
		super();
		link(nullOutputStream(), new NullProgressMonitor()); // this is convenience for tests
	}

	public static Collection<PGPSignature> getSignatures(IArtifactDescriptor artifact)
			throws IOException, PGPException {
		String signatureText = unnormalizedPGPProperty(artifact.getProperty(PGP_SIGNATURES_PROPERTY_NAME));
		if (signatureText == null) {
			return Collections.emptyList();
		}
		List<PGPSignature> res = new ArrayList<>();
		try (InputStream in = new ArmoredInputStream(
				new ByteArrayInputStream(signatureText.getBytes(StandardCharsets.US_ASCII)))) {
			PGPObjectFactory pgpFactory = new BcPGPObjectFactory(in);
			Object o = pgpFactory.nextObject();
			PGPSignatureList signatureList = new PGPSignatureList(new PGPSignature[0]);
			if (o instanceof PGPCompressedData) {
				PGPCompressedData pgpCompressData = (PGPCompressedData) o;
				pgpFactory = new BcPGPObjectFactory(pgpCompressData.getDataStream());
				signatureList = (PGPSignatureList) pgpFactory.nextObject();
			} else if (o instanceof PGPSignatureList) {
				signatureList = (PGPSignatureList) o;
			}
			signatureList.iterator().forEachRemaining(res::add);
		}
		return res;
	}

	public static PGPPublicKeyStore getKeys(IArtifactDescriptor artifact) {
		PGPPublicKeyStore keyStore = new PGPPublicKeyStore();
		String keyText = artifact.getProperty(PGPSignatureVerifier.PGP_SIGNER_KEYS_PROPERTY_NAME);
		PGPPublicKeyStore.readPublicKeys(keyText).stream().forEach(keyStore::addKey);
		return keyStore;
	}

	@Override
	public void initialize(IProvisioningAgent agent, IProcessingStepDescriptor descriptor,
			IArtifactDescriptor context) {
		super.initialize(agent, descriptor, context);

		sourceDescriptor = context;
		keyService = agent.getService(PGPPublicKeyService.class);

//		1. verify declared public keys have signature from a trusted key, if so, add to KeyStore
//		2. verify artifact signature matches signature of given keys, and at least 1 of this key is trusted
		String signatureText = unnormalizedPGPProperty(context.getProperty(PGP_SIGNATURES_PROPERTY_NAME));
		if (signatureText == null) {
			setStatus(Status.OK_STATUS);
			return;
		}

		Collection<PGPSignature> signatures;
		try {
			signatures = getSignatures(context);
		} catch (Exception ex) {
			setStatus(new Status(IStatus.ERROR, Activator.ID, Messages.Error_CouldNotLoadSignature, ex));
			return;
		}

		if (signatures.isEmpty()) {
			setStatus(Status.OK_STATUS);
			return;
		}

		IArtifactRepository repository = context.getRepository();

		PGPPublicKeyStore.readPublicKeys(context.getProperty(PGP_SIGNER_KEYS_PROPERTY_NAME))
				.forEach(keyService::addKey);
		if (repository != null) {
			PGPPublicKeyStore.readPublicKeys(repository.getProperty(PGP_SIGNER_KEYS_PROPERTY_NAME))
					.forEach(keyService::addKey);
		}

		for (PGPSignature signature : signatures) {
			long keyID = signature.getKeyID();
			Collection<PGPPublicKey> keys = keyService.getKeys(keyID);
			if (keys.isEmpty()) {
				LogHelper.log(new Status(IStatus.WARNING, Activator.ID,
						NLS.bind(Messages.Warning_publicKeyNotFound, PGPPublicKeyService.toHex(keyID),
								context.getArtifactKey().getId())));
			} else {
				try {
					PGPContentVerifierBuilder verifierBuilder = new BcPGPContentVerifierBuilderProvider()
							.get(signature.getKeyAlgorithm(), signature.getHashAlgorithm());
					List<PGPContentVerifier> verifiers = new ArrayList<>();
					signaturesToVerify.put(signature, verifiers);
					for (PGPPublicKey key : keys) {
						PGPContentVerifier verifier = verifierBuilder.build(key);
						verifierKeys.put(verifier, key);
						verifiers.add(verifier);
						signatureVerifiers.add(verifier.getOutputStream());
					}
				} catch (PGPException ex) {
					setStatus(new Status(IStatus.ERROR, Activator.ID, ex.getMessage(), ex));
					return;
				}
			}
		}
	}

	/**
	 * See // https://www.w3.org/TR/1998/REC-xml-19980210#AVNormalize, newlines
	 * replaced by spaces by parser, needs to be restored
	 *
	 * @param armoredPGPBlock the PGP block, in armored form
	 * @return fixed PGP armored blocks
	 */
	static String unnormalizedPGPProperty(String armoredPGPBlock) {
		if (armoredPGPBlock == null) {
			return null;
		}
		if (armoredPGPBlock.contains("\n") || armoredPGPBlock.contains("\r")) { //$NON-NLS-1$ //$NON-NLS-2$
			return armoredPGPBlock;
		}
		return armoredPGPBlock.replace(' ', '\n')
				.replace("-----BEGIN\nPGP\nSIGNATURE-----", "-----BEGIN PGP SIGNATURE-----") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("-----END\nPGP\nSIGNATURE-----", "-----END PGP SIGNATURE-----") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("-----BEGIN\nPGP\nPUBLIC\nKEY\nBLOCK-----", "-----BEGIN PGP PUBLIC KEY BLOCK-----") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("-----END\nPGP\nPUBLIC\nKEY\nBLOCK-----", "-----END PGP PUBLIC KEY BLOCK-----"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public void write(int b) throws IOException {
		getDestination().write(b);
		for (OutputStream verifier : signatureVerifiers) {
			verifier.write(b);
		}
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		getDestination().write(b, off, len);
		for (OutputStream verifier : signatureVerifiers) {
			verifier.write(b, off, len);
		}
	}

	@Override
	public void close() throws IOException {
		try {
			if (!getStatus().isOK()) {
				return;
			}

			if (signaturesToVerify.isEmpty()) {
				return;
			}

			PGPPublicKeyStore keyStore = new PGPPublicKeyStore();
			for (Entry<PGPSignature, List<PGPContentVerifier>> entry : signaturesToVerify.entrySet()) {
				PGPSignature signature = entry.getKey();
				List<PGPContentVerifier> verifiers = entry.getValue();
				boolean verified = false;
				for (PGPContentVerifier verifier : verifiers) {
					try {
						verifier.getOutputStream().write(signature.getSignatureTrailer());
						if (verifier.verify(signature.getSignature())) {
							PGPPublicKey verifyingKey = verifierKeys.get(verifier);
							if (!Boolean.FALSE.toString()
									.equalsIgnoreCase(System.getProperty("p2.pgp.verifyExpiration"))) { //$NON-NLS-1$
								if (PGPPublicKeyService.compareSignatureTimeToKeyValidityTime(signature,
										verifyingKey) != 0) {
									LogHelper.log(new Status(IStatus.WARNING, Activator.ID,
											NLS.bind(Messages.Error_SignatureAfterKeyExpiration, PGPPublicKeyService
													.toHexFingerprint(verifyingKey))));
								}
							}

							if (!Boolean.FALSE.toString()
									.equalsIgnoreCase(System.getProperty("p2.pgp.verifyRevocation"))) { //$NON-NLS-1$
								if (!keyService.isCreatedBeforeRevocation(signature, verifyingKey)) {
									LogHelper.log(new Status(IStatus.ERROR, Activator.ID,
											NLS.bind(Messages.Error_SignatureAfterKeyRevocation, PGPPublicKeyService
													.toHexFingerprint(verifyingKey))));
								}
							}

							keyStore.addKey(verifyingKey);
							verified = true;
							break;
						}
					} catch (PGPException ex) {
						LogHelper.log(new Status(IStatus.ERROR, Activator.ID, ex.getMessage(), ex));
					}
				}

				if (!verified) {
					setStatus(new Status(IStatus.ERROR, Activator.ID, Messages.Error_SignatureAndFileDontMatch));
					return;
				}
			}

			// Update the destination artifact descriptor with the signatures that have been
			// verified and the keys used for that verification.
			OutputStream destination = getDestination();
			if (destination instanceof IAdaptable) {
				ArtifactDescriptor destinationDescriptor = ((IAdaptable) destination)
						.getAdapter(ArtifactDescriptor.class);
				destinationDescriptor.setProperty(PGP_SIGNATURES_PROPERTY_NAME,
						sourceDescriptor.getProperty(PGP_SIGNATURES_PROPERTY_NAME));
				destinationDescriptor.setProperty(PGP_SIGNER_KEYS_PROPERTY_NAME, keyStore.toArmoredString());
			}

			setStatus(Status.OK_STATUS);
		} finally {
			super.close();
		}
	}

}
