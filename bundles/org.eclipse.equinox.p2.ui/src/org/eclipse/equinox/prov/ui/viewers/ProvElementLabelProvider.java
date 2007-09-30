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

import org.eclipse.equinox.prov.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.prov.core.repository.IRepositoryInfo;
import org.eclipse.equinox.prov.engine.Profile;
import org.eclipse.equinox.prov.metadata.*;
import org.eclipse.equinox.prov.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.prov.ui.ProvUIImages;
import org.eclipse.equinox.prov.ui.model.InstalledIUElement;
import org.eclipse.equinox.prov.ui.model.ProvElement;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Label provider for provisioning elements. Some provisioning objects are
 * wrapped by ProvElements and some are not.  This is the most generic of the
 * provisioning label providers.  A two-column format for elements is 
 * supported, with the content of the columns dependent on the type of object.
 * 
 * @since 3.4
 */
public class ProvElementLabelProvider extends LabelProvider implements ITableLabelProvider {

	public String getText(Object obj) {
		if (obj instanceof ProvElement) {
			return ((ProvElement) obj).getLabel(obj);
		}
		if (obj instanceof Profile) {
			return ((Profile) obj).getProfileId();
		}
		if (obj instanceof IInstallableUnit) {
			IInstallableUnit iu = (IInstallableUnit) obj;
			return iu.getId();
		}
		if (obj instanceof IRepositoryInfo) {
			String name = ((IRepositoryInfo) obj).getName();
			if (name != null && name.length() > 0) {
				return name;
			}
			return ((IRepositoryInfo) obj).getLocation().toExternalForm();
		}
		if (obj instanceof IArtifactKey) {
			IArtifactKey key = (IArtifactKey) obj;
			return key.getId() + " [" + key.getNamespace() + "]"; //$NON-NLS-1$//$NON-NLS-2$
		}

		return obj.toString();
	}

	public Image getImage(Object obj) {
		if (obj instanceof ProvElement) {
			return ((ProvElement) obj).getImage(obj);
		}
		if (obj instanceof Profile) {
			return ProvUIImages.getImage(ProvUIImages.IMG_PROFILE);
		}
		if (obj instanceof InstallableUnit) {
			return ProvUIImages.getImage(ProvUIImages.IMG_IU);
		}
		if (obj instanceof IArtifactRepository) {
			return ProvUIImages.getImage(ProvUIImages.IMG_ARTIFACT_REPOSITORY);
		}
		if (obj instanceof IMetadataRepository) {
			return ProvUIImages.getImage(ProvUIImages.IMG_METADATA_REPOSITORY);
		}
		if (obj instanceof IArtifactKey) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
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
			case 0 :
				return getText(element);
			case 1 :
				if (element instanceof Profile) {
					return ((Profile) element).getValue(Profile.PROP_NAME);
				}
				if (element instanceof IInstallableUnit) {
					IInstallableUnit iu = (IInstallableUnit) element;
					return iu.getVersion().toString();
				}
				if (element instanceof IRepositoryInfo) {
					String name = ((IRepositoryInfo) element).getName();
					if (name != null && name.length() > 0) {
						return ((IRepositoryInfo) element).getLocation().toExternalForm();
					}
					return ((IRepositoryInfo) element).getType();
				}
				if (element instanceof IArtifactKey) {
					IArtifactKey key = (IArtifactKey) element;
					return key.getVersion().toString();
				}
				if (element instanceof InstalledIUElement) {
					IInstallableUnit iu = (IInstallableUnit) ((InstalledIUElement) element).getAdapter(IInstallableUnit.class);
					if (iu != null)
						return iu.getVersion().toString();
				}
		}
		return null;
	}
}