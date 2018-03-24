package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.shared.util.StringUtil;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class EncodedOutputHandle extends HandleGeneric
{
    private final BufferedWriter m_writer;
    
    private final boolean doNotWrapUtf8;

    public EncodedOutputHandle( BufferedWriter writer )
    {
        super( writer );
        this.m_writer = writer;
        doNotWrapUtf8 = false;
    }

    public EncodedOutputHandle( OutputStream stream )
    {
        this( stream, "UTF-8" );
    }

    public EncodedOutputHandle( OutputStream stream, String encoding )
    {
        this( makeWriter( stream, encoding ) );
    }

    public EncodedOutputHandle( BufferedWriter writer, boolean doNotWrapUtf8 )
    {
        super( writer );
        this.m_writer = writer;
        this.doNotWrapUtf8 = doNotWrapUtf8;
    }

    public EncodedOutputHandle( OutputStream stream, boolean doNotWrapUtf8 )
    {
        this( stream, "UTF-8", doNotWrapUtf8 );
    }

    public EncodedOutputHandle( OutputStream stream, String encoding, boolean doNotWrapUtf8 )
    {
        this( makeWriter( stream, encoding ), doNotWrapUtf8 );
    }

    private static BufferedWriter makeWriter( OutputStream stream, String encoding )
    {
        if( encoding == null ) encoding = "UTF-8";
        OutputStreamWriter streamWriter;
        try
        {
            streamWriter = new OutputStreamWriter( stream, encoding );
        }
        catch( UnsupportedEncodingException e )
        {
            streamWriter = new OutputStreamWriter( stream );
        }
        return new BufferedWriter( streamWriter );
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return new String[] {
            "write",
            "writeLine",
            "flush",
            "close",
        };
    }

    @Override
    public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] args ) throws LuaException
    {
        switch( method )
        {
            case 0:
            {
                // write
                checkOpen();
                String text;
                if( args.length > 0 && args[ 0 ] != null )
                {
                    text = args[ 0 ].toString();
                }
                else
                {
                    text = "";
                }
                try
                {
                	if (this.doNotWrapUtf8)
                	{
                		// we assume that this is originally an LuaString wrapped by conversions from byte to char
                        m_writer.write( new String(StringUtil.encodeString(text), StandardCharsets.UTF_8) );
                	}
                	else
                	{
                		m_writer.write( text, 0, text.length() );
                	}
                    return null;
                }
                catch( IOException e )
                {
                    throw new LuaException( e.getMessage() );
                }
            }
            case 1:
            {
                // writeLine
                checkOpen();
                String text;
                if( args.length > 0 && args[ 0 ] != null )
                {
                    text = args[ 0 ].toString();
                }
                else
                {
                    text = "";
                }
                try
                {
                	if (this.doNotWrapUtf8)
                	{
                		// we assume that this is originally an LuaString wrapped by conversions from byte to char
                        m_writer.write( new String(StringUtil.encodeString(text), StandardCharsets.UTF_8) );
                	}
                	else
                	{
                		m_writer.write( text, 0, text.length() );
                	}
                    m_writer.newLine();
                    return null;
                }
                catch( IOException e )
                {
                    throw new LuaException( e.getMessage() );
                }
            }
            case 2:
                // flush
                checkOpen();
                try
                {
                    m_writer.flush();
                    return null;
                }
                catch( IOException e )
                {
                    return null;
                }
            case 3:
                // close
                close();
                return null;
            default:
                return null;
        }
    }
}
