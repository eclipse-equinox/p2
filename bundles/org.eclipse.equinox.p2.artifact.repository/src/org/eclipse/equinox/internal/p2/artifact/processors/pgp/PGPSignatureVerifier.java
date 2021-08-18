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
import java.util.*;
import java.util.stream.Collectors;
import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.jcajce.JcaPGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.osgi.util.NLS;

/**
 * This processing step verifies PGP signatures are correct (ie artifact was not
 * tampered during fetch). Note that is does <b>not</b> deal with trust. Dealing
 * with trusted signers is done as part of CheckTrust touchpoint and phase.
 */
public final class PGPSignatureVerifier extends ProcessingStep {

	/**
	 * ID of the registering
	 * <code>org.eclipse.equinox.p2.artifact.repository.processingSteps</tt>
	 * extension.
	 */
	public static final String ID = "org.eclipse.equinox.p2.processing.PGPSignatureCheck"; //$NON-NLS-1$

	private static Map<Long, PGPPublicKey> knownKeys = new HashMap<>();

	public static final String PGP_SIGNER_KEYS_PROPERTY_NAME = "pgp.publicKeys"; //$NON-NLS-1$
	public static final String PGP_SIGNATURES_PROPERTY_NAME = "pgp.signatures"; //$NON-NLS-1$
	private Collection<PGPSignature> signaturesToVerify;

	public PGPSignatureVerifier() {
		super();
		link(nullOutputStream(), new NullProgressMonitor()); // this is convenience for tests
	}

	private static Collection<PGPSignature> getSignatures(IArtifactDescriptor artifact)
			throws IOException, PGPException {
		String signatureText = unnormalizedPGPProperty(artifact.getProperty(PGP_SIGNATURES_PROPERTY_NAME));
		if (signatureText == null) {
			return Collections.emptyList();
		}
		List<PGPSignature> res = new ArrayList<>();
		try (InputStream in = new ArmoredInputStream(new ByteArrayInputStream(signatureText.getBytes()))) {
			JcaPGPObjectFactory pgpFactory = new JcaPGPObjectFactory(in);
			Object o = pgpFactory.nextObject();
			PGPSignatureList signatureList = new PGPSignatureList(new PGPSignature[0]);
			if (o instanceof PGPCompressedData) {
				PGPCompressedData pgpCompressData = (PGPCompressedData) o;
				pgpFactory = new JcaPGPObjectFactory(pgpCompressData.getDataStream());
				signatureList = (PGPSignatureList) pgpFactory.nextObject();
			} else if (o instanceof PGPSignatureList) {
				signatureList = (PGPSignatureList) o;
			}
			signatureList.iterator().forEachRemaining(res::add);
		}
		return res;
	}

	@Override
	public void initialize(IProvisioningAgent agent, IProcessingStepDescriptor descriptor,
			IArtifactDescriptor context) {
		super.initialize(agent, descriptor, context);
//		1. verify declared public keys have signature from a trusted key, if so, add to KeyStore
//		2. verify artifact signature matches signture of given keys, and at least 1 of this key is trusted
		String signatureText = unnormalizedPGPProperty(context.getProperty(PGP_SIGNATURES_PROPERTY_NAME));
		if (signatureText == null) {
			setStatus(Status.OK_STATUS);
			return;
		}
		try {
			signaturesToVerify = getSignatures(context);
		} catch (Exception ex) {
			setStatus(new Status(IStatus.ERROR, Activator.ID, Messages.Error_CouldNotLoadSignature, ex));
			return;
		}
		if (signaturesToVerify.isEmpty()) {
			setStatus(Status.OK_STATUS);
			return;
		}

		IArtifactRepository repository = context.getRepository();
		knownKeys.putAll(readPublicKeys(context.getProperty(PGP_SIGNER_KEYS_PROPERTY_NAME),
				repository != null ? repository.getProperty(PGP_SIGNER_KEYS_PROPERTY_NAME) : null));
		for (PGPSignature signature : signaturesToVerify) {
			PGPPublicKey publicKey = knownKeys.get(signature.getKeyID());
			if (publicKey == null) {
				setStatus(new Status(IStatus.ERROR, Activator.ID,
						NLS.bind(Messages.Error_publicKeyNotFound, signature.getKeyID())));
				return;
			}
			try {
				signature.init(new JcaPGPContentVerifierBuilderProvider().setProvider(new BouncyCastleProvider()),
						publicKey);
			} catch (PGPException ex) {
				setStatus(new Status(IStatus.ERROR, Activator.ID, ex.getMessage(), ex));
				return;
			}
		}
	}

	/**
	 * See // https://www.w3.org/TR/1998/REC-xml-19980210#AVNormalize, newlines
	 * replaced by spaces by parser, needs to be restored
	 *
	 * @param context
	 * @param pgpSignaturesPropertyName
	 * @return fixed PGP armored blocks
	 */
	private static String unnormalizedPGPProperty(String value) {
		if (value == null) {
			return null;
		}
		if (value.contains("\n") || value.contains("\r")) { //$NON-NLS-1$ //$NON-NLS-2$
			return value;
		}
		return value.replace(' ', '\n').replace("-----BEGIN\nPGP\nSIGNATURE-----", "-----BEGIN PGP SIGNATURE-----") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("-----END\nPGP\nSIGNATURE-----", "-----END PGP SIGNATURE-----") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("-----BEGIN\nPGP\nPUBLIC\nKEY\nBLOCK-----", "-----BEGIN PGP PUBLIC KEY BLOCK-----") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("-----END\nPGP\nPUBLIC\nKEY\nBLOCK-----", "-----END PGP PUBLIC KEY BLOCK-----"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static Map<Long, PGPPublicKey> readPublicKeys(String armoredPublicKeyring) {
		if (armoredPublicKeyring == null) {
			return Collections.emptyMap();
		}
		Map<Long, PGPPublicKey> res = new HashMap<>();
		try (InputStream stream = PGPUtil
				.getDecoderStream(new ByteArrayInputStream(unnormalizedPGPProperty(armoredPublicKeyring).getBytes()))) {
			PGPPublicKeyRingCollection pgpPub = new JcaPGPPublicKeyRingCollection(stream);

			pgpPub.getKeyRings().forEachRemaining(kRing ->
				kRing.getPublicKeys().forEachRemaining(key -> res.put(key.getKeyID(), key))
			);
		} catch (IOException | PGPException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, e.getMessage(), e));
		}
		return res;

	}

	private Map<Long, PGPPublicKey> readPublicKeys(String... armoredPublicKeys) {
		Map<Long, PGPPublicKey> keys = new HashMap<>();
		for (String armoredKey : armoredPublicKeys) {
			if (armoredKey != null) {
				keys.putAll(readPublicKeys(armoredKey));
			}
		}
		return keys;
	}

	@Override
	public void write(int b) {
		if (signaturesToVerify != null) {
			signaturesToVerify.iterator().forEachRemaining(signature -> signature.update((byte) b));
		}

	}

	@Override
	public void write(byte[] b) throws IOException {
		getDestination().write(b);
		if (signaturesToVerify != null) {
			signaturesToVerify.iterator().forEachRemaining(signature -> signature.update(b));
		}
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		getDestination().write(b, off, len);
		if (signaturesToVerify != null) {
			signaturesToVerify.iterator().forEachRemaining(signature -> signature.update(b, off, len));
		}
	}

	@Override
	public void close() {
		if (!getStatus().isOK()) {
			return;
		}
		if (signaturesToVerify == null || signaturesToVerify.isEmpty()) {
			return;
		}
		Iterator<PGPSignature> iterator = signaturesToVerify.iterator();
		while (iterator.hasNext()) {
			PGPSignature signature = iterator.next();
			try {
				if (!signature.verify()) {
					setStatus(new Status(IStatus.ERROR, Activator.ID, Messages.Error_SignatureAndFileDontMatch));
					return;
				}
			} catch (PGPException ex) {
				setStatus(new Status(IStatus.ERROR, Activator.ID, ex.getMessage(), ex));
				return;
			}
		}
		setStatus(Status.OK_STATUS);
	}

	public static Collection<PGPPublicKey> getSigners(IArtifactDescriptor artifact) {
		try {
			return getSignatures(artifact).stream() //
					.mapToLong(PGPSignature::getKeyID) //
					.mapToObj(Long::valueOf) //
					.map(knownKeys::get) //
					.filter(Objects::nonNull).collect(Collectors.toSet());
		} catch (IOException | PGPException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, e.getMessage(), e));
			return Collections.emptyList();
		}
	}

	public static void discardKnownKeys() {
		knownKeys.clear();
	}
}
