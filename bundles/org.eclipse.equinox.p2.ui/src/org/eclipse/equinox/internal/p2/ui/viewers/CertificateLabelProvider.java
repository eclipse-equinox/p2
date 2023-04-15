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
import java.util.ArrayList;
import java.util.List;
import org.eclipse.equinox.internal.provisional.security.ui.X500PrincipalHelper;
import org.eclipse.equinox.internal.provisional.security.ui.X509CertificateViewDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

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
				return getText(cert);
			}
		}
		return ""; //$NON-NLS-1$
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

	/**
	 * Returns a string that can be used as readable label for a certificate. This
	 * hides the internal implementation classes needed to produce this label.
	 */
	public static String getText(X509Certificate cert) {
		X500PrincipalHelper principalHelper = new X500PrincipalHelper(cert.getSubjectX500Principal());
		List<String> parts = new ArrayList<>();
		String cn = principalHelper.getCN();
		if (cn != null) {
			parts.add(cn);
		}
		String ou = principalHelper.getOU();
		if (ou != null) {
			parts.add(ou);
		}
		String o = principalHelper.getO();
		if (o != null) {
			parts.add(o);
		}
		return String.join("; ", parts); //$NON-NLS-1$
	}

	/**
	 * Opens a dialog to present detailed information about a certificate. This
	 * hides the internal implementation classes needed open this dialog.
	 */
	public static void openDialog(Shell shell, X509Certificate cert) {
		// create and open dialog for certificate chain
		X509CertificateViewDialog certificateViewDialog = new X509CertificateViewDialog(shell, cert);
		certificateViewDialog.open();
	}
}
