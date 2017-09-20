package org.jpromise;

import org.junit.Test;

import static org.junit.Assert.*;

public class PromiseTest
{
    @Test
    public void testThen() throws Exception
    {
        promiseFactory(new Callback<Integer, Integer>() {
            @Override
            public Integer run(Integer x)
            {
                return x + 1;
            }
        }, 1)
                .then(new Callback<Integer, Integer>() {
                    @Override
                    public Integer run(Integer x)
                    {
                        return x * 2;
                    }
                })
                .then(new Callback<Integer, Integer>() {
                    @Override
                    public Integer run(Integer x)
                    {
                        assertEquals(4, x.intValue());
                        return null;
                    }
                })
                .waitUntilHasRun();
    }

    @Test
    public void testFail() throws Exception
    {
        promiseFactory(new Callback<Object, Object>() {
            @Override
            public Object run(Object o) throws Exception
            {
                throw new Exception("except1");
            }
        }, null)
                .fail(new Callback<Exception, Object>() {
                    @Override
                    public Object run(Exception e)
                    {
                        assertEquals("except1", e.getMessage());
                        return null;
                    }
                })
                .waitUntilHasRun();
    }

    @Test
    public void testParentFail() throws Exception
    {
        promiseFactory(new Callback<Object, Object>() {
            @Override
            public Object run(Object o) throws Exception
            {
                throw new Exception("except1");
            }
        }, null)
                .then(new Callback<Object, Integer>() {
                    @Override
                    public Integer run(Object o)
                    {
                        return 1;
                    }
                })
                .fail(new Callback<Exception, Object>() {
                    @Override
                    public Object run(Exception e)
                    {
                        assertEquals("except1", e.getMessage());
                        return null;
                    }
                })
                .waitUntilHasRun();
    }

    @Test
    public void testOnePromiseOnlyTriggeredOnce() throws Exception
    {
        final Counter triggerNum1 = new Counter(0);
        final Counter triggerNum2 = new Counter(0);
        promiseFactory(new Callback<Object, Object>() {
            @Override
            public Object run(Object o) throws Exception
            {
                throw new Exception("except1");
            }
        }, null)
                .fail(new Callback<Exception, Object>() {
                    @Override
                    public Object run(Exception e) throws Exception
                    {
                        triggerNum1.num++;
                        throw new Exception("except2");
                    }
                })
                .fail(new Callback<Exception, Object>()
                {
                    @Override
                    public Object run(Exception e)
                    {
                        triggerNum2.num++;
                        return null;
                    }
                })
                .waitUntilHasRun();
        assertEquals(1, triggerNum1.num);
        assertEquals(1, triggerNum2.num);
    }

    @Test
    public void testCancel() throws Exception
    {
        final Counter num = new Counter(0);
        promiseFactory(new Callback<Object, Object>() {
            @Override
            public Object run(Object o) throws Exception
            {
                Thread.sleep(1000);
                return null;
            }
        }, null)
                .then(new Callback<Object, Object>() {
                    @Override
                    public Object run(Object o) throws Exception
                    {
                        num.num++;
                        return null;
                    }
                })
                .cancel();
        Thread.sleep(2000);
        assertEquals(0, num.num);
    }

    protected <IN,OUT> Promise<IN,OUT> promiseFactory(Callback<IN,OUT> callback, IN input)
    {
        return new Promise<IN,OUT>(callback, input);
    }

    private static class Counter
    {
        int num;
        Counter(int num) { this.num = num; }
    }
}
