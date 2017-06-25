/*
 * Copyright 2012 Terlici Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.project104.civyshkrpncalc;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.terlici.dragndroplist.DragNDropAdapter;
import com.terlici.dragndroplist.DragNDropListView;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.Locale;

class NumberStackDragableAdapter extends BaseAdapter implements DragNDropAdapter {
	private WeakReference<MainActivity> activityReference;

	NumberStackDragableAdapter(MainActivity activity) {
		activityReference = new WeakReference<MainActivity>(activity);
	}

	private class ViewHolder{
		TextView tvLetter, tvIndex, tvNumber;

		ViewHolder(View view) {
			tvLetter = (TextView) view.findViewById(R.id.tvLetter);
			tvIndex = (TextView) view.findViewById(R.id.tvIndex);
			tvNumber = (TextView) view.findViewById(R.id.tvNumber);
		}
	}


	@Override
	public int getCount() {
		return activityReference.get().numberStack.size();
	}

	@Override
	public Object getItem(int i){
		return activityReference.get().numberStack.get(i);
	}

	@Override
	public long getItemId(int i) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		MainActivity activity = activityReference.get();
		if (activity == null) {
			return convertView;
		}

		ViewHolder viewHolder;
		if(convertView == null){
			convertView = LayoutInflater.from(activity).inflate(R.layout.item_number_stack_draggable, parent, false);
			viewHolder = new ViewHolder(convertView);
			convertView.setTag(viewHolder);
		}else{
			viewHolder = (ViewHolder) convertView.getTag();
		}

		int size = activity.numberStack.size();
		String letter = "";
		if(position == size - 1){
			letter = "x";
		}else if(position == size - 2){
			letter = "y";
		}else if(position == size - 3){
			letter = "z";
		}
		viewHolder.tvLetter.setText(letter);
		int index = size - position;
		viewHolder.tvIndex.setText(String.format("%d:", index));
		viewHolder.tvNumber.setText(MainActivity.toString(((BigDecimal) getItem(position))));
		return convertView;
	}

	@Override
	public void onItemDrag(DragNDropListView parent, View view, int position, long id) {
    }

	@Override
	public void onItemDrop(DragNDropListView parent, View view, int startPosition, int endPosition, long id) {
        MainActivity activity = activityReference.get();
        if (activity != null) {
            activity.clickedSwap(true, startPosition, endPosition);
        }
	}

	@Override
	public int getDragHandler() {
		return R.id.draggableHandler;
	}
}
