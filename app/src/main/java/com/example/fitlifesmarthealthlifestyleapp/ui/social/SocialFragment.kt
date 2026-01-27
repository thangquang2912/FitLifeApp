package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitlifesmarthealthlifestyleapp.DeepLinkViewModel // [MỚI]
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

    private var allPosts: List<Post> = emptyList()
    private var currentFilterType = FilterType.ALL

    // ViewModel và biến lưu ID lọc
    private lateinit var deepLinkViewModel: DeepLinkViewModel
    private var targetPostId: String? = null

    enum class FilterType { ALL, MY_POSTS, MY_LIKES }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvCommunity = view.findViewById<RecyclerView>(R.id.rvCommunity)
        val fabCreatePost = view.findViewById<FloatingActionButton>(R.id.fabCreatePost)
        val searchView = view.findViewById<SearchView>(R.id.searchViewPost)
        val btnFilter = view.findViewById<ImageView>(R.id.btnFilter)

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
            onUserClick = { userId -> },
            onShareClick = { post -> sharePostContent(post) }
        )

        rvCommunity.layoutManager = LinearLayoutManager(context)
        rvCommunity.adapter = adapter

        // [MỚI] LẮNG NGHE VIEWMODEL
        deepLinkViewModel = ViewModelProvider(requireActivity())[DeepLinkViewModel::class.java]

        // Quan sát thay đổi (Đề phòng trường hợp tab đã mở sẵn)
        deepLinkViewModel.targetPostId.observe(viewLifecycleOwner) { id ->
            if (id != null) {
                targetPostId = id
                Toast.makeText(context, "Đang tìm bài viết...", Toast.LENGTH_SHORT).show()
                // Gọi lại bộ lọc nếu dữ liệu đã tải xong
                if (allPosts.isNotEmpty()) {
                    filterPostsWithDeepLink()
                }
            }
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterPosts(newText, currentFilterType)
                return true
            }
        })

        btnFilter.setOnClickListener { view ->
            showFilterMenu(view, searchView.query.toString())
        }

        listenToPosts()

        fabCreatePost.setOnClickListener {
            val dialog = CreatePostDialogFragment()
            dialog.show(parentFragmentManager, "CreatePost")
        }
    }

    private fun sharePostContent(post: Post) {
        val deepLink = "https://fit-life-app-dl.vercel.app/post/${post.postId}"

        val shareMessage = "${post.userName} vừa đăng tải bài viết trên FitLife!\nXem chi tiết tại: ${deepLink}"

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "Share via"))
    }

    private fun showFilterMenu(anchor: View, currentQuery: String) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 1, 0, "All Posts")
        popup.menu.add(0, 2, 0, "My Posts")
        popup.menu.add(0, 3, 0, "Liked by Me")

        popup.setOnMenuItemClickListener { item ->
            currentFilterType = when (item.itemId) {
                2 -> FilterType.MY_POSTS
                3 -> FilterType.MY_LIKES
                else -> FilterType.ALL
            }
            filterPosts(currentQuery, currentFilterType)
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

    // [MỚI] Hàm lọc riêng cho DeepLink
    private fun filterPostsWithDeepLink() {
        val id = targetPostId ?: return
        val targetPost = allPosts.find { it.postId == id }

        if (targetPost != null) {
            adapter.submitList(listOf(targetPost))
            // Xóa ID trong ViewModel để lần sau không bị lọc lại
            deepLinkViewModel.clearPostId()
            targetPostId = null
        } else {
            // Nếu không tìm thấy (có thể do mạng chậm, cứ để đó chờ update tiếp theo)
            if (allPosts.isNotEmpty()) {
                // Nếu đã load hết mà vẫn không thấy -> Báo lỗi
                Toast.makeText(context, "Bài viết không tồn tại hoặc đã bị xóa", Toast.LENGTH_SHORT).show()
                deepLinkViewModel.clearPostId()
                targetPostId = null
                filterPosts("", currentFilterType)
            }
        }
    }

    private fun listenToPosts() {
        db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                allPosts = snapshot?.toObjects(Post::class.java) ?: emptyList()

                // Logic lọc ưu tiên DeepLink
                if (targetPostId != null) {
                    filterPostsWithDeepLink()
                } else {
                    val searchView = view?.findViewById<SearchView>(R.id.searchViewPost)
                    filterPosts(searchView?.query.toString(), currentFilterType)
                }
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