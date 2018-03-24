/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.terminal;

import dan200.computercraft.shared.utf.UtfException;
import dan200.computercraft.shared.utf.UtfString;

public class TextBuffer
{
    private int[] m_text;

    public TextBuffer( char c, int length )
    {
        m_text = new int[length];
        for( int i = 0; i < length; ++i )
        {
            m_text[i] = c;
        }
    }

    public TextBuffer( int codepoint, int length )
    {
        m_text = new int[length];
        for( int i = 0; i < length; ++i )
        {
            m_text[i] = codepoint;
        }
    }

    public TextBuffer( String text )
    {
        this( text, 1 );
    }

    public TextBuffer( UtfString text )
    {
        this( text, 1 );
    }

    public TextBuffer( String text, int repetitions )
    {
        this(UtfString.fromString(text), repetitions);
    }

    public TextBuffer( UtfString text, int repetitions )
    {
        int textLength = text.length();
        m_text = new int[ textLength * repetitions ];
        for( int i = 0; i < repetitions; ++i )
        {
            for( int j = 0; j < textLength; ++j )
            {
                m_text[ j + i * textLength ] = text.charAt(j  );
            }
        }
    }

    public TextBuffer( TextBuffer text )
    {
        this( text, 1 );
    }

    public TextBuffer( TextBuffer text, int repetitions )
    {
        int textLength = text.length();
        m_text = new int[ textLength * repetitions ];
        for( int i = 0; i < repetitions; ++i )
        {
            for( int j = 0; j < textLength; ++j )
            {
                m_text[ j + i * textLength ] = text.codepointAt(j  );
            }
        }
    }

    public int length()
    {
        return m_text.length;
    }

    public String read()
    {
        return read( 0, m_text.length );
    }

    public String read( int start )
    {
        return read( start, m_text.length );
    }

    public String read( int start, int end )
    {
        start = Math.max( start, 0 );
        end = Math.min( end, m_text.length );
        int textLength = Math.max( end - start, 0 );
        return new String( m_text, start, textLength );
    }

    public UtfString readUtf()
    {
        return readUtf( 0, m_text.length );
    }

    public UtfString readUtf( int start )
    {
        return new UtfString( m_text, start, m_text.length - start );
    }

    public UtfString readUtf( int start, int end )
    {
        start = Math.max( start, 0 );
        end = Math.min( end, m_text.length );
        int textLength = Math.max( end - start, 0 );
        return new UtfString( m_text, start, textLength );
    }

    public void write( String text )
    {
        write( text, 0, text.length() );
    }

    public void write( String text, int start )
    {
        write( text, start, start + text.length() );
    }

    public void write( UtfString text, int start, int end )
    {
        int pos = start;
        start = Math.max( start, 0 );
        end = Math.min( end, pos + text.length() );
        end = Math.min( end, m_text.length );
        for( int i=start; i<end; ++i )
        {
            m_text[i] = text.charAt( i - pos );
        }
    }

    public void write( UtfString text )
    {
        write( text, 0, text.length() );
    }

    public void write( UtfString text, int start )
    {
        write( text, start, start + text.length() );
    }

    public void write( String text, int start, int end )
    {
    	write(UtfString.fromString(text), start, end);
    }

    public void write( TextBuffer text )
    {
        write( text, 0, text.length() );
    }

    public void write( TextBuffer text, int start )
    {
        write( text, start, start + text.length() );
    }

    public void write( TextBuffer text, int start, int end )
    {
        int pos = start;
        start = Math.max( start, 0 );
        end = Math.min( end, pos + text.length() );
        end = Math.min( end, m_text.length );
        for( int i=start; i<end; ++i )
        {
            m_text[i] = text.codepointAt( i - pos );
        }
    }

    public void fill( char c )
    {
        fill( c, 0, m_text.length );
    }

    public void fill( char c, int start )
    {
        fill( c, start, m_text.length );
    }

    public void fill( char c, int start, int end )
    {
        start = Math.max( start, 0 );
        end = Math.min( end, m_text.length );
        for( int i=start; i<end; ++i )
        {
            m_text[i] = c;
        }
    }

    public void fill( int codepoint )
    {
        fill( codepoint, 0, m_text.length );
    }

    public void fill( int codepoint, int start )
    {
        fill( codepoint, start, m_text.length );
    }

    public void fill( int codepoint, int start, int end )
    {
        start = Math.max( start, 0 );
        end = Math.min( end, m_text.length );
        for( int i=start; i<end; ++i )
        {
            m_text[i] = codepoint;
        }
    }

    public void fill( String text )
    {
        fill( text, 0, m_text.length );
    }

    public void fill( String text, int start )
    {
        fill( text, start, m_text.length );
    }

    public void fill( String text, int start, int end )
    {
        fill(UtfString.fromString(text), start, end);
    }

    public void fill( UtfString text )
    {
        fill( text, 0, m_text.length );
    }

    public void fill( UtfString text, int start )
    {
        fill( text, start, m_text.length );
    }

    public void fill( UtfString text, int start, int end )
    {
        int pos = start;
        start = Math.max( start, 0 );
        end = Math.min( end, m_text.length );

        int textLength = text.length();
        for( int i=start; i<end; ++i )
        {
            m_text[i] = text.charAt( (i - pos) % textLength );
        }
    }

    public void fill( TextBuffer text )
    {
        fill( text, 0, m_text.length );
    }

    public void fill( TextBuffer text, int start )
    {
        fill( text, start, m_text.length );
    }

    public void fill( TextBuffer text, int start, int end )
    {
        int pos = start;
        start = Math.max( start, 0 );
        end = Math.min( end, m_text.length );

        int textLength = text.length();
        for( int i=start; i<end; ++i )
        {
            m_text[i] = text.charAt( (i - pos) % textLength );
        }
    }

    public char charAt( int i )
    {
    	int c = m_text[ i ];
    	if (!Character.isBmpCodePoint(c)) return '?';
        return (char) m_text[ i ];
    }

    public int codepointAt( int i )
    {
        return m_text[ i ];
    }

    public void setChar( int i, char c )
    {
        if( i >= 0 && i <m_text.length )
        {
            m_text[ i ] = c;
        }
    }

    public void setChar( int i, int codepoint )
    {
        if( i >= 0 && i <m_text.length )
        {
            m_text[ i ] = codepoint;
        }
    }

    public String toString()
    {
        try {
			return new UtfString( m_text ).toJString();
		} catch (UtfException e) {
			return "";
		}
    }
}
