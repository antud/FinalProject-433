package com.example.finalproject_433;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class ListItemAdapter extends ArrayAdapter<ListItem> {

    public interface OnItemCheckListener {
        void onItemCheckedChange();
    }

    private OnItemCheckListener onItemCheckListener;
    private ArrayList<ListItem> dataList;
    private int count = 0;
    private boolean showCheckbox = false;

    ListItemAdapter(Context context, int resource, ArrayList<ListItem> objects) {
        super(context, resource, objects);
        dataList = objects;
    }

    public void setOnItemCheckListener(OnItemCheckListener onItemCheckListener) {
        this.onItemCheckListener = onItemCheckListener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }

        ListItem currentItem = getItem(position);
        ImageView currentImage = convertView.findViewById(R.id.image_in_list);
        TextView currentName = convertView.findViewById(R.id.text_in_list);
        CheckBox checkBox = convertView.findViewById(R.id.include_image);

        checkBox.setVisibility(showCheckbox ? View.VISIBLE : View.GONE);

        if (currentItem.getImageResource() != 0) {
            currentImage.setImageResource(currentItem.getImageResource());
        } else if (currentItem.getImageBitmap() != null) {
            currentImage.setImageBitmap(currentItem.getImageBitmap());
        }

        currentName.setText(currentItem.getTagText());

        checkBox.setOnCheckedChangeListener(null);
        checkBox.setChecked(currentItem.isChecked());

        checkBox.setEnabled(count < 3 || currentItem.isChecked());

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (count < 3) {
                        count++;
                        currentItem.setChecked(true);
                    } else {
                        buttonView.setChecked(false);
                        return;
                    }
                } else {
                    count--;
                    currentItem.setChecked(false);
                }

                if (onItemCheckListener != null) {
                    onItemCheckListener.onItemCheckedChange();
                }
                notifyDataSetChanged();
            }
        });

        return convertView;
    }

    public void updateData(ArrayList<ListItem> newData) {
        dataList.clear();
        dataList.addAll(newData);
        notifyDataSetChanged();
    }

    public void setShowCheckbox(boolean isVisible) {
        showCheckbox = isVisible;
    }

    public ArrayList<ListItem> getDataList() {
        return dataList;
    }

}
