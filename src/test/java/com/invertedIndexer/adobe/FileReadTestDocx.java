package com.invertedIndexer.adobe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class FileReadTestDocx extends TestCase
{
	public FileReadTestDocx(String testName)
	{
		super(testName);
	}

	public static Test suite()
	{
		return new TestSuite(FileReadTestDocx.class);
	}
	
	public void testReadDocx()
	{
		readDocx(TestHelpers.getDocumentsFile("Earth.docx"));
	}
	
	private void readDocx(File file)
	{
		try (FileInputStream fis = new FileInputStream(file.getAbsolutePath()))
		{
			XWPFDocument document = new XWPFDocument(fis);
			List<XWPFParagraph> paragraphs = document.getParagraphs();
			for (XWPFParagraph para : paragraphs) 
			{
				System.out.println(para.getText());
			}
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
