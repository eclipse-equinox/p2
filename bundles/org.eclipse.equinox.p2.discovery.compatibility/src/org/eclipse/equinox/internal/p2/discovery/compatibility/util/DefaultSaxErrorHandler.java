/*******************************************************************************
 * Copyright (c) 2009, 2018 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.discovery.compatibility.util;

import org.xml.sax.*;

/**
 * A default implementation of an error handler that throws exceptions on all errors.
 * 
 * @author David Green
 */
public class DefaultSaxErrorHandler implements ErrorHandler {

	@Override
	public void warning(SAXParseException exception) {
		// ignore
	}

	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		throw exception;
	}

	@Override
	public void error(SAXParseException exception) throws SAXException {
		throw exception;
	}

}