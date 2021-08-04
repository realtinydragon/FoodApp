package com.itu.yiting.foodapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Telephony
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.itu.yiting.foodapp.databinding.ActivityMainBinding
import com.squareup.picasso.Picasso

const val REQUEST_SMS_RECEIVE_PERMISSION = 2;

class MainActivity : AppCompatActivity() {

    lateinit var adapter: FoodAdapter
    var foodList = ArrayList<Food>()
    lateinit var database: DatabaseReference

    private lateinit var binding: ActivityMainBinding

    private val registerTakePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Firebase.auth.currentUser === null) {
            this.redirectToLogin()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        adapter = FoodAdapter(foodList, this)
        binding.grdiview.adapter = adapter
        binding.grdiview.layoutManager = GridLayoutManager(this, 2)

        database = Firebase.database.getReference("food")
        database.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                foodList.clear()
                snapshot.children.forEach {
                    foodList.add(Food(
                        it.key.toString(),
                        it.child("name").value.toString(),
                        it.child("desc").value.toString(),
                        it.child("imageUrl").value.toString(),
                    ))
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
        startListeningSMS()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_logout -> {
                this.logout()
                true
            }
            R.id.action_add_food -> {

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            REQUEST_SMS_RECEIVE_PERMISSION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setUpBroadCastReceiver()
                }
            }
        }
    }

    private fun startSharedFoodDialog(phoneNumber: String, foodId: String) {
        val li = LayoutInflater.from(this)
        val shareDialog = li.inflate(R.layout.share_receive_dialog, null)
        val foodDb = database.child(foodId)
        foodDb.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                Picasso.get()
                    .load(snapshot.child("imageUrl").value.toString())
                    .into(shareDialog.findViewById<ImageView>(R.id.sharedFoodImage))
                shareDialog.findViewById<TextView>(R.id.sharedFoodName).text = snapshot.child("name").value.toString()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
        MaterialAlertDialogBuilder(this)
            .setTitle("Your Friend: $phoneNumber")
            .setView(shareDialog)
            .setPositiveButton("Check Details") { _, _ ->
                val intent = Intent(this, FoodDetailActivity::class.java).apply {
                    putExtra("foodId", foodId)
                }
                startActivity(intent)
            }
            .setNeutralButton("Dismiss") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun checkSMSPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), REQUEST_SMS_RECEIVE_PERMISSION)
            return false
        }
        return true
    }

    private fun startListeningSMS() {
        if (checkSMSPermission()) {
            setUpBroadCastReceiver()
        }
    }

    private fun setUpBroadCastReceiver() {
        val bc = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                for (sms in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    if (sms.displayMessageBody.startsWith("FROM Food App:")) {
                        val regex = Regex("##\\w.+##")
                        val match = regex.find(sms.displayMessageBody)
                        match?.groups?.first()?.value?.let {
                            startSharedFoodDialog(
                                sms.displayOriginatingAddress,
                                it.replace("##", ""))
                        }
                    }
                }
            }
        }
        registerReceiver(bc, IntentFilter("android.provider.Telephony.SMS_RECEIVED"))
    }

    private fun redirectToLogin() {
        this.startActivity(Intent(this.applicationContext, LoginActivity::class.java))
        this.finish()
    }

    private fun logout() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestProfile()
            .requestEmail()
            .build()

        GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener {
            Firebase.auth.signOut()
            this.redirectToLogin()
        }
    }

}