/*******************************************************************************
 * Copyright (c) 2009, 2010 EclipseSource and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;

/**
 * Filter advice helps actions figure out where an IU with a given id and version
 * is applicable.  For example, when some IU being published depends on another, 
 * it is possible that the prerequisite is not applicable in all scenarios.  In that case
 * it is useful for new IU to spec its requirement using an applicability filter.  This
 * advice can supply that filter.
 */
public interface IFilterAdvice extends IPublisherAdvice {
	/**
	 * Returns the filter to use for depending on the IU with the given id.
	 * If an exact match is desired, only IUs with the given version are considered
	 * as sources of filter information.  If in-exact matches are acceptable, the
	 * advisor will attempt to find the most relevant IU (typically the one with the highest
	 * version less than that supplied) to supply the version information.
	 * 
	 * @param id the id of the target IU 
	 * @param version the version of the target IU 
	 * @param exact whether or not to consider information for IUs whose 
	 * version does not match the supplied version
	 * @return the filter to use when depending on the given IU or <code>null</code>
	 * if none.
	 */
	public IMatchExpression<IInstallableUnit> getFilter(String id, Version version, boolean exact);

}
