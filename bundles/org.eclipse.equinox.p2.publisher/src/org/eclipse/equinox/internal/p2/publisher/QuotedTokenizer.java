/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.publisher;

import java.io.*;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * Tokenzier which supports quoting using '"'
 * The resulting tokens will not contain the quote character '"' unless it was escaped '\"'
 */
public class QuotedTokenizer implements Enumeration<String> {
	private StreamTokenizer tokenizer = null;

	/**
	 * Default delimiter is whitespace characters
	 * @param str - String to be tokenized
	 */
	public QuotedTokenizer(String str) {
		this(str, null);
	}

	/**
	 * Tokenize based on the given delimiters.  The quote character '"' can not be
	 * used as a delimiter.
	 * @param str - String to be tokenized
	 * @param delim - delimiter characters
	 * @throws IllegalArgumentException if delim contains the quote character '"'
	 */
	public QuotedTokenizer(String str, String delim) {
		if (delim != null && delim.indexOf('"') > -1)
			throw new IllegalArgumentException();

		StringReader reader = new StringReader(str);
		tokenizer = new StreamTokenizer(reader);

		tokenizer.resetSyntax();
		if (delim == null)
			tokenizer.ordinaryChars(0, 0x20);
		else
			tokenizer.wordChars(0, 0x20);
		tokenizer.wordChars(0x21, 0xFF); //characters > 0xFF are also word chars
		tokenizer.quoteChar('"');

		if (delim != null) {
			for (int i = 0; i < delim.length(); i++) {
				tokenizer.ordinaryChar(delim.charAt(i));
			}
		}
	}

	/**
	 * Test to see if more tokens are available
	 * @return true if there is another token
	 */
	public boolean hasMoreTokens() {
		return (token(null) != StreamTokenizer.TT_EOF);
	}

	/**
	 * Return the next token,
	 * @return the next token
	 * @throws NoSuchElementException if there are no more tokens
	 */
	public String nextToken() {
		StringBuffer buffer = new StringBuffer(10);
		int tokenType = token(buffer);

		if (tokenType == StreamTokenizer.TT_EOF)
			throw new NoSuchElementException();

		return buffer.toString();
	}

	/**
	 * Get the next token, or check that there is a next token
	 * @param buffer to hold the token, or null if we just want to know if there is one 
	 */
	private int token(StringBuffer buffer) {
		int tokenType = 0;
		int next = 0;

		get_token: while (true) {
			try {
				tokenType = tokenizer.nextToken();
			} catch (IOException e) {
				tokenType = StreamTokenizer.TT_EOF;
			}
			switch (tokenType) {
				case StreamTokenizer.TT_WORD :
				case '"' :
					if (buffer == null) {
						//we just wanted to know if there was something coming
						tokenizer.pushBack();
						return tokenType;
					}
					buffer.append(tokenizer.sval);

					// peek at the next token, 
					try {
						next = tokenizer.nextToken();
						tokenizer.pushBack();
					} catch (IOException e) {
						next = StreamTokenizer.TT_EOF;
					}

					//if the next token is a quote, it is still this token, otherwise we are done
					if (next == '"')
						continue;
					break get_token;
				case StreamTokenizer.TT_EOF :
					break get_token;
				default :
					//ordinary char from delim, if we have something we are done, otherwise keep looking for a token
					if (buffer != null && buffer.length() > 0)
						break get_token;
					continue;
			}
		}
		return tokenType;
	}

	public boolean hasMoreElements() {
		return hasMoreTokens();
	}

	public String nextElement() {
		return nextToken();
	}
}
