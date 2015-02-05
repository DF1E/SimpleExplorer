package com.dnielfe.manager.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.dnielfe.manager.R;

public class DrawerListAdapter extends BaseAdapter {
    private final Context mContext;
    private final String[] mTitle;

    public DrawerListAdapter(final Context context) {
        this.mContext = context;
        this.mTitle = context.getResources().getStringArray(
                R.array.drawerTitles_array);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_drawermenu, parent,
                    false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.title.setText(mTitle[position]);
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

    private class ViewHolder {
        public TextView title = null;

        ViewHolder(View row) {
            title = (TextView) row.findViewById(R.id.title);
        }
    }
}