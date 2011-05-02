/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.comparator.java;

public class RuntimeVisibleAnnotationsAttribute extends ClassFileAttribute {

	private static final Annotation[] NO_ENTRIES = new Annotation[0];
	private int annotationsNumber;
	private Annotation[] annotations;

	/**
	 * Constructor for RuntimeVisibleAnnotations.
	 * @param classFileBytes
	 * @param constantPool
	 * @param offset
	 * @throws ClassFormatException
	 */
	public RuntimeVisibleAnnotationsAttribute(byte[] classFileBytes, ConstantPool constantPool, int offset) throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		final int length = u2At(classFileBytes, 6, offset);
		this.annotationsNumber = length;
		if (length != 0) {
			int readOffset = 8;
			this.annotations = new Annotation[length];
			for (int i = 0; i < length; i++) {
				Annotation annotation = new Annotation(classFileBytes, constantPool, offset + readOffset);
				this.annotations[i] = annotation;
				readOffset += annotation.sizeInBytes();
			}
		} else {
			this.annotations = NO_ENTRIES;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IRuntimeVisibleAnnotations#getAnnotations()
	 */
	public Annotation[] getAnnotations() {
		return this.annotations;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IRuntimeVisibleAnnotations#getAnnotationsNumber()
	 */
	public int getAnnotationsNumber() {
		return this.annotationsNumber;
	}
}
