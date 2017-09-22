package me.roastduck.jpromise;

public interface CallbackIOLambda<IN,OUT>
{
    OUT run(IN result) throws Exception;
}
