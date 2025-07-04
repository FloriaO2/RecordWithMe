package com.example.recordwithme

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // 사용자 인증 상태를 관찰할 수 있는 StateFlow
    private val _authenticatedUser = MutableStateFlow<FirebaseUser?>(null)
    val authenticatedUser: StateFlow<FirebaseUser?> get() = _authenticatedUser

    // Google 로그인 후 받은 ID Token으로 Firebase 인증
    fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        viewModelScope.launch {
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // 인증 성공
                        val user = auth.currentUser
                        _authenticatedUser.value = user
                        Log.d("AuthViewModel", "로그인 성공: ${user?.email}")
                    } else {
                        // 인증 실패
                        Log.w("AuthViewModel", "로그인 실패", task.exception)
                        _authenticatedUser.value = null
                    }
                }
        }
    }

    // 로그인 실패 또는 로그아웃 시 호출
    fun setUser(user: FirebaseUser?) {
        _authenticatedUser.value = user
    }
}