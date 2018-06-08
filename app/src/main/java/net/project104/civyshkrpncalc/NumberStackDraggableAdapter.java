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

class NumberStackDraggableAdapter extends BaseAdapter implements DragNDropAdapter {
    private WeakReference<ActivityMain> activityReference;

    NumberStackDraggableAdapter(ActivityMain activity) {
        activityReference = new WeakReference<>(activity);
    }

    class ViewHolder {
        TextView tvIndex, tvNumber;
        ImageView ivHandler;
        int position; // stores the index of the number in the stack. Needs to be updated on each getView()

        ViewHolder(View view) {
            tvIndex = (TextView) view.findViewById(R.id.tvIndex);
            tvNumber = (TextView) view.findViewById(R.id.tvNumber);
            ivHandler = (ImageView) view.findViewById(R.id.draggableHandler);
        }
    }

    @Override
    public int getCount() {
        return activityReference.get().numberStack.size();
    }

    @Override
    public Object getItem(int i) {
        return activityReference.get().numberStack.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ActivityMain activity = activityReference.get();
        if (activity == null) {
            return convertView;
        }

        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(activity).inflate(R.layout.item_number_stack_draggable, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.position = position;

        int size = activity.numberStack.size();
        String letter = null;
        if (position == size - 1) {
            letter = "x";
        } else if (position == size - 2) {
            letter = "y";
        } else if (position == size - 3) {
            letter = "z";
        }
        int index = size - position;
        viewHolder.tvIndex.setText(String.format("%s%d:", letter != null ? letter + "  " : "", index));
        viewHolder.tvNumber.setText(activity.asString(((BigDecimal) getItem(position))));
        viewHolder.ivHandler.setColorFilter(activity.getResources().getColor(R.color.error_text_color));

        return convertView;
    }

    @Override
    public void onItemDrag(DragNDropListView parent, View view, int position, long id) {
    }

    @Override
    public void onItemDrop(DragNDropListView parent, View view, int startPosition, int endPosition, long id) {
        ActivityMain activity = activityReference.get();
        if (activity != null) {
            activity.clickedSwap(true, startPosition, endPosition);
        }
    }

    @Override
    public int getDragHandler() {
        return R.id.draggableHandler;
    }
}
