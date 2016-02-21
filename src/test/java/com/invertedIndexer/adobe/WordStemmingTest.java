package com.invertedIndexer.adobe;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer.ALGORITHM;

public class WordStemmingTest extends TestCase
{
    public WordStemmingTest( String testName )
    {
        super( testName );
    }

    public static Test suite()
    {
        return new TestSuite( WordStemmingTest.class );
    }

    public void testStemmingInEnglish()
    {
    	stemTestEn("exceptionally", "except");
    	stemTestEn("algebraic", "algebra");
    	stemTestEn("manipulating", "manipul");
    	stemTestEn("requirements", "requir");
    }
    
    private void stemTestEn(String actual, String expected)
    {
    	stemTest(ALGORITHM.ENGLISH, actual, expected);
    }
    
    public void testStemmingInRomanian()
    {
    	stemTestRo("abstractizăm", "abstractiz");
    	stemTestRo("absorbantă", "absorb");
    	stemTestRo("absolutului", "absol");
    	stemTestRo("absenţă", "absenţ");
    	stemTestRo("analiza", "analiz");
    }
    
    private void stemTestRo(String actual, String expected)
    {
    	stemTest(ALGORITHM.ROMANIAN, actual, expected);
    }
    
    private void stemTest(ALGORITHM alg, String actual, String expected)
    {
    	Stemmer stemmer = new SnowballStemmer(alg);
    	CharSequence stemmedWord = stemmer.stem(actual);
    	assertEquals(expected, stemmedWord.toString());
    }
}
