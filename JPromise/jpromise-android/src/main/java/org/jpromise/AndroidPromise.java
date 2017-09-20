package org.jpromise;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class AndroidPromise<IN,OUT> extends Promise<IN,OUT>
{
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
        synchronized (runInUI)
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
    public <T> Promise<OUT,T> thenUI(Callback<OUT,T> callback)
    {
        AndroidPromise<OUT,T> ret = then(callback);
        ret.setRunInUI();
        return ret;
    }

    /** Same as `.fail`, but run in UI thread
     */
    public <T> Promise<Exception,T> failUI(Callback<Exception,T> callback)
    {
        AndroidPromise<Exception,T> ret = fail(callback);
        ret.setRunInUI();
        return ret;
    }

    @Override
    protected void submit(IN input)
    {
        synchronized (runInUI)
        {
            if (runInUI)
            {
                RunMessage runMessage = new RunMessage();
                runMessage.promise = this;
                runMessage.input = input;
                Message msg = uiHandler.obtainMessage(0, runMessage);
                uiHandler.sendMessage(msg);
            } else
                super.submit(input);
        }
    }

    private static Handler uiHandler = new Handler(Looper.getMainLooper()) {
        @Override
        @SuppressWarnings("unchecked")
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
            if (msg.what == 0)
            {
                RunMessage runMessage = (RunMessage)(msg.obj);
                runMessage.promise.runSync(runMessage.input);
            }
        }
    };

    private static class RunMessage
    {
        AndroidPromise promise;
        Object input;
    }
}
