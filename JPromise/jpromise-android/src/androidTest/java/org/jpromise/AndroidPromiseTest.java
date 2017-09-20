package org.jpromise;

import android.os.Looper;

import org.junit.Test;

import static org.junit.Assert.*;

public class AndroidPromiseTest extends PromiseTest
{
    @Override
    protected <IN,OUT> Promise<IN,OUT> promiseFactory(Callback<IN,OUT> callback, IN input)
    {
        return new AndroidPromise<IN,OUT>(callback, input);
    }

    @Override
    protected <IN,OUT> Promise<IN,OUT> promiseFactory(Callback<IN,OUT> callback)
    {
        return new AndroidPromise<IN,OUT>(callback);
    }

    @Test
    public void testDefaultNotInUIThread() throws Exception
    {
        promiseFactory(new CallbackIO<Integer, Integer>() {
            @Override
            public Integer run(Integer x) { return x + 1; }
        }, 1)
                .then(new CallbackI<Integer>() {
                    @Override
                    public void run(Integer x)
                    {
                        assertTrue(Looper.myLooper() != Looper.getMainLooper());
                    }
                })
                .waitUntilHasRun();
    }

    @Test
    public void testInUIThreadWhenSpecified() throws Exception
    {
        AndroidPromise p = ((AndroidPromise<Integer,Integer>)promiseFactory(new CallbackIO<Integer, Integer>() {
            @Override
            public Integer run(Integer x) { return x + 1; }
        }, 1))
                .thenUI(new CallbackI<Integer>() {
                    @Override
                    public void run(Integer x)
                    {
                        assertTrue(Looper.myLooper() == Looper.getMainLooper());
                    }
                });
        p.fail(new CallbackI<Exception>() {
            @Override
            public void run(Exception e) { assertTrue(false); }
        });
        p.waitUntilHasRun();
    }
}