package com.itu.yiting.foodapp

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class FoodAdapter(private val dataSet: ArrayList<Food>, private val ctx: Context) :
    RecyclerView.Adapter<FoodAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val foodImage: ImageView = view.findViewById(R.id.foodItemImage)
        val foodName: TextView = view.findViewById(R.id.foodItemName)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.food_item_layout, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val food = dataSet[position]
        Picasso.get()
            .load(food.imageUrl)
            .into(viewHolder.foodImage)
        viewHolder.foodName.text = food.name

        viewHolder.foodImage.setOnClickListener {
            val intent = Intent(ctx, FoodDetailActivity::class.java).apply {
                putExtra("foodId", food.id)
            }
            ctx.startActivity(intent)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}
