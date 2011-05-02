/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;

/**
 * A matcher that matches advice applicable to a given id and version.
 */
public class AdviceMatcher implements IArgumentMatcher {
	private final Version version;
	private final String id;
	private static Class clazz;

	public static IPublisherAdvice adviceMatches(String id, Version version, Class clazz) {
		AdviceMatcher.clazz = clazz;
		EasyMock.reportMatcher(new AdviceMatcher(id, version));
		return null;
	}

	public AdviceMatcher(String id, Version version) {
		this.id = id;
		this.version = version;
	}

	public void appendTo(StringBuffer buf) {
		buf.append("AdviceMatcher[" + id + ',' + version + ']');
	}

	public boolean matches(Object arg) {
		if (!(arg instanceof IPublisherAdvice))
			return false;
		if (!(clazz.isAssignableFrom(arg.getClass())))
			return false;
		IPublisherAdvice advice = (IPublisherAdvice) arg;
		return advice.isApplicable("", false, id, version);
	}
}
