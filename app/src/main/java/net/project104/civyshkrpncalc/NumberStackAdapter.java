package net.project104.civyshkrpncalc;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;

/**
 * Created by civyshk on 9/06/17.
 */

public class NumberStackAdapter extends BaseAdapter {
    private WeakReference<MainActivity> activityReference;

    NumberStackAdapter(MainActivity activity) {
        activityReference = new WeakReference<MainActivity>(activity);
    }

    private class ViewHolder{
        TextView tvLetter, tvIndex, tvNumber;

        public ViewHolder(View view) {
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
            convertView = LayoutInflater.from(activity).inflate(R.layout.item_number_stack, parent, false);
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
}
