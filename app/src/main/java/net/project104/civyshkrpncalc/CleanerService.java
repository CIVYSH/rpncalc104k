package net.project104.civyshkrpncalc;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;


public class CleanerService extends IntentService {

    public static final String TAG = CleanerService.class.getSimpleName();

    public CleanerService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        killApplication();
    }

    private void killApplication() {
        Log.d(TAG, "Killing whole app");
        stopSelf();
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
