package com.library.yang.eliyet.eliyetyanglibrary.adapter;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.library.yang.eliyet.eliyetyanglibrary.R;

import java.util.List;

import static com.library.yang.eliyet.eliyetyanglibrary.adapter.BaseLoadMoreAdapter.ViewType.VIEW_TYPE_FOOTER;
import static com.library.yang.eliyet.eliyetyanglibrary.adapter.BaseLoadMoreAdapter.ViewType.VIEW_TYPE_NONE_DATA_TOAST;

/**
 * Created by eliyetyang on 17-4-28.
 * 下拉到底加载更多与无数据时提示的基础adapter。
 * 只支持在RecyclerView使用LinearLayoutManager，GridLayoutManager，StaggeredGridLayoutManager。
 * 继承此view并要进一步添加特殊view（如header）时，请重写 {@link #getCustomViewCount()}方法及{@link #getHeaderCount()}并在各个方法中注意对position进行偏移。
 */

public abstract class BaseLoadMoreAdapter<T extends RecyclerView.ViewHolder, E> extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "BaseLoadMoreAdapter";
    private boolean mIsLoadingMore = true;//默认为true以标示初始化加载，在重新设置数据并刷新界面后置为false
    private boolean mLoadMoreEnable = false;//默认为false防止第一次加载便触发加载更多,在重新设置数据并刷新界面后置为false。
    private boolean mOffsetModel = false;//当前请求是否为行数类接口;

    private LoadMoreListener mLoadMoreListener;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    protected List<E> mDatas;
    private int mStartPage;//默认起始页
    private int mPage = 0;
    private int mPageSize = 20;
    private boolean mIsRefreshing = false;
    private RecyclerView.OnScrollListener mScrollListener;
    private GridLayoutManager mGridLayoutManager;

    public BaseLoadMoreAdapter(int startPage, int pageSize, LoadMoreListener loadMoreListener) {
        mLoadMoreListener = loadMoreListener;
        mStartPage = startPage;
        mPageSize = pageSize;
        mPage = startPage;
        mScrollListener = new ScrollListener();
    }

    public BaseLoadMoreAdapter() {
        this(1, 20, new NullLoadMoreListener());
    }

    public void setLoadMoreListener(LoadMoreListener loadMoreListener) {
        mLoadMoreListener = loadMoreListener;
    }

    public void setGridLayoutManager(GridLayoutManager gridLayoutManager) {
        mGridLayoutManager = gridLayoutManager;
        mGridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return getSpacialSpan(position);
            }
        });
    }

    private int getSpacialSpan(int position) {
        int count = getViewSpanCount(getItemViewType(position));
        if (mGridLayoutManager != null && (getItemViewType(position) == VIEW_TYPE_FOOTER || getItemViewType(position) == VIEW_TYPE_NONE_DATA_TOAST)) {
            count = mGridLayoutManager.getSpanCount();
        }

//        Log.d(TAG, "getSpacialSpan: " + position + " count = " + count);

        return count;
    }

    public int getViewSpanCount(int type) {
        return 1;
    }

    /**
     * 设置当前请求是否为行数类接口
     * 此类请求接口返回的数据数量不定，无法判断是否为最后一页，请自行判断何时停用加载更多并调用{@link #setLoadMoreEnable(boolean)}
     *
     * @param offsetModel
     */
    public void setOffsetModel(boolean offsetModel) {
        this.mOffsetModel = offsetModel;
    }

    /**
     * 设置加载更多是否可用。如果没有根多数据，请调用此方法并传入false。
     *
     * @param enable true为可用，反之为false。
     */
    public void setLoadMoreEnable(boolean enable) {
        mLoadMoreEnable = enable;
//        if (mRecyclerView != null) {
//            mRecyclerView.invalidate();
//        }
    }

    public void setSwipeRefreshLayout(SwipeRefreshLayout swipeRefreshLayout) {
        mSwipeRefreshLayout = swipeRefreshLayout;
        mSwipeRefreshLayout.setOnRefreshListener(this);
    }

    /**
     * 返回当前普通数据的数量。如果没有设定数据源返回 0；
     *
     * @return 当前普通数据的数量。
     */
    public int getDataCount() {
        return mDatas == null ? 0 : mDatas.size();
    }

    /**
     * 刷新数据内容为新数据，并刷新界面。
     * 会开启上拉加载更多功能。
     *
     * @param dataList 新数据列表。
     */
    public void refreshDataAndShow(List<E> dataList) {
        mLoadMoreEnable = true;
        mIsRefreshing = false;
        setDatas(dataList);
        loadDataComplete(0, dataList == null ? 0 : dataList.size());
    }

    /**
     * 设置数据源。如果已经存在，则清空原数据后添加进去。
     *
     * @param dataList 新数据列表。
     */
    private void setDatas(List<E> dataList) {
        mIsRefreshing = false;
        cleanData();
        if (dataList != null && dataList.size() > 0) {
            if (mDatas == null) {
                mDatas = dataList;
            } else {
                mDatas.addAll(dataList);
            }
        } else if (!mOffsetModel) {
            setLoadMoreEnable(false);
        }
    }

    /**
     * 增加新数据。如果原数据不存在，则新数据作为原数据。
     *
     * @param dataList 新数据列表
     */
    private int addNextPageData(List<E> dataList) {
        int start = 0;
        if (dataList != null && dataList.size() > 0) {
            if (mDatas == null) {
                mDatas = dataList;
            } else {
                start = mDatas.size() - 1;
                mDatas.addAll(dataList);
            }
        } else if (!mOffsetModel) {
            setLoadMoreEnable(false);//已经没有数据了，隐藏加载更多。
        }
        return start;
    }

    /**
     * 添加新数据，并刷新界面。
     * 如果新数据未空或数量为0则停用加载更多。
     *
     * @param dataList 新数据列表
     */
    public void addNextPageDataAndShow(List<E> dataList) {
        int start = addNextPageData(dataList);
        loadDataComplete(start, dataList == null ? 0 : dataList.size());
    }

    public void addNextPageDataAndShowSafely(long pageTimestamp, List<E> dataList) {
        if (pageTimestamp == mNextPageTimestamp) {
            addNextPageDataAndShow(dataList);
        }
    }

    /**
     * 根据下标移除数据，请注意在多线程操作时导致的下标问题。
     * 如果所要移除的数据不存在，则不做任何操作。
     *
     * @param index 需要移除的数据在集合中所在的起始位置。
     * @param count 移除的数量
     */
    public void removeIndexAndRefresh(int index, int count) {
        if (mDatas != null && mDatas.size() > index) {
            for (int i = 0; i < count; i++) {
                mDatas.remove(index);
            }
            notifyItemRangeRemoved(index, count);
            if (mDatas != null && mDatas.size() > 0) {
                notifyItemRangeChanged(0, mDatas.size());
            }
        }
    }

    /**
     * 根据对象，移除对应的第一个数据，并刷新界面。
     * 不支持动画。
     *
     * @param data 要移除的数据。
     */
    protected void removeDataAndRefresh(E data) {
        mDatas.remove(data);
        notifyDataSetChanged();
    }

    /**
     * 清空数据，并初始化加载更多不可用。
     */
    public void cleanData() {
        mPage = mStartPage;
        if (mDatas != null) {
            mDatas.clear();
        }
    }

    public abstract int getNoDataToastViewLayoutId();

    @Override
    public final RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_NONE_DATA_TOAST:
                return new NoneDataToastHolder(LayoutInflater.from(parent.getContext()).inflate(getNoDataToastViewLayoutId(), parent, false));
            case VIEW_TYPE_FOOTER:
                return new FooterHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.load_more_footer, parent, false));
            default:
                return onCreateDataViewHolder(parent, viewType);
        }
    }

    /**
     * 创建一般数据的ViewHolder 方法同{@link RecyclerView.Adapter#onBindViewHolder(RecyclerView.ViewHolder, int)}
     *
     * @param parent   父控件
     * @param viewType view类型，在自定义viewType请不要使用小于等于-1000的值，该值作为特殊保留值使用,已使用的值请查看{@link ViewType}。
     * @return
     */
    public abstract T onCreateDataViewHolder(ViewGroup parent, int viewType);

    @Override
    public final void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case VIEW_TYPE_NONE_DATA_TOAST://无数据提示
                return;
            case VIEW_TYPE_FOOTER://加载更多按钮
                return;
            default://一般数据
                onBindViewHolder((T) holder, getItemAt(position), position);
                break;
        }
    }

    /**
     * 方法同{@link android.support.v7.widget.RecyclerView.Adapter#onBindViewHolder(RecyclerView.ViewHolder, int)}
     *
     * @param holder   当前的ViewHolder。
     * @param item     当前数据实体。
     * @param position 当前位置。
     */
    public abstract void onBindViewHolder(T holder, E item, int position);

    /**
     * 返回指定位置的数据实体。
     *
     * @param position 数据在集合中的位置
     * @return
     */
    public E getItemAt(int position) {
        int afterOffsetPosition = position - getHeaderCount();
        if (afterOffsetPosition < 0 || mDatas == null || mDatas.size() == 0 || afterOffsetPosition >= mDatas.size()) {
            return null;
        }
        return mDatas.get(afterOffsetPosition);
    }

    /**
     * 需要修改返回总数据数量的，请重写{@link #getCustomViewCount()}
     *
     * @return
     */
    @Override
    public final int getItemCount() {
        if (mIsRefreshing) {//没有初始化过数据，不现实任何内容。(如有自定义view，则显示自定义view)
            return 0 + getCustomViewCount();
        }
        if (mDatas == null || mDatas.size() == 0) {//已经初始化过数据，但没有可用数据，显示无内容提示。
            return 1 + getCustomViewCount();
        }
        int count = mLoadMoreEnable ? 1 : 0;//存在可用数据，当加载更多功能可用时，增加加载更多footer。
        return mDatas.size() + count + getCustomViewCount();
    }

    /**
     * 如果添加自定义header请重写此方法，将会根据该值进行返回数据的偏移。
     *
     * @return 自定义header的数量
     */
    public int getHeaderCount() {
        return 0;
    }

    /**
     * 需要重写请重写{@link #getDataItemViewType(int)}
     *
     * @param position
     * @return
     */
    @Override
    public final int getItemViewType(int position) {
        if ((!mIsRefreshing) && (mDatas == null || mDatas.size() == 0)) {
            return VIEW_TYPE_NONE_DATA_TOAST;
        } else if (mLoadMoreEnable && position == getItemCount() - 1) {//当前位置未最后一位。
            return VIEW_TYPE_FOOTER;
        } else {
            return getDataItemViewType(position);
        }
    }

    /**
     * 返回常显示的特殊view数量。
     * 如添加一个常显示的header,请重写此方法并返回header在RecyclerView中的数量。
     * 但请注意对所有position进行偏移。
     *
     * @return 特殊view数量。
     */
    public int getCustomViewCount() {
        return 0;
    }

    /**
     * 同{@link android.support.v7.widget.RecyclerView.Adapter#getItemViewType(int)}
     * 添加多type view时，请重写此方法。
     * 当添加其他特殊的自定义View时（会固定显示，并不再data list中的数据）
     * 请重写{@link #getCustomViewCount()}方法，返回此类View在list中的数量。
     *
     * @param position
     * @return
     */
    public int getDataItemViewType(int position) {
        return super.getItemViewType(position);
    }

    /**
     * 数据获取完毕请调用此接口用于隐藏加载更多。
     */
    private void loadDataComplete(int start, int count) {
        mIsLoadingMore = false;
        mIsRefreshing = false;
        notifyDataSetChanged();
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setRefreshing(false);
        }
        checkLoadMore(mRecyclerView);
    }

    /**
     * 加载更多footer用
     */
    private class FooterHolder extends RecyclerView.ViewHolder {
        public FooterHolder(View itemView) {
            super(itemView);
        }
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        //StaggeredGridLayoutManager时，更改footer占全行
        if (getItemViewType(holder.getAdapterPosition()) == VIEW_TYPE_FOOTER) {
            ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();

            if (lp != null && lp instanceof StaggeredGridLayoutManager.LayoutParams) {
                StaggeredGridLayoutManager.LayoutParams p = (StaggeredGridLayoutManager.LayoutParams) lp;
                p.setFullSpan(true);
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        mRecyclerView = recyclerView;
        if (mScrollListener == null) {
            mScrollListener = new ScrollListener();
        }
        mRecyclerView.addOnScrollListener(mScrollListener);
        fitSpacialViewWhenGridUsed(mRecyclerView);
    }

    private void fitSpacialViewWhenGridUsed(RecyclerView recyclerView) {
        if (recyclerView == null) {
            return;
        }
        //GridLayoutManager时，更改footer占全行
        final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof GridLayoutManager) {
            setGridLayoutManager((GridLayoutManager) layoutManager);
        }
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (mScrollListener != null) {
            mRecyclerView.removeOnScrollListener(mScrollListener);
        }
    }

    /**
     * 没有数据提示用
     */
    private class NoneDataToastHolder extends RecyclerView.ViewHolder {
        public NoneDataToastHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cleanData();
                    notifyDataSetChanged();
                    onRefresh();
                }
            });
        }
    }

    /**
     * 加载更多监听。
     */
    public interface LoadMoreListener {
        /**
         * 触发加载更多时，回调此方法，请在次方法中触发加载更多数据的操作，并在加载完毕后调用 {@link #loadDataComplete(int, int)}。
         *
         * @param page     当前页数；
         * @param pageSize 每页数据条数；
         */
        void onLoadMore(long timestamp, int page, int pageSize);

        void onRefresh(int page, int pageSize);
    }

    private static class NullLoadMoreListener implements LoadMoreListener {

        @Override
        public void onLoadMore(long timestamp, int page, int pageSize) {
            //默认空对象，什么都不做。
        }

        @Override
        public void onRefresh(int page, int pageSize) {
            //默认空对象，什么都不做。
        }
    }

    /**
     * 基类持有并处理的特殊view type
     */
    public static class ViewType {
        public final static int VIEW_TYPE_FOOTER = -1000;//加载更多View
        public final static int VIEW_TYPE_NONE_DATA_TOAST = -1001;//无数据提示用View
    }

    private class ScrollListener extends RecyclerView.OnScrollListener {

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            checkLoadMore(recyclerView);
        }
    }

    private boolean checkFooterIsShowing(RecyclerView recyclerView) {
        int lastVisiblePosition = 0;

        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        if (manager instanceof LinearLayoutManager) {

            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) manager;

            lastVisiblePosition = linearLayoutManager.findLastVisibleItemPosition();
        } else if (manager instanceof GridLayoutManager) {

            GridLayoutManager gridLayoutManager = (GridLayoutManager) manager;

            lastVisiblePosition = gridLayoutManager.findLastVisibleItemPosition();
        } else if (manager instanceof StaggeredGridLayoutManager) {

            StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) manager;

            lastVisiblePosition = staggeredGridLayoutManager.findLastVisibleItemPositions(null)[0];
        }

        return lastVisiblePosition == getItemCount() - 1;
    }

    private long mNextPageTimestamp;

    private void checkLoadMore(RecyclerView recyclerView) {
        if (!mIsLoadingMore && mLoadMoreEnable && recyclerView != null) {//当前不在加载中并且加载更多可用
            if (checkFooterIsShowing(recyclerView)) {
                mIsLoadingMore = true;
                if (mLoadMoreListener != null) {
                    mPage++;
                    mNextPageTimestamp = System.currentTimeMillis();
                    mLoadMoreListener.onLoadMore(mNextPageTimestamp, mPage, mPageSize);
                }
            }
        }
    }

    @Override
    public void onRefresh() {
        cleanData();
        mIsRefreshing = true;
        setLoadMoreEnable(false);
        notifyDataSetChanged();
        if (mLoadMoreListener != null) {
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setRefreshing(true);
            }
            mLoadMoreListener.onRefresh(mPage, mPageSize);
        }
    }
}
