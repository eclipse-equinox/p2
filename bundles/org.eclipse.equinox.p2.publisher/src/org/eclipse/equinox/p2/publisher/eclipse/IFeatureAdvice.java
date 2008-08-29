/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import java.util.Properties;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;

public interface IFeatureAdvice extends IPublisherAdvice {

	/**
	 * Returns the set of extra properties to be associated with the IU for the feature
	 * at the given location
	 * @return extra properties for the given feature
	 */
	public Properties getIUProperties(Feature feature);

	/**
	 * Returns the set of extra properties to be associated with the artifact descriptor
	 * being published for the feature at the given location
	 * @param location the location of the feature to advise
	 * @return extra properties for the given feature
	 */
	public Properties getArtifactProperties(Feature feature);
}
