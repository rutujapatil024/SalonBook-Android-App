package com.salonbook.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SalonDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "salonbook.db";
    private static final int DATABASE_VERSION = 1;

    // Table
    private static final String TABLE_USERS = "users";
    private static final String COL_ID = "id";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD_HASH = "password_hash";
    private static final String COL_ROLE = "role";
    private static final String COL_FIREBASE_UID = "firebase_uid";

    public SalonDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USERNAME + " TEXT UNIQUE NOT NULL, " +
                COL_PASSWORD_HASH + " TEXT NOT NULL, " +
                COL_ROLE + " TEXT NOT NULL, " +
                COL_FIREBASE_UID + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // Hash password using SHA-256
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return password;
        }
    }

    // INSERT user
    public long insertUser(String username, String password, String role, String firebaseUid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username);
        values.put(COL_PASSWORD_HASH, hashPassword(password));
        values.put(COL_ROLE, role);
        values.put(COL_FIREBASE_UID, firebaseUid);
        long result = db.insert(TABLE_USERS, null, values);
        db.close();
        return result;
    }

    // CHECK user credentials (login)
    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String hashedPassword = hashPassword(password);
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COL_ID},
                COL_USERNAME + "=? AND " + COL_PASSWORD_HASH + "=?",
                new String[]{username, hashedPassword},
                null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    // GET user by username
    public Cursor getUserByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_USERS,
                null,
                COL_USERNAME + "=?",
                new String[]{username},
                null, null, null);
    }

    // GET user role
    public String getUserRole(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COL_ROLE},
                COL_USERNAME + "=?",
                new String[]{username},
                null, null, null);
        String role = "";
        if (cursor.moveToFirst()) {
            role = cursor.getString(cursor.getColumnIndexOrThrow(COL_ROLE));
        }
        cursor.close();
        db.close();
        return role;
    }

    // GET Firebase UID
    public String getFirebaseUid(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COL_FIREBASE_UID},
                COL_USERNAME + "=?",
                new String[]{username},
                null, null, null);
        String uid = "";
        if (cursor.moveToFirst()) {
            uid = cursor.getString(cursor.getColumnIndexOrThrow(COL_FIREBASE_UID));
        }
        cursor.close();
        db.close();
        return uid;
    }

    // UPDATE user
    public int updateUser(String username, ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.update(TABLE_USERS, values, COL_USERNAME + "=?", new String[]{username});
        db.close();
        return rows;
    }

    // DELETE user
    public int deleteUser(String username) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_USERS, COL_USERNAME + "=?", new String[]{username});
        db.close();
        return rows;
    }

    // CHECK if user exists
    public boolean userExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COL_ID},
                COL_USERNAME + "=?",
                new String[]{username},
                null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }
}
