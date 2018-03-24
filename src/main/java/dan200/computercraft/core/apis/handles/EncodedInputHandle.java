package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.StandardCharsets;

import static dan200.computercraft.core.apis.ArgumentHelper.*;

public class EncodedInputHandle extends HandleGeneric
{
    private final BufferedReader m_reader;
    
    private final boolean doNotWrapUtf8;

    public EncodedInputHandle( BufferedReader reader )
    {
        super( reader );
        this.m_reader = reader;
        this.doNotWrapUtf8 = false;
    }

    public EncodedInputHandle( InputStream stream )
    {
        this( stream, "UTF-8" );
    }

    public EncodedInputHandle( InputStream stream, String encoding )
    {
        this( makeReader( stream, encoding ) );
    }

    public EncodedInputHandle( BufferedReader reader, boolean doNotWrapUtf8 )
    {
        super( reader );
        this.m_reader = reader;
        this.doNotWrapUtf8 = doNotWrapUtf8;
    }

    public EncodedInputHandle( InputStream stream, boolean doNotWrapUtf8 )
    {
        this( stream, "UTF-8", doNotWrapUtf8 );
    }

    public EncodedInputHandle( InputStream stream, String encoding, boolean doNotWrapUtf8 )
    {
        this( makeReader( stream, encoding ), doNotWrapUtf8 );
    }

    private static BufferedReader makeReader( InputStream stream, String encoding )
    {
        if( encoding == null ) encoding = "UTF-8";
        InputStreamReader streamReader;
        try
        {
            streamReader = new InputStreamReader( stream, encoding );
        }
        catch( UnsupportedEncodingException e )
        {
            streamReader = new InputStreamReader( stream );
        }
        return new BufferedReader( streamReader );
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return new String[] {
            "readLine",
            "readAll",
            "close",
            "read",
        };
    }

    @Override
    public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] args ) throws LuaException
    {
        switch( method )
        {
            case 0:
                // readLine
                checkOpen();
                try
                {
                    String line = m_reader.readLine();
                    if( line != null )
                    {
                        if (this.doNotWrapUtf8)
                        {
                            return new Object[] { line.getBytes(StandardCharsets.UTF_8) };	
                        }
                        return new Object[] { line };
                    }
                    else
                    {
                        return null;
                    }
                }
                catch( IOException e )
                {
                    return null;
                }
            case 1:
                // readAll
                checkOpen();
                try
                {
                    StringBuilder result = new StringBuilder( "" );
                    String line = m_reader.readLine();
                    while( line != null )
                    {
                        result.append( line );
                        line = m_reader.readLine();
                        if( line != null )
                        {
                            result.append( "\n" );
                        }
                    }
                    if (this.doNotWrapUtf8)
                    {
                        return new Object[] { result.toString().getBytes(StandardCharsets.UTF_8) };	
                    }
                    return new Object[] { result.toString() };
                }
                catch( IOException e )
                {
                    return null;
                }
            case 2:
                // close
                close();
                return null;
            case 3:
                // read
                checkOpen();
                try
                {
                    int count = optInt( args, 0, 1 );
                    if( count <= 0 || count >= 1024 * 16 )
                    {
                        throw new LuaException( "Count out of range" );
                    }
                    if (this.doNotWrapUtf8)
                    {
                    	final StringBuilder builder = new StringBuilder(count);
                    	final char[] sbuf = new char[1];
                		// read character by character
                		// maybe we can tune the performance by reading count characters and testing for surrogates, then reading more....
                    	// but the m_reader is already fast (buffered reader)
                    	while (count > 0)
                    	{
                    		if (m_reader.read(sbuf, 0, 1) == 0)
                    		{
                    			// end of file reached
                    			break;
                    		}
                    		builder.append(sbuf[0]);
                    		if (Character.isLowSurrogate(sbuf[0]))
                    		{
                    			throw new LuaException("Invalid utf encoding (low surrogate without high surrogate)");
                    		}
                    		if (Character.isHighSurrogate(sbuf[0]))
                    		{
                    			if (m_reader.read(sbuf, 0, 1) == 0)
                        		{
                        			throw new LuaException("Invalid utf encoding (high surrogate and end of file)");
                        		}
                    			if (!Character.isLowSurrogate(sbuf[0]))
                    			{
                        			throw new LuaException("Invalid utf encoding (high surrogate without low surrogate)");
                    			}
                        		builder.append(sbuf[0]);
                    		}
                    		count--;
                    	}
                    	return new Object[] { builder.toString().getBytes(StandardCharsets.UTF_8) };
                    }
                    char[] bytes = new char[ count ];
                    count = m_reader.read( bytes );
                    if( count < 0 ) return null;
                    String str = new String( bytes, 0, count );
                    return new Object[] { str };
                }
                catch( IOException e )
                {
                    return null;
                }
            default:
                return null;
        }
    }
}
