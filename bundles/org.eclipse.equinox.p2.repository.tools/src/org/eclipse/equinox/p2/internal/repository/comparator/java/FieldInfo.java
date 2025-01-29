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

import java.util.Arrays;

public class FieldInfo extends ClassFileStruct {
	private final int accessFlags;
	private final int attributeBytes;
	private ClassFileAttribute[] attributes;
	private final int attributesCount;
	private ConstantValueAttribute constantValueAttribute;
	private final char[] descriptor;
	private final int descriptorIndex;
	private boolean isDeprecated;
	private boolean isSynthetic;
	private final char[] name;
	private final int nameIndex;

	/*
	 * @param classFileBytes byte[]
	 * @param constantPool IConstantPool
	 * @param offset int
	 */
	public FieldInfo(byte classFileBytes[], ConstantPool constantPool, int offset) throws ClassFormatException {
		final int flags = u2At(classFileBytes, 0, offset);
		this.accessFlags = flags;
		if ((flags & IModifierConstants.ACC_SYNTHETIC) != 0) {
			this.isSynthetic = true;
		}
		this.nameIndex = u2At(classFileBytes, 2, offset);
		ConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(this.nameIndex);
		if (constantPoolEntry.getKind() != ConstantPoolConstant.CONSTANT_Utf8) {
			throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
		}
		this.name = constantPoolEntry.getUtf8Value();

		this.descriptorIndex = u2At(classFileBytes, 4, offset);
		constantPoolEntry = constantPool.decodeEntry(this.descriptorIndex);
		if (constantPoolEntry.getKind() != ConstantPoolConstant.CONSTANT_Utf8) {
			throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
		}
		this.descriptor = constantPoolEntry.getUtf8Value();

		this.attributesCount = u2At(classFileBytes, 6, offset);
		this.attributes = ClassFileAttribute.NO_ATTRIBUTES;
		int readOffset = 8;
		if (this.attributesCount != 0) {
			this.attributes = new ClassFileAttribute[this.attributesCount];
		}
		int attributesIndex = 0;
		for (int i = 0; i < this.attributesCount; i++) {
			constantPoolEntry = constantPool.decodeEntry(u2At(classFileBytes, readOffset, offset));
			if (constantPoolEntry.getKind() != ConstantPoolConstant.CONSTANT_Utf8) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			char[] attributeName = constantPoolEntry.getUtf8Value();
			if (Arrays.equals(attributeName, AttributeNamesConstants.DEPRECATED)) {
				this.isDeprecated = true;
				this.attributes[attributesIndex++] = new ClassFileAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (Arrays.equals(attributeName, AttributeNamesConstants.SYNTHETIC)) {
				this.isSynthetic = true;
				this.attributes[attributesIndex++] = new ClassFileAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (Arrays.equals(attributeName, AttributeNamesConstants.CONSTANT_VALUE)) {
				this.constantValueAttribute = new ConstantValueAttribute(classFileBytes, constantPool, offset + readOffset);
				this.attributes[attributesIndex++] = this.constantValueAttribute;
			} else if (Arrays.equals(attributeName, AttributeNamesConstants.SIGNATURE)) {
				this.attributes[attributesIndex++] = new SignatureAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (Arrays.equals(attributeName, AttributeNamesConstants.RUNTIME_VISIBLE_ANNOTATIONS)) {
				this.attributes[attributesIndex++] = new RuntimeVisibleAnnotationsAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (Arrays.equals(attributeName, AttributeNamesConstants.RUNTIME_INVISIBLE_ANNOTATIONS)) {
				this.attributes[attributesIndex++] = new RuntimeInvisibleAnnotationsAttribute(classFileBytes, constantPool, offset + readOffset);
			} else {
				this.attributes[attributesIndex++] = new ClassFileAttribute(classFileBytes, constantPool, offset + readOffset);
			}
			readOffset += (6 + u4At(classFileBytes, readOffset + 2, offset));
		}

		this.attributeBytes = readOffset;
	}

	/*
	 * @see IFieldInfo#getAccessFlags()
	 */
	public int getAccessFlags() {
		return this.accessFlags;
	}

	/*
	 * @see IFieldInfo#getAttributeCount()
	 */
	public int getAttributeCount() {
		return this.attributesCount;
	}

	/*
	 * @see IFieldInfo#getAttributes()
	 */
	public ClassFileAttribute[] getAttributes() {
		return this.attributes;
	}

	/*
	 * @see IFieldInfo#getConstantValueAttribute()
	 */
	public ConstantValueAttribute getConstantValueAttribute() {
		return this.constantValueAttribute;
	}

	/*
	 * @see IFieldInfo#getDescriptor()
	 */
	public char[] getDescriptor() {
		return this.descriptor;
	}

	/*
	 * @see IFieldInfo#getDescriptorIndex()
	 */
	public int getDescriptorIndex() {
		return this.descriptorIndex;
	}

	/*
	 * @see IFieldInfo#getName()
	 */
	public char[] getName() {
		return this.name;
	}

	/*
	 * @see IFieldInfo#getNameIndex()
	 */
	public int getNameIndex() {
		return this.nameIndex;
	}

	/*
	 * @see IFieldInfo#hasConstantValueAttribute()
	 */
	public boolean hasConstantValueAttribute() {
		return this.constantValueAttribute != null;
	}

	/*
	 * @see IFieldInfo#isDeprecated()
	 */
	public boolean isDeprecated() {
		return this.isDeprecated;
	}

	/*
	 * @see IFieldInfo#isSynthetic()
	 */
	public boolean isSynthetic() {
		return this.isSynthetic;
	}

	int sizeInBytes() {
		return this.attributeBytes;
	}
}
