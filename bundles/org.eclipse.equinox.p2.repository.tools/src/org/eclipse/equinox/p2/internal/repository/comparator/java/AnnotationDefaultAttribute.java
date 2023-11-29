/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.comparator.java;

public class AnnotationDefaultAttribute extends ClassFileAttribute {

	private AnnotationComponentValue memberValue;

	/**
	 * Constructor for AnnotationDefaultAttribute.
	 */
	public AnnotationDefaultAttribute(byte[] classFileBytes, ConstantPool constantPool, int offset) throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		this.memberValue = new AnnotationComponentValue(classFileBytes, constantPool, offset + 6);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IAnnotationDefaultAttribute#getMemberValue()
	 */
	public AnnotationComponentValue getMemberValue() {
		return this.memberValue;
	}
}
