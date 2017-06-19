package com.library.yang.eliyet.eliyetyanglibrary.entity;

/**
 * Created by eliyetyang on 17-5-3.
 */

public interface IEquals {
    boolean sameIdWith(IEquals object);

    long getId();
}
