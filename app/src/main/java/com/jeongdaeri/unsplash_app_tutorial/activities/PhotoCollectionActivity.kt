package com.jeongdaeri.unsplash_app_tutorial.activities

import android.app.SearchManager
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.jeongdaeri.unsplash_app_tutorial.R
import com.jeongdaeri.unsplash_app_tutorial.model.Photo
import com.jeongdaeri.unsplash_app_tutorial.model.SearchData
import com.jeongdaeri.unsplash_app_tutorial.recyclerview.ISearchHistoryRecyclerView
import com.jeongdaeri.unsplash_app_tutorial.recyclerview.PhotoGridRecyeclerViewAdapter
import com.jeongdaeri.unsplash_app_tutorial.recyclerview.SearchHistoryRecyclerViewAdapter
import com.jeongdaeri.unsplash_app_tutorial.retrofit.RetrofitManager
import com.jeongdaeri.unsplash_app_tutorial.utils.Constants.TAG
import com.jeongdaeri.unsplash_app_tutorial.utils.RESPONSE_STATUS
import com.jeongdaeri.unsplash_app_tutorial.utils.SharedPrefManager
import com.jeongdaeri.unsplash_app_tutorial.utils.toSimpleString
import kotlinx.android.synthetic.main.activity_photo_collection.*
import java.util.*
import kotlin.collections.ArrayList

class PhotoCollectionActivity: AppCompatActivity(),
                                SearchView.OnQueryTextListener,
                                CompoundButton.OnCheckedChangeListener,
                                View.OnClickListener,
                                ISearchHistoryRecyclerView
{


    // 데이터
    private var photoList = ArrayList<Photo>()

    // 검색 기록 배열
    private var searchHistoryList = ArrayList<SearchData>()

    // 어답터
    // lateinit 을 통해 나중에 메모리에 올라가도 된다.
    private lateinit var photoGridRecyeclerViewAdapter: PhotoGridRecyeclerViewAdapter
    private lateinit var mySearchHistoryRecyclerViewAdapter: SearchHistoryRecyclerViewAdapter


    // 서치뷰
    private lateinit var mySearchView: SearchView

    // 서치뷰 에딧 텍스트
    private lateinit var mySearchViewEditText: EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_collection)

        val bundle = intent.getBundleExtra("array_bundle")

        val searchTerm = intent.getStringExtra("search_term")

        Log.d(TAG, "PhotoCollectionActivity - onCreate() called / searchTerm : $searchTerm, photoList.count() : ${photoList.count()}")




        search_history_mode_switch.setOnCheckedChangeListener(this)
        clear_search_history_buttton.setOnClickListener(this)

        search_history_mode_switch.isChecked = SharedPrefManager.checkSearchHistoryMode()

        top_app_bar.title = searchTerm

        // 액티비티에서 어떤 액션바를 사용할지 설정한다.
        setSupportActionBar(top_app_bar)

        photoList = bundle?.getSerializable("photo_array_list") as ArrayList<Photo>


        // 사진 리사이클러뷰 세팅
        this.photoCollectionRecyclerViewSetting(this.photoList)


        // 저장된 검색 기록 가져오기
        this.searchHistoryList = SharedPrefManager.getSearchHistoryList() as ArrayList<SearchData>

        this.searchHistoryList.forEach {
            Log.d(TAG, "저장된 검색 기록 - it.term : ${it.term} , it.timestamp: ${it.timestamp}")
        }

        handleSearchViewUi()

        // 검색 기록 리사이클러뷰 준비
        this.searchHistoryRecyclerViewSetting(this.searchHistoryList)

        if(searchTerm.isNotEmpty()){
            val term = searchTerm?.let {
                it
            }?: ""
            this.insertSearchTermHistory(term)
        }


    } //

    // 검색 기록 리사이클러뷰 준비
    private fun searchHistoryRecyclerViewSetting(searchHistoryList: ArrayList<SearchData>){
        Log.d(TAG, "PhotoCollectionActivity - searchHistoryRecyclerViewSetting() called")

        //
        this.mySearchHistoryRecyclerViewAdapter = SearchHistoryRecyclerViewAdapter(this)
        this.mySearchHistoryRecyclerViewAdapter.submitList(searchHistoryList)

        val myLinearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        myLinearLayoutManager.stackFromEnd = true

        search_history_recycler_view.apply {
            layoutManager = myLinearLayoutManager
            this.scrollToPosition(mySearchHistoryRecyclerViewAdapter.itemCount - 1)
            adapter = mySearchHistoryRecyclerViewAdapter
        }

    }


    // 그리드 사진 리사이클러뷰 세팅
    private fun photoCollectionRecyclerViewSetting(photoList: ArrayList<Photo>){
        Log.d(TAG, "PhotoCollectionActivity - photoCollecitonRecyclerViewSetting() called")

        this.photoGridRecyeclerViewAdapter = PhotoGridRecyeclerViewAdapter()

        this.photoGridRecyeclerViewAdapter.submitList(photoList)

        my_photo_recycler_view.layoutManager = GridLayoutManager(this,
            2,
            GridLayoutManager.VERTICAL,
            false)
        my_photo_recycler_view.adapter = this.photoGridRecyeclerViewAdapter

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        Log.d(TAG, "PhotoCollectionActivity - onCreateOptionsMenu() called")

        val inflater = menuInflater

        inflater.inflate(R.menu.top_app_bar_menu, menu)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager

        this.mySearchView = menu?.findItem(R.id.search_menu_item)?.actionView as SearchView

        this.mySearchView.apply {
            this.queryHint = "검색어를 입력해주세요"

            this.setOnQueryTextListener(this@PhotoCollectionActivity)

            this.setOnQueryTextFocusChangeListener { _, hasExpaned ->
                when(hasExpaned) {
                    true -> {
                        Log.d(TAG, "서치뷰 열림")
                        linear_search_history_view.visibility = View.VISIBLE

                        handleSearchViewUi()
                    }
                    false -> {
                        Log.d(TAG, "서치뷰 닫힘")
                        linear_search_history_view.visibility = View.INVISIBLE
                    }
                }
            }

            // 서치뷰에서 에딧텍스트를 가져온다.
            mySearchViewEditText = this.findViewById(androidx.appcompat.R.id.search_src_text)
        }


        this.mySearchViewEditText.apply {
            this.filters = arrayOf(InputFilter.LengthFilter(12))
            this.setTextColor(Color.WHITE)
            this.setHintTextColor(Color.WHITE)
        }


        return true
    }


    // 서치뷰 검색어 입력 이벤트
    // 검색버튼이 클릭되었을때
    override fun onQueryTextSubmit(query: String?): Boolean {

        Log.d(TAG, "PhotoCollectionActivity - onQueryTextSubmit() called / query: $query")


        if(!query.isNullOrEmpty()){
            this.top_app_bar.title = query

            //TODO:: api 호출
            //TODO:: 검색어 저장
            this.insertSearchTermHistory(query)
            this.searchPhotoApiCall(query)
        }

//        this.mySearchView.setQuery("", false)
//        this.mySearchView.clearFocus()

        this.top_app_bar.collapseActionView()

        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        Log.d(TAG, "PhotoCollectionActivity - onQueryTextChange() called / newText: $newText")

//        val userInputText = newText ?: ""

        val userInputText = newText.let {
            it
        }?: ""

        if(userInputText.count() == 12){
            Toast.makeText(this, "검색어는 12자 까지만 입력 가능합니다.", Toast.LENGTH_SHORT).show()
        }

        return true
    }


    override fun onCheckedChanged(switch: CompoundButton?, isChecked: Boolean) {
        when(switch){
            search_history_mode_switch ->{
                if(isChecked == true){
                    Log.d(TAG, "검색어 저장기능 온")
                    SharedPrefManager.setSearchHistoryMode(isActivated = true)
                } else {
                    Log.d(TAG, "검색어 저장기능 오프")
                    SharedPrefManager.setSearchHistoryMode(isActivated = false)
                }
            }

        }
    }

    override fun onClick(view: View?) {
        when(view){
            clear_search_history_buttton -> {
                Log.d(TAG, "검색 기록 삭제 버튼이 클릭 되었다.")
                SharedPrefManager.clearSearchHistoryList()
                this.searchHistoryList.clear()
                // ui 처리
                handleSearchViewUi()
            }
        }
    }

    // 검색 아이템삭제 버튼 이벤트
    override fun onSearchItemDeleteClicked(position: Int) {
        Log.d(TAG, "PhotoCollectionActivity - onSearchItemDeleteClicked() called / position: $position")
        // 해당 요소 삭제
        this.searchHistoryList.removeAt(position)
        // 데이터 덮어쓰기
        SharedPrefManager.storeSearchHistoryList(this.searchHistoryList)
        // 데이터 변경 됬다고 알려줌
        this.mySearchHistoryRecyclerViewAdapter.notifyDataSetChanged()

        handleSearchViewUi()
    }

    // 검색 아이템 버튼 이벤트
    override fun onSearchItemClicked(position: Int) {
        Log.d(TAG, "PhotoCollectionActivity - onSearchItemClicked() called / position: $position")
        // TODO:: 해당 녀석의 검색어로 API 호출

        val queryString = this.searchHistoryList[position].term

        searchPhotoApiCall(queryString)

        top_app_bar.title = queryString

        this.insertSearchTermHistory(searchTerm = queryString)

        this.top_app_bar.collapseActionView()


    }


    // 사진 검색 API 호출
    private fun searchPhotoApiCall(query: String){

        RetrofitManager.instance.searchPhotos(searchTerm = query, completion = { status, list ->
            when(status){
                RESPONSE_STATUS.OKAY -> {
                    Log.d(TAG, "PhotoCollectionActivity - searchPhotoApiCall() called 응답 성공 / list.size : ${list?.size}")

                    if (list != null){
                        this.photoList.clear()
                        this.photoList = list
                        this.photoGridRecyeclerViewAdapter.submitList(this.photoList)
                        this.photoGridRecyeclerViewAdapter.notifyDataSetChanged()
                    }

                }
                RESPONSE_STATUS.NO_CONTENT -> {
                    Toast.makeText(this, "$query 에 대한 검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        })

    }


    private fun handleSearchViewUi(){
        Log.d(TAG, "PhotoCollectionActivity - handleSearchViewUi() called / size : ${this.searchHistoryList.size}")

        if(this.searchHistoryList.size > 0){
            search_history_recycler_view.visibility = View.VISIBLE
            search_history_recycler_view_label.visibility = View.VISIBLE
            clear_search_history_buttton.visibility = View.VISIBLE
        } else {
            search_history_recycler_view.visibility = View.INVISIBLE
            search_history_recycler_view_label.visibility = View.INVISIBLE
            clear_search_history_buttton.visibility = View.INVISIBLE
        }

    }

    // 검색어 저장
    private fun insertSearchTermHistory(searchTerm: String){
        Log.d(TAG, "PhotoCollectionActivity - insertSearchTermHistory() called")

        if(SharedPrefManager.checkSearchHistoryMode() == true){
            // 중복 아이템 삭제
            var indexListToRemove = ArrayList<Int>()

            this.searchHistoryList.forEachIndexed{ index, searchDataItem ->

                if(searchDataItem.term == searchTerm){
                    Log.d(TAG, "index: $index")
                    indexListToRemove.add(index)
                }
            }

            indexListToRemove.forEach {
                this.searchHistoryList.removeAt(it)
            }

            // 새 아이템 넣기
            val newSearchData = SearchData(term = searchTerm, timestamp = Date().toSimpleString())
            this.searchHistoryList.add(newSearchData)

            // 기존 데이터에 덮어쓰기
            SharedPrefManager.storeSearchHistoryList(this.searchHistoryList)

            this.mySearchHistoryRecyclerViewAdapter.notifyDataSetChanged()
        }

    }


}














