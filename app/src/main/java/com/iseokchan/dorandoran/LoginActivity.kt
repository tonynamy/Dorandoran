package com.iseokchan.dorandoran

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.iseokchan.dorandoran.DoranDoranApplication.Companion.firebaseAuth
import com.iseokchan.dorandoran.DoranDoranApplication.Companion.rootRef
import com.iseokchan.dorandoran.classes.ForceUpdateChecker
import com.iseokchan.dorandoran.models.User
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        ForceUpdateChecker.with(this)
            .onUpdateNeeded(object : ForceUpdateChecker.OnUpdateNeededListener {
                override fun onUpdateNeeded(updateUrl: String?) {
                    val dialog: AlertDialog =
                        AlertDialog.Builder(this@LoginActivity)
                            .setTitle(getString(R.string.newVersionAvailable))
                            .setMessage(getString(R.string.pleaseUpdate))
                            .setPositiveButton(getString(R.string.update)) { _, _ ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                            }.setNegativeButton(getString(R.string.noThanks)) { _, _ ->
                                ActivityCompat.finishAffinity(this@LoginActivity)
                            }.create()
                    dialog.show()
                }

            }).check()

        btn_googleSignIn.setOnClickListener { _ ->
            signIn()
        }
    }

    public override fun onStart() {
        super.onStart()
        firebaseAuth.currentUser?.let {
            loginOrRegister(it)
        }

    }

    private fun signIn() {
        GoogleSignIn.getClient(this, DoranDoranApplication.googleSignInOptions)?.let {
            val signInIntent = it.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)
            } catch (e: ApiException) {
            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser

                    user?.let {
                        loginOrRegister(it)
                    }

                } else {
                    Snackbar.make(loginLayout, R.string.loginFailed, Snackbar.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    private fun loginOrRegister(firebaseUser: FirebaseUser) {

        val uid = firebaseUser.uid
        val userName = firebaseUser.displayName
        val email = firebaseUser.email
        val profileImage = getProfileImageByProvider(firebaseUser.photoUrl, firebaseUser.providerId)

        val user = User(userName, email, profileImage)

        val uidRef = rootRef.collection("users").document(uid)

        Snackbar.make(loginLayout, R.string.checkingUser, Snackbar.LENGTH_SHORT).show()

        uidRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {

                val document = task.result

                if (document?.exists() == false) {

                    Snackbar.make(loginLayout, R.string.creatingUser, Snackbar.LENGTH_SHORT).show()

                    uidRef.set(user).addOnCompleteListener {
                        updateUI(firebaseUser)
                    }

                } else {
                    updateUI(firebaseUser)
                }

            }
        }


    }

    private fun getProfileImageByProvider(photoURL: Uri?, providerId: String) : String? {

        var profileImage:String? = null

        if(photoURL == null) return profileImage

        profileImage = when {
            providerId.contains("google") -> { // GOOGLE
                photoURL.toString()
            }
            providerId.contains("facebook") -> { // FACEBOOK
                "${photoURL.toString()}?type=large"
            }
            else -> {
                photoURL.toString()
            }
        }

        return profileImage
    }

    private fun updateUI(user: FirebaseUser?) {

        if( user !== null) {

            val intent = Intent(this, ChatListActivity::class.java)
            startActivity(intent)
            ActivityCompat.finishAffinity(this)

        } else {
            Snackbar.make(loginLayout, R.string.loginFailed, Snackbar.LENGTH_SHORT).show()
        }

    }

}
