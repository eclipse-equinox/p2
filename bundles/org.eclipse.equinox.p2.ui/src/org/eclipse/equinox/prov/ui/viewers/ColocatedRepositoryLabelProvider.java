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

package org.eclipse.equinox.prov.ui.viewers;

import java.net.URL;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.equinox.prov.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.prov.ui.ColocatedRepositoryUtil;
import org.eclipse.equinox.prov.ui.ProvUIImages;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * Label provider for colocated repositories.
 * 
 * @since 3.4
 */
public class ColocatedRepositoryLabelProvider extends LabelProvider implements ITableLabelProvider {
	final static int COLUMN_NAME = 0;
	final static int COLUMN_LOCATION = 1;
	final static String BLANK = ""; //$NON-NLS-1$

	public String getColumnText(Object element, int columnIndex) {
		IMetadataRepository repo = getRepository(element);
		if (repo == null)
			return BLANK;

		switch (columnIndex) {
			case COLUMN_NAME :
				return repo.getName();
			case COLUMN_LOCATION :
				URL url = ColocatedRepositoryUtil.makeColocatedRepositoryURL(repo.getLocation());
				return url.toExternalForm();
		}
		return BLANK;
	}

	public Image getColumnImage(Object element, int index) {
		if (index == COLUMN_NAME && getRepository(element) != null) {
			return ProvUIImages.getImage(ProvUIImages.IMG_METADATA_REPOSITORY);
		}
		return null;
	}

	private IMetadataRepository getRepository(Object element) {
		if (element instanceof IMetadataRepository)
			return (IMetadataRepository) element;
		if (element instanceof IAdaptable)
			return (IMetadataRepository) ((IAdaptable) element).getAdapter(IMetadataRepository.class);
		return null;
	}
}