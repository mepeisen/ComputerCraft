/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.utf;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//Contributed by mepeisen

/**
 * Helper class for utf string operations; supports handling all utf multi bytes sequences as one character.
 * 
 * @author mepeisen
 *
 */
public class UtfString {
	
	/**
	 * utf codepoints (real characters)
	 */
	private int[] codepoints;
	
	/**
	 * Constructor to create by codepoints
	 * @param codepoints
	 */
	public UtfString(int[] codepoints)
	{
		this.codepoints = codepoints;
	}
	
	/**
	 * Constructor to create by codepoints
	 * @param codepoints
	 * @param start
	 * @param length
	 */
	public UtfString(int[] codepoints, int start, int length)
	{
		this.codepoints = Arrays.copyOfRange(codepoints, start, start + length);
	}
	
	/**
	 * Converts string to an utf string;
	 * throws an UtfException if we got an invalid byte sequence
	 * @param utf a utf byte array
	 * @param skipCharacters
	 * @param maxCharacterLen
	 * @return resulting list
	 * @throws UtfException 
	 */
	public UtfString(byte[] utf, int skipCharacters, int maxCharacterLen) throws UtfException
	{
		final List<Integer> result = new ArrayList<>();
		final UtfIter iter = new UtfIter(utf, maxCharacterLen);
		while (iter.hasNext())
		{
			if (skipCharacters > 0)
			{
				skipCharacters--;
				iter.next();
			}
			else
			{
				result.add(iter.next());
			}
		}
		this.codepoints = result.stream().mapToInt(i -> i).toArray();
	}
	
	/**
	 * Returns the codepoint at given character position
	 * @param pos
	 * @return codepoint
	 */
	public int code(int pos)
	{
		return this.codepoints[pos];
	}
	
	/**
	 * Returns the java string, returning empty string if invalid
	 */
	public String toString()
	{
		try {
			return new String(getBytes(), StandardCharsets.UTF_8);
		} catch (UtfException e) {
			return "";
		}
	}
	
	/**
	 * Returns the java string, throwing exceptions if invalid.
	 * @throws UtfException 
	 */
	public String toJString() throws UtfException
	{
		return new String(getBytes(), StandardCharsets.UTF_8);
	}
	
	/**
	 * Returns the utf bytes
	 * @return utf bytes
	 * @throws UtfException 
	 */
	public byte[] getBytes() throws UtfException
	{
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (int pos = 0, n = this.codepoints.length; pos < n; pos++)
		{
			appendBytes(baos, this.codepoints[pos]);
		}
		return baos.toByteArray();
	}
	
	/**
	 * Returns string length in characters
	 * @return string length
	 */
	public int length()
	{
		return this.codepoints.length;
	}
	
	/**
	 * Returns sub string
	 * @param startIndex the starting character; starting by 0
	 * @param length the length in characters
	 * @return substring
	 */
	public UtfString sub(int startIndex, int length)
	{
		return new UtfString(Arrays.copyOfRange(this.codepoints, startIndex, startIndex + length));
	}

	/**
	 * Convert to lower case.
	 * @return
	 * @throws UtfException
	 */
	public UtfString toLowerCase() throws UtfException {
		// TODO rework; a direct array operation will be faster...
		final String lower = new String(getBytes(), StandardCharsets.UTF_8).toLowerCase();
		return new UtfString(lower.getBytes(StandardCharsets.UTF_8), 0, -1);
	}

	/**
	 * Convert to upper case.
	 * @return
	 * @throws UtfException
	 */
	public UtfString toUpperCase() throws UtfException {
		// TODO rework; a direct array operation will be faster...
		final String lower = new String(getBytes(), StandardCharsets.UTF_8).toUpperCase();
		return new UtfString(lower.getBytes(StandardCharsets.UTF_8), 0, -1);
	}

	/**
	 * Reverse the string
	 * @return
	 * @throws UtfException
	 */
	public UtfString reverse() throws UtfException {
		int n = this.codepoints.length;
		final int[] rev = new int[n];
		for (int s = 0, t = n - 1; s < n; s++, t--)
		{
			rev[t] = this.codepoints[s];
		}
		return new UtfString(rev);
	}

	public int charAt(int i) {
		return this.codepoints[i];
	}

	public int indexOf(char c)
	{
		int res = Arrays.binarySearch(this.codepoints, (int) c);
		if (res < 0) res = -1;
		return res;
	}

	public int indexOf(char c, int i)
	{
		int res = Arrays.binarySearch(this.codepoints, (int) c, i, this.codepoints.length);
		if (res < 0) res = -1;
		return res;
	}

	public int indexOf(UtfString c, int fromIndex)
	{
		final int targetCount = c.length();
		if (fromIndex >= this.codepoints.length) {
            return (targetCount == 0 ? this.codepoints.length : -1);
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (targetCount == 0) {
            return fromIndex;
        }

        int first = c.codepoints[0];
        int max = this.codepoints.length - targetCount;

        for (int i = fromIndex; i <= max; i++) {
            /* Look for first character. */
            if (this.codepoints[i] != first) {
                while (++i <= max && this.codepoints[i] != first);
            }

            /* Found first character, now look at the rest of v2 */
            if (i <= max) {
                int j = i + 1;
                int end = j + targetCount - 1;
                for (int k = 1; j < end && this.codepoints[j]
                        == c.codepoints[k]; j++, k++);

                if (j == end) {
                    /* Found whole string. */
                    return i;
                }
            }
        }
        return -1;
	}
	
	public static final class UtfIter
	{

		private int curBytePos = 0;
		
		private int curCharPos = 0;
		
		private final byte[] utf;
		
		private final int maxCharacterLen;
		
		public UtfIter(byte[] utf, int maxCharacterLen) {
			this.utf = utf;
			this.maxCharacterLen = maxCharacterLen;
		}

		public int getCharPos()
		{
			return this.curCharPos;
		}
		
		public int getBytePos()
		{
			return this.curBytePos;
		}

		public boolean hasNext() {
			return curBytePos < utf.length && (curCharPos < maxCharacterLen || maxCharacterLen == -1);
		}

		public Integer next() throws UtfException {
			int expect = 0;
			int cur = 0;
			for (; curBytePos < utf.length && (curCharPos < maxCharacterLen || maxCharacterLen == -1); curBytePos++)
			{
				int b = (int) utf[curBytePos] & 0xFF;
				if (b >= 0b11111000)
				{
					// 5 byte sequence (or greater) considered illegal, see https://en.wikipedia.org/wiki/UTF-8 and RFC 3629
					throw new UtfException("invalid UTF-8 character sequence at byte " + curBytePos);
				}
				else if (b >= 0b11110000)
				{
					// 4 byte sequence
					if (expect > 0)
					{
						throw new UtfException("invalid UTF-8 character sequence at byte " + curBytePos);
					}
					expect = 3;
					cur = b & 0x0F;
				}
				else if (b >= 0b11100000)
				{
					// 3 byte sequence
					if (expect > 0)
					{
						throw new UtfException("invalid UTF-8 character sequence at byte " + curBytePos);
					}
					expect = 2;
					cur = b & 0x1F;
				}
				else if (b >= 0b11000000)
				{
					// 2 byte sequence
					if (expect > 0)
					{
						throw new UtfException("invalid UTF-8 character sequence at byte " + curBytePos);
					}
					expect = 1;
					cur = b & 0x3F;
				}
				else if (b >= 0b10000000)
				{
					// multi byte sequence part
					if (expect == 0)
					{
						throw new UtfException("invalid UTF-8 character sequence at byte " + curBytePos);
					}
					else
					{
						expect--;
						cur = (cur << 6) + (b & 0x3F);
						if (expect == 0)
						{
							curCharPos++;
							curBytePos++;
							return cur;
						}
					}
				}
				else
				{
					if (expect > 0)
					{
						throw new UtfException("invalid UTF-8 character sequence at byte " + curBytePos);
					}
					else
					{
						// ASCII
						curCharPos++;
						curBytePos++;
						return b;
					}
				}
			}
			if (expect > 0)
			{
				throw new UtfException("invalid UTF-8 character sequence at byte " + curBytePos);
			}
			throw new UtfException("Array index out of bounds");
		}
		
	}
	
	/**
	 * Check if character is multi byte sequence (continuation byte)
	 * @param p
	 * @return
	 */
	public static boolean iscont(byte p) {
		return ((p & 0xC0) == 0x80);
	}
	
	/**
	 * Appends characters (one or multiple) to character buffer by translating the given unicode codepoint
	 * @param buffer
	 * @param codepoint
	 */
	public static void appendChar(StringBuffer buffer, int codepoint) throws UtfException {
		if (codepoint < 0 || codepoint > 0x10FFFF) {
			throw new UtfException("invalid UTF-8 character sequence at byte " + 0);
		}
		buffer.appendCodePoint(codepoint);
	}
	
	/**
	 * Append bytes of given unicode codepoint to output stream
	 * @param baos
	 * @param codepoint
	 * @throws UtfException 
	 */
	private static void appendBytes(ByteArrayOutputStream baos, int codepoint) throws UtfException
	{
		if (codepoint < 0)
		{
			throw new UtfException("invalid UTF-8 character sequence at byte " + baos.size());
		}
		else if (codepoint <= 0x7F)
		{
			// ascii
			baos.write((byte) codepoint&0x7F);
		}
		else if (codepoint <= 0x7FF)
		{
			// 2 byte
			baos.write((byte) (0xC0 | (codepoint >> 6)));
			baos.write((byte) (0x80 | (codepoint & 0x3F)));
		}
		else if (codepoint <= 0xFFFF)
		{
			// 3 byte
			baos.write((byte) (0xE0 | (codepoint >> 12)));
			baos.write((byte) (0x80 | ((codepoint >> 6) & 0x3F)));
			baos.write((byte) (0x80 | (codepoint & 0x3F)));
		}
		else if (codepoint <= 0x10FFFF)
		{
			// 4 byte
			baos.write((byte) (0xF0 | (codepoint >> 18)));
			baos.write((byte) (0x80 | ((codepoint >> 12) & 0x3F)));
			baos.write((byte) (0x80 | ((codepoint >> 6) & 0x3F)));
			baos.write((byte) (0x80 | (codepoint & 0x3F)));
		}
		else
		{
			throw new UtfException("invalid UTF-8 character sequence at byte " + baos.size());
		}
	}

	/**
	 * Creates utf string from byte array with byte positions
	 * @param m_bytes
	 * @param m_offset
	 * @param m_length
	 * @return utf string
	 * @throws UtfException 
	 */
	public static UtfString fromBytePos(byte[] m_bytes, int m_offset, int m_length) throws UtfException {
		return new UtfString(Arrays.copyOfRange(m_bytes, m_offset, m_offset + m_length), 0, -1);
	}

	/**
	 * Creates utf string from valid utf string (do not use with LuaString.tostring values)
	 * @param src
	 * @return utf string 
	 */
	public static UtfString fromString(String src) {
		try {
			return new UtfString(src.getBytes(StandardCharsets.UTF_8), 0, -1);
		}
		catch (UtfException ex) {
			// should never happen because java strings should always return valid utf8
			return null;
		}
	}

}
