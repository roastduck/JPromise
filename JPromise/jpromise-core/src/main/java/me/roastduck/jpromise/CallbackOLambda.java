package me.roastduck.jpromise;

public interface CallbackOLambda<OUT>
{
    OUT run() throws Exception;
}
