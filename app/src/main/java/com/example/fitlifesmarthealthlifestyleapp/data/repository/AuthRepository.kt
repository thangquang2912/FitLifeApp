package com.example.fitlifesmarthealthlifestyleapp.data.repository

import com.example.fitlifesmarthealthlifestyleapp.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.GoogleAuthProvider

class AuthRepository {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun isSignedIn() : Boolean {
        return firebaseAuth.currentUser != null
    }

    suspend fun signIn(email: String, password: String): Result<Boolean> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun signUp (fullName: String, email: String, password: String) : Result<Boolean> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid

            if (uid != null) {
                val newUser = User(
                    uid = uid,
                    email = email,
                    displayName = fullName
                )

                firestore.collection("users").document(uid).set(newUser).await()
                Result.success(true)
            } else {
                Result.failure(Exception("User ID is null"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<Boolean> {
        return try {
            // Tạo credential từ Google Token
            val credential = GoogleAuthProvider.getCredential(idToken, null)

            // Đăng nhập vào Firebase
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
            val uid = firebaseUser?.uid

            if (uid != null) {
                // Kiểm tra xem User này đã có trong Firestore chưa
                val docSnapshot = firestore.collection("users").document(uid).get().await()

                if (!docSnapshot.exists()) {
                    // Chưa có: Đây là lần đầu login -> Tạo hồ sơ mới
                    val newUser = User(
                        uid = uid,
                        email = firebaseUser.email ?: "",
                        displayName = firebaseUser.displayName ?: "Google User",
                        photoUrl = firebaseUser.photoUrl.toString()
                    )

                    // Lưu vào Firestore
                    firestore.collection("users").document(uid).set(newUser).await()
                }
                // -> Nếu đã có (exists() == true): Không làm gì cả, giữ nguyên dữ liệu cũ.

                Result.success(true)
            } else {
                Result.failure(Exception("Google Sign In failed: UID is null"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Boolean> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            // Có thể check lỗi cụ thể như: FirebaseAuthInvalidUserException (Email không tồn tại)
            Result.failure(e)
        }
    }
}
