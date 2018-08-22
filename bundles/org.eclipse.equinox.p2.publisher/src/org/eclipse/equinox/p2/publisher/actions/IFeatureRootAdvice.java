/*******************************************************************************
 *  Copyright (c) 2009, 2010 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import org.eclipse.equinox.internal.p2.core.helpers.FileUtils.IPathComputer;
import org.eclipse.equinox.internal.p2.publisher.FileSetDescriptor;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;

public interface IFeatureRootAdvice extends IPublisherAdvice {

	/**
	 * Return an array of configSpecs for which this advice is applicable.
	 * @see IPublisherAdvice#isApplicable(String, boolean, String, org.eclipse.equinox.p2.metadata.Version)
	 * @return String [] : Array of configSpec ("ws,os,arch")
	 */
	public String[] getConfigurations();

	public IPathComputer getRootFileComputer(String configSpec);

	public FileSetDescriptor getDescriptor(String configSpec);
}
