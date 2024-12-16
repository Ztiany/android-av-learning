package me.ztiany.lib.avbase.app.activity;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.util.List;


public class BackHandlerHelper {

    public static boolean handleBackPress(FragmentManager fragmentManager) {
        List<Fragment> fragments = fragmentManager.getFragments();
        for (int i = fragments.size() - 1; i >= 0; i--) {
            Fragment child = fragments.get(i);
            if (isFragmentBackHandled(child)) {
                return true;
            }
        }
        return false;
    }


    public static boolean handleBackPress(Fragment fragment) {
        return handleBackPress(fragment.getChildFragmentManager());
    }

    public static boolean handleBackPress(FragmentActivity fragmentActivity) {
        return handleBackPress(fragmentActivity.getSupportFragmentManager());
    }

    public static boolean isFragmentBackHandled(Fragment fragment) {
        return fragment != null
                && fragment.isVisible()
                && fragment.getUserVisibleHint() //getUserVisibleHint默认情况下为true，在ViewPager中会被使用到。
                && fragment instanceof OnBackPressListener
                && ((OnBackPressListener) fragment).onBackPressed();
    }

}