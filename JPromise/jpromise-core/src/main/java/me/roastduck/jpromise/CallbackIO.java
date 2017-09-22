package me.roastduck.jpromise;

abstract public class CallbackIO<IN,OUT> implements Callback<IN,OUT>
{
    public abstract OUT run(IN result) throws Exception;

    @Override
    public final OUT runWrapped(IN result) throws Exception
    {
        return run(result);
    }
}
