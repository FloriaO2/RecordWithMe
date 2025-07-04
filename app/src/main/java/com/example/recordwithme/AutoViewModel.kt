import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _authenticatedUser = MutableStateFlow(auth.currentUser)
    val authenticatedUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = _authenticatedUser.asStateFlow()

    fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authenticatedUser.value = auth.currentUser
                } else {
                    _authenticatedUser.value = null
                }
            }
    }

    fun setUser(user: com.google.firebase.auth.FirebaseUser?) {
        _authenticatedUser.value = user
    }
}