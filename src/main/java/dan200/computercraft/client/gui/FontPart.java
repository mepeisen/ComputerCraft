package dan200.computercraft.client.gui;

import net.minecraft.util.ResourceLocation;

public class FontPart {

	private final ResourceLocation font;
    private final int fontHeight;
    private final int fontWidth;
    private final double texHeight;
    private final double texWidth;
    private final int begin;
    private final int end;
    private final int charsPerLine;
    
    protected FontPart(ResourceLocation font, int fontHeight, int fontWidth, int begin, int end, int charsPerLine, double texWidth, double texHeight) {
    	this.font = font;
		this.fontHeight = fontHeight;
		this.fontWidth = fontWidth;
		this.charsPerLine = charsPerLine;
		this.texWidth = texWidth;
		this.texHeight = texHeight;
		this.begin = begin;
		this.end = end;
	}

	public ResourceLocation font()
    {
    	return this.font;
    }
    
	public int fontHeight()
    {
    	return this.fontHeight;
    }
    
	public int fontWidth()
    {
    	return this.fontWidth;
    }
    
	public int charsPerLine()
    {
    	return this.charsPerLine;
    }
    
	public double texWidth()
    {
    	return texWidth;
    }
    
	public double texHeight()
    {
    	return texHeight;
    }

	public int begin() {
		return begin;
	}

	public int end() {
		return end;
	}

}
