/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
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
package org.eclipse.equinox.internal.p2.ui.admin.dialogs;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.ui.admin.*;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * A ProfileGroup is a reusable UI component that displays the
 * properties of a profile.  It can be used to create a new profile.
 * . It can be used in different dialogs that manipulate
 * or define profiles.
 * 
 * @since 3.4
 * 
 */
public class ProfileGroup {

	Text id;
	Text location;
	Text cache;
	Text name;
	Text description;
	Text environments;
	Text nl;
	IProfile profile;

	public ProfileGroup(final Composite parent, IProfile profile, ModifyListener listener) {
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
		label.setText(ProvAdminUIMessages.ProfileGroup_ID);
		id = new Text(composite, SWT.BORDER);
		id.setLayoutData(gd);
		setEditable(id, profile == null, listener);

		label = new Label(composite, SWT.NONE);
		label.setText(ProvAdminUIMessages.ProfileGroup_InstallFolder);
		location = new Text(composite, SWT.BORDER);
		location.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		setEditable(location, profile == null, listener);

		Button locationButton = new Button(composite, SWT.PUSH);
		locationButton.setText(ProvAdminUIMessages.ProfileGroup_Browse);
		locationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.APPLICATION_MODAL);
				dialog.setMessage(ProvAdminUIMessages.ProfileGroup_SelectProfileMessage);
				String dir = dialog.open();
				if (dir != null) {
					location.setText(dir);
				}
			}
		});
		setEditable(locationButton, profile == null, listener);

		label = new Label(composite, SWT.NONE);
		label.setText(ProvAdminUIMessages.ProfileGroup_Cache);
		cache = new Text(composite, SWT.BORDER);
		cache.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		setEditable(cache, profile == null, listener);

		locationButton = new Button(composite, SWT.PUSH);
		locationButton.setText(ProvAdminUIMessages.ProfileGroup_Browse2);
		locationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.APPLICATION_MODAL);
				dialog.setMessage(ProvAdminUIMessages.ProfileGroup_SelectBundlePoolCache);
				String dir = dialog.open();
				if (dir != null) {
					cache.setText(dir);
				}
			}
		});
		setEditable(locationButton, profile == null, listener);

		label = new Label(composite, SWT.NONE);
		label.setText(ProvAdminUIMessages.ProfileGroup_Name);
		name = new Text(composite, SWT.BORDER);
		name.setLayoutData(gd);
		setEditable(name, profile == null, listener);

		label = new Label(composite, SWT.NONE);
		label.setText(ProvAdminUIMessages.ProfileGroup_Description);
		description = new Text(composite, SWT.BORDER);
		description.setLayoutData(gd);
		setEditable(description, profile == null, listener);

		label = new Label(composite, SWT.NONE);
		label.setText(ProvAdminUIMessages.ProfileGroup_Environments);
		environments = new Text(composite, SWT.BORDER);
		environments.setLayoutData(gd);
		setEditable(environments, profile == null, listener);

		label = new Label(composite, SWT.NONE);
		label.setText(ProvAdminUIMessages.ProfileGroup_NL);
		nl = new Text(composite, SWT.BORDER);
		nl.setLayoutData(gd);
		setEditable(nl, profile == null, listener);

		initializeFields();
	}

	private void initializeFields() {
		if (profile == null) {
			location.setText(ProfileFactory.getDefaultLocation());
			environments.setText(ProfileFactory.getDefaultEnvironments());
			nl.setText(ProfileFactory.getDefaultNL());
		} else {
			String value = profile.getProfileId();
			// Should not happen, profiles must have an id, but just in case.
			if (value == null)
				value = ""; //$NON-NLS-1$
			id.setText(value);

			// The remaining values may be null
			value = profile.getProperty(IProfile.PROP_INSTALL_FOLDER);
			if (value != null) {
				location.setText(value);
			}
			value = profile.getProperty(IProfile.PROP_CACHE);
			if (value != null) {
				cache.setText(value);
			}

			value = profile.getProperty(IProfile.PROP_NAME);
			if (value != null) {
				name.setText(value);
			}
			value = profile.getProperty(IProfile.PROP_DESCRIPTION);
			if (value != null) {
				description.setText(value);
			}

			value = profile.getProperty(IProfile.PROP_ENVIRONMENTS);
			if (value != null) {
				environments.setText(value);
			}
			value = profile.getProperty(IProfile.PROP_NL);
			if (value != null) {
				nl.setText(value);
			}
		}
	}

	public Map<String, String> getProfileProperties() {
		if (profile == null) {
			Map<String, String> profileProperties = new HashMap<String, String>();

			String value = location.getText().trim();
			if (value.length() > 0) {
				profileProperties.put(IProfile.PROP_INSTALL_FOLDER, value);
			}

			value = cache.getText().trim();
			if (value.length() > 0) {
				profileProperties.put(IProfile.PROP_CACHE, value);
			}
			value = name.getText().trim();
			if (value.length() > 0) {
				profileProperties.put(IProfile.PROP_NAME, value);
			}
			value = description.getText().trim();
			if (value.length() > 0) {
				profileProperties.put(IProfile.PROP_DESCRIPTION, value);
			}
			value = environments.getText().trim();
			if (value.length() > 0) {
				profileProperties.put(IProfile.PROP_ENVIRONMENTS, value);
			}
			value = nl.getText().trim();
			if (value.length() > 0) {
				profileProperties.put(IProfile.PROP_NL, value);
			}
			return profileProperties;
		}
		return profile.getProperties();
	}

	public Composite getComposite() {
		if (id == null) {
			return null;
		}
		return id.getParent();
	}

	public IProfile getProfile() {
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
			return new Status(IStatus.ERROR, ProvAdminUIActivator.PLUGIN_ID, 0, ProvAdminUIMessages.ProfileGroup_ProfileIDRequired, null);
		}
		if (location.getText().trim().length() == 0) {
			return new Status(IStatus.ERROR, ProvAdminUIActivator.PLUGIN_ID, 0, ProvAdminUIMessages.ProfileGroup_ProfileInstallFolderRequired, null);
		}

		// TODO what kind of validation do we perform for other properties?
		return new Status(IStatus.OK, ProvAdminUIActivator.PLUGIN_ID, IStatus.OK, "", null); //$NON-NLS-1$

	}

	private void setEditable(Control control, boolean editable, ModifyListener listener) {
		if (control instanceof Text) {
			Text text = (Text) control;
			text.setEditable(editable);
			if (listener != null && editable)
				text.addModifyListener(listener);
		} else {
			control.setEnabled(editable);
		}
	}
}
