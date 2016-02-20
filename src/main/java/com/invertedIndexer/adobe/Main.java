package com.invertedIndexer.adobe;
import java.io.*;

import org.apache.commons.io.FilenameUtils;



/**
 * Hello world!
 *
 */
public class Main 
{
	private final static String TXT_EXTENSION = "txt";
	
    public static void main( String[] args )
    {
    	//file separator
    	String folderPath = "C:\\Users\\Cotkaria\\Downloads\\homework\\resources";
    
    	try 
		{

			File logFilesDirectory = new File(folderPath);
			if (!logFilesDirectory.exists()) //should not be necessary to make this validation
			{
				folderPath = "Cannot find path \"" + folderPath + "\"";
			}
			else 
			{	
				File[] files = logFilesDirectory.listFiles();
				for (File f:files) 
				{
					if (!f.isDirectory())
					{
						if(FilenameUtils.isExtension(f.getName(), TXT_EXTENSION))
						{
							printFileLines(f);
						}
					}
				}
			}
		}
    	catch (Exception e){}
    	finally{}

	}
    private static void printFileLines(File filepath) throws IOException
	{	
		try (BufferedReader br = new BufferedReader(new FileReader(filepath)))
		{			
			String currentLine = null;
			while((currentLine = br.readLine())!=null) 
			{
				System.out.println(currentLine);	
			}				
		} 
	}
}
