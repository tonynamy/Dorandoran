package com.iseokchan.dorandoran

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.iseokchan.dorandoran.models.User
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    private val RC_SIGN_IN = 9001;
    private val TAG = "Google Login"

    private lateinit var googleSignInClient:GoogleSignInClient

    private lateinit var rootRef: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        auth = FirebaseAuth.getInstance()
        rootRef = FirebaseFirestore.getInstance()

        btn_googleSignIn.setOnClickListener { _ ->
            signIn()
        }
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        currentUser?.let {
            loginOrRegister(it)
        }

    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
                // ...
            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id!!)

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser

                    user?.let {
                        loginOrRegister(it)
                    }

                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Snackbar.make(loginLayout, R.string.loginFailed, Snackbar.LENGTH_SHORT).show()
                    updateUI(null)
                }

                // ...
            }
    }

    private fun loginOrRegister(firebaseUser: FirebaseUser) {

        val uid = firebaseUser.uid
        val userName = firebaseUser.displayName
        val user = User(uid, userName!!)

        val uidRef = rootRef.collection("users").document(uid)

        Snackbar.make(loginLayout, R.string.checkingUser, Snackbar.LENGTH_SHORT).show()

        uidRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {

                val document = task.result

                if (document?.exists() == false) {

                    Snackbar.make(loginLayout, R.string.creatingUser, Snackbar.LENGTH_SHORT).show()

                    uidRef.set(user)

                }

                updateUI(firebaseUser)
            }
        }


    }

    private fun updateUI(user: FirebaseUser?) {

        if( user !== null) {

            val intent = Intent(this, ChatListActivity::class.java)
            startActivity(intent)
            finish()

        } else {
            Snackbar.make(loginLayout, R.string.loginFailed, Snackbar.LENGTH_SHORT).show()
        }

    }

}
