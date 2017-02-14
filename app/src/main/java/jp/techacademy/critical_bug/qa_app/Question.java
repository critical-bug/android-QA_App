package jp.techacademy.critical_bug.qa_app;

import java.io.Serializable;
import java.util.ArrayList;

public class Question implements Serializable {
    private String mTitle;
    private String mBody;
    private String mName;
    private String mUid;
    private String mQuestionUid;
    private int mGenre;
    private byte[] mImageBytes;
    private ArrayList<Answer> mAnswers;

    public String getTitle() {
        return mTitle;
    }

    public String getBody() {
        return mBody;
    }

    public String getName() {
        return mName;
    }

    public String getUid() {
        return mUid;
    }

    public String getQuestionUid() {
        return mQuestionUid;
    }

    public int getGenre() {
        return mGenre;
    }

    public byte[] getImageBytes() {
        return mImageBytes;
    }

    public ArrayList<Answer> getAnswers() {
        return mAnswers;
    }

    public Question(final String title, final String body, final String name, final String uid, final String questionUid, final int genre, final byte[] imageBytes, final ArrayList<Answer> answers) {
        mTitle = title;
        mBody = body;
        mName = name;
        mUid = uid;
        mQuestionUid = questionUid;
        mGenre = genre;
        mImageBytes = imageBytes;
        mAnswers = answers;
    }
}
