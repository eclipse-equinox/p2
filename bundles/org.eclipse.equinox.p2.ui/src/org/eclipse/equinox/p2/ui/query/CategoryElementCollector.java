/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ui.query;

import java.util.Iterator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.ui.model.CategoryElement;
import org.eclipse.equinox.p2.ui.model.UncategorizedCategoryElement;
import org.osgi.framework.Version;

/**
 * A collector that converts IU's to category elements as it accepts them.
 * It can be configured so that it is never empty.
 * 
 * @since 3.4
 */
public class CategoryElementCollector extends QueriedElementCollector {

	private boolean allowEmpty;

	public CategoryElementCollector(IProvElementQueryProvider queryProvider, IQueryable queryable, boolean allowEmpty) {
		super(queryProvider, queryable);
		this.allowEmpty = allowEmpty;
	}

	/**
	 * Accepts a result that matches the query criteria.
	 * 
	 * @param match an object matching the query
	 * @return <code>true</code> if the query should continue,
	 * or <code>false</code> to indicate the query should stop.
	 */
	public boolean accept(Object match) {
		if (!(match instanceof IInstallableUnit))
			return true;
		return super.accept(new CategoryElement((IInstallableUnit) match));
	}

	public Iterator iterator() {
		if (!allowEmpty)
			createDummyCategory();
		return super.iterator();
	}

	public Object[] toArray(Class clazz) {
		if (!allowEmpty)
			createDummyCategory();
		return super.toArray(clazz);
	}

	private void createDummyCategory() {
		InstallableUnitDescription unit = new InstallableUnitDescription();
		unit.setId(ProvUIMessages.CategoryElementCollector_Uncategorized);
		unit.setProperty(IInstallableUnit.PROP_CATEGORY_IU, Boolean.toString(true));
		unit.setVersion(Version.emptyVersion);
		IInstallableUnit iu = MetadataFactory.createInstallableUnit(unit);
		CategoryElement element = new UncategorizedCategoryElement(iu);
		element.setQueryable(queryable);
		element.setQueryProvider(queryProvider);
		getList().add(element);
	}
}
