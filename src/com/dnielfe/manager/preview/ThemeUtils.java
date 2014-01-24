package com.dnielfe.manager.preview;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;

public final class ThemeUtils {

	private ThemeUtils() {

	}

	public static Drawable getDrawable(final Resources.Theme theme,
			final int attr) {
		final TypedArray array = theme
				.obtainStyledAttributes(new int[] { attr });
		try {
			return array.getDrawable(0);
		} finally {
			array.recycle();
		}
	}
}
