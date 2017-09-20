package org.jpromise;

import java.util.List;
import java.util.Vector;
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

    Boolean runInUI; // Should not be placed in AndroidPromise for it is invoked in constructor

    Promise()
    {
        parents = new Vector<>();
        resNext = new Vector<>();
        rejNext = new Vector<>();
        runInUI = false;
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
        if (!alreadyRun)
            resNext.add(next);
        else if (resolved)
            next.submit(output);
    }

    <T> void failPipe(final Promise<Exception,T> next)
    {
        for (Promise<?,?> parent : parents)
            parent.failPipe(next);
        next.parents.add(this);
        if (!alreadyRun)
            rejNext.add(next);
        else if (rejected)
            next.submit(throwable);
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
    public synchronized Promise<IN,OUT> waitUntilHasRun(long timeout) throws InterruptedException
    {
        if (alreadyRun)
            return this;
        wait(timeout);
        if (!alreadyRun)
            throw new InterruptedException();
        return this;
    }

    protected synchronized void runSync(IN input)
    {
        if (canceled)
            return;
        if (alreadyRun)
            return;
        try
        {
            output = callback.runWrapped(input);
            for (Promise<OUT,?> next : resNext)
                next.submit(output);
            resolved = true;
        } catch (Exception e)
        {
            throwable = e;
            for (Promise<Exception,?> next : rejNext)
                next.submit(throwable);
            rejected = true;
        }
        resNext = null;
        rejNext = null;
        alreadyRun = true;
        notifyAll();
    }
}
