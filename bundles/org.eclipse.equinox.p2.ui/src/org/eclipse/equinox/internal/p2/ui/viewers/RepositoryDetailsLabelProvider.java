/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.viewers;

import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.graphics.Image;

/**
 * Label provider for repository elements.  The column structure is
 * assumed to be known by the caller who sets up the columns
 *
 * @since 3.5
 */
public class RepositoryDetailsLabelProvider extends LabelProvider implements ITableLabelProvider {
	public static final int COL_NAME = 0;
	public static final int COL_LOCATION = 1;
	public static final int COL_ENABLEMENT = 2;

	@Override
	public Image getImage(Object obj) {
		if (obj instanceof ProvElement) {
			return ((ProvElement) obj).getImage(obj);
		}
		if (obj instanceof IArtifactRepository) {
			return ProvUIImages.getImage(ProvUIImages.IMG_ARTIFACT_REPOSITORY);
		}
		if (obj instanceof IMetadataRepository) {
			return ProvUIImages.getImage(ProvUIImages.IMG_METADATA_REPOSITORY);
		}
		return null;
	}

	@Override
	public Image getColumnImage(Object element, int index) {
		if (index == 0) {
			return getImage(element);
		}
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {

		switch (columnIndex) {
			case COL_NAME :
				if (element instanceof IRepositoryElement<?>) {
					String name = ((IRepositoryElement<?>) element).getName();
					if (name != null) {
						return name;
					}
				}
				if (element instanceof IRepository<?>) {
					String name = ((IRepository<?>) element).getName();
					if (name != null) {
						return name;
					}
				}
				return ""; //$NON-NLS-1$
			case COL_LOCATION :
				if (element instanceof IRepository<?>) {
					return TextProcessor.process(URIUtil.toUnencodedString(((IRepository<?>) element).getLocation()));
				}
				if (element instanceof IRepositoryElement<?>) {
					return TextProcessor.process(URIUtil.toUnencodedString(((IRepositoryElement<?>) element).getLocation()));
				}
				break;
			case COL_ENABLEMENT :
				if (element instanceof MetadataRepositoryElement) {
					return ((MetadataRepositoryElement) element).isEnabled() ? ProvUIMessages.RepositoryDetailsLabelProvider_Enabled : ProvUIMessages.RepositoryDetailsLabelProvider_Disabled;
				}

		}
		return null;
	}

	public String getClipboardText(Object element, String columnDelimiter) {
		StringBuilder result = new StringBuilder();
		result.append(getColumnText(element, COL_NAME));
		result.append(columnDelimiter);
		result.append(getColumnText(element, COL_LOCATION));
		result.append(columnDelimiter);
		result.append(getColumnText(element, COL_ENABLEMENT));
		return result.toString();
	}
}
