package com.jarone.litterary.adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jarone.litterary.R;
import com.jarone.litterary.adapters.DebugItem.DebugLevel;

import java.util.List;

/**
 * Created by V on 2/23/2016.
 */
public class DebugMessageRecyclerAdapter extends RecyclerView.Adapter<DebugMessageRecyclerAdapter.CustomViewHolder> {

    public List<DebugItem> getDebugItemList() {
        return debugItemList;
    }

    private List<DebugItem> debugItemList;
    private Context mContext;

    public DebugMessageRecyclerAdapter(Context context, List<DebugItem> feedItemList) {
        this.debugItemList = feedItemList;
        this.mContext = context;
    }

    @Override
    public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.debug_item, null);

        return new CustomViewHolder(view);
    }

    private int getColorFromLevel(DebugLevel level) {
        switch (level) {
            case DEBUG:
                return ContextCompat.getColor(mContext, R.color.white);
            case WARN:
                return ContextCompat.getColor(mContext, R.color.orange);
            case ERROR:
                return ContextCompat.getColor(mContext, R.color.red);
            default:
                return ContextCompat.getColor(mContext, R.color.white);
        }
    }

    @Override
    public void onBindViewHolder(CustomViewHolder holder, int position) {
        holder.textView.setText(debugItemList.get(position).getText());
        holder.textView.setTextColor(getColorFromLevel(debugItemList.get(position).debugLevel));
    }


    @Override
    public int getItemCount() {
        return debugItemList.size();
    }

    public class CustomViewHolder extends RecyclerView.ViewHolder {
        protected TextView textView;

        public CustomViewHolder(View view) {
            super(view);
            this.textView = (TextView) view.findViewById(R.id.debug_text);
        }
    }
}
