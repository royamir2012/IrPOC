package com.robotdreams;

import android.app.Application;
import android.test.ApplicationTestCase;
import com.robotdreams.api.CamFindRestClient;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {

    public ApplicationTest()
    {
        super(Application.class);
    }


    public void testCamera()
    {
        CamFindRestClient client = new CamFindRestClient();
        client.imageRequest(getContext(), null);
    }
}
