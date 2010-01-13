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
