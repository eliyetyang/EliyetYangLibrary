package com.library.yang.eliyet.eliyetyanglibrary.adapter;

import android.support.v7.widget.RecyclerView;

import com.library.yang.eliyet.eliyetyanglibrary.entity.IEquals;

/**
 * Created by eliyetyang on 17-5-3.
 */

public abstract class DataChangeableAdapter<T extends RecyclerView.ViewHolder, E extends IEquals> extends BaseLoadMoreAdapter<T, E> {
    public DataChangeableAdapter(int startPage, int pageSize, LoadMoreListener loadMoreListener) {
        super(startPage, pageSize, loadMoreListener);
    }

    /**
     * 替换指定的旧对象。
     * 如果旧对象不在原来的位置，则会遍历对比到ID相同的对象，进行替换，只替换第一个ID相同的对象。
     *
     * @param object 新对象
     * @param index  旧对象应在的位置 -1则根据ID查找。{@link IEquals}
     */
    public void setData(E object, int index) {
        if (mDatas != null) {
            if (index > -1 && mDatas.size() > index && mDatas.get(index).sameIdWith(object)) {
                mDatas.set(index, object);
            } else {
                int indexNow = indexOfFirst(object);
                if (indexNow >= 0) {
                    mDatas.set(indexNow, object);
                }
            }
        }
    }

    protected int indexOfFirst(E object) {
        if (mDatas != null) {
            for (int i = 0; i < mDatas.size(); i++) {
                if (mDatas.get(i).sameIdWith(object)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void removeObjectAndRefresh(E object) {
        int index = indexOfFirst(object);
        if (index >= -1) {
            removeIndexAndRefresh(index, 1);
        }
    }
}
