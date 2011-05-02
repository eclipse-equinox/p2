/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.expression;

public interface IExpressionConstants {
	String KEYWORD_ALL = "all"; //$NON-NLS-1$

	String KEYWORD_BOOLEAN = "boolean"; //$NON-NLS-1$
	String KEYWORD_CLASS = "class"; //$NON-NLS-1$
	String KEYWORD_EXISTS = "exists"; //$NON-NLS-1$
	String KEYWORD_FALSE = "false"; //$NON-NLS-1$
	String KEYWORD_FILTER = "filter"; //$NON-NLS-1$
	String KEYWORD_NULL = "null"; //$NON-NLS-1$
	String KEYWORD_RANGE = "range"; //$NON-NLS-1$
	String KEYWORD_TRUE = "true"; //$NON-NLS-1$
	String KEYWORD_COLLECT = "collect"; //$NON-NLS-1$
	String KEYWORD_FIRST = "first"; //$NON-NLS-1$
	String KEYWORD_FLATTEN = "flatten"; //$NON-NLS-1$
	String KEYWORD_INTERSECT = "intersect"; //$NON-NLS-1$
	String KEYWORD_IQUERY = "iquery"; //$NON-NLS-1$
	String KEYWORD_LATEST = "latest"; //$NON-NLS-1$
	String KEYWORD_LIMIT = "limit"; //$NON-NLS-1$
	String KEYWORD_LOCALIZED_KEYS = "localizedKeys"; //$NON-NLS-1$
	String KEYWORD_LOCALIZED_MAP = "localizedMap"; //$NON-NLS-1$
	String KEYWORD_LOCALIZED_PROPERTY = "localizedProperty"; //$NON-NLS-1$
	String KEYWORD_SATISFIES_ALL = "satisfiesAll"; //$NON-NLS-1$
	String KEYWORD_SATISFIES_ANY = "satisfiesAny"; //$NON-NLS-1$
	String KEYWORD_SELECT = "select"; //$NON-NLS-1$
	String KEYWORD_SET = "set"; //$NON-NLS-1$
	String KEYWORD_TRAVERSE = "traverse"; //$NON-NLS-1$
	String KEYWORD_UNION = "union"; //$NON-NLS-1$
	String KEYWORD_UNIQUE = "unique"; //$NON-NLS-1$
	String KEYWORD_VERSION = "version"; //$NON-NLS-1$

	String OPERATOR_AND = "&&"; //$NON-NLS-1$
	String OPERATOR_AT = "[]"; //$NON-NLS-1$
	String OPERATOR_EQUALS = "=="; //$NON-NLS-1$
	String OPERATOR_GT = ">"; //$NON-NLS-1$
	String OPERATOR_GT_EQUAL = ">="; //$NON-NLS-1$
	String OPERATOR_LT = "<"; //$NON-NLS-1$
	String OPERATOR_LT_EQUAL = "<="; //$NON-NLS-1$
	String OPERATOR_MATCHES = "~="; //$NON-NLS-1$
	String OPERATOR_MEMBER = "."; //$NON-NLS-1$
	String OPERATOR_NOT = "!"; //$NON-NLS-1$
	String OPERATOR_NOT_EQUALS = "!="; //$NON-NLS-1$
	String OPERATOR_OR = "||"; //$NON-NLS-1$
	String OPERATOR_PARAMETER = "$"; //$NON-NLS-1$
	String OPERATOR_ARRAY = "[]"; //$NON-NLS-1$
	String OPERATOR_ASSIGN = "="; //$NON-NLS-1$
	String OPERATOR_EACH = "_"; //$NON-NLS-1$
	String OPERATOR_ELSE = ":"; //$NON-NLS-1$
	String OPERATOR_IF = "?"; //$NON-NLS-1$

	int PRIORITY_LITERAL = 1;
	int PRIORITY_VARIABLE = 1;
	int PRIORITY_FUNCTION = 2; // for extend query expressions
	int PRIORITY_MEMBER = 3;
	int PRIORITY_COLLECTION = 4;
	int PRIORITY_NOT = 5;
	int PRIORITY_BINARY = 6;
	int PRIORITY_AND = 7;
	int PRIORITY_OR = 8;
	int PRIORITY_CONDITION = 9;
	int PRIORITY_ASSIGNMENT = 10;
	int PRIORITY_LAMBDA = 11;
	int PRIORITY_COMMA = 12;
	int PRIORITY_MAX = 20;

	String VARIABLE_EVERYTHING = "everything"; //$NON-NLS-1$
	String VARIABLE_THIS = "this"; //$NON-NLS-1$
}
