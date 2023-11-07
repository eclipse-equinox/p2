/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
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
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.model.CategoryElement;
import org.eclipse.equinox.internal.p2.ui.model.IIUElement;
import org.eclipse.equinox.internal.p2.ui.viewers.IUColumnConfig;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * A class that handles filtering IU's based on a supplied matching string.
 *
 * @since 3.4
 */
public class AvailableIUPatternFilter extends PatternFilter {

	boolean checkName, checkDescription, checkVersion, checkId = false;
	String patternString;

	/**
	 * Create a new instance of a AvailableIUPatternFilter
	 */
	public AvailableIUPatternFilter(IUColumnConfig[] columnConfig) {
		super();
		for (IUColumnConfig element : columnConfig) {
			int field = element.getColumnType();
			switch (field) {
			case IUColumnConfig.COLUMN_ID:
				checkId = true;
				break;
			case IUColumnConfig.COLUMN_NAME:
				checkName = true;
				break;
			case IUColumnConfig.COLUMN_DESCRIPTION:
				checkDescription = true;
				break;
			case IUColumnConfig.COLUMN_VERSION:
				checkVersion = true;
				break;
			default:
				break;
			}
		}

	}

	@Override
	public boolean isElementSelectable(Object element) {
		return element instanceof IIUElement && !(element instanceof CategoryElement);
	}

	/*
	 * Overridden to remember the pattern string for an optimization in
	 * isParentMatch
	 */
	@Override
	public void setPattern(String patternString) {
		super.setPattern(patternString);
		this.patternString = patternString;
	}

	/*
	 * Overridden to avoid getting children unless there is actually a filter.
	 */
	@Override
	protected boolean isParentMatch(Viewer viewer, Object element) {
		if (patternString == null || patternString.length() == 0)
			return true;
		return super.isParentMatch(viewer, element);
	}

	@Override
	protected boolean isLeafMatch(Viewer viewer, Object element) {
		if (element instanceof CategoryElement) {
			return false;
		}

		String text = null;
		if (element instanceof IIUElement) {
			IInstallableUnit iu = ((IIUElement) element).getIU();
			if (checkName) {
				// Get the iu name in the default locale
				text = iu.getProperty(IInstallableUnit.PROP_NAME, null);
				if (text != null && wordMatches(text))
					return true;
				// Get the iu description in the default locale
				text = iu.getProperty(IInstallableUnit.PROP_DESCRIPTION, null);
				if (text != null && wordMatches(text))
					return true;
			}
			if (checkId || (checkName && text == null)) {
				text = iu.getId();
				if (wordMatches(text)) {
					return true;
				}
			}
			if (!checkName && checkDescription) {
				// Get the iu description in the default locale
				text = iu.getProperty(IInstallableUnit.PROP_DESCRIPTION, null);
				if (text != null && wordMatches(text))
					return true;
			}
			if (checkVersion) {
				text = iu.getVersion().toString();
				if (wordMatches(text))
					return true;
			}
		}
		return false;
	}
}
