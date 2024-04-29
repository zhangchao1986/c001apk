package com.example.c001apk.ui.feed.reply.attopic

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.c001apk.R
import com.example.c001apk.adapter.FooterAdapter
import com.example.c001apk.databinding.ActivityAtTopicBinding
import com.example.c001apk.logic.model.RecentAtUser
import com.example.c001apk.ui.base.BaseActivity
import com.google.android.material.color.MaterialColors
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AtTopicActivity : BaseActivity<ActivityAtTopicBinding>(), OnSearchContainer {

    private val type: String by lazy { intent.getStringExtra("type") ?: "user" }
    private val viewModel by viewModels<AtTopicViewModel>()
    private lateinit var atUserAdapter: AtUserAdapter
    private lateinit var footerAdapter: FooterAdapter
    private lateinit var mLayoutManager: LinearLayoutManager
    private val atList = ArrayList<Int>()
    override var container: OnSearchListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        initView()
        initScroll()
        initEditText()
        initObserve()

    }

    private fun initScroll() {
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val lastVisibleItemPosition =
                        mLayoutManager.findLastVisibleItemPosition()

                    if (lastVisibleItemPosition ==
                        viewModel.listSize + (viewModel.recentAtUsersData.value?.size ?: 0) - 1
                        && !viewModel.isEnd && !viewModel.isRefreshing && !viewModel.isLoadMore
                    ) {
                        loadMore()
                    }
                }
            }
        })
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (binding.fragmentContainer.isVisible) {
                binding.fragmentContainer.isVisible = false
            } else {
                finish()
            }
        }
    }

    private fun initObserve() {
        viewModel.footerState.observe(this) {
            footerAdapter.setLoadState(it)
        }

        if (type == "user") {
            viewModel.recentAtUsersData.observe(this) {
                if (viewModel.page == 1) {
                    viewModel.getFollowList()
                } else {
                    atUserAdapter.submitList(
                        atUserAdapter.currentList.filterNot {
                            it.group == "recent"
                        }
                    )
                }
            }
            viewModel.followListData.observe(this) {
                viewModel.listSize = it.size
                atUserAdapter.submitList(
                    (viewModel.recentAtUsersData.value ?: emptyList()) + it
                )
                binding.indicator.parent.apply {
                    isIndeterminate = false
                    isVisible = false
                }
            }
            viewModel.dataList.observe(this) {

            }
        } else {
            //viewModel.getHotTopics()
        }
    }

    private fun initEditText() {
        binding.editText.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(this, 0)
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            inputType = EditorInfo.TYPE_CLASS_TEXT
            hint = "搜索${if (type == "user") "用户" else "话题"}"
            highlightColor = ColorUtils.setAlphaComponent(
                MaterialColors.getColor(
                    this,
                    com.google.android.material.R.attr.colorPrimaryDark,
                    0
                ), 128
            )
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                @SuppressLint("CommitTransaction")
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    binding.clear.isVisible = s.isNullOrEmpty().not()
                    binding.fragmentContainer.isVisible = s.isNullOrEmpty().not()
                    if (supportFragmentManager.findFragmentById(R.id.fragmentContainer) == null) {
                        supportFragmentManager
                            .beginTransaction()
                            .replace(
                                R.id.fragmentContainer,
                                SearchFragment.newInstance(type, s.toString())
                            )
                            .commit()
                    } else {
                        container?.onSearch(type, s.toString())
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    private fun initView() {
        binding.indicator.parent.apply {
            isIndeterminate = true
            isVisible = true
        }
        binding.clearRecent.setOnClickListener {
            viewModel.clearAll()
        }
        binding.toolBar.apply {
            setNavigationIcon(R.drawable.ic_back)
            setNavigationOnClickListener {
                finish()
            }
        }
        if (type == "user") {
            binding.fab.setOnClickListener {
                val userList = atList.map {
                    atUserAdapter.currentList[it]
                }.distinctBy { it.username }
                viewModel.updateList(userList)

                val list = atList.map {
                    "@${atUserAdapter.currentList[it].username} "
                }.distinct().joinToString(separator = "")

                val intent = Intent()
                intent.putExtra("data", list)
                setResult(RESULT_OK, intent)
                finish()
            }
        }
        atUserAdapter = AtUserAdapter { position, isChecked ->
            if (isChecked)
                atList.add(position)
            else
                atList.remove(position)
            binding.fab.isVisible = atList.isNotEmpty()
        }
        footerAdapter = FooterAdapter(object : FooterAdapter.FooterListener {
            override fun onReLoad() {
                viewModel.isEnd = false
                loadMore()
            }
        })
        mLayoutManager = LinearLayoutManager(this)
        binding.recyclerView.apply {
            layoutManager = mLayoutManager
            adapter = atUserAdapter
            // ConcatAdapter(/*HeaderAdapter(), */atUserAdapter, footerAdapter)
            addItemDecoration(UserItemDecoration(this@AtTopicActivity))
        }
        binding.clear.setOnClickListener {
            binding.editText.text = null
        }
    }

    private fun loadMore() {
        viewModel.isLoadMore = true
        viewModel.getFollowList()
    }

    fun onClickUser(avatar: String, username: String) {
        val userList = atList.map {
            atUserAdapter.currentList[it]
        }.toMutableList().also {
            it.add(
                RecentAtUser(
                    avatar = avatar,
                    username = username
                )
            )
        }.distinctBy { it.username }
        viewModel.updateList(userList)

        val list = atList.map {
            "@${atUserAdapter.currentList[it].username} "
        }.toMutableList().also {
            it.add("@$username ")
        }.distinct().joinToString(separator = "")

        val intent = Intent()
        intent.putExtra("data", list)
        setResult(RESULT_OK, intent)
        finish()
    }

    fun onClickTopic(title: String) {
        val userList = atList.map {
            atUserAdapter.currentList[it]
        }.distinctBy { it.username }
        viewModel.updateList(userList)
        val list = atList.map {
            "@${atUserAdapter.currentList[it].username} "
        }.distinct().joinToString(separator = "")
        val intent = Intent()
        intent.putExtra("data", list)
        setResult(RESULT_OK, intent)
        finish()
    }

}