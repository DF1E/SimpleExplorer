package com.dnielfe.utils;

import com.dnielfe.manager.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class DrawerListAdapter extends BaseAdapter {
	private Context mContext;
	private String[] mTitle;
	private LayoutInflater inflater;

	public DrawerListAdapter(Context context, String[] pTitle) {
		mContext = context;
		mTitle = pTitle;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		inflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		convertView = inflater.inflate(R.layout.drawer_listitem, parent, false);

		TextView txtTitle = (TextView) convertView.findViewById(R.id.title);

		txtTitle.setText(mTitle[position]);

		return convertView;
	}

	@Override
	public int getCount() {
		return mTitle.length;
	}

	@Override
	public Object getItem(int position) {
		return mTitle[position];
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
}