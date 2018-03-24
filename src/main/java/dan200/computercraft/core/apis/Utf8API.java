/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.VarArgFunction;

import dan200.computercraft.shared.utf.UtfException;
import dan200.computercraft.shared.utf.UtfString;
import dan200.computercraft.shared.utf.UtfString.UtfIter;

// Contributed by mepeisen
// specialized version for utf handling

public class Utf8API extends OneArgFunction {

	protected IAPIEnvironment m_env;

	public Utf8API(IAPIEnvironment _environment) {
		this.m_env = _environment;
	}

	public LuaValue call(LuaValue arg) {
		LuaTable t = new LuaTable();
		t.set("dump", new Dump(t));
		t.set("len", new Len(t));
		t.set("lower", new Lower(t));
		t.set("reverse", new Reverse(t));
		t.set("upper", new Upper(t));
		t.set("char", new Char(t));
		t.set("find", new Find(t));
		t.set("format", new Format(t));
		t.set("gmatch", new Gmatch(t));
		t.set("gsub", new Gsub(t));
		t.set("match", new Match(t));
		t.set("rep", new Rep(t));
		t.set("sub", new Sub(t));
		t.set("charpattern", LuaValue.valueOf("[\\0-\\x7F\\xC2-\\xF4][\\x80-\\xBF]*"));
		t.set("codes", new Codes(t));
		t.set("codepoint", new Codepoint(t));
		t.set("offset", new Offset(t));
		t.set("convertAscii", new ConvertAscii(t));
		return t;
	}

	private class Dump extends OneArgFunction {

		private LuaValue m_orig;

		public Dump(LuaTable t) {
			name = "dump";
			env = t;
			this.m_orig = StringLib.instance.get("dump").checkfunction();
		}

		public LuaValue call(LuaValue arg) {
			// no utf handling needed, pass to orig
			return m_orig.call(arg);
		}

	}

	private class Len extends OneArgFunction {

		public Len(LuaTable t) {
			name = "len";
			env = t;
		}

		public LuaValue call(LuaValue arg) {
			try {
				UtfString utf = checkUtfString(arg);
				return valueOf(utf.length());
			} catch (UtfException e) {
				error(e.getMessage());
				return NIL;
			}
		}

	}

	private class Lower extends OneArgFunction {

		public Lower(LuaTable t) {
			name = "lower";
			env = t;
		}

		public LuaValue call(LuaValue arg) {
			try {
				return valueOfUtf(checkUtfString(arg).toLowerCase());
			} catch (UtfException e) {
				error(e.getMessage());
				return NIL;
			}
		}

	}

	private class Reverse extends OneArgFunction {

		public Reverse(LuaTable t) {
			name = "reverse";
			env = t;
		}

		public LuaValue call(LuaValue arg) {
			try {
				return valueOfUtf(checkUtfString(arg).reverse());
			} catch (UtfException e) {
				error(e.getMessage());
				return NIL;
			}
		}

	}

	private class Upper extends OneArgFunction {

		public Upper(LuaTable t) {
			name = "upper";
			env = t;
		}

		public LuaValue call(LuaValue arg) {
			try {
				return valueOfUtf(checkUtfString(arg).toUpperCase());
			} catch (UtfException e) {
				error(e.getMessage());
				return NIL;
			}
		}

	}

	private class Char extends VarArgFunction {

		public Char(LuaTable t) {
			name = "char";
			env = t;
		}

		public Varargs invoke(Varargs args) {
			int n = args.narg();
			final List<Integer> list = new ArrayList<>();
			for (int i = 0, a = 1; i < n; i++, a++) {
				list.add(args.checkint(a));
			}
			try {
				final LuaString res = valueOfUtf(list.stream());
				return res;
			} catch (UtfException e) {
				error(e.getMessage());
				return NIL;
			}
		}

	}

	private class Find extends VarArgFunction {

		public Find(LuaTable t) {
			name = "find";
			env = t;
		}

		public Varargs invoke(Varargs args) {
			try
			{
				return str_find_aux(args, true);
			}
			catch (UtfException ex)
			{
				error(ex.getMessage());
				return NIL;
			}
		}

	}

	private class Format extends VarArgFunction {

		public Format(LuaTable t) {
			name = "format";
			env = t;
		}

		public Varargs invoke(Varargs args) {
			// mostly taken from original StringLib but works on java strings
			try
			{
				String fmt = checkString(args, 1);
				final int n = fmt.length();
				StringBuffer result = new StringBuffer(n);
				int arg = 1;
				char c;
	
				for (int i = 0; i < n;) {
					switch (c = fmt.charAt(i++)) {
					case '\n':
						result.append("\n");
						break;
					default:
						result.append(c);
						break;
					case L_ESC:
						if (i < n) {
							if ((c = fmt.charAt(i)) == L_ESC) {
								++i;
								result.append(L_ESC);
							} else {
								arg++;
								FormatDesc fdsc = new FormatDesc(args, fmt, i);
								i += fdsc.length;
								switch (fdsc.conversion) {
								case 'c':
									fdsc.formatChar(result, args.checkint(arg));
									break;
								case 'i':
								case 'd':
									fdsc.format(result, args.checkint(arg));
									break;
								case 'o':
								case 'u':
								case 'x':
								case 'X':
									fdsc.format(result, args.checklong(arg));
									break;
								case 'e':
								case 'E':
								case 'f':
								case 'g':
								case 'G':
									fdsc.format(result, args.checkdouble(arg));
									break;
								case 'q':
									addquoted(result, checkUtfString(args, arg));
									break;
								case 's': {
									UtfString s = checkUtfString(args, arg);
									if (fdsc.precision == -1 && s.length() >= 100) {
										result.append(s);
									} else {
										fdsc.format(result, s);
									}
								}
									break;
								default:
									error("invalid option '%" + (char) fdsc.conversion + "' to 'format'");
									break;
								}
							}
						}
					}
				}
	
				return LuaString.valueOf(result.toString());
			}
			catch (UtfException ex)
			{
				error(ex.getMessage());
				return NIL;
			}
		}

	}

	private class Match extends VarArgFunction {

		public Match(LuaTable t) {
			name = "match";
			env = t;
		}

		public Varargs invoke(Varargs args) {
			try
			{
				return str_find_aux(args, false);
			}
			catch (UtfException ex)
			{
				error(ex.getMessage());
				return NIL;
			}
		}

	}

	private class Rep extends VarArgFunction {

		private LuaValue m_orig;

		public Rep(LuaTable t) {
			name = "rep";
			env = t;
			this.m_orig = StringLib.instance.get("rep").checkfunction();
		}

		public Varargs invoke(Varargs args) {
			// utf variant is not needed because StringLib duplicates underlying byte arrays
			// which is really ok for utf
			return m_orig.invoke(args);
		}

	}

	private class Sub extends VarArgFunction {

		public Sub(LuaTable t) {
			name = "sub";
			env = t;
		}

		public Varargs invoke(Varargs args) {
			try {
				UtfString s = checkUtfString(args, 1);
				final int l = s.length();

				int start = posrelat(args.checkint(2), l);
				int end = posrelat(args.optint(3, -1), l);

				if (start < 1)
					start = 1;
				if (end > l)
					end = l;

				if (start <= end) {
					return valueOfUtf(s.sub(start - 1, end - start + 1));
				} else {
					return EMPTYSTRING;
				}
			} catch (UtfException e) {
				error(e.getMessage());
				return NIL;
			}
		}

	}

	private class Codes extends VarArgFunction {

		public Codes(LuaTable t) {
			name = "codes";
			env = t;
		}

		public Varargs invoke(Varargs args) {
			final LuaString s = args.checkstring(1);
			return new CodesFunction(s);
		}

	}

	private class Codepoint extends VarArgFunction {

		public Codepoint(LuaTable t) {
			name = "codepoint";
			env = t;
		}

		public Varargs invoke(Varargs args) {
			// prolog mostly taken from lutf8lib.c
			final LuaString str = args.checkstring(1);
			int posi = posrelat(args.optint(2, 1), str.m_length);
			int pose = posrelat(args.optint(3, posi), str.m_length);
			
			args.argcheck(posi >= 1, 2, "out of range");
			args.argcheck(pose <= str.m_length, 3, "out of range");
			if (posi > pose) return LuaInteger.ZERO;
			
			int n = (int)(pose -  posi + 1);
			if (posi + n <= pose)  /* overflow? */
			    error("string slice too long");
			
			final byte[] sub = Arrays.copyOfRange(str.m_bytes, str.m_offset + posi - 1, str.m_offset + pose - 1);
			try
			{
				final UtfString utf = new UtfString(sub, 0, -1);
				final LuaValue[] res = new LuaValue[utf.length()];
				for (int i = 0, len = utf.length(); i < len; i++)
				{
					res[i] = valueOf(utf.code(i));
				}
				return varargsOf(res);
			}
			catch (UtfException e) {
				error(e.getMessage());
				return NIL;
			}
		}

	}

	private class ConvertAscii extends VarArgFunction {

		public ConvertAscii(LuaTable t) {
			name = "convertAscii";
			env = t;
		}

		public Varargs invoke(Varargs args) {
			// prolog mostly taken from lutf8lib.c
			final LuaString str = args.checkstring(1);
			final int[] bytes = new int[str.m_length];
			for (int i = 0; i < str.m_length; i++)
			{
				bytes[i] = (int)str.m_bytes[str.m_offset + i] & 0xFF;
			}
			try
			{
				return valueOfUtf(new UtfString(bytes));
			}
			catch (UtfException e) {
				error(e.getMessage());
				return NIL;
			}
		}

	}

	private class Offset extends VarArgFunction {

		public Offset(LuaTable t) {
			name = "offset";
			env = t;
		}

		public Varargs invoke(Varargs args) {
			// mostly taken from lutf8lib.c
			final LuaString str = args.checkstring(1);
			int n  = args.checkint(2);
			int posi = n >= 0 ? 1 : str.m_length + 1;
			posi = posrelat(args.optint(3, posi), str.m_length);
			
			args.argcheck(1 <= posi && --posi <= str.m_length, 3, "position out of range");
			
			if (n == 0) {
				/* find beginning of current byte sequence */
			    while (posi > 0 && UtfString.iscont(str.m_bytes[str.m_offset + posi])) posi--;
			}
			else {
				if (UtfString.iscont(str.m_bytes[str.m_offset + posi]))
			      error("initial position is a continuation byte");
			    if (n < 0) {
			       while (n < 0 && posi > 0) {  /* move back */
			         do {  /* find beginning of previous character */
			           posi--;
			         } while (posi > 0 && UtfString.iscont(str.m_bytes[str.m_offset + posi]));
			         n++;
			       }
			     }
			     else {
			       n--;  /* do not move for 1st character */
			       while (n > 0 && posi < str.m_length) {
			         do {  /* find beginning of next character */
			           posi++;
			         } while (UtfString.iscont(str.m_bytes[str.m_offset + posi]));  /* (cannot pass final '\0') */
			         n--;
			       }
			     }
			  }
			  if (n == 0)  /* did it find given character? */
			    return valueOf(posi + 1);
			  else  /* no such character */
			    return NIL;
		}

	}

	// utilities
	
	static class CodesFunction extends VarArgFunction {
		
		private UtfIter iter;
		
		public CodesFunction(LuaString s) {
			final byte[] bytes = Arrays.copyOfRange(s.m_bytes, s.m_offset, s.m_offset + s.m_length);
			this.iter = new UtfIter(bytes, -1);
		}

		public Varargs invoke(Varargs args) {
			if (iter.hasNext())
			{
				try {
					return varargsOf(LuaInteger.valueOf(iter.getBytePos()), LuaInteger.valueOf(iter.next()));
				} catch (UtfException e) {
					error(e.getMessage());
				}
			}
			return NIL;
		}
	}

	/**
	 * This utility method implements both string.find and string.match. mostly
	 * taken for original string lib
	 * @throws UtfException 
	 */
	static Varargs str_find_aux(Varargs args, boolean find) throws UtfException {
		UtfString s = checkUtfString(args, 1);
		UtfString pat = checkUtfString(args, 2);
		int init = args.optint(3, 1);

		if (init > 0) {
			init = Math.min(init - 1, s.length());
		} else if (init < 0) {
			init = Math.max(0, s.length() + init);
		}

		boolean fastMatch = find && (args.arg(4).toboolean() || indexOfAny(pat, SPECIALS) == -1);

		if (fastMatch) {
			int result = s.indexOf(pat, init);
			if (result != -1) {
				return varargsOf(valueOf(result + 1), valueOf(result + pat.length()));
			}
		} else {
			MatchState ms = new MatchState(args, s, pat);

			boolean anchor = false;
			int poff = 0;
			if (pat.charAt(0) == '^') {
				anchor = true;
				poff = 1;
			}

			int soff = init;
			do {
				int res;
				ms.reset();
				if ((res = ms.match(soff, poff)) != -1) {
					if (find) {
						return varargsOf(valueOf(soff + 1), valueOf(res), ms.push_captures(false, soff, res));
					} else {
						return ms.push_captures(true, soff, res);
					}
				}
			} while (soff++ < s.length() && !anchor);
		}
		return NIL;
	}

	// taken from original StringLib
	private static final String SPECIALS = "^$*+?.([%-";
	private static final int MAX_CAPTURES = 32;
	private static final char L_ESC = '%';
	private static final int CAP_UNFINISHED = -1;
	private static final int CAP_POSITION = -2;
	private static final byte MASK_ALPHA = 0x01;
	private static final byte MASK_LOWERCASE = 0x02;
	private static final byte MASK_UPPERCASE = 0x04;
	private static final byte MASK_DIGIT = 0x08;
	private static final byte MASK_PUNCT = 0x10;
	private static final byte MASK_SPACE = 0x20;
	private static final byte MASK_CONTROL = 0x40;
	private static final byte MASK_HEXDIGIT = (byte) 0x80;
	private static final byte[] CHAR_TABLE;

	// taken from original StringLib
	static {
		CHAR_TABLE = new byte[256];

		for (int i = 0; i < 256; ++i) {
			final char c = (char) i;
			CHAR_TABLE[i] = (byte) ((Character.isDigit(c) ? MASK_DIGIT : 0)
					| (Character.isLowerCase(c) ? MASK_LOWERCASE : 0) | (Character.isUpperCase(c) ? MASK_UPPERCASE : 0)
					| ((c < ' ' || c == 0x7F) ? MASK_CONTROL : 0));
			if ((c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F') || (c >= '0' && c <= '9')) {
				CHAR_TABLE[i] |= MASK_HEXDIGIT;
			}
			if ((c >= '!' && c <= '/') || (c >= ':' && c <= '@')) {
				CHAR_TABLE[i] |= MASK_PUNCT;
			}
			if ((CHAR_TABLE[i] & (MASK_LOWERCASE | MASK_UPPERCASE)) != 0) {
				CHAR_TABLE[i] |= MASK_ALPHA;
			}
		}

		CHAR_TABLE[' '] = MASK_SPACE;
		CHAR_TABLE['\r'] |= MASK_SPACE;
		CHAR_TABLE['\n'] |= MASK_SPACE;
		CHAR_TABLE['\t'] |= MASK_SPACE;
		/* DAN200 START */
		// CHAR_TABLE[0x0C /* '\v' */ ] |= MASK_SPACE;
		CHAR_TABLE[0x0B /* '\v' */ ] |= MASK_SPACE;
		/* DAN200 END */
		CHAR_TABLE['\f'] |= MASK_SPACE;
	};

	// similar to LuaString
	private static final int indexOfAny(UtfString src, String pattern) throws UtfException {
		final char[] srcarr = src.toJString().toCharArray();
		final char[] patarr = pattern.toCharArray();
		for (int i = 0, n = srcarr.length; i < n; i++) {
			for (int j = 0, n2 = patarr.length; j < n2; j++) {
				if (srcarr[i] == patarr[j])
					return i;
			}
		}
		return -1;
	}

	// mostly taken from StringLib but works on java strings for utf support
	static class MatchState {
		final UtfString s;
		final UtfString p;
		final Varargs args;
		int level;
		int[] cinit;
		int[] clen;

		MatchState(Varargs args, UtfString s, UtfString pattern) {
			this.s = s;
			this.p = pattern;
			this.args = args;
			this.level = 0;
			this.cinit = new int[MAX_CAPTURES];
			this.clen = new int[MAX_CAPTURES];
		}

		void reset() {
			level = 0;
		}

		private void add_s(StringBuffer lbuf, UtfString news, int soff, int e) throws UtfException {
			int l = news.length();
			for (int i = 0; i < l; ++i) {
				int b = news.charAt(i);
				if (b != L_ESC) {
					lbuf.append(b);
				} else {
					++i; // skip ESC
					b = news.charAt(i);
					if (!isAsciiDigit(b)) {
						lbuf.append(b);
					} else if (b == '0') {
						lbuf.append(s.sub(soff, e).toJString());
					} else {
						lbuf.append(checkString(push_onecapture(b - '1', soff, e)));
					}
				}
			}
		}

		public void add_value(StringBuffer lbuf, int soffset, int end, LuaValue repl) throws UtfException {
			switch (repl.type()) {
			case LuaValue.TSTRING:
			case LuaValue.TNUMBER:
				add_s(lbuf, checkUtfString(repl), soffset, end);
				return;

			case LuaValue.TFUNCTION:
				repl = repl.invoke(push_captures(true, soffset, end)).arg1();
				break;

			case LuaValue.TTABLE:
				// Need to call push_onecapture here for the error checking
				repl = repl.get(push_onecapture(0, soffset, end));
				break;

			default:
				error("bad argument: string/function/table expected");
				return;
			}

			if (!repl.toboolean()) {
				lbuf.append(s.sub(soffset, end));
			} else if (!repl.isstring()) {
				error("invalid replacement value (a " + repl.typename() + ")");
			} else {
				lbuf.append(checkString(repl));
			}
		}

		Varargs push_captures(boolean wholeMatch, int soff, int end) throws UtfException {
			int nlevels = (this.level == 0 && wholeMatch) ? 1 : this.level;
			switch (nlevels) {
			case 0:
				return NONE;
			case 1:
				return push_onecapture(0, soff, end);
			}
			LuaValue[] v = new LuaValue[nlevels];
			for (int i = 0; i < nlevels; ++i)
				v[i] = push_onecapture(i, soff, end);
			return varargsOf(v);
		}

		private LuaValue push_onecapture(int i, int soff, int end) throws UtfException {
			if (i >= this.level) {
				if (i == 0) {
					return valueOfUtf(s.sub(soff, end));
				} else {
					return error("invalid capture index");
				}
			} else {
				int l = clen[i];
				if (l == CAP_UNFINISHED) {
					return error("unfinished capture");
				}
				if (l == CAP_POSITION) {
					return valueOf(cinit[i] + 1);
				} else {
					int begin = cinit[i];
					return valueOfUtf(s.sub(begin, begin + l));
				}
			}
		}

		private int check_capture(int l) {
			l -= '1';
			if (l < 0 || l >= level || this.clen[l] == CAP_UNFINISHED) {
				error("invalid capture index");
			}
			return l;
		}

		private int capture_to_close() {
			int level = this.level;
			for (level--; level >= 0; level--)
				if (clen[level] == CAP_UNFINISHED)
					return level;
			error("invalid pattern capture");
			return 0;
		}

		int classend(int poffset) {
			switch (p.charAt(poffset++)) {
			case L_ESC:
				if (poffset == p.length()) {
					error("malformed pattern (ends with %)");
				}
				return poffset + 1;

			case '[':
				if (p.charAt(poffset) == '^')
					poffset++;
				do {
					if (poffset == p.length()) {
						error("malformed pattern (missing ])");
					}
					if (p.charAt(poffset++) == L_ESC && poffset != p.length())
						poffset++;
				} while (p.charAt(poffset) != ']');
				return poffset + 1;
			default:
				return poffset;
			}
		}

		static boolean match_class(int c, int cl) {

			int cdata;
			if (c < CHAR_TABLE.length) {
				cdata = CHAR_TABLE[c];
			} else {
				final char cc = (char) c;
				cdata = 0;
				if (Character.isDigit(cc))
					cdata |= MASK_DIGIT;
				if (Character.isLowerCase(cc))
					cdata |= MASK_LOWERCASE | MASK_ALPHA;
				if (Character.isUpperCase(cc))
					cdata |= MASK_UPPERCASE | MASK_ALPHA;
				if (Character.isWhitespace(cc))
					cdata |= MASK_SPACE;
			}

			boolean res;
			switch (c) {
			case 'A':
				res = (cdata & MASK_ALPHA) == 0;
				break;
			case 'a':
				res = (cdata & MASK_ALPHA) != 0;
				break;
			case 'D':
				res = (cdata & MASK_DIGIT) == 0;
				break;
			case 'd':
				res = (cdata & MASK_DIGIT) != 0;
				break;
			case 'L':
				res = (cdata & MASK_LOWERCASE) == 0;
				break;
			case 'l':
				res = (cdata & MASK_LOWERCASE) != 0;
				break;
			case 'U':
				res = (cdata & MASK_UPPERCASE) == 0;
				break;
			case 'u':
				res = (cdata & MASK_UPPERCASE) != 0;
				break;
			case 'C':
				res = (cdata & MASK_CONTROL) == 0;
				break;
			case 'c':
				res = (cdata & MASK_CONTROL) != 0;
				break;
			case 'P':
				res = (cdata & MASK_PUNCT) == 0;
				break;
			case 'p':
				res = (cdata & MASK_PUNCT) != 0;
				break;
			case 'S':
				res = (cdata & MASK_SPACE) == 0;
				break;
			case 's':
				res = (cdata & MASK_SPACE) != 0;
				break;
			case 'W':
				res = (cdata & (MASK_ALPHA | MASK_DIGIT)) == 0;
				break;
			case 'w':
				res = (cdata & (MASK_ALPHA | MASK_DIGIT)) != 0;
				break;
			case 'X':
				res = (cdata & MASK_HEXDIGIT) == 0;
				break;
			case 'x':
				res = (cdata & MASK_HEXDIGIT) != 0;
				break;
			case 'Z':
				res = (c != 0);
				break;
			case 'z':
				res = (c == 0);
				break;
			default:
				return cl == c;
			}
			return res;
		}

		boolean matchbracketclass(int c, int poff, int ec) {
			boolean sig = true;
			if (p.charAt(poff + 1) == '^') {
				sig = false;
				poff++;
			}
			while (++poff < ec) {
				if (p.charAt(poff) == L_ESC) {
					poff++;
					if (match_class(c, p.charAt(poff)))
						return sig;
				} else if ((p.charAt(poff + 1) == '-') && (poff + 2 < ec)) {
					poff += 2;
					if (p.charAt(poff - 2) <= c && c <= p.charAt(poff))
						return sig;
				} else if (p.charAt(poff) == c)
					return sig;
			}
			return !sig;
		}

		boolean singlematch(int c, int poff, int ep) {
			switch (p.charAt(poff)) {
			case '.':
				return true;
			case L_ESC:
				return match_class(c, p.charAt(poff + 1));
			case '[':
				return matchbracketclass(c, poff, ep - 1);
			default:
				return p.charAt(poff) == c;
			}
		}

		/**
		 * Perform pattern matching. If there is a match, returns offset into s where
		 * match ends, otherwise returns -1.
		 * @throws UtfException 
		 */
		int match(int soffset, int poffset) throws UtfException {
			while (true) {
				// Check if we are at the end of the pattern -
				// equivalent to the '\0' case in the C version, but our pattern
				// string is not NUL-terminated.
				if (poffset == p.length())
					return soffset;
				switch (p.charAt(poffset)) {
				case '(':
					if (++poffset < p.length() && p.charAt(poffset) == ')')
						return start_capture(soffset, poffset + 1, CAP_POSITION);
					else
						return start_capture(soffset, poffset, CAP_UNFINISHED);
				case ')':
					return end_capture(soffset, poffset + 1);
				case L_ESC:
					if (poffset + 1 == p.length())
						error("malformed pattern (ends with '%')");
					switch (p.charAt(poffset + 1)) {
					case 'b':
						soffset = matchbalance(soffset, poffset + 2);
						if (soffset == -1)
							return -1;
						poffset += 4;
						continue;
					case 'f': {
						poffset += 2;
						if (p.charAt(poffset) != '[') {
							error("Missing [ after %f in pattern");
						}
						int ep = classend(poffset);
						int previous = (soffset == 0) ? -1 : s.charAt(soffset - 1);
						if (matchbracketclass(previous, poffset, ep - 1)
								|| matchbracketclass(s.charAt(soffset), poffset, ep - 1))
							return -1;
						poffset = ep;
						continue;
					}
					default: {
						int c = p.charAt(poffset + 1);
						if (Character.isDigit((char) c)) {
							soffset = match_capture(soffset, c);
							if (soffset == -1)
								return -1;
							return match(soffset, poffset + 2);
						}
					}
					}
				case '$':
					if (poffset + 1 == p.length())
						return (soffset == s.length()) ? soffset : -1;
				}
				int ep = classend(poffset);
				boolean m = soffset < s.length() && singlematch(s.charAt(soffset), poffset, ep);
				int pc = (ep < p.length()) ? p.charAt(ep) : '\0';

				switch (pc) {
				case '?':
					int res;
					if (m && ((res = match(soffset + 1, ep + 1)) != -1))
						return res;
					poffset = ep + 1;
					continue;
				case '*':
					return max_expand(soffset, poffset, ep);
				case '+':
					return (m ? max_expand(soffset + 1, poffset, ep) : -1);
				case '-':
					return min_expand(soffset, poffset, ep);
				default:
					if (!m)
						return -1;
					soffset++;
					poffset = ep;
					continue;
				}
			}
		}

		int max_expand(int soff, int poff, int ep) throws UtfException {
			int i = 0;
			while (soff + i < s.length() && singlematch(s.charAt(soff + i), poff, ep))
				i++;
			while (i >= 0) {
				int res = match(soff + i, ep + 1);
				if (res != -1)
					return res;
				i--;
			}
			return -1;
		}

		int min_expand(int soff, int poff, int ep) throws UtfException {
			for (;;) {
				int res = match(soff, ep + 1);
				if (res != -1)
					return res;
				else if (soff < s.length() && singlematch(s.charAt(soff), poff, ep))
					soff++;
				else
					return -1;
			}
		}

		int start_capture(int soff, int poff, int what) throws UtfException {
			int res;
			int level = this.level;
			if (level >= MAX_CAPTURES) {
				error("too many captures");
			}
			cinit[level] = soff;
			clen[level] = what;
			this.level = level + 1;
			if ((res = match(soff, poff)) == -1)
				this.level--;
			return res;
		}

		int end_capture(int soff, int poff) throws UtfException {
			int l = capture_to_close();
			int res;
			clen[l] = soff - cinit[l];
			if ((res = match(soff, poff)) == -1)
				clen[l] = CAP_UNFINISHED;
			return res;
		}

		int match_capture(int soff, int l) throws UtfException {
			l = check_capture(l);
			int len = clen[l];
			if ((s.length() - soff) >= len
					&& LuaString.equals(valueOfUtf(s), cinit[l], valueOfUtf(s), soff, len))
				return soff + len;
			else
				return -1;
		}

		int matchbalance(int soff, int poff) {
			final int plen = p.length();
			if (poff == plen || poff + 1 == plen) {
				error("unbalanced pattern");
			}
			/* DAN200 START */
			if (soff >= s.length())
				return -1;
			/* DAN200 END */
			if (s.charAt(soff) != p.charAt(poff))
				return -1;
			else {
				int b = p.charAt(poff);
				int e = p.charAt(poff + 1);
				int cont = 1;
				while (++soff < s.length()) {
					if (s.charAt(soff) == e) {
						if (--cont == 0)
							return soff + 1;
					} else if (s.charAt(soff) == b)
						cont++;
				}
			}
			return -1;
		}
	}

	// taken from original StringLib
	private static int posrelat(int pos, int len) {
		return (pos >= 0) ? pos : len + pos + 1;
	}

	// taken from original StringLib but works on java strings
	static class FormatDesc {

		private boolean leftAdjust;
		private boolean zeroPad;
		private boolean explicitPlus;
		private boolean space;
		private boolean alternateForm;
		private static final int MAX_FLAGS = 5;

		private int width;
		private int precision;

		public final int conversion;
		public final int length;

		public FormatDesc(Varargs args, String strfrmt, final int start) {
			int p = start, n = strfrmt.length();
			char c = 0;

			boolean moreFlags = true;
			while (moreFlags) {
				switch (c = ((p < n) ? strfrmt.charAt(p++) : 0)) {
				case '-':
					leftAdjust = true;
					break;
				case '+':
					explicitPlus = true;
					break;
				case ' ':
					space = true;
					break;
				case '#':
					alternateForm = true;
					break;
				case '0':
					zeroPad = true;
					break;
				default:
					moreFlags = false;
					break;
				}
			}
			if (p - start > MAX_FLAGS)
				error("invalid format (repeated flags)");

			width = -1;
			if (isAsciiDigit(c)) {
				width = c - '0';
				c = ((p < n) ? strfrmt.charAt(p++) : 0);
				if (isAsciiDigit(c)) {
					width = width * 10 + (c - '0');
					c = ((p < n) ? strfrmt.charAt(p++) : 0);
				}
			}

			precision = -1;
			if (c == '.') {
				c = ((p < n) ? strfrmt.charAt(p++) : 0);
				if (isAsciiDigit(c)) {
					precision = c - '0';
					c = ((p < n) ? strfrmt.charAt(p++) : 0);
					if (isAsciiDigit(c)) {
						precision = precision * 10 + (c - '0');
						c = ((p < n) ? strfrmt.charAt(p++) : 0);
					}
				}
			}

			if (isAsciiDigit(c))
				error("invalid format (width or precision too long)");

			zeroPad &= !leftAdjust; // '-' overrides '0'
			conversion = c;
			length = p - start;
		}

		public void formatChar(StringBuffer result, int checkint) throws UtfException {
			UtfString.appendChar(result, checkint);
		}

		public void format(StringBuffer buf, long number) {
			String digits;

			if (number == 0 && precision == 0) {
				digits = "";
			} else {
				int radix;
				switch (conversion) {
				case 'x':
				case 'X':
					radix = 16;
					break;
				case 'o':
					radix = 8;
					break;
				default:
					radix = 10;
					break;
				}
				digits = Long.toString(number, radix);
				if (conversion == 'X')
					digits = digits.toUpperCase();
			}

			int minwidth = digits.length();
			int ndigits = minwidth;
			int nzeros;

			if (number < 0) {
				ndigits--;
			} else if (explicitPlus || space) {
				minwidth++;
			}

			if (precision > ndigits)
				nzeros = precision - ndigits;
			else if (precision == -1 && zeroPad && width > minwidth)
				nzeros = width - minwidth;
			else
				nzeros = 0;

			minwidth += nzeros;
			int nspaces = width > minwidth ? width - minwidth : 0;

			if (!leftAdjust)
				pad(buf, ' ', nspaces);

			if (number < 0) {
				if (nzeros > 0) {
					buf.append('-');
					digits = digits.substring(1);
				}
			} else if (explicitPlus) {
				buf.append('+');
			} else if (space) {
				buf.append(' ');
			}

			if (nzeros > 0)
				pad(buf, '0', nzeros);

			buf.append(digits);

			if (leftAdjust)
				pad(buf, ' ', nspaces);
		}

		public void format(StringBuffer buf, double x) {
			// TODO original StringLib does not handle formatting... Should be upgraded as soon as
			// we use some other lua version
			buf.append(String.valueOf(x));
		}

		public void format(StringBuffer buf, UtfString s) throws UtfException {
			int nullindex = s.indexOf('\0', 0);
			if (nullindex != -1)
				s = s.sub(0, nullindex);
			buf.append(s.toJString());
		}

		public static final void pad(StringBuffer buf, char c, int n) {
			while (n-- > 0)
				buf.append(c);
		}
	}

	// the original invokations of Character.isDigit will return true for eastern
	// arabic etc. However this may break the logic on how it works the "lua way".
	// maybe we could rework the methods to support eastern arabic and other digits.
	protected static boolean isAsciiDigit(int c) {
		return c >= '0' && c <= '9';
	}

	// taken from original StringLib but works on java strings
	static class GMatchAux extends VarArgFunction {
		private final int srclen;
		private final MatchState ms;
		private int soffset;

		public GMatchAux(Varargs args, UtfString src, UtfString pat) {
			this.srclen = src.length();
			this.ms = new MatchState(args, src, pat);
			this.soffset = 0;
		}

		public Varargs invoke(Varargs args) {
			try
			{
				for (; soffset < srclen; soffset++) {
					ms.reset();
					int res = ms.match(soffset, 0);
					if (res >= 0) {
						int soff = soffset;
						soffset = res;
						/* DAN200 START */
						if (res == soff)
							soffset++;
						/* DAN200 END */
						return ms.push_captures(true, soff, res);
					}
				}
				return NIL;
			}
			catch (UtfException ex)
			{
				error(ex.getMessage());
				return NIL;
			}
		}
	}

	// taken from StringLib
	private static void addquoted(StringBuffer buf, UtfString s) throws UtfException {
		int c;
		buf.append( (byte) '"' );
		final byte[] bytes = s.getBytes();
		for ( int i = 0, n = bytes.length; i < n; i++ ) {
			switch ( c = bytes[i] ) {
			case '"': case '\\':  case '\n':
				buf.append( (byte)'\\' );
				buf.append( (byte)c );
				break;
			case '\r':
				buf.append( "\\r" );
				break;
			case '\0':
				buf.append( "\\000" );
				break;
			default:
				/* DAN200 START */
				//buf.append( (byte) c );
				if( (c >= 32 && c <= 126) || (c >= 160 && c <= 255) ) {
					buf.append( (byte)c );
				} else {
					String str = Integer.toString(c);
					while( str.length() < 3 ) {
					    str = "0" + str;
					}					
					buf.append( "\\" + str );
				}
				/* DAN200 END */
			break;
			}
		}
		buf.append( (byte) '"' );
	}

	private class Gmatch extends VarArgFunction {

		public Gmatch(LuaTable t) {
			name = "gmatch";
			env = t;
		}

		public Varargs invoke(Varargs args) {
			try
			{
				UtfString src = checkUtfString(args, 1);
				UtfString pat = checkUtfString(args, 2);
				return new GMatchAux(args, src, pat);
			}
			catch (UtfException ex)
			{
				error(ex.getMessage());
				return NIL;
			}
		}

	}

	private class Gsub extends VarArgFunction {

		public Gsub(LuaTable t) {
			name = "gsub";
			env = t;
		}

		public Varargs invoke(Varargs args) {
			try
			{
				// mostly taken from original StringLib
				UtfString src = checkUtfString(args, 1);
				final int srclen = src.length();
				UtfString p = checkUtfString(args, 2);
				LuaValue repl = args.arg(3);
				int max_s = args.optint(4, srclen + 1);
				final boolean anchor = p.length() > 0 && p.charAt(0) == '^';
	
				StringBuffer lbuf = new StringBuffer(srclen);
				MatchState ms = new MatchState(args, src, p);
	
				int soffset = 0;
				int n = 0;
				while (n < max_s) {
					ms.reset();
					int res = ms.match(soffset, anchor ? 1 : 0);
					if (res != -1) {
						n++;
						ms.add_value(lbuf, soffset, res, repl);
					}
					if (res != -1 && res > soffset)
						soffset = res;
					else if (soffset < srclen)
						UtfString.appendChar(lbuf, src.charAt(soffset++));
					else
						break;
					if (anchor)
						break;
				}
				lbuf.append(src.sub(soffset, srclen).toJString());
				return varargsOf(LuaString.valueOf(lbuf.toString().getBytes(StandardCharsets.UTF_8)), valueOf(n));
			}
			catch (UtfException ex)
			{
				error(ex.getMessage());
				return NIL;
			}
		}

	}

	/**
	 * Check for valid utf string
	 * 
	 * @param arg
	 *            lua string
	 * @return java string by parsing the utf8 bytes
	 * @throws UtfException
	 */
	public static UtfString checkUtfString(LuaValue arg) throws UtfException {
		final LuaString str = arg.checkstring();
		for (int i = 0; i < str.length(); i++) System.out.println(i+": "+str.charAt(i));
		return UtfString.fromBytePos(str.m_bytes, str.m_offset, str.m_length);
	}

	/**
	 * Check for valid utf string
	 * 
	 * @param args
	 *            varargs
	 * @param i
	 *            number
	 * @return java string by parsing the utf8 bytes
	 * @throws UtfException
	 */
	public static UtfString checkUtfString(Varargs args, int i) throws UtfException {
		final LuaString str = args.checkstring(i);
		return UtfString.fromBytePos(str.m_bytes, str.m_offset, str.m_length);
	}

	/**
	 * Check for valid utf string
	 * 
	 * @param args
	 *            varargs
	 * @param i
	 *            number
	 * @return java string by parsing the utf8 bytes
	 * @throws UtfException
	 */
	public static String checkString(Varargs args, int i) throws UtfException {
		final LuaString str = args.checkstring(i);
		return UtfString.fromBytePos(str.m_bytes, str.m_offset, str.m_length).toJString();
	}

	/**
	 * Check for valid utf string
	 * 
	 * @param arg
	 *            argugment
	 * @return java string by parsing the utf8 bytes
	 * @throws UtfException
	 */
	public static String checkString(LuaValue arg) throws UtfException {
		final LuaString str = arg.checkstring();
		return UtfString.fromBytePos(str.m_bytes, str.m_offset, str.m_length).toJString();
	}

	/**
	 * Constructors a lua string with utf8 bytes.
	 * 
	 * @param str
	 *            java string
	 * @return lua string
	 * @throws UtfException
	 */
	public static LuaString valueOfUtf(UtfString str) throws UtfException {
		return valueOf(str.getBytes());
	}

	/**
	 * Constructs a lua string by interpreting the given integers as utf codepoints.
	 * 
	 * @param stream
	 * @return
	 */
	public static LuaString valueOfUtf(Stream<Integer> stream) throws UtfException {
		return valueOf(new UtfString(stream.mapToInt(i -> i).toArray()).getBytes());
	}

}
