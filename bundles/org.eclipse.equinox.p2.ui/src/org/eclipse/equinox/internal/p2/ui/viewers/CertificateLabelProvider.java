/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	public Image getImage(Object element) {
		return null;
	}

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

	public void addListener(ILabelProviderListener listener) {
		// do nothing
	}

	public void dispose() {
		// do nothing
	}

	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	public void removeListener(ILabelProviderListener listener) {
		// do nothing
	}

}
