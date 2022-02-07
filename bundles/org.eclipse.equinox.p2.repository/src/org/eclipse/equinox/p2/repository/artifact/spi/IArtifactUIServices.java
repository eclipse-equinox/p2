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
package org.eclipse.equinox.p2.repository.artifact.spi;

import java.io.File;
import java.security.cert.Certificate;
import java.util.*;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.eclipse.equinox.p2.core.UIServices;
import org.eclipse.equinox.p2.core.UIServices.TrustInfo;
import org.eclipse.equinox.p2.metadata.IArtifactKey;

/**
 * An interface optionally implemented by {@link UIServices} to provide richer
 * information for the users. In particular, the users often wish to know which
 * artifacts are signed by which certificates or which keys when they are
 * {@link UIServices#getTrustInfo(Certificate[][], Collection, String[])
 * prompted} whether to trust such certificates or keys.
 *
 * @since 2.6
 *
 * @see UIServices#getTrustInfo(Certificate[][], Collection, String[])
 */
public interface IArtifactUIServices {

	/**
	 * Opens a UI prompt to capture information about trusted content.
	 *
	 * @param untrustedCertificateChains a map from untrusted certificate chains to
	 *                                   the set of keys of the artifacts signed by
	 *                                   that chain.
	 * @param untrustedPGPKeys           a map of untrusted PGP public keys to the
	 *                                   set of keys of the artifacts signed by that
	 *                                   key.
	 * @param unsignedArtifacts          a set of keys of the artifacts that are not
	 *                                   signed.
	 * @param artifactFiles              a map from artifact keys to the file
	 *                                   associated with that artifact key.
	 *
	 * @return the TrustInfo that describes the user's choices for trusting
	 *         certificates, keys, and unsigned content.
	 *
	 * @see #getTrustInfo(UIServices, Map, Map, Set, Map)
	 * @see UIServices
	 */
	TrustInfo getTrustInfo( //
			Map<List<Certificate>, Set<IArtifactKey>> untrustedCertificateChains, //
			Map<PGPPublicKey, Set<IArtifactKey>> untrustedPGPKeys, //
			Set<IArtifactKey> unsignedArtifacts, //
			Map<IArtifactKey, File> artifactFiles);

	/**
	 * Opens a UI prompt to capture information about trusted content. This
	 * implementation is useful for delegating to an old-style
	 * {@link UIServices#getTrustInfo(Certificate[][], Collection, String[])
	 * UIServices implementation} that does not support the IArtifactUIServices
	 * interface.
	 *
	 * @param uiServices                 the delegate UI services.
	 * @param untrustedCertificateChains a map from untrusted certificate chains to
	 *                                   the set of keys of the artifacts signed by
	 *                                   that chain.
	 * @param untrustedPGPKeys           a map of untrusted PGP public keys to the
	 *                                   set of keys of the artifacts signed by that
	 *                                   key.
	 * @param unsignedArtifacts          a set of keys of the artifacts that are not
	 *                                   signed.
	 * @param artifactFiles              a map from artifact keys to the file
	 *                                   associated with that artifact key.
	 *
	 * @return the TrustInfo that describes the user's choices for trusting
	 *         certificates, keys, and unsigned content.
	 *
	 * @see #getTrustInfo(Map, Map, Set, Map)
	 * @see UIServices#getTrustInfo(Certificate[][], Collection, String[])
	 */
	static TrustInfo getTrustInfo(UIServices uiServices, //
			Map<List<Certificate>, Set<IArtifactKey>> untrustedCertificateChains, //
			Map<PGPPublicKey, Set<IArtifactKey>> untrustedPGPKeys, //
			Set<IArtifactKey> unsignedArtifacts, //
			Map<IArtifactKey, File> artifactFiles) {
		Certificate[][] unTrustedCertificateChainsArray = untrustedCertificateChains.keySet().stream()
				.map(c -> c.toArray(Certificate[]::new)).toArray(Certificate[][]::new);
		String[] details = unsignedArtifacts.isEmpty() ? null
				: unsignedArtifacts.stream().map(artifactFiles::get).map(Objects::toString).toArray(String[]::new);
		return uiServices.getTrustInfo(unTrustedCertificateChainsArray, untrustedPGPKeys.keySet(), details);
	}
}
