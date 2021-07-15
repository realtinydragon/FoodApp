package com.itu.yiting.foodapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.itu.yiting.foodapp.databinding.ActivityLoginBinding
import com.squareup.picasso.Picasso

const val RC_SIGN_IN = 1

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var signInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestProfile()
            .requestEmail()
            .build()

        signInClient = GoogleSignIn.getClient(this, gso)
        auth = Firebase.auth

        binding.login.setOnClickListener {
            if (auth.currentUser == null) {
                signIn()
            } else {
                navigateToMain()
            }
        }
        binding.logoutAccount.setOnClickListener {
            switchAccount()
        }
    }

    override fun onStart() {
        super.onStart()
        updateLoginUI(auth.currentUser)
    }

    private fun updateLoginUI(user: FirebaseUser?) {
        if (user == null) {
            binding.logoutAccount.visibility = View.INVISIBLE
            binding.loginUsername.text = "Please Sign In"
            binding.loginUserProfile.setImageResource(R.drawable.ic_baseline_account_box)
        } else {
            binding.logoutAccount.visibility = View.VISIBLE
            binding.loginUsername.text = "${user.displayName}\n${user.email}"
            Picasso.get()
                .load(user.photoUrl)
                .placeholder(R.drawable.ic_baseline_account_box)
                .into(binding.loginUserProfile)
        }

    }


    private fun signIn() {
        val signInIntent = signInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun switchAccount() {
        signInClient.signOut().addOnCompleteListener {
            updateLoginUI(null)
            signIn()
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d("TAG", "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("TAG", "signInWithCredential:success")
                    updateLoginUI(auth.currentUser)
                    navigateToMain()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("TAG", "signInWithCredential:failure", task.exception)
                }
            }
    }

}