/*******************************************************************************
 * Copyright (c) 2009, 2024 EclipseSource and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: 
 *   EclipseSource - initial API and implementation
 *   SAP SE - support macOS bundle URL types
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import java.util.List;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;

/**
 * Advice for branding executables and other element while publishing.
 */
public interface IBrandingAdvice extends IPublisherAdvice {

	/**
	 * Returns the OS that this branding advice is relevant for.
	 */
	public String getOS();

	/**
	 * Returns the list of icon files to be used in branding an executable. 
	 * The nature of the returned values and the images they represent is
	 * platform-specific.
	 * 
	 * @return the list of icons used in branding an executable or <code>null</code> if none.
	 */
	public String[] getIcons();

	/**
	 * Returns the name of the launcher.  This should be the OS-independent
	 * name. That is, ".exe" etc. should not be included.
	 * 
	 * @return the name of the branded launcher or <code>null</code> if none.
	 */
	public String getExecutableName();

	/**
	 * Returns the list of URL schemes / names to be handled by the macOS app
	 * bundle.
	 * <p>
	 * They will be stored in the Information Property List file
	 * (<code>Info.plist</code>) of the app bundle.
	 *
	 * @return the the list of URL schemes / names to be handled by the macOS app
	 *         bundle
	 */
	public List<IMacOsBundleUrlType> getMacOsBundleUrlTypes();
}
