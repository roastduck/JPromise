package org.jpromise;

interface Callback<IN,OUT>
{
    OUT runWrapped(IN result) throws Exception;
}
