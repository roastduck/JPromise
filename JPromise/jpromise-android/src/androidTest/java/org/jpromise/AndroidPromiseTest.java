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

    @Test
    public void testDefaultNotInUIThread() throws Exception
    {
        promiseFactory(new Callback<Integer, Integer>() {
            @Override
            public Integer run(Integer x)
            {
                return x + 1;
            }
        }, 1)
                .then(new Callback<Integer, Object>() {
                    @Override
                    public Object run(Integer x)
                    {
                        assertTrue(Looper.myLooper() != Looper.getMainLooper());
                        return null;
                    }
                })
                .waitUntilHasRun();
    }

    @Test
    public void testInUIThreadWhenSpecified() throws Exception
    {
        ((AndroidPromise<Integer,Integer>)promiseFactory(new Callback<Integer, Integer>() {
            @Override
            public Integer run(Integer x)
            {
                return x + 1;
            }
        }, 1))
                .thenUI(new Callback<Integer, Object>() {
                    @Override
                    public Object run(Integer x)
                    {
                        assertTrue(Looper.myLooper() == Looper.getMainLooper());
                        return null;
                    }
                })
                .waitUntilHasRun();
    }
}