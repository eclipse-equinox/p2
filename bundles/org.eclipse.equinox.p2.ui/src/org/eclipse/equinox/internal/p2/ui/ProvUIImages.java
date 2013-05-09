/*******************************************************************************
 *  Copyright (c) 2007, 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat, Inc. - support for remediation page
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

/**
 * ProvUIImages provides convenience methods for accessing shared images
 * provided by the <i>org.eclipse.equinox.internal.provisional.p2.ui</i> plug-in.
 * <p>
 * This class provides <code>ImageDescriptor</code>s for each named image in
 * {@link ProvUIImages}. All <code>Image</code> objects created from the
 * provided descriptors are managed the caller and must be disposed
 * appropriately.
 * </p>
 * <p>
 * This class is not intended to be subclassed or instantiated by clients
 * 
 * @since 3.4
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ProvUIImages {

	// bundle-relative icon path
	public final static String ICON_PATH = "$nl$/icons/"; //$NON-NLS-1$
	//objects
	public final static String IMG_ARTIFACT_REPOSITORY = "obj/artifact_repo_obj.gif"; //$NON-NLS-1$
	public final static String IMG_METADATA_REPOSITORY = "obj/metadata_repo_obj.gif"; //$NON-NLS-1$
	public final static String IMG_IU = "obj/iu_obj.gif"; //$NON-NLS-1$
	public final static String IMG_DISABLED_IU = "obj/iu_disabled_obj.gif"; //$NON-NLS-1$
	public final static String IMG_ADDED = "obj/iu_add.gif"; //$NON-NLS-1$
	public final static String IMG_REMOVED = "obj/iu_remove.gif"; //$NON-NLS-1$
	public final static String IMG_CHANGED = "obj/iu_update_obj.gif"; //$NON-NLS-1$
	public final static String IMG_NOTADDED = "obj/iu_notadd.gif"; //$NON-NLS-1$

	public final static String IMG_UPDATED_IU = "obj/iu_update_obj.gif"; //$NON-NLS-1$
	public final static String IMG_UPGRADED_IU = "obj/iu_upgraded.png"; //$NON-NLS-1$
	public final static String IMG_DOWNGRADED_IU = "obj/iu_downgraded.png"; //$NON-NLS-1$
	public final static String IMG_ADDED_OVERLAY = "obj/added_overlay.gif"; //$NON-NLS-1$
	public final static String IMG_REMOVED_OVERLAY = "obj/removed_overlay.gif"; //$NON-NLS-1$
	public final static String IMG_PATCH_IU = "obj/iu_patch_obj.gif"; //$NON-NLS-1$
	public final static String IMG_DISABLED_PATCH_IU = "obj/iu_disabled_patch_obj.gif"; //$NON-NLS-1$
	public final static String IMG_PROFILE = "obj/profile_obj.gif"; //$NON-NLS-1$
	public final static String IMG_CATEGORY = "obj/category_obj.gif"; //$NON-NLS-1$
	public final static String IMG_INFO = "obj/iu_info.gif"; //$NON-NLS-1$

	// wizard graphics
	public final static String WIZARD_BANNER_INSTALL = "wizban/install_wiz.gif"; //$NON-NLS-1$
	public final static String WIZARD_BANNER_UNINSTALL = "wizban/uninstall_wiz.gif"; //$NON-NLS-1$
	public final static String WIZARD_BANNER_UPDATE = "wizban/update_wiz.gif"; //$NON-NLS-1$
	public final static String WIZARD_BANNER_REVERT = "wizban/revert_wiz.gif"; //$NON-NLS-1$

	/**
	 * Returns the image descriptor for the given image ID. Returns
	 * <code>null</code> if there is no such image.
	 * 
	 * @param id
	 *            the identifier for the image to retrieve
	 * @return the image descriptor associated with the given ID
	 */
	public static ImageDescriptor getImageDescriptor(String id) {
		return ProvUIActivator.getDefault().getImageRegistry().getDescriptor(id);
	}

	/**
	 * Returns the image for the given image ID. Returns <code>null</code> if
	 * there is no such image.
	 * 
	 * @param id
	 *            the identifier for the image to retrieve
	 * @return the image associated with the given ID. This image is managed in
	 *         an image registry and should not be freed by the client.
	 */
	public static Image getImage(String id) {
		return ProvUIActivator.getDefault().getImageRegistry().get(id);
	}
}
