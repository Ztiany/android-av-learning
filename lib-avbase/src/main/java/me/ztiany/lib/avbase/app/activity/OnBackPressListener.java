package me.ztiany.lib.avbase.app.activity;

/**
 * Activity 的返回键监听。
 */
public interface OnBackPressListener {

    /**
     * @return true 表示 Fragment 处理 on back press，false 表示由 Activity 处理。
     */
    boolean onBackPressed();

}