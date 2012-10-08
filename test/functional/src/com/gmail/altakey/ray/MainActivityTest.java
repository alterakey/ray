package com.gmail.altakey.ray;

import android.test.ActivityInstrumentationTestCase2;
import com.jayway.android.robotium.solo.Solo;

import android.content.Context;
import android.app.ActivityManager;
import android.view.KeyEvent;

public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo mSolo;

    public MainActivityTest() {
        super("com.gmail.altakey.ray", MainActivity.class);
    }

    public void setUp() {
        mSolo = new Solo(getInstrumentation(), getActivity());
    }

    private void assertServiceIsRunning(String className) {
        boolean ret = false;
        ActivityManager manager = (ActivityManager)getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (className.equals(service.service.getClassName())) {
                ret = true;
            }
        }
        assertTrue(ret);
    }

    public void test_000() {
        assertServiceIsRunning(PlaybackService.class.getName());
    }

    public void test_001() {
        mSolo.sendKey(KeyEvent.KEYCODE_MENU);
        assertTrue(mSolo.searchText("Preferences"));
        assertTrue(mSolo.searchText("Quit"));
    }

    public void test_002() {
        mSolo.sendKey(KeyEvent.KEYCODE_MENU);
        mSolo.clickOnText("Preferences");
        assertTrue(mSolo.searchText("Air Intention"));
        assertTrue(mSolo.searchText("0.0.1"));
    }
}
