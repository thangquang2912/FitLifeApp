package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Post
import com.example.fitlifesmarthealthlifestyleapp.utils.NetworkUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class PersonalProfileFragment : Fragment(R.layout.fragment_personal_profile) {

    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var adapter: PostAdapter

    // Biến hỗ trợ Share
    private var pendingSharePostId: String? = null
    private val shareLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        pendingSharePostId?.let { postId ->
            incrementShareCount(postId)
            pendingSharePostId = null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Ánh xạ View
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val ivAvatar = view.findViewById<ImageView>(R.id.ivUserProfile)
        val tvName = view.findViewById<TextView>(R.id.tvUserName)
        val tvEmail = view.findViewById<TextView>(R.id.tvUserEmail)
        val tvDob = view.findViewById<TextView>(R.id.tvUserDob)

        // Các TextView thống kê
        val tvStatPostCount = view.findViewById<TextView>(R.id.tvStatPostCount) // Số bài viết ở hàng thống kê
        val tvStatFollowers = view.findViewById<TextView>(R.id.tvStatFollowers)
        val tvStatFollowing = view.findViewById<TextView>(R.id.tvStatFollowing)
        val layoutFollowers = view.findViewById<View>(R.id.layoutFollowers)
        val layoutFollowing = view.findViewById<View>(R.id.layoutFollowing)

        val btnBlockedList = view.findViewById<ImageView>(R.id.btnBlockedList)
        val rvMyPosts = view.findViewById<RecyclerView>(R.id.rvMyPosts)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyPosts)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBarProfile)

        // 2. Load Thông tin cá nhân & Thống kê
        loadUserInfoAndStats(ivAvatar, tvName, tvEmail, tvDob, tvStatFollowers, tvStatFollowing, layoutFollowers, layoutFollowing)

        // 3. Cấu hình Adapter với ĐẦY ĐỦ chức năng (Like, Share, Comment)
        adapter = PostAdapter(
            onLikeClick = { post -> toggleLike(post) }, // Xử lý Like
            onCommentClick = { post -> showCommentsDialog(post) }, // Xử lý Comment
            onShareClick = { post -> sharePostContent(post) }, // Xử lý Share
            onLikeCountClick = { userIds -> showLikeListDialog(userIds) }, // Xem danh sách like
            onUserClick = { /* Đang ở trang mình rồi, không cần bấm vào chính mình */ },
            onBlockClick = { _, _ -> /* Không thể tự block chính mình */ }
        )

        rvMyPosts.layoutManager = LinearLayoutManager(context)
        rvMyPosts.adapter = adapter

        // 4. Load bài viết của tôi
        loadMyPosts(progressBar, rvMyPosts, tvEmpty, tvStatPostCount)

        // 5. Sự kiện nút Back & Blocked List
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnBlockedList.setOnClickListener {
            if (NetworkUtils.checkConnection(requireContext())) {
                val dialog = BlockedUsersDialogFragment()
                dialog.show(childFragmentManager, "BlockedUsersDialog")
            }
        }
    }

    // --- PHẦN 1: LOAD INFO ---
    private fun loadUserInfoAndStats(
        iv: ImageView, tvName: TextView, tvEmail: TextView, tvDob: TextView,
        tvFollowers: TextView, tvFollowing: TextView,
        layoutFollowers: View, layoutFollowing: View
    ) {
        if (currentUid == null) return
        db.collection("users").document(currentUid).addSnapshotListener { doc, e ->
            if (e != null || doc == null) return@addSnapshotListener

            val name = doc.getString("displayName") ?: "User"
            val email = doc.getString("email") ?: ""
            val photoUrl = doc.getString("photoUrl")
            val timestamp = doc.getTimestamp("birthday")
            val followers = doc.get("followers") as? List<String> ?: emptyList()
            val following = doc.get("following") as? List<String> ?: emptyList()

            tvName.text = name
            tvEmail.text = email
            tvFollowers.text = followers.size.toString()
            tvFollowing.text = following.size.toString()

            Glide.with(this).load(photoUrl).placeholder(R.drawable.ic_user).circleCrop().into(iv)

            if (timestamp != null) {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                tvDob.text = "Born: ${sdf.format(timestamp.toDate())}"
            }

            // Click xem danh sách Follow
//            layoutFollowers.setOnClickListener {
//                UserListDialogFragment("Followers", followers).show(childFragmentManager, "Followers")
//            }
//            layoutFollowing.setOnClickListener {
//                UserListDialogFragment("Following", following).show(childFragmentManager, "Following")
//            }
        }
    }

    // --- PHẦN 2: LOAD BÀI VIẾT (REALTIME) ---
    private fun loadMyPosts(progressBar: ProgressBar, rv: RecyclerView, tvEmpty: TextView, tvStatPost: TextView?) {
        if (currentUid == null) return

        db.collection("posts")
            .whereEqualTo("userId", currentUid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                progressBar.visibility = View.GONE
                if (e != null) {
                    // LƯU Ý: Nếu lỗi này xảy ra, check Logcat. Có thể do thiếu Index trong Firestore.
                    // Firebase sẽ gửi 1 đường link trong Logcat, bạn cần bấm vào đó để tạo Index.
                    Toast.makeText(context, "Error loading posts: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val posts = snapshot?.toObjects(Post::class.java) ?: emptyList()

                // Cập nhật số lượng bài viết
                tvStatPost?.text = posts.size.toString()
//                tvHeaderPost?.text = "${posts.size} Posts"

                if (posts.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    rv.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    rv.visibility = View.VISIBLE
                    adapter.submitList(posts)
                }
            }
    }

    // --- PHẦN 3: XỬ LÝ TƯƠNG TÁC (QUAN TRỌNG) ---

    // 3.1. Xử lý Like
    private fun toggleLike(post: Post) {
        if (currentUid == null || !NetworkUtils.checkConnection(requireContext())) return

        val postRef = db.collection("posts").document(post.postId)

        if (post.likedBy.contains(currentUid)) {
            // Đang like -> Bỏ like (Giảm 1, Xóa ID)
            postRef.update("likeCount", FieldValue.increment(-1), "likedBy", FieldValue.arrayRemove(currentUid))
        } else {
            // Chưa like -> Like (Tăng 1, Thêm ID)
            postRef.update("likeCount", FieldValue.increment(1), "likedBy", FieldValue.arrayUnion(currentUid))
        }
    }

    // 3.2. Xử lý Comment (Hiện Dialog)
    private fun showCommentsDialog(post: Post) {
        if (NetworkUtils.checkConnection(requireContext())) {
            val dialog = CommentsDialogFragment()
            val bundle = Bundle()
            bundle.putString("postId", post.postId)
            dialog.arguments = bundle
            dialog.show(parentFragmentManager, "CommentsDialog")
        }
    }

    // 3.3. Xử lý Share
    private fun sharePostContent(post: Post) {
        if (!NetworkUtils.checkConnection(requireContext())) return
        pendingSharePostId = post.postId

        val deepLink = "https://fit-life-app-dl.vercel.app/post/${post.postId}"
        val shareMessage = "${post.caption}\n$deepLink"

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            type = "text/plain"
        }
        shareLauncher.launch(Intent.createChooser(sendIntent, "Share via"))
    }

    private fun incrementShareCount(postId: String) {
        if (!NetworkUtils.isNetworkAvailable(requireContext())) return
        db.collection("posts").document(postId)
            .update("shareCount", FieldValue.increment(1))
    }

    // 3.4. Xem danh sách người Like
    private fun showLikeListDialog(userIds: List<String>) {
        if (userIds.isNotEmpty()) {
            val dialog = LikeListDialogFragment(userIds)
            dialog.show(childFragmentManager, "LikeListDialog")
        }
    }
}