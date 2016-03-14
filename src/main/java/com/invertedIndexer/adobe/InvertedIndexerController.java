package com.invertedIndexer.adobe;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import com.invertedIndexer.adobe.types.MapWordToOccurences;
import com.invertedIndexer.adobe.types.MapFileToWordOccurencesEntry;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.*;
import javafx.util.Callback;

public class InvertedIndexerController implements Initializable{

    @FXML
    private TextField folderPath;

    @FXML
    private Button indexBt;

    @FXML
    private TextField stopWordsPath;

    @FXML
    private TextField searchedWords;

    @FXML
    private Button browseStopWordsBt;

    @FXML
    private Button searchBt;

    @FXML
    private Button browseFolderBt;
    
    @FXML
    private ComboBox<String> languageCb;
    
    @FXML
    private TableView<MapFileToWordOccurencesEntry> resultsTable;
    
    private MainWindow mMainWindow;
    
    @Override
	public void initialize(URL location, ResourceBundle resources) 
    {
    	browseFolderBt.setOnAction(event -> onFolderBrowse());
    	browseStopWordsBt.setOnAction(event -> onFileBrowse());
    	indexBt.setOnAction(event ->index());
    	searchBt.setOnAction(event -> search());
    	
    	List<String> languages = Stream.of(MainWindow.IndexerLanguage.values())
    			.map(MainWindow.IndexerLanguage::name)
    			.collect(Collectors.toList());
    	languageCb.setItems(FXCollections.observableArrayList(languages));
    	if(languages.isEmpty() == false)
    	{
    		languageCb.getSelectionModel().select(0);
    	}
    	configureResultsTable();
	}
    
    private void configureResultsTable()
    {
    	resultsTable.setOnMouseClicked(event ->
		{
			if(event.getClickCount() > 1)
			{
				MapFileToWordOccurencesEntry selection = resultsTable.getSelectionModel().getSelectedItem();
				if(selection != null)
				{
					String folder = folderPath.getText();
					String fileName = selection.getKey();
					String filePath = folder + File.separator + fileName;
					mMainWindow.openFile(filePath);
				}
			}
		});
    }
	
    public void setMain(MainWindow main)
    {
    	mMainWindow = main;
    }
    
    private void index()
    {
    	if(mMainWindow != null)
    	{
    		mMainWindow.index(stopWordsPath.getText(), folderPath.getText(), getSelectedLanguage());
		}
    }
    
    private void search()
    {
    	if(mMainWindow != null && !searchedWords.getText().isEmpty())
    	{
    		List<MapFileToWordOccurencesEntry> results = mMainWindow.search(searchedWords.getText(), getSelectedLanguage());
    		if(!results.isEmpty())
    		{
    			addResultsToTable(results);
    		}
		}
    }
    
    private void addResultsToTable(List<MapFileToWordOccurencesEntry> results)
    {
		List<TableColumn<MapFileToWordOccurencesEntry, ?>> tableColumns = new ArrayList<>();
		
		TableColumn<MapFileToWordOccurencesEntry, String> fileNameCol = new TableColumn<>("Document name");
		fileNameCol.setCellValueFactory(new Callback<CellDataFeatures<MapFileToWordOccurencesEntry, String>, 
				ObservableValue<String>>() 
		{
			public ObservableValue<String> call(CellDataFeatures<MapFileToWordOccurencesEntry, String> p) 
			{
		         return new SimpleStringProperty(p.getValue().getKey());
		    }
		});
		tableColumns.add(fileNameCol);
		
		MapWordToOccurences wordOccurences = results.get(0).getValue();
		for(Map.Entry<String, Integer> entry: wordOccurences.entrySet())
		{
			String word = entry.getKey();
			TableColumn<MapFileToWordOccurencesEntry, Number> wordColumn = 
					new TableColumn<>(word);
			wordColumn.setCellValueFactory(new Callback<CellDataFeatures<MapFileToWordOccurencesEntry, Number>, 
    				ObservableValue<Number>>()
    		{
    			public ObservableValue<Number> call(CellDataFeatures<MapFileToWordOccurencesEntry, Number> p) 
    			{
    				int value = 0;
    				MapFileToWordOccurencesEntry result = p.getValue();
    				MapWordToOccurences wordHits = result.getValue();
    				if(wordHits != null)
    				{
    					if(wordHits.containsKey(word))
    					{
            				value = wordHits.get(word);
    					}
    				}
    		        return new SimpleIntegerProperty(value);
    		    }
    		});
			tableColumns.add(wordColumn);
		}
		
		resultsTable.getColumns().clear();
		resultsTable.getColumns().addAll(tableColumns);
		
		resultsTable.setItems(FXCollections.observableArrayList(results));
    }
    
    private void onFolderBrowse() 
	{
		folderPath.setText(selectDirectory(folderPath.getText()));
	}

    private void onFileBrowse() 
	{
		stopWordsPath.setText(selectFile(folderPath.getText()));
	}
    
	private String selectDirectory(String directoryPath)
	{
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("Choose directory");
		
		if (directoryPath.isEmpty() == false)
		{
			File currentFolder = new File(directoryPath);
			if (currentFolder.exists())
			{ 
				directoryChooser.setInitialDirectory(currentFolder);
			}
		}
		
		File selectedDirectory = directoryChooser.showDialog(MainWindow
				.getStage());
		if (selectedDirectory != null) 
		{
			directoryPath = selectedDirectory.getAbsolutePath();
		}
		return directoryPath;
	}
	
	private String selectFile(String filePath)
	{
		 FileChooser fileChooser = new FileChooser();
		 fileChooser.setTitle("Select StopWords file");
		 fileChooser.getExtensionFilters().addAll(
		         new FileChooser.ExtensionFilter("Text Files", "*.txt"));
//		         new FileChooser.ExtensionFilter("PDF files", "*.pdf"),
//		         new FileChooser.ExtensionFilter("Doc Files", "*.doc", "*.docx"));			 	
		 if (filePath.isEmpty() == false)
			{
				File currentFolder = new File(filePath);
				if (currentFolder.exists())
				{ 
					fileChooser.setInitialDirectory(currentFolder);
				}
			}
		 
		 File selectedFile = fileChooser.showOpenDialog(MainWindow.getStage());
		 if (selectedFile != null) {
			 filePath = selectedFile.getAbsolutePath();
		 }
		 return filePath;
	}
	
	private MainWindow.IndexerLanguage getSelectedLanguage()
	{
		String selection = languageCb.getSelectionModel().getSelectedItem();
		return MainWindow.IndexerLanguage.valueOf(selection);
	}
}
