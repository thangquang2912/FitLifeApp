package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.data.repository.UserRepository
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Post
import com.example.fitlifesmarthealthlifestyleapp.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class PostAdapter(
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onLikeCountClick: (List<String>) -> Unit,
    private val onUserClick: (String) -> Unit,
    private val onShareClick: (Post) -> Unit
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    // Cache để tránh load lại thông tin user quá nhiều lần
    private val userCache = mutableMapOf<String, User>()
    private val userRepository = UserRepository()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Ánh xạ View
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivUserAvatar)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvTime: TextView = itemView.findViewById(R.id.tvPostTime)
        private val ivPostImage: ImageView = itemView.findViewById(R.id.ivPostImage)
        private val tvCaption: TextView = itemView.findViewById(R.id.tvCaption)
        private val tvStats: TextView = itemView.findViewById(R.id.tvPostStats)
        private val tvLikes: TextView = itemView.findViewById(R.id.tvLikeCount)
        private val btnLike: ImageView = itemView.findViewById(R.id.btnLike)
        private val btnComment: ImageView = itemView.findViewById(R.id.btnComment)
        private val tvCommentCount: TextView = itemView.findViewById(R.id.tvCommentCount)
        private val ivMore: ImageView = itemView.findViewById(R.id.ivMore)
        private val btnShare: ImageView = itemView.findViewById(R.id.btnShare)

        // Firebase & User
        private val db = FirebaseFirestore.getInstance()
        private val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        private val tvShareCount: TextView = itemView.findViewById(R.id.tvShareCount)
        private var lastClickTime: Long = 0
        fun bind(post: Post) {
            // 1. Hiển thị nội dung Text
            tvCaption.text = post.caption
            tvLikes.text = if (post.likeCount > 0) "${post.likeCount}" else ""
            tvCommentCount.text = if (post.commentCount > 0) "${post.commentCount}" else ""

            // 2. Hiển thị thông số tập luyện (nếu có)
            if (post.duration.isNotEmpty() && post.duration != "0 mins") {
                tvStats.visibility = View.VISIBLE
                val calStr = if (post.calories.isNotEmpty() && post.calories != "0 kcal") " • 🔥 ${post.calories}" else ""
                tvStats.text = "⏱️ ${post.duration}$calStr"
            } else if (post.calories.isNotEmpty() && post.calories != "0 kcal") {
                tvStats.visibility = View.VISIBLE
                tvStats.text = "🔥 ${post.calories}"
            } else {
                tvStats.visibility = View.GONE
            }

            // 3. Hiển thị ngày giờ
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            tvTime.text = sdf.format(post.createdAt.toDate())

            // 4. Load ảnh bài viết
            Glide.with(itemView.context)
                .load(post.postImageUrl)
                .centerCrop()
                .placeholder(R.drawable.bg_search_rounded) // Đổi thành placeholder của bạn nếu cần
                .into(ivPostImage)

            // Sự kiện xem ảnh Fullscreen
            ivPostImage.setOnClickListener {
                val activity = itemView.context as? AppCompatActivity
                activity?.let {
                    FullScreenImageDialogFragment.show(it.supportFragmentManager, post.postImageUrl)
                }
            }

            // 5. Load thông tin người dùng (Avatar + Tên)
            loadUserRealtime(post)

            // Sự kiện bấm vào Avatar -> Sang trang cá nhân
            ivAvatar.setOnClickListener { onUserClick(post.userId) }
            tvUserName.setOnClickListener { onUserClick(post.userId) }

            // 6. Xử lý Like UI & Sự kiện
            val isLiked = post.likedBy.contains(currentUid)
            updateLikeUI(isLiked)

            btnLike.setOnClickListener {
                onLikeClick(post)
            }

            // Sự kiện bấm vào số Like -> Xem danh sách
            tvLikes.setOnClickListener {
                if (post.likedBy.isNotEmpty()) {
                    onLikeCountClick(post.likedBy)
                }
            }

            // 7. Xử lý Comment
            btnComment.setOnClickListener {
                onCommentClick(post)
            }

            // Hiển thị số share
            if (post.shareCount > 0) {
                tvShareCount.visibility = View.VISIBLE
                tvShareCount.text = "${post.shareCount}"
            } else {
                tvShareCount.visibility = View.GONE
            }

            // 8. Xử lý Share
            btnShare.setOnClickListener {
                // Kiểm tra thời gian: Nếu lần bấm này cách lần trước dưới 1 giây (1000ms) thì bỏ qua
                if (android.os.SystemClock.elapsedRealtime() - lastClickTime < 1000) {
                    return@setOnClickListener
                }
                lastClickTime = android.os.SystemClock.elapsedRealtime()

                // Gọi hàm share
                onShareClick(post)
            }

            // 9. Menu 3 chấm (Chỉ hiện nếu là bài của mình)
            if (post.userId == currentUid) {
                ivMore.visibility = View.VISIBLE
                ivMore.setOnClickListener { showOptionsMenu(it, post) }
            } else {
                ivMore.visibility = View.GONE
            }
        }

        private fun loadUserRealtime(post: Post) {
            val cachedUser = userCache[post.userId]
            if (cachedUser != null) {
                applyUserToUI(cachedUser)
            } else {
                // Tạm thời hiện info cũ trong Post trước khi load mới
                tvUserName.text = post.userName
                loadAvatar(post.userAvatar)

                CoroutineScope(Dispatchers.Main).launch {
                    val result = withContext(Dispatchers.IO) { userRepository.getUserDetails(post.userId) }
                    result.getOrNull()?.let { user ->
                        userCache[post.userId] = user
                        applyUserToUI(user)
                    }
                }
            }
        }

        private fun applyUserToUI(user: User) {
            tvUserName.text = user.displayName
            loadAvatar(user.photoUrl)
        }

        private fun loadAvatar(url: String?) {
            Glide.with(itemView.context)
                .load(url)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .circleCrop()
                .into(ivAvatar)
        }

        private fun updateLikeUI(isLiked: Boolean) {
            if (isLiked) {
                btnLike.setImageResource(R.drawable.ic_heart_filled)
                btnLike.setColorFilter(ContextCompat.getColor(itemView.context, R.color.orange_primary))
            } else {
                btnLike.setImageResource(R.drawable.ic_heart_outline)
                btnLike.setColorFilter(ContextCompat.getColor(itemView.context, R.color.gray_text))
            }
        }

        private fun showOptionsMenu(view: View, post: Post) {
            val popup = PopupMenu(view.context, view)
            popup.menu.add("Edit")
            popup.menu.add("Delete")

            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Delete" -> {
                        db.collection("posts").document(post.postId).delete()
                            .addOnSuccessListener {
                                Toast.makeText(view.context, "Post deleted", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(view.context, "Error deleting post", Toast.LENGTH_SHORT).show()
                            }
                        true
                    }
                    "Edit" -> {
                        val activity = itemView.context as? AppCompatActivity
                        if (activity != null) {
                            val editDialog = EditPostDialogFragment()
                            val bundle = Bundle().apply {
                                putString("postId", post.postId)
                                putString("caption", post.caption)
                                putString("duration", post.duration)
                                putString("calories", post.calories)
                                putString("imageUrl", post.postImageUrl)
                            }
                            editDialog.arguments = bundle
                            editDialog.show(activity.supportFragmentManager, "EditPostDialog")
                        }
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem.postId == newItem.postId
        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem == newItem
    }
}