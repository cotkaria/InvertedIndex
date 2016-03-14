package com.invertedIndexer.adobe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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

import com.invertedIndexer.adobe.types.MapFileToOccurences;
import com.invertedIndexer.adobe.types.MapFileToWordOccurences;
import com.invertedIndexer.adobe.types.MapWordToFileOccurrences;
import com.invertedIndexer.adobe.types.MapWordToOccurences;
import com.invertedIndexer.adobe.types.MapFileToWordOccurencesEntry;

/**
 * @author Cotkaria
 * Class that contains application logic: 
 * -Files indexing (get the number of occurrences for each word, ignoring stop words)
 * -Word stemming (apply porter stemming algorithm for different languages)
 * -Word search (support for multiple words as well)
 */
public class InverseIndexer
{
	// 1. Go through each word (what is a word? a group of alphabetic characters
	// separated by specified separators: whitespace, digits and punctuation
	// marks, i.e. auto-enable)
	// 2. Make the word lower-case
	// 3. Filter the stop words
	// 4. Apply the stemming algorithm
	// 5. Add them to the document map

	
	private final static String EXTENSION_TXT = "txt";
	private final static String EXTENSION_PDF = "pdf";
	private final static String EXTENSION_DOC = "doc";
	private final static String EXTENSION_DOCX = "docx";

	private static final Pattern WORDS_PATTERN = Pattern.compile("[A-Za-z']+");
	private static final Pattern STOP_WORDS = Pattern.compile("(?:^\\s*)(\\w+).*");

	private List<String> mStopWords;
	private List<String> mIndexedFiles;
	
	private MapWordToFileOccurrences mInverseIndex;
	private Stemmer mStemmer;

	private SimpleStringProperty mCurrentlyIndexedFile;
	private boolean mIsCancelled;
	
	public InverseIndexer(ALGORITHM language)
	{
		mStemmer = new SnowballStemmer(language);
		mCurrentlyIndexedFile = new SimpleStringProperty();
		resetIndexData();
	}
	
	private void resetIndexData()
	{
		mInverseIndex = new MapWordToFileOccurrences();
		mStopWords = new ArrayList<String>();
		mIndexedFiles = new ArrayList<String>();
		mIsCancelled = false;
	}

	public List<MapFileToWordOccurencesEntry> findWithCount(String text)
	{
		MapFileToWordOccurences totalResults = findText(text);
		List<MapFileToWordOccurencesEntry> resultSet = getSortedResultsWithCount(totalResults);
		return resultSet;
	}
	
	public List<String> find(String text)
	{
		List<MapFileToWordOccurencesEntry> resultSet = findWithCount(text);
		
		List<String> results = new ArrayList<String>();
		for(MapFileToWordOccurencesEntry entry: resultSet)
		{
			results.add(entry.getKey());
		}		
		return results;
	}
	
	/**
	 * @param text: the word(s) to be searched, separated by white spaces
	 * @return a map containing the files in which the words are found, along with the number of occurrences in each file
	 */
	private MapFileToWordOccurences findText(String text)
	{
		MapFileToWordOccurences fileOccurences = new MapFileToWordOccurences();
		
		Matcher matcher = WORDS_PATTERN.matcher(text);
		while (matcher.find())
		{
			int start = matcher.start();
			int end = matcher.end();
			if (end - start > 1)
			{
				String word = text.substring(start, end).toLowerCase();
				if(!mStopWords.contains(word))
				{
					MapFileToOccurences wordMap = findWordOccurences(word);
					if(wordMap != null)
					{
						for (String fileName : mIndexedFiles)
						{
							if(!fileOccurences.containsKey(fileName))
							{
								MapWordToOccurences occurences = new MapWordToOccurences();
								fileOccurences.put(fileName, occurences);
							}
							
							int wordOccurences = 0;
							if(wordMap.containsKey(fileName))
							{
								wordOccurences = wordMap.get(fileName);
							}

							MapWordToOccurences occurences = fileOccurences.get(fileName); 
							occurences.put(word, wordOccurences);
						}
					}
				}
			}
		}
		return fileOccurences;
	}
	
	private MapFileToOccurences findWordOccurences(String word)
	{
		String stemmedWord = mStemmer.stem(word).toString().toLowerCase();
		MapFileToOccurences wordMap = mInverseIndex.get(stemmedWord);
		return wordMap;
	}
	
	private List<MapFileToWordOccurencesEntry> getSortedResultsWithCount(MapFileToWordOccurences totalResults)
	{
		List<MapFileToWordOccurencesEntry> entries = new ArrayList<MapFileToWordOccurencesEntry>();
		totalResults.entrySet().forEach(entry ->
		{
			entries.add(new MapFileToWordOccurencesEntry(entry.getKey(), entry.getValue()));
		});
		entries.removeIf(entry ->
		{
			int total = entry.getValue().entrySet().stream().mapToInt(Map.Entry<String, Integer>::getValue).sum();
			return (total == 0);
		});
		Collections.sort(entries,
				(MapFileToWordOccurencesEntry e1, MapFileToWordOccurencesEntry e2) -> 
		{
			MapWordToOccurences map1 = e1.getValue();
			MapWordToOccurences map2 = e2.getValue();
			int total1 = map1.entrySet().stream().mapToInt(Map.Entry<String, Integer>::getValue).sum();
			int total2 = map2.entrySet().stream().mapToInt(Map.Entry<String, Integer>::getValue).sum();
			return total2 - total1;
		});
		
		return entries;
	}

	
	/**
	 * @param docsDirectory
	 * @param stopWordsFile
	 * @throws Exception
	 */
	public void index(File docsDirectory, File stopWordsFile) throws Exception
	{
		resetIndexData();
		
		readStopWords(stopWordsFile);
		if (docsDirectory != null)
		{
			if (docsDirectory.isDirectory())
			{
				for (File file : docsDirectory.listFiles())
				{
					if(mIsCancelled)
					{
						break;
					}
					
					String fileName = file.getName();
					mCurrentlyIndexedFile.set(fileName);//set observableValue 
					
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
						System.err.println("InverseIndexer::index() ignoring unsupported file type: "
										+ file);
						break;
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

	/**
	 * @param file
	 * Method that parses and performs indexing for simple txt files
	 */
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

	
	/**
	 * @param file
	 * Method that parses and performs indexing for PDF files
	 */
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

	/**
	 * @param file
	 * Method that parses and performs indexing for Word docs
	 */
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

	/**
	 * @param text
	 * @param docKey
	 * This is core method for text indexing, called from the methods specialized for different file types
	 */
	public void indexText(String text, String docKey)
	{
		Matcher matcher = WORDS_PATTERN.matcher(text);
		while (matcher.find())	//here we go through each word in the text
		{
			int start = matcher.start();
			int end = matcher.end();
			if (end - start > 1)	//ignore one-letter word as they are definitely stop words
			{
				String word = text.substring(start, end).toLowerCase();
				if(mStopWords.contains(word) == false)	//ignore stop words
				{
					word = mStemmer.stem(word).toString();	//apply stemming algorithm
					addWordToIndexer(word, docKey);	//add word to Index Map
				}
			}
		}
		//keep the names of the indexed files in the list
		if(!mIndexedFiles.contains(docKey))
		{
			mIndexedFiles.add(docKey);
		}
	}

	private void addWordToIndexer(String word, String docKey)
	{
		if (mInverseIndex.containsKey(word) == false)
		{
			mInverseIndex.put(word, new MapFileToOccurences());
		}	
		MapFileToOccurences textMap = mInverseIndex.get(word);
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
	
	public void cancelIndex()
	{
		mIsCancelled = true;
	}
}
