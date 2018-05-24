package de.zipperle.cowtracker.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.core.SignInStateChangeListener;
import com.amazonaws.mobile.auth.facebook.FacebookButton;
import com.amazonaws.mobile.auth.ui.AuthUIConfiguration;
import com.amazonaws.mobile.auth.ui.SignInUI;
import com.amazonaws.mobile.client.AWSMobileClient;

import de.zipperle.cowtracker.R;

public class AuthenticatorActivity extends AppCompatActivity {

    private String LOG_TAG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticator);

        LOG_TAG = this.getClass().getSimpleName();

        AWSMobileClient.getInstance().initialize(this).execute();

        // Sign-in listener
        IdentityManager.getDefaultIdentityManager().addSignInStateChangeListener(new SignInStateChangeListener() {
            @Override
            public void onUserSignedIn() {
                Log.d(LOG_TAG, "User Signed In");
            }

            // Sign-out listener
            @Override
            public void onUserSignedOut() {
                Log.d(LOG_TAG, "User Signed Out");
                showSignIn();
            }
        });

        showSignIn();
    }

    /*
     * Display the AWS SDK sign-in/sign-up UI
     */
    private void showSignIn(){
        Log.d(LOG_TAG, "showSignIn");
        AuthUIConfiguration config = new AuthUIConfiguration.Builder()
                .userPools(true)  // true? show the Email and Password UI
                .signInButton(FacebookButton.class)
                .logoResId(R.mipmap.cow_foreground) // Change the logo
                .backgroundColor(Color.GRAY) // Change the backgroundColor
                .isBackgroundColorFullScreen(true) // Full screen backgroundColor the backgroundColor full screenff
                .fontFamily("sans-serif-light") // Apply sans-serif-light as the global font
                .canCancel(true)
                .build();
        SignInUI signin = (SignInUI) AWSMobileClient.getInstance().getClient(AuthenticatorActivity.this, SignInUI.class);
        signin.login(AuthenticatorActivity.this, MapsActivity.class).authUIConfiguration(config).execute();
    }
}
