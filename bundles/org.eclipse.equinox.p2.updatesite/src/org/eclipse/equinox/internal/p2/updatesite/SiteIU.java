/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite;

import java.util.ArrayList;
import java.util.List;

public class SiteIU {

	public static final String QUERY_TYPE_CONTEXT = "context"; //$NON-NLS-1$
	public static final String QUERY_TYPE_MATCH = "match"; //$NON-NLS-1$

	private String id = null;
	private String range = null;
	private String queryExpression = null;
	private String queryType = null;
	private List<String> queryParams = null;
	private List<String> categoryNames = null;

	/**
	 * Returns the id of the IU
	 * @return the id of the IU
	 */
	public String getID() {
		return id;
	}

	/**
	 * Returns the range of the IU
	 * @return the range of the IU
	 */
	public String getRange() {
		return range;
	}

	/**
	 * Returns the query expression for the IU.
	 * 
	 * @return query expression
	 */
	public String getQueryExpression() {
		return queryExpression;
	}

	/**
	 * Returns the query type for the IU.
	 * 
	 * @return the query type
	 */
	public String getQueryType() {
		return queryType;
	}

	/**
	 * Returns the params for the query expression for the IU
	 * 
	 * @return an array of query params.
	 */
	public String[] getQueryParams() {
		if (queryParams == null)
			return new String[0];

		return queryParams.toArray(new String[0]);
	}

	/**
	 * Returns the names of categories the referenced IU belongs to.
	 * 
	 * @return an array of names, or an empty array.
	 */
	public String[] getCategoryNames() {
		if (categoryNames == null)
			return new String[0];

		return categoryNames.toArray(new String[0]);
	}

	/**
	 * Sets the id for the IU.
	 * @param id the id
	 */
	public void setID(String id) {
		this.id = id;
	}

	/**
	 * Sets the range for the IU.
	 * @param range the range
	 */
	public void setRange(String range) {
		this.range = range;
	}

	/**
	 * Sets the query expression for the IU.
	 * 
	 * @param queryExpression query expression
	 */
	public void setQueryExpression(String queryExpression) {
		this.queryExpression = queryExpression;
	}

	/**
	 * Sets the query type for the IU.
	 * 
	 * @param queryType the query type
	 */
	public void setQueryType(String queryType) {
		this.queryType = queryType;
	}

	/**
	 * Adds the name of a category this IU belongs to.
	 * 
	 * @param categoryName category name
	 */
	public void addCategoryName(String categoryName) {
		if (this.categoryNames == null)
			this.categoryNames = new ArrayList<String>();
		if (!this.categoryNames.contains(categoryName))
			this.categoryNames.add(categoryName);
	}

	/**
	 * Adds a param for the query expression for this IU.
	 * 
	 * @param queryParam a query param.
	 */
	public void addQueryParams(String queryParam) {
		if (this.queryParams == null)
			this.queryParams = new ArrayList<String>();
		// don't do contains check, order matters and there may be duplicates
		this.queryParams.add(queryParam);
	}
}
