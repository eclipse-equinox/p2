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

public class Annotation extends ClassFileStruct {

	private static final AnnotationComponent[] NO_ENTRIES = new AnnotationComponent[0];

	private int typeIndex;
	private char[] typeName;
	private int componentsNumber;
	private AnnotationComponent[] components;
	private int readOffset;

	/**
	 * Constructor for Annotation.
	 */
	public Annotation(byte[] classFileBytes, ConstantPool constantPool, int offset) throws ClassFormatException {

		final int index = u2At(classFileBytes, 0, offset);
		this.typeIndex = index;
		if (index != 0) {
			ConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(index);
			if (constantPoolEntry.getKind() != ConstantPoolConstant.CONSTANT_Utf8) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			this.typeName = constantPoolEntry.getUtf8Value();
		} else {
			throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
		}
		final int length = u2At(classFileBytes, 2, offset);
		this.componentsNumber = length;
		this.readOffset = 4;
		if (length != 0) {
			this.components = new AnnotationComponent[length];
			for (int i = 0; i < length; i++) {
				AnnotationComponent component = new AnnotationComponent(classFileBytes, constantPool, offset + this.readOffset);
				this.components[i] = component;
				this.readOffset += component.sizeInBytes();
			}
		} else {
			this.components = NO_ENTRIES;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IAnnotation#getTypeIndex()
	 */
	public int getTypeIndex() {
		return this.typeIndex;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IAnnotation#getComponentsNumber()
	 */
	public int getComponentsNumber() {
		return this.componentsNumber;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IAnnotation#getComponents()
	 */
	public AnnotationComponent[] getComponents() {
		return this.components;
	}

	int sizeInBytes() {
		return this.readOffset;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IAnnotation#getTypeName()
	 */
	public char[] getTypeName() {
		return this.typeName;
	}
}
