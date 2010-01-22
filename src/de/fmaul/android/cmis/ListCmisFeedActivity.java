package de.fmaul.android.cmis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import de.fmaul.android.cmis.repo.CmisItem;
import de.fmaul.android.cmis.repo.CmisRepository;
import de.fmaul.android.cmis.repo.QueryType;

public class ListCmisFeedActivity extends ListActivity {

	/**
	 * Contains the current connection information and methods to access the
	 * CMIS repository.
	 */
	CmisRepository repository;

	/**
	 * The currently selected {@link QueryType}.
	 */
	QueryType queryType = QueryType.FULLTEXT;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initWindow();

		if (repository == null) {
			Prefs prefs = new Prefs(this);
			repository = CmisRepository.create(prefs);
		}

		if (activityIsCalledWithSearchAction()) {
			doSearchWithIntent(getIntent());
		} else {
			// display the feed that is passed in through the intent
			String feed = getFeedFromIntent();
			displayFeedInListView(feed);
		}
	}

	/**
	 * The type of the query is passed by the SearchManager through a bundle in
	 * the search intent.
	 * 
	 * @param intent
	 * @return
	 */
	private QueryType getQueryTypeFromIntent(Intent intent) {
		Bundle appData = intent.getBundleExtra(SearchManager.APP_DATA);
		if (appData != null) {
			String queryType = appData.getString(QueryType.class.getName());
			return QueryType.valueOf(queryType);
		}
		return QueryType.FULLTEXT;
	}

	/**
	 * Tests if this activity is called with a Search intent.
	 * 
	 * @return
	 */
	private boolean activityIsCalledWithSearchAction() {
		final String queryAction = getIntent().getAction();
		return Intent.ACTION_SEARCH.equals(queryAction);
	}

	/**
	 * Initialze the window and the activity.
	 */
	private void initWindow() {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		getListView().setTextFilterEnabled(true);
		getListView().setItemsCanFocus(true);
		getListView().setClickable(true);
		getListView().setOnItemClickListener(new CmisDocSelectedListener());
	}

	/**
	 * Process the current intent as search intent, build a query url and
	 * display the feed.
	 * 
	 * @param queryIntent
	 */
	private void doSearchWithIntent(final Intent queryIntent) {
		final String queryString = queryIntent
				.getStringExtra(SearchManager.QUERY);

		QueryType queryType = getQueryTypeFromIntent(queryIntent);
		String searchFeed = repository.getSearchFeed(queryType, queryString);
		displayFeedInListView(searchFeed);
	}

	/**
	 * Retrieves the feed to display from a regular intent. This is passed by
	 * the previous activity when a user selects a folder.
	 * 
	 * @return
	 */
	private String getFeedFromIntent() {
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			if (extras.get("feed") != null) {
				return extras.get("feed").toString();
			}
		}
		return null;
	}

	/*
	 * Is called when the user leaves the settings and possibly has changed the
	 * url/user/pw. Reinitializing the repo connection. There might be a better
	 * callback to listen for preference changes.
	 * 
	 * @see android.app.Activity#onRestart()
	 */
	@Override
	protected void onRestart() {
		super.onRestart();
		Prefs prefs = new Prefs(this);
		repository = CmisRepository.create(prefs);
		displayFeedInListView(null);
	}

	/**
	 * Displays the cmis feed given as in the list asynchronously
	 * 
	 * @param feed
	 */
	private void displayFeedInListView(final String feed) {
		setTitle(R.string.loading);
		new FeedDisplayTask(this, repository).execute(feed);
	}

	/**
	 * Downloads a file from an url to an {@link OutputStream}
	 * 
	 * @param os
	 * @param contentUrl
	 */
	private void downloadContent(OutputStream os, CmisItem item) {
		try {
			// FIXME this shouldn't be done on the UI thread, a AsyncTask is
			// needed.
			repository.fetchContent(item, os);
			os.close();
		} catch (Exception e) {
			Toast.makeText(this, R.string.error_downloading_content,
					Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Opens a file by downloading it and starting the associated app.
	 * 
	 * @param item
	 */
	private void openDocument(CmisItem item) {
		try {
			OutputStream os = openFileOutput(item.getTitle(),
					MODE_WORLD_READABLE);

			downloadContent(os, item);
		} catch (FileNotFoundException fnfe) {
			Toast.makeText(this, R.string.error_file_does_not_exists,
					Toast.LENGTH_SHORT).show();
		}

		File tempFile = new File(getFilesDir(), item.getTitle());
		if (!tempFile.exists()) {
			Toast.makeText(this, R.string.error_file_does_not_exists,
					Toast.LENGTH_SHORT).show();
			return;
		}

		viewFileInAssociatedApp(tempFile, item.getMimeType());
	}

	/**
	 * Displays a file on the local system with the associated app by calling
	 * the ACTION_VIEW intent.
	 * 
	 * @param tempFile
	 * @param mimeType
	 */
	private void viewFileInAssociatedApp(File tempFile, String mimeType) {
		Intent viewIntent = new Intent(Intent.ACTION_VIEW);
		Uri data = Uri.fromFile(tempFile);
		viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		viewIntent.setDataAndType(data, mimeType.toLowerCase());

		try {
			startActivity(viewIntent);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, R.string.application_not_available,
					Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Listener that is called whenever a user clicks on a file or folder in the
	 * list.
	 */
	private class CmisDocSelectedListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			CmisItem doc = (CmisItem) parent.getItemAtPosition(position);

			if (doc.hasChildren()) {
				openNewListViewActivity(doc);
			} else {
				openDocument(doc);
			}
		}
	}

	/**
	 * Opens a feed url in a new listview. This enables the user to use the
	 * backbutton to get back to the previous list (usually the parent folder).
	 * 
	 * @param item
	 */
	private void openNewListViewActivity(CmisItem item) {
		Intent intent = new Intent(this, ListCmisFeedActivity.class);
		intent.putExtra("feed", item.getDownLink());
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem settingsItem = menu.add(Menu.NONE, 1, 0, R.string.menu_item_settings);
		settingsItem.setIcon(android.R.drawable.ic_menu_edit);
		MenuItem aboutItem = menu.add(Menu.NONE, 2, 0, R.string.menu_item_about);
		aboutItem.setIcon(android.R.drawable.ic_menu_info_details);
		// MenuItem searchItem = menu.add(Menu.NONE, 3, 0, "Search");

		SubMenu searchMenu = menu.addSubMenu(R.string.menu_item_search);
		searchMenu.setIcon(android.R.drawable.ic_menu_search);
		searchMenu.getItem().setAlphabeticShortcut(SearchManager.MENU_KEY);

		searchMenu.add(Menu.NONE, 4, 0, R.string.menu_item_search_title);
		searchMenu.add(Menu.NONE, 5, 0, R.string.menu_item_search_fulltext);
		searchMenu.add(Menu.NONE, 6, 0, R.string.menu_item_search_cmis);

		return true;

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case 1:
			startActivity(new Intent(this, CmisPreferences.class));
			return true;
		case 2:
			Toast.makeText(this, R.string.about_message, 5)
					.show();
			return true;
		case 4:
			queryType = QueryType.TITLE;
			onSearchRequested();
			return true;
		case 5:
			queryType = QueryType.FULLTEXT;
			onSearchRequested();
			return true;
		case 6:
			queryType = QueryType.CMISQUERY;
			onSearchRequested();
			return true;
		default:
			Toast.makeText(this, "unknown menu item.", 5).show();
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onSearchRequested()
	 */
	@Override
	public boolean onSearchRequested() {
		Bundle appData = new Bundle();
		appData.putString(QueryType.class.getName(), queryType.name());
		startSearch("", false, appData, false);
		return true;
	}

}