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
import java.util.Comparator;
import org.eclipse.osgi.util.NLS;

/**
 * Disassembler of .class files. It generates an output in the Writer that looks close to
 * the javap output.
 */
public class Disassembler {
	/**
	 * The mode is the detailed mode to disassemble ClassFileReader. It returns the magic
	 * numbers, the version numbers and field and method descriptors.
	 */
	public final static int DETAILED = 1;

	/**
	 * This mode is used to compact the class name to a simple name instead of a qualified name.
	 * @since 3.1
	 */
	public final static int COMPACT = 8;

	private static final char[] ANY_EXCEPTION = Messages.classfileformat_anyexceptionhandler.toCharArray();
	private static final String VERSION_UNKNOWN = Messages.classfileformat_versionUnknown;

	private boolean appendModifier(StringBuffer buffer, int accessFlags, int modifierConstant, String modifier, boolean firstModifier) {
		if ((accessFlags & modifierConstant) != 0) {
			if (!firstModifier) {
				buffer.append(Messages.disassembler_space);
			}
			if (firstModifier) {
				firstModifier = false;
			}
			buffer.append(modifier);
		}
		return firstModifier;
	}

	private void decodeModifiers(StringBuffer buffer, int accessFlags, int[] checkBits) {
		decodeModifiers(buffer, accessFlags, false, false, checkBits);
	}

	private void decodeModifiers(StringBuffer buffer, int accessFlags, boolean printDefault, boolean asBridge, int[] checkBits) {
		if (checkBits == null)
			return;
		boolean firstModifier = true;
		for (int i = 0, max = checkBits.length; i < max; i++) {
			switch (checkBits[i]) {
				case IModifierConstants.ACC_PUBLIC :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_PUBLIC, "public", firstModifier); //$NON-NLS-1$
					break;
				case IModifierConstants.ACC_PROTECTED :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_PROTECTED, "protected", firstModifier); //$NON-NLS-1$
					break;
				case IModifierConstants.ACC_PRIVATE :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_PRIVATE, "private", firstModifier); //$NON-NLS-1$
					break;
				case IModifierConstants.ACC_ABSTRACT :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_ABSTRACT, "abstract", firstModifier); //$NON-NLS-1$
					break;
				case IModifierConstants.ACC_STATIC :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_STATIC, "static", firstModifier); //$NON-NLS-1$
					break;
				case IModifierConstants.ACC_FINAL :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_FINAL, "final", firstModifier); //$NON-NLS-1$
					break;
				case IModifierConstants.ACC_SYNCHRONIZED :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_SYNCHRONIZED, "synchronized", firstModifier); //$NON-NLS-1$
					break;
				case IModifierConstants.ACC_NATIVE :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_NATIVE, "native", firstModifier); //$NON-NLS-1$
					break;
				case IModifierConstants.ACC_STRICT :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_STRICT, "strictfp", firstModifier); //$NON-NLS-1$
					break;
				case IModifierConstants.ACC_TRANSIENT :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_TRANSIENT, "transient", firstModifier); //$NON-NLS-1$
					break;
				case IModifierConstants.ACC_VOLATILE :
					// case IModifierConstants.ACC_BRIDGE :
					if (asBridge) {
						firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_BRIDGE, "bridge", firstModifier); //$NON-NLS-1$
					} else {
						firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_VOLATILE, "volatile", firstModifier); //$NON-NLS-1$
					}
					break;
				case IModifierConstants.ACC_ENUM :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_ENUM, "enum", firstModifier); //$NON-NLS-1$
					break;
			}
		}
		if (!firstModifier) {
			if (!printDefault)
				buffer.append(Messages.disassembler_space);
		} else if (printDefault) {
			// no modifier: package default visibility
			buffer.append("default"); //$NON-NLS-1$
		}
	}

	private void decodeModifiersForField(StringBuffer buffer, int accessFlags) {
		decodeModifiers(buffer, accessFlags, new int[] {IModifierConstants.ACC_PUBLIC, IModifierConstants.ACC_PROTECTED, IModifierConstants.ACC_PRIVATE, IModifierConstants.ACC_STATIC, IModifierConstants.ACC_FINAL, IModifierConstants.ACC_TRANSIENT, IModifierConstants.ACC_VOLATILE, IModifierConstants.ACC_ENUM});
	}

	private final void decodeModifiersForInnerClasses(StringBuffer buffer, int accessFlags, boolean printDefault) {
		decodeModifiers(buffer, accessFlags, printDefault, false, new int[] {IModifierConstants.ACC_PUBLIC, IModifierConstants.ACC_PROTECTED, IModifierConstants.ACC_PRIVATE, IModifierConstants.ACC_ABSTRACT, IModifierConstants.ACC_STATIC, IModifierConstants.ACC_FINAL,});
	}

	private final void decodeModifiersForMethod(StringBuffer buffer, int accessFlags) {
		decodeModifiers(buffer, accessFlags, false, true, new int[] {IModifierConstants.ACC_PUBLIC, IModifierConstants.ACC_PROTECTED, IModifierConstants.ACC_PRIVATE, IModifierConstants.ACC_ABSTRACT, IModifierConstants.ACC_STATIC, IModifierConstants.ACC_FINAL, IModifierConstants.ACC_SYNCHRONIZED, IModifierConstants.ACC_NATIVE, IModifierConstants.ACC_STRICT, IModifierConstants.ACC_BRIDGE,});
	}

	private final void decodeModifiersForType(StringBuffer buffer, int accessFlags) {
		decodeModifiers(buffer, accessFlags, new int[] {IModifierConstants.ACC_PUBLIC, IModifierConstants.ACC_ABSTRACT, IModifierConstants.ACC_FINAL,});
	}

	public static String escapeString(String s) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0, max = s.length(); i < max; i++) {
			char c = s.charAt(i);
			switch (c) {
				case '\b' :
					buffer.append("\\b"); //$NON-NLS-1$
					break;
				case '\t' :
					buffer.append("\\t"); //$NON-NLS-1$
					break;
				case '\n' :
					buffer.append("\\n"); //$NON-NLS-1$
					break;
				case '\f' :
					buffer.append("\\f"); //$NON-NLS-1$
					break;
				case '\r' :
					buffer.append("\\r"); //$NON-NLS-1$
					break;
				case '\0' :
					buffer.append("\\0"); //$NON-NLS-1$
					break;
				case '\1' :
					buffer.append("\\1"); //$NON-NLS-1$
					break;
				case '\2' :
					buffer.append("\\2"); //$NON-NLS-1$
					break;
				case '\3' :
					buffer.append("\\3"); //$NON-NLS-1$
					break;
				case '\4' :
					buffer.append("\\4"); //$NON-NLS-1$
					break;
				case '\5' :
					buffer.append("\\5"); //$NON-NLS-1$
					break;
				case '\6' :
					buffer.append("\\6"); //$NON-NLS-1$
					break;
				case '\7' :
					buffer.append("\\7"); //$NON-NLS-1$
					break;
				default :
					buffer.append(c);
			}
		}
		return buffer.toString();
	}

	static String decodeStringValue(char[] chars) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0, max = chars.length; i < max; i++) {
			char c = chars[i];
			switch (c) {
				case '\b' :
					buffer.append("\\b"); //$NON-NLS-1$
					break;
				case '\t' :
					buffer.append("\\t"); //$NON-NLS-1$
					break;
				case '\n' :
					buffer.append("\\n"); //$NON-NLS-1$
					break;
				case '\f' :
					buffer.append("\\f"); //$NON-NLS-1$
					break;
				case '\r' :
					buffer.append("\\r"); //$NON-NLS-1$
					break;
				case '\0' :
					buffer.append("\\0"); //$NON-NLS-1$
					break;
				case '\1' :
					buffer.append("\\1"); //$NON-NLS-1$
					break;
				case '\2' :
					buffer.append("\\2"); //$NON-NLS-1$
					break;
				case '\3' :
					buffer.append("\\3"); //$NON-NLS-1$
					break;
				case '\4' :
					buffer.append("\\4"); //$NON-NLS-1$
					break;
				case '\5' :
					buffer.append("\\5"); //$NON-NLS-1$
					break;
				case '\6' :
					buffer.append("\\6"); //$NON-NLS-1$
					break;
				case '\7' :
					buffer.append("\\7"); //$NON-NLS-1$
					break;
				default :
					buffer.append(c);
			}
		}
		return buffer.toString();
	}

	static String decodeStringValue(String s) {
		return decodeStringValue(s.toCharArray());
	}

	/*
	 * @see org.eclipse.jdt.core.util.ClassFileBytesDisassembler#disassemble(byte[], java.lang.String, int)
	 */
	public String disassemble(byte[] classFileBytes, String lineSeparator, int mode) throws ClassFormatException {
		try {
			return disassemble(new ClassFileReader(classFileBytes, ClassFileReader.ALL), lineSeparator, mode);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ClassFormatException(e.getMessage(), e);
		}
	}

	private void disassemble(Annotation annotation, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		final char[] typeName = CharOperation.replaceOnCopy(annotation.getTypeName(), '/', '.');
		buffer.append(NLS.bind(Messages.disassembler_annotationentrystart, new String[] {new String(returnClassName(Signature.toCharArray(typeName), '.', mode))}));
		final AnnotationComponent[] components = annotation.getComponents();
		for (int i = 0, max = components.length; i < max; i++) {
			disassemble(components[i], buffer, lineSeparator, tabNumber + 1, mode);
		}
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		buffer.append(Messages.disassembler_annotationentryend);
	}

	private void disassemble(AnnotationComponent annotationComponent, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		buffer.append(NLS.bind(Messages.disassembler_annotationcomponent, new String[] {new String(annotationComponent.getComponentName())}));
		disassemble(annotationComponent.getComponentValue(), buffer, lineSeparator, tabNumber + 1, mode);
	}

	private void disassemble(AnnotationComponentValue annotationComponentValue, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		switch (annotationComponentValue.getTag()) {
			case AnnotationComponentValue.BYTE_TAG :
			case AnnotationComponentValue.CHAR_TAG :
			case AnnotationComponentValue.DOUBLE_TAG :
			case AnnotationComponentValue.FLOAT_TAG :
			case AnnotationComponentValue.INTEGER_TAG :
			case AnnotationComponentValue.LONG_TAG :
			case AnnotationComponentValue.SHORT_TAG :
			case AnnotationComponentValue.BOOLEAN_TAG :
			case AnnotationComponentValue.STRING_TAG :
				ConstantPoolEntry constantPoolEntry = annotationComponentValue.getConstantValue();
				String value = null;
				switch (constantPoolEntry.getKind()) {
					case ConstantPoolConstant.CONSTANT_Long :
						value = constantPoolEntry.getLongValue() + "L"; //$NON-NLS-1$
						break;
					case ConstantPoolConstant.CONSTANT_Float :
						value = constantPoolEntry.getFloatValue() + "f"; //$NON-NLS-1$
						break;
					case ConstantPoolConstant.CONSTANT_Double :
						value = Double.toString(constantPoolEntry.getDoubleValue());
						break;
					case ConstantPoolConstant.CONSTANT_Integer :
						switch (annotationComponentValue.getTag()) {
							case AnnotationComponentValue.CHAR_TAG :
								value = "'" + (char) constantPoolEntry.getIntegerValue() + "'"; //$NON-NLS-1$//$NON-NLS-2$
								break;
							case AnnotationComponentValue.BOOLEAN_TAG :
								value = constantPoolEntry.getIntegerValue() == 1 ? "true" : "false";//$NON-NLS-1$//$NON-NLS-2$
								break;
							case AnnotationComponentValue.BYTE_TAG :
								value = "(byte) " + constantPoolEntry.getIntegerValue(); //$NON-NLS-1$
								break;
							case AnnotationComponentValue.SHORT_TAG :
								value = "(short) " + constantPoolEntry.getIntegerValue(); //$NON-NLS-1$
								break;
							case AnnotationComponentValue.INTEGER_TAG :
								value = "(int) " + constantPoolEntry.getIntegerValue(); //$NON-NLS-1$
						}
						break;
					case ConstantPoolConstant.CONSTANT_Utf8 :
						value = "\"" + decodeStringValue(constantPoolEntry.getUtf8Value()) + "\"";//$NON-NLS-1$//$NON-NLS-2$
				}
				buffer.append(NLS.bind(Messages.disassembler_annotationdefaultvalue, value));
				break;
			case AnnotationComponentValue.ENUM_TAG :
				final char[] typeName = CharOperation.replaceOnCopy(annotationComponentValue.getEnumConstantTypeName(), '/', '.');
				final char[] constantName = annotationComponentValue.getEnumConstantName();
				buffer.append(NLS.bind(Messages.disassembler_annotationenumvalue, new String[] {new String(returnClassName(Signature.toCharArray(typeName), '.', mode)), new String(constantName)}));
				break;
			case AnnotationComponentValue.CLASS_TAG :
				constantPoolEntry = annotationComponentValue.getClassInfo();
				final char[] className = CharOperation.replaceOnCopy(constantPoolEntry.getUtf8Value(), '/', '.');
				buffer.append(NLS.bind(Messages.disassembler_annotationclassvalue, new String[] {new String(returnClassName(Signature.toCharArray(className), '.', mode))}));
				break;
			case AnnotationComponentValue.ANNOTATION_TAG :
				buffer.append(Messages.disassembler_annotationannotationvalue);
				Annotation annotation = annotationComponentValue.getAnnotationValue();
				disassemble(annotation, buffer, lineSeparator, tabNumber + 1, mode);
				break;
			case AnnotationComponentValue.ARRAY_TAG :
				buffer.append(Messages.disassembler_annotationarrayvaluestart);
				final AnnotationComponentValue[] annotationComponentValues = annotationComponentValue.getAnnotationComponentValues();
				for (int i = 0, max = annotationComponentValues.length; i < max; i++) {
					writeNewLine(buffer, lineSeparator, tabNumber + 1);
					disassemble(annotationComponentValues[i], buffer, lineSeparator, tabNumber + 1, mode);
				}
				writeNewLine(buffer, lineSeparator, tabNumber + 1);
				buffer.append(Messages.disassembler_annotationarrayvalueend);
		}
	}

	private void disassemble(AnnotationDefaultAttribute annotationDefaultAttribute, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		buffer.append(Messages.disassembler_annotationdefaultheader);
		AnnotationComponentValue componentValue = annotationDefaultAttribute.getMemberValue();
		writeNewLine(buffer, lineSeparator, tabNumber + 2);
		disassemble(componentValue, buffer, lineSeparator, tabNumber + 1, mode);
	}

	/**
	 * Disassemble a method info header
	 */
	private void disassemble(ClassFileReader classFileReader, char[] className, MethodInfo methodInfo, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber);
		final CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
		final char[] methodDescriptor = methodInfo.getDescriptor();
		final SignatureAttribute signatureAttribute = (SignatureAttribute) Utility.getAttribute(methodInfo, AttributeNamesConstants.SIGNATURE);
		final ClassFileAttribute runtimeVisibleAnnotationsAttribute = Utility.getAttribute(methodInfo, AttributeNamesConstants.RUNTIME_VISIBLE_ANNOTATIONS);
		final ClassFileAttribute runtimeInvisibleAnnotationsAttribute = Utility.getAttribute(methodInfo, AttributeNamesConstants.RUNTIME_INVISIBLE_ANNOTATIONS);
		final ClassFileAttribute runtimeVisibleParameterAnnotationsAttribute = Utility.getAttribute(methodInfo, AttributeNamesConstants.RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS);
		final ClassFileAttribute runtimeInvisibleParameterAnnotationsAttribute = Utility.getAttribute(methodInfo, AttributeNamesConstants.RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS);
		final ClassFileAttribute annotationDefaultAttribute = Utility.getAttribute(methodInfo, AttributeNamesConstants.ANNOTATION_DEFAULT);
		if (checkMode(mode, DETAILED)) {
			buffer.append(NLS.bind(Messages.classfileformat_methoddescriptor, new String[] {new String(methodDescriptor)}));
			if (methodInfo.isDeprecated()) {
				buffer.append(Messages.disassembler_deprecated);
			}
			writeNewLine(buffer, lineSeparator, tabNumber);
			if (signatureAttribute != null) {
				buffer.append(NLS.bind(Messages.disassembler_signatureattributeheader, new String(signatureAttribute.getSignature())));
				writeNewLine(buffer, lineSeparator, tabNumber);
			}
			if (codeAttribute != null) {
				buffer.append(NLS.bind(Messages.classfileformat_stacksAndLocals, new String[] {Integer.toString(codeAttribute.getMaxStack()), Integer.toString(codeAttribute.getMaxLocals())}));
				writeNewLine(buffer, lineSeparator, tabNumber);
			}
			// disassemble compact version of annotations
			if (runtimeInvisibleAnnotationsAttribute != null) {
				disassembleAsModifier((RuntimeInvisibleAnnotationsAttribute) runtimeInvisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
				writeNewLine(buffer, lineSeparator, tabNumber);
			}
			if (runtimeVisibleAnnotationsAttribute != null) {
				disassembleAsModifier((RuntimeVisibleAnnotationsAttribute) runtimeVisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
				writeNewLine(buffer, lineSeparator, tabNumber);
			}
		}
		final int accessFlags = methodInfo.getAccessFlags();
		decodeModifiersForMethod(buffer, accessFlags);
		if (methodInfo.isSynthetic()) {
			buffer.append("synthetic"); //$NON-NLS-1$
			buffer.append(Messages.disassembler_space);
		}
		CharOperation.replace(methodDescriptor, '/', '.');
		final boolean isVarArgs = isVarArgs(methodInfo);
		char[] methodHeader = null;
		if (methodInfo.isConstructor()) {
			methodHeader = Signature.toCharArray(methodDescriptor, returnClassName(className, '.', COMPACT), null, !checkMode(mode, COMPACT), false, isVarArgs);
		} else if (methodInfo.isClinit()) {
			methodHeader = Messages.classfileformat_clinitname.toCharArray();
		} else {
			methodHeader = Signature.toCharArray(methodDescriptor, methodInfo.getName(), null, !checkMode(mode, COMPACT), true, isVarArgs);
		}
		if (checkMode(mode, DETAILED) && (runtimeInvisibleParameterAnnotationsAttribute != null || runtimeVisibleParameterAnnotationsAttribute != null)) {
			ParameterAnnotation[] invisibleParameterAnnotations = null;
			ParameterAnnotation[] visibleParameterAnnotations = null;
			int length = -1;
			if (runtimeInvisibleParameterAnnotationsAttribute != null) {
				RuntimeInvisibleParameterAnnotationsAttribute attribute = (RuntimeInvisibleParameterAnnotationsAttribute) runtimeInvisibleParameterAnnotationsAttribute;
				invisibleParameterAnnotations = attribute.getParameterAnnotations();
				length = invisibleParameterAnnotations.length;
			}
			if (runtimeVisibleParameterAnnotationsAttribute != null) {
				RuntimeVisibleParameterAnnotationsAttribute attribute = (RuntimeVisibleParameterAnnotationsAttribute) runtimeVisibleParameterAnnotationsAttribute;
				visibleParameterAnnotations = attribute.getParameterAnnotations();
				length = visibleParameterAnnotations.length;
			}
			int insertionPosition = CharOperation.indexOf('(', methodHeader) + 1;
			int start = 0;
			StringBuffer stringBuffer = new StringBuffer();
			stringBuffer.append(methodHeader, 0, insertionPosition);
			for (int i = 0; i < length; i++) {
				if (i > 0) {
					stringBuffer.append(' ');
				}
				int stringBufferSize = stringBuffer.length();
				if (runtimeVisibleParameterAnnotationsAttribute != null) {
					disassembleAsModifier((RuntimeVisibleParameterAnnotationsAttribute) runtimeVisibleParameterAnnotationsAttribute, stringBuffer, i, lineSeparator, tabNumber, mode);
				}
				if (runtimeInvisibleParameterAnnotationsAttribute != null) {
					if (stringBuffer.length() != stringBufferSize) {
						stringBuffer.append(' ');
						stringBufferSize = stringBuffer.length();
					}
					disassembleAsModifier((RuntimeInvisibleParameterAnnotationsAttribute) runtimeInvisibleParameterAnnotationsAttribute, stringBuffer, i, lineSeparator, tabNumber, mode);
				}
				if (i == 0 && stringBuffer.length() != stringBufferSize) {
					stringBuffer.append(' ');
				}
				start = insertionPosition;
				insertionPosition = CharOperation.indexOf(',', methodHeader, start + 1) + 1;
				if (insertionPosition == 0) {
					stringBuffer.append(methodHeader, start, methodHeader.length - start);
				} else {
					stringBuffer.append(methodHeader, start, insertionPosition - start);
				}
			}
			buffer.append(stringBuffer);
		} else {
			buffer.append(methodHeader);
		}
		ExceptionAttribute exceptionAttribute = methodInfo.getExceptionAttribute();
		if (exceptionAttribute != null) {
			buffer.append(" throws "); //$NON-NLS-1$
			char[][] exceptionNames = exceptionAttribute.getExceptionNames();
			int length = exceptionNames.length;
			for (int i = 0; i < length; i++) {
				if (i != 0) {
					buffer.append(Messages.disassembler_comma).append(Messages.disassembler_space);
				}
				char[] exceptionName = exceptionNames[i];
				CharOperation.replace(exceptionName, '/', '.');
				buffer.append(returnClassName(exceptionName, '.', mode));
			}
		}
		if (checkMode(mode, DETAILED)) {
			if (annotationDefaultAttribute != null) {
				buffer.append(" default "); //$NON-NLS-1$
				disassembleAsModifier((AnnotationDefaultAttribute) annotationDefaultAttribute, buffer, lineSeparator, tabNumber, mode);
			}
		}
		buffer.append(Messages.disassembler_endofmethodheader);

		if (checkMode(mode, DETAILED)) {
			if (codeAttribute != null) {
				disassemble(codeAttribute, methodDescriptor, (accessFlags & IModifierConstants.ACC_STATIC) != 0, buffer, lineSeparator, tabNumber, mode);
			}
			if (annotationDefaultAttribute != null) {
				disassemble((AnnotationDefaultAttribute) annotationDefaultAttribute, buffer, lineSeparator, tabNumber, mode);
			}
			if (runtimeVisibleAnnotationsAttribute != null) {
				disassemble((RuntimeVisibleAnnotationsAttribute) runtimeVisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
			}
			if (runtimeInvisibleAnnotationsAttribute != null) {
				disassemble((RuntimeInvisibleAnnotationsAttribute) runtimeInvisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
			}
			if (runtimeVisibleParameterAnnotationsAttribute != null) {
				disassemble((RuntimeVisibleParameterAnnotationsAttribute) runtimeVisibleParameterAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
			}
			if (runtimeInvisibleParameterAnnotationsAttribute != null) {
				disassemble((RuntimeInvisibleParameterAnnotationsAttribute) runtimeInvisibleParameterAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
			}
		}
	}

	/**
	 * Answers back the disassembled string of the ClassFileReader according to the
	 * mode.
	 * This is an output quite similar to the javap tool.
	 *
	 * @param classFileReader The classFileReader to be disassembled
	 * @param lineSeparator the line separator to use.
	 * @param mode the mode used to disassemble the ClassFileReader
	 *
	 * @return the disassembled string of the ClassFileReader according to the mode
	 */
	private String disassemble(ClassFileReader classFileReader, String lineSeparator, int mode) {
		if (classFileReader == null)
			return Utility.EMPTY_STRING;
		char[] className = classFileReader.getClassName();
		if (className == null) {
			// incomplete initialization. We cannot go further.
			return Utility.EMPTY_STRING;
		}
		className = CharOperation.replaceOnCopy(className, '/', '.');
		final int accessFlags = classFileReader.getAccessFlags();
		final boolean isEnum = (accessFlags & IModifierConstants.ACC_ENUM) != 0;

		StringBuffer buffer = new StringBuffer();
		SourceFileAttribute sourceAttribute = classFileReader.getSourceFileAttribute();
		ClassFileAttribute classFileAttribute = Utility.getAttribute(classFileReader, AttributeNamesConstants.SIGNATURE);
		SignatureAttribute signatureAttribute = (SignatureAttribute) classFileAttribute;
		if (checkMode(mode, DETAILED)) {
			int minorVersion = classFileReader.getMinorVersion();
			int majorVersion = classFileReader.getMajorVersion();
			buffer.append(Messages.disassembler_begincommentline);
			if (sourceAttribute != null) {
				buffer.append(Messages.disassembler_sourceattributeheader);
				buffer.append(sourceAttribute.getSourceFileName());
			}
			String versionNumber = VERSION_UNKNOWN;
			if (minorVersion == 3 && majorVersion == 45) {
				versionNumber = IModifierConstants.VERSION_1_1;
			} else if (minorVersion == 0 && majorVersion == 46) {
				versionNumber = IModifierConstants.VERSION_1_2;
			} else if (minorVersion == 0 && majorVersion == 47) {
				versionNumber = IModifierConstants.VERSION_1_3;
			} else if (minorVersion == 0 && majorVersion == 48) {
				versionNumber = IModifierConstants.VERSION_1_4;
			} else if (minorVersion == 0 && majorVersion == 49) {
				versionNumber = IModifierConstants.VERSION_1_5;
			} else if (minorVersion == 0 && majorVersion == 50) {
				versionNumber = IModifierConstants.VERSION_1_6;
			} else if (minorVersion == 0 && majorVersion == 51) {
				versionNumber = IModifierConstants.VERSION_1_7;
			}
			buffer.append(NLS.bind(Messages.classfileformat_versiondetails, new String[] {versionNumber, Integer.toString(majorVersion), Integer.toString(minorVersion), ((accessFlags & IModifierConstants.ACC_SUPER) != 0 ? Messages.classfileformat_superflagisset : Messages.classfileformat_superflagisnotset) + (isDeprecated(classFileReader) ? ", deprecated" : Utility.EMPTY_STRING)//$NON-NLS-1$
			}));
			writeNewLine(buffer, lineSeparator, 0);
			if (signatureAttribute != null) {
				buffer.append(NLS.bind(Messages.disassembler_signatureattributeheader, new String(signatureAttribute.getSignature())));
				writeNewLine(buffer, lineSeparator, 0);
			}
		}

		InnerClassesAttribute innerClassesAttribute = classFileReader.getInnerClassesAttribute();
		ClassFileAttribute runtimeVisibleAnnotationsAttribute = Utility.getAttribute(classFileReader, AttributeNamesConstants.RUNTIME_VISIBLE_ANNOTATIONS);
		ClassFileAttribute runtimeInvisibleAnnotationsAttribute = Utility.getAttribute(classFileReader, AttributeNamesConstants.RUNTIME_INVISIBLE_ANNOTATIONS);

		if (checkMode(mode, DETAILED)) {
			// disassemble compact version of annotations
			if (runtimeInvisibleAnnotationsAttribute != null) {
				disassembleAsModifier((RuntimeInvisibleAnnotationsAttribute) runtimeInvisibleAnnotationsAttribute, buffer, lineSeparator, 0, mode);
				writeNewLine(buffer, lineSeparator, 0);
			}
			if (runtimeVisibleAnnotationsAttribute != null) {
				disassembleAsModifier((RuntimeVisibleAnnotationsAttribute) runtimeVisibleAnnotationsAttribute, buffer, lineSeparator, 0, mode);
				writeNewLine(buffer, lineSeparator, 0);
			}
		}
		boolean decoded = false;
		if (innerClassesAttribute != null) {
			// search the right entry
			InnerClassesAttributeEntry[] entries = innerClassesAttribute.getInnerClassAttributesEntries();
			for (int i = 0, max = entries.length; i < max; i++) {
				InnerClassesAttributeEntry entry = entries[i];
				char[] innerClassName = entry.getInnerClassName();
				if (innerClassName != null) {
					if (Arrays.equals(classFileReader.getClassName(), innerClassName)) {
						decodeModifiersForInnerClasses(buffer, entry.getAccessFlags(), false);
						decoded = true;
					}
				}
			}
		}
		if (!decoded) {
			decodeModifiersForType(buffer, accessFlags);
			if (isSynthetic(classFileReader)) {
				buffer.append("synthetic"); //$NON-NLS-1$
				buffer.append(Messages.disassembler_space);
			}
		}

		final boolean isAnnotation = (accessFlags & IModifierConstants.ACC_ANNOTATION) != 0;
		boolean isInterface = false;
		if (isEnum) {
			buffer.append("enum "); //$NON-NLS-1$
		} else if (classFileReader.isClass()) {
			buffer.append("class "); //$NON-NLS-1$
		} else {
			if (isAnnotation) {
				buffer.append("@"); //$NON-NLS-1$
			}
			buffer.append("interface "); //$NON-NLS-1$
			isInterface = true;
		}

		buffer.append(className);

		char[] superclassName = classFileReader.getSuperclassName();
		if (superclassName != null) {
			CharOperation.replace(superclassName, '/', '.');
			if (!isJavaLangObject(superclassName) && !isEnum) {
				buffer.append(" extends "); //$NON-NLS-1$
				buffer.append(returnClassName(superclassName, '.', mode));
			}
		}
		if (!isAnnotation) {
			char[][] superclassInterfaces = classFileReader.getInterfaceNames();
			int length = superclassInterfaces.length;
			if (length != 0) {
				if (isInterface) {
					buffer.append(" extends "); //$NON-NLS-1$
				} else {
					buffer.append(" implements "); //$NON-NLS-1$
				}
				for (int i = 0; i < length; i++) {
					if (i != 0) {
						buffer.append(Messages.disassembler_comma).append(Messages.disassembler_space);
					}
					char[] superinterface = superclassInterfaces[i];
					CharOperation.replace(superinterface, '/', '.');
					buffer.append(returnClassName(superinterface, '.', mode));
				}
			}
		}
		buffer.append(Messages.disassembler_opentypedeclaration);
		disassembleTypeMembers(classFileReader, className, buffer, lineSeparator, 1, mode, isEnum);
		if (checkMode(mode, DETAILED)) {
			ClassFileAttribute[] attributes = classFileReader.getAttributes();
			int length = attributes.length;
			EnclosingMethodAttribute enclosingMethodAttribute = getEnclosingMethodAttribute(classFileReader);
			int remainingAttributesLength = length;
			if (innerClassesAttribute != null) {
				remainingAttributesLength--;
			}
			if (enclosingMethodAttribute != null) {
				remainingAttributesLength--;
			}
			if (sourceAttribute != null) {
				remainingAttributesLength--;
			}
			if (signatureAttribute != null) {
				remainingAttributesLength--;
			}
			if (innerClassesAttribute != null || enclosingMethodAttribute != null || remainingAttributesLength != 0) {
				writeNewLine(buffer, lineSeparator, 0);
			}
			if (innerClassesAttribute != null) {
				disassemble(innerClassesAttribute, buffer, lineSeparator, 1);
			}
			if (enclosingMethodAttribute != null) {
				disassemble(enclosingMethodAttribute, buffer, lineSeparator, 0);
			}
			if (runtimeVisibleAnnotationsAttribute != null) {
				disassemble((RuntimeVisibleAnnotationsAttribute) runtimeVisibleAnnotationsAttribute, buffer, lineSeparator, 0, mode);
			}
			if (runtimeInvisibleAnnotationsAttribute != null) {
				disassemble((RuntimeInvisibleAnnotationsAttribute) runtimeInvisibleAnnotationsAttribute, buffer, lineSeparator, 0, mode);
			}
		}
		writeNewLine(buffer, lineSeparator, 0);
		buffer.append(Messages.disassembler_closetypedeclaration);
		return buffer.toString();
	}

	private boolean isJavaLangObject(final char[] className) {
		return Arrays.equals(TypeConstants.JAVA_LANG_OBJECT, CharOperation.splitOn('.', className));
	}

	private boolean isVarArgs(MethodInfo methodInfo) {
		int accessFlags = methodInfo.getAccessFlags();
		if ((accessFlags & IModifierConstants.ACC_VARARGS) != 0)
			return true;
		// check the presence of the unspecified Varargs attribute
		return Utility.getAttribute(methodInfo, AttributeNamesConstants.VAR_ARGS) != null;
	}

	private void disassemble(CodeAttribute codeAttribute, char[] methodDescriptor, boolean isStatic, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber - 1);
		DefaultBytecodeVisitor visitor = new DefaultBytecodeVisitor(codeAttribute, methodDescriptor, isStatic, buffer, lineSeparator, tabNumber, mode);
		try {
			codeAttribute.traverse(visitor);
		} catch (ClassFormatException e) {
			dumpTab(tabNumber + 2, buffer);
			buffer.append(Messages.classformat_classformatexception);
			writeNewLine(buffer, lineSeparator, tabNumber + 1);
		}
		final int exceptionTableLength = codeAttribute.getExceptionTableLength();
		if (exceptionTableLength != 0) {
			final int tabNumberForExceptionAttribute = tabNumber + 2;
			dumpTab(tabNumberForExceptionAttribute, buffer);
			final ExceptionTableEntry[] exceptionTableEntries = codeAttribute.getExceptionTable();
			buffer.append(Messages.disassembler_exceptiontableheader);
			writeNewLine(buffer, lineSeparator, tabNumberForExceptionAttribute + 1);
			for (int i = 0; i < exceptionTableLength; i++) {
				if (i != 0) {
					writeNewLine(buffer, lineSeparator, tabNumberForExceptionAttribute + 1);
				}
				ExceptionTableEntry exceptionTableEntry = exceptionTableEntries[i];
				char[] catchType;
				if (exceptionTableEntry.getCatchTypeIndex() != 0) {
					catchType = exceptionTableEntry.getCatchType();
					CharOperation.replace(catchType, '/', '.');
					catchType = returnClassName(catchType, '.', mode);
				} else {
					catchType = ANY_EXCEPTION;
				}
				buffer.append(NLS.bind(Messages.classfileformat_exceptiontableentry, new String[] {Integer.toString(exceptionTableEntry.getStartPC()), Integer.toString(exceptionTableEntry.getEndPC()), Integer.toString(exceptionTableEntry.getHandlerPC()), new String(catchType),}));
			}
		}
	}

	private void disassemble(EnclosingMethodAttribute enclosingMethodAttribute, StringBuffer buffer, String lineSeparator, int tabNumber) {
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		buffer.append(Messages.disassembler_enclosingmethodheader);
		buffer.append(" ")//$NON-NLS-1$
				.append(enclosingMethodAttribute.getEnclosingClass());
		if (enclosingMethodAttribute.getMethodNameAndTypeIndex() != 0) {
			buffer.append(".")//$NON-NLS-1$
					.append(enclosingMethodAttribute.getMethodName()).append(enclosingMethodAttribute.getMethodDescriptor());
		}
	}

	/**
	 * Disassemble a field info
	 */
	private void disassemble(FieldInfo fieldInfo, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber);
		final char[] fieldDescriptor = fieldInfo.getDescriptor();
		final SignatureAttribute signatureAttribute = (SignatureAttribute) Utility.getAttribute(fieldInfo, AttributeNamesConstants.SIGNATURE);
		if (checkMode(mode, DETAILED)) {
			buffer.append(NLS.bind(Messages.classfileformat_fieldddescriptor, new String[] {new String(fieldDescriptor)}));
			if (fieldInfo.isDeprecated()) {
				buffer.append(Messages.disassembler_deprecated);
			}
			writeNewLine(buffer, lineSeparator, tabNumber);
			if (signatureAttribute != null) {
				buffer.append(NLS.bind(Messages.disassembler_signatureattributeheader, new String(signatureAttribute.getSignature())));
				writeNewLine(buffer, lineSeparator, tabNumber);
			}
		}
		final ClassFileAttribute runtimeVisibleAnnotationsAttribute = Utility.getAttribute(fieldInfo, AttributeNamesConstants.RUNTIME_VISIBLE_ANNOTATIONS);
		final ClassFileAttribute runtimeInvisibleAnnotationsAttribute = Utility.getAttribute(fieldInfo, AttributeNamesConstants.RUNTIME_INVISIBLE_ANNOTATIONS);
		if (checkMode(mode, DETAILED)) {
			// disassemble compact version of annotations
			if (runtimeInvisibleAnnotationsAttribute != null) {
				disassembleAsModifier((RuntimeInvisibleAnnotationsAttribute) runtimeInvisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
				writeNewLine(buffer, lineSeparator, tabNumber);
			}
			if (runtimeVisibleAnnotationsAttribute != null) {
				disassembleAsModifier((RuntimeVisibleAnnotationsAttribute) runtimeVisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
				writeNewLine(buffer, lineSeparator, tabNumber);
			}
		}
		decodeModifiersForField(buffer, fieldInfo.getAccessFlags());
		if (fieldInfo.isSynthetic()) {
			buffer.append("synthetic"); //$NON-NLS-1$
			buffer.append(Messages.disassembler_space);
		}
		buffer.append(returnClassName(getSignatureForField(fieldDescriptor), '.', mode));
		buffer.append(' ');
		buffer.append(new String(fieldInfo.getName()));
		ConstantValueAttribute constantValueAttribute = fieldInfo.getConstantValueAttribute();
		if (constantValueAttribute != null) {
			buffer.append(Messages.disassembler_fieldhasconstant);
			ConstantPoolEntry constantPoolEntry = constantValueAttribute.getConstantValue();
			switch (constantPoolEntry.getKind()) {
				case ConstantPoolConstant.CONSTANT_Long :
					buffer.append(constantPoolEntry.getLongValue() + "L"); //$NON-NLS-1$
					break;
				case ConstantPoolConstant.CONSTANT_Float :
					buffer.append(constantPoolEntry.getFloatValue() + "f"); //$NON-NLS-1$
					break;
				case ConstantPoolConstant.CONSTANT_Double :
					buffer.append(constantPoolEntry.getDoubleValue());
					break;
				case ConstantPoolConstant.CONSTANT_Integer :
					switch (fieldDescriptor[0]) {
						case 'C' :
							buffer.append("'" + (char) constantPoolEntry.getIntegerValue() + "'"); //$NON-NLS-1$//$NON-NLS-2$
							break;
						case 'Z' :
							buffer.append(constantPoolEntry.getIntegerValue() == 1 ? "true" : "false");//$NON-NLS-1$//$NON-NLS-2$
							break;
						case 'B' :
							buffer.append(constantPoolEntry.getIntegerValue());
							break;
						case 'S' :
							buffer.append(constantPoolEntry.getIntegerValue());
							break;
						case 'I' :
							buffer.append(constantPoolEntry.getIntegerValue());
					}
					break;
				case ConstantPoolConstant.CONSTANT_String :
					buffer.append("\"" + decodeStringValue(constantPoolEntry.getStringValue()) + "\"");//$NON-NLS-1$//$NON-NLS-2$
			}
		}
		buffer.append(Messages.disassembler_endoffieldheader);
		if (checkMode(mode, DETAILED)) {
			if (runtimeVisibleAnnotationsAttribute != null) {
				disassemble((RuntimeVisibleAnnotationsAttribute) runtimeVisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
			}
			if (runtimeInvisibleAnnotationsAttribute != null) {
				disassemble((RuntimeInvisibleAnnotationsAttribute) runtimeInvisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
			}
		}
	}

	private void disassemble(InnerClassesAttribute innerClassesAttribute, StringBuffer buffer, String lineSeparator, int tabNumber) {
		writeNewLine(buffer, lineSeparator, tabNumber);
		buffer.append(Messages.disassembler_innerattributesheader);
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		InnerClassesAttributeEntry[] innerClassesAttributeEntries = innerClassesAttribute.getInnerClassAttributesEntries();
		int length = innerClassesAttributeEntries.length;
		int innerClassNameIndex, outerClassNameIndex, innerNameIndex, accessFlags;
		InnerClassesAttributeEntry innerClassesAttributeEntry;
		if (length > 1) {
			final char[] EMPTY_CHAR_ARRAY = Utility.EMPTY_STRING.toCharArray();
			Arrays.sort(innerClassesAttributeEntries, new Comparator<InnerClassesAttributeEntry>() {
				public int compare(InnerClassesAttributeEntry o1, InnerClassesAttributeEntry o2) {
					final char[] innerClassName1 = o1.getInnerClassName();
					final char[] innerClassName2 = o2.getInnerClassName();
					final char[] innerName1 = o1.getInnerName();
					final char[] innerName2 = o2.getInnerName();
					final char[] outerClassName1 = o1.getOuterClassName();
					final char[] outerClassName2 = o2.getOuterClassName();
					StringBuffer buffer1 = new StringBuffer();
					buffer1.append(innerClassName1 == null ? EMPTY_CHAR_ARRAY : innerClassName1);
					buffer1.append(innerName1 == null ? EMPTY_CHAR_ARRAY : innerName1);
					buffer1.append(outerClassName1 == null ? EMPTY_CHAR_ARRAY : outerClassName1);
					StringBuffer buffer2 = new StringBuffer();
					buffer2.append(innerClassName2 == null ? EMPTY_CHAR_ARRAY : innerClassName2);
					buffer2.append(innerName2 == null ? EMPTY_CHAR_ARRAY : innerName2);
					buffer2.append(outerClassName2 == null ? EMPTY_CHAR_ARRAY : outerClassName2);
					return buffer1.toString().compareTo(buffer2.toString());
				}
			});
		}
		for (int i = 0; i < length; i++) {
			if (i != 0) {
				buffer.append(Messages.disassembler_comma);
				writeNewLine(buffer, lineSeparator, tabNumber + 1);
			}
			innerClassesAttributeEntry = innerClassesAttributeEntries[i];
			innerClassNameIndex = innerClassesAttributeEntry.getInnerClassNameIndex();
			outerClassNameIndex = innerClassesAttributeEntry.getOuterClassNameIndex();
			innerNameIndex = innerClassesAttributeEntry.getInnerNameIndex();
			accessFlags = innerClassesAttributeEntry.getAccessFlags();
			buffer.append(Messages.disassembler_openinnerclassentry).append(Messages.disassembler_inner_class_info_name);
			if (innerClassNameIndex != 0) {
				buffer.append(Messages.disassembler_space).append(innerClassesAttributeEntry.getInnerClassName());
			}
			buffer.append(Messages.disassembler_comma).append(Messages.disassembler_space).append(Messages.disassembler_outer_class_info_name);
			if (outerClassNameIndex != 0) {
				buffer.append(Messages.disassembler_space).append(innerClassesAttributeEntry.getOuterClassName());
			}
			writeNewLine(buffer, lineSeparator, tabNumber);
			dumpTab(tabNumber, buffer);
			buffer.append(Messages.disassembler_space);
			buffer.append(Messages.disassembler_inner_name);
			if (innerNameIndex != 0) {
				buffer.append(Messages.disassembler_space).append(innerClassesAttributeEntry.getInnerName());
			}
			buffer.append(Messages.disassembler_comma).append(Messages.disassembler_space).append(Messages.disassembler_inner_accessflags).append(accessFlags).append(Messages.disassembler_space);
			decodeModifiersForInnerClasses(buffer, accessFlags, true);
			buffer.append(Messages.disassembler_closeinnerclassentry);
		}
	}

	private void disassemble(int index, ParameterAnnotation parameterAnnotation, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		Annotation[] annotations = parameterAnnotation.getAnnotations();
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		buffer.append(NLS.bind(Messages.disassembler_parameterannotationentrystart, new String[] {Integer.toString(index), Integer.toString(annotations.length)}));
		for (int i = 0, max = annotations.length; i < max; i++) {
			disassemble(annotations[i], buffer, lineSeparator, tabNumber + 1, mode);
		}
	}

	private void disassemble(RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		buffer.append(Messages.disassembler_runtimeinvisibleannotationsattributeheader);
		Annotation[] annotations = runtimeInvisibleAnnotationsAttribute.getAnnotations();
		for (int i = 0, max = annotations.length; i < max; i++) {
			disassemble(annotations[i], buffer, lineSeparator, tabNumber + 1, mode);
		}
	}

	private void disassemble(RuntimeInvisibleParameterAnnotationsAttribute runtimeInvisibleParameterAnnotationsAttribute, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		buffer.append(Messages.disassembler_runtimeinvisibleparameterannotationsattributeheader);
		ParameterAnnotation[] parameterAnnotations = runtimeInvisibleParameterAnnotationsAttribute.getParameterAnnotations();
		for (int i = 0, max = parameterAnnotations.length; i < max; i++) {
			disassemble(i, parameterAnnotations[i], buffer, lineSeparator, tabNumber + 1, mode);
		}
	}

	private void disassemble(RuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotationsAttribute, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		buffer.append(Messages.disassembler_runtimevisibleannotationsattributeheader);
		Annotation[] annotations = runtimeVisibleAnnotationsAttribute.getAnnotations();
		for (int i = 0, max = annotations.length; i < max; i++) {
			disassemble(annotations[i], buffer, lineSeparator, tabNumber + 1, mode);
		}
	}

	private void disassemble(RuntimeVisibleParameterAnnotationsAttribute runtimeVisibleParameterAnnotationsAttribute, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		buffer.append(Messages.disassembler_runtimevisibleparameterannotationsattributeheader);
		ParameterAnnotation[] parameterAnnotations = runtimeVisibleParameterAnnotationsAttribute.getParameterAnnotations();
		for (int i = 0, max = parameterAnnotations.length; i < max; i++) {
			disassemble(i, parameterAnnotations[i], buffer, lineSeparator, tabNumber + 1, mode);
		}
	}

	private void disassembleAsModifier(Annotation annotation, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		final char[] typeName = CharOperation.replaceOnCopy(annotation.getTypeName(), '/', '.');
		buffer.append('@').append(returnClassName(Signature.toCharArray(typeName), '.', mode));
		final AnnotationComponent[] components = annotation.getComponents();
		final int length = components.length;
		if (length != 0) {
			buffer.append('(');
			for (int i = 0; i < length; i++) {
				if (i > 0) {
					buffer.append(',');
					writeNewLine(buffer, lineSeparator, tabNumber);
				}
				disassembleAsModifier(components[i], buffer, lineSeparator, tabNumber + 1, mode);
			}
			buffer.append(')');
		}
	}

	private void disassembleAsModifier(AnnotationComponent annotationComponent, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		buffer.append(annotationComponent.getComponentName()).append('=');
		disassembleAsModifier(annotationComponent.getComponentValue(), buffer, lineSeparator, tabNumber + 1, mode);
	}

	private void disassembleAsModifier(AnnotationComponentValue annotationComponentValue, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		switch (annotationComponentValue.getTag()) {
			case AnnotationComponentValue.BYTE_TAG :
			case AnnotationComponentValue.CHAR_TAG :
			case AnnotationComponentValue.DOUBLE_TAG :
			case AnnotationComponentValue.FLOAT_TAG :
			case AnnotationComponentValue.INTEGER_TAG :
			case AnnotationComponentValue.LONG_TAG :
			case AnnotationComponentValue.SHORT_TAG :
			case AnnotationComponentValue.BOOLEAN_TAG :
			case AnnotationComponentValue.STRING_TAG :
				ConstantPoolEntry constantPoolEntry = annotationComponentValue.getConstantValue();
				String value = null;
				switch (constantPoolEntry.getKind()) {
					case ConstantPoolConstant.CONSTANT_Long :
						value = constantPoolEntry.getLongValue() + "L"; //$NON-NLS-1$
						break;
					case ConstantPoolConstant.CONSTANT_Float :
						value = constantPoolEntry.getFloatValue() + "f"; //$NON-NLS-1$
						break;
					case ConstantPoolConstant.CONSTANT_Double :
						value = Double.toString(constantPoolEntry.getDoubleValue());
						break;
					case ConstantPoolConstant.CONSTANT_Integer :
						switch (annotationComponentValue.getTag()) {
							case AnnotationComponentValue.CHAR_TAG :
								value = "'" + (char) constantPoolEntry.getIntegerValue() + "'"; //$NON-NLS-1$//$NON-NLS-2$
								break;
							case AnnotationComponentValue.BOOLEAN_TAG :
								value = constantPoolEntry.getIntegerValue() == 1 ? "true" : "false";//$NON-NLS-1$//$NON-NLS-2$
								break;
							case AnnotationComponentValue.BYTE_TAG :
								value = "(byte) " + constantPoolEntry.getIntegerValue(); //$NON-NLS-1$
								break;
							case AnnotationComponentValue.SHORT_TAG :
								value = "(short) " + constantPoolEntry.getIntegerValue(); //$NON-NLS-1$
								break;
							case AnnotationComponentValue.INTEGER_TAG :
								value = "(int) " + constantPoolEntry.getIntegerValue(); //$NON-NLS-1$
						}
						break;
					case ConstantPoolConstant.CONSTANT_Utf8 :
						value = "\"" + decodeStringValue(constantPoolEntry.getUtf8Value()) + "\"";//$NON-NLS-1$//$NON-NLS-2$
				}
				buffer.append(value);
				break;
			case AnnotationComponentValue.ENUM_TAG :
				final char[] typeName = CharOperation.replaceOnCopy(annotationComponentValue.getEnumConstantTypeName(), '/', '.');
				final char[] constantName = annotationComponentValue.getEnumConstantName();
				buffer.append(returnClassName(Signature.toCharArray(typeName), '.', mode)).append('.').append(constantName);
				break;
			case AnnotationComponentValue.CLASS_TAG :
				constantPoolEntry = annotationComponentValue.getClassInfo();
				final char[] className = CharOperation.replaceOnCopy(constantPoolEntry.getUtf8Value(), '/', '.');
				buffer.append(returnClassName(Signature.toCharArray(className), '.', mode));
				break;
			case AnnotationComponentValue.ANNOTATION_TAG :
				Annotation annotation = annotationComponentValue.getAnnotationValue();
				disassembleAsModifier(annotation, buffer, lineSeparator, tabNumber + 1, mode);
				break;
			case AnnotationComponentValue.ARRAY_TAG :
				final AnnotationComponentValue[] annotationComponentValues = annotationComponentValue.getAnnotationComponentValues();
				buffer.append('{');
				for (int i = 0, max = annotationComponentValues.length; i < max; i++) {
					if (i > 0) {
						buffer.append(',');
					}
					disassembleAsModifier(annotationComponentValues[i], buffer, lineSeparator, tabNumber + 1, mode);
				}
				buffer.append('}');
		}
	}

	private void disassembleAsModifier(AnnotationDefaultAttribute annotationDefaultAttribute, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		AnnotationComponentValue componentValue = annotationDefaultAttribute.getMemberValue();
		disassembleAsModifier(componentValue, buffer, lineSeparator, tabNumber + 1, mode);
	}

	private void disassembleAsModifier(RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		Annotation[] annotations = runtimeInvisibleAnnotationsAttribute.getAnnotations();
		for (int i = 0, max = annotations.length; i < max; i++) {
			disassembleAsModifier(annotations[i], buffer, lineSeparator, tabNumber + 1, mode);
		}
	}

	private void disassembleAsModifier(RuntimeInvisibleParameterAnnotationsAttribute runtimeInvisibleParameterAnnotationsAttribute, StringBuffer buffer, int index, String lineSeparator, int tabNumber, int mode) {
		ParameterAnnotation[] parameterAnnotations = runtimeInvisibleParameterAnnotationsAttribute.getParameterAnnotations();
		if (parameterAnnotations.length > index) {
			disassembleAsModifier(parameterAnnotations[index], buffer, lineSeparator, tabNumber + 1, mode);
		}
	}

	private void disassembleAsModifier(RuntimeVisibleParameterAnnotationsAttribute runtimeVisibleParameterAnnotationsAttribute, StringBuffer buffer, int index, String lineSeparator, int tabNumber, int mode) {
		ParameterAnnotation[] parameterAnnotations = runtimeVisibleParameterAnnotationsAttribute.getParameterAnnotations();
		if (parameterAnnotations.length > index) {
			disassembleAsModifier(parameterAnnotations[index], buffer, lineSeparator, tabNumber + 1, mode);
		}
	}

	private void disassembleAsModifier(ParameterAnnotation parameterAnnotation, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		Annotation[] annotations = parameterAnnotation.getAnnotations();
		for (int i = 0, max = annotations.length; i < max; i++) {
			if (i > 0) {
				buffer.append(' ');
			}
			disassembleAsModifier(annotations[i], buffer, lineSeparator, tabNumber + 1, mode);
		}
	}

	private void disassembleAsModifier(RuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotationsAttribute, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		Annotation[] annotations = runtimeVisibleAnnotationsAttribute.getAnnotations();
		for (int i = 0, max = annotations.length; i < max; i++) {
			if (i > 0) {
				writeNewLine(buffer, lineSeparator, tabNumber);
			}
			disassembleAsModifier(annotations[i], buffer, lineSeparator, tabNumber + 1, mode);
		}
	}

	private void disassembleTypeMembers(ClassFileReader classFileReader, char[] className, StringBuffer buffer, String lineSeparator, int tabNumber, int mode, boolean isEnum) {
		FieldInfo[] fields = classFileReader.getFieldInfos();
		// sort fields
		Arrays.sort(fields, new Comparator<FieldInfo>() {
			public int compare(FieldInfo fieldInfo1, FieldInfo fieldInfo2) {
				int compare = new String(fieldInfo1.getName()).compareTo(new String(fieldInfo2.getName()));
				if (compare == 0) {
					return new String(fieldInfo1.getDescriptor()).compareTo(new String(fieldInfo2.getDescriptor()));
				}
				return compare;
			}
		});
		for (int i = 0, max = fields.length; i < max; i++) {
			writeNewLine(buffer, lineSeparator, tabNumber);
			disassemble(fields[i], buffer, lineSeparator, tabNumber, mode);
		}
		MethodInfo[] methods = classFileReader.getMethodInfos();
		// sort methods
		Arrays.sort(methods, new Comparator<MethodInfo>() {
			public int compare(MethodInfo methodInfo1, MethodInfo methodInfo2) {
				int compare = new String(methodInfo1.getName()).compareTo(new String(methodInfo2.getName()));
				if (compare == 0) {
					return new String(methodInfo1.getDescriptor()).compareTo(new String(methodInfo2.getDescriptor()));
				}
				return compare;
			}
		});
		for (int i = 0, max = methods.length; i < max; i++) {
			writeNewLine(buffer, lineSeparator, tabNumber);
			disassemble(classFileReader, className, methods[i], buffer, lineSeparator, tabNumber, mode);
		}
	}

	private final void dumpTab(int tabNumber, StringBuffer buffer) {
		for (int i = 0; i < tabNumber; i++) {
			buffer.append(Messages.disassembler_indentation);
		}
	}

	private EnclosingMethodAttribute getEnclosingMethodAttribute(ClassFileReader classFileReader) {
		ClassFileAttribute[] attributes = classFileReader.getAttributes();
		for (int i = 0, max = attributes.length; i < max; i++) {
			if (Arrays.equals(attributes[i].getAttributeName(), AttributeNamesConstants.ENCLOSING_METHOD)) {
				return (EnclosingMethodAttribute) attributes[i];
			}
		}
		return null;
	}

	private char[] getSignatureForField(char[] fieldDescriptor) {
		char[] newFieldDescriptor = CharOperation.replaceOnCopy(fieldDescriptor, '/', '.');
		newFieldDescriptor = CharOperation.replaceOnCopy(newFieldDescriptor, '$', '%');
		char[] fieldDescriptorSignature = Signature.toCharArray(newFieldDescriptor);
		CharOperation.replace(fieldDescriptorSignature, '%', '$');
		return fieldDescriptorSignature;
	}

	private boolean isDeprecated(ClassFileReader classFileReader) {
		ClassFileAttribute[] attributes = classFileReader.getAttributes();
		for (int i = 0, max = attributes.length; i < max; i++) {
			if (Arrays.equals(attributes[i].getAttributeName(), AttributeNamesConstants.DEPRECATED)) {
				return true;
			}
		}
		return false;
	}

	private boolean isSynthetic(ClassFileReader classFileReader) {
		int flags = classFileReader.getAccessFlags();
		if ((flags & IModifierConstants.ACC_SYNTHETIC) != 0) {
			return true;
		}
		ClassFileAttribute[] attributes = classFileReader.getAttributes();
		for (int i = 0, max = attributes.length; i < max; i++) {
			if (Arrays.equals(attributes[i].getAttributeName(), AttributeNamesConstants.SYNTHETIC)) {
				return true;
			}
		}
		return false;
	}

	private boolean checkMode(int mode, int flag) {
		return (mode & flag) != 0;
	}

	private boolean isCompact(int mode) {
		return (mode & Disassembler.COMPACT) != 0;
	}

	private char[] returnClassName(char[] classInfoName, char separator, int mode) {
		if (classInfoName.length == 0) {
			return CharOperation.NO_CHAR;
		} else if (isCompact(mode)) {
			int lastIndexOfSlash = CharOperation.lastIndexOf(separator, classInfoName);
			if (lastIndexOfSlash != -1) {
				return CharOperation.subarray(classInfoName, lastIndexOfSlash + 1, classInfoName.length);
			}
		}
		return classInfoName;
	}

	private void writeNewLine(StringBuffer buffer, String lineSeparator, int tabNumber) {
		buffer.append(lineSeparator);
		dumpTab(tabNumber, buffer);
	}
}
