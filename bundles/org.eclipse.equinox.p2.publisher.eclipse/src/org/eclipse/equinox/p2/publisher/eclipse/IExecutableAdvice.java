/*******************************************************************************
 * Copyright (c) 2008, 2011 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   EclipseSource - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import org.eclipse.equinox.p2.publisher.IPublisherAdvice;

/**
 * Advice for executables while publishing.
 */
public interface IExecutableAdvice extends IPublisherAdvice {

	/**
	 * Returns the VM arguments for this executable.
	 * @return The list of VM Arguments for this executable or empty array for none
	 */
	public String[] getVMArguments();

	/**
	 * Returns the program arguments for this executable.
	 * 
	 * @return The list of program arguments for tihs executable or empty array for none
	 */
	public String[] getProgramArguments();

	/**
	 * Returns the name of the launcher.  This should be the OS-independent
	 * name. That is, ".exe" etc. should not be included.
	 * 
	 * @return the name of the branded launcher or <code>null</code> if none.
	 */
	public String getExecutableName();
}
