
package com.hv15.aperi.adaptors;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import com.hv15.aperi.DeviceListFragment;
import com.hv15.aperi.SelfFragment;

public class TabsPagerAdaptor extends FragmentPagerAdapter
{
    private String[] mTags;
    private FragmentManager mFragManager;

    public static final int SELF_FRAG = 0;
    public static final int LIST_FRAG = 1;
    private String[] mName = {
            "Main", "Peer List"
    };

    public TabsPagerAdaptor(FragmentManager fm)
    {
        super(fm);
        mTags = new String[2];
        mFragManager = fm;
    }

    @Override
    public Fragment getItem(int index)
    {
        Fragment frag = null;
        switch (index) {
            case SELF_FRAG:
                frag = new SelfFragment();
                break;
            case LIST_FRAG:
                frag = new DeviceListFragment();
                break;
        }
        return frag;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object)
    {
        super.destroyItem(container, position, object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position)
    {
        Fragment temp = (Fragment) super.instantiateItem(container, position);
        mTags[position] = temp.getTag();
        return temp;
    }

    @Override
    public int getCount()
    {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position)
    {
        return mName[position];
    }

    /**
     * Get Fragment at index position
     * <p>
     * This method uses a none-static means to retrieve a fragment from the
     * PageAdaptor. Unlike other methods, such as the static
     * {@link TabsPagerAdaptor#makeFragmentName(int, int)}, it does not assume
     * any tagging convention used internally by
     * {@linkplain FragmentPagerAdapter}.
     * <p>
     * <i>This method is the preferred way to retrieve a Fragment</i>
     * 
     * @param index
     *            Fragment position. See {@link FragmentPagerAdapter#getCount()}
     *            .
     * @return a Fragment or null if none found
     */
    public Fragment getFragmentByPosition(int index)
    {
        return mFragManager.findFragmentByTag(mTags[index]);
    }

    /**
     * Get the tag of a Fragment within the the ViewPager
     * <p>
     * This method generates the tag of a Fragment based upon its index and
     * container. With the tag, one can then retrieve the Fragment associated to
     * it from the {@link TabsPagerAdaptor}. However the actual tag format <b>is
     * not</b> guaranteed to be consistent with what is used by
     * {@link FragmentManager}.
     * <p>
     * <i><b>This method should not be used for production reasons. Instead use
     * {@link TabsPagerAdaptor#getFragmentByPosition(int index)} to retrieve a
     * Fragment.</b></i>
     * 
     * @see <a href="http://stackoverflow.com/a/8965602/1203078">StackOverflow
     *      Article</a>
     * @param viewId
     *            ViewPager ID
     * @param index
     *            index of the fragment
     * @return a generated tag
     */
    public static String makeFragmentName(int viewId, int index)
    {
        return "android:switcher:" + viewId + ":" + index;
    }
}
