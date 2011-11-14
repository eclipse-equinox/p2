/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * Sonatype, Inc. - initial implementation and ideas 
 ******************************************************************************/

package org.eclipse.equinox.p2.tests;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.LOCAL_VARIABLE})
public @interface IUDescription {
	public String content();
}
