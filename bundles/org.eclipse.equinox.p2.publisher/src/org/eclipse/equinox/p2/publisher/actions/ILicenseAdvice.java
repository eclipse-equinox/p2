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

import org.eclipse.equinox.p2.publisher.IPublisherAdvice;

/**
 * Provides advice about a license.
 */
public interface ILicenseAdvice extends IPublisherAdvice {

	public String getLicenseURL();

	public String getLicenseText();

}