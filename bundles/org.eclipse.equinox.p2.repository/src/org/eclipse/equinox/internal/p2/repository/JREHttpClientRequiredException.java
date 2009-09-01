/*******************************************************************************
 * Copyright (c) 2009, Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Cloudsmith - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.repository;

import java.net.ProtocolException;

/**
 * Exception signaling that the JRE Http Client is required to handle the request.
 */
public class JREHttpClientRequiredException extends ProtocolException {

	private static final long serialVersionUID = 1550518207489119010L;

}
