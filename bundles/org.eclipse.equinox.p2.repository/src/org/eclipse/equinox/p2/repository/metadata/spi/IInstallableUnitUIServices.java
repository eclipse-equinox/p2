/*******************************************************************************
 * Copyright (c) 2023 Eclipse contributors and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.p2.repository.metadata.spi;

import java.net.URI;
import java.security.cert.Certificate;
import java.util.*;
import org.eclipse.equinox.p2.core.UIServices;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * An interface optionally implemented by {@link UIServices} to provide
 * confirmation for the origin of installable units being installed.
 *
 * @since 2.7
 */
public interface IInstallableUnitUIServices {
	/**
	 * Trust information returned from an authority trust request.
	 */
	class TrustAuthorityInfo {
		private Collection<URI> trustedAuthorities;
		private boolean save;
		private boolean trustAlways;

		/**
		 * @param trustedAuthorities the trusted authority URIs.
		 * @param save               whether to store the trusted authority URIs.
		 * @param trustAlways        whether to trust all authorities.
		 */
		public TrustAuthorityInfo(Collection<URI> trustedAuthorities, boolean save, boolean trustAlways) {
			this.trustedAuthorities = trustedAuthorities;
			this.save = save;
			this.trustAlways = trustAlways;
		}

		/**
		 * Returns a collection of the authority URIs to be trusted for the requested
		 * operation.
		 *
		 * @return a collection of the authority URIs to be trusted for the requested
		 *         operation.
		 */
		public Collection<URI> getTrustedAuthorities() {
			return trustedAuthorities;
		}

		/**
		 * Returns whether the trusted authority URIs should be persisted for future
		 * operations.
		 *
		 * @return whether the trusted authority URIs should be persisted for future
		 *         operations.
		 */
		public boolean isSave() {
			return save;
		}

		/**
		 * Return whether to always trust all authorities, both during this operation
		 * and for all future operations.
		 *
		 * @return whether to always trust all authorities, both during this operation
		 *         and for all future operations.
		 */
		public boolean isTrustAlways() {
			return trustAlways;
		}
	}

	/**
	 * Opens a UI prompt to capture information about trusted authorities.
	 *
	 * @param siteIUs               a map from each untrusted repository location URI to the
	 *                              installable units originating from that
	 *                              repository.
	 * @param siteCertificates a map from each repository location URI, to the
	 *                              certificate chain of that location's secure
	 *                              connection.
	 *
	 * @return the trust information that describes the user's choices for trusting
	 *         the given authorities.
	 */
	TrustAuthorityInfo getTrustAuthorityInfo(Map<URI, Set<IInstallableUnit>> siteIUs,
			Map<URI, List<Certificate>> siteCertificates);
}
