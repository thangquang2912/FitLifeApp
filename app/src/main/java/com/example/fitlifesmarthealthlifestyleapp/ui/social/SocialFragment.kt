package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu // Import quan trọng
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Post
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SocialFragment : Fragment(R.layout.fragment_social) {

    private lateinit var adapter: PostAdapter
    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid

    // Lưu danh sách gốc để lọc
    private var allPosts: List<Post> = emptyList()
    private var currentFilterType = FilterType.ALL

    enum class FilterType { ALL, MY_POSTS, MY_LIKES }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvCommunity = view.findViewById<RecyclerView>(R.id.rvCommunity)
        val fabCreatePost = view.findViewById<FloatingActionButton>(R.id.fabCreatePost)
        val searchView = view.findViewById<SearchView>(R.id.searchViewPost)
        val btnFilter = view.findViewById<ImageView>(R.id.btnFilter) // Ánh xạ nút lọc mới

        // 1. KHỞI TẠO ADAPTER
        adapter = PostAdapter(
            onLikeClick = { post -> toggleLike(post) },
            onCommentClick = { post ->
                val dialog = CommentsDialogFragment()
                val bundle = Bundle()
                bundle.putString("postId", post.postId)
                dialog.arguments = bundle
                dialog.show(parentFragmentManager, "CommentsDialog")
            },
            onLikeCountClick = { userIds ->
                if (userIds.isNotEmpty()) {
                    val dialog = LikeListDialogFragment(userIds)
                    dialog.show(childFragmentManager, "LikeListDialog")
                }
            },
            onUserClick = { userId -> }
        )

        rvCommunity.layoutManager = LinearLayoutManager(context)
        rvCommunity.adapter = adapter

        // 2. SỰ KIỆN TÌM KIẾM
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterPosts(newText, currentFilterType)
                return true
            }
        })

        // 3. SỰ KIỆN BẤM NÚT LỌC (Thay cho ChipGroup)
        btnFilter.setOnClickListener { view ->
            showFilterMenu(view, searchView.query.toString())
        }

        listenToPosts()

        fabCreatePost.setOnClickListener {
            val dialog = CreatePostDialogFragment()
            dialog.show(parentFragmentManager, "CreatePost")
        }
    }

    // --- HÀM HIỂN THỊ MENU LỌC ---
    private fun showFilterMenu(anchor: View, currentQuery: String) {
        val popup = PopupMenu(requireContext(), anchor)

        // Thêm các mục vào menu (ID: 1, 2, 3)
        popup.menu.add(0, 1, 0, "All Posts")
        popup.menu.add(0, 2, 0, "My Posts")
        popup.menu.add(0, 3, 0, "Liked by Me")

        // Đánh dấu tick (check) vào mục đang được chọn
        val checkedIndex = when (currentFilterType) {
            FilterType.ALL -> 0
            FilterType.MY_POSTS -> 1
            FilterType.MY_LIKES -> 2
        }
        popup.menu.getItem(checkedIndex).isChecked = true
        popup.menu.setGroupCheckable(0, true, true)

        // Xử lý khi chọn item
        popup.setOnMenuItemClickListener { item ->
            currentFilterType = when (item.itemId) {
                2 -> FilterType.MY_POSTS
                3 -> FilterType.MY_LIKES
                else -> FilterType.ALL
            }

            // Gọi hàm lọc
            filterPosts(currentQuery, currentFilterType)

            // Đổi màu icon lọc để báo hiệu (Đen = All, Cam = Đang lọc)
            val iconColor = if (currentFilterType == FilterType.ALL) R.color.black else R.color.orange_primary
            (anchor as ImageView).setColorFilter(resources.getColor(iconColor, null))

            true
        }
        popup.show()
    }

    private fun filterPosts(query: String?, filterType: FilterType) {
        val searchText = query?.lowercase()?.trim() ?: ""

        val filteredList = allPosts.filter { post ->
            val matchName = if (searchText.isEmpty()) true else post.userName.lowercase().contains(searchText)
            val matchFilter = when (filterType) {
                FilterType.ALL -> true
                FilterType.MY_POSTS -> post.userId == currentUid
                FilterType.MY_LIKES -> post.likedBy.contains(currentUid)
            }
            matchName && matchFilter
        }
        adapter.submitList(filteredList)
    }

    private fun listenToPosts() {
        db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("SocialFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }
                allPosts = snapshot?.toObjects(Post::class.java) ?: emptyList()

                // Cập nhật UI nhưng vẫn giữ bộ lọc hiện tại
                val searchView = view?.findViewById<SearchView>(R.id.searchViewPost)
                filterPosts(searchView?.query.toString(), currentFilterType)
            }
    }

    private fun toggleLike(post: Post) {
        if (currentUid == null) return
        val postRef = db.collection("posts").document(post.postId)
        if (post.likedBy.contains(currentUid)) {
            postRef.update("likeCount", FieldValue.increment(-1), "likedBy", FieldValue.arrayRemove(currentUid))
        } else {
            postRef.update("likeCount", FieldValue.increment(1), "likedBy", FieldValue.arrayUnion(currentUid))
        }
    }
}