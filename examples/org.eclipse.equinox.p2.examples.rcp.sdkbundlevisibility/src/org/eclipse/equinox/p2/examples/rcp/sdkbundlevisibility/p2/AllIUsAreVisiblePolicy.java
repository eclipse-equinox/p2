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

import org.eclipse.equinox.internal.provisional.p2.ui.policy.ColocatedRepositoryManipulator;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IUViewQueryContext;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;

/**
 * CloudPolicy defines the RCP Cloud Example policies for the p2 UI. The policy
 * is declared as an OSGi service in the policy_component.xml file.
 * 
 * @since 3.5
 */
public class AllIUsAreVisiblePolicy extends Policy {
	public AllIUsAreVisiblePolicy() {
		// XXX Use the pref-based repository manipulator
		setRepositoryManipulator(new ColocatedRepositoryManipulator(this, PreferenceConstants.PREF_PAGE_SITES));
		
		// XXX Create an IUViewQueryContext to change the visibility of the IUs shown in the UI.
        // XXX Show the flat (non-categorized) view by default.
		IUViewQueryContext context = new IUViewQueryContext(IUViewQueryContext.AVAILABLE_VIEW_FLAT);
		// XXX All available IU's should be shown, not just groups/features
		context.setVisibleAvailableIUProperty(null);
		// XXX All installed IU's should be shown, not just the user-installed.
		context.setVisibleInstalledIUProperty(null);
		
		setQueryContext(context);
	}
}
