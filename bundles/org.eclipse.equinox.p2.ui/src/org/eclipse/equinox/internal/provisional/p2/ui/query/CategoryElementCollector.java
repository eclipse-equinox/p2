/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui.query;

import java.util.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;
import org.eclipse.equinox.internal.provisional.p2.ui.model.*;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;
import org.osgi.framework.Version;

/**
 * A collector that converts IU's to category elements as it accepts them.
 * It can be configured so that it is never empty.
 * 
 * @since 3.4
 */
public class CategoryElementCollector extends QueriedElementCollector {

	private boolean groupUncategorized;
	private Set referredIUs = new HashSet();

	public CategoryElementCollector(IQueryProvider queryProvider, IQueryable queryable, QueryContext queryContext, boolean showUncategorized) {
		super(queryProvider, queryable, queryContext);
		this.groupUncategorized = showUncategorized;
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
		IInstallableUnit iu = (IInstallableUnit) match;
		RequiredCapability[] requirements = iu.getRequiredCapabilities();
		for (int i = 0; i < requirements.length; i++) {
			if (requirements[i].getNamespace().equals(IInstallableUnit.NAMESPACE_IU_ID)) {
				referredIUs.add(requirements[i].getName());
			}
		}
		Iterator iter = super.iterator();
		// Don't add the same category IU twice
		while (iter.hasNext()) {
			CategoryElement element = (CategoryElement) iter.next();
			if (element.getIU().getId().equals(iu.getId())) {
				element.mergeIU(iu);
				return true;
			}
		}
		return super.accept(new CategoryElement(iu));
	}

	private void cleanList() {
		if (groupUncategorized)
			createDummyCategory();
		removeNestedCategories();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.query.Collector#iterator()
	 */
	public Iterator iterator() {
		cleanList();
		return super.iterator();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.query.Collector#toArray(java.lang.Class)
	 */
	public Object[] toArray(Class clazz) {
		cleanList();
		return super.toArray(clazz);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.query.Collector#toCollection()
	 */
	public Collection toCollection() {
		cleanList();
		return super.toCollection();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.query.Collector#size()
	 */
	public int size() {
		cleanList();
		return super.size();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.query.Collector#isEmpty()
	 */
	public boolean isEmpty() {
		cleanList();
		return super.isEmpty();
	}

	private void createDummyCategory() {
		InstallableUnitDescription unit = new InstallableUnitDescription();
		unit.setId(ProvUIMessages.CategoryElementCollector_Uncategorized);
		unit.setProperty(IInstallableUnit.PROP_TYPE_CATEGORY, Boolean.toString(true));
		unit.setVersion(new Version(0, 0, 0, "generated")); //$NON-NLS-1$
		unit.setProperty(IInstallableUnit.PROP_NAME, ProvUIMessages.CategoryElementCollector_Uncategorized);

		IInstallableUnit iu = MetadataFactory.createInstallableUnit(unit);
		CategoryElement element = new UncategorizedCategoryElement(iu);
		element.setQueryable(queryable);
		element.setQueryProvider(queryProvider);
		element.setQueryContext(queryContext);
		// This is costly, but the only way to know if we need this category is to perform the query in advance.
		// Note that this will end up querying the contents of all categories to determine which IU's were not
		// referred to.
		ElementQueryDescriptor queryDescriptor = queryProvider.getQueryDescriptor(element, element.getQueryType());
		Collector collector = queryDescriptor.queryable.query(queryDescriptor.query, queryDescriptor.collector, null);
		if (!collector.isEmpty())
			getList().add(element);
	}

	private void removeNestedCategories() {
		CategoryElement[] categoryIUs = (CategoryElement[]) getList().toArray(new CategoryElement[getList().size()]);
		// If any other element refers to a category element, remove it from the list
		for (int i = 0; i < categoryIUs.length; i++) {
			if (referredIUs.contains(categoryIUs[i].getIU().getId())) {
				getList().remove(categoryIUs[i]);
			}
		}
	}
}
