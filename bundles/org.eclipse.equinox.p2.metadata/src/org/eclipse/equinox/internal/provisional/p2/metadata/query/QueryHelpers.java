/******************************************************************************* 
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.metadata.query;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.eclipse.equinox.p2.metadata.query.IQuery;

/**
 * Static helper methods for the Query API.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class QueryHelpers {
	/**
	 * Gets the ID for a Query. 
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public static String getId(IQuery<?> query) {
		return query.getClass().getName();
	}

	/**
	 * Gets a particular property of a query.
	 * @param query The query to retrieve the property from
	 * @param property The property to retrieve 
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public static Object getProperty(IQuery<?> query, String property) {
		Class<?> clazz = query.getClass();
		Object result = null;
		try {
			Method method = clazz.getMethod("get" + property, new Class[0]); //$NON-NLS-1$
			result = method.invoke(query, new Object[0]);
		} catch (SecurityException e) {
			return null;
		} catch (NoSuchMethodException e) {
			return null;
		} catch (IllegalArgumentException e) {
			return null;
		} catch (IllegalAccessException e) {
			return null;
		} catch (InvocationTargetException e) {
			return null;
		}
		return result;
	}
}
