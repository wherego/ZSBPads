package com.lcc.msdq.favorite;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.github.johnpersano.supertoasts.SuperToast;
import com.lcc.adapter.FavAdapter;
import com.lcc.adapter.JSAdapter;
import com.lcc.entity.CompanyTest;
import com.lcc.entity.FavEntity;
import com.lcc.frame.Propertity;
import com.lcc.frame.data.DataManager;
import com.lcc.frame.fragment.base.BaseLazyLoadFragment;
import com.lcc.msdq.R;
import com.lcc.msdq.area.LoginDialogFragment;
import com.lcc.msdq.index.article.IndexMenuWebView;
import com.lcc.msdq.look.fav.ArticlesWebView;
import com.lcc.msdq.look.fav.LookComQuestionIndexActivity;
import com.lcc.msdq.look.fav.LookQuestionsActivity;
import com.lcc.msdq.test.answer.AnswerIndexActivity;
import com.lcc.mvp.presenter.FavPresenter;
import com.lcc.mvp.presenter.JSPresenter;
import com.lcc.mvp.presenter.impl.FavPresenterImpl;
import com.lcc.mvp.presenter.impl.JSPresenterImpl;
import com.lcc.mvp.view.FavView;
import com.lcc.mvp.view.JSView;
import com.lcc.utils.CoCoinToast;
import com.lcc.utils.SharePreferenceUtil;

import java.util.List;
import zsbpj.lccpj.frame.FrameManager;
import zsbpj.lccpj.utils.TimeUtils;
import zsbpj.lccpj.view.recyclerview.listener.OnRecycleViewScrollListener;

/**
 * Author:       梁铖城
 * Email:        1038127753@qq.com
 * Date:         2015年11月21日15:28:25
 * Description:  ArticleFragment
 */
public class ArticleFragment extends BaseLazyLoadFragment implements SwipeRefreshLayout.OnRefreshListener,
        FavView, FavAdapter.OnItemClickListener {
    protected static final int DEF_DELAY = 1000;
    protected final static int STATE_LOAD = 0;
    protected final static int STATE_NORMAL = 1;
    protected int currentState = STATE_NORMAL;
    protected long currentTime = 0;
    protected int currentPage = 1;

    private FavAdapter adapter;
    private SwipeRefreshLayout mSwipeRefreshWidget;
    private RecyclerView mRecyclerView;
    private FavPresenter mPresenter;
    private String type = "文章";
    private boolean isOpen=false;

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            isOpen=true;
        }else {
            if (isOpen) {
                isOpen = false;
            }
        }
    }

    public static ArticleFragment newInstance(String type) {
        ArticleFragment mFragment = new ArticleFragment();
        Bundle bundle = new Bundle();
        bundle.putString("type", type);
        mFragment.setArguments(bundle);
        return mFragment;
    }

    @Override
    public int initContentView() {
        return R.layout.fav_list_fragment;
    }

    @Override
    public void getBundle(Bundle bundle) {
        type = bundle.getString("type");
    }

    @Override
    public void initUI(View view) {
        mPresenter = new FavPresenterImpl(this);
        initRefreshView(view);
        initRecycleView(view);
    }

    private void initRefreshView(View view) {
        mSwipeRefreshWidget = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_widget);
        mSwipeRefreshWidget.setColorSchemeResources(R.color.colorPrimary);
        mSwipeRefreshWidget.setOnRefreshListener(this);
    }

    private void initRecycleView(View view) {
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity(),
                LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        adapter = new FavAdapter();
        adapter.setOnItemClickListener(this);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.addOnScrollListener(new OnRecycleViewScrollListener() {
            @Override
            public void onLoadMore() {
                if (currentState == STATE_NORMAL) {
                    currentState = STATE_LOAD;
                    currentTime = TimeUtils.getCurrentTime();
                    adapter.setHasFooter(true);
                    mRecyclerView.scrollToPosition(adapter.getItemCount() - 1);
                    currentPage++;
                    mPresenter.loadMore(currentPage,type);
                }
            }
        });
    }

    @Override
    public void initData() {
        currentPage = 1;
        mPresenter.getData(currentPage, type);
    }

    @Override
    public void getLoading() {
        showProgress(true);
    }

    @Override
    public void getDataEmpty() {
        showEmpty(true);
    }

    @Override
    public void getDataFail(String msg) {
        showError(true);
    }

    @Override
    public void refreshOrLoadFail(String msg) {
        if (mSwipeRefreshWidget.isRefreshing()) {
            mSwipeRefreshWidget.setRefreshing(false);
            showError(true);
        } else {
            FrameManager.getInstance().toastPrompt(msg);
        }
    }

    @Override
    public void refreshDataSuccess(final List<FavEntity> entities) {
        if (entities != null && entities.size() > 0) {
            adapter.bind(entities);
            showContent(true);
        }else {
            showEmpty(true);
        }
        mSwipeRefreshWidget.setRefreshing(false);
    }

    @Override
    public void loadMoreWeekDataSuccess(final List<FavEntity> entities) {

        int delay = 0;
        if (TimeUtils.getCurrentTime() - currentTime < DEF_DELAY) {
            delay = DEF_DELAY;
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                currentState = STATE_NORMAL;
                if (entities.isEmpty()) {
                    adapter.setHasMoreDataAndFooter(false, false);
                    FrameManager.getInstance().toastPrompt("没有更多数据...");
                } else {
                    adapter.appendToList(entities);
                    adapter.setHasMoreDataAndFooter(true, false);
                }
                adapter.notifyDataSetChanged();
            }
        }, delay);
    }

    @Override
    public void onItemClick(FavEntity data) {
        Intent intent = null;
        if (type.equals(Propertity.Article.NAME)) {
            //去文章的界面
            ArticlesWebView.startIndexMenuWebView(getActivity(),data);
        } else if (type.equals(Propertity.Test.QUESTION)) {
            //资料的界面
            LookQuestionsActivity.startLookQuestionsActivity(data,getActivity());
        } else if (type.equals(Propertity.COM.QUESTION)){
            //去公司的问题
            LookComQuestionIndexActivity.startLookQuestionsActivity(data,getActivity());
        }

    }

    @Override
    public void onRefresh() {
        mSwipeRefreshWidget.postDelayed(new Runnable() {
            @Override
            public void run() {
                currentPage = 1;
                mSwipeRefreshWidget.setRefreshing(true);
                mPresenter.refresh(currentPage,type);
            }
        }, 500);
    }

    @Override
    public void onReloadClicked() {
        currentPage = 1;
        mPresenter.getData(currentPage, type);
    }

    @Override
    public void checkToken() {
        if (isOpen){
            DataManager.deleteAllUser();
            SharePreferenceUtil.setUserTk("");
            FrameManager.getInstance().toastPrompt("身份失效请重现登录");
            LoginDialogFragment dialog = new LoginDialogFragment();
            dialog.show(getActivity().getFragmentManager(), "loginDialog");
        }
    }
}
