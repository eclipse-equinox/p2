/*******************************************************************************
 * Copyright (c) 2009-2011 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *     SAP - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import org.eclipse.osgi.util.NLS;

/**
 * TODO Shouldn't be a public class in an API package.
 */
public class Messages extends NLS {

	public static String _0_is_not_a_valid_qualifier_in_osgi_1;

	public static String array_can_not_be_empty;

	public static String array_can_not_have_character_group;

	public static String array_can_not_have_enum;

	public static String auto_can_not_have_pad_value;

	public static String bad_enum_definition;

	public static String cannot_combine_ignore_with_other_instruction;

	public static String cannot_combine_range_upper_bound_with_pad_value;

	public static String character_group_defined_more_then_once;

	public static String colon_expected_before_original_version_0;

	public static String default_defined_more_then_once;

	public static String delimiter_can_not_be_ignored;

	public static String delimiter_can_not_have_default_value;

	public static String delimiter_can_not_have_pad_value;

	public static String delimiter_can_not_have_range;

	public static String EOS_after_escape;

	public static String enum_defined_more_then_once;

	public static String expected_orignal_after_colon_0;

	public static String expected_orignal_after_slash_0;

	public static String expected_slash_after_raw_vector_0;

	public static String filter_trailing_characters;

	public static String filter_missing_leftparen;

	public static String filter_missing_rightparen;

	public static String filter_invalid_operator;

	public static String filter_missing_attr;

	public static String filter_invalid_value;

	public static String filter_missing_value;

	public static String filter_premature_end;

	public static String format_0_unable_to_parse_1;

	public static String format_0_unable_to_parse_empty_version;

	public static String format_is_empty;

	public static String format_must_be_delimited_by_colon_0;

	public static String group_can_not_be_empty;

	public static String ignore_defined_more_then_once;

	public static String illegal_character_encountered_ascii_0;

	public static String missing_comma_in_range_0;

	public static String negative_character_range;

	public static String neither_raw_vector_nor_format_specified_0;

	public static String number_can_not_have_pad_value;

	public static String only_format_specified_0;

	public static String only_max_and_empty_string_defaults_can_have_translations;

	public static String original_must_start_with_colon_0;

	public static String original_stated_but_missing_0;

	public static String pad_defined_more_then_once;

	public static String performing_subquery;

	public static String premature_end_of_format;

	public static String premature_end_of_format_expected_0;

	public static String premature_EOS_0;

	public static String range_boundaries_0_and_1_cannot_have_different_formats;

	public static String range_defined_more_then_once;

	public static String range_max_cannot_be_less_then_range_min;

	public static String range_max_cannot_be_zero;

	public static String range_min_0_is_not_less_then_range_max_1;

	public static String raw_and_original_must_use_same_range_inclusion_0;

	public static String raw_element_can_not_have_pad_value;

	public static String raw_element_expected_0;

	public static String string_can_not_have_pad_value;

	public static String syntax_error_in_version_format_0_1_2;

	public static String syntax_error_in_version_format_0_1_found_2_expected_3;

	public static String unbalanced_format_parenthesis;

	public static String no_expression_factory;

	public static String no_expression_parser;

	public static String provided_capability_name_not_defined;

	public static String provided_capability_namespace_not_defined;

	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.metadata.messages"; //$NON-NLS-1$

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Prevent instance creation
	}
}
