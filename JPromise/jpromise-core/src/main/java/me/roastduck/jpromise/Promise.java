package me.roastduck.jpromise;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class Promise<IN,OUT>
{
    private static ExecutorService executorService = Executors.newCachedThreadPool();

    Callback<IN,OUT> callback;

    private List< Promise<?,?> > parents;
    private List< Promise<OUT,?> > resNext;
    private List< Promise<Exception,?> > rejNext;

    private boolean alreadyRun = false;
    private boolean resolved = false;
    private boolean rejected = false;
    private OUT output;
    private Exception throwable;

    private boolean canceled = false;

    final Object runInUILock;
    final Object alreadyRunLock;

    Promise()
    {
        parents = new Vector<>();
        resNext = new Vector<>();
        rejNext = new Vector<>();
        runInUILock = new Object();
        alreadyRunLock = new Object();
    }

    /** Create a Promise object without input
     *  @param callback : Callback run immediately after the object being constructed
     */
    public Promise(Callback<IN,OUT> callback)
    {
        this(callback, null);
    }

    /** Create a Promise object with input
     *  @param callback : Callback run immediately after the object being constructed
     *  @param input : Input for the callback
     */
    public Promise(Callback<IN,OUT> callback, final IN input)
    {
        this();
        this.callback = callback;
        submit(input);
    }

    protected void submit(final IN input)
    {
        executorService.submit(new Runnable() {
            @Override
            public void run()
            {
                Promise.this.runSync(input);
            }
        });
    }

    <T> void thenPipe(final Promise<OUT,T> next)
    {
        next.parents.add(this);
        synchronized (alreadyRunLock)
        {
            if (!alreadyRun)
                resNext.add(next);
            else if (resolved)
                next.submit(output);
        }
    }

    <T> void failPipe(final Promise<Exception,T> next)
    {
        for (Promise<?,?> parent : parents)
            parent.failPipe(next);
        next.parents.add(this);
        synchronized (alreadyRunLock)
        {
            if (!alreadyRun)
                rejNext.add(next);
            else if (rejected)
                next.submit(throwable);
        }
    }

    /** What should be done after the Promise finished its callback normally
     *  You can attach more than 1 `then` callbacks by calling `then` multiple times
     *  @param callback : What you want do after the Promise finished its callback normally
     *  @return : A new Promise containing the new callback
     */
    public <T> Promise<OUT,T> then(Callback<OUT,T> callback)
    {
        Promise<OUT,T> next = new Promise<>();
        next.callback = callback;
        thenPipe(next);
        return next;
    }

    /** What should be done when this Promise AND ITS PARENT Promises failed to finish its callback
     *  You can attach more than 1 `fail` callbacks by calling `fail` multiple times
     *  @param callback : What you want to do after the Promise failed
     *  @return : A new Promise containing the new callback
     */
    public <T> Promise<Exception,T> fail(Callback<Exception,T> callback)
    {
        Promise<Exception,T> next = new Promise<>();
        next.callback = callback;
        failPipe(next);
        return next;
    }

    /** Cancel the task
     */
    public void cancel() { canceled = true; }

    private static final long DEFAULT_WAIT_TIMEOUT = 10000;

    /** Wait for this Promise to finish or fail with default timeout
     *  @throws InterruptedException : if time's out, throw an InterruptedException
     *  @return : this Promise
     */
    public Promise<IN,OUT> waitUntilHasRun() throws InterruptedException
    {
        return waitUntilHasRun(DEFAULT_WAIT_TIMEOUT);
    }

    /** Wait for this Promise to finish or fail
     *  @param timeout : if time's out, throw an InterruptedException
     *  @return : this Promise
     */
    public Promise<IN,OUT> waitUntilHasRun(long timeout) throws InterruptedException
    {
        synchronized (alreadyRunLock)
        {
            if (alreadyRun)
                return this;
            alreadyRunLock.wait(timeout);
            if (!alreadyRun)
                throw new InterruptedException();
            return this;
        }
    }

    /** Generate callback objects from lambda expression
     */
    public static <I> CallbackI<I> from(final CallbackILambda<I> o)
    {
        return new CallbackI<I>() {
            @Override
            public void run(I result) throws Exception
            {
                o.run(result);
            }
        };
    }
    public static <O> me.roastduck.jpromise.CallbackO<O> from(final CallbackOLambda<O> o)
    {
        return new me.roastduck.jpromise.CallbackO<O>() {
            @Override
            public O run() throws Exception
            {
                return o.run();
            }
        };
    }
    public static <I,O> CallbackIO<I,O> from(final CallbackIOLambda<I,O> o)
    {
        return new CallbackIO<I,O>() {
            @Override
            public O run(I result) throws Exception
            {
                return o.run(result);
            }
        };
    }
    public static CallbackV from(final me.roastduck.jpromise.CallbackVLambda o)
    {
        return new CallbackV() {
            @Override
            public void run() throws Exception
            {
                o.run();
            }
        };
    }

    /** Generate callback objects from Runnable
     */
    public static CallbackV from(final Runnable o)
    {
        return new CallbackV() {
            @Override
            public void run()
            {
                o.run();
            }
        };
    }

    /** Generate callback objects from Callable
     */
    public static <O> me.roastduck.jpromise.CallbackO<O> from(final Callable<O> o)
    {
        return new me.roastduck.jpromise.CallbackO<O>() {
            @Override
            public O run() throws Exception
            {
                return o.call();
            }
        };
    }

    synchronized void runSync(IN input) // Don't allow multiple runSync at the same time
    {
        if (canceled)
            return;
        if (alreadyRun)
            return;

        try
        {
            output = callback.runWrapped(input);
            resolved = true;
        } catch (Exception e)
        {
            throwable = e;
            rejected = true;
        }

        synchronized (alreadyRunLock) // Don't allow piping now
        {
            if (resolved)
                for (Promise<OUT, ?> next : resNext)
                    next.submit(output);
            else
                for (Promise<Exception, ?> next : rejNext)
                    next.submit(throwable);
            resNext = null;
            rejNext = null;
            alreadyRun = true;
            alreadyRunLock.notifyAll();
        }
    }
}
