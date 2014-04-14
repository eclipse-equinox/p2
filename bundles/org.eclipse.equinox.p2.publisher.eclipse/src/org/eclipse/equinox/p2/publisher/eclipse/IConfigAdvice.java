/*******************************************************************************
 * Copyright (c) 2008, 2014 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   Rapicorp Corporation - ongoing enhancements
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import java.util.Map;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;

public interface IConfigAdvice extends IPublisherAdvice {

	public BundleInfo[] getBundles();

	public Map<String, String> getProperties();

}
