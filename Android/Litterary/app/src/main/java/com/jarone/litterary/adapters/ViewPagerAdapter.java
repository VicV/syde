package com.jarone.litterary.adapters;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jarone.litterary.R;

/**
 * Created by V on 2/23/2016.
 * <p/>
 * Adapter for viewpages. Note that in {@link com.jarone.litterary.activities.MainActivity } we specify
 * that all view pages will be loaded at all times.
 */
public class ViewPagerAdapter extends PagerAdapter {
    private Context mContext;

    public ViewPagerAdapter(Context context) {
        mContext = context;
    }

    /**
     * Create the item to fill the page
     *
     * @param collection Viewgroup of the page
     * @param position   Position on viewpager (see enum order)
     * @return View
     */
    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        ViewPage viewPageEnum = ViewPage.values()[position];
        LayoutInflater inflater = LayoutInflater.from(mContext);

        //Inflate the appropriate layout for the respective page.
        ViewGroup layout = (ViewGroup) inflater.inflate(viewPageEnum.getLayoutResId(), collection, false);
        collection.addView(layout);
        return layout;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((View) view);
    }

    @Override
    public int getCount() {
        return ViewPage.values().length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        ViewPage customPagerEnum = ViewPage.values()[position];
        return customPagerEnum.getTitle();
    }

    //Little enumerator for easy creation of view pages
    enum ViewPage {

        DEBUG("messages", R.layout.debug_queue_layout_page),
        MAIN("main", R.layout.main_layout_page),
        STATUS("status", R.layout.info_layout_page);

        private String mTitle;
        private int mLayoutResId;

        ViewPage(String title, int layoutResId) {
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

}



