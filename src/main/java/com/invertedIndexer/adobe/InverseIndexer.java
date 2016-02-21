package com.invertedIndexer.adobe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableStringValue;
import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer.ALGORITHM;

import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

public class InverseIndexer
{
	// 1. Go through each word (what is a word? a group of alphabetic characters
	// separated by specified separators: whitespace, digits and punctuation
	// marks)
	// auto-enable
	// 2. Make the word lower-case
	// 3. Filter the stop words
	// 4. Apply the stemming algorithm
	// 5. Add them to the document map

	// Map<String, Map<String, int>> index;
	// index["retriev"]["document1"]++;

	private final static String EXTENSION_TXT = "txt";
	private final static String EXTENSION_PDF = "pdf";
	private final static String EXTENSION_DOC = "doc";
	private final static String EXTENSION_DOCX = "docx";

	private static final Pattern WORDS = Pattern.compile("[A-Za-z']+");
	private static final Pattern STOP_WORDS = Pattern.compile("(?:^\\s*)(\\w+).*");

	private List<String> mStopWords;
	private List<String> mSearchedFiles;
	
	private Map<String, Map<String, Integer>> mInverseIndex;
	private Stemmer mStemmer;

	private SimpleStringProperty mCurrentlyIndexedFile;
	
	public InverseIndexer(ALGORITHM language)
	{
		mInverseIndex = new HashMap<String, Map<String, Integer>>();
		mStemmer = new SnowballStemmer(language);
		mStopWords = new ArrayList<String>();
		mSearchedFiles = new ArrayList<String>();
		mCurrentlyIndexedFile = new SimpleStringProperty();
	}

	public List<Map.Entry<String, Map<String, Integer>>> findWithCount(String text)
	{
		Map<String, Map<String, Integer>> totalResults = findText(text);
		List<Map.Entry<String, Map<String, Integer>>> resultSet = getSortedResultsWithCount(totalResults);
		return resultSet;
	}
	
	public List<String> find(String text)
	{
		List<Map.Entry<String, Map<String, Integer>>> resultSet = findWithCount(text);
		
		List<String> results = new ArrayList<String>();
		for(Map.Entry<String, Map<String, Integer>> entry: resultSet)
		{
			results.add(entry.getKey());
		}
		
		return results;
	}
	
	private Map<String, Map<String, Integer>> findText(String text)
	{
		Map<String, Map<String, Integer>> fileOccurences = new HashMap<String, Map<String, Integer>>();
		
		Matcher matcher = WORDS.matcher(text);
		while (matcher.find())
		{
			int start = matcher.start();
			int end = matcher.end();
			if (end - start > 1)
			{
				String word = text.substring(start, end).toLowerCase();
				if(!mStopWords.contains(word))
				{
					Map<String, Integer> wordMap = findWordInternal(word);
					if(wordMap != null)
					{
						for (String fileName : mSearchedFiles)
						{
							if(!fileOccurences.containsKey(fileName))
							{
								Map<String, Integer> occurences = new HashMap<String, Integer>();
								fileOccurences.put(fileName, occurences);
							}
							
							Map<String, Integer> occurences = fileOccurences.get(fileName); 
							int wordOccurences = 0;
							if(wordMap.containsKey(fileName))
							{
								wordOccurences = wordMap.get(fileName);
							}
							occurences.put(word, wordOccurences);
						}
					}
				}
			}
		}
		return fileOccurences;
	}
	
	private Map<String, Integer> findWordInternal(String word)
	{
		String stemmedWord = mStemmer.stem(word).toString().toLowerCase();
		Map<String, Integer> wordMap = mInverseIndex.get(stemmedWord);
		return wordMap;
	}
	
	private List<Map.Entry<String, Map<String, Integer>>> getSortedResultsWithCount(Map<String, Map<String, Integer>> totalResults)
	{
		List<Map.Entry<String, Map<String, Integer>>> entries = new ArrayList<Map.Entry<String, Map<String, Integer>>>();
		entries.addAll(totalResults.entrySet());
		Collections.sort(entries,
				(Map.Entry<String, Map<String, Integer>> e1, Map.Entry<String, Map<String, Integer>> e2) -> 
		{
			Map<String, Integer> map1 = e1.getValue();
			Map<String, Integer> map2 = e2.getValue();
			int total1 = map1.entrySet().stream().mapToInt(Map.Entry<String, Integer>::getValue).sum();
			int total2 = map2.entrySet().stream().mapToInt(Map.Entry<String, Integer>::getValue).sum();
			return total2 - total1;
		});
		entries.removeIf(entry ->
		{
			int total = entry.getValue().entrySet().stream().mapToInt(Map.Entry<String, Integer>::getValue).sum();
			return (total == 0);
		});
		
		return entries;
	}

	public void index(File docsDirectory, File stopWordsFile) throws Exception
	{
		mStopWords.clear();
		mSearchedFiles.clear();
		mInverseIndex.clear();
		
		readStopWords(stopWordsFile);
		if (docsDirectory != null)
		{
			if (docsDirectory.isDirectory())
			{
				for (File file : docsDirectory.listFiles())
				{
					String fileName = file.getName();
					mCurrentlyIndexedFile.set(fileName);
					
					boolean isValidFile = true;
					
					String extension = FilenameUtils.getExtension(file.getName());
					switch (extension)
					{
					case EXTENSION_TXT:
						indexFileTxt(file);
						break;
					case EXTENSION_PDF:
						indexFilePdf(file);
						break;
					case EXTENSION_DOC:
					case EXTENSION_DOCX:
						indexFileDocx(file);
						break;
					default:
						isValidFile = false;
						System.err.println("InverseIndexer::index() ignoring unsupported file type: "
										+ file);
						break;
					}
					
					if(isValidFile)
					{
						mSearchedFiles.add(fileName);
					}
				}
			}
			else
			{
				System.err.println("InverseIndexer::index() docsDirectory is not a directory: "
						+ docsDirectory);
			}
		}
		else
		{
			System.err.println("InverseIndexer::index() docsDirectory is null.");
		}
	}

	private void indexFileTxt(File file)
	{
		String fileName = FilenameUtils.getName(file.getPath());
		try (BufferedReader br = new BufferedReader(new FileReader(file)))
		{
			String currentLine = null;
			while ((currentLine = br.readLine()) != null)
			{
				indexText(currentLine, fileName);
			}
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
	}

	private void indexFilePdf(File file)
	{
		PDFParser parser = null;
		PDDocument pdDoc = null;
		COSDocument cosDoc = null;
		PDFTextStripper pdfStripper;

		try
		{
			parser = new PDFParser(new FileInputStream(file));
			parser.parse();
			cosDoc = parser.getDocument();
			pdfStripper = new PDFTextStripper();
			pdDoc = new PDDocument(cosDoc);
			
			String text = pdfStripper.getText(pdDoc);
			String fileName = FilenameUtils.getName(file.getPath());
			indexText(text, fileName);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (pdDoc != null)
				{
					pdDoc.close();
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			
			try
			{
				if (cosDoc != null)
				{
					cosDoc.close();
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			
			if (parser != null)
			{
				parser.clearResources();
			}
		}
	}

	private void indexFileDocx(File file)
	{
		try (FileInputStream fis = new FileInputStream(file.getAbsolutePath());
				XWPFDocument document = new XWPFDocument(fis))
		{
			String fileName = FilenameUtils.getName(file.getPath());

			List<XWPFParagraph> paragraphs = document.getParagraphs();
			for (XWPFParagraph para : paragraphs)
			{
				indexText(para.getText(), fileName);
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

	public void indexText(String text, String docKey)
	{
		Matcher matcher = WORDS.matcher(text);
		while (matcher.find())
		{
			int start = matcher.start();
			int end = matcher.end();
			if (end - start > 1)
			{
				String word = text.substring(start, end).toLowerCase();
				if(mStopWords.contains(word) == false)
				{
					word = mStemmer.stem(word).toString();
					addWordToIndexer(word, docKey);
				}
			}
		}
	}

	private void addWordToIndexer(String word, String docKey)
	{
		if (mInverseIndex.containsKey(word) == false)
		{
			mInverseIndex.put(word, new HashMap<String, Integer>());
		}
		Map<String, Integer> textMap = mInverseIndex.get(word);
		if (textMap.containsKey(docKey) == false)
		{
			textMap.put(docKey, 0);
		}
		int oldValue = textMap.get(docKey);
		textMap.put(docKey, oldValue + 1);
	}

	private void readStopWords(File configFile) throws Exception
	{
		setStopWordsList(getStopWords(configFile));
	}
	
	public void setStopWordsList(List<String> stopWords)
	{
		mStopWords.clear();
		for(String stopWord: stopWords)
		{
			mStopWords.add(stopWord.toLowerCase());
		}
	}
	
	public ObservableStringValue getCurrentlyIndexedFile()
	{
		return mCurrentlyIndexedFile;
	}
	
	private static List<String> getStopWords(File filepath) throws Exception
	{
		List<String> stopWords = new ArrayList<String>();
		try (BufferedReader br = new BufferedReader(new FileReader(filepath)))
		{
			String currentLine = null;
			while ((currentLine = br.readLine()) != null)
			{
				if (!currentLine.isEmpty())
				{
					Matcher m = STOP_WORDS.matcher(currentLine);
					if (m.matches())
					{
						stopWords.add(m.group(1));
					}
				}
			}
		}
		return stopWords;
	}
}
