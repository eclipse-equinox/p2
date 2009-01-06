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
package org.eclipse.equinox.internal.p2.ui.query;

import java.util.*;
import org.eclipse.equinox.internal.p2.ui.model.CategoryElement;
import org.eclipse.equinox.internal.p2.ui.model.QueriedElementCollector;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;

/**
 * A collector that converts IU's to category elements as it accepts them.
 * It can be configured so that it is never empty.
 * 
 * @since 3.4
 */
public class CategoryElementCollector extends QueriedElementCollector {

	// Used to track nested categories
	private Set referredIUs = new HashSet();

	public CategoryElementCollector(IQueryable queryable, Object parent) {
		super(queryable, parent);
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
		IRequiredCapability[] requirements = iu.getRequiredCapabilities();
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
		return super.accept(new CategoryElement(parent, iu));
	}

	private void cleanList() {
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

	private void removeNestedCategories() {
		CategoryElement[] categoryIUs = (CategoryElement[]) getCollection().toArray(new CategoryElement[getCollection().size()]);
		// If any other element refers to a category element, remove it from the list
		for (int i = 0; i < categoryIUs.length; i++) {
			if (referredIUs.contains(categoryIUs[i].getIU().getId())) {
				getCollection().remove(categoryIUs[i]);
			}
		}
	}
}
