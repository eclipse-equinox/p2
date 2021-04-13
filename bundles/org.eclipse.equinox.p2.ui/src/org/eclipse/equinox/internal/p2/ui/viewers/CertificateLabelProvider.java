/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package org.eclipse.equinox.internal.p2.ui.viewers;

import java.security.cert.X509Certificate;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.eclipse.equinox.internal.provisional.security.ui.X500PrincipalHelper;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;

/**
 * A label provider that displays X509 certificates.
 */
public class CertificateLabelProvider implements ILabelProvider {

	@Override
	public Image getImage(Object element) {
		return null;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof TreeNode) {
			Object o = ((TreeNode) element).getValue();
			if (o instanceof X509Certificate) {
				X509Certificate cert = (X509Certificate) o;
				X500PrincipalHelper principalHelper = new X500PrincipalHelper(cert.getSubjectX500Principal());
				return principalHelper.getCN() + "; " + principalHelper.getOU() + "; " //$NON-NLS-1$ //$NON-NLS-2$
						+ principalHelper.getO();
			} else if (o instanceof PGPPublicKey) {
				return userFriendlyFingerPrint((PGPPublicKey) o);
			}
		}
		return ""; //$NON-NLS-1$
	}

	private String userFriendlyFingerPrint(PGPPublicKey key) {
		if (key == null) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		boolean spaceSuffix = false;
		for (byte b : key.getFingerprint()) {
			builder.append(String.format("%02X", Byte.toUnsignedInt(b))); //$NON-NLS-1$
			if (spaceSuffix) {
				builder.append(' ');
			}
			spaceSuffix = !spaceSuffix;
		}
		builder.deleteCharAt(builder.length() - 1);
		return builder.toString();
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
		// do nothing
	}

	@Override
	public void dispose() {
		// do nothing
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		// do nothing
	}

}
