package com.invertedIndexer.adobe.types;

import javafx.util.Pair;

public class MapFileToWordOccurencesEntry extends Pair<String, MapWordToOccurences>
{
	public MapFileToWordOccurencesEntry(String key, MapWordToOccurences value)
	{
		super(key, value);
	}
}
