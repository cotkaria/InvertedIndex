package com.invertedIndexer.adobe;

import java.io.File;
import java.io.FileInputStream;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class FileReadTestPdf extends TestCase
{
	public FileReadTestPdf(String testName)
	{
		super(testName);
	}

	public static Test suite()
	{
		return new TestSuite(FileReadTestPdf.class);
	}

	public void testReadPdf()
	{
		readPdf(TestHelpers.getDocumentsFile("Computer science.pdf"));
	}
	
	private void readPdf(File file)
	{
		PDFParser parser = null;
		PDDocument pdDoc = null;
		COSDocument cosDoc = null;
		PDFTextStripper pdfStripper;

		String parsedText;
		try
		{
			parser = new PDFParser(new FileInputStream(file));
			parser.parse();
			cosDoc = parser.getDocument();
			pdfStripper = new PDFTextStripper();
			pdDoc = new PDDocument(cosDoc);
			parsedText = pdfStripper.getText(pdDoc);
			System.out.println(parsedText);
		}
		catch (Exception e)
		{
			fail();
			e.printStackTrace();
			try
			{
				if (cosDoc != null)
					cosDoc.close();
				if (pdDoc != null)
					pdDoc.close();
			}
			catch (Exception e1)
			{
				e.printStackTrace();
			}
		}
	}
}
