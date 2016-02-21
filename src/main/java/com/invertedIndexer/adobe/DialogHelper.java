package com.invertedIndexer.adobe;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class DialogHelper
{
	public static void showErrorPopup(String message)
	{
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText("An error has occured");
		alert.setContentText(message);

		alert.showAndWait();
	}
}
