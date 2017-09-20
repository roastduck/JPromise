package org.jpromise;

abstract public class CallbackI<IN> implements Callback<IN,Void>
{
    public abstract void run(IN result) throws Exception;

    @Override
    public final Void runWrapped(IN result) throws Exception
    {
        run(result);
        return null;
    }
}
