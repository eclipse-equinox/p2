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
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import org.bouncycastle.bcpg.*;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.repository.spi.PGPPublicKeyService;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

/**
 * Presents information about a key in a format similar to what key servers
 * display.
 *
 * @since 1.2.4
 */
public class PGPPublicKeyViewDialog extends TitleAreaDialog {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'") //$NON-NLS-1$
			.withZone(ZoneOffset.UTC);

	final private PGPPublicKey originalKey;

	final private PGPPublicKeyService keyService;

	private StyledText styledText;

	public PGPPublicKeyViewDialog(Shell parentShell, PGPPublicKey key, PGPPublicKeyService keyService) {
		super(parentShell);
		this.originalKey = key;
		this.keyService = keyService;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(ProvUIMessages.PGPPublicKeyViewDialog_Title);
		if (keyService != null) {
			computeVerifiedCertifications(newShell);
		}
	}

	@Override
	protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(newShellStyle | SWT.RESIZE | SWT.DIALOG_TRIM);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(data);

		styledText = new StyledText(composite, SWT.READ_ONLY | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		styledText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Create a sightly smaller text (mono-space) font.
		FontData[] fontData = JFaceResources.getTextFont().getFontData();
		for (FontData fontDataElement : fontData) {
			fontDataElement.setHeight(fontDataElement.getHeight() - 1);
		}
		Font font = new Font(styledText.getDisplay(), fontData);
		styledText.setFont(font);
		styledText.addDisposeListener(e -> font.dispose());

		GC gc = new GC(styledText);
		gc.setFont(font);
		data.widthHint = convertWidthInCharsToPixels(gc.getFontMetrics(), 110);
		gc.dispose();

		update(originalKey, Set.of());
		return composite;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true).setFocus();
	}

	@SuppressWarnings("nls")
	protected void update(PGPPublicKey key, Set<PGPPublicKey> verifiedCertifications) {
		StyledString content = new StyledString();
		String fingerprint = PGPPublicKeyService.toHexFingerprint(key);

		PublicKeyPacket publicKeyPacket = key.getPublicKeyPacket();
		publicKeyPacket.getAlgorithm();
		content.append(" ");
		content.append(publicKeyPacket instanceof PublicSubkeyPacket ? "sub" : "pub", StyledString.QUALIFIER_STYLER);
		content.append(" ");

		int algorithm = publicKeyPacket.getAlgorithm();
		switch (algorithm) {
		case PublicKeyAlgorithmTags.RSA_GENERAL:
		case PublicKeyAlgorithmTags.RSA_ENCRYPT:
		case PublicKeyAlgorithmTags.RSA_SIGN: {
			content.append("rsa");
			break;
		}
		case PublicKeyAlgorithmTags.DSA: {
			content.append("dsa");
			break;
		}
		case PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT:
		case PublicKeyAlgorithmTags.ELGAMAL_GENERAL: {
			content.append("elgamal");
			break;
		}
		default: {
			content.append("[");
			content.append(Integer.toString(algorithm));
			content.append("]");
			break;
		}

		}
		int bitStrength = key.getBitStrength();
		content.append(Integer.toString(bitStrength));
		content.append("/");
		content.append(fingerprint);

		content.append(" ");
		content.append(DATE_FORMAT.format(key.getCreationTime().toInstant()));

		content.append(" ");
		content.append("\n");

		List<String> users = new ArrayList<>();
		key.getUserIDs().forEachRemaining(users::add);
		if (!users.isEmpty()) {
			for (String user : users) {
				content.append(" ");
				content.append("uid", StyledString.QUALIFIER_STYLER);
				content.append(" ");
				content.append(user, StyledString.COUNTER_STYLER);
				content.append("\n");
			}
		}

		Long subKeyOf = null;

		for (Iterator<PGPSignature> signatures = key.getSignatures(); signatures.hasNext();) {
			PGPSignature signature = signatures.next();
			long keyID = signature.getKeyID();

			if (signature.getSignatureType() == PGPSignature.SUBKEY_BINDING) {
				subKeyOf = keyID;
			}

			content.append(" ");
			content.append("sig", StyledString.QUALIFIER_STYLER);
			content.append(" ");
			content.append(PGPPublicKeyService.toHex(keyID));
			content.append(" ");
			Date creationTime = signature.getCreationTime();
			String formattedCreationTime = DATE_FORMAT.format(creationTime.toInstant());
			content.append(formattedCreationTime);
			long signatureExpirationTime = signature.getHashedSubPackets().getSignatureExpirationTime();
			content.append(" ");
			content.append(signatureExpirationTime == 0 ? formattedCreationTime.replaceAll(".", "_")
					: DATE_FORMAT
							.format(Instant.ofEpochMilli(creationTime.getTime() + 1000 * signatureExpirationTime)));

			content.append(" ");
			Optional<PGPPublicKey> resolvedKey = verifiedCertifications.stream().filter(k -> k.getKeyID() == keyID)
					.findFirst();

			long keyExpirationTime = signature.getHashedSubPackets().getKeyExpirationTime();
			content.append(keyExpirationTime == 0 || resolvedKey == null || !resolvedKey.isPresent()
					? formattedCreationTime.replaceAll(".", "_")
					: DATE_FORMAT.format(Instant
							.ofEpochMilli(resolvedKey.get().getCreationTime().getTime() + 1000 * keyExpirationTime)));

			if (resolvedKey != null && resolvedKey.isPresent()) {
				content.append(" ");
				content.append(getLabel(resolvedKey.get()), StyledString.COUNTER_STYLER);
			}

			content.append("\n");
		}

		styledText.setText(content.getString());
		styledText.setStyleRanges(content.getStyleRanges());

		List<String> title = new ArrayList<>();
		if (subKeyOf != null) {
			long keyID = subKeyOf;
			verifiedCertifications.stream().filter(k -> k.getKeyID() == keyID).findFirst()
					.ifPresentOrElse(k -> title.add(getLabel(k)), () -> title.add(PGPPublicKeyService.toHex(keyID)));
		}
		title.add((subKeyOf == null ? "" : "sub ") + (users.isEmpty() ? fingerprint : users.get(0)));

		setTitle(String.join("\n", title));
	}

	private String getLabel(PGPPublicKey key) {
		Iterator<String> userIDs = key.getUserIDs();
		if (userIDs.hasNext()) {
			return userIDs.next();

		}
		return PGPPublicKeyService.toHexFingerprint(key);
	}

	private void computeVerifiedCertifications(Shell shell) {
		Display display = shell.getDisplay();
		new Job(PGPPublicKeyViewDialog.class.getName()) {
			{
				setSystem(true);
				setPriority(Job.SHORT);
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				synchronized (keyService) {
					PGPPublicKey enhancedKey = keyService.addKey(originalKey);
					Set<PGPPublicKey> verifiedCertifications = keyService.getVerifiedCertifications(originalKey);
					display.asyncExec(() -> {
						if (!shell.isDisposed()) {
							update(enhancedKey, verifiedCertifications);
						}
					});
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}
}
