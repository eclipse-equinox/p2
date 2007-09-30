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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.core.repository.IRepositoryInfo;
import org.eclipse.equinox.p2.ui.ProvisioningUtil;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * PropertyPage that shows a repository's properties
 * 
 * @since 3.4
 */
public class RepositoryPropertyPage extends PropertyPage {

	IRepositoryInfo repository;
	private RepositoryGroup repoGroup;

	protected Control createContents(Composite parent) {
		noDefaultAndApplyButton();
		this.repository = getRepository();

		if (repository == null) {
			Label label = new Label(parent, SWT.DEFAULT);
			label.setText(ProvUIMessages.RepositoryPropertyPage_NoRepoSelected);
		}
		repoGroup = createRepositoryGroup(parent);
		// exists
		Dialog.applyDialogFont(repoGroup.getComposite());
		verifyComplete();
		return repoGroup.getComposite();
	}

	protected RepositoryGroup createRepositoryGroup(Composite parent) {
		return new RepositoryGroup(parent, repository, new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				verifyComplete();
			}
		}, false, null, null); // these don't matter since repo already
	}

	public boolean performOk() {
		String nameValue = repoGroup.getRepositoryName();
		if (repository != null && nameValue != null && !nameValue.equals(repository.getName())) {
			// TODO HACK - if I could get event notification from core, I
			// wouldn't have to call the
			// util class
			ProvisioningUtil.setRepositoryName(repository, nameValue);
		}
		return true;
	}

	void verifyComplete() {
		if (repoGroup == null) {
			return;
		}
		IStatus status = repoGroup.verify();
		setValid(status.isOK());
	}

	protected IRepositoryInfo getRepository() {
		return (IRepositoryInfo) getElement().getAdapter(IRepositoryInfo.class);
	}
}
