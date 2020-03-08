package com.example.android.hola;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    public static final int RC_PHOTO_PICKER=2;
    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public static final int RC_SIGN_IN=1;
    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;
    private FirebaseDatabase mfirebaseDatabase;
    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private String mUsername;
    private ChildEventListener  mchildEventListener;
    private FirebaseStorage firebaseStorage;
    private StorageReference photostorageReference;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mfirebaseDatabase= FirebaseDatabase.getInstance();
        firebaseAuth=FirebaseAuth.getInstance();
        databaseReference= mfirebaseDatabase.getReference().child("messages");
        firebaseStorage=FirebaseStorage.getInstance();
        photostorageReference = firebaseStorage.getReference().child("Photos");

                mUsername = ANONYMOUS;

        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);

        // Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);

            }
        });





        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Send messages on click
                FriendlyMessage friendlyMessage=new FriendlyMessage(mMessageEditText.getText().toString(),mUsername,null,getLocalTime());
                databaseReference.push().setValue(friendlyMessage);
                // Clear input box
                mMessageEditText.setText("");
            }
        });


        authStateListener=new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                if (firebaseUser!=null){
                onSignedIn(firebaseUser.getDisplayName());
                  //  Toast.makeText(MainActivity.this,"YOU ARE LOGGED IN WELCOME BACK",Toast.LENGTH_SHORT).show();
                }
                else {
                    // Choose authentication providers
                    OnSignedOut();
                    List<AuthUI.IdpConfig> providers = Arrays.asList(
                            new AuthUI.IdpConfig.EmailBuilder().build(),
                            new AuthUI.IdpConfig.GoogleBuilder().build()
                            );

// Create and launch sign-in intent
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setAvailableProviders(providers)
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.sign_out_menu :
                AuthUI.getInstance().signOut(this);
                return true;

            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

            if ((requestCode==RC_SIGN_IN) && resultCode == RESULT_OK) {
                Toast.makeText(MainActivity.this, "Logged In", Toast.LENGTH_SHORT).show();
            }
            else if((requestCode==RC_SIGN_IN) &&requestCode==RESULT_CANCELED) {
                Toast.makeText(MainActivity.this,"Sorry to see you go !",Toast.LENGTH_SHORT).show();
                finish();
            }
            else if (requestCode==RC_PHOTO_PICKER && resultCode==RESULT_OK){
                Uri imageUri=data.getData();
                final StorageReference photoRef= photostorageReference.child(imageUri.getLastPathSegment());
                photoRef.putFile(imageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                      photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                          @Override
                          public void onSuccess(Uri uri) {
                              FriendlyMessage friendlyMessage=new FriendlyMessage(null,mUsername,uri.toString(),getLocalTime());
                              databaseReference.push().setValue(friendlyMessage);
                          }
                      });

                    }
                });
            }

        }



    @Override
    protected void onPause() {
        super.onPause();
        firebaseAuth.removeAuthStateListener(authStateListener);
        removeDatabaseListener();
        mMessageAdapter.clear();
    }

    @Override
    protected void onResume(){
        super.onResume();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    private void onSignedIn(String username){
        mUsername=username;
        addDatabaseListener();
    }

    private void OnSignedOut(){
        mUsername=ANONYMOUS;
        mMessageAdapter.clear();
        removeDatabaseListener();

    }

    private void addDatabaseListener() {
        if (mchildEventListener == null) {
            mchildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
                    mMessageAdapter.add(friendlyMessage);
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            };
            databaseReference.addChildEventListener(mchildEventListener);
        }
    }
    private void removeDatabaseListener(){
        if(mchildEventListener!=null){
            databaseReference.removeEventListener(mchildEventListener);
            mchildEventListener=null;
        }
    }


    private String getLocalTime(){

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+1:00"));
        Date currentLocalTime = cal.getTime();
        DateFormat date = new SimpleDateFormat("HH:mm ");

        date.setTimeZone(TimeZone.getTimeZone("GMT+5:30"));
        final String localtime = date.format(currentLocalTime);
        return localtime;

    }


}
