package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class UserProfileFragment : Fragment(R.layout.fragment_user_profile) {

    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    private var targetUserId: String? = null

    private lateinit var btnFollow: Button
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvDob: TextView
    private lateinit var ivAvatar: ImageView
    private lateinit var tvPostCount: TextView
    private lateinit var tvFollowersCount: TextView
    private lateinit var tvFollowingCount: TextView
    private lateinit var rvPosts: RecyclerView

    private lateinit var postAdapter: PostAdapter
    private var isFollowing = false

    private var userListener: ListenerRegistration? = null
    private var postsListener: ListenerRegistration? = null

    private var pendingSharePostId: String? = null
    private val shareLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        pendingSharePostId?.let { postId ->
            incrementShareCount(postId)
            pendingSharePostId = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetUserId = arguments?.getString(ARG_USER_ID)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (targetUserId == null) {
            Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        // Ánh xạ View
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val btnMore = view.findViewById<ImageView>(R.id.btnMore)
        btnFollow = view.findViewById(R.id.btnFollow)
        tvName = view.findViewById(R.id.tvOtherName)
        tvEmail = view.findViewById(R.id.tvOtherEmail)
        tvDob = view.findViewById(R.id.tvOtherDob)
        ivAvatar = view.findViewById(R.id.ivOtherAvatar)
        tvPostCount = view.findViewById(R.id.tvStatPostCount)
        tvFollowersCount = view.findViewById(R.id.tvStatFollowersCount)
        tvFollowingCount = view.findViewById(R.id.tvStatFollowingCount)
        rvPosts = view.findViewById(R.id.rvOtherPosts)

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        btnMore.setOnClickListener { showOptionsMenu(it) }

        setupRecyclerView()
        loadUserInfo()
        checkFollowStatus()
        loadUserPosts()

        btnFollow.setOnClickListener { toggleFollow() }
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            onLikeClick = { post -> toggleLike(post) },
            onCommentClick = { post -> showCommentsDialog(post) },
            onShareClick = { post -> sharePost(post) },
            onLikeCountClick = { userIds -> showLikeListDialog(userIds) },
            onUserClick = { /* Hiện tại đã ở trang cá nhân này rồi */ },
            onBlockClick = { userId, userName -> /* Logic block đã có trong Menu More */ },
            onImageClick = { imageUrl -> FullScreenImageDialogFragment.show(parentFragmentManager, imageUrl) }
        )
        rvPosts.layoutManager = LinearLayoutManager(context)
        rvPosts.adapter = postAdapter
        rvPosts.isNestedScrollingEnabled = false
    }

    private fun showOptionsMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add("Block User")
        popup.setOnMenuItemClickListener {
            if (it.title == "Block User") showBlockConfirmation()
            true
        }
        popup.show()
    }

    private fun showBlockConfirmation() {
        val name = tvName.text.toString()
        AlertDialog.Builder(requireContext())
            .setTitle("Block User")
            .setMessage("Are you sure you want to block $name? Their posts will be hidden from your feed.")
            .setPositiveButton("Block") { _, _ -> performBlockUser() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performBlockUser() {
        if (!NetworkUtils.checkConnection(requireContext()) || currentUid == null || targetUserId == null) return

        val batch = db.batch()
        val myRef = db.collection("users").document(currentUid!!)
        val targetRef = db.collection("users").document(targetUserId!!)

        batch.update(myRef, "blockedUsers", FieldValue.arrayUnion(targetUserId))
        batch.update(targetRef, "blockedBy", FieldValue.arrayUnion(currentUid))

        // Unfollow lẫn nhau khi block
        batch.update(myRef, "following", FieldValue.arrayRemove(targetUserId))
        batch.update(targetRef, "followers", FieldValue.arrayRemove(currentUid))

        batch.commit().addOnSuccessListener {
            if (isAdded) {
                Toast.makeText(context, "User blocked", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun loadUserInfo() {
        userListener = db.collection("users").document(targetUserId!!)
            .addSnapshotListener { document, _ ->
                if (document == null) return@addSnapshotListener
                tvName.text = document.getString("displayName") ?: "User"
                tvEmail.text = document.getString("email") ?: ""

                val followers = document.get("followers") as? List<*> ?: emptyList<Any>()
                val following = document.get("following") as? List<*> ?: emptyList<Any>()
                tvFollowersCount.text = followers.size.toString()
                tvFollowingCount.text = following.size.toString()

                val dob = document.getTimestamp("dateOfBirth")
                if (dob != null) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    tvDob.text = "Born: ${sdf.format(dob.toDate())}"
                }

                if (isAdded) {
                    Glide.with(this).load(document.getString("photoUrl")).placeholder(R.drawable.ic_user).circleCrop().into(ivAvatar)
                }
            }
    }

    private fun checkFollowStatus() {
        if (currentUid == null) return
        db.collection("users").document(targetUserId!!).addSnapshotListener { snapshot, _ ->
            val followers = snapshot?.get("followers") as? List<String> ?: emptyList()
            isFollowing = followers.contains(currentUid)
            if (isAdded) updateFollowButtonUI()
        }
    }

    private fun updateFollowButtonUI() {
        if (isFollowing) {
            btnFollow.text = "Following"
            btnFollow.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.gray_text)
        } else {
            btnFollow.text = "Follow"
            btnFollow.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.orange_primary)
        }
    }

    private fun toggleFollow() {
        if (currentUid == null || targetUserId == null) return
        val batch = db.batch()
        val myRef = db.collection("users").document(currentUid!!)
        val targetRef = db.collection("users").document(targetUserId!!)

        if (isFollowing) {
            batch.update(myRef, "following", FieldValue.arrayRemove(targetUserId))
            batch.update(targetRef, "followers", FieldValue.arrayRemove(currentUid))
        } else {
            batch.update(myRef, "following", FieldValue.arrayUnion(targetUserId))
            batch.update(targetRef, "followers", FieldValue.arrayUnion(currentUid))

            db.collection("users").document(currentUid!!).get().addOnSuccessListener { myDoc ->
                NotificationHelper.sendNotification(
                    recipientId = targetUserId!!,
                    senderId = currentUid!!,
                    senderName = myDoc.getString("displayName") ?: "User",
                    senderAvatar = myDoc.getString("photoUrl") ?: "",
                    postId = "",
                    type = "FOLLOW"
                )
            }
        }
        batch.commit()
    }

    private fun loadUserPosts() {
        postsListener = db.collection("posts")
            .whereEqualTo("userId", targetUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val posts = snapshot?.toObjects(Post::class.java) ?: emptyList()
                tvPostCount.text = posts.size.toString()
                postAdapter.submitList(posts)
            }
    }

    private fun toggleLike(post: Post) {
        if (currentUid == null) return
        val postRef = db.collection("posts").document(post.postId)

        if (post.likedBy.contains(currentUid)) {
            // Unlike
            postRef.update("likeCount", FieldValue.increment(-1), "likedBy", FieldValue.arrayRemove(currentUid))
        } else {
            // [FIX] Like và Gửi thông báo
            postRef.update("likeCount", FieldValue.increment(1), "likedBy", FieldValue.arrayUnion(currentUid))
                .addOnSuccessListener {
                    // Lấy info của mình để gửi sang cho người kia biết ai like
                    db.collection("users").document(currentUid).get().addOnSuccessListener { myDoc ->
                        NotificationHelper.sendNotification(
                            recipientId = post.userId,
                            senderId = currentUid,
                            senderName = myDoc.getString("displayName") ?: "Someone",
                            senderAvatar = myDoc.getString("photoUrl") ?: "",
                            postId = post.postId,
                            type = "LIKE"
                        )
                    }
                }
        }
    }

    private fun sharePost(post: Post) {
        val deepLink = "https://fit-life-app-dl.vercel.app/post/${post.postId}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, "$deepLink")
            type = "text/plain"
        }
        shareLauncher.launch(Intent.createChooser(intent, "Share"))
    }

    private fun incrementShareCount(postId: String) {
        db.collection("posts").document(postId).update("shareCount", FieldValue.increment(1))
    }

    private fun showCommentsDialog(post: Post) {
        val dialog = CommentsDialogFragment()
        dialog.arguments = Bundle().apply { putString("postId", post.postId) }
        dialog.show(parentFragmentManager, "CommentsDialog")
    }

    private fun showLikeListDialog(userIds: List<String>) {
        if (userIds.isNotEmpty()) LikeListDialogFragment(userIds).show(childFragmentManager, "LikeListDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userListener?.remove()
        postsListener?.remove()
    }

    companion object {
        private const val ARG_USER_ID = "user_id"
        fun newInstance(userId: String) = UserProfileFragment().apply {
            arguments = Bundle().apply { putString(ARG_USER_ID, userId) }
        }
    }
}
