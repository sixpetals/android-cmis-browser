package de.fmaul.android.cmis.utils;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import de.fmaul.android.cmis.CmisApp;
import de.fmaul.android.cmis.DocumentDetailsActivity;
import de.fmaul.android.cmis.ListCmisFeedActivity;
import de.fmaul.android.cmis.R;
import de.fmaul.android.cmis.ServerActivity;
import de.fmaul.android.cmis.asynctask.AbstractDownloadTask;
import de.fmaul.android.cmis.database.Database;
import de.fmaul.android.cmis.database.FavoriteDAO;
import de.fmaul.android.cmis.database.ServerDAO;
import de.fmaul.android.cmis.model.Server;
import de.fmaul.android.cmis.repo.CmisItem;
import de.fmaul.android.cmis.repo.CmisProperty;
import de.fmaul.android.cmis.repo.CmisRepository;

public class ActionUtils {

	public static void openDocument(final Activity contextActivity, final CmisItem item) {

		File content = item.getContent(contextActivity.getIntent().getStringExtra("workspace"));
		if (content != null && content.length() > 0 && content.length() == Long.parseLong(getContentFromIntent(contextActivity))){
			viewFileInAssociatedApp(contextActivity, content, item.getMimeType());
		} else {
			new AbstractDownloadTask(getRepository(contextActivity), contextActivity) {
				@Override
				public void onDownloadFinished(File contentFile) {
					if (contentFile != null && contentFile.exists()) {
						viewFileInAssociatedApp(contextActivity, contentFile, item.getMimeType());
					} else {
						displayError(contextActivity, R.string.error_file_does_not_exists);
					}
				}
			}.execute(item);
		}
	}
	
	
	public static void displayError(Activity contextActivity, int messageId) {
		Toast.makeText(contextActivity, messageId, Toast.LENGTH_LONG).show();
	}
	
	public static void displayError(Activity contextActivity, String messageId) {
		Toast.makeText(contextActivity, messageId, Toast.LENGTH_LONG).show();
	}
	
	private static void viewFileInAssociatedApp(Activity contextActivity, File tempFile, String mimeType) {
		Intent viewIntent = new Intent(Intent.ACTION_VIEW);
		Uri data = Uri.fromFile(tempFile);
		viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		viewIntent.setDataAndType(data, mimeType.toLowerCase());

		try {
			contextActivity.startActivity(viewIntent);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(contextActivity, R.string.application_not_available, Toast.LENGTH_SHORT).show();
		}
	}
	
	public static void shareDocument(final Activity contextActivity, final String workspace, final CmisItem item) {
		File content = item.getContent(workspace);
		if (item.getMimeType().length() == 0){
			shareFileInAssociatedApp(contextActivity, content, item);
		} else if (content != null && content.length() > 0 && content.length() == Long.parseLong(getContentFromIntent(contextActivity))) {
			shareFileInAssociatedApp(contextActivity, content, item);
		} else {
			new AbstractDownloadTask(getRepository(contextActivity), contextActivity) {
				@Override
				public void onDownloadFinished(File contentFile) {
						shareFileInAssociatedApp(contextActivity, contentFile, item);
				}
			}.execute(item);
		}
	}
	
	private static void shareFileInAssociatedApp(Activity contextActivity, File contentFile, CmisItem item) {
		Intent i = new Intent(Intent.ACTION_SEND);
		i.putExtra(Intent.EXTRA_SUBJECT, item.getTitle());
		if (contentFile != null && contentFile.exists()){
			i.putExtra(Intent.EXTRA_TEXT, item.getContentUrl());
			i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(contentFile));
			i.setType(item.getMimeType());
		} else {
			i.putExtra(Intent.EXTRA_TEXT, item.getSelfUrl());
			i.setType("text/plain");
		}
		contextActivity.startActivity(Intent.createChooser(i, "Share..."));
	}
	
	private static CmisRepository getRepository(Activity activity) {
		return ((CmisApp) activity.getApplication()).getRepository();
	}
	
	private static String getContentFromIntent(Activity activity) {
		return activity.getIntent().getStringExtra("contentStream");
	}
	
	
	public static void createFavorite(Activity activity, Server server, CmisItem item){
		Database database = Database.create(activity);
		FavoriteDAO favDao = new FavoriteDAO(database.open());
		long result;
		if (item.hasChildren()){
			result = favDao.insert(item.getTitle(), item.getDownLink(), server.getId(), "");
		} else {
			result = favDao.insert(item.getTitle(), item.getSelfUrl(), server.getId(), item.getMimeType());
		}
		database.close();
		
		if (result == -1){
			Toast.makeText(activity, "ERROR during Favorite Added", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(activity, "Favorite Added", Toast.LENGTH_LONG).show();
		}
		
	}
	
	public static void displayDocumentDetails(Activity activity, CmisItem doc) {
		displayDocumentDetails(activity, getRepository(activity).getServer(), doc);
	}
	
	public static void displayDocumentDetails(Activity activity, Server server, CmisItem doc) {
		Intent intent = new Intent(activity, DocumentDetailsActivity.class);

		ArrayList<CmisProperty> propList = new ArrayList<CmisProperty>(doc.getProperties().values());
		
		intent.putParcelableArrayListExtra("properties", propList);
		
		intent.putExtra("workspace", server.getWorkspace());
		intent.putExtra("title", doc.getTitle());
		intent.putExtra("mimetype", doc.getMimeType());
		intent.putExtra("objectTypeId", doc.getProperties().get("cmis:objectTypeId").getValue());
		intent.putExtra("baseTypeId", doc.getProperties().get("cmis:baseTypeId").getValue());
		intent.putExtra("contentUrl", doc.getContentUrl());
		intent.putExtra("self", doc.getSelfUrl());
		
		CmisProperty fileSize = doc.getProperties().get("cmis:contentStreamLength");
		if (fileSize != null) {
			intent.putExtra("contentStream",fileSize.getValue());
		}
		
		activity.startActivity(intent);
	}
	
	
}
