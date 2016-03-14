package com.invertedIndexer.adobe;
import java.awt.Desktop;
import java.io.*;
import java.util.List;

import com.invertedIndexer.adobe.types.MapFileToWordOccurencesEntry;

import opennlp.tools.stemmer.snowball.SnowballStemmer.ALGORITHM;
import javafx.application.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;

/**
 * @author Cotkaria
 *
 */

public class MainWindow extends Application
{
	public enum IndexerLanguage
	{
		ENGLISH,
		ROMANIAN
	}
	
	private static final String RESOURCES_PATH = "layouts/";
	private static final String CONFIG_DIALOG_PATH = RESOURCES_PATH + "InvertedIndex.fxml";
	private static Stage mStage;
	
	private InverseIndexer mEnglishIndexer = new InverseIndexer(ALGORITHM.ENGLISH);
	private InverseIndexer mRomanianIndexer = new InverseIndexer(ALGORITHM.ROMANIAN);
	
	Task<Void> mIndexTask = null;
	private Alert mAlert = null;
	
	public static void main(String[] args)
	{
		System.out.println("main()");
		Application.launch(args);
	}
	
	@Override
	public void start(Stage stage) throws Exception 
	{
		System.out.println("start()");

		mStage = stage;
		mStage.setTitle("InvertedIndexerApp");
		mStage.setResizable(false);
		
		InvertedIndexerController dialogController = (InvertedIndexerController)loadScene(CONFIG_DIALOG_PATH);
		if(dialogController != null)
		{
			dialogController.setMain(this);
		}
		else
		{
			System.out.println("Could not initialize the Configuration Dialog.");
		}
	}
	public static Stage getStage()
	{
		return mStage;
	}
	
	private Object loadScene(String scenePath)
	{
		Parent root=null;
		FXMLLoader loader;
		try 
		{
			 loader = new FXMLLoader();
			 loader.setBuilderFactory(new JavaFXBuilderFactory());
			 loader.setLocation(getClass().getResource(scenePath));
			 InputStream inputStream = getClass().getResourceAsStream(scenePath);
			 
			 if(inputStream != null)
			 {
				 root = (Parent)loader.load(inputStream);
				 
				 Scene scene = new Scene(root);
				 mStage.setScene(scene); 
				 mStage.show();
				 
			 	return loader.getController();
			 }
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}
		
	/**
	 * @param stopWordsFile (file containing words to ignore while indexing)
	 * @param docsFolder (folder containing all files to be indexed)
	 * @param language (selected language for indexing)
	 * Method performs file indexing with given parameters
	 */
	public void index(String stopWordsFile, String docsFolder, IndexerLanguage language)
	{
		InverseIndexer indexer = getIndexer(language);
		
		mIndexTask = new Task<Void>()
		{
			@Override
			protected Void call() throws Exception
			{
				Platform.runLater(() ->
				{
					showIndexingPopup(indexer);
				});
				
				System.out.println("Indexing started!");
				try
				{
					indexer.index(new File(docsFolder), new File(stopWordsFile));
				}
				catch (Exception e)
				{
					Platform.runLater(() ->
					{
						DialogHelper.showErrorPopup(e.getMessage());
					});
				}
				System.out.println("Indexing finished!");
				Platform.runLater(() ->
				{
					closeIndexingPopup();
				});
				return null;
			}
		};
		
		new Thread(mIndexTask).start();
	}
		
	/**
	 * @param searchedWords (one or more words, separated by white space, to be searched for)
	 * @param language (selected language for indexing files)
	 * @return number of occurrences for every word in all indexed files
	 */
	public List<MapFileToWordOccurencesEntry> search(String searchedWords, IndexerLanguage language)
	{
		return getIndexer(language).findWithCount(searchedWords);
	}
	
	
	private InverseIndexer getIndexer(IndexerLanguage language)
	{
		InverseIndexer indexer = null;
		switch(language)
		{
		case ROMANIAN:
			indexer = mRomanianIndexer;
			break;
		case ENGLISH:
		default:
			indexer = mEnglishIndexer;
			break;
		}
		return indexer;
	}
		
	/**
	 * @param indexer
	 * Method that displays Pop-Up during files indexing
	 */
	private void showIndexingPopup(InverseIndexer indexer)
	{
		mAlert = new Alert(AlertType.NONE, "", ButtonType.CANCEL);
		mAlert.setTitle("Processing");
		mAlert.setHeaderText("Please be patient while indexing!");
		indexer.getCurrentlyIndexedFile().addListener(new ChangeListener<String>()
		{
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue,
					String newValue)
			{
				Platform.runLater(() -> 
				{
					mAlert.setContentText("Indexing: " + newValue);
				});
			}
		});
		mAlert.setGraphic(new ProgressIndicator(-1));
		mAlert.setOnCloseRequest(event ->
		{
			indexer.cancelIndex();
		});
		mAlert.showAndWait()
			.filter(response -> response == ButtonType.CANCEL)
			.ifPresent(response -> 
			{
				indexer.cancelIndex();
			});
	}
	
	private void closeIndexingPopup()
	{
		if(mAlert != null)
		{
			mAlert.close();
		}
	}
		
	/**
	 * @param filePath
	 * method that opens the given file
	 */
	public void openFile(String filePath)
	{
		File file = new File(filePath);
		if(file.exists())
		{
			try
			{
				Desktop.getDesktop().open(file);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
