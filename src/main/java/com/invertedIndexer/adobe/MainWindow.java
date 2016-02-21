package com.invertedIndexer.adobe;
import java.awt.Desktop;
import java.io.*;
import java.util.List;
import java.util.Map;

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
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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
	
	private Alert mAlert = null;
	private boolean mIsIndexingFinished = false;
	
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
	
	public void index(String stopWordsFile, String docsFolder, IndexerLanguage language)
	{
		InverseIndexer indexer = getIndexer(language);
		showIndexingPopup(indexer);
		
		Task<Void> indexTask = new Task<Void>()
		{
			@Override
			protected Void call() throws Exception
			{
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
		new Thread(indexTask).start();
	}
	
	public List<Map.Entry<String, Map<String, Integer>>> search(String searchedWords, IndexerLanguage language)
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
	
	private void showIndexingPopup(InverseIndexer indexer)
	{
		mIsIndexingFinished = false;
		mAlert = new Alert(AlertType.INFORMATION);
		mAlert.setTitle("Loading");
		mAlert.initStyle(StageStyle.TRANSPARENT);
		mAlert.setHeaderText("Please be patient while indexing");
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
			if(mIsIndexingFinished == false)
			{
				event.consume();
			}
		});
		mAlert.show();
	}
	
	private void closeIndexingPopup()
	{
		if(mAlert != null)
		{
			mIsIndexingFinished = true;
			mAlert.close();
		}
	}
	
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
