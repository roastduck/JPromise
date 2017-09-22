package me.roastduck.jpromise;

abstract public class CallbackV implements Callback<Void,Void>
{
    public abstract void run() throws Exception;

    @Override
    public final Void runWrapped(Void result) throws Exception
    {
        run();
        return null;
    }
}
