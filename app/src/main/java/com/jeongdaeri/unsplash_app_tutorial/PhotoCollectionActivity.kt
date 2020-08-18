package com.jeongdaeri.unsplash_app_tutorial

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.jeongdaeri.unsplash_app_tutorial.model.Photo
import com.jeongdaeri.unsplash_app_tutorial.recyclerview.PhotoGridRecyeclerViewAdapter
import com.jeongdaeri.unsplash_app_tutorial.utils.Constants.TAG
import kotlinx.android.synthetic.main.activity_photo_collection.*

class PhotoCollectionActivity: AppCompatActivity() {


    // 데이터
    private var photoList = ArrayList<Photo>()

    // 어답터
    private lateinit var photoGridRecyeclerViewAdapter: PhotoGridRecyeclerViewAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_collection)

        val bundle = intent.getBundleExtra("array_bundle")

        val searchTerm = intent.getStringExtra("search_term")

        photoList = bundle?.getSerializable("photo_array_list") as ArrayList<Photo>


        Log.d(TAG, "PhotoCollectionActivity - onCreate() called / searchTerm : $searchTerm, photoList.count() : ${photoList.count()}")


        top_app_bar.title = searchTerm


        this.photoGridRecyeclerViewAdapter = PhotoGridRecyeclerViewAdapter()

        this.photoGridRecyeclerViewAdapter.submitList(photoList)



        my_photo_recycler_view.layoutManager = GridLayoutManager(this,
                                                                2,
                                                                GridLayoutManager.VERTICAL,
                                                                false)
        my_photo_recycler_view.adapter = this.photoGridRecyeclerViewAdapter


    } //

}
