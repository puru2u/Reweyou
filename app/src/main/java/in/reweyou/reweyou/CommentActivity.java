package in.reweyou.reweyou;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.androidnetworking.interfaces.StringRequestListener;
import com.google.gson.Gson;
import com.klinker.android.sliding.MultiShrinkScroller;
import com.klinker.android.sliding.SlidingActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import in.reweyou.reweyou.adapter.CommentsAdapter;
import in.reweyou.reweyou.classes.UserSessionManager;
import in.reweyou.reweyou.model.CommentModel;
import in.reweyou.reweyou.model.ReplyCommentModel;

public class CommentActivity extends SlidingActivity {

    private static final String TAG = CommentActivity.class.getName();
    public EditText editText;
    private ImageView send;
    private TextView replyheader;
    private UserSessionManager userSessionManager;
    private String threadid;
    private String tempcommentid;

    @Override
    protected void configureScroller(MultiShrinkScroller scroller) {
        super.configureScroller(scroller);
        scroller.setIntermediateHeaderHeightRatio(0);

    }

    @Override
    public void init(Bundle savedInstanceState) {
        disableHeader();
        enableFullscreen();


        setContent(R.layout.content_comment);

        try {
            threadid = getIntent().getStringExtra("threadid");
        } catch (Exception e) {
            e.printStackTrace();
        }
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        editText = (EditText) findViewById(R.id.edittext);
        send = (ImageView) findViewById(R.id.send);


        userSessionManager = new UserSessionManager(this);
        replyheader = (TextView) findViewById(R.id.t2);

        final CommentsAdapter adapterComment = new CommentsAdapter(this);
        recyclerView.setAdapter(adapterComment);

        initSendButton();
        initTextWatcherEditText();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                AndroidNetworking.get("https://reweyou.in/reviews/samplecomment.php")
                        .setTag("report")

                        .setPriority(Priority.HIGH)
                        .build()
                        .getAsJSONArray(new JSONArrayRequestListener() {
                            @Override
                            public void onResponse(JSONArray response) {
                                Log.d(TAG, "onResponse: " + response);
                                Gson gson = new Gson();
                                List<Object> list = new ArrayList<>();

                                try {
                                    for (int i = 0; i < response.length(); i++) {
                                        JSONObject json = response.getJSONObject(i);
                                        JSONArray jsonReply = json.getJSONArray("reply");
                                        CommentModel coModel = gson.fromJson(json.toString(), CommentModel.class);
                                        list.add(coModel);
                                        for (int j = 0; j < jsonReply.length(); j++) {
                                            JSONObject jsontemp = jsonReply.getJSONObject(j);
                                            ReplyCommentModel temp = gson.fromJson(jsontemp.toString(), ReplyCommentModel.class);
                                            list.add(temp);
                                        }


                                    }
                                    adapterComment.add(list);


                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }

                            @Override
                            public void onError(ANError anError) {

                            }
                        });
            }
        }, 500);

    }

    private void initSendButton() {
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) CommentActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                if (editText.getText().toString().trim().length() > 0) {


                    HashMap<String, String> hashMap = new HashMap<String, String>();
                    hashMap.put("uid", userSessionManager.getUID());
                    hashMap.put("threadid", threadid);
                    hashMap.put("image", "");
                    String url;
                    if (replyheader.getVisibility() == View.VISIBLE) {
                        url = "https://www.reweyou.in/google/create_reply.php";
                        // hashMap.put("commentid", tempcommentid);
                        hashMap.put("commentid", tempcommentid);
                        hashMap.put("reply", editText.getText().toString());

                        Log.d(TAG, "onClick: reply");
                    } else {
                        url = "https://www.reweyou.in/google/create_comments.php";
                        hashMap.put("comment", editText.getText().toString());

                    }


                    AndroidNetworking.post(url)
                            .addBodyParameter(hashMap)
                            .setTag("comment")
                            .setPriority(Priority.HIGH)
                            .build()
                            .getAsString(new StringRequestListener() {
                                @Override
                                public void onResponse(String response) {
                                    Log.d(TAG, "onResponse: " + response);
                                    if (response.equals("Comment created")) {

                                    }
                                }

                                @Override
                                public void onError(ANError anError) {
                                    Log.d(TAG, "onError: anerror" + anError);
                                }
                            });
                }


            }
        });
        send.setClickable(false);
        send.setTag("0");
    }

    private void initTextWatcherEditText() {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().length() == 0) {
                    send.setTag("0");
                    send.setClickable(false);
                    send.animate().scaleY(0.0f).scaleX(0.0f).setDuration(200).setInterpolator(new AccelerateInterpolator()).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            send.setImageResource(R.drawable.button_send_disable);
                            send.animate().scaleX(1.0f).scaleY(1.0f).setInterpolator(new DecelerateInterpolator()).setDuration(300);
                        }
                    });

                } else if (s.toString().trim().length() > 0) {

                    send.setClickable(true);

                    if (!send.getTag().toString().equals("1")) {
                        send.setTag("1");
                        send.animate().scaleY(0.0f).scaleX(0.0f).setDuration(200).setInterpolator(new AccelerateInterpolator()).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                send.setImageResource(R.drawable.button_send_comments);

                                send.animate().scaleX(1.0f).scaleY(1.0f).setInterpolator(new DecelerateInterpolator()).setDuration(300);
                            }
                        });
                    }

                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    public void passClicktoEditText(String s, String commentid) {
        this.tempcommentid = commentid;
        if (replyheader.getVisibility() == View.GONE) {
            replyheader.setVisibility(View.VISIBLE);
            editText.setHint("Write a reply...");

        } else {
            replyheader.setVisibility(View.GONE);
            editText.setHint("Write a comment...");

        }

        replyheader.setText("Reply to " + s);
        editText.setFocusableInTouchMode(true);
        editText.requestFocus();
        editText.performClick();
        final InputMethodManager inputMethodManager = (InputMethodManager) this
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }
}
