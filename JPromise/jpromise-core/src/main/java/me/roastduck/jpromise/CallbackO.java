package me.roastduck.jpromise;

abstract public class CallbackO<OUT> implements Callback<Void,OUT>
{
    public abstract OUT run() throws Exception;

    @Override
    public final OUT runWrapped(Void result) throws Exception
    {
        return run();
    }
}
