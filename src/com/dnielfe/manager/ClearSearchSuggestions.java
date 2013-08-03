package com.dnielfe.manager;

import android.content.Context;
import android.preference.Preference;
import android.provider.SearchRecentSuggestions;
import android.util.AttributeSet;
import android.widget.Toast;

/* ClearSearchSuggestions
 * 		The Special clear recent search suggestions
 * 		Needs its own special class so you can just click on it */
public class ClearSearchSuggestions extends Preference {
	// This is the constructor called by the inflater
	public ClearSearchSuggestions(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onClick() {
		SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
				getContext(), SearchSuggestions.AUTHORITY,
				SearchSuggestions.MODE);
		suggestions.clearHistory();

		Toast.makeText(getContext(), R.string.suggestionscleared, Toast.LENGTH_SHORT).show();
		notifyChanged();
	}
}