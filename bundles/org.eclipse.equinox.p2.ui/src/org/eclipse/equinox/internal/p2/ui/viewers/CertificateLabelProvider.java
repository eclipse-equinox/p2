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

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
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
		Certificate cert = null;
		if (element instanceof TreeNode) {
			cert = (Certificate) ((TreeNode) element).getValue();
		}
		if (cert != null) {
			X500PrincipalHelper principalHelper = new X500PrincipalHelper(((X509Certificate) cert).getSubjectX500Principal());
			return principalHelper.getCN() + "; " + principalHelper.getOU() + "; " + principalHelper.getO(); //$NON-NLS-1$ //$NON-NLS-2$
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

}
