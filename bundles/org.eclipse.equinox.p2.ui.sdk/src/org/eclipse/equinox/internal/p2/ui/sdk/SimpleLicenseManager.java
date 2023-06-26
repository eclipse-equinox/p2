/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
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
 *     Genuitec, LLC - added license support
 *     Hannes Wellmann - Bug 574622: Persist remembered accepted licenses
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.eclipse.core.runtime.ILog;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.ProfileScope;
import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.equinox.p2.ui.LicenseManager;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * SimpleLicenseManager is a license manager that keeps track of
 * IInstallableUnit licenses using their UUID.  The licenses ids
 * are stored in the profile's preferences.
 *
 * @since 3.6
 */
public class SimpleLicenseManager extends LicenseManager {
	private final Set<String> accepted = new HashSet<>();
	private final String profileId;

	public SimpleLicenseManager(String profileId) {
		super();
		this.profileId = profileId;
		initializeFromPreferences();
	}

	public SimpleLicenseManager() {
		this(IProfileRegistry.SELF);
	}

	@Override
	public boolean accept(ILicense license) {
		accepted.add(license.getUUID());
		updatePreferences();
		return true;
	}

	@Override
	public boolean reject(ILicense license) {
		accepted.remove(license.getUUID());
		updatePreferences();
		return true;
	}

	@Override
	public boolean isAccepted(ILicense license) {
		return accepted.contains(license.getUUID());
	}

	@Override
	public boolean hasAcceptedLicenses() {
		return !accepted.isEmpty();
	}

	private Preferences getPreferences() {
		IAgentLocation location = ProvSDKUIActivator.getDefault().getProvisioningAgent()
				.getService(IAgentLocation.class);
		return new ProfileScope(location, profileId).getNode(ProvSDKUIActivator.PLUGIN_ID);
	}

	private static final Pattern DIGESTS_DELIMITER = Pattern.compile(","); //$NON-NLS-1$

	private void initializeFromPreferences() {
		Preferences pref = getPreferences();
		if (pref != null) {
			String digestList = pref.get(PreferenceConstants.PREF_LICENSE_DIGESTS, ""); //$NON-NLS-1$
			DIGESTS_DELIMITER.splitAsStream(digestList).filter(s -> !s.isBlank()).map(String::strip)
					.forEach(accepted::add);
		}
	}

	private void updatePreferences() {
		Preferences pref = getPreferences();
		String acceptedLicenseDigests = String.join(",", accepted); //$NON-NLS-1$
		pref.put(PreferenceConstants.PREF_LICENSE_DIGESTS, acceptedLicenseDigests);
		try {
			pref.flush();
		} catch (BackingStoreException e) {
			ILog.of(SimpleLicenseManager.class).error("Persisting remembered licenses failed", e); //$NON-NLS-1$
		}
	}
}
