/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.ui.viewers;

import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.ui.model.IIUElement;
import org.eclipse.equinox.internal.p2.ui.model.ProvElement;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepDescriptor;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IRepositoryElement;
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
		if (obj instanceof IProfile) {
			return ((IProfile) obj).getProfileId();
		}
		if (obj instanceof IInstallableUnit) {
			IInstallableUnit iu = (IInstallableUnit) obj;
			return iu.getId();
		}
		if (obj instanceof IRepository) {
			String name = ((IRepository) obj).getName();
			if (name != null && name.length() > 0) {
				return name;
			}
			return URIUtil.toUnencodedString(((IRepository) obj).getLocation());
		}
		if (obj instanceof IRepositoryElement) {
			String name = ((IRepositoryElement) obj).getName();
			if (name != null && name.length() > 0) {
				return name;
			}
			return URIUtil.toUnencodedString(((IRepositoryElement) obj).getLocation());
		}
		if (obj instanceof IArtifactKey) {
			IArtifactKey key = (IArtifactKey) obj;
			return key.getId() + " [" + key.getClassifier() + "]"; //$NON-NLS-1$//$NON-NLS-2$
		}
		if (obj instanceof ProcessingStepDescriptor) {
			ProcessingStepDescriptor descriptor = (ProcessingStepDescriptor) obj;
			return descriptor.getProcessorId();
		}
		if (obj instanceof IRequiredCapability) {
			return ((IRequiredCapability) obj).getName();
		}
		return obj.toString();
	}

	public Image getImage(Object obj) {
		if (obj instanceof ProvElement) {
			return ((ProvElement) obj).getImage(obj);
		}
		if (obj instanceof IProfile) {
			return ProvUIImages.getImage(ProvUIImages.IMG_PROFILE);
		}
		if (obj instanceof IInstallableUnit) {
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
		if (obj instanceof IRequiredCapability) {
			return ProvUIImages.getImage(ProvUIImages.IMG_IU);
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
				if (element instanceof IProfile) {
					return ((IProfile) element).getProperty(IProfile.PROP_NAME);
				}
				if (element instanceof IIUElement) {
					if (((IIUElement) element).shouldShowVersion())
						return ((IIUElement) element).getIU().getVersion().toString();
				}
				IInstallableUnit iu = (IInstallableUnit) ProvUI.getAdapter(element, IInstallableUnit.class);
				if (iu != null) {
					return iu.getVersion().toString();
				}
				if (element instanceof IRepository) {
					return URIUtil.toUnencodedString(((IRepository) element).getLocation());
				}
				if (element instanceof IRepositoryElement) {
					return URIUtil.toUnencodedString(((IRepositoryElement) element).getLocation());
				}
				if (element instanceof IArtifactKey) {
					IArtifactKey key = (IArtifactKey) element;
					return key.getVersion().toString();
				}
				if (element instanceof IRequiredCapability) {
					return ((IRequiredCapability) element).getRange().getMaximum().toString();
				}

		}
		return null;
	}
}
