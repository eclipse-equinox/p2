/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.examples.rcp.sdkbundlevisibility.p2;

import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.ui.Policy;

/**
 * AllIUsAreVisiblePolicy redefines the IU visibility.  In this
 * example, the policy is declared using OSGi declarative services.
 * The declaration is in the policy_component.xml file.
 * 
 * @since 3.5
 */
public class AllIUsAreVisiblePolicy extends Policy {
	public AllIUsAreVisiblePolicy() {
		// XXX Use the pref-based repository manipulator
		setRepositoryPreferencePageId(PreferenceConstants.PREF_PAGE_SITES);
		
		// XXX All available IU's should be shown, not just groups/features
		setVisibleAvailableIUQuery(QueryUtil.createIUAnyQuery());
		// XXX All installed IU's should be shown, not just the user-installed.
		setVisibleInstalledIUQuery(QueryUtil.createIUAnyQuery());
	}
}
