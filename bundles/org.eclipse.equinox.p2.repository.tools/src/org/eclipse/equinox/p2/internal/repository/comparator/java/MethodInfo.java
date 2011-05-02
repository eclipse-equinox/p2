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

import java.util.Arrays;

public class MethodInfo extends ClassFileStruct {
	private int accessFlags;
	private int attributeBytes;
	private ClassFileAttribute[] attributes;
	private int attributesCount;
	private CodeAttribute codeAttribute;
	private char[] descriptor;
	private int descriptorIndex;
	private ExceptionAttribute exceptionAttribute;
	private boolean isDeprecated;
	private boolean isSynthetic;
	private char[] name;
	private int nameIndex;

	/*
	 * @param classFileBytes byte[]
	 * @param constantPool IConstantPool
	 * @param offset int
	 * @param decodingFlags int
	 */
	public MethodInfo(byte classFileBytes[], ConstantPool constantPool, int offset, int decodingFlags) throws ClassFormatException {

		boolean no_code_attribute = (decodingFlags & ClassFileReader.METHOD_BODIES) == 0;
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
		if (this.attributesCount != 0) {
			if (no_code_attribute && !isAbstract() && !isNative()) {
				if (this.attributesCount != 1) {
					this.attributes = new ClassFileAttribute[this.attributesCount - 1];
				}
			} else {
				this.attributes = new ClassFileAttribute[this.attributesCount];
			}
		}
		int attributesIndex = 0;
		int readOffset = 8;
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
			} else if (Arrays.equals(attributeName, AttributeNamesConstants.CODE)) {
				if (!no_code_attribute) {
					this.codeAttribute = new CodeAttribute(classFileBytes, constantPool, offset + readOffset);
					this.attributes[attributesIndex++] = this.codeAttribute;
				}
			} else if (Arrays.equals(attributeName, AttributeNamesConstants.EXCEPTIONS)) {
				this.exceptionAttribute = new ExceptionAttribute(classFileBytes, constantPool, offset + readOffset);
				this.attributes[attributesIndex++] = this.exceptionAttribute;
			} else if (Arrays.equals(attributeName, AttributeNamesConstants.SIGNATURE)) {
				this.attributes[attributesIndex++] = new SignatureAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (Arrays.equals(attributeName, AttributeNamesConstants.RUNTIME_VISIBLE_ANNOTATIONS)) {
				this.attributes[attributesIndex++] = new RuntimeVisibleAnnotationsAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (Arrays.equals(attributeName, AttributeNamesConstants.RUNTIME_INVISIBLE_ANNOTATIONS)) {
				this.attributes[attributesIndex++] = new RuntimeInvisibleAnnotationsAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (Arrays.equals(attributeName, AttributeNamesConstants.RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS)) {
				this.attributes[attributesIndex++] = new RuntimeVisibleParameterAnnotationsAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (Arrays.equals(attributeName, AttributeNamesConstants.RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS)) {
				this.attributes[attributesIndex++] = new RuntimeInvisibleParameterAnnotationsAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (Arrays.equals(attributeName, AttributeNamesConstants.ANNOTATION_DEFAULT)) {
				this.attributes[attributesIndex++] = new AnnotationDefaultAttribute(classFileBytes, constantPool, offset + readOffset);
			} else {
				this.attributes[attributesIndex++] = new ClassFileAttribute(classFileBytes, constantPool, offset + readOffset);
			}
			readOffset += (6 + u4At(classFileBytes, readOffset + 2, offset));
		}
		this.attributeBytes = readOffset;
	}

	/*
	 * @see IMethodInfo#getAccessFlags()
	 */
	public int getAccessFlags() {
		return this.accessFlags;
	}

	/*
	 * @see IMethodInfo#getAttributeCount()
	 */
	public int getAttributeCount() {
		return this.attributesCount;
	}

	/*
	 * @see IMethodInfo#getAttributes()
	 */
	public ClassFileAttribute[] getAttributes() {
		return this.attributes;
	}

	/*
	 * @see IMethodInfo#getCodeAttribute()
	 */
	public CodeAttribute getCodeAttribute() {
		return this.codeAttribute;
	}

	/*
	 * @see IMethodInfo#getDescriptor()
	 */
	public char[] getDescriptor() {
		return this.descriptor;
	}

	/*
	 * @see IMethodInfo#getDescriptorIndex()
	 */
	public int getDescriptorIndex() {
		return this.descriptorIndex;
	}

	/*
	 * @see IMethodInfo#getExceptionAttribute()
	 */
	public ExceptionAttribute getExceptionAttribute() {
		return this.exceptionAttribute;
	}

	/*
	 * @see IMethodInfo#getName()
	 */
	public char[] getName() {
		return this.name;
	}

	/*
	 * @see IMethodInfo#getNameIndex()
	 */
	public int getNameIndex() {
		return this.nameIndex;
	}

	private boolean isAbstract() {
		return (this.accessFlags & IModifierConstants.ACC_ABSTRACT) != 0;
	}

	/*
	 * @see IMethodInfo#isClinit()
	 */
	public boolean isClinit() {
		return this.name[0] == '<' && this.name.length == 8; // Can only match <clinit>
	}

	/*
	 * @see IMethodInfo#isConstructor()
	 */
	public boolean isConstructor() {
		return this.name[0] == '<' && this.name.length == 6; // Can only match <init>
	}

	/*
	 * @see IMethodInfo#isDeprecated()
	 */
	public boolean isDeprecated() {
		return this.isDeprecated;
	}

	private boolean isNative() {
		return (this.accessFlags & IModifierConstants.ACC_NATIVE) != 0;
	}

	/*
	 * @see IMethodInfo#isSynthetic()
	 */
	public boolean isSynthetic() {
		return this.isSynthetic;
	}

	int sizeInBytes() {
		return this.attributeBytes;
	}
}
