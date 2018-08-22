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

public class RuntimeInvisibleParameterAnnotationsAttribute extends ClassFileAttribute {

	private static final ParameterAnnotation[] NO_ENTRIES = new ParameterAnnotation[0];
	private ParameterAnnotation[] parameterAnnotations;
	private int parametersNumber;

	/**
	 * Constructor for RuntimeVisibleParameterAnnotations.
	 * @param classFileBytes
	 * @param constantPool
	 * @param offset
	 * @throws ClassFormatException
	 */
	public RuntimeInvisibleParameterAnnotationsAttribute(byte[] classFileBytes, ConstantPool constantPool, int offset) throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		final int length = u1At(classFileBytes, 6, offset);
		this.parametersNumber = length;
		if (length != 0) {
			int readOffset = 7;
			this.parameterAnnotations = new ParameterAnnotation[length];
			for (int i = 0; i < length; i++) {
				ParameterAnnotation parameterAnnotation = new ParameterAnnotation(classFileBytes, constantPool, offset + readOffset);
				this.parameterAnnotations[i] = parameterAnnotation;
				readOffset += parameterAnnotation.sizeInBytes();
			}
		} else {
			this.parameterAnnotations = NO_ENTRIES;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IRuntimeInvisibleParameterAnnotations#getAnnotations()
	 */
	public ParameterAnnotation[] getParameterAnnotations() {
		return this.parameterAnnotations;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IRuntimeInvisibleParameterAnnotations#getParametersNumber()
	 */
	public int getParametersNumber() {
		return this.parametersNumber;
	}
}
