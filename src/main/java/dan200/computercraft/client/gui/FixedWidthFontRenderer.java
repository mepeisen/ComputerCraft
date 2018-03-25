/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui;

import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.util.Palette;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;

public class FixedWidthFontRenderer
{
	
    public static ResourceLocation background = new ResourceLocation( "computercraft", "textures/gui/term_background.png" );

    public static final int FONT_HEIGHT = FontManager.LEGACY.getPart(' ').fontHeight(); // original values
    public static final int FONT_WIDTH = FontManager.LEGACY.getPart(' ').fontWidth(); // original values

    private TextureManager m_textureManager;

    public FixedWidthFontRenderer( TextureManager textureManager )
    {
        m_textureManager = textureManager;
    }

    private static void greyscaleify( double[] rgb )
    {
        Arrays.fill( rgb, ( rgb[0] + rgb[1] + rgb[2] ) / 3.0f );
    }

    private void drawChar( FontPart fd, BufferBuilder renderer, double x, double y, int index, int color, Palette p, boolean greyscale )
    {
        int column = (index - fd.begin()) % fd.charsPerLine();
        int row = (index - fd.begin()) / fd.charsPerLine();

        double[] colour = p.getColour( 15 - color );
        if(greyscale)
        {
            greyscaleify( colour );
        }
        float r = (float)colour[0];
        float g = (float)colour[1];
        float b = (float)colour[2];

        int xStart = 1 + column * (fd.fontWidth() + 2);
        int yStart = 1 + row * (fd.fontHeight() + 2);
        
        renderer.pos( x, y, 0.0 ).tex( xStart / fd.texWidth(), yStart / fd.texHeight() ).color( r, g, b, 1.0f ).endVertex();
        renderer.pos( x, y + FONT_HEIGHT, 0.0 ).tex( xStart / fd.texWidth(), (yStart + fd.fontHeight()) / fd.texHeight() ).color( r, g, b, 1.0f ).endVertex();
        renderer.pos( x + FONT_WIDTH, y, 0.0 ).tex( (xStart + fd.fontWidth()) / fd.texWidth(), yStart / fd.texHeight() ).color( r, g, b, 1.0f ).endVertex();
        renderer.pos( x + FONT_WIDTH, y, 0.0 ).tex( (xStart + fd.fontWidth()) / fd.texWidth(), yStart / fd.texHeight() ).color( r, g, b, 1.0f ).endVertex();
        renderer.pos( x, y + FONT_HEIGHT, 0.0 ).tex( xStart / fd.texWidth(), (yStart + fd.fontHeight()) / fd.texHeight() ).color( r, g, b, 1.0f ).endVertex();
        renderer.pos( x + FONT_WIDTH, y + FONT_HEIGHT, 0.0 ).tex( (xStart + fd.fontWidth()) / fd.texWidth(), (yStart + fd.fontHeight()) / fd.texHeight() ).color( r, g, b, 1.0f ).endVertex();
    }

    private void drawQuad( BufferBuilder renderer, double x, double y, int color, double width, Palette p, boolean greyscale )
    {
        double[] colour = p.getColour( 15 - color );
        if(greyscale)
        {
            greyscaleify( colour );
        }
        float r = (float)colour[0];
        float g = (float)colour[1];
        float b = (float)colour[2];

        renderer.pos( x, y, 0.0 ).color( r, g, b, 1.0f ).endVertex();
        renderer.pos( x, y + FONT_HEIGHT, 0.0 ).color( r, g, b, 1.0f ).endVertex();
        renderer.pos( x + width, y, 0.0 ).color( r, g, b, 1.0f ).endVertex();
        renderer.pos( x + width, y, 0.0 ).color( r, g, b, 1.0f ).endVertex();
        renderer.pos( x, y + FONT_HEIGHT, 0.0 ).color( r, g, b, 1.0f ).endVertex();
        renderer.pos( x + width, y + FONT_HEIGHT, 0.0 ).color( r, g, b, 1.0f ).endVertex();
    }

    private boolean isGreyScale( int colour )
    {
        return (colour == 0 || colour == 15 || colour == 7 || colour == 8);
    }

    public void drawStringBackgroundPart( int x, int y, TextBuffer backgroundColour, double leftMarginSize, double rightMarginSize, boolean greyScale, Palette p )
    {
        // Draw the quads
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder renderer = tessellator.getBuffer();
        renderer.begin( GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR );
        if( leftMarginSize > 0.0 )
        {
            int colour1 = "0123456789abcdef".indexOf( backgroundColour.charAt( 0 ) );
            if( colour1 < 0 || (greyScale && !isGreyScale(colour1)) )
            {
                colour1 = 15;
            }
            drawQuad( renderer, x - leftMarginSize, y, colour1, leftMarginSize, p, greyScale );
        }
        if( rightMarginSize > 0.0 )
        {
            int colour2 = "0123456789abcdef".indexOf( backgroundColour.charAt( backgroundColour.length() - 1 ) );
            if( colour2 < 0 || (greyScale && !isGreyScale(colour2)) )
            {
                colour2 = 15;
            }
            drawQuad( renderer, x + backgroundColour.length() * FONT_WIDTH, y, colour2, rightMarginSize, p, greyScale );
        }
        for( int i = 0; i < backgroundColour.length(); i++ )
        {
            int colour = "0123456789abcdef".indexOf( backgroundColour.charAt( i ) );
            if( colour < 0 || ( greyScale && !isGreyScale( colour ) ) )
            {
                colour = 15;
            }
            drawQuad( renderer, x + i * FONT_WIDTH, y, colour, FONT_WIDTH, p, greyScale );
        }
        GlStateManager.disableTexture2D();
        tessellator.draw();
        GlStateManager.enableTexture2D();
    }

    public void drawStringTextPart( int x, int y, TextBuffer s, TextBuffer textColour, boolean greyScale, Palette p )
    {
    	drawStringTextPart(FontManager.LEGACY, x, y, s, textColour, greyScale, p);
    }

    public void drawStringTextPart( FontDefinition fd, int x, int y, TextBuffer s, TextBuffer textColour, boolean greyScale, Palette p )
    {
        // Draw the quads
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder renderer = tessellator.getBuffer();
    	FontPart curPart = null;

    	for( int i = 0; i < s.length(); i++ )
        {
            // Draw char
            int index = s.codepointAt( i );
            if( index < 0 || index > fd.maxChars() )
            {
                index = (int)'?';
            }
            FontPart newPart = fd.getPart(index);
            if (newPart == null)
            {
            	// fall back to '?'
            	index = (int)'?';
            	newPart = fd.getPart(index);
            }
            
            // if newPart is null this font is corrupt... It does not know how to display simple quotion mark ('?')
            if (newPart != null)
            {
            	if (curPart != newPart)
            	{
            		if (curPart != null)
            		{
            	        tessellator.draw();
            		}
            		curPart = newPart;
            		// begin drawing using this font part
            		bindFont(fd, curPart);
            		renderer.begin( GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX_COLOR );
            	}
            }
    	            
            // Switch colour
            int colour = "0123456789abcdef".indexOf( textColour.charAt( i ) );
            if( colour < 0 || ( greyScale && !isGreyScale( colour ) ) )
            {
                colour = 0;
            }

            drawChar( curPart, renderer, x + i * FONT_WIDTH, y, index, colour, p, greyScale );
        }
    	
    	if (curPart != null)
    	{
    		// we did draw something. Finish it.
    		tessellator.draw();
    	}
    }

    public void drawString( TextBuffer s, int x, int y, TextBuffer textColour, TextBuffer backgroundColour, double leftMarginSize, double rightMarginSize, boolean greyScale, Palette p )
    {
    	drawString(FontManager.LEGACY, s, x, y, textColour, backgroundColour, leftMarginSize, rightMarginSize, greyScale, p);
    }

    public void drawString( FontDefinition fd, TextBuffer s, int x, int y, TextBuffer textColour, TextBuffer backgroundColour, double leftMarginSize, double rightMarginSize, boolean greyScale, Palette p )
    {
        // Draw background
        if( backgroundColour != null )
        {
            // Bind the background texture
            m_textureManager.bindTexture( background );

            // Draw the quads
            drawStringBackgroundPart( x, y, backgroundColour, leftMarginSize, rightMarginSize, greyScale, p );
        }
    
        // Draw text
        if( s != null && textColour != null )
        {
            // Draw the quads
            drawStringTextPart( fd, x, y, s, textColour, greyScale, p );
        }
    }

    public int getStringWidth(String s)
    {
        if(s == null)
        {
            return 0;
        }
        return s.length() * FONT_WIDTH;
    }

    public void bindFont()
    {
        bindFont(FontManager.LEGACY);
    }

    public void bindFont(FontDefinition fd)
    {
        bindFont(fd, fd.getPart(' '));
    }

    public void bindFont(FontDefinition fd, FontPart part)
    {
        m_textureManager.bindTexture( part.font() );
        GlStateManager.glTexParameteri( GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP );
        if (fd.isBlending())
        {
        	GlStateManager.enableBlend();
        	GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        }
    }
}
