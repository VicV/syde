package com.jarone.litterary.fragments;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jarone.litterary.R;

/**
 * Created by V on 2/23/2016.
 */
public class MainPagerAdapter extends PagerAdapter {
    private Context mContext;

    public MainPagerAdapter(Context context) {
        mContext = context;
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        CustomPagerEnum customPagerEnum = CustomPagerEnum.values()[position];
        LayoutInflater inflater = LayoutInflater.from(mContext);
        ViewGroup layout = (ViewGroup) inflater.inflate(customPagerEnum.getLayoutResId(), collection, false);
        collection.addView(layout);
        return layout;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((View) view);
    }

    @Override
    public int getCount() {
        return CustomPagerEnum.values().length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        CustomPagerEnum customPagerEnum = CustomPagerEnum.values()[position];
        return customPagerEnum.getTitle();
    }


}


enum CustomPagerEnum {

    DEBUG("messages", R.layout.debug_queue_layout_page),
    MAIN("main", R.layout.main_layout_page),
    STATUS("status", R.layout.info_layout_page);

    private String mTitle;
    private int mLayoutResId;

    CustomPagerEnum(String title, int layoutResId) {
        mTitle = title;
        mLayoutResId = layoutResId;
    }

    public String getTitle() {
        return mTitle;
    }

    public int getLayoutResId() {
        return mLayoutResId;
    }
}
