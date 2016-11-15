/*******************************************************************************
 * Copyright (c) 2016 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mikael Barbero (Eclipse Foundation) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.ProfileScope;
import org.osgi.service.prefs.Preferences;

public class LastAutoCheckForUpdateMemo {

	private static final String LAST_CHECK_FOR_UPDATE__DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"; //$NON-NLS-1$

	private final Preferences prefs;

	public LastAutoCheckForUpdateMemo(IAgentLocation agentLocation) {
		this.prefs = new ProfileScope(agentLocation, IProfileRegistry.SELF).getNode(AutomaticUpdatePlugin.PLUGIN_ID);
	}

	public Date store(Date datetime) {
		prefs.put(PreferenceConstants.PREF_LAST_AUTO_CHECK_FOR_UPDATES, toString(datetime));
		return datetime;
	}

	public Date readAndStoreIfAbsent(Date toStore) {
		Date date = read();
		if (date == null) {
			return store(toStore);
		}
		return date;
	}

	public Date read() {
		String lastCheckDateString = prefs.get(PreferenceConstants.PREF_LAST_AUTO_CHECK_FOR_UPDATES, null);
		if (lastCheckDateString == null || lastCheckDateString.length() == 0) {
			return null;
		}
		return valueOf(lastCheckDateString);
	}

	private static String toString(Date date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(LAST_CHECK_FOR_UPDATE__DATE_FORMAT, Locale.US);
		return dateFormat.format(date);
	}

	private static Date valueOf(String date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(LAST_CHECK_FOR_UPDATE__DATE_FORMAT, Locale.US);
		try {
			return dateFormat.parse(date);
		} catch (ParseException e) {
			AutomaticUpdatePlugin.getDefault().getLog().log(new Status(IStatus.ERROR, AutomaticUpdatePlugin.PLUGIN_ID, e.getMessage(), e));
			return null;
		}
	}

}
