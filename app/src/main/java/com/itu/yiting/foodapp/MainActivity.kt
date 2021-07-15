package com.itu.yiting.foodapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.itu.yiting.foodapp.databinding.ActivityMainBinding
import com.squareup.picasso.Picasso

const val REQUEST_SMS_RECEIVE_PERMISSION = 2;

class MainActivity : AppCompatActivity() {

    lateinit var adapter: FoodAdapter
    var foodList = ArrayList<Food>()
    lateinit var database: DatabaseReference

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            R.id.action_add_food -> true
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

//    private fun initData() {
//        val foodlist = ArrayList<Food>()
//        foodlist.add(Food("Coffee","Coffee", "Coffe Descp", "https://images.immediate.co.uk/production/volatile/sites/30/2020/08/flat-white-3402c4f.jpg?quality=90&resize=960,872"))
//        foodlist.add(Food("Icecream", "Icecream", "Icecream Descp", "https://hips.hearstapps.com/hmg-prod.s3.amazonaws.com/images/easiest-ever-fruit-and-coconut-ice-cream-1589550075.jpg"))
//        foodlist.add(Food("Honey", "Honey", "Honey Descp", "https://hips.hearstapps.com/hmg-prod.s3.amazonaws.com/images/gettyimages-183354852-1558479028.jpg"))
//        foodlist.add(Food("French Fry", "French Fry", "French Fry Descp", "https://livinginyellow.com/wp-content/uploads/2020/03/French-Fry-500x375.png"))
//
//        for (food in foodlist) {
//            val foodDb = database.child(food.id)
//            foodDb.child("name").setValue(food.name)
//            foodDb.child("desc").setValue(food.des)
//            foodDb.child("imageUrl").setValue(food.imageUrl)
//        }
//    }

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

}