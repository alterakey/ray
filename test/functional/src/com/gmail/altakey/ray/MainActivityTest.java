package com.gmail.altakey.ray;

import android.test.ActivityInstrumentationTestCase2;
import com.jayway.android.robotium.solo.Solo;

public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo mSolo;

    public MainActivityTest() {
        super("com.gmail.altakey.ray", MainActivity.class);
    }

    public void setUp() {
        mSolo = new Solo(getInstrumentation(), getActivity());
    }

    public void test_000() {
        mSolo.clickOnButton("Play");
        assertTrue(mSolo.searchText("copied corpse"));
        assertTrue(mSolo.searchText("playing corpse"));
    }
}
