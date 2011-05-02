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

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.p2.internal.repository.comparator.java.messages"; //$NON-NLS-1$
	public static String differentNumberOfEntries;
	public static String binaryDifferentLength;
	public static String classesDifferent;
	public static String propertiesSizesDifferent;
	public static String differentPropertyValueFull;
	public static String missingProperty;
	public static String manifestDifferentSize;
	public static String manifestMissingEntry;
	public static String manifestDifferentValue;
	public static String binaryFilesDifferent;
	public static String differentEntry;
	public static String missingEntry;
	public static String ioexception;

	public static String featureSize;
	public static String featureIdsDontMatch;
	public static String featureVersionsDontMatch;
	public static String featureEntry;
	public static String featureEntryOptional;
	public static String featureEntryUnpack;
	public static String featureEntryMatch;
	public static String featureEntryFilter;

	public static String disassembler_opentypedeclaration;
	public static String disassembler_closetypedeclaration;
	public static String disassembler_endofmethodheader;
	public static String disassembler_begincommentline;
	public static String disassembler_fieldhasconstant;
	public static String disassembler_endoffieldheader;
	public static String disassembler_sourceattributeheader;
	public static String disassembler_enclosingmethodheader;
	public static String disassembler_exceptiontableheader;
	public static String disassembler_innerattributesheader;
	public static String disassembler_inner_class_info_name;
	public static String disassembler_outer_class_info_name;
	public static String disassembler_inner_name;
	public static String disassembler_inner_accessflags;
	public static String disassembler_signatureattributeheader;
	public static String disassembler_indentation;
	public static String disassembler_space;
	public static String disassembler_comma;
	public static String disassembler_openinnerclassentry;
	public static String disassembler_closeinnerclassentry;
	public static String disassembler_deprecated;
	public static String disassembler_annotationdefaultheader;
	public static String disassembler_annotationdefaultvalue;
	public static String disassembler_annotationenumvalue;
	public static String disassembler_annotationclassvalue;
	public static String disassembler_annotationannotationvalue;
	public static String disassembler_annotationarrayvaluestart;
	public static String disassembler_annotationarrayvalueend;
	public static String disassembler_annotationentrystart;
	public static String disassembler_annotationentryend;
	public static String disassembler_annotationcomponent;
	public static String disassembler_runtimevisibleannotationsattributeheader;
	public static String disassembler_runtimeinvisibleannotationsattributeheader;
	public static String disassembler_runtimevisibleparameterannotationsattributeheader;
	public static String disassembler_runtimeinvisibleparameterannotationsattributeheader;
	public static String disassembler_parameterannotationentrystart;
	public static String classfileformat_versiondetails;
	public static String classfileformat_methoddescriptor;
	public static String classfileformat_fieldddescriptor;
	public static String classfileformat_stacksAndLocals;
	public static String classfileformat_superflagisnotset;
	public static String classfileformat_superflagisset;
	public static String classfileformat_clinitname;
	public static String classformat_classformatexception;
	public static String classformat_anewarray;
	public static String classformat_checkcast;
	public static String classformat_instanceof;
	public static String classformat_ldc_w_class;
	public static String classformat_ldc_w_float;
	public static String classformat_ldc_w_integer;
	public static String classformat_ldc_w_string;
	public static String classformat_ldc2_w_long;
	public static String classformat_ldc2_w_double;
	public static String classformat_multianewarray;
	public static String classformat_new;
	public static String classformat_iinc;
	public static String classformat_invokespecial;
	public static String classformat_invokeinterface;
	public static String classformat_invokestatic;
	public static String classformat_invokevirtual;
	public static String classformat_getfield;
	public static String classformat_getstatic;
	public static String classformat_putstatic;
	public static String classformat_putfield;
	public static String classformat_newarray_boolean;
	public static String classformat_newarray_char;
	public static String classformat_newarray_float;
	public static String classformat_newarray_double;
	public static String classformat_newarray_byte;
	public static String classformat_newarray_short;
	public static String classformat_newarray_int;
	public static String classformat_newarray_long;
	public static String classformat_store;
	public static String classformat_load;
	public static String classfileformat_anyexceptionhandler;
	public static String classfileformat_exceptiontableentry;
	public static String classfileformat_versionUnknown;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// prevent instantiation
	}
}
