package org.jpromise;

import org.junit.Test;

import static org.junit.Assert.*;

public class PromiseTest
{
    @Test
    public void testThen() throws Exception
    {
        promiseFactory(new CallbackIO<Integer, Integer>() {
            @Override
            public Integer run(Integer x) { return x + 1; }
        }, 1)
                .then(new CallbackIO<Integer, Integer>() {
                    @Override
                    public Integer run(Integer x) { return x * 2; }
                })
                .then(new CallbackI<Integer>() {
                    @Override
                    public void run(Integer x)
                    {
                        assertEquals(4, x.intValue());
                    }
                })
                .waitUntilHasRun();
    }

    @Test
    public void testFail() throws Exception
    {
        promiseFactory(new CallbackV() {
            @Override
            public void run() throws Exception
            {
                throw new Exception("except1");
            }
        })
                .fail(new CallbackI<Exception>() {
                    @Override
                    public void run(Exception e)
                    {
                        assertEquals("except1", e.getMessage());
                    }
                })
                .waitUntilHasRun();
    }

    @Test
    public void testParentFail() throws Exception
    {
        promiseFactory(new CallbackV() {
            @Override
            public void run() throws Exception
            {
                throw new Exception("except1");
            }
        })
                .then(new CallbackO<Integer>() {
                    @Override
                    public Integer run() { return 1; }
                })
                .fail(new CallbackI<Exception>() {
                    @Override
                    public void run(Exception e)
                    {
                        assertEquals("except1", e.getMessage());
                    }
                })
                .waitUntilHasRun();
    }

    @Test
    public void testOnePromiseOnlyTriggeredOnce() throws Exception
    {
        final Counter triggerNum1 = new Counter(0);
        final Counter triggerNum2 = new Counter(0);
        Promise p = promiseFactory(new CallbackV() {
            @Override
            public void run() throws Exception
            {
                throw new Exception("except1");
            }
        })
                .fail(new CallbackI<Exception>() {
                    @Override
                    public void run(Exception e) throws Exception
                    {
                        triggerNum1.num++;
                        throw new Exception("except2");
                    }
                });
        p.
                fail(new CallbackI<Exception>()
                {
                    @Override
                    public void run(Exception e) { triggerNum2.num++; }
                })
                .waitUntilHasRun();
        p.waitUntilHasRun();
        assertEquals(1, triggerNum1.num);
        assertEquals(1, triggerNum2.num);
    }

    @Test
    public void testCancel() throws Exception
    {
        final Counter num = new Counter(0);
        promiseFactory(new CallbackV() {
            @Override
            public void run() throws Exception { Thread.sleep(1000); }
        }, null)
                .then(new CallbackV() {
                    @Override
                    public void run() throws Exception { num.num++; }
                })
                .cancel();
        Thread.sleep(2000);
        assertEquals(0, num.num);
    }

    protected <IN,OUT> Promise<IN,OUT> promiseFactory(Callback<IN,OUT> callback, IN input)
    {
        return new Promise<IN,OUT>(callback, input);
    }

    protected <IN,OUT> Promise<IN,OUT> promiseFactory(Callback<IN,OUT> callback)
    {
        return new Promise<IN,OUT>(callback);
    }

    private static class Counter
    {
        int num;
        Counter(int num) { this.num = num; }
    }
}
