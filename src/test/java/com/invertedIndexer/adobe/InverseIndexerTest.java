package com.invertedIndexer.adobe;

import java.util.ArrayList;
import java.util.List;

import com.invertedIndexer.adobe.types.MapFileToWordOccurencesEntry;
import com.invertedIndexer.adobe.types.MapWordToOccurences;

import opennlp.tools.stemmer.snowball.SnowballStemmer.ALGORITHM;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class InverseIndexerTest extends TestCase
{
	public InverseIndexerTest(String testName)
	{
		super(testName);
	}

	public static Test suite()
	{
		return new TestSuite(InverseIndexerTest.class);
	}
	
	public void testWithSimpleWords()
	{
		InverseIndexer indexer = new InverseIndexer(ALGORITHM.ENGLISH);
		
		String textA = "Ana are mere";
		String docA = "DocA.txt";
		indexer.indexText(textA, docA);
		
		String textB = "Ana are ana";
		String docB = "DocB.txt";
		indexer.indexText(textB, docB);
		
		String word = "ana";
		List<String> results = indexer.find(word);
		assertEquals(2, results.size());
		if(results.size() == 2)
		{
			assertEquals(results.get(0), docB);
			assertEquals(results.get(1), docA);
		}
	}
	
	public void testWithStemmedWords()
	{
		InverseIndexer indexer = new InverseIndexer(ALGORITHM.ENGLISH);
		
		String textA = "Myself was being blackmailed by thyself";
		String docA = "DocA.txt";
		indexer.indexText(textA, docA);
		
		String textB = "Eurepides, the blackmailer, himself was blackmailing thyself himself myself";
		String docB = "DocB.txt";
		indexer.indexText(textB, docB);
		
		String word = "BLACKmail";
		List<String> results = indexer.find(word);
		assertEquals(2, results.size());
		if(results.size() == 2)
		{
			assertEquals(results.get(0), docB);
			assertEquals(results.get(1), docA);
		}
	}
	
	public void testWithComposedWords()
	{
		InverseIndexer indexer = new InverseIndexer(ALGORITHM.ENGLISH);
		
		String textA = "Myself was being auto-asphyxiated by thyself";
		String docA = "DocA.txt";
		indexer.indexText(textA, docA);
		
		String textB = "Eurepides, the blackmailer, dies from asphyxiation";
		String docB = "DocB.txt";
		indexer.indexText(textB, docB);
		
		String word = "asphyxi";
		List<String> results = indexer.find(word);
		assertEquals(2, results.size());
		if(results.size() == 2)
		{
			assertEquals(results.get(0), docB);
			assertEquals(results.get(1), docA);
		}
	}
	
	public void testWithStopWords()
	{
		InverseIndexer indexer = new InverseIndexer(ALGORITHM.ENGLISH);
		
		List<String> stopWords = new ArrayList<String>();
		stopWords.add("by");
		stopWords.add("was");
		indexer.setStopWordsList(stopWords);
		
		String textA = "Myself was being auto-asphyxiated by thyself";
		String docA = "DocA.txt";
		indexer.indexText(textA, docA);
		
		List<String> results1 = indexer.find("by");
		assertTrue(results1.isEmpty());
		
		List<String> results2 = indexer.find("was");
		assertTrue(results2.isEmpty());
		
		List<String> results3 = indexer.find("be");
		assertTrue(results3.size() == 1);
		if(results3.size() == 1)
		{
			assertEquals(results3.get(0), docA);
		}
	}
	
	public void testWithStemmedSearchWords()
	{
		InverseIndexer indexer = new InverseIndexer(ALGORITHM.ENGLISH);
		
		String textA = "Myself was being auto-asphyxiated by thyself through blackmailing, blackmailed";
		String docA = "DocA.txt";
		indexer.indexText(textA, docA);
		
		String textB = "Eurepides, the blackmailer, dies from asphyxiation";
		String docB = "DocB.txt";
		indexer.indexText(textB, docB);
		
		String word = "blackmailers";
		List<String> results = indexer.find(word);
		assertEquals(2, results.size());
		if(results.size() == 2)
		{
			assertEquals(results.get(0), docA);
			assertEquals(results.get(1), docB);
		}
	}
	
	public void testWithMultipleSearchWords()
	{
		InverseIndexer indexer = new InverseIndexer(ALGORITHM.ENGLISH);
		
		String textA = "Myself was being auto-asphyxiated by thyself through blackmailing, blackmailed";
		String docA = "DocA.txt";
		indexer.indexText(textA, docA);
		
		String textB = "Eurepides, the blackmailer, dies from asphyxiation";
		String docB = "DocB.txt";
		indexer.indexText(textB, docB);
		
		String word = "blackmailers asphyxiations";
		List<String> results = indexer.find(word);
		assertEquals(2, results.size());
		if(results.size() == 2)
		{
			assertEquals(results.get(0), docA);
			assertEquals(results.get(1), docB);
		}
	}
	
	public void testWithCountForMultipleSearchWords()
	{
		InverseIndexer indexer = new InverseIndexer(ALGORITHM.ENGLISH);
		
		String textA = "Myself was being auto-asphyxiated by thyself through blackmailing, blackmailed";
		String docA = "DocA.txt";
		indexer.indexText(textA, docA);
		
		String textB = "Eurepides, the blackmailer, dies from asphyxiation";
		String docB = "DocB.txt";
		indexer.indexText(textB, docB);
		
		String word1 = "blackmailers";
		String word2 = "asphyxiations";
		String composedWord = word1 + " " + word2;
		List<MapFileToWordOccurencesEntry> results = indexer.findWithCount(composedWord);
		assertEquals(2, results.size());
		if(results.size() == 2)
		{
			String fileName0 = results.get(0).getKey();
			String fileName1 = results.get(1).getKey();
			
			assertEquals(fileName0, docA);
			assertEquals(fileName1, docB);
			
			MapWordToOccurences wordToOccurrences0 = results.get(0).getValue();
			assertEquals(wordToOccurrences0.get(word1).intValue(), 2);
			assertEquals(wordToOccurrences0.get(word2).intValue(), 1);
			
			MapWordToOccurences wordToOccurrences1 = results.get(1).getValue();
			assertEquals(wordToOccurrences1.get(word1).intValue(), 1);
			assertEquals(wordToOccurrences1.get(word2).intValue(), 1);
		}
	}
	
	
}
