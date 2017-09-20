package org.jpromise;

public interface Callback<IN,OUT>
{
    OUT run(IN result) throws Exception;
}
