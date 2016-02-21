package com.invertedIndexer.adobe;

import java.io.File;
import java.net.URL;

public class TestHelpers
{
	public static final String DOCS_FOLDER = "resources/documents/";
	
	public static File getDocumentsFolder()
	{
		File file = null;
		URL url = FileReadTestPdf.class.getResource(DOCS_FOLDER);
		if(url != null)
		{
			try
			{
				file = new File(url.getPath());
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return file;
	}
	
	public static File getDocumentsFile(String fileName)
	{
		File file = null;
		URL url = FileReadTestPdf.class.getResource(DOCS_FOLDER);
		if(url != null)
		{
			try
			{
				file = new File(url.getPath() + fileName);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return file;
	}
}
