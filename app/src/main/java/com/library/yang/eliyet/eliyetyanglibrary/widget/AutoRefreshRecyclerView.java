package com.library.yang.eliyet.eliyetyanglibrary.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import com.library.yang.eliyet.eliyetyanglibrary.adapter.BaseLoadMoreAdapter;

/**
 * Created by eliyetyang on 17-6-15.
 */

public class AutoRefreshRecyclerView extends SwipeRefreshLayout {
    private RecyclerView mRecyclerView;
    private BaseLoadMoreAdapter mAdapter;

    public AutoRefreshRecyclerView(Context context) {
        super(context);
        init();
    }

    public AutoRefreshRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        initRecyclerView();
        addView(mRecyclerView);
    }

    private void initRecyclerView() {
        mRecyclerView = new RecyclerView(getContext());
        LayoutParams recyclerViewParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mRecyclerView.setLayoutParams(recyclerViewParams);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    public void setAdapter(@NonNull BaseLoadMoreAdapter adapter) {
        mAdapter = adapter;
        mAdapter.setSwipeRefreshLayout(this);
        setOnRefreshListener(mAdapter);
        mRecyclerView.setAdapter(adapter);
    }

    /**
     * 请在setAdapter之前使用
     *
     * @param layoutManager
     */
    public void setLayoutManager(RecyclerView.LayoutManager layoutManager) {
        mRecyclerView.setLayoutManager(layoutManager);
        if (layoutManager instanceof GridLayoutManager && mAdapter != null) {
            mAdapter.setGridLayoutManager((GridLayoutManager) layoutManager);
        }
    }

    public void setItemAnimator(RecyclerView.ItemAnimator animator) {
        mRecyclerView.setItemAnimator(animator);
    }

    public void setLoadMoreListener(BaseLoadMoreAdapter.LoadMoreListener loadMoreListener) {
        mAdapter.setLoadMoreListener(loadMoreListener);
    }

    public void refresh() {
        mAdapter.onRefresh();
    }
}
