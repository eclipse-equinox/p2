/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Andrew Overholt <overholt@redhat.com> - Fix for Bug 197970  
 *        	[prov] unset Profile name causes exception bringing up profile properties
 *******************************************************************************/
package org.eclipse.equinox.prov.ui.dialogs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.prov.engine.Profile;
import org.eclipse.equinox.prov.ui.ProvUIActivator;
import org.eclipse.equinox.prov.ui.internal.ProvUIMessages;
import org.eclipse.equinox.prov.ui.model.ProfileFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * A ProfileGroup is a reusable UI component that displays and edits the
 * properties of a profile. It can be used in different dialogs that manipulate
 * or define profiles.
 * 
 * @since 3.4
 * 
 */
public class ProfileGroup {

	// FIXME: temporary for M1; should make flavor a dropdown, populated
	//		 via a query for all flavors in known repositories
	static private String FLAVOR_DEFAULT = "tooling"; //$NON-NLS-1$

	Text id;
	Text location;
	Text cache;
	Text name;
	Text description;
	Text flavor;
	Text environments;
	Text nl;
	Profile profile;

	public ProfileGroup(final Composite parent, Profile profile, ModifyListener listener) {
		this.profile = profile;

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		composite.setLayout(layout);
		GridData data = new GridData();
		data.widthHint = 350;
		composite.setLayoutData(data);

		// Grid data for most text controls
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;

		Label label = new Label(composite, SWT.NONE);
		label.setText(ProvUIMessages.ProfileGroup_ID);
		id = new Text(composite, SWT.BORDER);
		id.setLayoutData(gd);
		if (profile == null && listener != null) {
			id.addModifyListener(listener);
		} else {
			id.setEditable(false);
		}

		label = new Label(composite, SWT.NONE);
		label.setText(ProvUIMessages.ProfileGroup_InstallFolder);
		location = new Text(composite, SWT.BORDER);
		location.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		location.addModifyListener(listener);
		Button locationButton = new Button(composite, SWT.PUSH);
		locationButton.setText(ProvUIMessages.ProfileGroup_Browse);
		locationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.APPLICATION_MODAL);
				dialog.setMessage(ProvUIMessages.ProfileGroup_SelectProfileMessage);
				String dir = dialog.open();
				if (dir != null) {
					location.setText(dir);
				}
			}
		});

		label = new Label(composite, SWT.NONE);
		label.setText(ProvUIMessages.ProfileGroup_Cache);
		cache = new Text(composite, SWT.BORDER);
		cache.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		cache.addModifyListener(listener);
		locationButton = new Button(composite, SWT.PUSH);
		locationButton.setText(ProvUIMessages.ProfileGroup_Browse);
		locationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.APPLICATION_MODAL);
				dialog.setMessage(ProvUIMessages.ProfileGroup_SelectBundlePoolCache);
				String dir = dialog.open();
				if (dir != null) {
					cache.setText(dir);
				}
			}
		});

		label = new Label(composite, SWT.NONE);
		label.setText(ProvUIMessages.ProfileGroup_Name);
		name = new Text(composite, SWT.BORDER);
		name.setLayoutData(gd);
		name.addModifyListener(listener);
		label = new Label(composite, SWT.NONE);
		label.setText(ProvUIMessages.ProfileGroup_Description);
		description = new Text(composite, SWT.BORDER);
		description.setLayoutData(gd);
		description.addModifyListener(listener);

		label = new Label(composite, SWT.NONE);
		label.setText(ProvUIMessages.ProfileGroup_Flavor);
		flavor = new Text(composite, SWT.BORDER);
		flavor.setLayoutData(gd);
		flavor.addModifyListener(listener);

		label = new Label(composite, SWT.NONE);
		label.setText(ProvUIMessages.ProfileGroup_Environments);
		environments = new Text(composite, SWT.BORDER);
		environments.setLayoutData(gd);
		environments.addModifyListener(listener);

		label = new Label(composite, SWT.NONE);
		label.setText(ProvUIMessages.ProfileGroup_NL);
		nl = new Text(composite, SWT.BORDER);
		nl.setLayoutData(gd);
		nl.addModifyListener(listener);

		initializeFields();
	}

	private void initializeFields() {
		if (profile == null) {
			location.setText(ProfileFactory.getDefaultLocation());
			environments.setText(ProfileFactory.getDefaultEnvironments());
			nl.setText(ProfileFactory.getDefaultNL());
			flavor.setText(ProfileFactory.getDefaultFlavor());
		} else {
			String value = profile.getProfileId();
			// Should not happen, profiles must have an id, but just in case.
			if (value == null)
				value = ""; //$NON-NLS-1$
			id.setText(value);

			// The remaining values may be null
			value = profile.getValue(Profile.PROP_INSTALL_FOLDER);
			if (value != null) {
				location.setText(value);
			}
			value = profile.getValue(Profile.PROP_CACHE);
			if (value != null) {
				cache.setText(value);
			}

			value = profile.getValue(Profile.PROP_NAME);
			if (value != null) {
				name.setText(value);
			}
			value = profile.getValue(Profile.PROP_DESCRIPTION);
			if (value != null) {
				description.setText(value);
			}
			value = profile.getValue(Profile.PROP_FLAVOR);
			// TODO: temporary for M1; should make flavor a dropdown
			flavor.setText(value != null ? value : FLAVOR_DEFAULT);
			// if (value != null) {
			//     flavor.setText(value);
			// }
			value = profile.getValue(Profile.PROP_ENVIRONMENTS);
			if (value != null) {
				environments.setText(value);
			}
			value = profile.getValue(Profile.PROP_NL);
			if (value != null) {
				nl.setText(value);
			}
		}
	}

	public void updateProfile() {
		// We forced the id field to have content so don't 
		// check its length
		if (profile == null) {
			profile = new Profile(id.getText().trim());
		}
		String value = location.getText().trim();
		if (value.length() > 0) {
			profile.setValue(Profile.PROP_INSTALL_FOLDER, value);
		}

		value = cache.getText().trim();
		if (value.length() > 0) {
			profile.setValue(Profile.PROP_CACHE, value);
		}

		value = name.getText().trim();
		if (value.length() > 0) {
			profile.setValue(Profile.PROP_NAME, value);
		}
		value = description.getText().trim();
		if (value.length() > 0) {
			profile.setValue(Profile.PROP_DESCRIPTION, value);
		}
		value = flavor.getText().trim();
		if (value.length() > 0) {
			profile.setValue(Profile.PROP_FLAVOR, value);
		}
		value = environments.getText().trim();
		if (value.length() > 0) {
			profile.setValue(Profile.PROP_ENVIRONMENTS, value);
		}
		value = nl.getText().trim();
		if (value.length() > 0) {
			profile.setValue(Profile.PROP_NL, value);
		}
	}

	public Composite getComposite() {
		if (id == null) {
			return null;
		}
		return id.getParent();
	}

	public Profile getProfile() {
		return profile;
	}

	public String getProfileId() {
		if (profile != null) {
			return profile.getProfileId();
		}
		return id.getText().trim();
	}

	/**
	 * Return a status indicating the validity of the profile info
	 * 
	 * @return a status indicating the validity of the profile info
	 */
	public IStatus verify() {
		if (id.getText().trim().length() == 0) {
			return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, 0, ProvUIMessages.ProfileGroup_ProfileIDRequired, null);
		}
		if (location.getText().trim().length() == 0) {
			return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, 0, ProvUIMessages.ProfileGroup_ProfileInstallFolderRequired, null);
		}

		// TODO what kind of validation do we perform for other properties?
		return new Status(IStatus.OK, ProvUIActivator.PLUGIN_ID, IStatus.OK, "", null); //$NON-NLS-1$

	}
}
