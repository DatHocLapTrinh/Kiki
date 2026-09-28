package com.example.security

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class GoogleAuthResult {
    data class Success(
        val uid: String,
        val email: String,
        val displayName: String?,
        val photoUrl: String?,
        val isNewUser: Boolean
    ) : GoogleAuthResult()

    data class Error(val message: String) : GoogleAuthResult()
    object Cancelled : GoogleAuthResult()
}

@Singleton
class FirebaseAuthManager @Inject constructor(
    @param:ApplicationContext private val appContext: Context
) {
    val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val credentialManager by lazy { CredentialManager.create(appContext) }

    fun getCurrentUser(): FirebaseUser? {
        return try {
            firebaseAuth.currentUser
        } catch (_: Exception) {
            null
        }
    }

    fun isUserSignedIn(): Boolean {
        return getCurrentUser() != null
    }

    suspend fun signInWithGoogle(activityContext: Context): GoogleAuthResult {
        return try {
            // Lấy default_web_client_id tự động sinh bởi google-services plugin
            val resId = appContext.resources.getIdentifier("default_web_client_id", "string", appContext.packageName)
            val serverClientId = if (resId != 0) {
                try { appContext.getString(resId) } catch (_: Exception) { "" }
            } else {
                ""
            }

            if (serverClientId.isBlank()) {
                return GoogleAuthResult.Error(
                    "Chưa tìm thấy Web Client ID. Bạn hãy vào Firebase Console -> Authentication -> Sign-in method -> Bật Google, sau đó tải lại google-services.json vào thư mục app/ nhé."
                )
            }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                val user = authResult.user

                if (user != null) {
                    val isNew = authResult.additionalUserInfo?.isNewUser ?: false
                    GoogleAuthResult.Success(
                        uid = user.uid,
                        email = user.email ?: googleIdTokenCredential.id,
                        displayName = user.displayName ?: googleIdTokenCredential.displayName,
                        photoUrl = user.photoUrl?.toString() ?: googleIdTokenCredential.profilePictureUri?.toString(),
                        isNewUser = isNew
                    )
                } else {
                    GoogleAuthResult.Error("Không tìm thấy thông tin tài khoản Google.")
                }
            } else {
                GoogleAuthResult.Error("Loại chứng thực không được hỗ trợ.")
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleAuthResult.Cancelled
        } catch (e: Exception) {
            GoogleAuthResult.Error(e.localizedMessage ?: "Lỗi xác thực Google.")
        }
    }

    suspend fun signInAnonymously(): GoogleAuthResult {
        return try {
            val authResult = firebaseAuth.signInAnonymously().await()
            val user = authResult.user
            if (user != null) {
                GoogleAuthResult.Success(
                    uid = user.uid,
                    email = "guest_${user.uid.take(8)}@kiki.app",
                    displayName = "Kiki Explorer",
                    photoUrl = null,
                    isNewUser = true
                )
            } else {
                GoogleAuthResult.Error("Không thể tạo phiên đăng nhập khách.")
            }
        } catch (_: Exception) {
            // Fallback offline nếu chưa kích hoạt anonymous hoặc không có mạng
            val guestId = System.currentTimeMillis()
            GoogleAuthResult.Success(
                uid = "offline_guest_$guestId",
                email = "guest_$guestId@kiki.app",
                displayName = "Kiki Explorer",
                photoUrl = null,
                isNewUser = true
            )
        }
    }

    fun signOut() {
        try {
            firebaseAuth.signOut()
        } catch (_: Exception) {}
    }
}
