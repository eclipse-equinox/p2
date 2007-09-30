/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ui.dialogs;

import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;

/**
 * PropertyPage that shows a repository's properties
 * 
 * @since 3.4
 */
public class RepositoryImplementationPropertyPage extends RepositoryPropertyPage {

	protected RepositoryGroup createRepositoryGroup(Composite parent) {
		return new RepositoryImplementationGroup(parent, repository, new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				verifyComplete();
			}
		}, false, null, null); // these don't matter since repo already
	}
}
