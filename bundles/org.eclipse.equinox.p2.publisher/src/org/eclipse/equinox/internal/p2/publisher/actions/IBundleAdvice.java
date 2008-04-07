/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.actions;

import java.io.File;
import java.util.Properties;
import org.eclipse.equinox.internal.p2.publisher.IPublishingAdvice;

public interface IBundleAdvice extends IPublishingAdvice {

	/**
	 * Returns the set of extra properties to be associated with the IU for the bundle
	 * at the given location
	 * @param location the location of the feature to advise
	 * @return extra properties for the given feature
	 */
	public Properties getProperties(File location);
}
