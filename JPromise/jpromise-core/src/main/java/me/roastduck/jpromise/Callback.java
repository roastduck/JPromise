package me.roastduck.jpromise;

interface Callback<IN,OUT>
{
    OUT runWrapped(IN result) throws Exception;
}
