/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.query.IUViewQueryContext;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.osgi.util.NLS;

/**
 * A wrapper that assigns a query provider and the queryable
 * who was performing the query to the wrapped elements
 * as they are accepted.
 * 
 * @since 3.4
 */
public abstract class QueriedElementWrapper extends ElementWrapper {

	protected IQueryable<?> queryable;
	protected Object parent;
	protected String emptyExplanationString;
	protected int emptyExplanationSeverity;
	protected String emptyExplanationDescription;

	public QueriedElementWrapper(IQueryable<?> queryable, Object parent) {
		this.queryable = queryable;
		this.parent = parent;
	}

	/**
	 * Sets an item as Queryable if it is a QueriedElement
	 */
	protected Object wrap(Object item) {
		if (item instanceof QueriedElement) {
			QueriedElement element = (QueriedElement) item;
			if (!element.knowsQueryable()) {
				element.setQueryable(queryable);
			}
		}
		return item;
	}

	public Collection<?> getElements(Collector<?> collector) {
		// Any previously stored explanations are not valid.
		emptyExplanationString = null;
		emptyExplanationSeverity = IStatus.INFO;
		emptyExplanationDescription = null;
		if (collector.isEmpty()) {
			// Before we are even filtering out items, there is nothing in the collection.
			// All we can do is look for the most common reasons and guess.  If the collection
			// is empty and the parent is an IU, then being empty is not a big deal, it means
			// we are in drilldown.
			if (parent instanceof MetadataRepositoryElement) {
				MetadataRepositoryElement repo = (MetadataRepositoryElement) parent;
				RepositoryTracker manipulator = repo.getProvisioningUI().getRepositoryTracker();
				if (manipulator.hasNotFoundStatusBeenReported(repo.getLocation())) {
					return emptyExplanation(IStatus.ERROR, NLS.bind(ProvUIMessages.QueriedElementWrapper_SiteNotFound, URIUtil.toUnencodedString(repo.getLocation())), ""); //$NON-NLS-1$
				}
			}
			if (parent instanceof QueriedElement) {
				QueriedElement element = (QueriedElement) parent;
				IUViewQueryContext context = element.getQueryContext();
				if (context == null)
					context = ProvUI.getQueryContext(element.getPolicy());
				if (parent instanceof MetadataRepositoryElement || parent instanceof MetadataRepositories) {
					if (context != null && context.getViewType() == IUViewQueryContext.AVAILABLE_VIEW_BY_CATEGORY && context.getUseCategories()) {
						return emptyExplanation(IStatus.INFO, ProvUIMessages.QueriedElementWrapper_NoCategorizedItemsExplanation, context.getUsingCategoriesDescription());
					}
					return emptyExplanation(IStatus.INFO, ProvUIMessages.QueriedElementWrapper_NoItemsExplanation, null);
				}
			}
			// It is empty, but the parent is an IU, so this could be a drilldown.
			return Collections.EMPTY_LIST;
		}
		Collection<?> elements = super.getElements(collector);
		// We had elements but now they have been filtered out.  Hopefully
		// we can explain this.
		if (elements.isEmpty()) {
			if (emptyExplanationString != null)
				return emptyExplanation(emptyExplanationSeverity, emptyExplanationString, emptyExplanationDescription);
			// We filtered out content but never explained it.  Ideally this doesn't happen if
			// all wrappers explain any filtering.
			return emptyExplanation(emptyExplanationSeverity, ProvUIMessages.QueriedElementWrapper_NoItemsExplanation, null);
		}
		return elements;
	}

	Collection<?> emptyExplanation(int severity, String explanationString, String explanationDescription) {
		ArrayList<Object> collection = new ArrayList<Object>(1);
		collection.add(new EmptyElementExplanation(parent, severity, explanationString, explanationDescription));
		return collection;
	}
}
