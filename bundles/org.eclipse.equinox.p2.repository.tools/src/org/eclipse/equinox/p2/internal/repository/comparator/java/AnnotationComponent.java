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

public class AnnotationComponent extends ClassFileStruct {

	private int componentNameIndex;
	private char[] componentName;
	private AnnotationComponentValue componentValue;
	private int readOffset;

	public AnnotationComponent(byte[] classFileBytes, ConstantPool constantPool, int offset) throws ClassFormatException {
		final int nameIndex = u2At(classFileBytes, 0, offset);
		this.componentNameIndex = nameIndex;
		if (nameIndex != 0) {
			ConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(nameIndex);
			if (constantPoolEntry.getKind() != ConstantPoolConstant.CONSTANT_Utf8) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			this.componentName = constantPoolEntry.getUtf8Value();
		}
		this.readOffset = 2;
		AnnotationComponentValue value = new AnnotationComponentValue(classFileBytes, constantPool, offset + this.readOffset);
		this.componentValue = value;
		this.readOffset += value.sizeInBytes();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IAnnotationComponent#getComponentNameIndex()
	 */
	public int getComponentNameIndex() {
		return this.componentNameIndex;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IAnnotationComponent#getComponentName()
	 */
	public char[] getComponentName() {
		return this.componentName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IAnnotationComponent#getComponentValue()
	 */
	public AnnotationComponentValue getComponentValue() {
		return this.componentValue;
	}

	int sizeInBytes() {
		return this.readOffset;
	}
}
