package me.roastduck.jpromise;

public interface CallbackILambda<IN>
{
    void run(IN result) throws Exception;
}
