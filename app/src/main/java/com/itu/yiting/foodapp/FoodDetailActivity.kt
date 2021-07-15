package com.itu.yiting.foodapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso


const val REQUEST_SMS_SEND_PERMISSION = 1;

class FoodDetailActivity: AppCompatActivity() {
    private val commentList = ArrayList<Comment>()
    private lateinit var foodId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_details)

        foodId = intent.extras?.getString("foodId")?: ""

        val commentAdapter = CommentAdapter(commentList)
        val foodCommentList = findViewById<RecyclerView>(R.id.foodCommentList)
        foodCommentList.adapter = commentAdapter
        foodCommentList.layoutManager = LinearLayoutManager(this)

        val database = Firebase.database.getReference("food")
        val foodDb = database.child(foodId)
        val commentsDb = foodDb.child("comments")

        setupShareBtn(foodId)
        setUpUI(foodDb)

        val foodBtn = findViewById<Button>(R.id.foodBtn)
        foodBtn.text = "Comment"
        foodBtn.setOnClickListener {
            showCommentDialog(commentsDb)
        }

        commentsDb.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                commentList.clear()
                snapshot.children.forEach {
                    val comment = Comment(
                        it.child("commenter").value.toString(),
                        it.child("photoUrl").value.toString(),
                        it.child("text").value.toString())
                    commentList.add(comment)
                }
                commentAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            REQUEST_SMS_SEND_PERMISSION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showShareDialog(foodId)
                }
            }
        }
    }

    private fun setUpUI(foodDb: DatabaseReference) {
        val ratingBar = findViewById<RatingBar>(R.id.foodRating)
        val foodImage = findViewById<ImageView>(R.id.foodImage)
        val toolbar = findViewById<Toolbar>(R.id.foodDetailToolBar)
        val foodDesc = findViewById<TextView>(R.id.foodDesc)
        toolbar.setNavigationOnClickListener {
            this.finish()
        }
        ratingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser) {
                foodDb.child("rating")
                    .setValue(rating)
            }
        }
        foodDb.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                Picasso.get()
                    .load(snapshot.child("imageUrl").value.toString())
                    .into(foodImage)

                val rate = snapshot.child("rating")
                val value = rate.getValue<Float>()
                if (value != null) {
                    ratingBar.rating = value
                }

                toolbar.title = snapshot.child("name").value.toString()
                foodDesc.text = snapshot.child("desc").value.toString()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun checkSMSPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), REQUEST_SMS_SEND_PERMISSION)
            return false
        }
        return true
    }

    private fun setupShareBtn(foodId: String) {
        val shareBtn = findViewById<Button>(R.id.shareBtn)
        shareBtn.setOnClickListener {
            if (checkSMSPermission()) {
                showShareDialog(foodId)
            }
        }
    }

    private fun showShareDialog(foodId: String) {
        val li = LayoutInflater.from(this)
        val shareDialog = li.inflate(R.layout.share_dialog, null)
        MaterialAlertDialogBuilder(this)
            .setTitle("Share with your friend!")
            .setView(shareDialog)
            .setPositiveButton("Send") { _, _ ->
                val sms = SmsManager.getDefault()
                val number = shareDialog.findViewById<EditText>(R.id.sharePhoneNumber)
                sms.sendTextMessage(number.text.toString(), "ME", "FROM Food App: Check out this amazing food, I bet you'll like it! ##$foodId##", null, null)
                Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun showCommentDialog(commentsDb: DatabaseReference) {
        val li = LayoutInflater.from(this)
        val commentDialog = li.inflate(R.layout.comment_dialog, null)
        MaterialAlertDialogBuilder(this)
            .setTitle("Left your comments")
            .setView(commentDialog)
            .setPositiveButton("Send") { _, _ ->
                val commentArea = commentDialog.findViewById<EditText>(R.id.commentArea)
                commentsDb.push().key?.let {
                    val commentDb = commentsDb.child(it)
                    Firebase.auth.currentUser?.let { me ->
                        commentDb.child("commenter").setValue(me.displayName)
                        commentDb.child("email").setValue(me.email)
                        commentDb.child("photoUrl").setValue(me.photoUrl.toString())
                        commentDb.child("text").setValue(commentArea.text.toString())
                    }
                }
            }.show()
    }
}