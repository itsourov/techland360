package com.techland360.app.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.ads.AdView;
import com.techland360.app.R;
import com.techland360.app.adapters.CommentAdapter;
import com.techland360.app.api.ApiUtilities;
import com.techland360.app.api.HttpParams;
import com.techland360.app.data.constant.AppConstant;
import com.techland360.app.models.comment.Comments;
import com.techland360.app.utility.AdsUtilities;
import com.techland360.app.utility.CommentDialogUtilities;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommentListActivity extends BaseActivity implements CommentDialogUtilities.OnCommentCompleteListener {

    private Activity mActivity;
    private Context mContext;

    private ArrayList<Comments> mCommentList;
    private CommentAdapter mCommentAdapter = null;
    private RecyclerView mRecycler;
    private int mPostId;
    private String mCommentsLink;
    private int mItemCount = 5;
    private boolean mShouldDialogOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initVar();
        initView();
        initFunctionality();
    }

    private void initVar() {
        mActivity = CommentListActivity.this;
        mContext = mActivity.getApplicationContext();

        Intent intent = getIntent();
        if (intent != null) {
            mPostId = intent.getIntExtra(AppConstant.BUNDLE_KEY_POST_ID, 0);
            mCommentsLink = intent.getStringExtra(AppConstant.BUNDLE_KEY_COMMENTS_LINK);
            mShouldDialogOpen = intent.getBooleanExtra(AppConstant.BUNDLE_KEY_DIALOG_OPTION, false);
        }

        mCommentList = new ArrayList<>();
    }

    private void initView() {
        setContentView(R.layout.activity_comment_list);

        mRecycler = (RecyclerView) findViewById(R.id.rvComment);
        mRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mCommentAdapter = new CommentAdapter(mContext, mCommentList);
        mRecycler.setAdapter(mCommentAdapter);

        initToolbar(true);
        setToolbarTitle(getString(R.string.comment_list));
        enableUpButton();
        initLoader();
    }

    private void initFunctionality() {

        loadComments();

        if (!mShouldDialogOpen) {
            // show full-screen ads
            AdsUtilities.getInstance(mContext).showFullScreenAd();
        }
        // show banner ads
        AdsUtilities.getInstance(mContext).showBannerAd((AdView) findViewById(R.id.adsView));
    }

    private void loadComments() {

        showLoader();

        ApiUtilities.getApiInterface().getComments(mCommentsLink, mItemCount).enqueue(new Callback<List<Comments>>() {
            @Override
            public void onResponse(Call<List<Comments>> call, Response<List<Comments>> response) {
                if (response.isSuccessful()) {

                    int totalPages = Integer.parseInt(response.headers().get(HttpParams.TOTAL_PAGE));

                    if (totalPages > 1) {
                        mItemCount = mItemCount * totalPages;
                        loadComments();
                    } else {
                        ArrayList<Comments> model = new ArrayList<>();
                        model.addAll(response.body());
                        mCommentList.clear();
                        for (int i = 0; i < model.size(); i++) {
                            if (model.get(i).getParent() == 0) {
                                mCommentList.add(model.get(i));
                            }
                        }
                        if (mShouldDialogOpen) {
                            mShouldDialogOpen = !mShouldDialogOpen;
                            writeANewComment();
                        }
                        mCommentAdapter.notifyDataSetChanged();
                        if (mCommentList.size() == 0) {
                            showEmptyView();
                        } else {
                            hideLoader();
                        }
                    }

                }
            }

            @Override
            public void onFailure(Call<List<Comments>> call, Throwable t) {
                showEmptyView();
                t.printStackTrace();
            }
        });
    }

    private void writeANewComment() {
        FragmentManager manager = getSupportFragmentManager();
        CommentDialogUtilities dialog = CommentDialogUtilities.newInstance(getString(R.string.write_comment), getString(R.string.ok), getString(R.string.cancel), mPostId);
        dialog.show(manager, AppConstant.BUNDLE_KEY_DIALOG_FRAGMENT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menus_new_comment:
                writeANewComment();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_new_comment, menu);
        return true;
    }

    @Override
    public void onCommentComplete(Boolean isOkPressed) {
        if (isOkPressed) {
            loadComments();
        }
    }
}