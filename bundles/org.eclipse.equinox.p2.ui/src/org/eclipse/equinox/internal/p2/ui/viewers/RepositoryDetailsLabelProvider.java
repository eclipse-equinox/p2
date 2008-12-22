/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.viewers;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.MetadataRepositoryElement;
import org.eclipse.equinox.internal.p2.ui.model.ProvElement;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IRepositoryElement;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
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

	public Image getColumnImage(Object element, int index) {
		if (index == 0) {
			return getImage(element);
		}
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {

		switch (columnIndex) {
			case COL_NAME :
				String name = ((IRepositoryElement) element).getName();
				if (name != null) {
					return name;
				}
				return ""; //$NON-NLS-1$
			case COL_LOCATION :
				if (element instanceof IRepository) {
					return ((IRepository) element).getLocation().toString();
				}
				if (element instanceof IRepositoryElement) {
					return ((IRepositoryElement) element).getLocation().toString();
				}
				break;
			case COL_ENABLEMENT :
				if (element instanceof MetadataRepositoryElement)
					return ((MetadataRepositoryElement) element).isEnabled() ? ProvUIMessages.RepositoryDetailsLabelProvider_Enabled : ProvUIMessages.RepositoryDetailsLabelProvider_Disabled;

		}
		return null;
	}
}
