package dan200.computercraft.client.gui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.core.filesystem.FileMount;
import dan200.computercraft.core.filesystem.JarMount;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

public class FontManager {
	
	private Map<String, FontDefinition> fonts = new HashMap<>();

	private TextureManager m_textureManager;
	
	public static final FontDefinition LEGACY = new FontDefinition(
			"LEGACY", 256, false,
			new FontPart(
					new ResourceLocation( "computercraft", "textures/gui/term_font.png" ),
					9, 6,
					0, 256,
					16,
					256.0, 256.0));
	
	public FontManager(TextureManager textureManager)
	{
		this.m_textureManager = textureManager;
		this.loadFonts();
	}
	
	public List<FontDefinition> getFonts()
	{
		return new ArrayList<>(fonts.values());
	}
	
	public FontDefinition get(String name)
	{
		return fonts.get(name);
	}
	
	public FontDefinition getLegacy()
	{
		return LEGACY;
	}
	
	private void loadFonts(IMount mount, String name)
	{
		final List<String> list = new ArrayList<>();
		try
		{
			mount.list("/", list);
			list.stream().filter(f -> f.endsWith(".properties")).forEach(f -> {
				final String fname = f.substring(0, f.length()-".properties".length());
				final String png = fname + ".png";
				final Properties props = new Properties();
				try (final InputStream is = mount.openForRead(f))
				{
					props.load(is);
					FontDefinition fd = null;
					if ("1".equals(props.getProperty("version")))
					{
						 fd = new FontDefinition(
									fname,
									Integer.parseInt(props.getProperty("maxChars")),
									"true".equalsIgnoreCase(props.getProperty("blending")),
									new FontPart(
											new ResourceLocation( "computercraft", "textures/gui/fonts/" + png ),
											Integer.parseInt(props.getProperty("fontHeight")),
											Integer.parseInt(props.getProperty("fontWidth")),
											0,
											Integer.parseInt(props.getProperty("maxChars")),
											Integer.parseInt(props.getProperty("charsPerLine")),
											Integer.parseInt(props.getProperty("texWidth")),
											Integer.parseInt(props.getProperty("texHeight"))
									
									));
					}
					else if ("2".equals(props.getProperty("version")))
					{
						final List<FontPart> parts = new ArrayList<>();
						int partcount = Integer.parseInt(props.getProperty("parts"));
						for (int partno = 0; partno < partcount; partno++) {
							parts.add(
									new FontPart(
											new ResourceLocation( "computercraft", "textures/gui/fonts/" + props.getProperty("texture_"+partno) ),
											Integer.parseInt(props.getProperty("fontHeight_"+partno)),
											Integer.parseInt(props.getProperty("fontWidth_"+partno)),
											Integer.parseInt(props.getProperty("start_"+partno)),
											Integer.parseInt(props.getProperty("end_"+partno)),
											Integer.parseInt(props.getProperty("charsPerLine_"+partno)),
											Integer.parseInt(props.getProperty("texWidth_"+partno)),
											Integer.parseInt(props.getProperty("texHeight_"+partno))
									));
						}

						fd = new FontDefinition(
									fname,
									Integer.parseInt(props.getProperty("maxChars")),
									"true".equalsIgnoreCase(props.getProperty("blending")),
									parts.toArray(new FontPart[parts.size()]));
					}
					else
					{
						ComputerCraft.log.error("font version " + fname + ":" + props.getProperty("version") + " not supported");
					}
					
					if (fd != null)
					{
						fd.getParts().forEach(fp -> m_textureManager.bindTexture( fp.font() ));
						fonts.put(fname, fd);
					}
				}
				catch (IOException | NullPointerException | NumberFormatException ex)
				{
					ComputerCraft.log.error("Error loading font " + fname + " from " + name, ex);
				}
			});
		}
		catch (IOException ex)
		{
			ComputerCraft.log.error("Error loading fonts from " + name, ex);
		}
	}
	
	private void loadFonts()
	{
		File codeDir = ComputerCraft.getDebugCodeDir( getClass() );
        if( codeDir != null )
        {
            File subResource = new File( codeDir, "assets/computercraft/textures/gui/fonts" );
            if( subResource.exists() )
            {
                IMount resourcePackMount = new FileMount( subResource, 0 );
                loadFonts(resourcePackMount, "dir:"+codeDir);
            }
        }
        
		final File jar = ComputerCraft.getContainingJar(getClass());
		if (jar != null)
		{
			try
			{
				final JarMount jarMount = new JarMount( jar, "assets/computercraft/textures/gui/fonts" );
				loadFonts(jarMount, "jar:"+jar);
			}
			catch (IOException ex)
			{
				ComputerCraft.log.error("Error loading fonts from jar:"+jar, ex);
			}
		}
		
        final File resourcePackDir = new File(Minecraft.getMinecraft().mcDataDir, "resourcepacks");
        if( resourcePackDir.exists() && resourcePackDir.isDirectory() )
        {
            String[] resourcePacks = resourcePackDir.list();
            for( String resourcePack1 : resourcePacks )
            {
                try
                {
                    File resourcePack = new File( resourcePackDir, resourcePack1 );
                    if( !resourcePack.isDirectory() )
                    {
                        // Mount a resource pack from a jar
                        IMount resourcePackMount = new JarMount( resourcePack, "assets/computercraft/textures/gui/fonts" );
                        loadFonts(resourcePackMount, "resourcePack:"+resourcePack);
                    }
                    else
                    {
                        // Mount a resource pack from a folder
                        File subResource = new File( resourcePack, "assets/computercraft/textures/gui/fonts" );
                        if( subResource.exists() )
                        {
                            IMount resourcePackMount = new FileMount( subResource, 0 );
                            loadFonts(resourcePackMount, "resourcePack:"+resourcePack);
                        }
                    }
                }
                catch( IOException e )
                {
                    // Ignore
                }
            }
        }
	}

}
