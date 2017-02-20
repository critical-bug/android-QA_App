package jp.techacademy.critical_bug.qa_app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

/**
 * 0番目に質問を置き、それへの回答を1番目以降に配置するアダプタ
 */
public class QuestionDetailListAdapter extends BaseAdapter {
    private final static int TYPE_QUESTION = 0;
    private final static int TYPE_ANSWER = 1;

    private final LayoutInflater mLayoutInflater;
    private final Question mQuestion;
    private final DatabaseReference mUserRef;
    private final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();

    public QuestionDetailListAdapter(final Context context, final Question question, final FirebaseUser user) {
        mLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mQuestion = question;
        if (user != null) {
            mUserRef = mDatabase.getReference().child(Const.UsersPATH).child(user.getUid());
        } else {
            mUserRef = null;
        }
    }

    @Override
    public int getCount() {
        return 1 + mQuestion.getAnswers().size();
    }

    @Override
    public Object getItem(final int position) {
        return mQuestion;
    }

    @Override
    public long getItemId(final int position) {
        return 0;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        if (getItemViewType(position) == TYPE_QUESTION) {
            return constructQuestionLayout(convertView != null ? convertView : mLayoutInflater.inflate(R.layout.list_question_detail, parent, false));
        } else {
            final View cv = convertView != null ? convertView : mLayoutInflater.inflate(R.layout.list_answer, parent, false);

            Answer answer = mQuestion.getAnswers().get(position - 1);
            String body = answer.getBody();
            String name = answer.getName();

            TextView bodyTextView = (TextView) cv.findViewById(R.id.bodyTextView);
            bodyTextView.setText(body);

            TextView nameTextView = (TextView) cv.findViewById(R.id.nameTextView);
            nameTextView.setText(name);
            return cv;
        }
    }

    private View constructQuestionLayout(final View convertView) {
        String body = mQuestion.getBody();
        String name = mQuestion.getName();

        TextView bodyTextView = (TextView) convertView.findViewById(R.id.bodyTextView);
        bodyTextView.setText(body);

        TextView nameTextView = (TextView) convertView.findViewById(R.id.nameTextView);
        nameTextView.setText(name);

        byte[] bytes = mQuestion.getImageBytes();
        if (bytes.length != 0) {
            ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView);
            imageView.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length).copy(Bitmap.Config.ARGB_8888, true));
        }

        if (mUserRef != null) {
            final Button starButton = (Button) convertView.findViewById(R.id.starredByUser);
            final DatabaseReference starredRef = mUserRef.child(Const.STARRED_PATH);
            Log.d("getView starredRef", starredRef.toString());
            final View.OnClickListener toStarOnClick = new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final DatabaseReference ref = starredRef.child(mQuestion.getQuestionUid());
                    Log.d("toStarOnClick", ref.toString());
                    ref.setValue(1, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(final DatabaseError databaseError, final DatabaseReference databaseReference) {
                            Log.d("toStarOnClick", "complete");
                        }
                    });
                }
            };
            final View.OnClickListener toUnstarOnClick = new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final DatabaseReference ref = starredRef.child(mQuestion.getQuestionUid());
                    Log.d("toUnstarOnClick", ref.toString());
                    ref.removeValue(new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(final DatabaseError databaseError, final DatabaseReference databaseReference) {
                            Log.d("toUnstarOnClick", "complete");
                        }
                    });
                }
            };
            // star をトグルしたい
            starredRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(final DataSnapshot dataSnapshot) {
                    Log.d("onDataChange", dataSnapshot.toString());
                    final HashMap map = (HashMap) dataSnapshot.getValue();
                    if (map != null && map.containsKey(mQuestion.getQuestionUid())) {
                        starButton.setOnClickListener(toUnstarOnClick);
                        starButton.setText("★");
                    } else {
                        starButton.setOnClickListener(toStarOnClick);
                        starButton.setText("☆");
                    }
                }

                @Override
                public void onCancelled(final DatabaseError databaseError) {
                    Log.d("onCancelled", databaseError.toString());
                }
            });
        }
        return convertView;
    }

    @Override
    public int getItemViewType(final int position) {
        if (position == 0) {
            return TYPE_QUESTION;
        } else {
            return TYPE_ANSWER;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }


}
