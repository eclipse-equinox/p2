/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.query;

import java.util.*;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.p2.ui.model.CategoryElement;
import org.eclipse.equinox.internal.p2.ui.model.QueriedElementWrapper;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.IQueryable;

/**
 * A collector that converts IU's to category elements as it accepts them.
 * It can be configured so that it is never empty.
 * 
 * @since 3.4
 */
public class CategoryElementWrapper extends QueriedElementWrapper {

	// Used to track nested categories
	private Set<String> referredIUs = new HashSet<String>();

	public CategoryElementWrapper(IQueryable<?> queryable, Object parent) {
		super(queryable, parent);
	}

	protected boolean shouldWrap(Object match) {
		if (match instanceof IInstallableUnit) {
			IInstallableUnit iu = (IInstallableUnit) match;
			Collection<IRequirement> requirements = iu.getRequirements();
			for (IRequirement requirement : requirements) {
				if (requirement instanceof IRequiredCapability) {
					if (((IRequiredCapability) requirement).getNamespace().equals(IInstallableUnit.NAMESPACE_IU_ID)) {
						referredIUs.add(((IRequiredCapability) requirement).getName());
					}
				}
			}

			Iterator<?> iter = super.getCollection().iterator();
			// Don't add the same category IU twice.  
			while (iter.hasNext()) {
				CategoryElement element = (CategoryElement) iter.next();
				if (element.shouldMerge(iu)) {
					element.mergeIU(iu);
					return false;
				}
			}
			return true;
		}

		return false;
	}

	public Collection<?> getElements(Collector<?> collector) {
		if (collector.isEmpty())
			return super.getElements(collector);
		Collection<?> results = super.getElements(collector);
		cleanList();
		return results;
	}

	protected Object wrap(Object item) {
		IInstallableUnit iu = (IInstallableUnit) item;
		return super.wrap(new CategoryElement(parent, iu));
	}

	private void cleanList() {
		removeNestedCategories();
	}

	private void removeNestedCategories() {
		CategoryElement[] categoryIUs = getCollection().toArray(new CategoryElement[getCollection().size()]);
		// If any other element refers to a category element, remove it from the list
		for (int i = 0; i < categoryIUs.length; i++) {
			if (referredIUs.contains(categoryIUs[i].getIU().getId())) {
				getCollection().remove(categoryIUs[i]);
			}
		}
	}
}
