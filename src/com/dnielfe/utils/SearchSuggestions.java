package com.dnielfe.utils;

import android.content.SearchRecentSuggestionsProvider;

public class SearchSuggestions extends SearchRecentSuggestionsProvider {
	public final static String AUTHORITY = "com.dnielfe.manager";
	public final static int MODE = DATABASE_MODE_QUERIES;

	public SearchSuggestions() {
		setupSuggestions(AUTHORITY, MODE);
	}
}