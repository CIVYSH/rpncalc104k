package net.project104.civyshkrpncalc;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by civyshk on 1/08/17.
 */

class MyThreadPoolExecutor extends ThreadPoolExecutor {
    static final String TAG = MyThreadPoolExecutor.class.getSimpleName();

    MyThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTimeS, TimeUnit keepAliveUnit, BlockingQueue<Runnable> workQueue){
        super(corePoolSize, maximumPoolSize, keepAliveTimeS, keepAliveUnit, workQueue, new MyThreadFactory());
    }

    static private class MyThreadFactory implements ThreadFactory {
        static final int THREAD_JAVA_PRIORITY = 2;//java priority from 0(least) to 10(most)
        static final String TAG = MyThreadFactory.class.getSimpleName();

        public Thread newThread(@NonNull Runnable r){
            Thread t = new Thread(r);
            t.setPriority(THREAD_JAVA_PRIORITY);
            t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler(){
                @Override
                public void uncaughtException(Thread t, Throwable e){
                    Log.d(TAG, "Factory-made '"+t.getName() + "' ended with exception", e);
                }
            });
            return t;
        }
    }
}
