/* ========================================================================
 * PlantUML : a free UML diagram generator
 * ========================================================================
 *
 * (C) Copyright 2009-2017, Arnaud Roques
 *
 * Project Info:  http://plantuml.com
 * 
 * If you like this project or if you find it useful, you can support us at:
 * 
 * http://plantuml.com/patreon (only 1$ per month!)
 * http://plantuml.com/paypal
 * 
 * This file is part of PlantUML.
 *
 * Licensed under The MIT License (Massachusetts Institute of Technology License)
 * 
 * See http://opensource.org/licenses/MIT
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 *
 * Original Author:  Arnaud Roques
 */
package net.sourceforge.plantuml.code;

public class AsciiEncoder implements URLEncoder {

	// Temporary because of AsciiEncoderFinalZeros
	private final static char[] encode6bit = new char[64];
	private final static byte[] decode6bit = new byte[128];

	static {
		for (byte b = 0; b < 64; b++) {
			encode6bit[b] = encode6bit(b);
			decode6bit[encode6bit[b]] = b;
		}
	}

	public String encode(byte data[]) {
		if (data == null) {
			return "";
		}
		final StringBuilder result = new StringBuilder((data.length * 4 + 2) / 3);
		for (int i = 0; i < data.length; i += 3) {
			append3bytes(result, data[i] & 0xFF, i + 1 < data.length ? data[i + 1] & 0xFF : 0,
					i + 2 < data.length ? data[i + 2] & 0xFF : 0);
		}
		return result.toString();
	}

	public byte[] decode(String s) {
		// if (s.length() % 4 != 0) {
		// throw new IllegalArgumentException("Cannot decode " + s);
		// }
		final byte data[] = new byte[computeSize(s.length())];
		int pos = 0;
		for (int i = 0; i < s.length(); i += 4) {
			decode3bytes(data, pos, scharAt(s, i), scharAt(s, i + 1), scharAt(s, i + 2), scharAt(s, i + 3));
			pos += 3;
		}
		return data;
	}

	private int computeSize(int length) {
		// while (length % 4 != 0) {
		// length++;
		// }
		final int r = length % 4;
		if (r != 0) {
			length += 4 - r;
		}
		// System.err.println("length=" + length);
		// System.err.println("length1=" + (length % 4));
		// length += length % 4;
		// System.err.println("length2=" + length);
		assert length % 4 == 0 : "length=" + length;
		return (length * 3 + 3) / 4;
	}

	private char scharAt(String s, int i) {
		if (i >= s.length()) {
			return '0';
		}
		return s.charAt(i);
	}

	public static int decode6bit(char c) {
		return decode6bit[c];
	}

	private static char encode6bit(byte b) {
		assert b >= 0 && b < 64;
		if (b < 10) {
			return (char) ('0' + b);
		}
		b -= 10;
		if (b < 26) {
			return (char) ('A' + b);
		}
		b -= 26;
		if (b < 26) {
			return (char) ('a' + b);
		}
		b -= 26;
		if (b == 0) {
			return '-';
		}
		if (b == 1) {
			return '_';
		}
		assert false;
		return '?';
	}

	private void append3bytes(StringBuilder sb, int b1, int b2, int b3) {
		final int c1 = b1 >> 2;
		final int c2 = ((b1 & 0x3) << 4) | (b2 >> 4);
		final int c3 = ((b2 & 0xF) << 2) | (b3 >> 6);
		final int c4 = b3 & 0x3F;
		sb.append(encode6bit[c1 & 0x3F]);
		sb.append(encode6bit[c2 & 0x3F]);
		sb.append(encode6bit[c3 & 0x3F]);
		sb.append(encode6bit[c4 & 0x3F]);
	}

	private void decode3bytes(byte r[], int pos, char cc1, char cc2, char cc3, char cc4) {
		final int c1 = decode6bit[cc1];
		final int c2 = decode6bit[cc2];
		final int c3 = decode6bit[cc3];
		final int c4 = decode6bit[cc4];
		r[pos] = (byte) ((c1 << 2) | (c2 >> 4));
		r[pos + 1] = (byte) (((c2 & 0x0F) << 4) | (c3 >> 2));
		r[pos + 2] = (byte) (((c3 & 0x3) << 6) | c4);
	}

}
