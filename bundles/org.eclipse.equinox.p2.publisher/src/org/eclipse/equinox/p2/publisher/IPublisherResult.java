/*******************************************************************************
 * Copyright (c) 2008, 2010 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher;

import java.util.Collection;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQueryable;

/**
 * Publisher results represent the result of running a publishing operation.  A result is a 
 * collection of IInstallableUnits that were created or read duing the operation.  The result
 * IUs are categorized either as "roots" or "non-roots".  The meaning of these types is 
 * determined by the actions involved in the operation and it is up to the consumer of the
 * result to interpret the collections.
 */
public interface IPublisherResult extends IQueryable<IInstallableUnit> {
	/**
	 * Merge mode setting that causes all root results to be merged into 
	 * the root of the merged results and all non-roots to become non-roots.
	 */
	public static final int MERGE_MATCHING = 0;

	/** 
	 * Merge mode setting that causes all root and non-root result entries to 
	 * be placed in the root collection of the result.
	 */
	public static final int MERGE_ALL_ROOT = 1;

	/** 
	 * Merge mode setting that causes all root and non-root result entries to 
	 * be placed in the non-root collection of the result.
	 */
	public static final int MERGE_ALL_NON_ROOT = 2;

	/**
	 * An opaque token used to identify the root elements of a result.
	 */
	public static final String ROOT = "root"; //$NON-NLS-1$

	/**
	 * An opaque token used to identify the non-root elements of a result.
	 */
	public static final String NON_ROOT = "non_root"; //$NON-NLS-1$

	/**
	 * Add the given IU to this result and identify it as the given type
	 * @see #ROOT
	 * @see #NON_ROOT
	 * 
	 * @param iu the IU to add
	 * @param type the type of the IU in this result
	 */
	public void addIU(IInstallableUnit iu, String type);

	/**
	 * Add all of the given IUs to this result and identify it as the given type
	 * @see #ROOT
	 * @see #NON_ROOT
	 * 
	 * @param ius the IUs to add
	 * @param type the type of the IUs in this result
	 */
	public void addIUs(Collection<IInstallableUnit> ius, String type);

	/**
	 * Returns the IUs of the given type with the given id in this result.
	 * @param id the id of the IUs to look for
	 * @param type the type of IUs to look for in this result
	 * @return the requested IUs
	 * @see #ROOT
	 * @see #NON_ROOT
	 */
	public Collection<IInstallableUnit> getIUs(String id, String type);

	/**
	 * Returns the first available IU of the given type with the given id in this result.
	 * @param id the id of the IU to look for
	 * @param type the type of IU to look for in this result
	 * @return the requested IU
	 * @see #ROOT
	 * @see #NON_ROOT
	 * @deprecated This method should be removed as it essentially returns a random IU
	 * with the given ID.  There are some uses for this but not many and they can use #getIUs
	 */
	public IInstallableUnit getIU(String id, String type);

	/**
	 * Returns the IU of the given type with the given id and version in this result.
	 * @param id the id of the IU to look for
	 * @param version the version of the IU to look for
	 * @param type the type of IU to look for in this result
	 * @return the requested IU
	 * @see #ROOT
	 * @see #NON_ROOT
	 */
	public IInstallableUnit getIU(String id, Version version, String type);

	/**
	 * Merges the given result in this result according to the given mode. 
	 * @see #MERGE_ALL_NON_ROOT
	 * @see #MERGE_ALL_ROOT
	 * @see #MERGE_MATCHING
	 * @param result the result to merge into this result
	 * @param mode the merge mode to use
	 */
	public void merge(IPublisherResult result, int mode);
}
