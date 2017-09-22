package me.roastduck.jpromise;

import android.os.Handler;
import android.os.Looper;

public class AndroidPromise<IN,OUT> extends Promise<IN,OUT>
{
    private boolean runInUI = false;

    private static Handler uiHandler = new Handler(Looper.getMainLooper());

    AndroidPromise() {}

    /** Create a Promise object without input
     *  @param callback : Callback run immediately after the object being constructed
     */
    public AndroidPromise(Callback<IN,OUT> callback) { this(callback, null); }

    /** Create a Promise object with input
     *  @param callback : Callback run immediately after the object being constructed
     *  @param input : Input for the callback
     */
    public AndroidPromise(Callback<IN,OUT> callback, final IN input)
    {
        super(callback, input);
    }

    /** Set the callback if this Promise to be run in the UI thread
     *  This doesn't affect following "then" or "fail" callbacks
     */
    public void setRunInUI()
    {
        synchronized (runInUILock)
        {
            runInUI = true;
        }
    }

    /** Overrode method for AndroidPromise
     */
    @Override
    public <T> AndroidPromise<OUT,T> then(Callback<OUT,T> callback)
    {
        AndroidPromise<OUT,T> next = new AndroidPromise<>();
        next.callback = callback;
        thenPipe(next);
        return next;
    }

    /** Overrode method for AndroidPromise
     */
    @Override
    public <T> AndroidPromise<Exception,T> fail(Callback<Exception,T> callback)
    {
        AndroidPromise<Exception,T> next = new AndroidPromise<>();
        next.callback = callback;
        failPipe(next);
        return next;
    }

    /** Same as `.then`, but run in UI thread
     */
    public <T> AndroidPromise<OUT,T> thenUI(Callback<OUT,T> callback)
    {
        AndroidPromise<OUT,T> next = new AndroidPromise<>();
        next.callback = callback;
        next.setRunInUI(); // Do this before piping
        thenPipe(next);
        return next;
    }

    /** Same as `.fail`, but run in UI thread
     */
    public <T> AndroidPromise<Exception,T> failUI(Callback<Exception,T> callback)
    {
        AndroidPromise<Exception,T> next = new AndroidPromise<>();
        next.callback = callback;
        next.setRunInUI();
        failPipe(next);
        return next;
    }

    @Override
    protected void submit(final IN input)
    {
        synchronized (runInUILock)
        {
            if (runInUI)
            {
                boolean success = uiHandler.post(new Runnable() {
                    @Override
                    public void run()
                    {
                        AndroidPromise.this.runSync(input);
                    }
                });
                if (!success)
                {
                    callback = new me.roastduck.jpromise.CallbackIO<IN,OUT>() {
                        @Override
                        public OUT run(IN o) throws Exception
                        {
                            throw new Exception("Failed to post to UI Thread");
                        }
                    };
                    super.submit(input);
                }
            } else
                super.submit(input);
        }
    }
}
