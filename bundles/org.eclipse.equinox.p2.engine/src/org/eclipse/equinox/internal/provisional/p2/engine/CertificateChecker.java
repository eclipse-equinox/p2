/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.engine;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUI;
import org.eclipse.osgi.service.security.TrustEngine;
import org.eclipse.osgi.signedcontent.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.util.tracker.ServiceTracker;

public class CertificateChecker {
	private ArrayList artifacts;
	private ServiceTracker trustEngineTracker;

	public CertificateChecker() {
		artifacts = new ArrayList();
		trustEngineTracker = new ServiceTracker(EngineActivator.getContext(), TrustEngine.class.getName(), null);
		trustEngineTracker.open();
	}

	public IStatus start() {
		return checkCertificates();
	}

	private IStatus checkCertificates() {
		SignedContentFactory verifierFactory = (SignedContentFactory) ServiceHelper.getService(EngineActivator.getContext(), SignedContentFactory.class.getName());
		IServiceUI serviceUI = (IServiceUI) ServiceHelper.getService(EngineActivator.getContext(), IServiceUI.class.getName());
		SignedContent content = null;
		SignerInfo[] signerInfo = null;
		ArrayList untrusted = new ArrayList();
		ArrayList unsigned = new ArrayList();
		ArrayList untrustedChain = new ArrayList();
		IStatus status = Status.OK_STATUS;
		if (artifacts.size() == 0 || serviceUI == null)
			return status;
		TrustEngine trustEngine = (TrustEngine) trustEngineTracker.getService();
		for (Iterator it = artifacts.iterator(); it.hasNext();) {
			File artifact = (File) it.next();
			try {
				content = verifierFactory.getSignedContent(artifact);
				if (!content.isSigned()) {
					unsigned.add(artifact);
					continue;
				}
				signerInfo = content.getSignerInfos();
			} catch (GeneralSecurityException e) {
				return new Status(IStatus.ERROR, EngineActivator.ID, Messages.CertificateChecker_SignedContentError, e);
			} catch (IOException e) {
				return new Status(IStatus.ERROR, EngineActivator.ID, Messages.CertificateChecker_SignedContentIOError, e);
			}
			for (int i = 0; i < signerInfo.length; i++) {
				Certificate[] certificateChain = signerInfo[i].getCertificateChain();
				try {
					Certificate trustAnchor = trustEngine.findTrustAnchor(certificateChain);
					if (trustAnchor == null) {
						if (!untrusted.contains(certificateChain[0])) {
							untrusted.add(certificateChain[0]);
							untrustedChain.add(certificateChain);
						}
					}
				} catch (IOException e) {
					return new Status(IStatus.ERROR, EngineActivator.ID, Messages.CertificateChecker_KeystoreConnectionError, e);
				}
			}
		}
		status = checkUnsigned(serviceUI, unsigned);
		if (status.getSeverity() == IStatus.ERROR || status.getSeverity() == IStatus.CANCEL)
			return status;
		if (!untrusted.isEmpty()) {
			Certificate[][] certificates;
			certificates = new Certificate[untrustedChain.size()][];
			for (int i = 0; i < untrustedChain.size(); i++) {
				certificates[i] = (Certificate[]) untrustedChain.get(i);
			}
			Certificate[] trustedCertificates = serviceUI.showCertificates(certificates);
			if (trustedCertificates == null) {
				return new Status(IStatus.CANCEL, EngineActivator.ID, Messages.CertificateChecker_CertificateRejected);
			}
			for (int i = 0; i < trustedCertificates.length; i++) {
				untrusted.remove(trustedCertificates[i]);
			}
			if (untrusted.size() > 0)
				return new Status(IStatus.CANCEL, EngineActivator.ID, Messages.CertificateChecker_CertificateRejected);
			// add newly trusted certificates to trust engine
			for (int i = 0; i < trustedCertificates.length; i++) {
				try {
					trustEngine.addTrustAnchor(trustedCertificates[i], trustedCertificates[i].toString());
				} catch (IOException e) {
					//just return an INFO so the user can proceed with the install
					return new Status(IStatus.INFO, EngineActivator.ID, Messages.CertificateChecker_KeystoreConnectionError, e);
				} catch (GeneralSecurityException e) {
					return new Status(IStatus.INFO, EngineActivator.ID, Messages.CertificateChecker_CertificateError, e);
				}
			}
		}

		return status;
	}

	/**
	 * Perform necessary checks on unsigned content.
	 */
	private IStatus checkUnsigned(IServiceUI serviceUI, ArrayList unsigned) {
		if (unsigned.isEmpty())
			return Status.OK_STATUS;
		String policy = EngineActivator.getContext().getProperty(EngineActivator.PROP_UNSIGNED_POLICY);
		//if the policy says we should always allow it, there is nothing more to do
		if (EngineActivator.UNSIGNED_ALLOW.equals(policy))
			return Status.OK_STATUS;
		//if the policy says we should never allow unsigned, then fail
		if (EngineActivator.UNSIGNED_FAIL.equals(policy))
			return new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.CertificateChecker_UnsignedNotAllowed, unsigned));
		//default policy is to prompt for confirmation if possible
		String[] details = new String[unsigned.size()];
		for (int i = 0; i < details.length; i++) {
			details[i] = unsigned.get(i).toString();
		}
		if (serviceUI != null && !serviceUI.promptForUnsignedContent(details))
			return Status.CANCEL_STATUS;
		return Status.OK_STATUS;
	}

	public void add(File toAdd) {
		artifacts.add(toAdd);
	}

	public void add(Object[] toAdd) {
		for (int i = 0; i < toAdd.length; i++) {
			if (toAdd[i] instanceof File)
				add((File) toAdd[i]);
		}
	}
}
