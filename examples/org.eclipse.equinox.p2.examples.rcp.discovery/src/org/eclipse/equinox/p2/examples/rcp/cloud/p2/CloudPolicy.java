/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
package org.eclipse.equinox.p2.examples.rcp.cloud.p2;

import org.eclipse.equinox.p2.ui.Policy;

/**
 * CloudPolicy defines the RCP Cloud Example policies for the p2 UI. The policy
 * is registered as an OSGi service when the example bundle starts.
 * 
 * @since 3.5
 */
public class CloudPolicy extends Policy {
	public CloudPolicy() {
		// XXX User has no visibility for repos
		setRepositoriesVisible(false);
		
		// XXX Default view is by category
		setGroupByCategory(true);
	}
}
