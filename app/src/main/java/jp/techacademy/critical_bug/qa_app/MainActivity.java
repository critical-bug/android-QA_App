package jp.techacademy.critical_bug.qa_app;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static final int MENU_ITEM_ID_STARRED = 9;
    private Toolbar mToolbar;
    private int mGenre;
    private QuestionListAdapter mAdapter;
    private ArrayList<Question> mQuestionArrayList;
    private DatabaseReference mDatabaseReference;
    private ListView mListView;
    private DatabaseReference mGenreRef;
    private final ChildEventListener mGenreChangeEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(final DataSnapshot dataSnapshot, final String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();
            Question question = constructQuestionFromMap(dataSnapshot.getKey(), mGenre, map);
            mQuestionArrayList.add(question);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildChanged(final DataSnapshot dataSnapshot, final String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();

            // 変更があったQuestionを探す
            for (Question question: mQuestionArrayList) {
                if (dataSnapshot.getKey().equals(question.getQuestionUid())) {
                    // このアプリで変更がある可能性があるのは回答(Answer)のみ
                    constructAnswerList(map, question.getAnswers());

                    mAdapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void onChildRemoved(final DataSnapshot dataSnapshot) {
        }

        @Override
        public void onChildMoved(final DataSnapshot dataSnapshot, final String s) {
        }

        @Override
        public void onCancelled(final DatabaseError databaseError) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) {
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                    return;
                }

                // ジャンルを選択していない場合（mGenre == 0）はエラーを表示するだけ
                if (mGenre == 0) {
                    Snackbar.make(view, "ジャンルを選択して下さい", Snackbar.LENGTH_LONG).show();
                    return;
                }

                Intent intent = new Intent(getApplicationContext(), QuestionSendActivity.class);
                intent.putExtra("genre", mGenre);
                startActivity(intent);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, mToolbar, R.string.app_name, R.string.app_name);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Menu m = navigationView.getMenu();
            m.add(1, MENU_ITEM_ID_STARRED, Menu.NONE, R.string.starred);
            m.setGroupCheckable(1, true, true);
        }
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
                int id = item.getItemId();
                Log.d("NavigationItemSelected", String.valueOf(id));

                if (id == MENU_ITEM_ID_STARRED) {
                    mToolbar.setTitle(R.string.starred);
                    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                    drawer.closeDrawer(GravityCompat.START);

                    mQuestionArrayList.clear();
                    mListView.setAdapter(mAdapter);

                    if (mGenreRef != null) {
                        mGenreRef.removeEventListener(mGenreChangeEventListener);
                    }
                    DatabaseReference ref = mDatabaseReference.child(Const.UsersPATH)
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .child(Const.STARRED_PATH);
                    Log.d("NavigationItemSelected", ref.toString());
                    // お気に入り質問全てについて呼ばれるリスナ
                    ref.addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(final DataSnapshot dataSnapshot, final String s) {
                            Log.d("ChildEventListener", dataSnapshot.toString());
                            final int genre = (int)(long) dataSnapshot.getValue();
                            DatabaseReference questionRef = mDatabaseReference.child(Const.ContentsPATH)
                                    .child(String.valueOf(genre))
                                    .child(dataSnapshot.getKey());
                            questionRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(final DataSnapshot dataSnapshot) {
                                    Log.d("questionRef", dataSnapshot.getKey());
                                    HashMap map = (HashMap) dataSnapshot.getValue();
                                    mQuestionArrayList.add(constructQuestionFromMap(dataSnapshot.getKey(), genre, map));
                                    mAdapter.notifyDataSetChanged(); // お気に入り質問1個ごとに呼ばれるの無駄っぽい
                                }
                                @Override
                                public void onCancelled(final DatabaseError databaseError) {
                                }
                            });
                        }
                        @Override
                        public void onChildChanged(final DataSnapshot dataSnapshot, final String s) {
                        }
                        @Override
                        public void onChildRemoved(final DataSnapshot dataSnapshot) {
                        }
                        @Override
                        public void onChildMoved(final DataSnapshot dataSnapshot, final String s) {
                        }
                        @Override
                        public void onCancelled(final DatabaseError databaseError) {
                        }
                    });
                } else {
                    if (id == R.id.nav_hobby) {
                        mToolbar.setTitle("趣味");
                        mGenre = 1;
                    } else if (id == R.id.nav_life) {
                        mToolbar.setTitle("生活");
                        mGenre = 2;
                    } else if (id == R.id.nav_health) {
                        mToolbar.setTitle("健康");
                        mGenre = 3;
                    } else if (id == R.id.nav_compter) {
                        mToolbar.setTitle("コンピューター");
                        mGenre = 4;
                    }

                    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                    drawer.closeDrawer(GravityCompat.START);

                    // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
                    mQuestionArrayList.clear();
                    mListView.setAdapter(mAdapter);

                    // 選択したジャンルにリスナーを登録する
                    if (mGenreRef != null) {
                        mGenreRef.removeEventListener(mGenreChangeEventListener);
                    }
                    mGenreRef = mDatabaseReference.child(Const.ContentsPATH).child(String.valueOf(mGenre));
                    mGenreRef.addChildEventListener(mGenreChangeEventListener);
                }
                return true;
            }
        });

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        mListView = (ListView) findViewById(R.id.listView);
        mAdapter = new QuestionListAdapter(this);
        mQuestionArrayList = new ArrayList<Question>();
        mAdapter.setQuestionArrayList(mQuestionArrayList);
        mAdapter.notifyDataSetChanged();
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                Intent intent = new Intent(getApplicationContext(), QuestionDetailActivity.class);
                intent.putExtra("question", mQuestionArrayList.get(position));
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(getApplicationContext(), SettingActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @NonNull
    private static Question constructQuestionFromMap(final String key, final int genre, final HashMap map) {
        String imageString = (String) map.get("image");
        byte[] bytes;
        if (imageString != null) {
            bytes = Base64.decode(imageString, Base64.DEFAULT);
        } else {
            bytes = new byte[0];
        }

        ArrayList<Answer> answerArrayList = new ArrayList<Answer>();
        constructAnswerList(map, answerArrayList);

        return new Question((String) map.get("title"),
                (String) map.get("body"),
                (String) map.get("name"),
                (String) map.get("uid"),
                key,
                genre,
                bytes,
                answerArrayList);
    }

    private static void constructAnswerList(final HashMap map, final ArrayList<Answer> answerArrayList) {
        answerArrayList.clear();
        HashMap answerMap = (HashMap) map.get("answers");
        if (answerMap != null) {
            for (Object key : answerMap.keySet()) {
                HashMap temp = (HashMap) answerMap.get(key);
                String answerBody = (String) temp.get("body");
                String answerName = (String) temp.get("name");
                String answerUid = (String) temp.get("uid");
                Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);
                answerArrayList.add(answer);
            }
        }
    }
}
