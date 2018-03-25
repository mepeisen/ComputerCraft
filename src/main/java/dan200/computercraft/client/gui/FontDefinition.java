package dan200.computercraft.client.gui;

import java.util.List;

import scala.actors.threadpool.Arrays;

public class FontDefinition {

    private final int maxChars;
	private final String name;
	private final boolean blending;
	private final FontPart[] parts;
    
    protected FontDefinition(String name, int maxChars, boolean blending, FontPart... parts) {
		this.name = name;
		this.maxChars = maxChars;
		this.blending = blending;
		this.parts = parts;
	}
    
    public String name()
    {
    	return this.name;
    }
    
    @SuppressWarnings("unchecked")
	public List<FontPart> getParts()
    {
    	return Arrays.asList(this.parts);
    }
    
	public int maxChars()
    {
    	return this.maxChars;
    }

	public boolean isBlending() {
		return blending;
	}
	
	public FontPart getPart(int codepoint)
	{
		for (final FontPart part : this.parts)
		{
			if (codepoint >= part.begin() && codepoint <= part.end())
			{
				return part;
			}
		}
		return null;
	}

}
