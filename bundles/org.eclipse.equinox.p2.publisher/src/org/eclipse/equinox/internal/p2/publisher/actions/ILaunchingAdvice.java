/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.actions;

import org.eclipse.equinox.internal.p2.publisher.IPublishingAdvice;

public interface ILaunchingAdvice extends IPublishingAdvice {

	public String[] getVMArguments();

	public String[] getProgramArguments();

	public String getExecutableName();
}
