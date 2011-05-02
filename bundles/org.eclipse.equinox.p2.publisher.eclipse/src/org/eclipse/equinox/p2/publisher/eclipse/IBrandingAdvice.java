/*******************************************************************************
 * Copyright (c) 2009, 2011 EclipseSource and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

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
}
