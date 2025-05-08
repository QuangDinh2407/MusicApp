    package com.ck.music_app.Auth;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;


import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ck.music_app.MainActivity;
import com.ck.music_app.R;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.facebook.AccessToken;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.FacebookSdk;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    TextInputEditText email, password;
    MaterialButton btnLogin, btnLoginGG, btnLoginFB;

    FirebaseAuth auth;

    CallbackManager callbackManager;

    GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 9001;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        init();
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void init() {
        email = findViewById(R.id.emailEditText);
        password = findViewById(R.id.passwordEditText);
        btnLogin = findViewById(R.id.loginButton);
        btnLoginGG = findViewById(R.id.googleLoginButton);
        btnLoginFB = findViewById(R.id.facebookLoginButton);

        auth = FirebaseAuth.getInstance();
        callbackManager = CallbackManager.Factory.create();

        // Cấu hình Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("793718457936-dsanvpollknp33gs092bbc2pc39398o0.apps.googleusercontent.com")
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Khởi tạo Facebook SDK
        FacebookSdk.sdkInitialize(getApplicationContext());

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mEmail, mPassword;
                mEmail = email.getText().toString();
                mPassword = password.getText().toString();
                loginWithEmailPassword(mEmail,mPassword);

            }
        });

        btnLoginGG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginWithGG();
            }
        });

        btnLoginFB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginWithFacebook();
            }
        });

        TextView registerTextView = findViewById(R.id.registerTextView);
        registerTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    public void loginWithEmailPassword(String email, String password){
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // Đăng nhập thành công, chuyển sang MainActivity
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Đăng nhập không thành công", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void loginWithGG() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Xử lý kết quả đăng nhập Facebook
        callbackManager.onActivityResult(requestCode, resultCode, data);

        // Xử lý kết quả đăng nhập Google
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {

                System.out.println("Google sign in failed: " + e.getMessage());
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Đăng nhập thành công
                        FirebaseUser user = auth.getCurrentUser();
                        String email = user != null ? user.getEmail() : "";
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("email", email);
                        startActivity(intent);
                        finish();
                    } else {
                        Exception e = task.getException();
                        if (e instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                            // Đã có tài khoản với email này, thử liên kết
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                user.linkWithCredential(credential)
                                        .addOnCompleteListener(this, linkTask -> {
                                            if (linkTask.isSuccessful()) {
                                                String email = user.getEmail();
                                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                                intent.putExtra("email", email);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                Toast.makeText(this, "Liên kết Google thất bại: " + linkTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                Toast.makeText(this, "Vui lòng đăng nhập bằng Facebook hoặc Email trước để liên kết Google.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(this, "Đăng nhập Google không thành công: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void loginWithFacebook() {
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        handleFacebookAccessToken(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        System.out.println("Facebook login cancelled");
                    }

                    @Override
                    public void onError(FacebookException error) {
                        System.out.println("Facebook login error: " + error.getMessage());
                    }
                });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Đăng nhập thành công
                        FirebaseUser user = auth.getCurrentUser();
                        String email = user != null ? user.getEmail() : "";
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("email", email);
                        startActivity(intent);
                        finish();
                    } else {
                        Exception e = task.getException();
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            // Đã có tài khoản với email này, thử liên kết
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                user.linkWithCredential(credential)
                                        .addOnCompleteListener(this, linkTask -> {
                                            if (linkTask.isSuccessful()) {
                                                // Liên kết thành công
                                                String email = user.getEmail();
                                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                                intent.putExtra("email", email);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                Toast.makeText(this, "Liên kết Facebook thất bại: " + linkTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                Toast.makeText(this, "Vui lòng đăng nhập bằng Google trước để liên kết Facebook.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(this, "Đăng nhập Facebook không thành công: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}